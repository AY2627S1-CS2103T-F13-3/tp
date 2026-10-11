package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.PonHubData;
import seedu.address.model.person.Address;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Phone;
import seedu.address.storage.JsonUserPrefsStorage;

/**
 * Exercises stricter contact rejection through the active canonical startup, command and shutdown lifecycle.
 */
public class ContactValidationStartupTest {
    private static final String ADD_COMMAND = "add r/parent n/Alex p/91234567 e/alex@example.com a/Somewhere";

    @TempDir
    Path testFolder;

    @Test
    public void startup_previouslyAcceptedContacts_preservesOriginals() throws Exception {
        String[][] rejectedRecords = {
            {personJson("P1", "123", "123", "alex@example.com", "Somewhere"), Name.MESSAGE_CONSTRAINTS},
            {personJson("P1", "Alex", "1234567890123456", "alex@example.com", "Somewhere"),
                Phone.MESSAGE_CONSTRAINTS},
            {personJson("P1", "Alex", "123", "alex@example.com", "Blk 10/Unit 2"), Address.MESSAGE_CONSTRAINTS},
            {personJson("P1", "Alex", "123", "alex@localhost", "Somewhere"), Email.MESSAGE_CONSTRAINTS},
            {personJson("P1", "Alex", "123", "a.".repeat(5000) + "a@example.com", "Somewhere"),
                Email.MESSAGE_CONSTRAINTS}
        };
        for (String[] rejectedRecord : rejectedRecords) {
            assertProtectedLifecycle(versionedPeopleJson(rejectedRecord[0]), rejectedRecord[1]);
        }
    }

    @Test
    public void startup_normalizationCreatesDuplicateIdentity_preservesOriginals() throws Exception {
        String document = versionedPeopleJson(
                personJson("P1", "Alex  Tan", "123", "alex@example.com", "Somewhere"),
                personJson("P2", "Alex Tan", "123", "other@example.com", "Elsewhere"));
        assertProtectedLifecycle(document, "Operation would result in duplicate persons");
    }

    private void assertProtectedLifecycle(String document, String expectedCause) throws Exception {
        Path data = testFolder.resolve("data.json");
        Path prefs = testFolder.resolve("prefs.json");
        Files.writeString(data, document);
        byte[] originalBytes = Files.readAllBytes(data);
        MainApp app = new MainApp();
        app.initialize(data, prefs);

        String recoveryMessage = app.canonicalSession.getDataLoadError().orElseThrow();
        assertTrue(recoveryMessage.contains(expectedCause));
        assertTrue(recoveryMessage.contains(data.toAbsolutePath().toString()));
        assertTrue(recoveryMessage.toLowerCase(Locale.ROOT).contains("back up"));
        assertTrue(recoveryMessage.contains("restart"));
        assertEquals(new PonHubData().exportState(), app.canonicalSession.getDataSnapshot());
        app.logic.execute("help");
        app.logic.execute("list");
        assertTrue(app.logic.execute("exit").isExit());
        for (String command : new String[]{ADD_COMMAND, "delete 1"}) {
            CommandException failure = assertThrows(CommandException.class, () -> app.logic.execute(command));
            assertEquals(recoveryMessage, failure.getMessage());
        }
        app.stop();
        assertTrue(new JsonUserPrefsStorage(prefs).readUserPrefs().isPresent());
        assertArrayEquals(originalBytes, Files.readAllBytes(data));

        MainApp restarted = new MainApp();
        restarted.initialize(data, prefs);
        assertTrue(restarted.canonicalSession.getDataLoadError().isPresent());
        assertEquals(new PonHubData().exportState(), restarted.canonicalSession.getDataSnapshot());
        restarted.stop();
        assertArrayEquals(originalBytes, Files.readAllBytes(data));

        Files.writeString(data, versionedPeopleJson(personJson("P1", "Anne-Marie  O'Neil", "00123456",
                "Anne%School@Example.COM", "Blk 10,  #01-02")));
        MainApp repaired = new MainApp();
        repaired.initialize(data, prefs);
        assertTrue(repaired.canonicalSession.getDataLoadError().isEmpty());
        var contact = repaired.logic.getPersonRecords().getFirst().getContactDetails();
        assertEquals("Anne-Marie O'Neil", contact.getName().fullName);
        assertEquals("00123456", contact.getPhone().orElseThrow().value);
        assertEquals("Blk 10, #01-02", contact.getAddress().orElseThrow().value);
        repaired.logic.execute(ADD_COMMAND);
        repaired.stop();
        assertFalse(repaired.canonicalSession.getDataLoadError().isPresent());
        MainApp reloaded = new MainApp();
        reloaded.initialize(data, prefs);
        assertEquals(repaired.canonicalSession.getDataSnapshot(), reloaded.canonicalSession.getDataSnapshot());
    }

    private String personJson(String id, String name, String phone, String email, String address) {
        return "{\"id\":\"" + id + "\",\"role\":\"PARENT\",\"name\":\"" + name + "\",\"phone\":\"" + phone
                + "\",\"email\":\"" + email + "\",\"address\":\"" + address + "\"}";
    }

    private String versionedPeopleJson(String... people) {
        return "{\"schemaVersion\":1,\"people\":[" + String.join(",", people) + "],"
                + "\"personCounters\":{\"STUDENT\":0,\"TUTOR\":0,\"PARENT\":" + people.length + "},"
                + "\"lastAllocatedLessonSequence\":0,\"lessons\":[],\"attendance\":[]}";
    }
}
