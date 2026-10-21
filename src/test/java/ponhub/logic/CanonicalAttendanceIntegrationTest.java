package ponhub.logic;

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

import javafx.collections.ListChangeListener;
import ponhub.logic.commands.exceptions.CommandException;
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
import ponhub.model.person.Parent;
import ponhub.model.person.PeopleRegistryState;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.PersonRole;
import ponhub.model.person.Phone;
import ponhub.model.person.Student;
import ponhub.model.person.Tutor;
import ponhub.storage.JsonPonHubDataStorage;
import ponhub.storage.ProtectedPonHubDataStorage;

/**
 * Exercises attendance mutations through parsing, current-view selection, staging and persistence.
 */
public class CanonicalAttendanceIntegrationTest {
    private static final PersonId STUDENT_ONE = new PersonId("S9");
    private static final PersonId STUDENT_TWO = new PersonId("S3");
    private static final PersonId TUTOR = new PersonId("T7");
    private static final LessonId LESSON = new LessonId("L10");
    private static final LocalDate FIRST_MONDAY = LocalDate.of(2026, 10, 5);
    private static final LocalDate SECOND_MONDAY = LocalDate.of(2026, 10, 12);

    @TempDir
    Path directory;

    @Test
    public void execute_markCreatesCorrectsAndSkipsIdenticalStatus() throws Exception {
        Path path = directory.resolve("canonical.json");
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        PonHubDataState before = attendanceState(Set.of(STUDENT_ONE, STUDENT_TWO), List.of());
        storage.saveData(before);
        int[] writes = {0};
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            writes[0]++;
            storage.saveData(state);
        });
        session.execute("search c/student");
        var visiblePeople = session.getPeople();
        var queries = session.getLessonQueries();
        var lessonList = queries.getLessonList();
        var roster = queries.getLessonRoster(LESSON);
        var studentLessons = queries.getStudentLessons(STUDENT_TWO);
        int[] lessonEvents = {0};
        lessonList.addListener((ListChangeListener<? super ponhub.model.query.LessonView>) change ->
                lessonEvents[0]++);

        assertEquals("Attendance for Student S3 in lesson L10 on 2026-10-05 marked present.",
                session.execute("mark 2 lid/L10 d/2026-10-05 s/present").getFeedbackToUser());
        assertEquals(1, writes[0]);
        assertTrue(lessonEvents[0] > 0);
        assertEquals(List.of(attendance(STUDENT_TWO, FIRST_MONDAY, AttendanceStatus.PRESENT)),
                session.getDataSnapshot().attendance());
        int eventsAfterCreation = lessonEvents[0];
        PonHubDataState created = session.getDataSnapshot();
        byte[] createdBytes = Files.readAllBytes(path);

        assertEquals("Attendance for Student S3 in lesson L10 on 2026-10-05 is already marked present. "
                        + "No changes made.",
                session.execute("mark 2 s/present d/2026-10-05 lid/L10").getFeedbackToUser());
        assertEquals(1, writes[0]);
        assertEquals(eventsAfterCreation, lessonEvents[0]);
        assertSame(created, session.getDataSnapshot());
        assertArrayEquals(createdBytes, Files.readAllBytes(path));

        assertEquals("Attendance for Student S3 in lesson L10 on 2026-10-05 marked absent.",
                session.execute("mark 2 lid/L10 d/2026-10-05 s/absent").getFeedbackToUser());
        assertEquals("Attendance for Student S3 in lesson L10 on 2026-10-12 marked present.",
                session.execute("mark 2 lid/L10 d/2026-10-12 s/present").getFeedbackToUser());
        assertEquals("Attendance for Student S9 in lesson L10 on 2026-10-05 marked absent.",
                session.execute("mark 1 lid/L10 d/2026-10-05 s/absent").getFeedbackToUser());
        assertEquals(4, writes[0]);
        assertEquals(List.of(
                attendance(STUDENT_TWO, FIRST_MONDAY, AttendanceStatus.ABSENT),
                attendance(STUDENT_TWO, SECOND_MONDAY, AttendanceStatus.PRESENT),
                attendance(STUDENT_ONE, FIRST_MONDAY, AttendanceStatus.ABSENT)),
                session.getDataSnapshot().attendance());
        assertSame(visiblePeople, session.getPeople());
        assertSame(queries, session.getLessonQueries());
        assertSame(lessonList, queries.getLessonList());
        assertSame(roster, queries.getLessonRoster(LESSON));
        assertSame(studentLessons, queries.getStudentLessons(STUDENT_TWO));
        assertEquals(List.of(STUDENT_ONE, STUDENT_TWO), visibleIds(session));
        assertEquals(Optional.of(PersonRole.STUDENT), session.getRoleFilter());
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
    }

    @Test
    public void execute_markHistoricalCorrectionAllowedButNewHistoryRequiresMembership() throws Exception {
        Path path = directory.resolve("canonical.json");
        Attendance historical = attendance(STUDENT_ONE, FIRST_MONDAY, AttendanceStatus.PRESENT);
        PonHubDataState before = attendanceState(Set.of(), List.of(historical));
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        storage.saveData(before);
        int[] writes = {0};
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            writes[0]++;
            storage.saveData(state);
        });
        session.execute("list r/student");

        assertEquals("Attendance for Student S9 in lesson L10 on 2026-10-05 marked absent.",
                session.execute("mark 1 lid/L10 d/2026-10-05 s/absent").getFeedbackToUser());
        assertEquals(List.of(attendance(STUDENT_ONE, FIRST_MONDAY, AttendanceStatus.ABSENT)),
                session.getDataSnapshot().attendance());
        PonHubDataState corrected = session.getDataSnapshot();
        byte[] correctedBytes = Files.readAllBytes(path);

        CommandException notEnrolled = assertThrows(CommandException.class, () -> session.execute(
                "mark 1 lid/L10 d/2026-10-12 s/present"));
        assertEquals("Student S9 must be enrolled in lesson L10 before new attendance can be recorded.",
                notEnrolled.getMessage());
        assertEquals(1, writes[0]);
        assertSame(corrected, session.getDataSnapshot());
        assertArrayEquals(correctedBytes, Files.readAllBytes(path));
    }

    @Test
    public void execute_markInvalidSelectionLessonOrWeekday_rejectsWithoutSaving() throws Exception {
        Path path = directory.resolve("canonical.json");
        PonHubDataState before = attendanceState(Set.of(STUDENT_TWO), List.of());
        new JsonPonHubDataStorage(path).saveData(before);
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            throw new AssertionError("Rejected attendance commands must not save");
        });
        byte[] original = Files.readAllBytes(path);

        CommandException wrongRole = assertThrows(CommandException.class, () -> session.execute(
                "mark 1 lid/L10 d/2026-10-05 s/present"));
        assertEquals(PersonIndexResolver.MESSAGE_NOT_STUDENT, wrongRole.getMessage());
        CommandException outsideView = assertThrows(CommandException.class, () -> session.execute(
                "mark 5 lid/L10 d/2026-10-05 s/present"));
        assertEquals(Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX, outsideView.getMessage());
        CommandException missingLesson = assertThrows(CommandException.class, () -> session.execute(
                "mark 4 lid/L99 d/2026-10-05 s/present"));
        assertEquals("The lesson does not exist: L99", missingLesson.getMessage());
        CommandException wrongWeekday = assertThrows(CommandException.class, () -> session.execute(
                "mark 4 lid/L10 d/2026-10-06 s/present"));
        assertEquals("Attendance date 2026-10-06 does not match lesson L10's Mon schedule.",
                wrongWeekday.getMessage());

        assertEquals(before, session.getDataSnapshot());
        assertArrayEquals(original, Files.readAllBytes(path));
    }

    @Test
    public void execute_markSaveFailureRestoresStateViewsAndRetryCommits() throws Exception {
        Path path = directory.resolve("canonical.json");
        PonHubDataState before = attendanceState(Set.of(STUDENT_TWO), List.of());
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        storage.saveData(before);
        byte[] original = Files.readAllBytes(path);
        CanonicalPeopleSession[] current = new CanonicalPeopleSession[1];
        int[] writes = {0};
        current[0] = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            assertEquals(before, current[0].getDataSnapshot());
            if (writes[0]++ == 0) {
                throw new IOException("Injected attendance save failure");
            }
            storage.saveData(state);
        });
        CanonicalPeopleSession session = current[0];
        session.execute("search c/student");
        var visiblePeople = session.getPeople();
        var lessonList = session.getLessonQueries().getLessonList();
        var roster = session.getLessonQueries().getLessonRoster(LESSON);
        int[] lessonEvents = {0};
        lessonList.addListener((ListChangeListener<? super ponhub.model.query.LessonView>) change ->
                lessonEvents[0]++);

        CommandException failedSave = assertThrows(CommandException.class, () -> session.execute(
                "mark 2 lid/L10 d/2026-10-05 s/present"));
        assertTrue(failedSave.getMessage().contains("Injected attendance save failure"));
        assertEquals(before, session.getDataSnapshot());
        assertArrayEquals(original, Files.readAllBytes(path));
        assertSame(visiblePeople, session.getPeople());
        assertSame(lessonList, session.getLessonQueries().getLessonList());
        assertSame(roster, session.getLessonQueries().getLessonRoster(LESSON));
        assertEquals(List.of(STUDENT_ONE, STUDENT_TWO), visibleIds(session));
        assertEquals(Optional.of(PersonRole.STUDENT), session.getRoleFilter());
        assertEquals(0, lessonEvents[0]);

        assertEquals("Attendance for Student S3 in lesson L10 on 2026-10-05 marked present.",
                session.execute("mark 2 lid/L10 d/2026-10-05 s/present").getFeedbackToUser());
        assertEquals(2, writes[0]);
        assertTrue(lessonEvents[0] > 0);
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
    }

    @Test
    public void execute_unmarkRemovesOnlySelectedKeyAndMissingKeyIsNoOp() throws Exception {
        Path path = directory.resolve("canonical.json");
        Attendance selected = attendance(STUDENT_TWO, FIRST_MONDAY, AttendanceStatus.PRESENT);
        Attendance otherDate = attendance(STUDENT_TWO, SECOND_MONDAY, AttendanceStatus.ABSENT);
        Attendance otherStudent = attendance(STUDENT_ONE, FIRST_MONDAY, AttendanceStatus.ABSENT);
        PonHubDataState before = attendanceState(Set.of(STUDENT_ONE, STUDENT_TWO),
                List.of(selected, otherDate, otherStudent));
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        storage.saveData(before);
        int[] writes = {0};
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            writes[0]++;
            storage.saveData(state);
        });
        session.execute("search c/student");
        var visiblePeople = session.getPeople();
        var queries = session.getLessonQueries();
        var lessonList = queries.getLessonList();
        var roster = queries.getLessonRoster(LESSON);
        var studentLessons = queries.getStudentLessons(STUDENT_TWO);
        int[] lessonEvents = {0};
        lessonList.addListener((ListChangeListener<? super ponhub.model.query.LessonView>) change ->
                lessonEvents[0]++);

        assertEquals("Attendance for Student S3 in lesson L10 on 2026-10-05 removed.",
                session.execute("unmark 2 lid/L10 d/2026-10-05").getFeedbackToUser());
        assertEquals(1, writes[0]);
        assertTrue(lessonEvents[0] > 0);
        assertEquals(List.of(otherDate, otherStudent), session.getDataSnapshot().attendance());
        assertEquals(before.lessons(), session.getDataSnapshot().lessons());
        assertSame(visiblePeople, session.getPeople());
        assertSame(queries, session.getLessonQueries());
        assertSame(lessonList, queries.getLessonList());
        assertSame(roster, queries.getLessonRoster(LESSON));
        assertSame(studentLessons, queries.getStudentLessons(STUDENT_TWO));
        assertEquals(List.of(STUDENT_ONE, STUDENT_TWO), visibleIds(session));
        assertEquals(Optional.of(PersonRole.STUDENT), session.getRoleFilter());
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());

        int eventsAfterRemoval = lessonEvents[0];
        PonHubDataState removed = session.getDataSnapshot();
        byte[] removedBytes = Files.readAllBytes(path);
        assertEquals("No attendance record exists for Student S3 in lesson L10 on 2026-10-05. "
                        + "No changes made.",
                session.execute("unmark 2 d/2026-10-05 lid/L10").getFeedbackToUser());
        assertEquals(1, writes[0]);
        assertEquals(eventsAfterRemoval, lessonEvents[0]);
        assertSame(removed, session.getDataSnapshot());
        assertArrayEquals(removedBytes, Files.readAllBytes(path));
        assertSame(visiblePeople, session.getPeople());
        assertEquals(List.of(STUDENT_ONE, STUDENT_TWO), visibleIds(session));
    }

    @Test
    public void execute_unmarkHistoricalAttendanceAllowedAfterUnenrolment() throws Exception {
        Path path = directory.resolve("canonical.json");
        Attendance historical = attendance(STUDENT_ONE, FIRST_MONDAY, AttendanceStatus.PRESENT);
        PonHubDataState before = attendanceState(Set.of(), List.of(historical));
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        storage.saveData(before);
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path),
                storage::saveData);
        session.execute("list r/student");

        assertEquals("Attendance for Student S9 in lesson L10 on 2026-10-05 removed.",
                session.execute("unmark 1 lid/L10 d/2026-10-05").getFeedbackToUser());
        assertTrue(session.getDataSnapshot().attendance().isEmpty());
        assertEquals(before.lessons(), session.getDataSnapshot().lessons());
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
    }

    @Test
    public void execute_unmarkInvalidSelectionLessonOrWeekday_rejectsWithoutSaving() throws Exception {
        Path path = directory.resolve("canonical.json");
        Attendance attendance = attendance(STUDENT_TWO, FIRST_MONDAY, AttendanceStatus.PRESENT);
        PonHubDataState before = attendanceState(Set.of(STUDENT_TWO), List.of(attendance));
        new JsonPonHubDataStorage(path).saveData(before);
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            throw new AssertionError("Rejected attendance commands must not save");
        });
        byte[] original = Files.readAllBytes(path);

        CommandException wrongRole = assertThrows(CommandException.class, () -> session.execute(
                "unmark 1 lid/L10 d/2026-10-05"));
        assertEquals(PersonIndexResolver.MESSAGE_NOT_STUDENT, wrongRole.getMessage());
        CommandException outsideView = assertThrows(CommandException.class, () -> session.execute(
                "unmark 5 lid/L10 d/2026-10-05"));
        assertEquals(Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX, outsideView.getMessage());
        CommandException missingLesson = assertThrows(CommandException.class, () -> session.execute(
                "unmark 4 lid/L99 d/2026-10-05"));
        assertEquals("The lesson does not exist: L99", missingLesson.getMessage());
        CommandException wrongWeekday = assertThrows(CommandException.class, () -> session.execute(
                "unmark 4 lid/L10 d/2026-10-06"));
        assertEquals("Attendance date 2026-10-06 does not match lesson L10's Mon schedule.",
                wrongWeekday.getMessage());

        assertEquals(before, session.getDataSnapshot());
        assertArrayEquals(original, Files.readAllBytes(path));
    }

    @Test
    public void execute_unmarkSaveFailureRestoresStateViewsAndRetryCommits() throws Exception {
        Path path = directory.resolve("canonical.json");
        Attendance selected = attendance(STUDENT_TWO, FIRST_MONDAY, AttendanceStatus.PRESENT);
        PonHubDataState before = attendanceState(Set.of(STUDENT_TWO), List.of(selected));
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        storage.saveData(before);
        byte[] original = Files.readAllBytes(path);
        CanonicalPeopleSession[] current = new CanonicalPeopleSession[1];
        int[] writes = {0};
        current[0] = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            assertEquals(before, current[0].getDataSnapshot());
            if (writes[0]++ == 0) {
                throw new IOException("Injected attendance removal save failure");
            }
            storage.saveData(state);
        });
        CanonicalPeopleSession session = current[0];
        session.execute("search c/student");
        var visiblePeople = session.getPeople();
        var lessonList = session.getLessonQueries().getLessonList();
        var roster = session.getLessonQueries().getLessonRoster(LESSON);
        int[] lessonEvents = {0};
        lessonList.addListener((ListChangeListener<? super ponhub.model.query.LessonView>) change ->
                lessonEvents[0]++);

        CommandException failedSave = assertThrows(CommandException.class, () -> session.execute(
                "unmark 2 lid/L10 d/2026-10-05"));
        assertTrue(failedSave.getMessage().contains("Injected attendance removal save failure"));
        assertEquals(before, session.getDataSnapshot());
        assertArrayEquals(original, Files.readAllBytes(path));
        assertSame(visiblePeople, session.getPeople());
        assertSame(lessonList, session.getLessonQueries().getLessonList());
        assertSame(roster, session.getLessonQueries().getLessonRoster(LESSON));
        assertEquals(List.of(STUDENT_ONE, STUDENT_TWO), visibleIds(session));
        assertEquals(Optional.of(PersonRole.STUDENT), session.getRoleFilter());
        assertEquals(0, lessonEvents[0]);

        assertEquals("Attendance for Student S3 in lesson L10 on 2026-10-05 removed.",
                session.execute("unmark 2 lid/L10 d/2026-10-05").getFeedbackToUser());
        assertEquals(2, writes[0]);
        assertTrue(lessonEvents[0] > 0);
        assertTrue(session.getDataSnapshot().attendance().isEmpty());
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
    }

    private static List<PersonId> visibleIds(CanonicalPeopleSession session) {
        return session.getPeople().stream().map(PersonRecord::getId).toList();
    }

    private static Attendance attendance(PersonId studentId, LocalDate date, AttendanceStatus status) {
        return new Attendance(studentId, LESSON, date, status);
    }

    private static PonHubDataState attendanceState(Set<PersonId> roster, List<Attendance> attendance) {
        Tutor tutor = new Tutor(TUTOR, contacts("Tutor Tan", "70000001"));
        Student firstStudent = new Student(STUDENT_ONE, new ContactDetails(new Name("Alice Student")),
                new EducationLevel("P1"), new Phone("60000001"));
        Parent parent = new Parent(new PersonId("P2"), contacts("Parent Tan", "60000002"));
        Student secondStudent = new Student(STUDENT_TWO, new ContactDetails(new Name("Bob Student")),
                new EducationLevel("P2"), new Phone("60000002"));
        PeopleRegistryState people = new PeopleRegistryState(List.of(tutor, firstStudent, parent, secondStudent),
                Map.of(PersonRole.STUDENT, 9L, PersonRole.TUTOR, 7L, PersonRole.PARENT, 2L));
        Lesson lesson = new Lesson(LESSON, TUTOR,
                new LessonTimeSlot(LessonDay.MONDAY, new LessonTime("0900"), new LessonTime("1000")),
                new Subject("Mathematics"), new Room("R1"), roster);
        return new PonHubDataState(people, List.of(lesson), attendance, 10);
    }

    private static ContactDetails contacts(String name, String phone) {
        return new ContactDetails(new Name(name), Optional.of(new Phone(phone)), Optional.empty(), Optional.empty());
    }
}
