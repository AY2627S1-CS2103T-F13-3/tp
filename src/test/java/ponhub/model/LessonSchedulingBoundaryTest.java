package ponhub.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
import ponhub.model.person.Phone;
import ponhub.model.person.Student;
import ponhub.model.person.Tutor;

public class LessonSchedulingBoundaryTest {
    private PeopleRegistry registry;
    private Tutor firstTutor;
    private Tutor secondTutor;
    private Student firstStudent;
    private Student secondStudent;

    @BeforeEach
    public void setUp() {
        registry = new PeopleRegistry();
        firstTutor = registry.addTutor(details("First Tutor", "90000001"));
        secondTutor = registry.addTutor(details("Second Tutor", "90000002"));
        firstStudent = registry.addStudent(new ContactDetails(new Name("First Student")),
                new EducationLevel("P1"), new Phone("80000001"));
        secondStudent = registry.addStudent(new ContactDetails(new Name("Second Student")),
                new EducationLevel("P2"), new Phone("80000002"));
    }

    @Test
    public void enrolStudent_containedLessonsWithDisjointNonEmptyRosters_succeedsAndRestores() {
        PonHubData data = new PonHubData(new PonHubDataState(registry.exportState(), List.of(), List.of(), 0));
        Lesson outer = data.addLesson(firstTutor.getId(), slot("0900", "1200"),
                new Subject("Mathematics"), new Room("R1"));
        Lesson inner = data.addLesson(secondTutor.getId(), slot("1000", "1100"),
                new Subject("Science"), new Room("R2"));

        Lesson enrolledOuter = data.enrolStudent(firstStudent.getId(), outer.getId());
        Lesson enrolledInner = data.enrolStudent(secondStudent.getId(), inner.getId());

        assertEquals(List.of(enrolledOuter, enrolledInner), data.getLessons());
        assertEquals(Set.of(firstStudent.getId()), enrolledOuter.getEnrolledStudentIds());
        assertEquals(Set.of(secondStudent.getId()), enrolledInner.getEnrolledStudentIds());
        assertEquals(List.of(enrolledOuter), data.getStudentLessons(firstStudent.getId()));
        assertEquals(List.of(enrolledInner), data.getStudentLessons(secondStudent.getId()));
        assertEquals(2, data.exportState().lastAllocatedLessonSequence());
        assertEquals(data.exportState(), new PonHubData(data.exportState()).exportState());
        PonHubData reversed = new PonHubData(new PonHubDataState(registry.exportState(),
                List.of(enrolledInner, enrolledOuter), List.of(), 2));
        assertEquals(List.of(enrolledInner, enrolledOuter), reversed.getLessons());
    }

    @Test
    public void enrolStudent_containedTargetWithSharedStudent_rejectsAtomically() {
        assertSharedStudentRejected(slot("0900", "1200"), slot("1000", "1100"));
    }

    @Test
    public void enrolStudent_containingTargetWithSharedStudent_rejectsAtomically() {
        assertSharedStudentRejected(slot("1000", "1100"), slot("0900", "1200"));
    }

    @Test
    public void restore_sharedStudentContainmentInEitherCatalogueOrder_rejectsAtomically() {
        Lesson outer = lesson("L1", firstTutor, slot("0900", "1200"), "R1")
                .withEnrolledStudent(firstStudent.getId());
        Lesson inner = lesson("L2", secondTutor, slot("1000", "1100"), "R2")
                .withEnrolledStudent(firstStudent.getId());
        PonHubData data = new PonHubData(new PonHubDataState(registry.exportState(),
                List.of(outer), List.of(), 1));
        PonHubDataState before = data.exportState();
        AtomicInteger publications = new AtomicInteger();
        data.stateProperty().addListener((observable, oldState, newState) -> publications.incrementAndGet());

        assertThrows(IllegalArgumentException.class, () -> data.resetData(new PonHubDataState(
                registry.exportState(), List.of(outer, inner), List.of(), 2)));
        assertSame(before, data.exportState());
        assertThrows(IllegalArgumentException.class, () -> data.resetData(new PonHubDataState(
                registry.exportState(), List.of(inner, outer), List.of(), 2)));
        assertSame(before, data.exportState());
        assertEquals(0, publications.get());
    }

    @Test
    public void addLesson_containmentWithSameTutorOrNormalizedRoom_rejectsAtomically() {
        for (List<LessonTimeSlot> slots : List.of(List.of(slot("0900", "1200"), slot("1000", "1100")),
                List.of(slot("1000", "1100"), slot("0900", "1200")))) {
            Lesson existing = lesson("L1", firstTutor, slots.get(0), "R1")
                    .withEnrolledStudent(firstStudent.getId());
            PonHubData data = new PonHubData(new PonHubDataState(registry.exportState(),
                    List.of(existing), List.of(), 1));
            PonHubDataState before = data.exportState();

            assertThrows(IllegalArgumentException.class, () -> data.addLesson(firstTutor.getId(), slots.get(1),
                    new Subject("Science"), new Room("R2")));
            assertSame(before, data.exportState());
            assertThrows(IllegalArgumentException.class, () -> data.addLesson(secondTutor.getId(), slots.get(1),
                    new Subject("Science"), new Room(" r1 ")));
            assertSame(before, data.exportState());
            Lesson next = data.addLesson(firstTutor.getId(), slot("1200", "1300"),
                    new Subject("Science"), new Room("R1"));
            assertEquals(new LessonId("L2"), next.getId());
        }
    }

    private void assertSharedStudentRejected(LessonTimeSlot sourceSlot, LessonTimeSlot targetSlot) {
        Lesson source = lesson("L1", firstTutor, sourceSlot, "R1").withEnrolledStudent(firstStudent.getId());
        Lesson target = lesson("L2", secondTutor, targetSlot, "R2").withEnrolledStudent(secondStudent.getId());
        Attendance retained = new Attendance(firstStudent.getId(), source.getId(), LocalDate.of(2026, 10, 5),
                AttendanceStatus.PRESENT);
        PonHubData data = new PonHubData(new PonHubDataState(registry.exportState(),
                List.of(source, target), List.of(retained), 2));
        PonHubDataState before = data.exportState();
        AtomicInteger publications = new AtomicInteger();
        data.stateProperty().addListener((observable, oldState, newState) -> publications.incrementAndGet());

        assertThrows(IllegalArgumentException.class, () -> data.enrolStudent(firstStudent.getId(), target.getId()));

        assertSame(before, data.exportState());
        assertEquals(0, publications.get());
        assertEquals(Set.of(secondStudent.getId()), data.getLesson(target.getId()).orElseThrow()
                .getEnrolledStudentIds());
        assertEquals(List.of(retained), data.getAttendance());
        assertEquals(2, data.exportState().lastAllocatedLessonSequence());
        Lesson next = data.addLesson(firstTutor.getId(), slot("1200", "1300"),
                new Subject("Science"), new Room("R1"));
        assertEquals(new LessonId("L3"), next.getId());
    }

    private Lesson lesson(String id, Tutor tutor, LessonTimeSlot timeSlot, String room) {
        return new Lesson(new LessonId(id), tutor.getId(), timeSlot, new Subject("Mathematics"),
                new Room(room), Set.of());
    }

    private LessonTimeSlot slot(String start, String end) {
        return new LessonTimeSlot(LessonDay.MONDAY, new LessonTime(start), new LessonTime(end));
    }

    private ContactDetails details(String name, String phone) {
        return new ContactDetails(new Name(name), Optional.of(new Phone(phone)), Optional.empty(), Optional.empty());
    }
}
