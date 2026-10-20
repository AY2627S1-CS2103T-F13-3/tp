package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.collections.ListChangeListener;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.PonHubDataState;
import seedu.address.model.attendance.AttendanceStatus;
import seedu.address.model.lesson.Lesson;
import seedu.address.model.lesson.LessonDay;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.lesson.LessonTime;
import seedu.address.model.lesson.Room;
import seedu.address.model.lesson.Subject;
import seedu.address.model.person.PersonId;
import seedu.address.model.query.AttendanceHistoryEntry;
import seedu.address.model.query.LessonDetailView;
import seedu.address.model.query.LessonView;
import seedu.address.storage.JsonPonHubDataStorage;
import seedu.address.storage.ProtectedPonHubDataStorage;

/** Exercises Lesson editing through real parsing, staged execution, persistence and live views. */
public class CanonicalLessonEditIntegrationTest {
    @TempDir
    Path directory;

    @Test
    public void execute_allFields_preservesIdentityRosterHistoryPositionAndCounters() throws Exception {
        CanonicalPeopleSession session = baseline("all-fields.json");
        session.execute("mark 1 lid/L1 d/2026-10-05 s/present");
        session.execute("enrol 2 lid/L1");
        session.execute("mark 2 lid/L1 d/2026-10-05 s/absent");
        session.execute("unenrol 2 lid/L1");
        PonHubDataState before = session.getDataSnapshot();
        List<LessonId> ids = before.lessons().stream().map(Lesson::getId).toList();

        assertEquals("Edited lesson: L1.", session.execute("editlesson rm/r4 tp/00333333 tu/tutor   TWO "
                + "s/Advanced Math et/1045 lid/l1 st/0915 d/mon").getFeedbackToUser());

        PonHubDataState after = session.getDataSnapshot();
        Lesson edited = after.lessons().getFirst();
        assertEquals(new LessonId("L1"), edited.getId());
        assertEquals(new PersonId("T3"), edited.getTutorId());
        assertEquals(LessonDay.MONDAY, edited.getTimeSlot().getDay());
        assertEquals(new LessonTime("0915"), edited.getTimeSlot().getStartTime());
        assertEquals(new LessonTime("1045"), edited.getTimeSlot().getEndTime());
        assertEquals(new Subject("Advanced Math"), edited.getSubject());
        assertEquals(new Room("R4"), edited.getRoom());
        assertEquals(List.of(new PersonId("S1")), List.copyOf(edited.getEnrolledStudentIds()));
        assertEquals(ids, after.lessons().stream().map(Lesson::getId).toList());
        assertEquals(before.attendance(), after.attendance());
        assertEquals(before.people(), after.people());
        assertEquals(before.people().getLastAllocatedSequences(), after.people().getLastAllocatedSequences());
        assertEquals(before.lastAllocatedLessonSequence(), after.lastAllocatedLessonSequence());
        assertEquals(after, new CanonicalPeopleSession(session.getDataFilePath()).getDataSnapshot());
    }

    @Test
    public void execute_emptyAndPopulatedLessons_refreshesRosterDetailAndHistoryResults() throws Exception {
        CanonicalPeopleSession session = baseline("result-views.json");
        session.execute("addlesson d/Wed st/1400 et/1500 s/English tu/Tutor One rm/R4");
        session.execute("lessons");
        var displayedLessons = session.getDisplayedLessons();

        LessonView emptyBefore = findDisplayedLesson(displayedLessons, "L4");
        assertEquals(0, emptyBefore.getRosterSize());
        assertEquals("Edited lesson: L4.", session.execute(
                "editlesson lid/L4 st/1430 et/1530 s/Physics rm/R5").getFeedbackToUser());
        LessonView emptyAfter = findDisplayedLesson(displayedLessons, "L4");
        assertSame(displayedLessons, session.getDisplayedLessons());
        assertEquals(new LessonId("L4"), emptyAfter.getLessonId());
        assertEquals(0, emptyAfter.getRosterSize());
        assertEquals(new Subject("Physics"), emptyAfter.getSubject());
        assertEquals(new Room("R5"), emptyAfter.getRoom());
        assertEquals(new LessonTime("1430"), emptyAfter.getTimeSlot().getStartTime());
        assertEquals(new LessonTime("1530"), emptyAfter.getTimeSlot().getEndTime());

        session.execute("mark 1 lid/L1 d/2026-10-05 s/present");
        session.execute("enrol 2 lid/L1");
        session.execute("mark 2 lid/L1 d/2026-10-05 s/absent");
        session.execute("unenrol 2 lid/L1");
        PonHubDataState beforeEdit = session.getDataSnapshot();
        List<LessonId> lessonIds = beforeEdit.lessons().stream().map(Lesson::getId).toList();
        session.execute("showlesson lid/L1 d/2026-10-05");
        var displayedDetail = session.getDisplayedLessons();

        assertEquals("Edited lesson: L1.", session.execute("editlesson lid/L1 st/0915 et/1045 "
                + "s/Advanced Math tu/Tutor Two tp/00333333 rm/R6").getFeedbackToUser());
        assertSame(displayedDetail, session.getDisplayedLessons());
        LessonDetailView detail = (LessonDetailView) displayedDetail.getFirst();
        assertEquals(new LessonId("L1"), detail.getLessonId());
        assertEquals(new PersonId("T3"), detail.getTutorId());
        assertEquals(new Subject("Advanced Math"), detail.getSubject());
        assertEquals(List.of(new PersonId("S1")), detail.currentRoster().stream()
                .map(entry -> entry.student().getId()).toList());
        assertEquals(List.of(new PersonId("S2")), detail.formerAttendance().stream()
                .map(entry -> entry.student().getId()).toList());
        assertTrue(detail.currentRoster().getFirst().status().isPresent());
        assertEquals(AttendanceStatus.PRESENT, detail.currentRoster().getFirst().status().orElseThrow());
        assertEquals(AttendanceStatus.ABSENT, detail.formerAttendance().getFirst().status());

        assertEquals("Showing 1 attendance record(s).",
                session.execute("history 1 lid/L1").getFeedbackToUser());
        AttendanceHistoryEntry currentHistory = session.getDisplayedHistory().getFirst();
        assertEquals(new PersonId("S1"), currentHistory.getStudentId());
        assertEquals(new LessonId("L1"), currentHistory.getLessonId());
        assertTrue(currentHistory.isCurrentlyEnrolled());
        assertEquals(AttendanceHistoryEntry.Status.PRESENT, currentHistory.getStatus());
        assertEquals("Showing 1 attendance record(s).",
                session.execute("history 2 lid/L1").getFeedbackToUser());
        AttendanceHistoryEntry formerHistory = session.getDisplayedHistory().getFirst();
        assertEquals(new PersonId("S2"), formerHistory.getStudentId());
        assertEquals(new LessonId("L1"), formerHistory.getLessonId());
        assertFalse(formerHistory.isCurrentlyEnrolled());
        assertEquals(AttendanceHistoryEntry.Status.ABSENT, formerHistory.getStatus());

        PonHubDataState afterEdit = session.getDataSnapshot();
        assertEquals(lessonIds, afterEdit.lessons().stream().map(Lesson::getId).toList());
        assertEquals(beforeEdit.attendance(), afterEdit.attendance());
        assertEquals(beforeEdit.people().getLastAllocatedSequences(), afterEdit.people().getLastAllocatedSequences());
        assertEquals(beforeEdit.lastAllocatedLessonSequence(), afterEdit.lastAllocatedLessonSequence());
        assertEquals(afterEdit, new CanonicalPeopleSession(session.getDataFilePath()).getDataSnapshot());
    }

    @Test
    public void execute_singleEndpointsAndSelfExclusion_allowValidPartialChanges() throws Exception {
        CanonicalPeopleSession session = baseline("endpoints.json");

        assertEquals("Edited lesson: L2.",
                session.execute("editlesson lid/L2 st/1000").getFeedbackToUser());
        assertEquals("Edited lesson: L2.",
                session.execute("editlesson lid/L2 et/1230").getFeedbackToUser());
        Lesson edited = session.getDataSnapshot().lessons().get(1);
        assertEquals(new LessonTime("1000"), edited.getTimeSlot().getStartTime());
        assertEquals(new LessonTime("1230"), edited.getTimeSlot().getEndTime());
        assertEquals("Edited lesson: L2.",
                session.execute("editlesson lid/L2 s/MATH").getFeedbackToUser());
        assertEquals(session.getDataSnapshot(),
                new CanonicalPeopleSession(session.getDataFilePath()).getDataSnapshot());
    }

    @Test
    public void execute_noOpAndRejectedConflicts_doNotSaveOrPublish() throws Exception {
        Path noOpPath = baseline("no-op.json").getDataFilePath();
        AtomicInteger saves = new AtomicInteger();
        CanonicalPeopleSession noOp = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(noOpPath), state -> {
            saves.incrementAndGet();
            new JsonPonHubDataStorage(noOpPath).saveData(state);
        });
        PonHubDataState before = noOp.getDataSnapshot();
        byte[] bytes = Files.readAllBytes(noOpPath);
        AtomicInteger notifications = new AtomicInteger();
        noOp.getDisplayedLessons().addListener((ListChangeListener<LessonView>) change ->
                notifications.incrementAndGet());

        assertEquals("No changes to lesson: L1.", noOp.execute(
                "editlesson lid/l1 d/MON st/0900 et/1000 s/ Math rm/r1 tu/Tutor One")
                .getFeedbackToUser());
        assertSame(before, noOp.getDataSnapshot());
        assertArrayEquals(bytes, Files.readAllBytes(noOpPath));
        assertEquals(0, saves.get());
        assertEquals(0, notifications.get());

        String[][] conflicts = {
            {"tutor.json", "editlesson lid/L2 st/0930 et/1030 tu/Tutor One", "Tutor schedule overlap"},
            {"room.json", "editlesson lid/L2 st/0930 et/1030 rm/R1", "Room schedule overlap"},
            {"student.json", "editlesson lid/L2 st/0930 et/1030", "Student S1 schedule overlap"}
        };
        for (String[] testCase : conflicts) {
            CanonicalPeopleSession session = baseline(testCase[0]);
            PonHubDataState unchanged = session.getDataSnapshot();
            byte[] unchangedBytes = Files.readAllBytes(session.getDataFilePath());
            CommandException failure = assertThrows(CommandException.class, () -> session.execute(testCase[1]));
            assertTrue(failure.getMessage().contains(testCase[2]));
            assertSame(unchanged, session.getDataSnapshot());
            assertArrayEquals(unchangedBytes, Files.readAllBytes(session.getDataFilePath()));
        }
    }

    @Test
    public void execute_wrongAndMissingSelectors_preserveResultsStateAndSavedBytes() throws Exception {
        CanonicalPeopleSession session = baseline("selectors.json");
        session.execute("lessons");
        PonHubDataState before = session.getDataSnapshot();
        var displayed = session.getDisplayedLessons();
        List<LessonView> results = List.copyOf(displayed);
        byte[] bytes = Files.readAllBytes(session.getDataFilePath());

        for (String command : List.of("editlesson s/Math", "editlesson lid/L0 s/Math",
                "editlesson lid/L1", "editlesson L1 s/Math")) {
            assertThrows(ParseException.class, () -> session.execute(command), command);
            assertSame(before, session.getDataSnapshot());
            assertSame(displayed, session.getDisplayedLessons());
            assertEquals(results, displayed);
            assertArrayEquals(bytes, Files.readAllBytes(session.getDataFilePath()));
        }
        CommandException missing = assertThrows(CommandException.class, () ->
                session.execute("editlesson lid/L99 s/Math"));
        assertEquals("The lesson does not exist: L99", missing.getMessage());
        assertSame(before, session.getDataSnapshot());
        assertEquals(results, displayed);
        assertArrayEquals(bytes, Files.readAllBytes(session.getDataFilePath()));
    }

    @Test
    public void execute_retainedAttendanceRejectsDayChangeThenClearsObsoleteSelectedDate() throws Exception {
        CanonicalPeopleSession session = baseline("weekday.json");
        session.execute("mark 1 lid/L1 d/2026-10-05 s/present");
        session.execute("enrol 2 lid/L1");
        session.execute("mark 2 lid/L1 d/2026-10-05 s/absent");
        session.execute("unenrol 2 lid/L1");
        PonHubDataState before = session.getDataSnapshot();
        byte[] bytes = Files.readAllBytes(session.getDataFilePath());

        CommandException failure = assertThrows(CommandException.class, () ->
                session.execute("editlesson lid/L1 d/Tue"));
        assertTrue(failure.getMessage().contains("retained attendance on 2026-10-05"));
        assertTrue(failure.getMessage().contains("unmark"));
        assertSame(before, session.getDataSnapshot());
        assertArrayEquals(bytes, Files.readAllBytes(session.getDataFilePath()));

        session.execute("unmark 1 lid/L1 d/2026-10-05");
        session.execute("unmark 2 lid/L1 d/2026-10-05");
        session.execute("showlesson lid/L1 d/2026-10-05");
        var displayed = session.getDisplayedLessons();
        assertEquals("Edited lesson: L1.", session.execute("editlesson lid/L1 d/Tue").getFeedbackToUser());
        assertSame(displayed, session.getDisplayedLessons());
        LessonDetailView detail = (LessonDetailView) displayed.getFirst();
        assertTrue(detail.date().isEmpty());
        assertEquals("Lesson L1 · current roster and retained attendance",
                session.lessonResultContextProperty().get());
        assertTrue(session.getDataSnapshot().attendance().isEmpty());
    }

    @Test
    public void execute_failedSavesAndProtectedStartup_preserveStateAndHideRoute() throws Exception {
        CanonicalPeopleSession seeded = baseline("save-failure.json");
        seeded.execute("mark 1 lid/L1 d/2026-10-05 s/present");
        Path path = seeded.getDataFilePath();
        for (IOException injected : List.of(new IOException("disk failure"),
                new AccessDeniedException(path.toString()))) {
            AtomicBoolean failSave = new AtomicBoolean(true);
            AtomicInteger saves = new AtomicInteger();
            CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
                saves.incrementAndGet();
                if (failSave.get()) {
                    throw injected;
                }
                new JsonPonHubDataStorage(path).saveData(state);
            });
            session.execute("list r/student");
            session.execute("history 1 lid/L1");
            var history = session.getDisplayedHistory();
            AttendanceHistoryEntry selectedHistory = history.getFirst();
            String historyContext = session.historyResultContextProperty().get();
            session.execute("showlesson lid/L1 d/2026-10-05");
            PonHubDataState unchanged = session.getDataSnapshot();
            var displayed = session.getDisplayedLessons();
            LessonView selected = displayed.getFirst();
            String context = session.lessonResultContextProperty().get();
            byte[] unchangedBytes = Files.readAllBytes(path);

            assertThrows(CommandException.class, () -> session.execute("editlesson lid/L1 s/Changed Subject"));
            assertSame(unchanged, session.getDataSnapshot());
            assertSame(displayed, session.getDisplayedLessons());
            assertSame(selected, displayed.getFirst());
            assertEquals(context, session.lessonResultContextProperty().get());
            assertSame(history, session.getDisplayedHistory());
            assertSame(selectedHistory, history.getFirst());
            assertEquals(historyContext, session.historyResultContextProperty().get());
            assertArrayEquals(unchangedBytes, Files.readAllBytes(path));
            assertEquals(1, saves.get());

            failSave.set(false);
            assertEquals("Edited lesson: L1.",
                    session.execute("editlesson lid/L1 s/Changed Subject").getFeedbackToUser());
            assertEquals(2, saves.get());
            assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
            new JsonPonHubDataStorage(path).saveData(unchanged);
        }

        Path rejectedPath = directory.resolve("protected.json");
        Files.writeString(rejectedPath, "{");
        CanonicalPeopleSession protectedSession = new CanonicalPeopleSession(rejectedPath);
        String guidance = protectedSession.getDataLoadError().orElseThrow();
        for (String command : List.of("editlesson", "editlesson lid/L1 s/Math", "editlesson lid/L1 s/")) {
            assertEquals(guidance,
                    assertThrows(CommandException.class, () -> protectedSession.execute(command)).getMessage());
        }
        assertFalse(protectedSession.getCommandWords().contains("editlesson"));
        assertThrows(ParseException.class, () -> protectedSession.execute("help editlesson"));
        assertEquals("{", Files.readString(rejectedPath));
    }

    private CanonicalPeopleSession baseline(String filename) throws Exception {
        CanonicalPeopleSession session = new CanonicalPeopleSession(directory.resolve(filename));
        session.execute("add r/tutor n/Tutor One p/00111111");
        session.execute("add r/tutor n/Tutor Two p/00222222");
        session.execute("add r/tutor n/TUTOR TWO p/00333333");
        session.execute("add r/student n/Student One l/P1 pp/00900001");
        session.execute("add r/student n/Student Two l/P2 pp/00900002");
        session.execute("addlesson d/Mon st/0900 et/1000 s/Math tu/Tutor One rm/R1");
        session.execute("addlesson d/Mon st/1100 et/1200 s/Math tu/Tutor Two tp/00222222 rm/R2");
        session.execute("addlesson d/Tue st/0900 et/1000 s/Science tu/Tutor Two tp/00222222 rm/R3");
        session.execute("list r/student");
        session.execute("enrol 1 lid/L1");
        session.execute("enrol 1 lid/L2");
        session.execute("enrol 2 lid/L3");
        return session;
    }

    private static LessonView findDisplayedLesson(List<LessonView> lessons, String id) {
        LessonId lessonId = new LessonId(id);
        return lessons.stream().filter(lesson -> lesson.getLessonId().equals(lessonId)).findFirst().orElseThrow();
    }
}
