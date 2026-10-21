package ponhub;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ponhub.commons.core.GuiSettings;
import ponhub.model.ReadOnlyUserPrefs;
import ponhub.model.UserPrefs;
import ponhub.storage.JsonPonHubDataStorage;
import ponhub.storage.JsonUserPrefsStorage;
import ponhub.storage.ProtectedPonHubDataStorage.LoadStatus;
import ponhub.testutil.LessonQueryTestData;

/**
 * Exercises independent preference recovery through MainApp's initialization and shutdown boundaries.
 */
public class MainAppPreferencesTest {
    @TempDir
    Path directory;

    @Test
    public void initPrefs_missingFile_createsReloadableDefaults() throws Exception {
        JsonUserPrefsStorage storage = new JsonUserPrefsStorage(directory.resolve("prefs.json"));
        assertEquals(new UserPrefs(), new MainApp().initPrefs(storage));
        assertEquals(new UserPrefs(), storage.readUserPrefs().orElseThrow());
    }

    @Test
    public void initPrefs_corruptPreferences_savesReloadableDefaults() throws Exception {
        for (String document : new String[]{"null", "{\"guiSettings\":null}",
            "{\"guiSettings\":{\"windowWidth\":null}}", "[]", "{broken"}) {
            Path file = directory.resolve("prefs.json");
            Files.writeString(file, document);
            JsonUserPrefsStorage storage = new JsonUserPrefsStorage(file);
            assertEquals(new UserPrefs(), new MainApp().initPrefs(storage), document);
            assertEquals(new UserPrefs(), storage.readUserPrefs().orElseThrow(), document);
        }
    }

    @Test
    public void initPrefs_validPreferences_preservesSettingsAcrossInitialSave() throws Exception {
        JsonUserPrefsStorage storage = new JsonUserPrefsStorage(directory.resolve("prefs.json"));
        UserPrefs expected = new UserPrefs();
        expected.setGuiSettings(new GuiSettings(950, 700, -1000, -200));
        storage.saveUserPrefs(expected);
        assertEquals(expected, new MainApp().initPrefs(storage));
        assertEquals(expected, storage.readUserPrefs().orElseThrow());
    }

    @Test
    public void initPrefs_initialSaveFails_returnsLoadedOrDefaultPreferences() throws Exception {
        Path file = directory.resolve("prefs.json");
        FailingSaveStorage storage = new FailingSaveStorage(file);
        assertEquals(new UserPrefs(), new MainApp().initPrefs(storage));
        assertEquals(new UserPrefs(), storage.attemptedPrefs);
        assertFalse(Files.exists(file));

        UserPrefs expected = new UserPrefs();
        expected.setGuiSettings(new GuiSettings(910, 710, 21, 31));
        new JsonUserPrefsStorage(file).saveUserPrefs(expected);
        byte[] validOriginal = Files.readAllBytes(file);
        assertEquals(expected, new MainApp().initPrefs(storage));
        assertEquals(expected, storage.attemptedPrefs);
        assertArrayEquals(validOriginal, Files.readAllBytes(file));

        Files.writeString(file, "null");
        byte[] corruptOriginal = Files.readAllBytes(file);
        assertEquals(new UserPrefs(), new MainApp().initPrefs(storage));
        assertEquals(new UserPrefs(), storage.attemptedPrefs);
        assertArrayEquals(corruptOriginal, Files.readAllBytes(file));
        assertEquals(3, storage.saveAttempts);
    }

    @Test
    public void initialize_unreadablePreferencesAndFailedInitialSave_keepsCanonicalDataUsable() throws Exception {
        Path data = directory.resolve("data.json");
        var expected = LessonQueryTestData.createData().exportState();
        new JsonPonHubDataStorage(data).saveData(expected);
        byte[] original = Files.readAllBytes(data);
        Path prefs = Files.createDirectory(directory.resolve("prefs.json"));
        Path child = prefs.resolve("keep.txt");
        Files.writeString(child, "Keep this directory intact");
        MainApp app = new MainApp();

        assertDoesNotThrow(() -> app.initialize(data, prefs));
        assertEquals(new GuiSettings(), app.logic.getGuiSettings());
        assertEquals(LoadStatus.LOADED, app.canonicalSession.getLoadStatus());
        assertEquals(expected, app.canonicalSession.getDataSnapshot());
        app.logic.execute("help");
        app.logic.execute("list");
        assertTrue(app.logic.execute("exit").isExit());
        assertDoesNotThrow(app::stop);
        assertArrayEquals(original, Files.readAllBytes(data));
        assertEquals("Keep this directory intact", Files.readString(child));
    }

    @Test
    public void initialize_corruptPreferences_recoversWithoutChangingCanonicalBytes() throws Exception {
        Path data = directory.resolve("data.json");
        var expected = LessonQueryTestData.createData().exportState();
        new JsonPonHubDataStorage(data).saveData(expected);
        byte[] original = Files.readAllBytes(data);
        Path prefs = directory.resolve("prefs.json");
        for (String document : new String[]{"null", "{\"guiSettings\":null}",
            "{\"guiSettings\":{\"windowHeight\":null}}", "[]"}) {
            Files.writeString(prefs, document);
            MainApp app = new MainApp();
            app.initialize(data, prefs);
            assertEquals(new GuiSettings(), app.logic.getGuiSettings());
            assertEquals(expected, app.canonicalSession.getDataSnapshot());
            app.logic.execute("help");
            app.logic.execute("list");
            assertTrue(app.logic.execute("exit").isExit());
            app.stop();
            assertEquals(new UserPrefs(), new JsonUserPrefsStorage(prefs).readUserPrefs().orElseThrow());
            assertArrayEquals(original, Files.readAllBytes(data));
        }
    }

    @Test
    public void stop_preferenceSaveFails_preservesLoadedAndRejectedCanonicalBytes() throws Exception {
        Path data = directory.resolve("data.json");
        Path prefs = directory.resolve("prefs.json");
        for (boolean isValidData : new boolean[]{true, false}) {
            if (isValidData) {
                new JsonPonHubDataStorage(data).saveData(LessonQueryTestData.createData().exportState());
            } else {
                Files.writeString(data, "{preserve rejected operational bytes");
            }
            byte[] original = Files.readAllBytes(data);
            MainApp app = new MainApp();
            app.initialize(data, prefs);
            assertEquals(isValidData ? LoadStatus.LOADED : LoadStatus.CORRUPT,
                    app.canonicalSession.getLoadStatus());
            app.logic.setGuiSettings(new GuiSettings(910, 710, 21, 31));
            Files.delete(prefs);
            Files.createDirectory(prefs);
            Path child = prefs.resolve("keep.txt");
            Files.writeString(child, "preserve");

            app.logic.execute("help");
            app.logic.execute("list");
            assertTrue(app.logic.execute("exit").isExit());
            assertDoesNotThrow(app::stop);
            assertEquals(new GuiSettings(910, 710, 21, 31), app.logic.getGuiSettings());
            assertArrayEquals(original, Files.readAllBytes(data));
            assertEquals("preserve", Files.readString(child));
            Files.delete(child);
            Files.delete(prefs);
        }
    }

    @Test
    public void stop_preferenceSaveFails_doesNotCreateMissingCanonicalFile() throws Exception {
        Path data = directory.resolve("data.json");
        Path prefs = directory.resolve("prefs.json");
        MainApp app = new MainApp();
        app.initialize(data, prefs);
        assertEquals(LoadStatus.MISSING, app.canonicalSession.getLoadStatus());
        Files.delete(prefs);
        Files.createDirectory(prefs);
        Files.writeString(prefs.resolve("keep.txt"), "preserve");
        assertDoesNotThrow(app::stop);
        assertFalse(Files.exists(data));
        assertEquals("preserve", Files.readString(prefs.resolve("keep.txt")));
    }

    /**
     * Injects an initial write failure without OS-specific permission assumptions; reads use real JSON storage.
     */
    private static class FailingSaveStorage extends JsonUserPrefsStorage {
        private UserPrefs attemptedPrefs;
        private int saveAttempts;

        FailingSaveStorage(Path file) {
            super(file);
        }

        @Override
        public void saveUserPrefs(ReadOnlyUserPrefs prefs) throws IOException {
            saveAttempts++;
            attemptedPrefs = new UserPrefs(prefs);
            throw new IOException("Injected initial preference-save failure");
        }
    }
}
