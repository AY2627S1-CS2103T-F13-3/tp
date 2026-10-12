package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.ADDRESS_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.EMAIL_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.PHONE_DESC_AMY;
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
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.ExitCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.exceptions.CommandException;
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
    public void execute_protectedAddAndDelete_rejectsWithoutMutationNotificationOrSave() throws Exception {
        int[] notifications = {0};
        visible.addListener((ListChangeListener<Person>) change -> notifications[0]++);
        String addAmy = "add" + NAME_DESC_AMY + PHONE_DESC_AMY + EMAIL_DESC_AMY + ADDRESS_DESC_AMY;

        for (String command : new String[]{addAmy, "delete 1"}) {
            CommandException failure = assertThrows(CommandException.class, () -> logic.execute(command));
            assertEquals(GUIDANCE, failure.getMessage());
            assertEquals(List.of(ALICE), visible);
            assertUnchangedOperationalData();
        }
        assertEquals(0, notifications[0]);
    }

    @Test
    public void execute_protectedHelp_returnsOverviewAndTopicWithoutChangingFilterOrSaving() throws Exception {
        CommandResult overview = logic.execute("help");
        assertEquals(CommandCatalog.getOverview(), overview.getFeedbackToUser());
        assertFalse(overview.isExit());
        assertEquals(CommandCatalog.getUsage("add").orElseThrow(), logic.execute("help add").getFeedbackToUser());
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
