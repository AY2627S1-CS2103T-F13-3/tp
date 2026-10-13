package seedu.address.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.address.model.attendance.Attendance;
import seedu.address.model.attendance.AttendanceStatus;
import seedu.address.model.lesson.Lesson;
import seedu.address.model.lesson.LessonDay;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.lesson.LessonTime;
import seedu.address.model.lesson.LessonTimeSlot;
import seedu.address.model.lesson.Room;
import seedu.address.model.lesson.Subject;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Name;
import seedu.address.model.person.PeopleRegistry;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Student;
import seedu.address.model.person.Tutor;

public class PonHubDataAttendanceTest {
    private PeopleRegistry people;
    private Tutor tutor;
    private Student firstStudent;
    private Student secondStudent;
    private Lesson lesson;

    @BeforeEach
    public void setUp() {
        people = new PeopleRegistry();
        tutor = people.addTutor(contacts("Tutor Tan", "70000001"));
        firstStudent = people.addStudent(new ContactDetails(new Name("First Student")),
                new EducationLevel("P1"), new Phone("60000001"));
        secondStudent = people.addStudent(new ContactDetails(new Name("Second Student")),
                new EducationLevel("P2"), new Phone("60000002"));
        lesson = lesson(Set.of(firstStudent.getId(), secondStudent.getId()));
    }

    @Test
    public void markAttendance_newKeys_appendsIndependentStudentsAndDates() {
        PonHubData data = dataWith(lesson, List.of());

        Attendance first = data.markAttendance(firstStudent.getId(), lesson.getId(),
                LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT);
        Attendance second = data.markAttendance(secondStudent.getId(), lesson.getId(),
                LocalDate.of(2026, 10, 5), AttendanceStatus.ABSENT);
        Attendance later = data.markAttendance(firstStudent.getId(), lesson.getId(),
                LocalDate.of(2026, 10, 12), AttendanceStatus.ABSENT);

        assertEquals(List.of(first, second, later), data.getAttendance());
        assertEquals(List.of(lesson), data.getLessons());
        assertEquals(1, data.exportState().lastAllocatedLessonSequence());
    }

    @Test
    public void markAttendance_existingHistoryAfterUnenrol_correctsInPlaceAndSameStatusIsNoOp() {
        Lesson emptyLesson = lesson(Set.of());
        Attendance historical = new Attendance(firstStudent.getId(), emptyLesson.getId(),
                LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT);
        PonHubData data = dataWith(emptyLesson, List.of(historical));

        Attendance corrected = data.markAttendance(firstStudent.getId(), emptyLesson.getId(),
                historical.getDate(), AttendanceStatus.ABSENT);

        assertEquals(List.of(corrected), data.getAttendance());
        assertEquals(AttendanceStatus.ABSENT, corrected.getStatus());
        PonHubDataState correctedState = data.exportState();
        Attendance unchanged = data.markAttendance(firstStudent.getId(), emptyLesson.getId(),
                historical.getDate(), AttendanceStatus.ABSENT);
        assertSame(corrected, unchanged);
        assertSame(correctedState, data.exportState());
        assertThrows(IllegalArgumentException.class, () -> data.markAttendance(firstStudent.getId(),
                emptyLesson.getId(), LocalDate.of(2026, 10, 12), AttendanceStatus.PRESENT));
        assertSame(correctedState, data.exportState());
    }

    @Test
    public void markAttendance_invalidReferenceMembershipOrWeekday_rejectsWithoutChange() {
        Lesson firstStudentOnly = lesson(Set.of(firstStudent.getId()));
        PonHubData data = dataWith(firstStudentOnly, List.of());
        PonHubDataState before = data.exportState();

        assertThrows(IllegalArgumentException.class, () -> data.markAttendance(new PersonId("S99"),
                firstStudentOnly.getId(), LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT));
        assertThrows(IllegalArgumentException.class, () -> data.markAttendance(tutor.getId(),
                firstStudentOnly.getId(), LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT));
        assertThrows(IllegalArgumentException.class, () -> data.markAttendance(firstStudent.getId(),
                new LessonId("L99"), LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT));
        assertThrows(IllegalArgumentException.class, () -> data.markAttendance(secondStudent.getId(),
                firstStudentOnly.getId(), LocalDate.of(2026, 10, 5), AttendanceStatus.ABSENT));
        assertThrows(IllegalArgumentException.class, () -> data.markAttendance(firstStudent.getId(),
                firstStudentOnly.getId(), LocalDate.of(2026, 10, 6), AttendanceStatus.PRESENT));

        assertSame(before, data.exportState());
    }

    private PonHubData dataWith(Lesson initialLesson, List<Attendance> attendance) {
        return new PonHubData(new PonHubDataState(people.exportState(), List.of(initialLesson), attendance, 1));
    }

    private Lesson lesson(Set<PersonId> roster) {
        return new Lesson(new LessonId("L1"), tutor.getId(),
                new LessonTimeSlot(LessonDay.MONDAY, new LessonTime("0900"), new LessonTime("1000")),
                new Subject("Mathematics"), new Room("R1"), roster);
    }

    private ContactDetails contacts(String name, String phone) {
        return new ContactDetails(new Name(name), Optional.of(new Phone(phone)), Optional.empty(), Optional.empty());
    }
}
