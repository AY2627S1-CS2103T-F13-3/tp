package ponhub.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.PonHubData;
import ponhub.model.PonHubDataState;
import ponhub.model.attendance.AttendanceStatus;
import ponhub.model.person.PersonRecord;
import ponhub.model.query.AttendanceHistoryEntry;
import ponhub.storage.JsonPonHubDataStorage;
import ponhub.storage.ProtectedPonHubDataStorage;
import ponhub.testutil.LessonQueryTestData;

public class CanonicalHistoryIntegrationTest {
    @TempDir
    Path directory;

    @Test
    public void history_filteredIndexRetainedMembershipCorrectionAndRestart() throws Exception {
        Path path = directory.resolve("canonical.json");
        PonHubData data = LessonQueryTestData.createData();
        data.markAttendance(LessonQueryTestData.STUDENT_TWO, LessonQueryTestData.SHARED_LESSON,
                LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT);
        new JsonPonHubDataStorage(path).saveData(data.exportState());
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("search c/student n/Jamie");
        var people = session.getPeople();
        List<String> ids = people.stream().map(person -> person.getId().toString()).toList();
        assertEquals(List.of("S2"), ids);
        byte[] before = Files.readAllBytes(path);

        assertEquals("Showing 1 attendance record(s).", session.execute("history 1 lid/L10").getFeedbackToUser());
        var history = session.getDisplayedHistory();
        assertTrue(session.historyViewActiveProperty().get());
        assertEquals("Student S2 · attendance history · L10", session.historyResultContextProperty().get());
        assertEquals(AttendanceHistoryEntry.Status.PRESENT, history.get(0).getStatus());
        assertTrue(history.get(0).isCurrentlyEnrolled());
        assertSame(people, session.getPeople());
        assertEquals(ids, people.stream().map(PersonRecord::getId).map(Object::toString).toList());
        assertArrayEquals(before, Files.readAllBytes(path));

        session.execute("unenrol 1 lid/L10");
        assertSame(history, session.getDisplayedHistory());
        assertFalse(history.get(0).isCurrentlyEnrolled());
        assertEquals("Showing 1 attendance record(s).", session.execute("history 1").getFeedbackToUser());
        session.execute("mark 1 lid/L10 d/2026-10-05 s/absent");
        assertEquals(AttendanceHistoryEntry.Status.ABSENT, history.get(0).getStatus());
        assertFalse(history.get(0).isCurrentlyEnrolled());
        assertEquals(ids, people.stream().map(PersonRecord::getId).map(Object::toString).toList());

        CanonicalPeopleSession restarted = new CanonicalPeopleSession(path);
        restarted.execute("search c/student n/Jamie");
        restarted.execute("history 1 lid/L10");
        assertEquals(AttendanceHistoryEntry.Status.ABSENT, restarted.getDisplayedHistory().get(0).getStatus());
        assertFalse(restarted.getDisplayedHistory().get(0).isCurrentlyEnrolled());
        assertFalse(restarted.getLessonQueries().getStudentLessons(LessonQueryTestData.STUDENT_TWO).stream()
                .anyMatch(lesson -> lesson.getLessonId().equals(LessonQueryTestData.SHARED_LESSON)));
    }

    @Test
    public void history_emptyErrorsAndLessonViewSwitchPreserveSelectionAndBytes() throws Exception {
        Path path = directory.resolve("canonical.json");
        new JsonPonHubDataStorage(path).saveData(LessonQueryTestData.createData().exportState());
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        byte[] before = Files.readAllBytes(path);
        assertThrows(CommandException.class, () -> session.execute("history 1"));
        assertThrows(CommandException.class, () -> session.execute("history 99"));
        assertThrows(ParseException.class, () -> session.execute("history 1 lid/L1 lid/L2"));
        session.execute("search c/student n/Jamie");
        assertEquals("Showing 0 attendance record(s).", session.execute("history 1").getFeedbackToUser());
        assertTrue(session.getDisplayedHistory().isEmpty());
        assertThrows(CommandException.class, () -> session.execute("history 1 lid/L99"));
        assertTrue(session.historyViewActiveProperty().get());
        assertEquals("Student S2 · attendance history", session.historyResultContextProperty().get());
        assertThrows(ParseException.class, () -> session.execute("lessons si/0"));
        assertTrue(session.historyViewActiveProperty().get());
        assertThrows(CommandException.class, () -> session.execute("showlesson lid/L99"));
        assertThrows(CommandException.class, () -> session.execute("showlesson lid/L10 d/2026-10-06"));
        assertTrue(session.historyViewActiveProperty().get());
        session.execute("showlesson lid/L10 d/2026-10-05");
        assertFalse(session.historyViewActiveProperty().get());
        session.execute("history 1");
        session.execute("lessons");
        assertFalse(session.historyViewActiveProperty().get());
        session.execute("history 1");
        session.execute("search c/lesson s/Math");
        assertFalse(session.historyViewActiveProperty().get());
        assertEquals(List.of("S2"), session.getPeople().stream()
                .map(person -> person.getId().toString()).toList());
        assertArrayEquals(before, Files.readAllBytes(path));
    }

    @Test
    public void history_failedCorrectionDoesNotPublishAndRetryRefreshes() throws Exception {
        Path path = directory.resolve("canonical.json");
        PonHubData data = LessonQueryTestData.createData();
        data.markAttendance(LessonQueryTestData.STUDENT_ONE, LessonQueryTestData.SHARED_LESSON,
                LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT);
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        storage.saveData(data.exportState());
        byte[] before = Files.readAllBytes(path);
        int[] writes = {0};
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            if (writes[0]++ == 0) {
                throw new IOException("Injected failure");
            }
            storage.saveData(state);
        });
        session.execute("search c/student n/Alex l/P3");
        session.execute("history 1");
        var history = session.getDisplayedHistory();
        PonHubDataState original = session.getDataSnapshot();
        assertThrows(CommandException.class, () -> session.execute("mark 1 lid/L10 d/2026-10-05 s/absent"));
        assertSame(original, session.getDataSnapshot());
        assertEquals(AttendanceHistoryEntry.Status.PRESENT, history.get(0).getStatus());
        assertArrayEquals(before, Files.readAllBytes(path));
        session.execute("mark 1 lid/L10 d/2026-10-05 s/absent");
        assertEquals(AttendanceHistoryEntry.Status.ABSENT, history.get(0).getStatus());
        assertEquals(2, writes[0]);
    }

    @Test
    public void history_protectedStartupRejectsRouteWithoutChangingRejectedFile() throws Exception {
        Path path = directory.resolve("rejected.json");
        Files.writeString(path, "{broken json");
        byte[] before = Files.readAllBytes(path);
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        assertFalse(session.getCommandWords().contains("history"));
        assertThrows(CommandException.class, () -> session.execute("history 1"));
        assertTrue(session.getDisplayedHistory().isEmpty());
        assertArrayEquals(before, Files.readAllBytes(path));
    }

    @Test
    public void history_failedRemovalPreservesRetainedRecordsAndRetryRefreshes() throws Exception {
        Path path = directory.resolve("canonical.json");
        PonHubData data = LessonQueryTestData.createData();
        data.markAttendance(LessonQueryTestData.STUDENT_TWO, LessonQueryTestData.SHARED_LESSON,
                LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT);
        data.unenrolStudent(LessonQueryTestData.STUDENT_TWO, LessonQueryTestData.SHARED_LESSON);
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        storage.saveData(data.exportState());
        byte[] before = Files.readAllBytes(path);
        int[] writes = {0};
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            if (writes[0]++ == 0) {
                throw new IOException("Injected failure");
            }
            storage.saveData(state);
        });
        session.execute("search c/student n/Jamie");
        session.execute("history 1 lid/L10");
        var history = session.getDisplayedHistory();
        PonHubDataState original = session.getDataSnapshot();
        assertFalse(history.getFirst().isCurrentlyEnrolled());

        assertThrows(CommandException.class, () -> session.execute("unmark 1 lid/L10 d/2026-10-05"));
        assertSame(original, session.getDataSnapshot());
        assertEquals(AttendanceHistoryEntry.Status.PRESENT, history.getFirst().getStatus());
        assertArrayEquals(before, Files.readAllBytes(path));
        session.execute("unmark 1 lid/L10 d/2026-10-05");
        assertTrue(history.isEmpty());
        assertTrue(session.historyViewActiveProperty().get());
        session.execute("unmark 1 lid/L10 d/2026-10-05");
        assertTrue(history.isEmpty());
        assertEquals(2, writes[0]);
        assertTrue(new CanonicalPeopleSession(path).getDataSnapshot().attendance().isEmpty());
    }
}
