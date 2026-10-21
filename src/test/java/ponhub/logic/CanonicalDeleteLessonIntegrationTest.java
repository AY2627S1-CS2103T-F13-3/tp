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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.collections.ListChangeListener;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.exceptions.ParseException;
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
import ponhub.model.query.LessonView;
import ponhub.storage.JsonPonHubDataStorage;
import ponhub.storage.ProtectedPonHubDataStorage;

/**
 * Exercises guarded shared-lesson deletion through parsing, staging, persistence and live query views.
 */
public class CanonicalDeleteLessonIntegrationTest {
    private static final PersonId TUTOR = new PersonId("T7");
    private static final PersonId STUDENT_ONE = new PersonId("S9");
    private static final PersonId STUDENT_TWO = new PersonId("S3");
    private static final LessonId LESSON = new LessonId("L10");
    private static final LocalDate LESSON_DATE = LocalDate.of(2026, 10, 5);

    @TempDir
    Path directory;

    @Test
    public void execute_unreferencedLesson_releasesScheduleRefreshesViewsAndNeverReusesId() throws Exception {
        Path path = savedData(Set.of(), List.of());
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("search c/student");
        session.execute("search c/lesson s/Mathematics");
        var people = session.getPeople();
        var queries = session.getLessonQueries();
        var catalogue = queries.getLessonList();
        var tutorSchedule = queries.getTutorSchedule(TUTOR);
        var displayed = session.getDisplayedLessons();

        assertEquals("Deleted lesson: L10.", session.execute("deletelesson lid/l10").getFeedbackToUser());
        assertSame(people, session.getPeople());
        assertSame(queries, session.getLessonQueries());
        assertSame(catalogue, queries.getLessonList());
        assertSame(tutorSchedule, queries.getTutorSchedule(TUTOR));
        assertSame(displayed, session.getDisplayedLessons());
        assertTrue(catalogue.isEmpty());
        assertTrue(tutorSchedule.isEmpty());
        assertTrue(displayed.isEmpty());
        assertEquals("Lesson search: s/mathematics", session.lessonResultContextProperty().get());
        assertEquals(10, session.getDataSnapshot().lastAllocatedLessonSequence());

        assertEquals("New lesson added: L11.", session.execute(
                "addlesson d/Mon st/0900 et/1000 s/Mathematics tu/Assigned Tutor rm/R1")
                .getFeedbackToUser());
        assertEquals(List.of(new LessonId("L11")), catalogue.stream().map(LessonView::getLessonId).toList());
        assertEquals(List.of(new LessonId("L11")), tutorSchedule.stream().map(LessonView::getLessonId).toList());
        assertEquals(List.of(new LessonId("L11")), displayed.stream().map(LessonView::getLessonId).toList());
        assertEquals(11, session.getDataSnapshot().lastAllocatedLessonSequence());
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
    }

    @Test
    public void execute_rosterThenRetainedAttendance_blockUntilExplicitlyCleared() throws Exception {
        Attendance attendance = new Attendance(STUDENT_ONE, LESSON, LESSON_DATE, AttendanceStatus.PRESENT);
        Set<PersonId> roster = new LinkedHashSet<>(List.of(STUDENT_ONE, STUDENT_TWO));
        Path path = savedData(roster, List.of(attendance));
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("list r/student");
        PonHubDataState original = session.getDataSnapshot();
        byte[] originalBytes = Files.readAllBytes(path);

        CommandException rosterFailure = assertThrows(CommandException.class, () ->
                session.execute("deletelesson lid/L10"));
        assertEquals("Lesson L10 cannot be deleted while its roster contains these Student IDs: S9, S3. "
                + "Unenrol all Students first.", rosterFailure.getMessage());
        assertSame(original, session.getDataSnapshot());
        assertArrayEquals(originalBytes, Files.readAllBytes(path));

        session.execute("unenrol 1 lid/L10");
        session.execute("unenrol 2 lid/L10");
        PonHubDataState historical = session.getDataSnapshot();
        byte[] historicalBytes = Files.readAllBytes(path);
        CommandException attendanceFailure = assertThrows(CommandException.class, () ->
                session.execute("deletelesson lid/L10"));
        assertEquals("Lesson L10 cannot be deleted while retained attendance exists for Student S9 on 2026-10-05. "
                + "Unmark all attendance for this Lesson first.", attendanceFailure.getMessage());
        assertSame(historical, session.getDataSnapshot());
        assertArrayEquals(historicalBytes, Files.readAllBytes(path));

        session.execute("unmark 1 lid/L10 d/2026-10-05");
        assertEquals("Deleted lesson: L10.", session.execute("deletelesson lid/L10").getFeedbackToUser());
        assertTrue(session.getDataSnapshot().lessons().isEmpty());
        assertTrue(session.getDataSnapshot().attendance().isEmpty());
        assertEquals(10, session.getDataSnapshot().lastAllocatedLessonSequence());
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
    }

    @Test
    public void execute_invalidOrMissingLesson_rejectsWithoutSaving() throws Exception {
        Path path = savedData(Set.of(), List.of());
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            throw new AssertionError("Rejected lesson deletion must not save");
        });
        PonHubDataState before = session.getDataSnapshot();
        byte[] original = Files.readAllBytes(path);
        for (String command : List.of("deletelesson", "deletelesson L10", "deletelesson 1",
                "deletelesson lid/", "deletelesson lid/L10 extra", "deletelesson lid/L10 lid/L11",
                "deletelesson lid/L0", "deletelesson LID/L10", "deletelesson lid/L10\n")) {
            assertThrows(ParseException.class, () -> session.execute(command));
        }
        CommandException missing = assertThrows(CommandException.class, () ->
                session.execute("deletelesson lid/L99"));
        assertEquals("The lesson does not exist: L99", missing.getMessage());
        assertSame(before, session.getDataSnapshot());
        assertArrayEquals(original, Files.readAllBytes(path));

        PonHubData data = new PonHubData(before);
        assertThrows(IllegalArgumentException.class, () -> data.removeLesson(new LessonId("L99")));
        assertThrows(NullPointerException.class, () -> data.removeLesson(null));
        assertSame(before, data.exportState());
    }

    @Test
    public void execute_failedSavePreservesStateCountersAndViewsBeforeSuccessfulRetry() throws Exception {
        Path path = savedData(Set.of(), List.of());
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        PonHubDataState before = storage.readData().orElseThrow();
        byte[] original = Files.readAllBytes(path);
        CanonicalPeopleSession[] current = new CanonicalPeopleSession[1];
        int[] writes = {0};
        current[0] = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), candidate -> {
            assertEquals(before, current[0].getDataSnapshot());
            if (writes[0]++ == 0) {
                throw new IOException("Injected lesson deletion save failure");
            }
            storage.saveData(candidate);
        });
        CanonicalPeopleSession session = current[0];
        PonHubDataState liveBefore = session.getDataSnapshot();
        session.execute("search c/student");
        session.execute("search c/lesson s/Mathematics");
        var people = session.getPeople();
        var catalogue = session.getLessonQueries().getLessonList();
        var tutorSchedule = session.getLessonQueries().getTutorSchedule(TUTOR);
        var displayed = session.getDisplayedLessons();
        int[] events = {0};
        catalogue.addListener((ListChangeListener<? super LessonView>) change -> events[0]++);

        CommandException failure = assertThrows(CommandException.class, () ->
                session.execute("deletelesson lid/L10"));
        assertTrue(failure.getMessage().contains("Injected lesson deletion save failure"));
        assertSame(liveBefore, session.getDataSnapshot());
        assertEquals(10, session.getDataSnapshot().lastAllocatedLessonSequence());
        assertArrayEquals(original, Files.readAllBytes(path));
        assertSame(people, session.getPeople());
        assertSame(catalogue, session.getLessonQueries().getLessonList());
        assertSame(tutorSchedule, session.getLessonQueries().getTutorSchedule(TUTOR));
        assertSame(displayed, session.getDisplayedLessons());
        assertEquals(List.of(LESSON), catalogue.stream().map(LessonView::getLessonId).toList());
        assertEquals(List.of(LESSON), displayed.stream().map(LessonView::getLessonId).toList());
        assertEquals(0, events[0]);

        assertEquals("Deleted lesson: L10.", session.execute("deletelesson lid/L10").getFeedbackToUser());
        assertEquals(2, writes[0]);
        assertTrue(events[0] > 0);
        assertTrue(catalogue.isEmpty());
        assertTrue(tutorSchedule.isEmpty());
        assertTrue(displayed.isEmpty());
        assertEquals(10, session.getDataSnapshot().lastAllocatedLessonSequence());
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
    }

    private Path savedData(Set<PersonId> roster, List<Attendance> attendance) throws IOException {
        Path path = directory.resolve("canonical-" + roster.size() + "-" + attendance.size() + ".json");
        List<PersonRecord> people = List.of(
                new Tutor(TUTOR, contacts("Assigned Tutor", "70000001")),
                new Student(STUDENT_ONE, new ContactDetails(new Name("Alice Student")),
                        new EducationLevel("P1"), new Phone("60000001")),
                new Student(STUDENT_TWO, new ContactDetails(new Name("Bob Student")),
                        new EducationLevel("P2"), new Phone("60000002")));
        PeopleRegistryState registry = new PeopleRegistryState(people,
                Map.of(PersonRole.STUDENT, 9L, PersonRole.TUTOR, 7L, PersonRole.PARENT, 0L));
        Lesson lesson = new Lesson(LESSON, TUTOR,
                new LessonTimeSlot(LessonDay.MONDAY, new LessonTime("0900"), new LessonTime("1000")),
                new Subject("Mathematics"), new Room("R1"), roster);
        new JsonPonHubDataStorage(path).saveData(new PonHubDataState(registry, List.of(lesson), attendance, 10));
        return path;
    }

    private static ContactDetails contacts(String name, String phone) {
        return new ContactDetails(new Name(name), Optional.of(new Phone(phone)), Optional.empty(), Optional.empty());
    }
}
