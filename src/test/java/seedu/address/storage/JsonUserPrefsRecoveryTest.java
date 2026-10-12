package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Point;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.UserPrefs;

/**
 * Protects the preference loading contract for corrupt files and supported human edits.
 */
public class JsonUserPrefsRecoveryTest {
    @TempDir
    Path directory;

    @Test
    public void readUserPrefs_nullRoot_throwsDataLoadingExceptionWithoutWriting() throws Exception {
        assertRejected("null");
    }

    @Test
    public void readUserPrefs_invalidRootShapes_throwsDataLoadingExceptionWithoutWriting() throws Exception {
        for (String document : new String[]{"[]", "42", "true", "\"preferences\""}) {
            assertRejected(document);
        }
    }

    @Test
    public void readUserPrefs_nullGuiSettings_throwsDataLoadingExceptionWithoutWriting() throws Exception {
        assertRejected("{\"guiSettings\":null}");
    }

    @Test
    public void readUserPrefs_invalidGuiSettingsShapes_throwsDataLoadingExceptionWithoutWriting() throws Exception {
        for (String value : new String[]{"[]", "42", "true", "\"placement\""}) {
            assertRejected("{\"guiSettings\":" + value + "}");
        }
    }

    @Test
    public void readUserPrefs_nullDimensions_throwsDataLoadingExceptionWithoutWriting() throws Exception {
        for (String dimension : new String[]{"windowWidth", "windowHeight"}) {
            assertRejected("{\"guiSettings\":{\"" + dimension + "\":null}}");
        }
    }

    @Test
    public void readUserPrefs_nullCoordinateComponents_throwsDataLoadingExceptionWithoutWriting() throws Exception {
        for (String component : new String[]{"x", "y"}) {
            assertRejected("{\"guiSettings\":{\"windowCoordinates\":{\"" + component + "\":null}}}");
        }
    }

    @Test
    public void readUserPrefs_invalidDimensionShapes_throwsDataLoadingExceptionWithoutWriting() throws Exception {
        for (String value : new String[]{"{}", "[]", "\"wide\""}) {
            assertRejected("{\"guiSettings\":{\"windowWidth\":" + value + "}}");
        }
    }

    @Test
    public void readUserPrefs_missingFields_retainsDefaults() throws Exception {
        for (String document : new String[]{"{}", "{\"guiSettings\":{}}"}) {
            assertEquals(new UserPrefs(), readPreferences(document));
        }
        UserPrefs partial = readPreferences("{\"guiSettings\":{\"windowWidth\":900}}");
        assertEquals(900, partial.getGuiSettings().getWindowWidth());
        assertEquals(new GuiSettings().getWindowHeight(), partial.getGuiSettings().getWindowHeight());
    }

    @Test
    public void readUserPrefs_nullCoordinatesAndUnknownFields_preservesEditedDimensionsAcrossSave() throws Exception {
        UserPrefs prefs = readPreferences("""
                {"guiSettings":{"windowWidth":910,"windowHeight":710,"windowCoordinates":null,
                 "futureSetting":null},"extra":null}
                """);
        GuiSettings settings = prefs.getGuiSettings();
        assertEquals(910, settings.getWindowWidth());
        assertEquals(710, settings.getWindowHeight());
        assertNull(settings.getWindowCoordinates());
        JsonUserPrefsStorage storage = new JsonUserPrefsStorage(directory.resolve("prefs.json"));
        storage.saveUserPrefs(prefs);
        assertEquals(prefs, storage.readUserPrefs().orElseThrow());
    }

    @Test
    public void readUserPrefs_validSecondaryScreenPlacement_preservesCoordinatesAcrossSave() throws Exception {
        UserPrefs prefs = readPreferences("""
                {"guiSettings":{"windowWidth":950,"windowHeight":700,
                 "windowCoordinates":{"x":-1000,"y":-200}}}
                """);
        assertEquals(new GuiSettings(950, 700, -1000, -200), prefs.getGuiSettings());
        Point coordinates = prefs.getGuiSettings().getWindowCoordinates();
        coordinates.translate(100, 100);
        JsonUserPrefsStorage storage = new JsonUserPrefsStorage(directory.resolve("prefs.json"));
        storage.saveUserPrefs(prefs);
        assertEquals(new GuiSettings(950, 700, -1000, -200),
                storage.readUserPrefs().orElseThrow().getGuiSettings());
    }

    private UserPrefs readPreferences(String document) throws Exception {
        Path file = directory.resolve("prefs.json");
        Files.writeString(file, document);
        return new JsonUserPrefsStorage(file).readUserPrefs().orElseThrow();
    }

    private void assertRejected(String document) throws Exception {
        Path file = directory.resolve("prefs.json");
        Files.writeString(file, document);
        byte[] original = Files.readAllBytes(file);
        assertThrows(DataLoadingException.class, () -> new JsonUserPrefsStorage(file).readUserPrefs(), document);
        assertArrayEquals(original, Files.readAllBytes(file));
        try (var children = Files.list(directory)) {
            assertFalse(children.anyMatch(child -> !child.equals(file)));
        }
    }
}
