package ponhub.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ponhub.logic.parser.LessonEditInput;
import ponhub.model.PonHubData;
import ponhub.model.PonHubDataState;
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
import ponhub.model.person.PeopleRegistryState;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.PersonRole;
import ponhub.model.person.Phone;
import ponhub.model.person.Student;
import ponhub.model.person.Tutor;

public class LessonEditCandidateTest {

    @Test
    public void prepare_allFieldsSet_preservesPositionIdentityRosterHistoryAndCounters() {
        PonHubData liveData = new PonHubData(sourceWithHistory());
        PonHubDataState source = liveData.exportState();
        LessonEditInput input = input(Optional.of(LessonDay.WEDNESDAY), Optional.of(new LessonTime("1100")),
                Optional.of(new LessonTime("1200")), Optional.of(new Subject("Physics")),
                Optional.of(new PersonId("T3")), Optional.of(new Room("R4")));
        LessonEditCandidate candidate = LessonEditCandidate.prepare(source, new LessonId("L3"), input);
        Lesson original = findLesson(source, "L3");
        Lesson expected = new Lesson(new LessonId("L3"), new PersonId("T3"),
                new LessonTimeSlot(LessonDay.WEDNESDAY, new LessonTime("1100"), new LessonTime("1200")),
                new Subject("Physics"), new Room("R4"), original.getEnrolledStudentIds());

        assertTrue(candidate.isChanged());
        assertEquals(expected, candidate.getEditedLesson());
        assertEquals(List.of("L1", "L2", "L3"), candidate.getCandidateState().lessons().stream()
                .map(lesson -> lesson.getId().toString()).toList());
        assertEquals(List.copyOf(original.getEnrolledStudentIds()),
                List.copyOf(candidate.getEditedLesson().getEnrolledStudentIds()));
        assertNotSame(source, candidate.getCandidateState());
        assertRetainedState(source, candidate.getCandidateState());
        assertSame(source, liveData.exportState());
    }

    @Test
    public void prepare_nonDayEditWithCurrentAndFormerAttendance_preservesCompleteHistory() {
        PonHubDataState source = sourceWithHistory();
        LessonEditInput input = input(Optional.empty(), Optional.of(new LessonTime("1100")),
                Optional.of(new LessonTime("1200")), Optional.of(new Subject("Advanced Math")),
                Optional.of(new PersonId("T3")), Optional.of(new Room("R4")));
        LessonEditCandidate candidate = LessonEditCandidate.prepare(source, new LessonId("L1"), input);

        assertTrue(candidate.isChanged());
        assertEquals(LessonDay.MONDAY, candidate.getEditedLesson().getTimeSlot().getDay());
        assertEquals(new LessonTime("1100"), candidate.getEditedLesson().getTimeSlot().getStartTime());
        assertEquals(new LessonTime("1200"), candidate.getEditedLesson().getTimeSlot().getEndTime());
        assertEquals(new PersonId("T3"), candidate.getEditedLesson().getTutorId());
        assertEquals(source.attendance(), candidate.getCandidateState().attendance());
        assertEquals(source.people(), candidate.getCandidateState().people());
        assertEquals(source.lastAllocatedLessonSequence(),
                candidate.getCandidateState().lastAllocatedLessonSequence());
    }

    @Test
    public void prepare_omittedAndNormalizedIdenticalValues_returnsOriginalSnapshotWithoutChange() {
        PonHubDataState source = sourceWithHistory();
        Lesson original = findLesson(source, "L1");
        LessonEditInput sameValues = input(Optional.of(LessonDay.MONDAY), Optional.of(new LessonTime("0900")),
                Optional.of(new LessonTime("1000")), Optional.of(new Subject("  Math  ")),
                Optional.of(new PersonId("T1")), Optional.of(new Room(" r1 ")));

        for (LessonEditInput input : List.of(emptyInput(), sameValues)) {
            LessonEditCandidate candidate = LessonEditCandidate.prepare(source, original.getId(), input);

            assertFalse(candidate.isChanged());
            assertSame(source, candidate.getCandidateState());
            assertSame(original, candidate.getEditedLesson());
        }
    }

    @Test
    public void prepare_subjectDisplayCaseChange_isObservableAndExcludesSelfFromConflicts() {
        PonHubDataState source = sourceWithHistory();
        LessonEditInput input = input(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(new Subject("MATH")), Optional.empty(), Optional.empty());
        LessonEditCandidate candidate = LessonEditCandidate.prepare(source, new LessonId("L1"), input);

        assertTrue(candidate.isChanged());
        assertEquals(new Subject("MATH"), candidate.getEditedLesson().getSubject());
        assertEquals(findLesson(source, "L1").getTimeSlot(), candidate.getEditedLesson().getTimeSlot());
        assertEquals(findLesson(source, "L1").getTutorId(), candidate.getEditedLesson().getTutorId());
        assertEquals(findLesson(source, "L1").getRoom(), candidate.getEditedLesson().getRoom());
    }

    @Test
    public void prepare_adjacentSharedResources_allowsBoundaryWithoutOverlap() {
        PonHubDataState source = sourceWithHistory();
        LessonEditInput input = input(Optional.of(LessonDay.MONDAY), Optional.of(new LessonTime("0800")),
                Optional.of(new LessonTime("0900")), Optional.empty(), Optional.of(new PersonId("T1")),
                Optional.of(new Room("R1")));
        LessonEditCandidate candidate = LessonEditCandidate.prepare(source, new LessonId("L3"), input);

        assertTrue(candidate.isChanged());
        assertEquals(new LessonTimeSlot(LessonDay.MONDAY, new LessonTime("0800"), new LessonTime("0900")),
                candidate.getEditedLesson().getTimeSlot());
        assertEquals(new PersonId("T1"), candidate.getEditedLesson().getTutorId());
        assertEquals(new Room("R1"), candidate.getEditedLesson().getRoom());
    }

    @Test
    public void prepare_tutorRoomAndStudentConflicts_rejectsWithoutChangingSource() {
        PonHubDataState source = sourceWithHistory();
        Optional<LessonDay> monday = Optional.of(LessonDay.MONDAY);
        Optional<LessonTime> startTime = Optional.of(new LessonTime("0900"));
        Optional<LessonTime> endTime = Optional.of(new LessonTime("1000"));
        List<LessonEditInput> conflicts = List.of(
                input(monday, startTime, endTime, Optional.empty(), Optional.of(new PersonId("T1")),
                        Optional.of(new Room("R4"))),
                input(monday, startTime, endTime, Optional.empty(), Optional.of(new PersonId("T3")),
                        Optional.of(new Room("R1"))),
                input(monday, startTime, endTime, Optional.empty(), Optional.of(new PersonId("T3")),
                        Optional.of(new Room("R4"))));

        for (LessonEditInput input : conflicts) {
            assertThrows(IllegalArgumentException.class, () ->
                    LessonEditCandidate.prepare(source, new LessonId("L3"), input));
            assertEquals(sourceWithHistory(), source);
        }
    }

    @Test
    public void prepare_invalidCombinedSlot_rejectsWithoutChangingSource() {
        PonHubDataState source = sourceWithHistory();
        LessonEditInput input = input(Optional.empty(), Optional.of(new LessonTime("1000")),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                LessonEditCandidate.prepare(source, new LessonId("L1"), input));
        assertEquals(sourceWithHistory(), source);
    }

    @Test
    public void prepare_missingAndWrongRoleTutors_rejectsWithoutChangingSource() {
        PonHubDataState source = sourceWithHistory();
        for (PersonId tutorId : List.of(new PersonId("T99"), new PersonId("S1"))) {
            LessonEditInput input = input(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.of(tutorId), Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                    LessonEditCandidate.prepare(source, new LessonId("L1"), input));
            assertEquals(sourceWithHistory(), source);
        }
    }

    @Test
    public void prepare_dayChangeInvalidatingFormerMemberAttendance_rejectsWithoutChangingSource() {
        PonHubDataState baseline = sourceWithHistory();
        Attendance formerMemberAttendance = baseline.attendance().get(1);
        Lesson original = findLesson(baseline, "L1");
        assertFalse(original.hasEnrolledStudent(formerMemberAttendance.getStudentId()));
        PonHubDataState source = new PonHubDataState(baseline.people(), baseline.lessons(),
                List.of(formerMemberAttendance), baseline.lastAllocatedLessonSequence());
        LessonEditInput input = input(Optional.of(LessonDay.TUESDAY), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                LessonEditCandidate.prepare(source, original.getId(), input));
        assertEquals(List.of(formerMemberAttendance), source.attendance());
        assertEquals(original, findLesson(source, "L1"));
    }

    @Test
    public void prepare_exhaustedAllocationHistory_editsWithoutAllocating() {
        PonHubDataState baseline = sourceWithHistory();
        PonHubDataState source = new PonHubDataState(baseline.people(), baseline.lessons(), baseline.attendance(),
                Long.MAX_VALUE);
        LessonEditInput input = input(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(new Subject("Further Math")), Optional.empty(), Optional.empty());
        LessonEditCandidate candidate = LessonEditCandidate.prepare(source, new LessonId("L1"), input);

        assertTrue(candidate.isChanged());
        assertEquals(Long.MAX_VALUE, candidate.getCandidateState().lastAllocatedLessonSequence());
        assertEquals(source.people().getLastAllocatedSequences(),
                candidate.getCandidateState().people().getLastAllocatedSequences());
        assertEquals(source.lessons().stream().map(Lesson::getId).toList(),
                candidate.getCandidateState().lessons().stream().map(Lesson::getId).toList());
    }

    @Test
    public void prepare_nullArgumentsAndMissingTarget_rejectsWithoutChangingLiveState() {
        PonHubData liveData = new PonHubData(sourceWithHistory());
        PonHubDataState source = liveData.exportState();
        LessonEditInput input = emptyInput();

        assertThrows(NullPointerException.class, () ->
                LessonEditCandidate.prepare(null, new LessonId("L1"), input));
        assertThrows(NullPointerException.class, () -> LessonEditCandidate.prepare(source, null, input));
        assertThrows(NullPointerException.class, () ->
                LessonEditCandidate.prepare(source, new LessonId("L1"), null));
        assertThrows(IllegalArgumentException.class, () ->
                LessonEditCandidate.prepare(source, new LessonId("L99"), input));
        assertSame(source, liveData.exportState());
    }

    private static LessonEditInput emptyInput() {
        return input(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty());
    }

    private static LessonEditInput input(Optional<LessonDay> day, Optional<LessonTime> startTime,
            Optional<LessonTime> endTime, Optional<Subject> subject, Optional<PersonId> tutorId,
            Optional<Room> room) {
        return new LessonEditInput(day, startTime, endTime, subject, tutorId, room);
    }

    private static Lesson findLesson(PonHubDataState source, String id) {
        return source.lessons().stream().filter(lesson -> lesson.getId().equals(new LessonId(id)))
                .findFirst().orElseThrow();
    }

    private static void assertRetainedState(PonHubDataState source, PonHubDataState candidate) {
        assertEquals(source.people(), candidate.people());
        assertEquals(source.attendance(), candidate.attendance());
        assertEquals(source.lastAllocatedLessonSequence(), candidate.lastAllocatedLessonSequence());
        assertEquals(source.lessons().getFirst(), candidate.lessons().getFirst());
        assertEquals(source.lessons().get(1), candidate.lessons().get(1));
    }

    private static PonHubDataState sourceWithHistory() {
        List<PersonRecord> people = List.of(
                tutor("T1", "Tutor One", "00111111"),
                student("S1", "Student One", "00900001"),
                tutor("T2", "Tutor Two", "00222222"),
                student("S2", "Student Two", "00900002"),
                tutor("T3", "Tutor Three", "00333333"),
                student("S3", "Former Student", "00900003"));
        PeopleRegistryState peopleState = new PeopleRegistryState(people,
                Map.of(PersonRole.STUDENT, 20L, PersonRole.TUTOR, 10L, PersonRole.PARENT, 8L));
        Lesson first = lesson("L1", "T1", LessonDay.MONDAY, "0900", "1000", "Math", "R1",
                new LinkedHashSet<>(List.of(new PersonId("S1"), new PersonId("S2"))));
        Lesson second = lesson("L2", "T1", LessonDay.MONDAY, "1000", "1100", "Science", "R2",
                new LinkedHashSet<>(List.of(new PersonId("S1"))));
        Lesson third = lesson("L3", "T2", LessonDay.TUESDAY, "0900", "1000", "English", "R3",
                new LinkedHashSet<>(List.of(new PersonId("S2"))));
        Attendance currentAttendance = new Attendance(new PersonId("S1"), first.getId(),
                LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT);
        Attendance formerAttendance = new Attendance(new PersonId("S3"), first.getId(),
                LocalDate.of(2026, 10, 12), AttendanceStatus.ABSENT);
        return new PonHubDataState(peopleState, List.of(first, second, third),
                List.of(currentAttendance, formerAttendance), 30);
    }

    private static Lesson lesson(String id, String tutorId, LessonDay day, String startTime, String endTime,
            String subject, String room, Set<PersonId> students) {
        return new Lesson(new LessonId(id), new PersonId(tutorId),
                new LessonTimeSlot(day, new LessonTime(startTime), new LessonTime(endTime)),
                new Subject(subject), new Room(room), students);
    }

    private static Tutor tutor(String id, String name, String phone) {
        return new Tutor(new PersonId(id), new ContactDetails(new Name(name), Optional.of(new Phone(phone)),
                Optional.empty(), Optional.empty()));
    }

    private static Student student(String id, String name, String parentPhone) {
        return new Student(new PersonId(id), new ContactDetails(new Name(name)), new EducationLevel("P1"),
                new Phone(parentPhone));
    }
}
