package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import javafx.collections.ObservableList;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.PonHubDataState;
import seedu.address.model.UserPrefs;
import seedu.address.model.lesson.Lesson;
import seedu.address.model.lesson.LessonDay;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.lesson.LessonTime;
import seedu.address.model.lesson.LessonTimeSlot;
import seedu.address.model.lesson.Room;
import seedu.address.model.lesson.Subject;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.Name;
import seedu.address.model.person.PeopleRegistryState;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Tutor;
import seedu.address.model.query.LessonView;
import seedu.address.storage.JsonPonHubDataStorage;
import seedu.address.storage.ProtectedPonHubDataStorage;

/**
 * Exercises tutor disambiguation, allocation boundaries and protected routes through application Logic.
 */
public class CanonicalLessonCreationBoundaryTest {
    private static final PersonId FIRST_TUTOR = new PersonId("T7");
    private static final PersonId SECOND_TUTOR = new PersonId("T8");
    private static final String SECOND_TUTOR_COMMAND =
            "addlesson d/Mon st/0900 et/1000 s/Math tu/mei   LIM tp/00987654 rm/R1";

    @TempDir
    Path directory;

    @Test
    public void execute_sameNormalizedNameSelectsSecondTutorByExactPhoneAndPersistsAssignment() throws Exception {
        Fixture fixture = new Fixture("second-tutor.json", 12, false);

        assertEquals("New lesson added: L13.", fixture.logic.execute(SECOND_TUTOR_COMMAND).getFeedbackToUser());

        Lesson expected = new Lesson(new LessonId("L13"), SECOND_TUTOR,
                new LessonTimeSlot(LessonDay.MONDAY, new LessonTime("0900"), new LessonTime("1000")),
                new Subject("Math"), new Room("R1"), Set.of());
        assertEquals(new PonHubDataState(fixture.initial.people(), List.of(expected), List.of(), 13),
                fixture.session.getDataSnapshot());
        assertEquals(fixture.session.getDataSnapshot(), new CanonicalPeopleSession(fixture.path).getDataSnapshot());
        assertEquals(SECOND_TUTOR, fixture.logic.findLessonById(expected.getId()).orElseThrow().getTutorId());
        assertTrue(fixture.logic.getTutorSchedule(FIRST_TUTOR).isEmpty());
        assertEquals(List.of(expected.getId()), fixture.logic.getTutorSchedule(SECOND_TUTOR).stream()
                .map(LessonView::getLessonId).toList());
        assertEquals(1, fixture.writes);
        fixture.assertViewRetained();
    }

    @Test
    public void execute_unmatchedPhoneNeverFallsBackAndMalformedRoutePreservesState() throws Exception {
        Fixture fixture = new Fixture("rejections.json", 12, false);
        byte[] bytes = Files.readAllBytes(fixture.path);

        CommandException failure = assertThrows(CommandException.class, () -> fixture.logic.execute(
                SECOND_TUTOR_COMMAND.replace("00987654", "00011122")));
        assertEquals("No tutor named 'mei LIM' has the exact phone '00011122'.", failure.getMessage());
        fixture.assertUnchanged(fixture.initial, bytes);
        CommandException wrongName = assertThrows(CommandException.class, () -> fixture.logic.execute(
                SECOND_TUTOR_COMMAND.replace("mei   LIM", "Another Tutor")));
        assertEquals("No tutor named 'Another Tutor' has the exact phone '00987654'.", wrongName.getMessage());
        fixture.assertUnchanged(fixture.initial, bytes);

        ParseException repeatedPhone = assertThrows(ParseException.class, () -> fixture.logic.execute(
                SECOND_TUTOR_COMMAND + " tp/00123456"));
        assertEquals("Repeated addlesson prefix 'tp/'.", repeatedPhone.getMessage());
        fixture.assertUnchanged(fixture.initial, bytes);
        assertEquals(0, fixture.writes);
    }

    @Test
    public void execute_lastLessonIdSaveFailureRetriesAndExhaustionIsCheckedWithoutWrites() throws Exception {
        Fixture fixture = new Fixture("last-id.json", Long.MAX_VALUE - 1, true);
        byte[] originalBytes = Files.readAllBytes(fixture.path);
        LessonId lastId = new LessonId("L9223372036854775807");

        CommandException saveFailure = assertThrows(CommandException.class, () -> fixture.logic.execute(
                SECOND_TUTOR_COMMAND));
        assertSame(fixture.saveFailure, saveFailure.getCause());
        assertEquals(String.format(CanonicalCommandExecutor.FILE_OPS_ERROR_FORMAT,
                    fixture.saveFailure.getMessage()),
                saveFailure.getMessage());
        fixture.assertUnchanged(fixture.initial, originalBytes);
        assertEquals(1, fixture.writes);

        assertEquals("New lesson added: " + lastId + ".",
                fixture.logic.execute(SECOND_TUTOR_COMMAND).getFeedbackToUser());
        PonHubDataState committed = fixture.session.getDataSnapshot();
        assertEquals(lastId, committed.lessons().getFirst().getId());
        assertEquals(SECOND_TUTOR, committed.lessons().getFirst().getTutorId());
        assertEquals(Long.MAX_VALUE, committed.lastAllocatedLessonSequence());
        assertEquals(fixture.initial.people(), committed.people());
        assertEquals(committed, new CanonicalPeopleSession(fixture.path).getDataSnapshot());
        assertEquals(2, fixture.writes);
        byte[] committedBytes = Files.readAllBytes(fixture.path);
        fixture.notifications = 0;

        // Use a disjoint slot so exhaustion cannot be confused with schedule rejection.
        CommandException exhausted = assertThrows(CommandException.class, () -> fixture.logic.execute(
                SECOND_TUTOR_COMMAND.replace("d/Mon", "d/Tue")));
        assertEquals(LessonId.MESSAGE_SEQUENCE_EXHAUSTED, exhausted.getMessage());
        assertInstanceOf(IllegalStateException.class, exhausted.getCause());
        fixture.assertUnchanged(committed, committedBytes);
        assertEquals(2, fixture.writes);

        CanonicalPeopleSession restarted = new CanonicalPeopleSession(fixture.path);
        Logic restartedLogic = new CanonicalLogicManager(restarted, new UserPrefs());
        CommandException afterRestart = assertThrows(CommandException.class, () -> restartedLogic.execute(
                SECOND_TUTOR_COMMAND.replace("d/Mon", "d/Tue")));
        assertEquals(LessonId.MESSAGE_SEQUENCE_EXHAUSTED, afterRestart.getMessage());
        assertEquals(committed, restarted.getDataSnapshot());
        assertArrayEquals(committedBytes, Files.readAllBytes(fixture.path));
    }

    @Test
    public void execute_rejectedStartupProtectsAllLessonMutatorsBeforeParsing() throws Exception {
        Path path = directory.resolve("protected.json");
        Files.writeString(path, "Rejected original bytes\n");
        byte[] bytes = Files.readAllBytes(path);
        int[] writes = {0};
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path),
                candidate -> writes[0]++);
        Logic logic = new CanonicalLogicManager(session, new UserPrefs());
        var people = logic.getPersonRecords();
        var lessons = logic.getLessonList();
        PonHubDataState before = session.getDataSnapshot();

        for (String command : new String[]{SECOND_TUTOR_COMMAND, "addlesson", "enrol 1 lid/L10",
            "enrol S3 lid/L10", "unenrol 1 lid/L10", "unenrol"}) {
            CommandException failure = assertThrows(CommandException.class, () -> logic.execute(command));
            assertEquals(session.getDataLoadError().orElseThrow(), failure.getMessage());
            assertEquals(before, session.getDataSnapshot());
            assertArrayEquals(bytes, Files.readAllBytes(path));
            assertSame(people, logic.getPersonRecords());
            assertSame(lessons, logic.getLessonList());
            assertTrue(people.isEmpty());
            assertTrue(lessons.isEmpty());
            assertEquals(0, writes[0]);
        }
    }

    private static PonHubDataState state(long counter) {
        Tutor first = new Tutor(FIRST_TUTOR, contacts("Mei Lim", "00123456"));
        Tutor second = new Tutor(SECOND_TUTOR, contacts("MEI LIM", "00987654"));
        Tutor other = new Tutor(new PersonId("T9"), contacts("Another Tutor", "00011122"));
        PeopleRegistryState people = new PeopleRegistryState(List.of(first, second, other),
                Map.of(PersonRole.STUDENT, 0L, PersonRole.TUTOR, 9L, PersonRole.PARENT, 0L));
        return new PonHubDataState(people, List.of(), List.of(), counter);
    }

    private static ContactDetails contacts(String name, String phone) {
        return new ContactDetails(new Name(name), Optional.of(new Phone(phone)), Optional.empty(), Optional.empty());
    }

    /**
     * Retains observable identity and counts writes and notifications around one real storage transaction.
     */
    private class Fixture {
        private final Path path;
        private final PonHubDataState initial;
        private final CanonicalPeopleSession session;
        private final Logic logic;
        private final ObservableList<PersonRecord> people;
        private final ObservableList<LessonView> lessons;
        private final IOException saveFailure = new IOException("Injected last lesson ID save failure");
        private int writes;
        private int notifications;

        Fixture(String filename, long counter, boolean shouldFailFirstSave) throws Exception {
            path = directory.resolve(filename);
            initial = state(counter);
            JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
            storage.saveData(initial);
            session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), candidate -> {
                assertLiveInitial();
                if (++writes == 1 && shouldFailFirstSave) {
                    throw saveFailure;
                }
                storage.saveData(candidate);
            });
            logic = new CanonicalLogicManager(session, new UserPrefs());
            logic.execute("search c/tutor n/Mei");
            people = logic.getPersonRecords();
            lessons = logic.getLessonList();
            people.addListener((ListChangeListener<PersonRecord>) change -> notifications++);
            lessons.addListener((ListChangeListener<LessonView>) change -> notifications++);
        }

        void assertLiveInitial() {
            assertEquals(initial, session.getDataSnapshot());
        }

        void assertViewRetained() {
            assertSame(people, logic.getPersonRecords());
            assertSame(lessons, logic.getLessonList());
            assertEquals(List.of(FIRST_TUTOR, SECOND_TUTOR), people.stream().map(PersonRecord::getId).toList());
            assertEquals(Optional.of(PersonRole.TUTOR), session.getRoleFilter());
        }

        void assertUnchanged(PonHubDataState before, byte[] bytes) throws Exception {
            assertEquals(before, session.getDataSnapshot());
            assertEquals(before.people().getLastAllocatedSequences(), session.getDataSnapshot().people()
                    .getLastAllocatedSequences());
            assertEquals(before.lastAllocatedLessonSequence(), session.getDataSnapshot().lastAllocatedLessonSequence());
            assertArrayEquals(bytes, Files.readAllBytes(path));
            assertEquals(0, notifications);
            assertViewRetained();
        }
    }
}
