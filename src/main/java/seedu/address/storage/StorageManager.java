package seedu.address.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.ReadOnlyUserPrefs;
import seedu.address.model.UserPrefs;

/**
 * Manages storage of AddressBook data in local storage.
 */
public class StorageManager implements Storage {

    private static final Logger logger = LogsCenter.getLogger(StorageManager.class);
    private JsonAddressBookStorage addressBookStorage;
    private JsonUserPrefsStorage userPrefsStorage;
    private String dataLoadError;

    /**
     * Creates a {@code StorageManager} with the given address book and user prefs storage.
     */
    public StorageManager(JsonAddressBookStorage addressBookStorage, JsonUserPrefsStorage userPrefsStorage) {
        this.addressBookStorage = addressBookStorage;
        this.userPrefsStorage = userPrefsStorage;
    }

    // ================ UserPrefs methods ==============================

    @Override
    public Path getUserPrefsFilePath() {
        return userPrefsStorage.getUserPrefsFilePath();
    }

    @Override
    public Optional<UserPrefs> readUserPrefs() throws DataLoadingException {
        return userPrefsStorage.readUserPrefs();
    }

    @Override
    public void saveUserPrefs(ReadOnlyUserPrefs userPrefs) throws IOException {
        userPrefsStorage.saveUserPrefs(userPrefs);
    }


    // ================ AddressBook methods ==============================

    @Override
    public Path getAddressBookFilePath() {
        return addressBookStorage.getAddressBookFilePath();
    }

    @Override
    public Optional<ReadOnlyAddressBook> readAddressBook() throws DataLoadingException {
        logger.fine("Attempting to read data from file: " + addressBookStorage.getAddressBookFilePath());
        try {
            return addressBookStorage.readAddressBook();
        } catch (DataLoadingException failure) {
            dataLoadError = "Could not load data at " + getAddressBookFilePath().toAbsolutePath()
                    + ". The file has not been changed. This session is protected: only help, list and exit"
                    + " are available, and the empty view is not your saved data. Close the app and back up"
                    + " the original data folder before recovery. Restore a known-good file compatible with"
                    + " this build or correct its JSON/access permissions, then restart. For a versioned file,"
                    + " use a compatible build on a separate working copy. Do not delete the original to"
                    + " bypass this protection. Cause: " + failure.getMessage();
            throw failure;
        }
    }

    @Override
    public Optional<String> getDataLoadError() {
        return Optional.ofNullable(dataLoadError);
    }

    @Override
    public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
        if (dataLoadError != null) {
            throw new IOException(dataLoadError);
        }
        logger.fine("Attempting to write to data file: " + addressBookStorage.getAddressBookFilePath());
        addressBookStorage.saveAddressBook(addressBook);
    }

}
