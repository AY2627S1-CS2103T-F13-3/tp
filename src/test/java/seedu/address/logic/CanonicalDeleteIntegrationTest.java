package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.collections.ListChangeListener;
import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.ParserUtil;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.PonHubData;
import seedu.address.model.PonHubDataState;
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
import seedu.address.storage.JsonPonHubDataStorage;
import seedu.address.storage.ProtectedPonHubDataStorage;

/**
 * Exercises guarded deletion through current selectors, live command routes, transactions and file reload.
 */
public class CanonicalDeleteIntegrationTest {
    private static final PersonId FIRST_STUDENT = new PersonId("S9");
    private static final PersonId SECOND_STUDENT = new PersonId("S3");
    private static final LessonId LESSON = new LessonId("L10");

    @TempDir
    Path directory;

    @Test
    public void execute_searchFilteredStudent_preservesFilterCountersAndReload() throws Exception {
        Path path = savedData(Set.of(FIRST_STUDENT));
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("search c/student n/Student");
        var visible = session.getPeople();
        var emptyLessons = session.getLessonQueries().getStudentLessons(SECOND_STUDENT);
        var roster = session.getLessonQueries().getLessonRoster(LESSON);
        var counters = session.getDataSnapshot().people().getLastAllocatedSequences();

        assertEquals("Deleted student: Bob Student (S3).", session.execute("delete 2").getFeedbackToUser());
        assertSame(visible, session.getPeople());
        assertEquals(List.of(FIRST_STUDENT), visibleIds(session));
        assertEquals(Optional.of(PersonRole.STUDENT), session.getRoleFilter());
        assertTrue(emptyLessons.isEmpty());
        assertSame(roster, session.getLessonQueries().getLessonRoster(LESSON));
        assertEquals(counters, session.getDataSnapshot().people().getLastAllocatedSequences());

        CanonicalPeopleSession restarted = new CanonicalPeopleSession(path);
        assertEquals(session.getDataSnapshot(), restarted.getDataSnapshot());
        assertTrue(restarted.execute("add r/student n/New Student l/P2 pp/60000003")
                .getFeedbackToUser().contains("S10"));
    }

    @Test
    public void execute_deletedStudentWithSelectedLessons_keepsStableEmptySubscription() throws Exception {
        Path path = savedData(Set.of(FIRST_STUDENT));
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("lessons si/4");
        var displayed = session.getDisplayedLessons();
        assertTrue(displayed.isEmpty());
        session.execute("delete 4");
        session.execute("add r/student n/New Student l/P2 pp/60000003");
        session.execute("list r/student");
        assertSame(displayed, session.getDisplayedLessons());
        assertTrue(displayed.isEmpty());
        assertEquals("Student S3 · current lessons", session.lessonResultContextProperty().get());
        assertEquals(List.of(FIRST_STUDENT, new PersonId("S10")), visibleIds(session));

        session.execute("lessons si/1");
        assertSame(displayed, session.getDisplayedLessons());
        assertEquals(List.of(LESSON), displayed.stream().map(lesson -> lesson.getLessonId()).toList());
        assertEquals("Student S9 · current lessons", session.lessonResultContextProperty().get());
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
    }

    @Test
    public void execute_parentsAndUnassignedTutor_preservesStudentPhonesAndRenumbers() throws Exception {
        Path path = savedData(Set.of(FIRST_STUDENT));
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("list r/parent");
        assertEquals("Deleted parent: Pat Parent (P2).", session.execute("delete 1").getFeedbackToUser());
        assertEquals(List.of(new PersonId("P5")), visibleIds(session));
        assertEquals(Optional.of(PersonRole.PARENT), session.getRoleFilter());
        assertEquals("Deleted parent: Other Parent (P5).", session.execute("delete 1").getFeedbackToUser());
        assertTrue(session.getPeople().isEmpty());
        for (PersonRecord person : session.getDataSnapshot().people().getPeople()) {
            if (person instanceof Student student) {
                assertEquals(new Phone("60000001"), student.getParentPhone());
            }
        }
        session.execute("list r/tutor");
        assertEquals("Deleted tutor: Free Tutor (T2).", session.execute("delete 2").getFeedbackToUser());
        assertEquals(List.of(new PersonId("T7")), visibleIds(session));
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
        assertTrue(session.execute("add r/parent n/New Parent p/60000003").getFeedbackToUser().contains("P6"));
        assertTrue(session.execute("add r/tutor n/New Tutor p/70000003").getFeedbackToUser().contains("T8"));
    }

    @Test
    public void execute_enrolledStudentAndEmptyLessonTutor_rejectsUntilDependencyRemoved() throws Exception {
        Path path = savedData(Set.of(FIRST_STUDENT));
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("lessons si/2");
        var displayed = session.getDisplayedLessons();
        PonHubDataState before = session.getDataSnapshot();
        byte[] original = Files.readAllBytes(path);
        CommandException studentFailure = assertThrows(CommandException.class, () -> session.execute("delete 2"));
        assertTrue(studentFailure.getMessage().contains("enrolled in lesson L10"));
        assertSame(before, session.getDataSnapshot());
        assertSame(displayed, session.getDisplayedLessons());
        assertEquals(List.of(LESSON), displayed.stream().map(lesson -> lesson.getLessonId()).toList());
        assertEquals("Student S9 · current lessons", session.lessonResultContextProperty().get());
        assertArrayEquals(original, Files.readAllBytes(path));

        session.execute("unenrol 2 lid/L10");
        PonHubDataState emptyLesson = session.getDataSnapshot();
        byte[] emptyLessonBytes = Files.readAllBytes(path);
        CommandException tutorFailure = assertThrows(CommandException.class, () -> session.execute("delete 1"));
        assertTrue(tutorFailure.getMessage().contains("assigned to lesson L10"));
        assertSame(emptyLesson, session.getDataSnapshot());
        assertTrue(displayed.isEmpty());
        assertEquals("Student S9 · current lessons", session.lessonResultContextProperty().get());
        assertArrayEquals(emptyLessonBytes, Files.readAllBytes(path));
        assertEquals("Deleted student: Alice Student (S9).", session.execute("delete 2").getFeedbackToUser());
        assertTrue(session.getLessonQueries().getLessonRoster(LESSON).isEmpty());
        assertEquals(10, session.getDataSnapshot().lastAllocatedLessonSequence());
    }

    @Test
    public void execute_retainedAttendanceAfterUnenrol_blocksStudentDeletion() throws Exception {
        Path path = savedData(Set.of());
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("enrol 2 lid/L10");
        session.execute("mark 2 lid/L10 d/2026-10-05 s/present");
        session.execute("unenrol 2 lid/L10");
        session.execute("list r/student");
        PonHubDataState before = session.getDataSnapshot();
        byte[] original = Files.readAllBytes(path);
        var visible = session.getPeople();

        CommandException failure = assertThrows(CommandException.class, () -> session.execute("delete 1"));
        assertTrue(failure.getMessage().contains("retained attendance"));
        assertTrue(failure.getMessage().contains("lesson L10 on 2026-10-05"));
        assertSame(before, session.getDataSnapshot());
        assertSame(visible, session.getPeople());
        assertEquals(List.of(FIRST_STUDENT, SECOND_STUDENT), visibleIds(session));
        assertArrayEquals(original, Files.readAllBytes(path));
        assertEquals(before, new CanonicalPeopleSession(path).getDataSnapshot());
    }

    @Test
    public void execute_invalidSelectorsAndMissingModelRecord_preservesDataWithoutSaving() throws Exception {
        Path path = savedData(Set.of());
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            throw new AssertionError("Invalid deletion must not save");
        });
        session.execute("list r/parent");
        PonHubDataState before = session.getDataSnapshot();
        byte[] original = Files.readAllBytes(path);
        for (String command : List.of("delete", "delete 0", "delete -1", "delete P2", "delete 1 2",
                "delete 2147483648", "delete 1\t2", "delete 1\n")) {
            assertThrows(ParseException.class, () -> session.execute(command));
        }
        CommandException rangeFailure = assertThrows(CommandException.class, () -> session.execute("delete 3"));
        assertEquals(Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX, rangeFailure.getMessage());
        assertSame(before, session.getDataSnapshot());
        assertArrayEquals(original, Files.readAllBytes(path));
        PonHubData data = new PonHubData(before);
        assertThrows(IllegalArgumentException.class, () -> data.removePerson(new PersonId("S99")));
        assertThrows(NullPointerException.class, () -> data.removePerson(null));
        assertSame(before, data.exportState());
    }

    @Test
    public void execute_controlBearingSelectors_preservesBytesSnapshotAndSearchPositions() throws Exception {
        Path path = savedData(Set.of());
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        int[] writes = {0};
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            writes[0]++;
            storage.saveData(state);
        });
        session.execute("search c/parent n/Other");
        var visible = session.getPeople();
        PersonRecord selected = PersonIndexResolver.resolve(Index.fromOneBased(1), visible);
        assertEquals(new PersonId("P5"), selected.getId());
        var searchContext = session.personSearchContextProperty().get();
        var before = session.getDataSnapshot();
        byte[] original = Files.readAllBytes(path);
        int[] events = {0};
        visible.addListener((ListChangeListener<PersonRecord>) change -> events[0]++);

        for (String control : List.of("\0", "\b", "\u001b", "\u0010", "\u007f", "\u0080", "\u009f")) {
            for (String index : List.of(control + "1", "1" + control, "1" + control + "1")) {
                ParseException failure = assertThrows(ParseException.class, () -> session.execute("delete " + index));
                assertEquals(ParserUtil.MESSAGE_INVALID_INDEX, failure.getMessage());
                assertEquals(0, writes[0]);
                assertSame(before, session.getDataSnapshot());
                assertArrayEquals(original, Files.readAllBytes(path));
                assertSame(visible, session.getPeople());
                assertEquals(Optional.of(PersonRole.PARENT), session.getRoleFilter());
                assertSame(searchContext, session.personSearchContextProperty().get());
                assertEquals(List.of(new PersonId("P5")), visibleIds(session));
                assertSame(selected, PersonIndexResolver.resolve(Index.fromOneBased(1), visible));
                assertEquals(0, events[0]);
            }
        }
        assertEquals(before, new CanonicalPeopleSession(path).getDataSnapshot());

        // A valid retry must still use the narrowed search, not another Parent's global position.
        assertEquals("Deleted parent: Other Parent (P5).", session.execute("delete \t1\t ").getFeedbackToUser());
        assertEquals(1, writes[0]);
        assertTrue(visible.isEmpty());
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
    }

    @Test
    public void execute_lineBreakWithControl_keepsSingleLineErrorPrecedence() throws Exception {
        Path path = savedData(Set.of());
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            throw new AssertionError("Rejected deletion must not save");
        });
        var before = session.getDataSnapshot();
        byte[] original = Files.readAllBytes(path);
        ParseException failure = assertThrows(ParseException.class, () -> session.execute("delete 1\0\n"));
        assertEquals("Commands must be on one line.", failure.getMessage());
        assertSame(before, session.getDataSnapshot());
        assertArrayEquals(original, Files.readAllBytes(path));
    }

    @Test
    public void execute_failedSaveHidesDeletionAndRetryCommitsOnce() throws Exception {
        Path path = savedData(Set.of(FIRST_STUDENT));
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        CanonicalPeopleSession[] current = new CanonicalPeopleSession[1];
        PonHubDataState before = storage.readData().orElseThrow();
        int[] writes = {0};
        current[0] = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            assertEquals(before, current[0].getDataSnapshot());
            if (writes[0]++ == 0) {
                throw new IOException("Injected deletion save failure");
            }
            storage.saveData(state);
        });
        CanonicalPeopleSession session = current[0];
        session.execute("search c/parent n/Pat");
        session.execute("search c/lesson s/Math");
        var displayed = session.getDisplayedLessons();
        int[] displayedEvents = {0};
        displayed.addListener((ListChangeListener<? super seedu.address.model.query.LessonView>) change ->
                displayedEvents[0]++);
        PonHubDataState originalSnapshot = session.getDataSnapshot();
        var visible = session.getPeople();
        var queries = session.getLessonQueries();
        var lessons = queries.getLessonList();
        int[] events = {0};
        lessons.addListener((ListChangeListener<? super seedu.address.model.query.LessonView>) change -> events[0]++);
        byte[] original = Files.readAllBytes(path);

        CommandException failure = assertThrows(CommandException.class, () -> session.execute("delete 1"));
        assertTrue(failure.getMessage().contains("Injected deletion save failure"));
        assertSame(originalSnapshot, session.getDataSnapshot());
        assertSame(visible, session.getPeople());
        assertEquals(List.of(new PersonId("P2")), visibleIds(session));
        assertSame(queries, session.getLessonQueries());
        assertSame(lessons, queries.getLessonList());
        assertEquals(0, events[0]);
        assertEquals(0, displayedEvents[0]);
        assertSame(displayed, session.getDisplayedLessons());
        assertEquals(List.of(LESSON), displayed.stream().map(lesson -> lesson.getLessonId()).toList());
        assertEquals("Lesson search: s/math", session.lessonResultContextProperty().get());
        assertArrayEquals(original, Files.readAllBytes(path));

        session.execute("delete 1");
        assertEquals(2, writes[0]);
        assertTrue(visibleIds(session).isEmpty());
        assertEquals(Optional.of(PersonRole.PARENT), session.getRoleFilter());
        assertTrue(events[0] > 0);
        assertSame(displayed, session.getDisplayedLessons());
        assertEquals(List.of(LESSON), displayed.stream().map(lesson -> lesson.getLessonId()).toList());
        assertEquals("Lesson search: s/math", session.lessonResultContextProperty().get());
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
    }

    private Path savedData(Set<PersonId> roster) throws IOException {
        Path path = directory.resolve("canonical.json");
        List<PersonRecord> people = List.of(
                new Tutor(new PersonId("T7"), contacts("Assigned Tutor", "70000001")),
                new Student(FIRST_STUDENT, new ContactDetails(new Name("Alice Student")),
                        new EducationLevel("P1"), new Phone("60000001")),
                new Parent(new PersonId("P2"), contacts("Pat Parent", "60000001")),
                new Student(SECOND_STUDENT, new ContactDetails(new Name("Bob Student")),
                        new EducationLevel("P2"), new Phone("60000001")),
                new Tutor(new PersonId("T2"), contacts("Free Tutor", "70000002")),
                new Parent(new PersonId("P5"), contacts("Other Parent", "60000002")));
        PeopleRegistryState registry = new PeopleRegistryState(people,
                Map.of(PersonRole.STUDENT, 9L, PersonRole.TUTOR, 7L, PersonRole.PARENT, 5L));
        Lesson lesson = new Lesson(LESSON, new PersonId("T7"),
                new LessonTimeSlot(LessonDay.MONDAY, new LessonTime("0900"), new LessonTime("1000")),
                new Subject("Mathematics"), new Room("R1"), roster);
        new JsonPonHubDataStorage(path).saveData(new PonHubDataState(registry, List.of(lesson), List.of(), 10));
        return path;
    }

    private static List<PersonId> visibleIds(CanonicalPeopleSession session) {
        return session.getPeople().stream().map(PersonRecord::getId).toList();
    }

    private static ContactDetails contacts(String name, String phone) {
        return new ContactDetails(new Name(name), Optional.of(new Phone(phone)), Optional.empty(), Optional.empty());
    }
}
