package seedu.address.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import seedu.address.model.person.Parent;
import seedu.address.model.person.PeopleRegistry;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Student;
import seedu.address.model.person.Tutor;
import seedu.address.model.person.exceptions.ReferencedPersonException;

public class PonHubDataLessonTest {
    private PeopleRegistry registry;
    private Tutor firstTutor;
    private Tutor secondTutorWithSameName;
    private Student firstStudent;
    private Student secondStudent;
    private Student attendanceOnlyStudent;
    private Parent parent;

    @BeforeEach
    public void setUp() {
        registry = new PeopleRegistry();
        firstTutor = registry.addTutor(details("Alex Tan", "90000001"));
        secondStudent = registry.addStudent(new ContactDetails(new Name("Second Student")),
                new EducationLevel("P2"), new Phone("80000002"));
        parent = registry.addParent(details("Parent Tan", "80000000"));
        firstStudent = registry.addStudent(new ContactDetails(new Name("First Student")),
                new EducationLevel("P1"), new Phone("80000001"));
        secondTutorWithSameName = registry.addTutor(details("Alex Tan", "90000002"));
        attendanceOnlyStudent = registry.addStudent(
                new ContactDetails(new Name("Attendance Only Student")),
                new EducationLevel("P3"), new Phone("80000003"));
    }

    @Test
    public void addLesson_existingTutor_allocatesMonotonicIdsAndEmptyRosters() {
        PonHubData data = dataWithAllocationHistory(5);

        Lesson firstLesson = data.addLesson(firstTutor.getId(), slot(LessonDay.MONDAY, "0900", "1000"),
                new Subject("Mathematics"), new Room("R1"));
        Lesson secondLesson = data.addLesson(firstTutor.getId(), slot(LessonDay.MONDAY, "1000", "1100"),
                new Subject("Science"), new Room("R1"));

        assertEquals(new LessonId("L6"), firstLesson.getId());
        assertEquals(new LessonId("L7"), secondLesson.getId());
        assertEquals(List.of(firstLesson, secondLesson), data.getLessons());
        assertEquals(Optional.of(firstLesson), data.getLesson(new LessonId("l6")));
        assertTrue(firstLesson.getEnrolledStudentIds().isEmpty());
        assertTrue(secondLesson.getEnrolledStudentIds().isEmpty());
        assertEquals(7, data.exportState().lastAllocatedLessonSequence());
    }

    @Test
    public void addLesson_overlappingTutorOrNormalizedRoom_rejectsWithoutConsumingId() {
        PonHubData data = dataWithAllocationHistory(0);
        Lesson firstLesson = data.addLesson(firstTutor.getId(), slot(LessonDay.MONDAY, "0900", "1000"),
                new Subject("Mathematics"), new Room("r1"));
        PonHubDataState beforeRejections = data.exportState();

        assertThrows(IllegalArgumentException.class, () -> data.addLesson(firstTutor.getId(),
                slot(LessonDay.MONDAY, "0930", "1030"), new Subject("English"), new Room("R2")));
        assertThrows(IllegalArgumentException.class, () -> data.addLesson(secondTutorWithSameName.getId(),
                slot(LessonDay.MONDAY, "0930", "1030"), new Subject("English"), new Room(" R1 ")));

        assertEquals(beforeRejections, data.exportState());
        Lesson adjacentLesson = data.addLesson(firstTutor.getId(), slot(LessonDay.MONDAY, "1000", "1100"),
                new Subject("English"), new Room("R1"));
        assertEquals(new LessonId("L2"), adjacentLesson.getId());
        assertEquals(List.of(firstLesson, adjacentLesson), data.getLessons());
    }

    @Test
    public void addLesson_differentTutorAndRoomOrWeekday_allowsOverlap() {
        PonHubData data = dataWithAllocationHistory(0);
        Lesson firstLesson = data.addLesson(firstTutor.getId(), slot(LessonDay.MONDAY, "0900", "1000"),
                new Subject("Mathematics"), new Room("R1"));
        Lesson differentTutorAndRoom = data.addLesson(secondTutorWithSameName.getId(),
                slot(LessonDay.MONDAY, "0930", "1030"), new Subject("Science"), new Room("R2"));
        Lesson differentDay = data.addLesson(firstTutor.getId(), slot(LessonDay.TUESDAY, "0900", "1000"),
                new Subject("English"), new Room("R1"));

        assertEquals(List.of(firstLesson, differentTutorAndRoom, differentDay), data.getLessons());
        assertEquals(new LessonId("L2"), differentTutorAndRoom.getId());
        assertEquals(firstTutor.getName(), secondTutorWithSameName.getName());
    }

    @Test
    public void addLesson_missingOrWrongRoleTutor_rejectsCompleteChange() {
        PonHubData data = dataWithAllocationHistory(0);
        PonHubDataState initialState = data.exportState();

        assertThrows(IllegalArgumentException.class, () -> data.addLesson(new PersonId("T99"),
                slot(LessonDay.MONDAY, "0900", "1000"), new Subject("Mathematics"), new Room("R1")));
        assertThrows(IllegalArgumentException.class, () -> data.addLesson(firstStudent.getId(),
                slot(LessonDay.MONDAY, "0900", "1000"), new Subject("Mathematics"), new Room("R1")));

        assertEquals(initialState, data.exportState());
        assertEquals(new LessonId("L1"), data.addLesson(firstTutor.getId(),
                slot(LessonDay.MONDAY, "0900", "1000"), new Subject("Mathematics"), new Room("R1")).getId());
    }

    @Test
    public void addLesson_exhaustedAllocation_rejectsCompleteChange() {
        PonHubData data = dataWithAllocationHistory(Long.MAX_VALUE);
        PonHubDataState initialState = data.exportState();

        assertThrows(IllegalStateException.class, () -> data.addLesson(firstTutor.getId(),
                slot(LessonDay.MONDAY, "0900", "1000"), new Subject("Mathematics"), new Room("R1")));

        assertEquals(initialState, data.exportState());
    }

    @Test
    public void state_overlappingTutorOrNormalizedRoom_rejectsRestoredCandidate() {
        Lesson original = lesson("L1", firstTutor, LessonDay.MONDAY, "0900", "1000", "R1", Set.of());
        Lesson tutorConflict = lesson("L2", firstTutor, LessonDay.MONDAY, "0930", "1030", "R2", Set.of());
        Lesson roomConflict = lesson("L2", secondTutorWithSameName, LessonDay.MONDAY,
                "0930", "1030", "r1", Set.of());

        assertThrows(IllegalArgumentException.class, () -> new PonHubDataState(registry.exportState(),
                List.of(original, tutorConflict), List.of(), 2));
        assertThrows(IllegalArgumentException.class, () -> new PonHubDataState(registry.exportState(),
                List.of(original, roomConflict), List.of(), 2));
    }

    @Test
    public void readApis_returnCanonicalImmutableRelationshipsInDeterministicOrder() {
        Lesson firstLesson = lesson("L1", firstTutor, LessonDay.MONDAY, "0900", "1000", "R1",
                Set.of(firstStudent.getId(), secondStudent.getId()));
        Lesson secondLesson = lesson("L2", firstTutor, LessonDay.TUESDAY, "1000", "1100", "R2",
                Set.of(firstStudent.getId()));
        Lesson thirdLesson = lesson("L3", secondTutorWithSameName, LessonDay.WEDNESDAY,
                "1100", "1200", "R3", Set.of());
        Lesson fourthLesson = lesson("L4", secondTutorWithSameName, LessonDay.THURSDAY,
                "1200", "1300", "R4", Set.of());
        Attendance firstAttendance = attendance(firstStudent, firstLesson, LocalDate.of(2026, 10, 5));
        Attendance secondAttendance = attendance(secondStudent, firstLesson, LocalDate.of(2026, 10, 5));
        Attendance historicalAttendance = attendance(attendanceOnlyStudent, thirdLesson, LocalDate.of(2026, 10, 7));
        PonHubData data = new PonHubData(new PonHubDataState(registry.exportState(),
                List.of(firstLesson, secondLesson, thirdLesson, fourthLesson),
                List.of(firstAttendance, secondAttendance, historicalAttendance), 4));

        List<Student> roster = data.getLessonRoster(firstLesson.getId());
        List<Lesson> studentLessons = data.getStudentLessons(firstStudent.getId());
        List<Attendance> studentAttendance = data.getAttendanceForStudent(secondStudent.getId());
        List<Attendance> lessonAttendance = data.getAttendanceForLesson(firstLesson.getId());

        assertEquals(List.of(secondStudent, firstStudent), roster);
        assertEquals(List.of(firstLesson, secondLesson), studentLessons);
        assertEquals(List.of(secondAttendance), studentAttendance);
        assertEquals(List.of(firstAttendance, secondAttendance), lessonAttendance);
        assertTrue(data.getStudentLessons(attendanceOnlyStudent.getId()).isEmpty());
        assertEquals(List.of(historicalAttendance), data.getAttendanceForStudent(attendanceOnlyStudent.getId()));
        assertThrows(UnsupportedOperationException.class, () -> roster.clear());
        assertThrows(UnsupportedOperationException.class, () -> studentLessons.clear());
        assertThrows(UnsupportedOperationException.class, () -> studentAttendance.clear());
        assertThrows(UnsupportedOperationException.class, () -> lessonAttendance.clear());

        assertTrue(data.isPersonReferenced(firstStudent));
        assertTrue(data.isPersonReferenced(secondStudent));
        assertTrue(data.isPersonReferenced(firstTutor));
        assertTrue(data.isPersonReferenced(secondTutorWithSameName));
        assertTrue(data.isPersonReferenced(attendanceOnlyStudent));
        assertFalse(data.isPersonReferenced(parent));
        assertTrue(data.isLessonReferenced(firstLesson.getId()));
        assertTrue(data.isLessonReferenced(secondLesson.getId()));
        assertTrue(data.isLessonReferenced(thirdLesson.getId()));
        assertFalse(data.isLessonReferenced(fourthLesson.getId()));
    }

    @Test
    public void referenceGuard_integratesWithPeopleRegistryRemovalPredicate() {
        Lesson lesson = lesson("L1", firstTutor, LessonDay.MONDAY, "0900", "1000", "R1",
                Set.of(firstStudent.getId()));
        PonHubData data = new PonHubData(new PonHubDataState(registry.exportState(),
                List.of(lesson), List.of(), 1));

        assertThrows(ReferencedPersonException.class, () ->
                registry.remove(firstStudent.getId(), data::isPersonReferenced));
        assertEquals(parent, registry.remove(parent.getId(), data::isPersonReferenced));
    }

    @Test
    public void relationshipQueries_missingOrWrongRoleIdentity_reject() {
        PonHubData data = dataWithAllocationHistory(0);

        assertThrows(IllegalArgumentException.class, () -> data.getLessonRoster(new LessonId("L1")));
        assertThrows(IllegalArgumentException.class, () -> data.getStudentLessons(new PersonId("S99")));
        assertThrows(IllegalArgumentException.class, () -> data.getStudentLessons(firstTutor.getId()));
        assertThrows(IllegalArgumentException.class, () -> data.getAttendanceForStudent(new PersonId("S99")));
        assertThrows(IllegalArgumentException.class, () -> data.getAttendanceForLesson(new LessonId("L1")));
        assertThrows(IllegalArgumentException.class, () -> data.isLessonReferenced(new LessonId("L1")));
    }

    private PonHubData dataWithAllocationHistory(long lastAllocatedLessonSequence) {
        return new PonHubData(new PonHubDataState(registry.exportState(), List.of(), List.of(),
                lastAllocatedLessonSequence));
    }

    private Lesson lesson(String id, Tutor tutor, LessonDay day, String startTime, String endTime, String room,
            Set<PersonId> studentIds) {
        return new Lesson(new LessonId(id), tutor.getId(), slot(day, startTime, endTime),
                new Subject("Mathematics"), new Room(room), studentIds);
    }

    private LessonTimeSlot slot(LessonDay day, String startTime, String endTime) {
        return new LessonTimeSlot(day, new LessonTime(startTime), new LessonTime(endTime));
    }

    private Attendance attendance(Student student, Lesson lesson, LocalDate date) {
        return new Attendance(student.getId(), lesson.getId(), date, AttendanceStatus.PRESENT);
    }

    private ContactDetails details(String name, String phone) {
        return new ContactDetails(new Name(name), Optional.of(new Phone(phone)),
                Optional.empty(), Optional.empty());
    }
}
