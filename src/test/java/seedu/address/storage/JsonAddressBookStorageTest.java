package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.HOON;
import static seedu.address.testutil.TypicalPersons.IDA;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.AddressBook;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

public class JsonAddressBookStorageTest {
    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonAddressBookStorageTest");

    @TempDir
    public Path testFolder;

    @Test
    public void read_missingTargetAtExistingPath_throwsOnEveryPlatform() throws Exception {
        Path path = testFolder.resolve("existing.json");
        Files.writeString(path, "{\"persons\":[]}");
        byte[] original = Files.readAllBytes(path);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(path) {
            @Override
            String readContents(Path file) throws IOException {
                // Exercise the existing-path branch even where real symbolic links are unavailable.
                throw new NoSuchFileException(file.toString());
            }
        };
        assertThrows(DataLoadingException.class, storage::readAddressBook);
        assertArrayEquals(original, Files.readAllBytes(path));
    }

    @Test
    public void read_invalidUtf8_throwsDataLoadingException() throws Exception {
        Path path = testFolder.resolve("invalid-utf8.json");
        Files.write(path, new byte[]{(byte) 0xc3, (byte) 0x28});
        assertThrows(DataLoadingException.class, () -> new JsonAddressBookStorage(path).readAddressBook());
    }

    @Test
    public void read_deniedAccess_throwsInsteadOfTreatingAsMissing() {
        Path path = testFolder.resolve("denied.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(path) {
            @Override
            String readContents(Path file) throws IOException {
                throw new AccessDeniedException(file.toString());
            }
        };
        assertThrows(DataLoadingException.class, storage::readAddressBook);
    }

    @Test
    public void read_unrelatedProgrammingFault_propagates() {
        JsonAddressBookStorage storage = new JsonAddressBookStorage(testFolder.resolve("data.json")) {
            @Override
            String readContents(Path file) {
                throw new IllegalStateException("Programming fault");
            }
        };
        assertThrows(IllegalStateException.class, storage::readAddressBook);
    }

    @Test
    public void read_directory_throwsInsteadOfTreatingAsMissing() {
        assertThrows(DataLoadingException.class, () -> new JsonAddressBookStorage(testFolder).readAddressBook());
    }

    @Test
    public void readAddressBook_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> readAddressBook(null));
    }

    private java.util.Optional<ReadOnlyAddressBook> readAddressBook(String filePath) throws Exception {
        return new JsonAddressBookStorage(Paths.get(filePath)).readAddressBook(addToTestDataPathIfNotNull(filePath));
    }

    private Path addToTestDataPathIfNotNull(String prefsFileInTestDataFolder) {
        return prefsFileInTestDataFolder != null
                ? TEST_DATA_FOLDER.resolve(prefsFileInTestDataFolder)
                : null;
    }

    @Test
    public void read_missingFile_emptyResult() throws Exception {
        assertFalse(readAddressBook("NonExistentFile.json").isPresent());
    }

    @Test
    public void read_notJsonFormat_exceptionThrown() {
        assertThrows(DataLoadingException.class, () -> readAddressBook("notJsonFormatAddressBook.json"));
    }

    @Test
    public void readAddressBook_invalidPersonAddressBook_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readAddressBook("invalidPersonAddressBook.json"));
    }

    @Test
    public void readAddressBook_invalidAndValidPersonAddressBook_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readAddressBook("invalidAndValidPersonAddressBook.json"));
    }

    @Test
    public void readAddressBook_largeSpaceRuns_normalizesContacts() throws Exception {
        String spaces = " ".repeat(100_000);
        Path filePath = testFolder.resolve("largeContacts.json");
        JsonAdaptedPerson person = new JsonAdaptedPerson(spaces + "Anne-Marie" + spaces + "O'Neil" + spaces,
                "00123456", "Anne+School@Example.COM", spaces + "Blk 10," + spaces + "#01-02" + spaces, List.of());
        JsonUtil.saveJsonFile(new JsonSerializableAddressBook(List.of(person)), filePath);
        Person expectedPerson = new PersonBuilder().withName("Anne-Marie O'Neil").withPhone("00123456")
                .withEmail("Anne+School@Example.COM").withAddress("Blk 10, #01-02").withTags().build();

        ReadOnlyAddressBook loaded = new JsonAddressBookStorage(filePath).readAddressBook().orElseThrow();
        assertEquals(List.of(expectedPerson), loaded.getPersonList());
    }

    @Test
    public void readAddressBook_largeInvalidContacts_throwDataLoadingException() throws Exception {
        String spaces = " ".repeat(100_000);
        List<JsonAdaptedPerson> invalidPersons = List.of(
                new JsonAdaptedPerson("Anne-Marie" + spaces + "O'Neil\t", "00123456", "anne@example.com",
                        "Blk 10", List.of()),
                new JsonAdaptedPerson("Anne-Marie", "00123456", "anne@example.com",
                        "Blk 10," + spaces + "#01-02\n", List.of()),
                new JsonAdaptedPerson("Anne-Marie", "00123456", "a.".repeat(5_000) + "a@example.com",
                        "Blk 10", List.of()));
        Path filePath = testFolder.resolve("largeInvalidContacts.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(filePath);
        for (JsonAdaptedPerson person : invalidPersons) {
            JsonUtil.saveJsonFile(new JsonSerializableAddressBook(List.of(person)), filePath);
            String originalJson = Files.readString(filePath);

            assertThrows(DataLoadingException.class, storage::readAddressBook);
            assertEquals(originalJson, Files.readString(filePath));
        }
    }

    @Test
    public void readAddressBook_oversizedEmails_throwsDataLoadingExceptionAndPreservesBytes() throws Exception {
        String[] emails = {
            "a.".repeat(5000) + "a@example.com",
            "a@" + "a-".repeat(5000) + "ab",
            "a@" + "a.".repeat(5000) + "ab",
            "a@" + "a".repeat(10000)
        };
        Path filePath = testFolder.resolve("long-email.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(filePath);
        for (String email : emails) {
            writePersonWithEmail(filePath, email);
            byte[] originalBytes = Files.readAllBytes(filePath);
            assertThrows(DataLoadingException.class, storage::readAddressBook);
            assertArrayEquals(originalBytes, Files.readAllBytes(filePath));
        }
    }

    @Test
    public void readAddressBook_longInvalidEmails_throwsDataLoadingExceptionAndPreservesBytes() throws Exception {
        String[] emails = {
            "a.".repeat(5000) + "a!@example.com",
            "a@" + "a-".repeat(5000) + "a",
            "a@" + "a.".repeat(5000) + "a"
        };
        Path filePath = testFolder.resolve("invalid-long-email.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(filePath);
        for (String email : emails) {
            writePersonWithEmail(filePath, email);
            byte[] originalBytes = Files.readAllBytes(filePath);
            assertThrows(DataLoadingException.class, storage::readAddressBook);
            assertArrayEquals(originalBytes, Files.readAllBytes(filePath));
        }
    }

    private void writePersonWithEmail(Path filePath, String email) throws IOException {
        Files.writeString(filePath, "{\"persons\":[{\"name\":\"Long Email\",\"phone\":\"123\",\"email\":\""
                + email + "\",\"address\":\"Somewhere\",\"tags\":[]}]}");
    }

    @Test
    public void readAndSaveAddressBook_allInOrder_success() throws Exception {
        Path filePath = testFolder.resolve("nested/data/TempAddressBook.json");
        AddressBook original = getTypicalAddressBook();
        JsonAddressBookStorage jsonAddressBookStorage = new JsonAddressBookStorage(filePath);

        // Save in new file and read back
        jsonAddressBookStorage.saveAddressBook(original, filePath);
        ReadOnlyAddressBook readBack = jsonAddressBookStorage.readAddressBook(filePath).get();
        assertEquals(original, new AddressBook(readBack));

        // Modify data, overwrite existing file, and read back
        original.addPerson(HOON);
        original.removePerson(ALICE);
        jsonAddressBookStorage.saveAddressBook(original, filePath);
        readBack = jsonAddressBookStorage.readAddressBook(filePath).get();
        assertEquals(original, new AddressBook(readBack));

        // Save and read without specifying file path
        original.addPerson(IDA);
        jsonAddressBookStorage.saveAddressBook(original); // file path not specified
        readBack = jsonAddressBookStorage.readAddressBook().get(); // file path not specified
        assertEquals(original, new AddressBook(readBack));

    }

    @Test
    public void saveAddressBook_nullAddressBook_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveAddressBook(null, "SomeFile.json"));
    }

    /**
     * Saves {@code addressBook} at the specified {@code filePath}.
     */
    private void saveAddressBook(ReadOnlyAddressBook addressBook, String filePath) {
        try {
            new JsonAddressBookStorage(Paths.get(filePath))
                    .saveAddressBook(addressBook, addToTestDataPathIfNotNull(filePath));
        } catch (IOException ioe) {
            throw new AssertionError("There should not be an error writing to the file.", ioe);
        }
    }

    @Test
    public void saveAddressBook_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveAddressBook(new AddressBook(), null));
    }
}
