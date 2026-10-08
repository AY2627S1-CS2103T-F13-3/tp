package seedu.address.model.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import seedu.address.model.lesson.LessonId;
import seedu.address.model.person.PersonId;

public class AttendanceTest {

    private static final PersonId STUDENT_ID = new PersonId("S1");
    private static final LessonId LESSON_ID = new LessonId("L1");
    private static final LocalDate DATE = LocalDate.of(2026, 10, 8);

    @Test
    public void constructor_validValues_preservesUniqueKeyAndStatus() {
        Attendance attendance = new Attendance(STUDENT_ID, LESSON_ID, DATE, AttendanceStatus.PRESENT);

        assertEquals(STUDENT_ID, attendance.getStudentId());
        assertEquals(LESSON_ID, attendance.getLessonId());
        assertEquals(DATE, attendance.getDate());
        assertEquals(new AttendanceKey(STUDENT_ID, LESSON_ID, DATE), attendance.getKey());
        assertEquals(AttendanceStatus.PRESENT, attendance.getStatus());
    }

    @Test
    public void constructor_wrongRoleOrNullValues_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Attendance(new PersonId("T1"), LESSON_ID, DATE, AttendanceStatus.PRESENT));
        assertThrows(IllegalArgumentException.class, () ->
                new Attendance(new PersonId("P1"), LESSON_ID, DATE, AttendanceStatus.PRESENT));
        assertThrows(NullPointerException.class, () ->
                new Attendance((PersonId) null, LESSON_ID, DATE, AttendanceStatus.PRESENT));
        assertThrows(NullPointerException.class, () ->
                new Attendance(STUDENT_ID, null, DATE, AttendanceStatus.PRESENT));
        assertThrows(NullPointerException.class, () ->
                new Attendance(STUDENT_ID, LESSON_ID, null, AttendanceStatus.PRESENT));
        assertThrows(NullPointerException.class, () ->
                new Attendance(STUDENT_ID, LESSON_ID, DATE, null));
    }

    @Test
    public void parseDate_strictRealDates_acceptsLeapDateAndRejectsInvalidForms() {
        assertEquals(LocalDate.of(2024, 2, 29), Attendance.parseDate("2024-02-29"));
        assertEquals(LocalDate.of(2026, 1, 1), Attendance.parseDate("2026-01-01"));

        String[] invalidDates = {
            "0000-01-01", "2023-02-29", "2024-02-30", "2026-13-01", "2026-00-01", "2026-01-00",
            "26-01-01", "2026-1-01", "2026-01-1", " 2026-01-01", "2026-01-01 ",
            "2026/01/01", "20260101", "+2026-01-01", "２０２６-０１-０１"
        };
        for (String invalidDate : invalidDates) {
            assertThrows(IllegalArgumentException.class, () -> Attendance.parseDate(invalidDate), invalidDate);
        }
        assertThrows(NullPointerException.class, () -> Attendance.parseDate(null));
    }

    @Test
    public void fromStrings_validDateAndCaseInsensitiveStatus_createsRecord() {
        Attendance attendance = Attendance.fromStrings(STUDENT_ID, LESSON_ID, "2024-02-29", " PrEsEnT ");

        assertEquals(LocalDate.of(2024, 2, 29), attendance.getDate());
        assertEquals(AttendanceStatus.PRESENT, attendance.getStatus());
    }

    @Test
    public void attendanceKey_eachComponentControlsEqualityAndHashCode() {
        AttendanceKey original = new AttendanceKey(STUDENT_ID, LESSON_ID, DATE);
        AttendanceKey same = new AttendanceKey(new PersonId("s1"), new LessonId("l1"), DATE);

        assertEquals(original, same);
        assertEquals(original.hashCode(), same.hashCode());
        assertNotEquals(original, new AttendanceKey(new PersonId("S2"), LESSON_ID, DATE));
        assertNotEquals(original, new AttendanceKey(STUDENT_ID, new LessonId("L2"), DATE));
        assertNotEquals(original, new AttendanceKey(STUDENT_ID, LESSON_ID, DATE.plusDays(1)));
        assertNotEquals(original, null);
        assertNotEquals(original, STUDENT_ID);
    }

    @Test
    public void withStatus_returnsReplacementAndLeavesOriginalUnchanged() {
        Attendance original = new Attendance(STUDENT_ID, LESSON_ID, DATE, AttendanceStatus.PRESENT);
        Attendance replacement = original.withStatus(AttendanceStatus.ABSENT);

        assertEquals(original.getKey(), replacement.getKey());
        assertEquals(AttendanceStatus.PRESENT, original.getStatus());
        assertEquals(AttendanceStatus.ABSENT, replacement.getStatus());
        assertNotEquals(original, replacement);
        assertThrows(NullPointerException.class, () -> original.withStatus(null));
    }

    @Test
    public void copyConstructor_preservesValueAndImmutableKey() {
        Attendance original = new Attendance(STUDENT_ID, LESSON_ID, DATE, AttendanceStatus.PRESENT);
        Attendance copy = new Attendance(original);

        assertEquals(original, copy);
        assertEquals(original.hashCode(), copy.hashCode());
        assertNotSame(original, copy);
        assertThrows(NullPointerException.class, () -> new Attendance((Attendance) null));
    }

    @Test
    public void equals_eachKeyComponentAndStatus_distinguishesState() {
        Attendance original = new Attendance(STUDENT_ID, LESSON_ID, DATE, AttendanceStatus.PRESENT);
        Attendance same = new Attendance(new PersonId("s1"), new LessonId("l1"), DATE,
                AttendanceStatus.PRESENT);

        assertEquals(original, same);
        assertEquals(original.hashCode(), same.hashCode());
        assertNotEquals(original,
                new Attendance(new PersonId("S2"), LESSON_ID, DATE, AttendanceStatus.PRESENT));
        assertNotEquals(original,
                new Attendance(STUDENT_ID, new LessonId("L2"), DATE, AttendanceStatus.PRESENT));
        assertNotEquals(original,
                new Attendance(STUDENT_ID, LESSON_ID, DATE.plusDays(1), AttendanceStatus.PRESENT));
        assertNotEquals(original,
                new Attendance(STUDENT_ID, LESSON_ID, DATE, AttendanceStatus.ABSENT));
        assertNotEquals(original, null);
        assertNotEquals(original, original.getKey());
    }
}
