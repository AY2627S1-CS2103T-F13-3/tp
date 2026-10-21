package ponhub.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ponhub.model.attendance.Attendance;
import ponhub.model.attendance.AttendanceStatus;
import ponhub.model.lesson.Lesson;
import ponhub.model.lesson.LessonDay;
import ponhub.model.lesson.LessonId;
import ponhub.model.lesson.LessonTime;
import ponhub.model.lesson.LessonTimeSlot;
import ponhub.model.lesson.Room;
import ponhub.model.lesson.Subject;
import ponhub.model.person.ContactDetails;
import ponhub.model.person.EducationLevel;
import ponhub.model.person.Name;
import ponhub.model.person.PeopleRegistry;
import ponhub.model.person.PersonId;
import ponhub.model.person.Phone;
import ponhub.model.person.Student;
import ponhub.model.person.Tutor;

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

    @Test
    public void unmarkAttendance_existingKey_removesOnlySelectedRecord() {
        Attendance selected = new Attendance(firstStudent.getId(), lesson.getId(),
                LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT);
        Attendance otherStudent = new Attendance(secondStudent.getId(), lesson.getId(),
                LocalDate.of(2026, 10, 5), AttendanceStatus.ABSENT);
        Attendance otherDate = new Attendance(firstStudent.getId(), lesson.getId(),
                LocalDate.of(2026, 10, 12), AttendanceStatus.ABSENT);
        PonHubData data = dataWith(lesson, List.of(selected, otherStudent, otherDate));

        assertEquals(Optional.of(selected), data.unmarkAttendance(firstStudent.getId(), lesson.getId(),
                selected.getDate()));
        assertEquals(List.of(otherStudent, otherDate), data.getAttendance());
        assertEquals(List.of(lesson), data.getLessons());
        assertEquals(Set.of(firstStudent.getId(), secondStudent.getId()),
                data.getLesson(lesson.getId()).orElseThrow().getEnrolledStudentIds());
    }

    @Test
    public void unmarkAttendance_historicalOrMissingRecord_removesWithoutMembershipAndSkipsNoOp() {
        Lesson emptyLesson = lesson(Set.of());
        Attendance historical = new Attendance(firstStudent.getId(), emptyLesson.getId(),
                LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT);
        PonHubData data = dataWith(emptyLesson, List.of(historical));

        assertEquals(Optional.of(historical), data.unmarkAttendance(firstStudent.getId(), emptyLesson.getId(),
                historical.getDate()));
        assertTrue(data.getAttendance().isEmpty());
        PonHubDataState removedState = data.exportState();

        assertEquals(Optional.empty(), data.unmarkAttendance(firstStudent.getId(), emptyLesson.getId(),
                historical.getDate()));
        assertSame(removedState, data.exportState());
        assertEquals(List.of(emptyLesson), data.getLessons());
    }

    @Test
    public void unmarkAttendance_invalidReferenceOrWeekday_rejectsWithoutChange() {
        Attendance attendance = new Attendance(firstStudent.getId(), lesson.getId(),
                LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT);
        PonHubData data = dataWith(lesson, List.of(attendance));
        PonHubDataState before = data.exportState();

        assertThrows(IllegalArgumentException.class, () -> data.unmarkAttendance(new PersonId("S99"),
                lesson.getId(), attendance.getDate()));
        assertThrows(IllegalArgumentException.class, () -> data.unmarkAttendance(tutor.getId(),
                lesson.getId(), attendance.getDate()));
        assertThrows(IllegalArgumentException.class, () -> data.unmarkAttendance(firstStudent.getId(),
                new LessonId("L99"), attendance.getDate()));
        assertThrows(IllegalArgumentException.class, () -> data.unmarkAttendance(firstStudent.getId(),
                lesson.getId(), LocalDate.of(2026, 10, 6)));

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
