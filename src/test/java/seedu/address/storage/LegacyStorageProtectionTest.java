package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.PonHubData;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;

/**
 * Guards the retained legacy persistence boundary until its planned removal.
 */
public class LegacyStorageProtectionTest {
    private static final String REJECTED_DATA = "{\"persons\":[null]}\n";

    @TempDir
    public Path folder;

    private Path dataFile;
    private Path prefsFile;
    private CountingAddressBookStorage addressBookStorage;
    private StorageManager storage;

    @BeforeEach
    public void setUp() {
        dataFile = folder.resolve("addressbook.json");
        prefsFile = folder.resolve("preferences.json");
        addressBookStorage = new CountingAddressBookStorage(dataFile);
        storage = new StorageManager(addressBookStorage, new JsonUserPrefsStorage(prefsFile));
    }

    @Test
    public void read_rejectedData_blocksBothSaveApisAndPreservesBytes() throws Exception {
        String guidance = rejectRead();

        IOException directFailure = assertThrows(IOException.class, () ->
                storage.saveAddressBook(getTypicalAddressBook()));
        IOException modelFailure = assertThrows(IOException.class, () ->
                storage.saveModel(new ModelManager(getTypicalAddressBook(), new UserPrefs())));

        assertEquals(guidance, directFailure.getMessage());
        assertEquals(guidance, modelFailure.getMessage());
        assertEquals(0, addressBookStorage.saveCalls);
        assertArrayEquals(REJECTED_DATA.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(dataFile));
    }

    @Test
    public void read_externallyRepairedFile_doesNotUnlockSession() throws Exception {
        String guidance = rejectRead();
        AddressBook repaired = getTypicalAddressBook();
        new JsonAddressBookStorage(dataFile).saveAddressBook(repaired);
        byte[] repairedBytes = Files.readAllBytes(dataFile);

        assertEquals(repaired, storage.readAddressBook().orElseThrow());
        assertEquals(guidance, storage.getDataLoadError().orElseThrow());
        assertEquals(guidance, assertThrows(IOException.class, () ->
                storage.saveAddressBook(new AddressBook())).getMessage());
        assertEquals(0, addressBookStorage.saveCalls);
        assertArrayEquals(repairedBytes, Files.readAllBytes(dataFile));
    }

    @Test
    public void read_externallyRemovedFile_doesNotUnlockOrRecreateData() throws Exception {
        String guidance = rejectRead();
        Files.delete(dataFile);

        assertTrue(storage.readAddressBook().isEmpty());
        assertEquals(guidance, storage.getDataLoadError().orElseThrow());
        assertEquals(guidance, assertThrows(IOException.class, () ->
                storage.saveAddressBook(getTypicalAddressBook())).getMessage());
        assertEquals(0, addressBookStorage.saveCalls);
        assertFalse(Files.exists(dataFile));
    }

    @Test
    public void saveUserPrefs_protectedSession_roundTripsWithoutChangingOperationalData() throws Exception {
        String guidance = rejectRead();
        UserPrefs prefs = new UserPrefs();
        prefs.setGuiSettings(new GuiSettings(800, 600, 20, 30));

        assertTrue(storage.readUserPrefs().isEmpty());
        storage.saveUserPrefs(prefs);

        assertEquals(prefsFile, storage.getUserPrefsFilePath());
        assertEquals(prefs, storage.readUserPrefs().orElseThrow());
        assertEquals(prefs, new JsonUserPrefsStorage(prefsFile).readUserPrefs().orElseThrow());
        assertEquals(dataFile, storage.getAddressBookFilePath());
        assertEquals(guidance, storage.getDataLoadError().orElseThrow());
        assertThrows(IOException.class, () -> storage.saveAddressBook(getTypicalAddressBook()));
        assertEquals(0, addressBookStorage.saveCalls);
        assertArrayEquals(REJECTED_DATA.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(dataFile));
    }

    @Test
    public void decode_nullPersonRecord_rejectsAtCodecAndFileBoundary() throws Exception {
        JsonSerializableAddressBook decoded = JsonUtil.fromJsonString(REJECTED_DATA,
                JsonSerializableAddressBook.class);
        String message = "Persons list contains a null record.";

        assertEquals(message, assertThrows(IllegalValueException.class, decoded::toModelType).getMessage());
        assertRejectedFile(REJECTED_DATA, IllegalValueException.class, message);
    }

    @Test
    public void decode_nullTagEntry_rejectsAtCodecAndFileBoundary() throws Exception {
        String personJson = "{\"name\":\"Amy Bee\",\"phone\":\"12345678\","
                + "\"email\":\"amy@example.com\",\"address\":\"Somewhere\",\"tags\":[\"friend\",null]}";
        JsonAdaptedPerson decoded = JsonUtil.fromJsonString(personJson, JsonAdaptedPerson.class);
        String message = "Person's tags contain a null entry.";

        assertEquals(message, assertThrows(IllegalValueException.class, decoded::toModelType).getMessage());
        assertRejectedFile("{\"persons\":[" + personJson + "]}\n", IllegalValueException.class, message);
    }

    @Test
    public void read_versionedCanonicalData_rejectsWithoutChangingBytes() throws Exception {
        var state = new PonHubData().exportState();
        String canonicalJson = JsonPonHubDataCodec.encode(state);
        assertEquals(state, JsonPonHubDataCodec.decode(canonicalJson));

        assertRejectedFile(canonicalJson, IOException.class,
                "Rejected data format: PONHUB_V1_CANDIDATE. This build supports only unversioned AB3 contact files.");
    }

    @Test
    public void read_versionedLegacyLookingData_rejectsVersionBeforeDecodingPersons() throws Exception {
        for (String version : new String[]{"1", "2", "null", "\"1\""}) {
            String json = "{\"schemaVersion\":" + version + ",\"persons\":[]}\n";
            String format = switch (version) {
                case "1" -> "PONHUB_V1_CANDIDATE";
                case "2" -> "UNSUPPORTED_VERSION";
                default -> "INVALID_VERSION";
            };
            assertRejectedFile(json, IOException.class, "Rejected data format: " + format
                    + ". This build supports only unversioned AB3 contact files.");
        }
    }

    /**
     * Rejects a real legacy file and returns the session's recovery guidance.
     */
    private String rejectRead() throws Exception {
        Files.writeString(dataFile, REJECTED_DATA);
        assertTrue(storage.getDataLoadError().isEmpty());
        DataLoadingException failure = assertThrows(DataLoadingException.class, storage::readAddressBook);
        assertInstanceOf(IllegalValueException.class, failure.getCause());
        String guidance = storage.getDataLoadError().orElseThrow();
        assertTrue(guidance.contains(dataFile.toAbsolutePath().toString()));
        assertTrue(guidance.contains("This session is protected"));
        assertTrue(guidance.contains(failure.getMessage()));
        return guidance;
    }

    /**
     * Verifies controlled rejection at the public file boundary and exact byte preservation.
     */
    private void assertRejectedFile(String json, Class<? extends Exception> causeType, String message)
            throws Exception {
        Files.writeString(dataFile, json);
        byte[] original = Files.readAllBytes(dataFile);

        DataLoadingException failure = assertThrows(DataLoadingException.class, addressBookStorage::readAddressBook);

        assertInstanceOf(causeType, failure.getCause());
        assertEquals(message, failure.getCause().getMessage());
        assertArrayEquals(original, Files.readAllBytes(dataFile));
    }

    /**
     * Counts delegated writes so rejected saves cannot reach the legacy writer.
     */
    private static class CountingAddressBookStorage extends JsonAddressBookStorage {
        private int saveCalls;

        CountingAddressBookStorage(Path path) {
            super(path);
        }

        @Override
        public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
            saveCalls++;
            super.saveAddressBook(addressBook);
        }
    }
}
