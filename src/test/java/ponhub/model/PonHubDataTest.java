package ponhub.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
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
import ponhub.model.person.PeopleRegistryState;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRole;
import ponhub.model.person.Phone;

public class PonHubDataTest {
    private PeopleRegistryState people;
    private Lesson lesson;
    private Attendance attendance;
    private PonHubDataState original;

    @BeforeEach
    public void setUp() {
        PeopleRegistry registry = new PeopleRegistry();
        var tutor = registry.addTutor(new ContactDetails(new Name("Tutor"), Optional.of(new Phone("91234567")),
                Optional.empty(), Optional.empty()));
        var student = registry.addStudent(new ContactDetails(new Name("Student")),
                new EducationLevel("P1"), new Phone("92345678"));
        people = registry.exportState();
        lesson = new Lesson(new LessonId("L1"), tutor.getId(), new LessonTimeSlot(LessonDay.MONDAY,
                new LessonTime("0900"), new LessonTime("1000")), new Subject("Mathematics"), new Room("R1"),
                Set.of(student.getId()));
        attendance = new Attendance(student.getId(), lesson.getId(), LocalDate.of(2026, 10, 5),
                AttendanceStatus.PRESENT);
        original = new PonHubDataState(people, List.of(lesson), List.of(attendance), 5);
    }

    @Test
    public void snapshot_replaceRestoreAndSelfReset_preserveIndependentCompleteState() {
        PonHubData data = new PonHubData(original);
        PonHubData copy = new PonHubData(data);
        PonHubDataState snapshot = data.exportState();
        PonHubDataState changed = new PonHubDataState(people, List.of(lesson.withEnrolledStudentIds(Set.of())),
                List.of(attendance.withStatus(AttendanceStatus.ABSENT)), 6);
        data.resetData(changed);
        assertEquals(original, snapshot);
        assertEquals(original, copy.exportState());
        assertNotEquals(copy, data);
        data.resetData(snapshot);
        data.resetData(data);
        data.resetData(data.exportState());
        assertEquals(copy, data);
        assertEquals(copy.hashCode(), data.hashCode());
        copy.resetData(new PonHubData());
        assertEquals(original, data.exportState());
    }

    @Test
    public void state_copiesInputCollectionsAndProvidesImmutableLookups() {
        List<Lesson> lessons = new ArrayList<>(List.of(lesson));
        List<Attendance> records = new ArrayList<>(List.of(attendance));
        PonHubData data = new PonHubData(new PonHubDataState(people, lessons, records, 5));
        lessons.clear();
        records.clear();
        assertEquals(original, data.exportState());
        assertEquals(people.getPeople(), data.getPeople());
        assertEquals(Optional.of(lesson), data.getLesson(lesson.getId()));
        assertEquals(Optional.of(attendance), data.getAttendance(attendance.getKey()));
        assertEquals(Optional.of(people.getPeople().get(0)), data.getPerson(lesson.getTutorId()));
        assertTrue(data.getPerson(new PersonId("S99")).isEmpty());
        assertTrue(data.getLesson(new LessonId("L99")).isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> data.getPeople().clear());
        assertThrows(UnsupportedOperationException.class, () -> data.getLessons().clear());
        assertThrows(UnsupportedOperationException.class, () -> data.getAttendance().clear());
        assertThrows(UnsupportedOperationException.class, () ->
                data.getLessons().get(0).getEnrolledStudentIds().clear());
    }

    @Test
    public void state_allocationHistoryIncludingExhaustion_survivesEmptyCollectionsAndRestore() {
        var counters = new HashMap<>(people.getLastAllocatedSequences());
        counters.put(PersonRole.STUDENT, Long.MAX_VALUE);
        PeopleRegistryState emptyPeople = new PeopleRegistryState(List.of(), counters);
        PonHubDataState exhausted = new PonHubDataState(emptyPeople, List.of(), List.of(), Long.MAX_VALUE);
        PonHubData data = new PonHubData(new PonHubData(exhausted));
        assertEquals(exhausted, data.exportState());
        data.resetData(original);
        data.resetData(exhausted);
        assertEquals(Long.MAX_VALUE, data.exportState().lastAllocatedLessonSequence());
        assertEquals(counters, data.exportState().people().getLastAllocatedSequences());
        assertThrows(UnsupportedOperationException.class, () -> data.exportState().people()
                .getLastAllocatedSequences().clear());
    }

    @Test
    public void equality_includesRecordsRostersOrderAndCounters() {
        PonHubData data = new PonHubData(original);
        assertTrue(data.equals(data));
        assertFalse(data.equals(null));
        assertFalse(data.equals("not a canonical root"));
        assertNotEquals(original, new PonHubDataState(people, List.of(lesson), List.of(attendance), 6));
        assertNotEquals(original, new PonHubDataState(people, List.of(lesson.withEnrolledStudentIds(Set.of())),
                List.of(attendance), 5));
        assertNotEquals(original, new PonHubDataState(people, List.of(lesson),
                List.of(attendance.withStatus(AttendanceStatus.ABSENT)), 5));
        PeopleRegistryState reordered = new PeopleRegistryState(people.getPeople().reversed(),
                people.getLastAllocatedSequences());
        assertNotEquals(original, new PonHubDataState(reordered, List.of(lesson), List.of(attendance), 5));
        var counters = new HashMap<>(people.getLastAllocatedSequences());
        counters.put(PersonRole.PARENT, 10L);
        assertNotEquals(original, new PonHubDataState(new PeopleRegistryState(people.getPeople(), counters),
                List.of(lesson), List.of(attendance), 5));
    }

    @Test
    public void invalidReplacement_neverChangesExistingState() {
        PonHubData data = new PonHubData(original);
        assertThrows(IllegalArgumentException.class, () -> data.resetData(
                new PonHubDataState(people, List.of(lesson, lesson), List.of(), 5)));
        assertThrows(IllegalArgumentException.class, () -> data.resetData(
                new PonHubDataState(people, List.of(lesson), List.of(attendance, attendance), 5)));
        assertThrows(IllegalArgumentException.class, () -> data.resetData(
                new PonHubDataState(people, List.of(), List.of(attendance), 5)));
        assertThrows(IllegalArgumentException.class, () -> data.resetData(
                new PonHubDataState(people, List.of(lesson), List.of(), 0)));
        assertThrows(IllegalArgumentException.class, () -> new PonHubDataState(people, List.of(), List.of(), -1));
        assertThrows(NullPointerException.class, () -> data.resetData((PonHubDataState) null));
        assertThrows(NullPointerException.class, () -> data.resetData((PonHubData) null));
        assertEquals(original, data.exportState());
    }

    @Test
    public void state_missingPeopleReferences_rejects() {
        PeopleRegistryState emptyPeople = new PeopleRegistryState(List.of(), people.getLastAllocatedSequences());
        assertThrows(IllegalArgumentException.class, () -> new PonHubDataState(emptyPeople,
                List.of(lesson), List.of(), 5));
        assertThrows(IllegalArgumentException.class, () -> new PonHubDataState(people,
                List.of(lesson.withEnrolledStudentIds(Set.of(new PersonId("S99")))), List.of(), 5));
        Attendance missingStudent = new Attendance(new PersonId("S99"), lesson.getId(), attendance.getDate(),
                AttendanceStatus.PRESENT);
        assertThrows(IllegalArgumentException.class, () -> new PonHubDataState(people,
                List.of(lesson), List.of(missingStudent), 5));
    }

    @Test
    public void state_historicalAttendance_doesNotRequireCurrentEnrolment() {
        PonHubData data = new PonHubData(new PonHubDataState(people,
                List.of(lesson.withEnrolledStudentIds(Set.of())), List.of(attendance), 5));
        assertEquals(Optional.of(attendance), data.getAttendance(attendance.getKey()));
        assertTrue(data.getLessons().get(0).getEnrolledStudentIds().isEmpty());
    }
}
