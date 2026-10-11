package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX;
import static seedu.address.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static seedu.address.logic.commands.CommandTestUtil.ADDRESS_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.EMAIL_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.PHONE_DESC_AMY;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.AMY;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonId;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

public class LogicManagerTest {
    private static final IOException DUMMY_IO_EXCEPTION = new IOException("dummy IO exception");
    private static final IOException DUMMY_AD_EXCEPTION = new AccessDeniedException("dummy access denied exception");

    @TempDir
    public Path temporaryFolder;

    private Model model = new ModelManager();
    private Logic logic;

    @BeforeEach
    public void setUp() {
        JsonAddressBookStorage addressBookStorage =
                new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"));
        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"));
        StorageManager storage = new StorageManager(addressBookStorage, userPrefsStorage);
        logic = new LogicManager(model, storage);
    }

    @Test
    public void execute_invalidCommandFormat_throwsParseException() {
        String invalidCommand = "uicfhmowqewca";
        assertParseException(invalidCommand, MESSAGE_UNKNOWN_COMMAND);
    }

    @Test
    public void execute_commandExecutionError_throwsCommandException() {
        String deleteCommand = "delete 9";
        assertCommandException(deleteCommand, MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void execute_validCommand_success() throws Exception {
        String listCommand = ListCommand.COMMAND_WORD;
        assertCommandSuccess(listCommand, ListCommand.MESSAGE_SUCCESS, model);
    }

    @Test
    public void execute_readOnlyCommandsWithUnavailableStorage_neverAttemptSave() throws Exception {
        model.addPerson(AMY);
        model.updateFilteredPersonList(person -> false);
        ReadOnlyAddressBook expected = new seedu.address.model.AddressBook(model.getAddressBook());
        int[] saveCalls = {0};
        JsonAddressBookStorage failingStorage = new JsonAddressBookStorage(temporaryFolder.resolve("unwritable.json")) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                saveCalls[0]++;
                throw new IOException("Read-only commands must not save");
            }
        };
        logic = new LogicManager(model, new StorageManager(failingStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));

        assertEquals(CommandCatalog.getOverview(), logic.execute("help").getFeedbackToUser());
        assertEquals(java.util.List.of(), logic.getFilteredPersonList());
        assertEquals(ListCommand.MESSAGE_SUCCESS, logic.execute("list").getFeedbackToUser());
        assertEquals(java.util.List.of(AMY), logic.getFilteredPersonList());
        assertTrue(logic.execute("exit").isExit());
        assertEquals(0, saveCalls[0]);
        assertEquals(expected, model.getAddressBook());
    }

    @Test
    public void execute_readOnlyCommands_doNotCreateOrRewriteDataFile() throws Exception {
        Path data = temporaryFolder.resolve("addressBook.json");
        for (String command : new String[]{"help", "list", "exit"}) {
            logic.execute(command);
            assertFalse(Files.exists(data));
        }
        // Formatting and comments would be lost if the file were rewritten from the model.
        String original = "{\"_comment\": \"keep this formatting\", \"persons\": []}\n";
        Files.writeString(data, original);
        for (String command : new String[]{"help", "list", "exit"}) {
            logic.execute(command);
            assertEquals(original, Files.readString(data));
        }
    }

    @Test
    public void execute_addAndDelete_stillPersistChanges() throws Exception {
        Path data = temporaryFolder.resolve("addressBook.json");
        logic.execute(AddCommand.COMMAND_WORD + NAME_DESC_AMY + PHONE_DESC_AMY + EMAIL_DESC_AMY + ADDRESS_DESC_AMY);
        assertEquals(1, new JsonAddressBookStorage(data).readAddressBook().orElseThrow().getPersonList().size());
        logic.execute("delete 1");
        assertEquals(0, new JsonAddressBookStorage(data).readAddressBook().orElseThrow().getPersonList().size());
    }

    @Test
    public void execute_failedDelete_preservesFilteredViewAndObservableList() throws Exception {
        Person other = new PersonBuilder(AMY).withName("Other Person").build();
        model.addPerson(other);
        model.addPerson(AMY);
        model.updateFilteredPersonList(AMY::equals);
        ReadOnlyAddressBook before = new seedu.address.model.AddressBook(model.getAddressBook());
        var visible = model.getFilteredPersonList();
        int[] notifications = {0};
        visible.addListener((javafx.collections.ListChangeListener<Person>) change -> notifications[0]++);
        int[] saves = {0};
        JsonAddressBookStorage failing = new JsonAddressBookStorage(temporaryFolder.resolve("failed.json")) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook staged) throws IOException {
                saves[0]++;
                assertEquals(before, model.getAddressBook());
                assertEquals(java.util.List.of(other), staged.getPersonList());
                throw DUMMY_IO_EXCEPTION;
            }
        };
        logic = new LogicManager(model, new StorageManager(failing,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));

        assertThrows(CommandException.class, () -> logic.execute("delete 1"));
        assertEquals(before, model.getAddressBook());
        assertSame(visible, model.getFilteredPersonList());
        assertEquals(java.util.List.of(AMY), visible);
        assertEquals(0, notifications[0]);
        logic.execute("list");
        assertEquals(before.getPersonList(), visible);
        assertEquals(1, saves[0]);
    }

    @Test
    public void execute_failedAddThenSuccessfulAdd_doesNotPersistRejectedPerson() throws Exception {
        int[] saves = {0};
        JsonAddressBookStorage flaky = new JsonAddressBookStorage(temporaryFolder.resolve("retry.json")) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook staged) throws IOException {
                if (saves[0]++ == 0) {
                    throw DUMMY_IO_EXCEPTION;
                }
                assertTrue(model.getAddressBook().getPersonList().isEmpty());
                super.saveAddressBook(staged);
            }
        };
        logic = new LogicManager(model, new StorageManager(flaky,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
        String amy = AddCommand.COMMAND_WORD + NAME_DESC_AMY + PHONE_DESC_AMY + EMAIL_DESC_AMY + ADDRESS_DESC_AMY;
        assertThrows(CommandException.class, () -> logic.execute(amy));
        logic.execute("list");
        assertTrue(model.getFilteredPersonList().isEmpty());
        var visible = model.getFilteredPersonList();
        int[] refreshes = {0};
        visible.addListener((javafx.collections.ListChangeListener<Person>) change -> refreshes[0]++);
        logic.execute(amy.replace(NAME_DESC_AMY, " n/Other Person"));
        assertTrue(refreshes[0] > 0);
        assertSame(visible, model.getFilteredPersonList());
        assertEquals("Other Person", visible.get(0).getName().fullName);
        assertEquals(model.getAddressBook(), flaky.readAddressBook().orElseThrow());
        assertEquals(2, saves[0]);
    }

    @Test
    public void execute_help_doesNotCreateOperationalDataFile() throws Exception {
        logic.execute("help");
        logic.execute("help add");
        assertFalse(Files.exists(temporaryFolder.resolve("addressBook.json")));
    }

    @Test
    public void execute_helpWithUnavailableStorage_preservesFilteredModel() throws Exception {
        model.addPerson(AMY);
        model.updateFilteredPersonList(person -> false);
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.updateFilteredPersonList(person -> false);
        JsonAddressBookStorage failingStorage = new JsonAddressBookStorage(temporaryFolder.resolve("unwritable.json")) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                throw new IOException("Help must not save operational data");
            }
        };
        logic = new LogicManager(model, new StorageManager(failingStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
        assertCommandSuccess("help", CommandCatalog.getOverview(), expectedModel);
        for (String topic : CommandCatalog.getCommandWords()) {
            assertCommandSuccess("help " + topic, CommandCatalog.getUsage(topic).orElseThrow(), expectedModel);
        }
        assertEquals(java.util.List.of(), logic.getFilteredPersonList());
    }

    @Test
    public void execute_withdrawnCommands_preservesModel() {
        model.addPerson(AMY);
        for (String input : new String[]{"edit 1 n/Alex", "clear", "find Amy"}) {
            assertParseException(input, MESSAGE_UNKNOWN_COMMAND);
        }
    }

    @Test
    public void execute_storageThrowsIoException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_IO_EXCEPTION, String.format(
                LogicManager.FILE_OPS_ERROR_FORMAT, DUMMY_IO_EXCEPTION.getMessage()));
    }

    @Test
    public void execute_storageThrowsAdException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_AD_EXCEPTION, String.format(
                LogicManager.FILE_OPS_PERMISSION_ERROR_FORMAT, DUMMY_AD_EXCEPTION.getMessage()));
    }

    @Test
    public void getFilteredPersonList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> logic.getFilteredPersonList().remove(0));
    }

    @Test
    public void retrievalBeforeCanonicalIntegration_failsExplicitlyWithoutChangingPeopleOrSaving() {
        model.addPerson(AMY);
        model.updateFilteredPersonList(person -> false);
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.updateFilteredPersonList(person -> false);
        PersonId studentId = new PersonId("S1");
        PersonId tutorId = new PersonId("T1");
        LessonId lessonId = new LessonId("L1");
        var peopleView = logic.getFilteredPersonList();
        assertRetrievalUnavailable(logic::getLessonList);
        assertRetrievalUnavailable(logic::getFilteredLessonList);
        assertRetrievalUnavailable(() -> logic.findLessonById(lessonId));
        assertRetrievalUnavailable(() -> logic.getStudentLessons(studentId));
        assertRetrievalUnavailable(() -> logic.getLessonRoster(lessonId));
        assertRetrievalUnavailable(() -> logic.getTutorSchedule(tutorId));
        assertRetrievalUnavailable(() -> logic.getAttendanceHistory(studentId));
        assertRetrievalUnavailable(() -> logic.getAttendanceHistory(studentId, lessonId));

        assertEquals(expectedModel, model);
        assertSame(peopleView, logic.getFilteredPersonList());
        assertFalse(Files.exists(temporaryFolder.resolve("addressBook.json")));
    }

    private void assertRetrievalUnavailable(Executable query) {
        assertThrows(UnsupportedOperationException.class, LogicManager.RETRIEVAL_UNAVAILABLE, query);
    }

    /**
     * Executes the command and confirms that
     * - no exceptions are thrown <br>
     * - the feedback message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandSuccess(String inputCommand, String expectedMessage,
            Model expectedModel) throws CommandException, ParseException {
        CommandResult result = logic.execute(inputCommand);
        assertEquals(expectedMessage, result.getFeedbackToUser());
        assertEquals(expectedModel, model);
    }

    /**
     * Executes the command, confirms that a ParseException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertParseException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, ParseException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that a CommandException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, CommandException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that the exception is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage) {
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        assertCommandFailure(inputCommand, expectedException, expectedMessage, expectedModel);
    }

    /**
     * Executes the command and confirms that
     * - the {@code expectedException} is thrown <br>
     * - the resulting error message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandSuccess(String, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage, Model expectedModel) {
        assertThrows(expectedException, expectedMessage, () -> logic.execute(inputCommand));
        assertEquals(expectedModel, model);
    }

    /**
     * Tests the Logic component's handling of an {@code IOException} thrown by the Storage component.
     *
     * @param e the exception to be thrown by the Storage component
     * @param expectedMessage the message expected inside exception thrown by the Logic component
     */
    private void assertCommandFailureForExceptionFromStorage(IOException e, String expectedMessage) {
        Path prefPath = temporaryFolder.resolve("ExceptionUserPrefs.json");

        // Inject LogicManager with a JsonAddressBookStorage that throws the IOException e when saving
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(prefPath) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                throw e;
            }
        };

        JsonUserPrefsStorage userPrefsStorage =
                new JsonUserPrefsStorage(temporaryFolder.resolve("ExceptionUserPrefs.json"));
        StorageManager storage = new StorageManager(addressBookStorage, userPrefsStorage);

        logic = new LogicManager(model, storage);

        // Triggers the saveAddressBook method by executing an add command
        String addCommand = AddCommand.COMMAND_WORD + NAME_DESC_AMY + PHONE_DESC_AMY
                + EMAIL_DESC_AMY + ADDRESS_DESC_AMY;
        ModelManager expectedModel = new ModelManager(model.getAddressBook(), model.getUserPrefs());
        assertCommandFailure(addCommand, CommandException.class, expectedMessage, expectedModel);
    }
}
