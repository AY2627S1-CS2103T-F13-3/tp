package ponhub.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ponhub.commons.core.GuiSettings;
import ponhub.commons.exceptions.DataLoadingException;
import ponhub.model.UserPrefs;

public class JsonUserPrefsStorageTest {

    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonUserPrefsStorageTest");

    @TempDir
    public Path testFolder;

    @Test
    public void readUserPrefs_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> readUserPrefs(null));
    }

    private Optional<UserPrefs> readUserPrefs(String userPrefsFileInTestDataFolder) throws DataLoadingException {
        Path prefsFilePath = addToTestDataPathIfNotNull(userPrefsFileInTestDataFolder);
        return new JsonUserPrefsStorage(prefsFilePath).readUserPrefs(prefsFilePath);
    }

    @Test
    public void readUserPrefs_missingFile_emptyResult() throws DataLoadingException {
        assertFalse(readUserPrefs("NonExistentFile.json").isPresent());
    }

    @Test
    public void readUserPrefs_notJsonFormat_exceptionThrown() {
        assertThrows(DataLoadingException.class, () -> readUserPrefs("NotJsonFormatUserPrefs.json"));
    }

    private Path addToTestDataPathIfNotNull(String userPrefsFileInTestDataFolder) {
        return userPrefsFileInTestDataFolder != null
                ? TEST_DATA_FOLDER.resolve(userPrefsFileInTestDataFolder)
                : null;
    }

    @Test
    public void readUserPrefs_fileInOrder_successfullyRead() throws DataLoadingException {
        UserPrefs expected = getTypicalUserPrefs();
        UserPrefs actual = readUserPrefs("TypicalUserPref.json").get();
        assertEquals(expected, actual);
    }

    @Test
    public void readUserPrefs_valuesMissingFromFile_defaultValuesUsed() throws DataLoadingException {
        UserPrefs actual = readUserPrefs("EmptyUserPrefs.json").get();
        assertEquals(new UserPrefs(), actual);
    }

    @Test
    public void readUserPrefs_extraValuesInFile_extraValuesIgnored() throws DataLoadingException {
        UserPrefs expected = getTypicalUserPrefs();
        UserPrefs actual = readUserPrefs("ExtraValuesUserPref.json").get();

        assertEquals(expected, actual);
    }

    @Test
    public void readUserPrefs_hugeCoordinate_rejectsBeforeConversionWithoutChangingFile() throws Exception {
        Path file = testFolder.resolve("huge.json");
        String json = "{\"guiSettings\":{\"windowCoordinates\":{\"x\":" + "9".repeat(1_000_000) + ",\"y\":0}}}";
        Files.writeString(file, json);
        DataLoadingException error = assertThrows(DataLoadingException.class, () ->
                new JsonUserPrefsStorage(file).readUserPrefs());
        assertTrue(error.getMessage().contains("numeric token exceeds"));
        assertTrue(error.getMessage().length() < 200);
        assertEquals(json, Files.readString(file));
    }

    @Test
    public void readUserPrefs_longInvalidString_boundsWarningAndExceptionChain() throws Exception {
        Path file = testFolder.resolve("invalid-string.json");
        Files.writeString(file, "{\"guiSettings\":{\"windowWidth\":\"" + "x".repeat(10_000) + "\"}}");
        List<String> warnings = new ArrayList<>();
        Logger logger = ponhub.commons.core.LogsCenter.getLogger(ponhub.commons.util.JsonUtil.class);
        Handler recorder = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= java.util.logging.Level.WARNING.intValue()) {
                    warnings.add(record.getMessage());
                }
            }
            @Override
            public void flush() { }
            @Override
            public void close() { }
        };
        logger.addHandler(recorder);
        try {
            DataLoadingException error = assertThrows(DataLoadingException.class, () ->
                new JsonUserPrefsStorage(file).readUserPrefs());
            assertTrue(error.getMessage().length() < 600);
            assertTrue(error.getCause().getMessage().endsWith("[truncated]"));
            assertNull(error.getCause().getCause());
            assertEquals(1, warnings.size());
            assertTrue(warnings.get(0).length() < 1000);
        } finally {
            logger.removeHandler(recorder);
        }
    }

    @Test
    public void readUserPrefs_nonfiniteDimensionsAndExtremeCoordinates_keepExistingPolicy() throws Exception {
        Path file = testFolder.resolve("extreme.json");
        Files.writeString(file, "{\"guiSettings\":{\"windowWidth\":\"NaN\",\"windowHeight\":1e309,"
                + "\"windowCoordinates\":{\"x\":-2147483648,\"y\":2147483647}}}");
        GuiSettings actual = new JsonUserPrefsStorage(file).readUserPrefs().orElseThrow().getGuiSettings();
        assertEquals(new GuiSettings(Double.NaN, Double.POSITIVE_INFINITY,
                Integer.MIN_VALUE, Integer.MAX_VALUE), actual);
    }

    private UserPrefs getTypicalUserPrefs() {
        UserPrefs userPrefs = new UserPrefs();
        userPrefs.setGuiSettings(new GuiSettings(1000, 500, 300, 100));
        return userPrefs;
    }

    @Test
    public void savePrefs_nullPrefs_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveUserPrefs(null, "SomeFile.json"));
    }

    @Test
    public void saveUserPrefs_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveUserPrefs(new UserPrefs(), null));
    }

    /**
     * Saves {@code userPrefs} at the specified {@code prefsFileInTestDataFolder} filepath.
     */
    private void saveUserPrefs(UserPrefs userPrefs, String prefsFileInTestDataFolder) {
        try {
            new JsonUserPrefsStorage(addToTestDataPathIfNotNull(prefsFileInTestDataFolder))
                    .saveUserPrefs(userPrefs);
        } catch (IOException ioe) {
            throw new AssertionError("There should not be an error writing to the file", ioe);
        }
    }

    @Test
    public void saveUserPrefs_allInOrder_success() throws DataLoadingException, IOException {

        UserPrefs original = new UserPrefs();
        original.setGuiSettings(new GuiSettings(1200, 200, 0, 2));

        Path pefsFilePath = testFolder.resolve("nested/config/TempPrefs.json");
        JsonUserPrefsStorage jsonUserPrefsStorage = new JsonUserPrefsStorage(pefsFilePath);

        //Try writing when the file doesn't exist
        jsonUserPrefsStorage.saveUserPrefs(original);
        UserPrefs readBack = jsonUserPrefsStorage.readUserPrefs().get();
        assertEquals(original, readBack);

        //Try saving when the file exists
        original.setGuiSettings(new GuiSettings(5, 5, 5, 5));
        jsonUserPrefsStorage.saveUserPrefs(original);
        readBack = jsonUserPrefsStorage.readUserPrefs().get();
        assertEquals(original, readBack);
    }

}
