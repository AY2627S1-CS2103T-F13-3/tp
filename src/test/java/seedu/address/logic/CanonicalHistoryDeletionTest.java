package seedu.address.logic;

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

import javafx.collections.ObservableList;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.PonHubData;
import seedu.address.model.attendance.AttendanceStatus;
import seedu.address.model.query.AttendanceHistoryEntry;
import seedu.address.storage.JsonPonHubDataStorage;
import seedu.address.storage.ProtectedPonHubDataStorage;
import seedu.address.testutil.LessonQueryTestData;

/**
 * Covers history selection validity at the committed deletion boundary.
 */
public class CanonicalHistoryDeletionTest {
    @TempDir
    Path directory;

    @Test
    public void delete_selectedStudentClearsEmptyHistoryAcrossFilters() throws Exception {
        for (String suffix : List.of("", " lid/L3")) {
            CanonicalPeopleSession session = createSession(LessonQueryTestData.createData());
            session.execute("search c/student");
            session.execute("history 3" + suffix);
            var history = session.getDisplayedHistory();
            // The selected Student remains S3 even when another role occupies index 1.
            session.execute("search c/tutor");
            assertTrue(session.historyViewActiveProperty().get());
            session.execute("search c/student");
            session.execute("delete 3");
            assertInvalidated(session);
            assertSame(history, session.getDisplayedHistory());
            assertEquals(2, session.getPeople().size());
            assertFalse(new CanonicalPeopleSession(session.getDataFilePath()).getDataSnapshot().people().getPeople()
                    .stream().anyMatch(person -> person.getId().equals(LessonQueryTestData.STUDENT_WITHOUT_LESSONS)));
            session.execute("history 1");
            assertTrue(session.historyViewActiveProperty().get());
            assertEquals("Student S1 · attendance history", session.historyResultContextProperty().get());
        }
    }

    @Test
    public void deletelesson_selectedEmptyLessonClearsHistoryAndCanSelectAgain() throws Exception {
        CanonicalPeopleSession session = createSession(LessonQueryTestData.createData());
        session.execute("search c/student");
        session.execute("history 2 lid/L3");
        var people = List.copyOf(session.getPeople());
        session.execute("deletelesson lid/L3");
        assertInvalidated(session);
        assertEquals(people, session.getPeople());
        assertFalse(new CanonicalPeopleSession(session.getDataFilePath()).getLessonQueries()
                .findLessonById(LessonQueryTestData.EMPTY_LESSON).isPresent());
        assertThrows(CommandException.class, () -> session.execute("history 2 lid/L3"));
        assertInvalidated(session);
        session.execute("history 2 lid/L10");
        assertTrue(session.historyViewActiveProperty().get());
        assertEquals("Student S2 · attendance history · L10", session.historyResultContextProperty().get());
    }

    @Test
    public void unrelatedDeletionsAndRejectedDeletionsPreserveValidHistory() throws Exception {
        for (String suffix : List.of("", " lid/L10")) {
            PonHubData data = LessonQueryTestData.createData();
            data.markAttendance(LessonQueryTestData.STUDENT_TWO, LessonQueryTestData.SHARED_LESSON,
                    LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT);
            CanonicalPeopleSession session = createSession(data);
            session.execute("search c/student");
            session.execute("history 2" + suffix);
            var history = session.getDisplayedHistory();
            var rows = List.copyOf(history);
            String context = session.historyResultContextProperty().get();
            for (String command : List.of("delete 2", "delete 99", "deletelesson lid/L10",
                    "deletelesson lid/L99")) {
                var before = session.getDataSnapshot();
                byte[] bytes = Files.readAllBytes(session.getDataFilePath());
                assertThrows(CommandException.class, () -> session.execute(command));
                assertSame(before, session.getDataSnapshot());
                assertArrayEquals(bytes, Files.readAllBytes(session.getDataFilePath()));
                assertHistory(session, history, rows, context);
            }
            session.execute("delete 3");
            assertHistory(session, history, rows, context);
            session.execute("deletelesson lid/L3");
            assertHistory(session, history, rows, context);
        }
    }

    @Test
    public void failedSavePreservesSelectedIdentityAndRetryInvalidates() throws Exception {
        for (String command : List.of("delete 3", "deletelesson lid/L3")) {
            Path path = directory.resolve(command.startsWith("delete ") ? "student.json" : "lesson.json");
            JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
            storage.saveData(LessonQueryTestData.createData().exportState());
            int[] writes = {0};
            CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
                if (writes[0]++ == 0) {
                    throw new IOException("Injected deletion save failure");
                }
                storage.saveData(state);
            });
            session.execute("search c/student");
            session.execute("history 3 lid/L3");
            var history = session.getDisplayedHistory();
            String context = session.historyResultContextProperty().get();
            var before = session.getDataSnapshot();
            byte[] bytes = Files.readAllBytes(path);
            assertThrows(CommandException.class, () -> session.execute(command));
            assertSame(before, session.getDataSnapshot());
            assertArrayEquals(bytes, Files.readAllBytes(path));
            assertHistory(session, history, List.of(), context);
            session.execute(command);
            assertInvalidated(session);
            assertEquals(2, writes[0]);
        }
    }

    private CanonicalPeopleSession createSession(PonHubData data) throws IOException {
        Path path = directory.resolve("canonical.json");
        new JsonPonHubDataStorage(path).saveData(data.exportState());
        return new CanonicalPeopleSession(path);
    }

    private void assertInvalidated(CanonicalPeopleSession session) {
        assertFalse(session.historyViewActiveProperty().get());
        assertTrue(session.getDisplayedHistory().isEmpty());
        assertEquals("Attendance history", session.historyResultContextProperty().get());
    }

    private void assertHistory(CanonicalPeopleSession session, ObservableList<AttendanceHistoryEntry> history,
            List<AttendanceHistoryEntry> rows, String context) {
        assertTrue(session.historyViewActiveProperty().get());
        assertSame(history, session.getDisplayedHistory());
        assertEquals(rows, session.getDisplayedHistory());
        assertEquals(context, session.historyResultContextProperty().get());
    }
}
