package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.ReadOnlyAddressBook;

/**
 * A class to access AddressBook data stored as a JSON file on the hard disk.
 */
public class JsonAddressBookStorage {

    private static final Logger logger = LogsCenter.getLogger(JsonAddressBookStorage.class);

    private Path filePath;

    public JsonAddressBookStorage(Path filePath) {
        this.filePath = filePath;
    }

    public Path getAddressBookFilePath() {
        return filePath;
    }

    /**
     * Returns AddressBook data as a {@link ReadOnlyAddressBook}.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if loading the data from storage failed.
     */
    public Optional<ReadOnlyAddressBook> readAddressBook() throws DataLoadingException {
        return readAddressBook(filePath);
    }

    /**
     * Similar to {@link #readAddressBook()}.
     *
     * @param filePath location of the data. Cannot be null.
     * @throws DataLoadingException if loading the data from storage failed.
     */
    public Optional<ReadOnlyAddressBook> readAddressBook(Path filePath) throws DataLoadingException {
        requireNonNull(filePath);

        try {
            String contents = readContents(filePath);
            JsonDataVersionDetector.Format format = JsonDataVersionDetector.detect(contents);
            if (format != JsonDataVersionDetector.Format.LEGACY_AB3) {
                throw new IOException("Rejected data format: " + format
                        + ". This build supports only unversioned AB3 contact files.");
            }
            // Classify and decode the same contents; never reopen between these steps.
            return Optional.of(JsonUtil.fromJsonString(contents, JsonSerializableAddressBook.class).toModelType());
        } catch (NoSuchFileException missing) {
            if (Files.notExists(filePath, LinkOption.NOFOLLOW_LINKS)) {
                return Optional.empty();
            }
            throw new DataLoadingException(missing);
        } catch (IOException | IllegalValueException failure) {
            logger.info("Cannot load " + filePath + ": " + failure.getMessage());
            throw new DataLoadingException(failure);
        }
    }

    /** Reads the file without treating denied access or broken symbolic links as missing data. */
    String readContents(Path path) throws IOException {
        return Files.readString(path);
    }

    /**
     * Saves the given {@link ReadOnlyAddressBook} to the storage.
     * @param addressBook cannot be null.
     * @throws IOException if there was any problem writing to the file.
     */
    public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
        saveAddressBook(addressBook, filePath);
    }

    /**
     * Similar to {@link #saveAddressBook(ReadOnlyAddressBook)}.
     *
     * @param filePath location of the data. Cannot be null.
     */
    public void saveAddressBook(ReadOnlyAddressBook addressBook, Path filePath) throws IOException {
        requireNonNull(addressBook);
        requireNonNull(filePath);

        JsonUtil.saveJsonFile(new JsonSerializableAddressBook(addressBook), filePath);
    }

}
