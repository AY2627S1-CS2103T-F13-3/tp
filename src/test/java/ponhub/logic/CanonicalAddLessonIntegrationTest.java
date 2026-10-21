package ponhub.logic;

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

import javafx.collections.ObservableList;
import ponhub.logic.commands.CommandResult;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.model.PonHubDataState;
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

public class CanonicalAddLessonIntegrationTest {
    private static final PersonId FIRST_TUTOR_ID = new PersonId("T7");
    private static final PersonId SECOND_TUTOR_ID = new PersonId("T8");
    private static final PersonId UNIQUE_TUTOR_ID = new PersonId("T9");
    private static final LessonId NEXT_LESSON_ID = new LessonId("L13");

    @TempDir
    Path directory;

    @Test
    public void execute_normalizedNameAndLeadingZeroPhone_savesEmptyLessonAndRefreshesQueries() throws Exception {
        Path path = directory.resolve("folder with spaces/ponhub.json");
        PonHubDataState before = createState(List.of(), 12);
        new JsonPonHubDataStorage(path).saveData(before);
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("list r/student");
        ObservableList<PersonRecord> people = session.getPeople();
        var queries = session.getLessonQueries();
        ObservableList<LessonView> lessons = queries.getLessonList();

        CommandResult result = session.execute("addlesson rm/room2 tu/mei   lim tp/00123456"
                + " s/Primary  Math et/1730 st/1600 d/mOn");

        assertEquals("New lesson added: L13.", result.getFeedbackToUser());
        assertEquals(Optional.of(PersonRole.STUDENT), session.getRoleFilter());
        assertSame(people, session.getPeople());
        assertSame(queries, session.getLessonQueries());
        assertSame(lessons, queries.getLessonList());
        Lesson added = session.getDataSnapshot().lessons().getLast();
        assertEquals(NEXT_LESSON_ID, added.getId());
        assertEquals(FIRST_TUTOR_ID, added.getTutorId());
        assertEquals(new LessonTimeSlot(LessonDay.MONDAY, new LessonTime("1600"), new LessonTime("1730")),
                added.getTimeSlot());
        assertEquals(new Subject("Primary Math"), added.getSubject());
        assertEquals(new Room("ROOM2"), added.getRoom());
        assertTrue(added.getEnrolledStudentIds().isEmpty());
        LessonView projected = queries.findLessonById(NEXT_LESSON_ID).orElseThrow();
        assertEquals(FIRST_TUTOR_ID, projected.getTutorId());
        assertEquals(0, projected.getRosterSize());
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
    }

    @Test
    public void execute_tutorSelection_requiresUniqueNameAndHonorsEverySuppliedPhone() throws Exception {
        CanonicalPeopleSession session = createSessionWithoutWrites(createState(List.of(), 12));
        PonHubDataState before = session.getDataSnapshot();

        assertCommandFailure(session, "addlesson " + lessonArguments("Mei Lim", "", "R2"),
                "More than one tutor is named 'Mei Lim'. Supply tp/TUTOR_PHONE to select one.");
        assertCommandFailure(session, "addlesson " + lessonArguments("Unique Tutor", " tp/99999999", "R2"),
                "No tutor named 'Unique Tutor' has the exact phone '99999999'.");
        assertCommandFailure(session, "addlesson " + lessonArguments("Missing Tutor", "", "R2"),
                "No tutor found with the full name 'Missing Tutor'.");
        assertEquals(before, session.getDataSnapshot());
    }

    @Test
    public void execute_tutorAndRoomClashes_rejectWithoutWritingOrConsumingId() throws Exception {
        Lesson existing = createLesson(new LessonId("L12"), FIRST_TUTOR_ID, LessonDay.MONDAY,
                "0900", "1000", "R1");
        Path path = directory.resolve("clashes.json");
        PonHubDataState before = createState(List.of(existing), 12);
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        storage.saveData(before);
        int[] writes = {0};
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            writes[0]++;
            storage.saveData(state);
        });

        assertCommandFailure(session,
                "addlesson d/Mon st/0930 et/1030 s/Math tu/Mei Lim tp/00123456 rm/R2",
                "Tutor schedule overlap between L12 and L13.");
        assertCommandFailure(session,
                "addlesson d/Mon st/0930 et/1030 s/Math tu/Unique Tutor rm/r1",
                "Room schedule overlap between L12 and L13.");
        assertEquals(before, session.getDataSnapshot());
        assertEquals(0, writes[0]);

        assertEquals("New lesson added: L13.", session.execute(
                "addlesson d/Mon st/1000 et/1100 s/Math tu/Mei Lim tp/00123456 rm/R2")
                .getFeedbackToUser());
        assertEquals(NEXT_LESSON_ID, session.getDataSnapshot().lessons().getLast().getId());
        assertEquals(1, writes[0]);
    }

    @Test
    public void execute_failedSaveThenRetry_preservesStateFilterFileAndLessonId() throws Exception {
        Path path = directory.resolve("save-failure.json");
        PonHubDataState before = createState(List.of(), 12);
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        storage.saveData(before);
        byte[] original = Files.readAllBytes(path);
        int[] writes = {0};
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            if (writes[0]++ == 0) {
                throw new IOException("Injected lesson save failure");
            }
            storage.saveData(state);
        });
        session.execute("list r/student");
        ObservableList<PersonRecord> people = session.getPeople();
        var queries = session.getLessonQueries();
        ObservableList<LessonView> lessons = queries.getLessonList();
        String command = "addlesson " + lessonArguments("Unique Tutor", " tp/00011122", "R2");

        CommandException failure = assertThrows(CommandException.class, () -> session.execute(command));
        assertTrue(failure.getMessage().contains("Injected lesson save failure"));
        assertEquals(before, session.getDataSnapshot());
        assertEquals(Optional.of(PersonRole.STUDENT), session.getRoleFilter());
        assertSame(people, session.getPeople());
        assertSame(queries, session.getLessonQueries());
        assertSame(lessons, queries.getLessonList());
        assertTrue(lessons.isEmpty());
        assertArrayEquals(original, Files.readAllBytes(path));

        assertEquals("New lesson added: L13.", session.execute(command).getFeedbackToUser());
        assertEquals(NEXT_LESSON_ID, session.getDataSnapshot().lessons().getLast().getId());
        assertEquals(2, writes[0]);
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
    }

    private CanonicalPeopleSession createSessionWithoutWrites(PonHubDataState state) throws Exception {
        Path path = directory.resolve("rejected-selections.json");
        new JsonPonHubDataStorage(path).saveData(state);
        return new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), candidate -> {
            throw new AssertionError("Rejected tutor selection must not save: " + candidate);
        });
    }

    private static void assertCommandFailure(CanonicalPeopleSession session, String command, String message) {
        CommandException failure = assertThrows(CommandException.class, () -> session.execute(command));
        assertEquals(message, failure.getMessage());
    }

    private static String lessonArguments(String tutorName, String tutorPhone, String room) {
        return "d/Tue st/1200 et/1300 s/Math tu/" + tutorName + tutorPhone + " rm/" + room;
    }

    private static PonHubDataState createState(List<Lesson> lessons, long lessonCounter) {
        Tutor firstTutor = new Tutor(FIRST_TUTOR_ID, createContacts("Mei Lim", "00123456"));
        Tutor secondTutor = new Tutor(SECOND_TUTOR_ID, createContacts("MEI LIM", "00987654"));
        Tutor uniqueTutor = new Tutor(UNIQUE_TUTOR_ID, createContacts("Unique Tutor", "00011122"));
        Student student = new Student(new PersonId("S4"), new ContactDetails(new Name("Student Tan")),
                new EducationLevel("S2"), new Phone("00222222"));
        PeopleRegistryState people = new PeopleRegistryState(List.of(firstTutor, secondTutor, uniqueTutor, student),
                Map.of(PersonRole.STUDENT, 4L, PersonRole.TUTOR, 9L, PersonRole.PARENT, 0L));
        return new PonHubDataState(people, lessons, List.of(), lessonCounter);
    }

    private static ContactDetails createContacts(String name, String phone) {
        return new ContactDetails(new Name(name), Optional.of(new Phone(phone)), Optional.empty(), Optional.empty());
    }

    private static Lesson createLesson(LessonId id, PersonId tutorId, LessonDay day, String startTime,
            String endTime, String room) {
        return new Lesson(id, tutorId,
                new LessonTimeSlot(day, new LessonTime(startTime), new LessonTime(endTime)),
                new Subject("Math"), new Room(room), Set.of());
    }
}
