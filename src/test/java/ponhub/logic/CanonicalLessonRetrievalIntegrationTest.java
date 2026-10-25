package ponhub.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.LessonQueryTestData.createData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ponhub.logic.LessonRetrieval.ResultKind;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.query.LessonView;
import ponhub.storage.JsonPonHubDataStorage;
import ponhub.storage.ProtectedPonHubDataStorage;

/**
 * Exercises the active lesson routes against real saved canonical data and the protected persistence boundary.
 */
public class CanonicalLessonRetrievalIntegrationTest {
    @TempDir
    Path directory;

    @Test
    public void realMembership_selectedStudentSurvivesPeopleFilterChangesAndKeepsAttendance() throws Exception {
        Path path = saveFixture();
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        var people = session.getPeople();
        var displayed = session.getDisplayedLessons();
        session.execute("lessons 4"); // Mixed position 4 resolves S2 once.
        assertIds(session, "L10", "L11");
        assertEquals("Student S2 · current lessons", session.lessonResultContextProperty().get());
        session.execute("list r/tutor");
        assertThrows(CommandException.class, () -> session.execute("enrol 1 lid/L4"));
        assertIds(session, "L10", "L11");
        session.execute("search c/student n/Jamie");
        var visiblePeople = List.copyOf(people);
        assertEquals("Student S2 enrolled in lesson L4.", session.execute("enrol 1 lid/L4").getFeedbackToUser());
        assertIds(session, "L10", "L4", "L11");
        session.execute("mark 1 lid/L4 d/2026-10-12 s/present");
        assertIds(session, "L10", "L4", "L11");
        session.execute("unenrol 1 lid/L4");
        assertIds(session, "L10", "L11");
        assertEquals(1, session.getDataSnapshot().attendance().size());
        session.execute("search c/lesson sn/Jamie s/Math");
        assertIds(session, "L10"); // Retained L4 attendance does not satisfy sn/.
        session.execute("enrol 1 lid/L4");
        assertIds(session, "L10", "L4");
        session.execute("unenrol 1 lid/L4");
        assertIds(session, "L10");
        assertSame(displayed, session.getDisplayedLessons());
        assertSame(people, session.getPeople());
        assertEquals(visiblePeople, people);
        assertEquals(11, session.getDataSnapshot().lastAllocatedLessonSequence());
        CanonicalPeopleSession reloaded = new CanonicalPeopleSession(path);
        assertEquals(session.getDataSnapshot(), reloaded.getDataSnapshot());
        reloaded.execute("search c/lesson sn/Jamie s/Math");
        assertIds(reloaded, "L10");
        reloaded.execute("search c/student n/Jamie");
        reloaded.execute("lessons 1");
        assertIds(reloaded, "L10", "L11");
    }

    @Test
    public void realMembership_failedEnrolAndUnenrolPreserveBothResultModesThenRetryAndReload() throws Exception {
        for (String route : List.of("lessons si/1", "search c/lesson sn/Jamie s/Math")) {
            Path path = saveFixture();
            AtomicBoolean rejectWrites = new AtomicBoolean(true);
            CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
                if (rejectWrites.get()) {
                    throw new IOException("Injected membership save failure");
                }
                new JsonPonHubDataStorage(path).saveData(state);
            });
            session.execute("search c/student n/Jamie");
            session.execute(route);
            var displayed = session.getDisplayedLessons();
            var beforeResults = List.copyOf(displayed);
            var beforePeople = List.copyOf(session.getPeople());
            var before = session.getDataSnapshot();
            String context = session.lessonResultContextProperty().get();
            ResultKind kind = session.lessonResultKindProperty().get();
            byte[] bytes = Files.readAllBytes(path);
            assertThrows(CommandException.class, () -> session.execute("enrol 1 lid/L4"));
            assertEquals(before, session.getDataSnapshot());
            assertEquals(beforeResults, displayed);
            assertEquals(context, session.lessonResultContextProperty().get());
            assertEquals(kind, session.lessonResultKindProperty().get());
            assertEquals(beforePeople, session.getPeople());
            assertArrayEquals(bytes, Files.readAllBytes(path));
            rejectWrites.set(false);
            session.execute("enrol 1 lid/L4");
            assertTrue(displayed.stream().anyMatch(lesson -> lesson.getLessonId().toString().equals("L4")));
            var enrolledResults = List.copyOf(displayed);
            var enrolled = session.getDataSnapshot();
            bytes = Files.readAllBytes(path);
            CanonicalPeopleSession reloaded = new CanonicalPeopleSession(path);
            reloaded.execute("search c/student n/Jamie");
            reloaded.execute(route);
            assertEquals(displayed.stream().map(LessonView::getLessonId).toList(),
                    reloaded.getDisplayedLessons().stream().map(LessonView::getLessonId).toList());
            rejectWrites.set(true);
            assertThrows(CommandException.class, () -> session.execute("unenrol 1 lid/L4"));
            assertEquals(enrolled, session.getDataSnapshot());
            assertEquals(enrolledResults, displayed);
            assertEquals(kind, session.lessonResultKindProperty().get());
            assertEquals(beforePeople, session.getPeople());
            assertArrayEquals(bytes, Files.readAllBytes(path));
            rejectWrites.set(false);
            session.execute("unenrol 1 lid/L4");
            assertFalse(displayed.stream().anyMatch(lesson -> lesson.getLessonId().toString().equals("L4")));
            assertSame(displayed, session.getDisplayedLessons());
            assertEquals(beforePeople, session.getPeople());
            reloaded = new CanonicalPeopleSession(path);
            reloaded.execute("search c/student n/Jamie");
            reloaded.execute(route);
            assertEquals(session.getDataSnapshot(), reloaded.getDataSnapshot());
            assertEquals(displayed.stream().map(LessonView::getLessonId).toList(),
                    reloaded.getDisplayedLessons().stream().map(LessonView::getLessonId).toList());
        }
    }

    @Test
    public void catalogueAndStudentSelection_resolveCurrentPeoplePositionAndNeverSave() throws Exception {
        Path path = saveFixture();
        byte[] bytes = Files.readAllBytes(path);
        CanonicalPeopleSession session = readOnlySession(path);
        var people = session.getPeople();
        var results = session.getDisplayedLessons();
        var before = session.getDataSnapshot();
        assertEquals("Showing 5 lesson(s).", session.execute("lessons").getFeedbackToUser());
        assertIds(session, "L2", "L10", "L3", "L4", "L11");
        assertEquals(0, results.get(2).getRosterSize());
        assertEquals(2, results.get(1).getRosterSize());
        session.execute("lessons si/4"); // Mixed people position 4 is S2, not S4.
        assertIds(session, "L10", "L11");
        session.execute("list r/student");
        session.execute("lessons si/2");
        assertIds(session, "L10", "L11");
        session.execute("search c/student n/Jamie");
        session.execute("lessons si/1"); // Filtered position 1 still selects S2.
        assertIds(session, "L10", "L11");
        session.execute("list r/tutor"); // Changing people afterwards must not retarget the held result.
        assertIds(session, "L10", "L11");
        session.execute("lessons");
        assertEquals(3, people.size());
        assertSame(people, session.getPeople());
        assertSame(results, session.getDisplayedLessons());
        assertThrows(UnsupportedOperationException.class, results::clear);
        assertSame(before, session.getDataSnapshot());
        assertArrayEquals(bytes, Files.readAllBytes(path));
    }

    @Test
    public void search_allSevenFiltersMatchOneLessonAndKeepPeopleSearch() throws Exception {
        Path path = saveFixture();
        byte[] bytes = Files.readAllBytes(path);
        CanonicalPeopleSession session = readOnlySession(path);
        session.execute("search c/student n/Jamie");
        var people = List.copyOf(session.getPeople());
        assertEquals("Found 1 lesson(s).", session.execute("search c/LeSsOn sn/aLeX d/mON st/0900 et/1000"
                + " s/at tu/MEI rm/r1").getFeedbackToUser());
        assertIds(session, "L10");
        session.execute("search c/lesson d/Tue s/Math");
        assertIds(session); // One Tuesday Science and another Monday Math cannot combine.
        session.execute("search c/lesson sn/Alex d/Mon");
        assertIds(session, "L10");
        session.execute("search c/lesson d/Mon st/0900");
        assertIds(session, "L10", "L3"); // Empty roster survives without sn/.
        session.execute("search c/lesson et/2359");
        assertIds(session, "L11");
        session.execute("search c/lesson s/中文 st/0000");
        assertIds(session, "L11");
        session.execute("search c/lesson tu/T1");
        assertIds(session); // tu/ is a tutor name, never an ID.
        assertEquals("Found 0 lesson(s).", session.execute("search c/lesson sn/Nobody").getFeedbackToUser());
        assertIds(session);
        session.execute("search c/lesson");
        assertIds(session, "L2", "L10", "L3", "L4", "L11");
        assertEquals(people, session.getPeople());
        assertArrayEquals(bytes, Files.readAllBytes(path));
    }

    @Test
    public void invalidCommands_preserveSelectedLessonsPeopleAndSavedBytes() throws Exception {
        Path path = saveFixture();
        byte[] bytes = Files.readAllBytes(path);
        CanonicalPeopleSession session = readOnlySession(path);
        session.execute("search c/lesson s/Math");
        var people = List.copyOf(session.getPeople());
        for (String arguments : List.of("si/", "si/0", "si/-1", "si/+1", "si/1.0", "si/2147483648",
                "si/999999999999999999999999999999", "si/S1", "si/１", "sid/S1", "si/2 si/4",
                "si/2 x/no", "si/2\n", "si/2\u2028", "si/2\r", "si/2\u0000")) {
            assertThrows(ParseException.class, () -> session.execute("lessons " + arguments), arguments);
            assertIds(session, "L10", "L4");
            assertEquals(ResultKind.SEARCH, session.lessonResultKindProperty().get());
        }
        for (String arguments : List.of("n/Mei", "pp/12345678", "sn/", "s/   ", "rm/", "d/Funday",
                "d/Monday", "st/900", "st/2400", "et/1260", "st/-100", "st/0900 et/0900",
                "st/1000 et/0900", "tu/Mei tu/Lim", "d/Mon d/Mon", "c/lesson", "unknown/x",
                "s/a/b", "s/Math\n", "rm/\t")) {
            assertThrows(ParseException.class, () -> session.execute("search c/lesson " + arguments), arguments);
            assertIds(session, "L10", "L4");
            assertEquals(ResultKind.SEARCH, session.lessonResultKindProperty().get());
        }
        for (String arguments : List.of("1", "si/1", "3", "si/3", "8", "si/8")) {
            assertThrows(CommandException.class, () -> session.execute("lessons " + arguments), arguments);
            assertIds(session, "L10", "L4");
            assertEquals(ResultKind.SEARCH, session.lessonResultKindProperty().get());
        }
        assertEquals(people, session.getPeople());
        assertArrayEquals(bytes, Files.readAllBytes(path));
    }

    @Test
    public void emptyCatalogueAndStudentWithoutLessons_reportZeroAndNeverCreateFile() throws Exception {
        Path path = directory.resolve("missing/data.json");
        CanonicalPeopleSession empty = new CanonicalPeopleSession(path);
        assertEquals(ResultKind.ALL_LESSONS, empty.lessonResultKindProperty().get());
        assertEquals("Showing 0 lesson(s).", empty.execute("lessons").getFeedbackToUser());
        assertEquals(ResultKind.ALL_LESSONS, empty.lessonResultKindProperty().get());
        assertEquals("Found 0 lesson(s).", empty.execute("search c/lesson").getFeedbackToUser());
        assertEquals(ResultKind.SEARCH, empty.lessonResultKindProperty().get());
        assertThrows(CommandException.class, () -> empty.execute("lessons si/1"));
        assertEquals(ResultKind.SEARCH, empty.lessonResultKindProperty().get());
        assertFalse(Files.exists(path.getParent()));
        CanonicalPeopleSession session = readOnlySession(saveFixture());
        assertEquals("Showing 0 lesson(s).", session.execute("lessons si/6").getFeedbackToUser());
        assertEquals(ResultKind.STUDENT_LESSONS, session.lessonResultKindProperty().get());
        assertIds(session);
    }

    @Test
    public void addLesson_saveFailureThenCommitAndReload_refreshOnlyMatchingSelectedResults() throws Exception {
        Path path = saveFixture();
        byte[] bytes = Files.readAllBytes(path);
        int[] writes = {0};
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            if (writes[0]++ == 0) {
                throw new IOException("Injected write failure");
            }
            new JsonPonHubDataStorage(path).saveData(state);
        });
        session.execute("search c/student n/Jamie");
        session.execute("search c/lesson s/Math");
        var people = List.copyOf(session.getPeople());
        var displayed = session.getDisplayedLessons();
        String command = "addlesson d/Fri st/1000 et/1100 s/Math tu/Unassigned Tutor rm/R9";
        assertThrows(CommandException.class, () -> session.execute(command));
        assertIds(session, "L10", "L4");
        assertArrayEquals(bytes, Files.readAllBytes(path));
        session.execute(command);
        assertIds(session, "L10", "L4", "L12");
        assertEquals(0, displayed.getLast().getRosterSize());
        assertSame(displayed, session.getDisplayedLessons());
        assertEquals(people, session.getPeople());
        CanonicalPeopleSession reloaded = new CanonicalPeopleSession(path);
        assertEquals(session.getDataSnapshot(), reloaded.getDataSnapshot());
        reloaded.execute("search c/lesson s/Math");
        assertIds(reloaded, "L10", "L4", "L12");
        session.execute("lessons si/1");
        session.execute("addlesson d/Sat st/1000 et/1100 s/Math tu/Unassigned Tutor rm/R9");
        assertIds(session, "L10", "L11"); // The selected student's roster excludes the new empty lesson.
    }

    @Test
    public void helpAndProtectedStartup_advertiseOnlyPermittedRoutes() throws Exception {
        CanonicalPeopleSession session = readOnlySession(saveFixture());
        assertTrue(session.execute("help").getFeedbackToUser().contains("lessons -"));
        String lessonsHelp = session.execute("help LESSONS").getFeedbackToUser();
        assertTrue(lessonsHelp.contains("Usage: lessons [STUDENT_INDEX]"));
        assertTrue(lessonsHelp.contains("si/STUDENT_INDEX form remains accepted for compatibility"));
        assertTrue(session.execute("help search").getFeedbackToUser().contains("sn/STUDENT_NAME"));
        Path path = directory.resolve("rejected.json");
        Files.writeString(path, "{rejected");
        CanonicalPeopleSession protectedSession = new CanonicalPeopleSession(path);
        for (String command : List.of("lessons", "lessons si/0", "search c/lesson")) {
            assertThrows(CommandException.class, () -> protectedSession.execute(command));
            assertEquals(ResultKind.ALL_LESSONS, protectedSession.lessonResultKindProperty().get());
            assertFalse(protectedSession.historyViewActiveProperty().get());
        }
        assertThrows(ParseException.class, () -> protectedSession.execute("help lessons"));
        assertFalse(protectedSession.execute("help").getFeedbackToUser().contains("lessons -"));
        assertEquals("{rejected", Files.readString(path));
    }

    @Test
    public void resultKinds_helpPeopleChangesAndFailedReadRoutesRetainAuthoritativeScope() throws Exception {
        for (var route : resultRoutes().entrySet()) {
            Path path = saveFixture();
            CanonicalPeopleSession session = readOnlySession(path);
            byte[] bytes = Files.readAllBytes(path);
            var kind = session.lessonResultKindProperty();
            assertSame(kind, session.lessonResultKindProperty());
            session.execute(route.getKey());
            assertEquals(route.getValue(), kind.get());
            String context = session.lessonResultContextProperty().get();
            var results = List.copyOf(session.getDisplayedLessons());
            for (String command : List.of("help", "list r/tutor", "search c/parent")) {
                session.execute(command);
                assertEquals(route.getValue(), kind.get());
                assertEquals(context, session.lessonResultContextProperty().get());
                assertEquals(results, session.getDisplayedLessons());
                assertFalse(session.historyViewActiveProperty().get());
            }
            // Parent position 1 cannot be borrowed as a Student position after filtering.
            for (String command : List.of("lessons si/1", "lessons si/99", "history 1",
                    "showlesson lid/L99", "showlesson lid/L10 d/2026-10-06")) {
                assertThrows(CommandException.class, () -> session.execute(command), command);
                assertEquals(route.getValue(), kind.get());
                assertEquals(context, session.lessonResultContextProperty().get());
                assertEquals(results, session.getDisplayedLessons());
                assertFalse(session.historyViewActiveProperty().get());
            }
            for (String command : List.of("lessons si/0", "search c/lesson d/Funday", "history 0",
                    "showlesson lid/L3 d/invalid")) {
                assertThrows(ParseException.class, () -> session.execute(command), command);
                assertEquals(route.getValue(), kind.get());
            }
            session.execute("list r/student");
            session.execute("history 3 lid/L3");
            assertTrue(session.getDisplayedHistory().isEmpty());
            assertTrue(session.historyViewActiveProperty().get());
            assertEquals(route.getValue(), kind.get());
            String historyContext = session.historyResultContextProperty().get();
            assertThrows(CommandException.class, () -> session.execute("lessons si/99"));
            assertThrows(ParseException.class, () -> session.execute("search c/lesson s/"));
            assertTrue(session.historyViewActiveProperty().get());
            assertEquals(historyContext, session.historyResultContextProperty().get());
            assertEquals(route.getValue(), kind.get());
            assertArrayEquals(bytes, Files.readAllBytes(path));
        }
    }

    @Test
    public void historyInvalidation_restoresEachRetainedLessonKindAfterCommittedDeletion() throws Exception {
        for (var route : resultRoutes().entrySet()) {
            CanonicalPeopleSession session = new CanonicalPeopleSession(saveFixture());
            session.execute(route.getKey());
            session.execute("list r/student");
            session.execute("history 3");
            assertTrue(session.historyViewActiveProperty().get());
            session.execute("delete 3");
            assertFalse(session.historyViewActiveProperty().get());
            assertEquals(route.getValue(), session.lessonResultKindProperty().get());
        }
        CanonicalPeopleSession session = new CanonicalPeopleSession(saveFixture());
        session.execute("showlesson lid/L3");
        session.execute("history 6 lid/L3");
        session.execute("deletelesson lid/L3");
        assertFalse(session.historyViewActiveProperty().get());
        assertEquals(ResultKind.DETAIL, session.lessonResultKindProperty().get());
        assertTrue(session.getDisplayedLessons().isEmpty());
        assertEquals("Lesson L3 is no longer available", session.lessonResultContextProperty().get());
    }

    @Test
    public void failedSaveAndCommittedRefresh_preserveEveryResultKindAndHistoryContext() throws Exception {
        for (var route : resultRoutes().entrySet()) {
            Path path = saveFixture();
            AtomicBoolean rejectWrites = new AtomicBoolean(true);
            CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
                if (rejectWrites.get()) {
                    throw new IOException("Injected result-state save failure");
                }
                new JsonPonHubDataStorage(path).saveData(state);
            });
            session.execute(route.getKey());
            session.execute("history 6");
            var before = session.getDataSnapshot();
            var results = List.copyOf(session.getDisplayedLessons());
            String context = session.lessonResultContextProperty().get();
            String historyContext = session.historyResultContextProperty().get();
            byte[] bytes = Files.readAllBytes(path);
            String edit = "editlesson lid/L3 s/Updated";
            assertThrows(CommandException.class, () -> session.execute(edit));
            assertSame(before, session.getDataSnapshot());
            assertEquals(results, session.getDisplayedLessons());
            assertEquals(context, session.lessonResultContextProperty().get());
            assertEquals(route.getValue(), session.lessonResultKindProperty().get());
            assertTrue(session.historyViewActiveProperty().get());
            assertEquals(historyContext, session.historyResultContextProperty().get());
            assertArrayEquals(bytes, Files.readAllBytes(path));
            rejectWrites.set(false);
            session.execute(edit);
            assertEquals(route.getValue(), session.lessonResultKindProperty().get());
            assertTrue(session.historyViewActiveProperty().get());
            assertEquals(historyContext, session.historyResultContextProperty().get());
            session.execute("delete 6");
            assertFalse(session.historyViewActiveProperty().get());
            assertEquals(route.getValue(), session.lessonResultKindProperty().get());
        }
    }

    private static Map<String, ResultKind> resultRoutes() {
        return Map.of("lessons", ResultKind.ALL_LESSONS, "lessons si/4", ResultKind.STUDENT_LESSONS,
                "search c/lesson", ResultKind.SEARCH, "showlesson lid/L3", ResultKind.DETAIL);
    }

    private Path saveFixture() throws Exception {
        Path path = directory.resolve("canonical.json");
        new JsonPonHubDataStorage(path).saveData(createData().exportState());
        return path;
    }

    private CanonicalPeopleSession readOnlySession(Path path) {
        return new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            throw new AssertionError("Retrieval must not call the snapshot writer");
        });
    }

    private static void assertIds(CanonicalPeopleSession session, String... expected) {
        assertEquals(List.of(expected), session.getDisplayedLessons().stream()
                .map(LessonView::getLessonId).map(Object::toString).toList());
    }
}
