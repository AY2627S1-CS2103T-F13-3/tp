package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.PonHubDataState;
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
import seedu.address.model.person.PeopleRegistryState;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Student;
import seedu.address.model.person.Tutor;
import seedu.address.model.query.LessonView;
import seedu.address.model.query.StudentView;
import seedu.address.storage.JsonPonHubDataStorage;
import seedu.address.storage.ProtectedPonHubDataStorage;

/** Exercises shared-lesson membership commands through parsing, staging, persistence and live queries. */
public class CanonicalLessonMembershipIntegrationTest {
    private static final PersonId STUDENT_ONE = new PersonId("S9");
    private static final PersonId STUDENT_TWO = new PersonId("S3");
    private static final PersonId TUTOR_ONE = new PersonId("T7");
    private static final PersonId TUTOR_TWO = new PersonId("T8");
    private static final LessonId TARGET_LESSON = new LessonId("L10");
    private static final LessonId EXISTING_LESSON = new LessonId("L11");
    private static final LessonId ADJACENT_LESSON = new LessonId("L12");
    private static final LessonId DIFFERENT_DAY_LESSON = new LessonId("L13");

    @TempDir
    Path directory;

    @Test
    public void execute_enrolUsesCurrentIndexAndRefreshesCanonicalQueries() throws Exception {
        Path path = directory.resolve("canonical.json");
        PonHubDataState before = membershipState(Set.of(), List.of());
        new JsonPonHubDataStorage(path).saveData(before);
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        assertEquals("Found 2 student(s).", session.execute("search c/student").getFeedbackToUser());
        var visiblePeople = session.getPeople();
        var queries = session.getLessonQueries();
        var lessonList = queries.getLessonList();
        var roster = queries.getLessonRoster(TARGET_LESSON);
        var studentLessons = queries.getStudentLessons(STUDENT_TWO);
        Lesson originalLesson = before.lessons().getFirst();

        assertEquals("Student S3 enrolled in lesson L10.",
                session.execute("enrol 2 lid/l10").getFeedbackToUser());

        PonHubDataState committed = session.getDataSnapshot();
        Lesson updatedLesson = committed.lessons().getFirst();
        assertEquals(4, committed.lessons().size());
        assertEquals(originalLesson.getId(), updatedLesson.getId());
        assertEquals(originalLesson.getTutorId(), updatedLesson.getTutorId());
        assertEquals(originalLesson.getTimeSlot(), updatedLesson.getTimeSlot());
        assertEquals(originalLesson.getSubject(), updatedLesson.getSubject());
        assertEquals(originalLesson.getRoom(), updatedLesson.getRoom());
        assertEquals(Set.of(STUDENT_TWO), updatedLesson.getEnrolledStudentIds());
        assertSame(visiblePeople, session.getPeople());
        assertSame(queries, session.getLessonQueries());
        assertSame(lessonList, queries.getLessonList());
        assertSame(roster, queries.getLessonRoster(TARGET_LESSON));
        assertSame(studentLessons, queries.getStudentLessons(STUDENT_TWO));
        assertEquals(List.of(STUDENT_ONE, STUDENT_TWO), visibleIds(session));
        assertEquals(Optional.of(PersonRole.STUDENT), session.getRoleFilter());
        assertEquals(List.of(STUDENT_TWO), roster.stream().map(StudentView::getStudentId).toList());
        assertEquals(List.of(TARGET_LESSON), studentLessons.stream().map(LessonView::getLessonId).toList());
        assertEquals(committed, new CanonicalPeopleSession(path).getDataSnapshot());

        byte[] committedBytes = Files.readAllBytes(path);
        CommandException duplicate = assertThrows(CommandException.class, () -> session.execute(
                "enrol 2 lid/L10"));
        assertEquals("Student S3 is already enrolled in lesson L10.", duplicate.getMessage());
        assertEquals(committed, session.getDataSnapshot());
        assertArrayEquals(committedBytes, Files.readAllBytes(path));
    }

    @Test
    public void execute_enrolRejectsInvalidSelectionAndClashButAllowsAdjacentSlots() throws Exception {
        Path path = directory.resolve("canonical.json");
        PonHubDataState before = membershipState(Set.of(), List.of());
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        storage.saveData(before);
        int[] writes = {0};
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            writes[0]++;
            storage.saveData(state);
        });

        CommandException wrongRole = assertThrows(CommandException.class, () -> session.execute(
                "enrol 1 lid/L10"));
        assertEquals(PersonIndexResolver.MESSAGE_NOT_STUDENT, wrongRole.getMessage());
        CommandException outsideView = assertThrows(CommandException.class, () -> session.execute(
                "enrol 6 lid/L10"));
        assertEquals(Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX, outsideView.getMessage());
        CommandException missingLesson = assertThrows(CommandException.class, () -> session.execute(
                "enrol 2 lid/L99"));
        assertEquals("The lesson does not exist: L99", missingLesson.getMessage());
        CommandException clash = assertThrows(CommandException.class, () -> session.execute(
                "enrol 2 lid/L10"));
        assertEquals("Student S9 schedule overlap between L10 and L11.", clash.getMessage());
        assertEquals(0, writes[0]);
        assertEquals(before, session.getDataSnapshot());

        assertEquals("Student S9 enrolled in lesson L12.",
                session.execute("enrol 2 lid/L12").getFeedbackToUser());
        assertEquals("Student S9 enrolled in lesson L13.",
                session.execute("enrol 2 lid/L13").getFeedbackToUser());
        assertEquals(2, writes[0]);
        assertEquals(Set.of(STUDENT_ONE), session.getDataSnapshot().lessons().get(2).getEnrolledStudentIds());
        assertEquals(Set.of(STUDENT_ONE), session.getDataSnapshot().lessons().get(3).getEnrolledStudentIds());
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
    }

    @Test
    public void execute_unenrolRetainsAttendanceAndRejectsMissingMembership() throws Exception {
        Path path = directory.resolve("canonical.json");
        Attendance historicalAttendance = new Attendance(STUDENT_TWO, TARGET_LESSON,
                LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT);
        PonHubDataState before = membershipState(Set.of(STUDENT_TWO), List.of(historicalAttendance));
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        storage.saveData(before);
        int[] writes = {0};
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            writes[0]++;
            storage.saveData(state);
        });
        session.execute("search c/student");
        var roster = session.getLessonQueries().getLessonRoster(TARGET_LESSON);
        var studentLessons = session.getLessonQueries().getStudentLessons(STUDENT_TWO);

        assertEquals("Student S3 unenrolled from lesson L10.",
                session.execute("unenrol 2 lid/L10").getFeedbackToUser());

        PonHubDataState committed = session.getDataSnapshot();
        assertTrue(committed.lessons().getFirst().getEnrolledStudentIds().isEmpty());
        assertEquals(before.attendance(), committed.attendance());
        assertEquals(before.people(), committed.people());
        assertEquals(before.lastAllocatedLessonSequence(), committed.lastAllocatedLessonSequence());
        assertEquals(before.lessons().subList(1, 4), committed.lessons().subList(1, 4));
        assertSame(roster, session.getLessonQueries().getLessonRoster(TARGET_LESSON));
        assertSame(studentLessons, session.getLessonQueries().getStudentLessons(STUDENT_TWO));
        assertTrue(roster.isEmpty());
        assertTrue(studentLessons.isEmpty());
        assertEquals(1, writes[0]);
        assertEquals(committed, new CanonicalPeopleSession(path).getDataSnapshot());

        byte[] committedBytes = Files.readAllBytes(path);
        CommandException missingMembership = assertThrows(CommandException.class, () -> session.execute(
                "unenrol 2 lid/L10"));
        assertEquals("Student S3 is not enrolled in lesson L10.", missingMembership.getMessage());
        assertEquals(1, writes[0]);
        assertEquals(committed, session.getDataSnapshot());
        assertArrayEquals(committedBytes, Files.readAllBytes(path));
    }

    @Test
    public void execute_enrolSaveFailureRestoresAllStateAndRetryCommitsOnce() throws Exception {
        Path path = directory.resolve("canonical.json");
        PonHubDataState before = membershipState(Set.of(), List.of());
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        storage.saveData(before);
        byte[] original = Files.readAllBytes(path);
        CanonicalPeopleSession[] current = new CanonicalPeopleSession[1];
        int[] writes = {0};
        current[0] = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            assertEquals(before, current[0].getDataSnapshot());
            if (writes[0]++ == 0) {
                throw new IOException("Injected membership save failure");
            }
            storage.saveData(state);
        });
        CanonicalPeopleSession session = current[0];
        session.execute("search c/student");
        var visiblePeople = session.getPeople();
        var lessonList = session.getLessonQueries().getLessonList();
        var roster = session.getLessonQueries().getLessonRoster(TARGET_LESSON);
        var studentLessons = session.getLessonQueries().getStudentLessons(STUDENT_TWO);

        CommandException failedSave = assertThrows(CommandException.class, () -> session.execute(
                "enrol 2 lid/L10"));
        assertTrue(failedSave.getMessage().contains("Injected membership save failure"));
        assertEquals(before, session.getDataSnapshot());
        assertArrayEquals(original, Files.readAllBytes(path));
        assertSame(visiblePeople, session.getPeople());
        assertSame(lessonList, session.getLessonQueries().getLessonList());
        assertSame(roster, session.getLessonQueries().getLessonRoster(TARGET_LESSON));
        assertSame(studentLessons, session.getLessonQueries().getStudentLessons(STUDENT_TWO));
        assertEquals(List.of(STUDENT_ONE, STUDENT_TWO), visibleIds(session));
        assertEquals(Optional.of(PersonRole.STUDENT), session.getRoleFilter());
        assertTrue(roster.isEmpty());
        assertTrue(studentLessons.isEmpty());

        assertEquals("Student S3 enrolled in lesson L10.",
                session.execute("enrol 2 lid/L10").getFeedbackToUser());
        assertEquals(2, writes[0]);
        assertEquals(List.of(STUDENT_TWO), roster.stream().map(StudentView::getStudentId).toList());
        assertEquals(List.of(TARGET_LESSON), studentLessons.stream().map(LessonView::getLessonId).toList());
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
    }

    private static List<PersonId> visibleIds(CanonicalPeopleSession session) {
        return session.getPeople().stream().map(PersonRecord::getId).toList();
    }

    private static PonHubDataState membershipState(Set<PersonId> targetRoster, List<Attendance> attendance) {
        Tutor firstTutor = new Tutor(TUTOR_ONE, contacts("Tutor One", "70000001"));
        Student firstStudent = new Student(STUDENT_ONE, new ContactDetails(new Name("Alice Student")),
                new EducationLevel("P1"), new Phone("60000001"));
        Parent parent = new Parent(new PersonId("P2"), contacts("Parent Two", "60000002"));
        Student secondStudent = new Student(STUDENT_TWO, new ContactDetails(new Name("Bob Student")),
                new EducationLevel("P2"), new Phone("60000002"));
        Tutor secondTutor = new Tutor(TUTOR_TWO, contacts("Tutor Two", "70000002"));
        PeopleRegistryState people = new PeopleRegistryState(
                List.of(firstTutor, firstStudent, parent, secondStudent, secondTutor),
                Map.of(PersonRole.STUDENT, 9L, PersonRole.TUTOR, 8L, PersonRole.PARENT, 2L));
        List<Lesson> lessons = List.of(
                lesson(TARGET_LESSON, TUTOR_ONE, LessonDay.MONDAY, "0900", "1000", "R1", targetRoster),
                lesson(EXISTING_LESSON, TUTOR_TWO, LessonDay.MONDAY, "0930", "1030", "R2",
                        Set.of(STUDENT_ONE)),
                lesson(ADJACENT_LESSON, TUTOR_TWO, LessonDay.MONDAY, "1030", "1130", "R2", Set.of()),
                lesson(DIFFERENT_DAY_LESSON, TUTOR_TWO, LessonDay.TUESDAY, "0930", "1030", "R2", Set.of()));
        return new PonHubDataState(people, lessons, attendance, 13);
    }

    private static Lesson lesson(LessonId id, PersonId tutorId, LessonDay day, String start, String end,
            String room, Set<PersonId> roster) {
        return new Lesson(id, tutorId, new LessonTimeSlot(day, new LessonTime(start), new LessonTime(end)),
                new Subject("Mathematics"), new Room(room), roster);
    }

    private static ContactDetails contacts(String name, String phone) {
        return new ContactDetails(new Name(name), Optional.of(new Phone(phone)), Optional.empty(), Optional.empty());
    }
}
