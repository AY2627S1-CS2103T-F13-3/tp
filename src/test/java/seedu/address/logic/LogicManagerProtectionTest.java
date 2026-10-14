package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import seedu.address.logic.commands.Command;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.ExitCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.AddressBookParser;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

// Developed with OpenAI Codex assistance for ultramanarm.
/**
 * Guards the protected-command allowlist in the retained legacy logic.
 */
public class LogicManagerProtectionTest {
    private static final String GUIDANCE = "Rejected operational data; restore a known-good file and restart.";
    private static final String REJECTED_DATA = "{\"persons\":[null]}\n";

    @TempDir
    public Path folder;

    private Path dataFile;
    private ModelManager model;
    private AddressBook original;
    private ObservableList<Person> visible;
    private ProtectedStorage storage;
    private LogicManager logic;

    @BeforeEach
    public void setUp() throws Exception {
        dataFile = folder.resolve("addressbook.json");
        Files.writeString(dataFile, REJECTED_DATA);
        original = getTypicalAddressBook();
        model = new ModelManager(original, new UserPrefs());
        model.updateFilteredPersonList(ALICE::equals);
        visible = model.getFilteredPersonList();
        storage = new ProtectedStorage(dataFile, folder.resolve("preferences.json"));
        logic = new LogicManager(model, storage);
    }

    @Test
    public void execute_withdrawnLegacyMutators_rejectsWithoutMutationNotificationOrSave() throws Exception {
        int[] notifications = {0};
        visible.addListener((ListChangeListener<Person>) change -> notifications[0]++);

        for (String command : new String[]{"add n/Amy p/123", "delete 1"}) {
            ParseException failure = assertThrows(ParseException.class, () -> logic.execute(command));
            assertEquals(MESSAGE_UNKNOWN_COMMAND, failure.getMessage());
            assertEquals(List.of(ALICE), visible);
            assertUnchangedOperationalData();
        }
        assertEquals(0, notifications[0]);
    }

    @Test
    public void execute_protectedInjectedMutation_rejectsBeforeExecutionOrSave() throws Exception {
        logic = new LogicManager(model, storage, new AddressBookParser() {
            @Override
            public Command parseCommand(String text) {
                return new Command() {
                    @Override
                    public CommandResult execute(seedu.address.model.Model target) {
                        throw new AssertionError("Protected logic must reject before command execution");
                    }
                };
            }
        });

        CommandException failure = assertThrows(CommandException.class, () -> logic.execute("test"));
        assertEquals(GUIDANCE, failure.getMessage());
        assertEquals(List.of(ALICE), visible);
        assertUnchangedOperationalData();
    }

    @Test
    public void execute_protectedHelp_returnsOverviewAndTopicWithoutChangingFilterOrSaving() throws Exception {
        CommandResult overview = logic.execute("help");
        assertEquals(CommandCatalog.getOverview(), overview.getFeedbackToUser());
        assertFalse(overview.isExit());
        assertEquals(CommandCatalog.getUsage("list").orElseThrow(), logic.execute("help list").getFeedbackToUser());
        assertEquals(List.of(ALICE), visible);
        assertUnchangedOperationalData();
    }

    @Test
    public void execute_protectedList_resetsOnlyFilterWithoutSaving() throws Exception {
        CommandResult result = logic.execute("list");

        assertEquals(ListCommand.MESSAGE_SUCCESS, result.getFeedbackToUser());
        assertFalse(result.isExit());
        assertEquals(original.getPersonList(), visible);
        assertUnchangedOperationalData();
    }

    @Test
    public void execute_protectedExit_returnsExitResultWithoutChangingFilterOrSaving() throws Exception {
        CommandResult result = logic.execute("exit");

        assertEquals(ExitCommand.MESSAGE_EXIT_ACKNOWLEDGEMENT, result.getFeedbackToUser());
        assertTrue(result.isExit());
        assertEquals(List.of(ALICE), visible);
        assertUnchangedOperationalData();
    }

    private void assertUnchangedOperationalData() throws IOException {
        assertEquals(original, model.getAddressBook());
        assertSame(visible, logic.getFilteredPersonList());
        assertEquals(0, storage.saveCalls);
        assertArrayEquals(REJECTED_DATA.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(dataFile));
    }

    /**
     * Supplies guidance without the storage write lock, so the logic guard must prevent writes itself.
     */
    private static class ProtectedStorage extends StorageManager {
        private int saveCalls;

        ProtectedStorage(Path dataFile, Path prefsFile) {
            super(new JsonAddressBookStorage(dataFile), new JsonUserPrefsStorage(prefsFile));
        }

        @Override
        public Optional<String> getDataLoadError() {
            return Optional.of(GUIDANCE);
        }

        @Override
        public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
            saveCalls++;
            super.saveAddressBook(addressBook);
        }
    }
}
