package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Address;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Phone;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/**
 * Exercises stricter contact rejection through the inherited startup, command and shutdown lifecycle.
 */
public class ContactValidationStartupTest {
    private static final String ADD_COMMAND = "add n/Alex p/91234567 e/alex@example.com a/Somewhere";

    @TempDir
    public Path testFolder;

    @Test
    public void startup_previouslyAcceptedContacts_preservesOriginals() throws Exception {
        String[][] rejectedRecords = {
            {personJson("123", "123", "alex@example.com", "Somewhere"), Name.MESSAGE_CONSTRAINTS},
            {personJson("Alex", "1234567890123456", "alex@example.com", "Somewhere"), Phone.MESSAGE_CONSTRAINTS},
            {personJson("Alex", "123", "alex@example.com", "Blk 10/Unit 2"), Address.MESSAGE_CONSTRAINTS},
            {personJson("Alex", "123", "alex@localhost", "Somewhere"), Email.MESSAGE_CONSTRAINTS},
            {personJson("Alex", "123", "a.".repeat(5000) + "a@example.com", "Somewhere"),
                Email.MESSAGE_CONSTRAINTS}
        };
        for (String[] rejectedRecord : rejectedRecords) {
            assertProtectedLifecycle("{\"persons\":[" + rejectedRecord[0] + "]}", rejectedRecord[1]);
        }
    }

    @Test
    public void startup_normalizationCreatesDuplicateIdentity_preservesOriginals() throws Exception {
        String document = "{\"persons\":["
                + personJson("Alex  Tan", "123", "alex@example.com", "Somewhere") + ","
                + personJson("Alex Tan", "456", "other@example.com", "Elsewhere") + "]}";
        assertProtectedLifecycle(document, "Persons list contains duplicate person(s).");
    }

    private void assertProtectedLifecycle(String document, String expectedCause) throws Exception {
        Path data = testFolder.resolve("data.json");
        Files.writeString(data, document);
        byte[] originalBytes = Files.readAllBytes(data);
        StorageManager storage = new StorageManager(new JsonAddressBookStorage(data),
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));
        MainApp app = new MainApp();
        Model model = app.initModelManager(storage, new UserPrefs());
        app.storage = storage;
        app.model = model;
        LogicManager logic = new LogicManager(model, storage);

        String recoveryMessage = storage.getDataLoadError().orElseThrow();
        assertTrue(recoveryMessage.contains(expectedCause));
        assertTrue(recoveryMessage.contains(data.toAbsolutePath().toString()));
        assertTrue(recoveryMessage.contains("back up"));
        assertTrue(recoveryMessage.contains("restart"));
        assertEquals(new AddressBook(), model.getAddressBook());
        logic.execute("help");
        logic.execute("list");
        assertTrue(logic.execute("exit").isExit());
        for (String command : new String[]{ADD_COMMAND, "delete 1"}) {
            CommandException failure = assertThrows(CommandException.class, () -> logic.execute(command));
            assertEquals(recoveryMessage, failure.getMessage());
        }
        assertThrows(IOException.class, () -> storage.saveAddressBook(model.getAddressBook()));
        assertEquals(new AddressBook(), model.getAddressBook());
        app.stop();
        assertTrue(storage.readUserPrefs().isPresent());
        assertArrayEquals(originalBytes, Files.readAllBytes(data));

        StorageManager restartedStorage = new StorageManager(new JsonAddressBookStorage(data),
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));
        Model restartedModel = new MainApp().initModelManager(restartedStorage, new UserPrefs());
        assertTrue(restartedStorage.getDataLoadError().isPresent());
        assertEquals(new AddressBook(), restartedModel.getAddressBook());
        assertArrayEquals(originalBytes, Files.readAllBytes(data));

        Files.writeString(data, "{\"persons\":["
                + personJson("Anne-Marie  O'Neil", "00123456", "Anne%School@Example.COM", "Blk 10,  #01-02") + "]}");
        StorageManager repairedStorage = new StorageManager(new JsonAddressBookStorage(data),
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));
        Model repairedModel = new MainApp().initModelManager(repairedStorage, new UserPrefs());
        assertTrue(repairedStorage.getDataLoadError().isEmpty());
        assertEquals("Anne-Marie O'Neil", repairedModel.getAddressBook().getPersonList().getFirst().getName().fullName);
        assertEquals("00123456", repairedModel.getAddressBook().getPersonList().getFirst().getPhone().value);
        assertEquals("Blk 10, #01-02", repairedModel.getAddressBook().getPersonList().getFirst().getAddress().value);
        new LogicManager(repairedModel, repairedStorage).execute(ADD_COMMAND);
        assertFalse(repairedStorage.getDataLoadError().isPresent());
        assertEquals(repairedModel.getAddressBook(), repairedStorage.readAddressBook().orElseThrow());
    }

    private String personJson(String name, String phone, String email, String address) {
        return "{\"name\":\"" + name + "\",\"phone\":\"" + phone + "\",\"email\":\"" + email
                + "\",\"address\":\"" + address + "\",\"tags\":[]}";
    }
}
