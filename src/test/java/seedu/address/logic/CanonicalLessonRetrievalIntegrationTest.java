package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.LessonQueryTestData.createData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.query.LessonView;
import seedu.address.storage.JsonPonHubDataStorage;
import seedu.address.storage.ProtectedPonHubDataStorage;

/**
 * Exercises the active lesson routes against real saved canonical data and the protected persistence boundary.
 */
public class CanonicalLessonRetrievalIntegrationTest {
    @TempDir
    Path directory;

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
                "si/999999999999999999999999999999", "si/S1", "si/１", "1", "sid/S1", "si/2 si/4",
                "si/2 x/no", "si/2\n", "si/2\u2028", "si/2\r", "si/2\u0000")) {
            assertThrows(ParseException.class, () -> session.execute("lessons " + arguments), arguments);
            assertIds(session, "L10", "L4");
        }
        for (String arguments : List.of("n/Mei", "pp/12345678", "sn/", "s/   ", "rm/", "d/Funday",
                "d/Monday", "st/900", "st/2400", "et/1260", "st/-100", "st/0900 et/0900",
                "st/1000 et/0900", "tu/Mei tu/Lim", "d/Mon d/Mon", "c/lesson", "unknown/x",
                "s/a/b", "s/Math\n", "rm/\t")) {
            assertThrows(ParseException.class, () -> session.execute("search c/lesson " + arguments), arguments);
            assertIds(session, "L10", "L4");
        }
        for (String arguments : List.of("si/1", "si/3", "si/8")) {
            assertThrows(CommandException.class, () -> session.execute("lessons " + arguments), arguments);
            assertIds(session, "L10", "L4");
        }
        assertEquals(people, session.getPeople());
        assertArrayEquals(bytes, Files.readAllBytes(path));
    }

    @Test
    public void emptyCatalogueAndStudentWithoutLessons_reportZeroAndNeverCreateFile() throws Exception {
        Path path = directory.resolve("missing/data.json");
        CanonicalPeopleSession empty = new CanonicalPeopleSession(path);
        assertEquals("Showing 0 lesson(s).", empty.execute("lessons").getFeedbackToUser());
        assertEquals("Found 0 lesson(s).", empty.execute("search c/lesson").getFeedbackToUser());
        assertThrows(CommandException.class, () -> empty.execute("lessons si/1"));
        assertFalse(Files.exists(path.getParent()));
        CanonicalPeopleSession session = readOnlySession(saveFixture());
        assertEquals("Showing 0 lesson(s).", session.execute("lessons si/6").getFeedbackToUser());
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
        assertTrue(session.execute("help LESSONS").getFeedbackToUser().contains("si/STUDENT_INDEX"));
        assertTrue(session.execute("help search").getFeedbackToUser().contains("sn/STUDENT_NAME"));
        Path path = directory.resolve("rejected.json");
        Files.writeString(path, "{rejected");
        CanonicalPeopleSession protectedSession = new CanonicalPeopleSession(path);
        for (String command : List.of("lessons", "lessons si/0", "search c/lesson")) {
            assertThrows(CommandException.class, () -> protectedSession.execute(command));
        }
        assertThrows(ParseException.class, () -> protectedSession.execute("help lessons"));
        assertFalse(protectedSession.execute("help").getFeedbackToUser().contains("lessons -"));
        assertEquals("{rejected", Files.readString(path));
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
