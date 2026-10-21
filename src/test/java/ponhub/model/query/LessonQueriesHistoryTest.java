package ponhub.model.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import ponhub.model.PonHubData;
import ponhub.model.PonHubDataState;
import ponhub.model.attendance.AttendanceStatus;
import ponhub.model.lesson.LessonId;
import ponhub.model.person.PersonId;
import ponhub.testutil.LessonQueryTestData;

public class LessonQueriesHistoryTest {
    @Test
    public void history_sortsAndRefreshesRetainedRecordsWithoutChangingMembership() {
        PonHubData data = LessonQueryTestData.createData();
        var student = LessonQueryTestData.STUDENT_ONE;
        var shared = LessonQueryTestData.SHARED_LESSON;
        var other = new LessonId("L4");
        data.enrolStudent(student, other);
        data.markAttendance(student, shared, LocalDate.of(2026, 10, 12), AttendanceStatus.PRESENT);
        data.markAttendance(student, shared, LocalDate.of(2026, 10, 5), AttendanceStatus.ABSENT);
        data.markAttendance(student, other, LocalDate.of(2026, 10, 12), AttendanceStatus.ABSENT);
        LessonQueries queries = new LessonQueries(data);
        var all = queries.getAttendanceHistory(student);
        var oneLesson = queries.getAttendanceHistory(student, shared);

        assertEquals(List.of(other, shared, shared), all.stream().map(AttendanceHistoryEntry::getLessonId).toList());
        assertEquals(List.of(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 10, 5)), all.stream().map(AttendanceHistoryEntry::getDate).toList());
        assertEquals(2, oneLesson.size());
        assertEquals(AttendanceHistoryEntry.Status.PRESENT, oneLesson.get(0).getStatus());
        assertTrue(oneLesson.get(0).isCurrentlyEnrolled());
        assertEquals(student, oneLesson.get(0).getStudentId());
        assertThrows(UnsupportedOperationException.class, all::clear);

        data.unenrolStudent(student, shared);
        assertSame(all, queries.getAttendanceHistory(student));
        assertSame(oneLesson, queries.getAttendanceHistory(student, shared));
        assertFalse(oneLesson.get(0).isCurrentlyEnrolled());
        assertTrue(all.get(0).isCurrentlyEnrolled());
        assertEquals(List.of(new LessonId("L2"), other), queries.getStudentLessons(student).stream()
                .map(LessonView::getLessonId).toList());

        data.markAttendance(student, shared, LocalDate.of(2026, 10, 12), AttendanceStatus.ABSENT);
        assertEquals(AttendanceHistoryEntry.Status.ABSENT, oneLesson.get(0).getStatus());
        assertEquals(3, all.size());
        PonHubDataState state = data.exportState();
        data.resetData(new PonHubDataState(state.people(), state.lessons(), state.attendance().stream()
                .filter(record -> !record.getLessonId().equals(shared)
                        || !record.getDate().equals(LocalDate.of(2026, 10, 12))).toList(),
                state.lastAllocatedLessonSequence()));
        assertEquals(1, oneLesson.size());
        assertEquals(LocalDate.of(2026, 10, 5), oneLesson.get(0).getDate());
        assertEquals(2, all.size());
    }

    @Test
    public void history_validatesReferencesAndReturnsStableEmptyResults() {
        LessonQueries queries = new LessonQueries(LessonQueryTestData.createData());
        var student = LessonQueryTestData.STUDENT_WITHOUT_LESSONS;
        var all = queries.getAttendanceHistory(student);
        var oneLesson = queries.getAttendanceHistory(student, LessonQueryTestData.EMPTY_LESSON);
        assertTrue(all.isEmpty());
        assertTrue(oneLesson.isEmpty());
        assertSame(all, queries.getAttendanceHistory(student));
        assertSame(oneLesson, queries.getAttendanceHistory(student, LessonQueryTestData.EMPTY_LESSON));
        assertThrows(IllegalArgumentException.class, () -> queries.getAttendanceHistory(new PersonId("T1")));
        assertThrows(IllegalArgumentException.class, () -> queries.getAttendanceHistory(new PersonId("S99")));
        assertThrows(IllegalArgumentException.class, () -> queries.getAttendanceHistory(student, new LessonId("L99")));
        assertThrows(NullPointerException.class, () -> queries.getAttendanceHistory(null));
        assertThrows(NullPointerException.class, () -> queries.getAttendanceHistory(student, null));
        assertThrows(IllegalArgumentException.class, () -> new AttendanceHistoryEntryData(
                new PersonId("T1"), LessonQueryTestData.EMPTY_LESSON, LocalDate.of(2026, 10, 5),
                AttendanceHistoryEntry.Status.PRESENT, false));
        assertThrows(NullPointerException.class, () -> new AttendanceHistoryEntryData(
                null, LessonQueryTestData.EMPTY_LESSON, LocalDate.of(2026, 10, 5),
                AttendanceHistoryEntry.Status.PRESENT, false));
    }
}
