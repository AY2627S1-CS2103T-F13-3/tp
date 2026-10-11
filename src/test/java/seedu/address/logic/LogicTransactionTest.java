package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.Command;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.AddressBookParser;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ModelTransaction;
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
import seedu.address.model.person.PeopleRegistry;
import seedu.address.model.person.PeopleRegistryState;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Phone;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.TypicalPersons;

/**
 * Exercises the transaction seam with commands and a future aggregate adapter test double.
 */
public class LogicTransactionTest {
    @TempDir
    public Path directory;

    @Test
    public void execute_partialCommandFailure_doesNotPublishOrSave() {
        Model model = new ModelManager();
        int[] writes = {0};
        StorageManager storage = storage();
        Command command = new Command() {
            @Override
            public CommandResult execute(Model staged) throws CommandException {
                staged.addPerson(TypicalPersons.AMY);
                throw new CommandException("Validation failed after staged changes");
            }
        };
        Logic logic = logic(model, new StorageManager(new JsonAddressBookStorage(directory.resolve("unused")),
                new JsonUserPrefsStorage(directory.resolve("prefs"))) {
            @Override
            public void saveModel(Model staged) throws IOException {
                writes[0]++;
                storage.saveModel(staged);
            }
        }, command);
        assertThrows(CommandException.class, () -> logic.execute("test"));
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
        assertEquals(0, writes[0]);
    }

    @Test
    public void execute_operationalNoOp_skipsSaveAndPublishesView() throws Exception {
        Model model = new ModelManager();
        model.addPerson(TypicalPersons.AMY);
        Command command = new Command() {
            @Override
            public CommandResult execute(Model staged) {
                staged.setPerson(TypicalPersons.AMY, TypicalPersons.AMY);
                staged.updateFilteredPersonList(person -> false);
                return new CommandResult("Unchanged");
            }
        };
        Logic logic = logic(model, new StorageManager(new JsonAddressBookStorage(directory.resolve("unused")),
                new JsonUserPrefsStorage(directory.resolve("prefs"))) {
            @Override
            public void saveModel(Model staged) {
                throw new AssertionError("No-op must not save");
            }
        }, command);
        assertEquals("Unchanged", logic.execute("test").getFeedbackToUser());
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertEquals(List.of(TypicalPersons.AMY), model.getAddressBook().getPersonList());
    }

    @Test
    public void execute_aggregateFailureThenSuccess_preservesAllStateUntilSaveSucceeds() throws Exception {
        PeopleRegistry registry = new PeopleRegistry();
        var tutor = registry.addTutor(new ContactDetails(new Name("Tutor"), Optional.of(new Phone("91234567")),
                Optional.empty(), Optional.empty()));
        var student = registry.addStudent(new ContactDetails(new Name("Student")),
                new EducationLevel("P1"), new Phone("92345678"));
        Lesson lesson = new Lesson(new LessonId("L1"), tutor.getId(), new LessonTimeSlot(LessonDay.MONDAY,
                new LessonTime("0900"), new LessonTime("1000")), new Subject("Math"), new Room("R1"),
                Set.of(student.getId()));
        Attendance attendance = new Attendance(student.getId(), lesson.getId(), LocalDate.of(2026, 10, 5),
                AttendanceStatus.PRESENT);
        PonHubDataState before = new PonHubDataState(registry.exportState(), List.of(lesson), List.of(attendance), 1);
        var counters = new HashMap<>(before.people().getLastAllocatedSequences());
        counters.put(PersonRole.STUDENT, Long.MAX_VALUE);
        PonHubDataState after = new PonHubDataState(new PeopleRegistryState(before.people().getPeople(), counters),
                List.of(lesson.withEnrolledStudentIds(Set.of())), List.of(attendance), Long.MAX_VALUE);
        AggregateModel model = new AggregateModel(before);
        Command command = new Command() {
            @Override
            public CommandResult execute(Model staged) {
                AggregateModel working = (AggregateModel) staged;
                working.state = after;
                return new CommandResult("Committed");
            }
        };
        int[] writes = {0};
        StorageManager storage = new StorageManager(new JsonAddressBookStorage(directory.resolve("unused")),
                new JsonUserPrefsStorage(directory.resolve("prefs"))) {
            @Override
            public void saveModel(Model staged) throws IOException {
                assertEquals(before, model.state);
                assertEquals(after, ((AggregateModel) staged).state);
                if (writes[0]++ == 0) {
                    throw new IOException("Injected failure");
                }
            }
        };
        Logic logic = logic(model, storage, command);
        assertThrows(CommandException.class, () -> logic.execute("test"));
        assertEquals(before, model.state);
        assertEquals("Committed", logic.execute("test").getFeedbackToUser());
        assertEquals(after, model.state);
        // Identical aggregate status, including counters and retained attendance, is a no-op.
        logic.execute("test");
        assertEquals(2, writes[0]);
    }

    private StorageManager storage() {
        return new StorageManager(new JsonAddressBookStorage(directory.resolve("data.json")),
                new JsonUserPrefsStorage(directory.resolve("prefs.json")));
    }

    private Logic logic(Model model, StorageManager storage, Command command) {
        return new LogicManager(model, storage, new AddressBookParser() {
            @Override
            public Command parseCommand(String text) {
                return command;
            }
        });
    }

    /**
     * Minimal adapter proving that logic compares and persists complete aggregate state through the seam.
     * This does not replace the application's legacy runtime adapter.
     */
    private static class AggregateModel extends ModelManager {
        private PonHubDataState state;

        AggregateModel(PonHubDataState state) {
            this.state = state;
        }

        @Override
        public ModelTransaction<Model> beginTransaction() {
            PonHubDataState before = state;
            AggregateModel staged = new AggregateModel(before);
            return new ModelTransaction<>() {
                @Override
                public Model getStagedModel() {
                    return staged;
                }

                @Override
                public boolean hasOperationalChanges() {
                    return !before.equals(staged.state);
                }

                @Override
                public void commit() {
                    state = staged.state;
                }
            };
        }
    }
}
