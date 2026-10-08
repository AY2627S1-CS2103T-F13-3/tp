package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Exercises the actual startup model initialization and command/storage boundaries together. */
public class ProtectedStartupTest {
    private static final String ADD_COMMAND = "add n/Alex p/91234567 e/alex@example.com a/Somewhere";

    @TempDir
    public Path testFolder;

    @Test
    public void startup_rejectedData_preservesBytesAcrossCommandsAndDirectSaves() throws Exception {
        String personWithNullTag = "{\"name\":\"Alex\",\"phone\":\"91234567\","
                + "\"email\":\"alex@example.com\",\"address\":\"Somewhere\",\"tags\":[null]}";
        String[] documents = {
            "{broken", "", "null", "[]", "{}", "{\"persons\":null}", "{\"persons\":{}}",
            "{\"persons\":[null]}", "{\"persons\":[42]}", "{\"persons\":[{}]}",
            "{\"persons\":[" + personWithNullTag + "]}",
            "{\"persons\":[],\"lessons\":[]}",
            "{\"schemaVersion\":99,\"persons\":[],\"lessons\":[]}",
            "{\"schemaVersion\":1,\"persons\":[]}", "{\"schemaVersion\":null,\"persons\":[]}",
            "{\"persons\":[]} {}", "{\"persons\":[],\"persons\":[]}"
        };
        for (String document : documents) {
            Path data = testFolder.resolve("data.json");
            Files.writeString(data, document);
            byte[] original = Files.readAllBytes(data);
            CountingStorage storage = new CountingStorage(data, testFolder.resolve("prefs.json"));
            Model model = new MainApp().initModelManager(storage, new UserPrefs());
            LogicManager logic = new LogicManager(model, storage);

            assertEquals(new AddressBook(), model.getAddressBook());
            assertTrue(storage.getDataLoadError().orElseThrow().contains(data.toAbsolutePath().toString()));
            logic.execute("help");
            logic.execute("list");
            assertTrue(logic.execute("exit").isExit());
            for (String command : new String[]{ADD_COMMAND, "delete 1"}) {
                CommandException error = assertThrows(CommandException.class, () -> logic.execute(command));
                assertEquals(storage.getDataLoadError().orElseThrow(), error.getMessage());
            }
            assertEquals(new AddressBook(), model.getAddressBook());
            assertEquals(0, storage.saveCalls);
            assertThrows(IOException.class, () -> storage.saveAddressBook(model.getAddressBook()));
            storage.saveUserPrefs(new UserPrefs());
            assertTrue(storage.readUserPrefs().isPresent());
            assertArrayEquals(original, Files.readAllBytes(data), document);
        }
    }

    @Test
    public void startup_unreadableFile_protectsOriginalAndAllowsExit() throws Exception {
        Path data = testFolder.resolve("denied.json");
        Files.writeString(data, "{\"persons\":[]}");
        byte[] original = Files.readAllBytes(data);
        JsonAddressBookStorage denied = new JsonAddressBookStorage(data) {
            @Override
            public java.util.Optional<ReadOnlyAddressBook> readAddressBook() throws DataLoadingException {
                throw new DataLoadingException(new AccessDeniedException(data.toString()));
            }
        };
        StorageManager storage = new StorageManager(denied,
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));
        Model model = new MainApp().initModelManager(storage, new UserPrefs());
        LogicManager logic = new LogicManager(model, storage);
        assertTrue(logic.execute("exit").isExit());
        assertThrows(CommandException.class, () -> logic.execute(ADD_COMMAND));
        assertThrows(IOException.class, () -> storage.saveAddressBook(new AddressBook()));
        assertArrayEquals(original, Files.readAllBytes(data));
    }

    @Test
    public void startup_missingFile_remainsWritableAndRoundTrips() throws Exception {
        Path data = testFolder.resolve("new.json");
        CountingStorage storage = new CountingStorage(data, testFolder.resolve("prefs.json"));
        Model model = new MainApp().initModelManager(storage, new UserPrefs());
        assertTrue(storage.getDataLoadError().isEmpty());
        assertFalse(Files.exists(data));
        new LogicManager(model, storage).execute(ADD_COMMAND);
        assertEquals(model.getAddressBook(), storage.readAddressBook().orElseThrow());
    }

    @Test
    public void startup_validHumanEditedLegacyFile_loadsAndRemainsWritable() throws Exception {
        Path data = testFolder.resolve("data.json");
        Files.writeString(data, "{\"_comment\":\"Manually checked contacts\",\"persons\":[]}");
        CountingStorage storage = new CountingStorage(data, testFolder.resolve("prefs.json"));
        Model model = new MainApp().initModelManager(storage, new UserPrefs());
        assertTrue(storage.getDataLoadError().isEmpty());
        new LogicManager(model, storage).execute(ADD_COMMAND);
        assertEquals(1, model.getAddressBook().getPersonList().size());
        assertEquals(model.getAddressBook(), storage.readAddressBook().orElseThrow());
    }

    @Test
    public void startup_danglingSymbolicLink_preservesLinkAndMissingTarget() throws Exception {
        Path data = testFolder.resolve("data.json");
        Path target = testFolder.resolve("missing.json");
        try {
            Files.createSymbolicLink(data, target.getFileName());
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(false, "Symbolic links unavailable: " + exception);
        }
        assertThrows(DataLoadingException.class, () -> new JsonAddressBookStorage(data).readAddressBook());
        CountingStorage storage = new CountingStorage(data, testFolder.resolve("prefs.json"));
        Model model = new MainApp().initModelManager(storage, new UserPrefs());
        LogicManager logic = new LogicManager(model, storage);
        assertTrue(storage.getDataLoadError().isPresent());
        assertEquals(new AddressBook(), model.getAddressBook());
        logic.execute("help");
        logic.execute("list");
        assertTrue(logic.execute("exit").isExit());
        for (String command : new String[]{ADD_COMMAND, "delete 1"}) {
            CommandException error = assertThrows(CommandException.class, () -> logic.execute(command));
            assertEquals(storage.getDataLoadError().orElseThrow(), error.getMessage());
        }
        assertEquals(0, storage.saveCalls);
        assertThrows(IOException.class, () -> storage.saveAddressBook(model.getAddressBook()));
        assertEquals(new AddressBook(), model.getAddressBook());
        assertTrue(Files.isSymbolicLink(data));
        assertEquals(target.getFileName(), Files.readSymbolicLink(data));
        assertTrue(Files.notExists(target, LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    public void startup_repairedFileAndSuccessfulReread_staysLockedUntilRestart() throws Exception {
        Path data = testFolder.resolve("data.json");
        Files.writeString(data, "{broken");
        CountingStorage storage = new CountingStorage(data, testFolder.resolve("prefs.json"));
        Model model = new MainApp().initModelManager(storage, new UserPrefs());
        LogicManager logic = new LogicManager(model, storage);
        String originalError = storage.getDataLoadError().orElseThrow();

        Path fixture = Path.of("src", "test", "data", "JsonSerializableAddressBookTest",
                "typicalPersonsAddressBook.json");
        byte[] repaired = Files.readAllBytes(fixture);
        Files.write(data, repaired);
        assertEquals(getTypicalAddressBook(), storage.readAddressBook().orElseThrow());
        assertEquals(originalError, storage.getDataLoadError().orElseThrow());
        for (String command : new String[]{ADD_COMMAND, "delete 1"}) {
            CommandException error = assertThrows(CommandException.class, () -> logic.execute(command));
            assertEquals(originalError, error.getMessage());
        }
        logic.execute("help");
        logic.execute("list");
        assertTrue(logic.execute("exit").isExit());
        assertEquals(0, storage.saveCalls);
        assertThrows(IOException.class, () -> storage.saveAddressBook(model.getAddressBook()));
        assertEquals(new AddressBook(), model.getAddressBook());
        assertArrayEquals(repaired, Files.readAllBytes(data));

        CountingStorage restarted = new CountingStorage(data, testFolder.resolve("prefs.json"));
        Model restartedModel = new MainApp().initModelManager(restarted, new UserPrefs());
        assertTrue(restarted.getDataLoadError().isEmpty());
        assertEquals(getTypicalAddressBook(), restartedModel.getAddressBook());
        assertArrayEquals(repaired, Files.readAllBytes(data));
        new LogicManager(restartedModel, restarted).execute(ADD_COMMAND);
        assertEquals(1, restarted.saveCalls);
        assertEquals(restartedModel.getAddressBook(), restarted.readAddressBook().orElseThrow());
    }

    @Test
    public void startup_rejectedThenExternallyRemovedFile_doesNotUnlockSession() throws Exception {
        Path data = testFolder.resolve("data.json");
        Files.writeString(data, "null");
        CountingStorage storage = new CountingStorage(data, testFolder.resolve("prefs.json"));
        new MainApp().initModelManager(storage, new UserPrefs());
        Files.delete(data);
        assertTrue(storage.readAddressBook().isEmpty());
        assertThrows(IOException.class, () -> storage.saveAddressBook(new AddressBook()));
        assertFalse(Files.exists(data));
    }

    private static class CountingStorage extends StorageManager {
        private int saveCalls;

        CountingStorage(Path data, Path prefs) {
            super(new JsonAddressBookStorage(data), new JsonUserPrefsStorage(prefs));
        }

        @Override
        public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
            saveCalls++;
            super.saveAddressBook(addressBook);
        }
    }
}
