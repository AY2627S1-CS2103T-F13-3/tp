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
import ponhub.model.attendance.AttendanceStatus;
import ponhub.model.lesson.LessonId;
import ponhub.model.query.AttendanceHistoryEntry;
import ponhub.storage.JsonPonHubDataStorage;
import ponhub.storage.ProtectedPonHubDataStorage;
import ponhub.testutil.LessonQueryTestData;

public class CanonicalHistoryFilterTest {
    @TempDir
    Path directory;

    @Test
    public void history_filtersCombineWithoutWritingOrChangingPeople() throws Exception {
        Path path = createFixture();
        CanonicalPeopleSession session = studentSession(path);
        var people = session.getPeople();
        byte[] bytes = Files.readAllBytes(path);

        assertRecords(session, "history 1", "L10:2026-10-12", "L2:2026-10-06", "L10:2026-10-05");
        assertRecords(session, "history 1 s/absent", "L10:2026-10-12", "L2:2026-10-06");
        assertRecords(session, "history 1 d/2026-10-05", "L10:2026-10-05");
        assertRecords(session, "history 1 s/PRESENT d/2026-10-05 lid/l10", "L10:2026-10-05");
        assertEquals("Student S1 · attendance history · L10 · 2026-10-05 · present",
                session.historyResultContextProperty().get());
        assertRecords(session, "history 1 lid/L10 s/absent", "L10:2026-10-12");
        assertRecords(session, "history 1 d/2026-10-05 s/absent");
        assertRecords(session, "history 1 d/2026-10-07");
        assertRecords(session, "history 1 lid/L3 s/absent");
        assertSame(people, session.getPeople());
        assertEquals(List.of("S1"), people.stream().map(person -> person.getId().toString()).toList());
        assertArrayEquals(bytes, Files.readAllBytes(path));
    }

    @Test
    public void history_sameSourceReplacesFiltersAndClearsOmittedFilters() throws Exception {
        CanonicalPeopleSession session = studentSession(createFixture());
        assertRecords(session, "history 1 s/absent", "L10:2026-10-12", "L2:2026-10-06");
        var history = session.getDisplayedHistory();
        assertRecords(session, "history 1 s/present", "L10:2026-10-05");
        assertSame(history, session.getDisplayedHistory());
        assertRecords(session, "history 1", "L10:2026-10-12", "L2:2026-10-06", "L10:2026-10-05");
        assertEquals("Student S1 · attendance history", session.historyResultContextProperty().get());
    }

    @Test
    public void history_filteredResultsRefreshOnCorrectionUnenrolmentAndRemoval() throws Exception {
        Path path = createFixture();
        CanonicalPeopleSession session = studentSession(path);
        assertRecords(session, "history 1 s/absent", "L10:2026-10-12", "L2:2026-10-06");
        var history = session.getDisplayedHistory();
        session.execute("mark 1 lid/L10 d/2026-10-05 s/absent");
        assertEquals(3, history.size());
        session.execute("mark 1 lid/L10 d/2026-10-12 s/present");
        assertEquals(List.of("L2:2026-10-06", "L10:2026-10-05"), keys(history));
        session.execute("unenrol 1 lid/L2");
        assertFalse(history.getFirst().isCurrentlyEnrolled());
        session.execute("unmark 1 lid/L2 d/2026-10-06");
        assertEquals(List.of("L10:2026-10-05"), keys(history));
        session.execute("list r/parent");
        assertSame(history, session.getDisplayedHistory());
        assertEquals(List.of("L10:2026-10-05"), keys(history));
        assertTrue(session.historyViewActiveProperty().get());
        assertRecords(studentSession(path), "history 1 s/absent", "L10:2026-10-05");
    }

    @Test
    public void history_invalidFiltersPreservePriorSelectionAndBytes() throws Exception {
        Path path = createFixture();
        CanonicalPeopleSession session = studentSession(path);
        assertRecords(session, "history 1 s/absent", "L10:2026-10-12", "L2:2026-10-06");
        byte[] bytes = Files.readAllBytes(path);
        String context = session.historyResultContextProperty().get();
        var history = session.getDisplayedHistory();
        List<String> original = keys(history);
        for (String command : List.of("history 1 d/2026-02-29", "history 1 s/unrecorded",
                "history 1 s/absent s/present", "history 1 d/2026-10-05 x/value")) {
            assertThrows(ParseException.class, () -> session.execute(command));
        }
        assertThrows(CommandException.class, () -> session.execute("history 1 lid/L99 s/present"));
        assertThrows(CommandException.class, () -> session.execute("history 99 d/2026-10-05"));
        assertEquals(context, session.historyResultContextProperty().get());
        assertEquals(original, keys(history));
        assertTrue(session.historyViewActiveProperty().get());
        assertArrayEquals(bytes, Files.readAllBytes(path));
    }

    @Test
    public void history_failedSavePreservesFilteredMembershipAndRetryRefreshes() throws Exception {
        Path path = createFixture();
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        int[] writes = {0};
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            if (writes[0]++ == 0) {
                throw new IOException("Injected failure");
            }
            storage.saveData(state);
        });
        session.execute("search c/student n/Alex l/P3");
        assertRecords(session, "history 1 d/2026-10-05 s/present", "L10:2026-10-05");
        byte[] before = Files.readAllBytes(path);
        var original = session.getDataSnapshot();
        assertThrows(CommandException.class, () -> session.execute("mark 1 lid/L10 d/2026-10-05 s/absent"));
        assertSame(original, session.getDataSnapshot());
        assertEquals(List.of("L10:2026-10-05"), keys(session.getDisplayedHistory()));
        assertArrayEquals(before, Files.readAllBytes(path));
        session.execute("mark 1 lid/L10 d/2026-10-05 s/absent");
        assertTrue(session.getDisplayedHistory().isEmpty());
        assertEquals(2, writes[0]);
    }

    private Path createFixture() throws Exception {
        var data = LessonQueryTestData.createData();
        data.markAttendance(LessonQueryTestData.STUDENT_ONE, LessonQueryTestData.SHARED_LESSON,
                LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT);
        data.markAttendance(LessonQueryTestData.STUDENT_ONE, LessonQueryTestData.SHARED_LESSON,
                LocalDate.of(2026, 10, 12), AttendanceStatus.ABSENT);
        data.markAttendance(LessonQueryTestData.STUDENT_ONE, new LessonId("L2"),
                LocalDate.of(2026, 10, 6), AttendanceStatus.ABSENT);
        data.markAttendance(LessonQueryTestData.STUDENT_TWO, LessonQueryTestData.SHARED_LESSON,
                LocalDate.of(2026, 10, 5), AttendanceStatus.ABSENT);
        Path path = directory.resolve("history.json");
        new JsonPonHubDataStorage(path).saveData(data.exportState());
        return path;
    }

    private CanonicalPeopleSession studentSession(Path path) throws Exception {
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("search c/student n/Alex l/P3");
        return session;
    }

    private void assertRecords(CanonicalPeopleSession session, String command, String... expected) throws Exception {
        assertEquals("Showing " + expected.length + " attendance record(s).",
                session.execute(command).getFeedbackToUser());
        assertEquals(List.of(expected), keys(session.getDisplayedHistory()));
    }

    private List<String> keys(List<AttendanceHistoryEntry> history) {
        return history.stream().map(entry -> entry.getLessonId() + ":" + entry.getDate()).toList();
    }
}
