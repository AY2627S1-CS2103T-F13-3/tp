package ponhub;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ponhub.commons.core.GuiSettings;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.model.PonHubData;
import ponhub.model.person.PersonRole;
import ponhub.storage.JsonPonHubDataCodec;
import ponhub.storage.JsonUserPrefsStorage;
import ponhub.storage.ProtectedPonHubDataStorage;

/**
 * Exercises the active application's canonical startup, command and shutdown boundaries together.
 */
public class ProtectedStartupTest {
    private static final String ADD_COMMAND = "add r/parent n/Alex p/91234567 e/alex@example.com a/Somewhere";

    @TempDir
    Path testFolder;

    @Test
    public void startup_rejectedData_preservesBytesAcrossCommandsShutdownAndRestart() throws Exception {
        String valid = JsonPonHubDataCodec.encode(new PonHubData().exportState());
        String[] documents = {
            "{broken", "", "null", "[]", "{}", "{\"persons\":null}", "{\"persons\":{}}",
            "{\"persons\":[null]}", "{\"persons\":[42]}", "{\"persons\":[{}]}",
            "{\"persons\":[]}", "{\"persons\":[],\"lessons\":[]}",
            "{\"schemaVersion\":99,\"people\":[],\"lessons\":[]}",
            "{\"schemaVersion\":1,\"persons\":[]}", "{\"schemaVersion\":null,\"persons\":[]}",
            valid.replace("\"people\" : [ ]", "\"people\" : [null]"),
            valid.replace("\"people\" : [ ]", "\"people\" : [42]"),
            valid.replace("\"STUDENT\" : 0", "\"STUDENT\" : -1"),
            valid.replace("\"attendance\" : [ ]", "\"attendance\" : [{\"date\":\"2026-02-30\"}]"),
            valid + " {}", "{\"schemaVersion\":1,\"schemaVersion\":1}"
        };
        for (String document : documents) {
            Path data = testFolder.resolve("data.json");
            Files.writeString(data, document);
            byte[] original = Files.readAllBytes(data);
            MainApp app = startSession(data);

            assertProtectedCommands(app);
            app.stop();
            assertTrue(new JsonUserPrefsStorage(testFolder.resolve("prefs.json")).readUserPrefs().isPresent());
            assertArrayEquals(original, Files.readAllBytes(data), document);
            MainApp restarted = startSession(data);
            assertProtectedCommands(restarted);
            restarted.stop();
            assertArrayEquals(original, Files.readAllBytes(data), document);
        }
    }

    @Test
    public void startup_directoryPath_preservesContentsAndAllowsExit() throws Exception {
        Path data = testFolder.resolve("directory instead of file");
        Files.createDirectory(data);
        Path child = data.resolve("keep.txt");
        Files.writeString(child, "preserve");
        MainApp app = startSession(data);

        assertEquals(ProtectedPonHubDataStorage.LoadStatus.UNREADABLE, app.canonicalSession.getLoadStatus());
        assertProtectedCommands(app);
        app.stop();
        assertTrue(Files.isDirectory(data));
        assertEquals("preserve", Files.readString(child));
    }

    @Test
    public void startup_missingFile_startsEmptyAndWritesOnlyOnMutation() throws Exception {
        Path data = testFolder.resolve("new folder/new.json");
        MainApp app = startSession(data);

        assertEquals(ProtectedPonHubDataStorage.LoadStatus.MISSING, app.canonicalSession.getLoadStatus());
        assertTrue(app.logic.getPersonRecords().isEmpty());
        app.logic.execute("help");
        app.logic.execute("list");
        assertTrue(app.logic.execute("exit").isExit());
        app.stop();
        assertFalse(Files.exists(data));

        MainApp writable = startSession(data);
        writable.logic.execute(ADD_COMMAND);
        assertEquals(1, writable.logic.getPersonRecords().size());
        writable.stop();
        MainApp restarted = startSession(data);
        assertEquals(writable.canonicalSession.getDataSnapshot(), restarted.canonicalSession.getDataSnapshot());
        assertEquals("P1", restarted.logic.getPersonRecords().getFirst().getId().toString());
    }

    @Test
    public void startup_validHumanEditedVersionedFile_loadsAndRemainsWritable() throws Exception {
        Path data = testFolder.resolve("edited.json");
        String edited = JsonPonHubDataCodec.encode(new PonHubData().exportState())
                .replace("\"PARENT\" : 0", "\"PARENT\" : 7");
        Files.writeString(data, edited);
        MainApp app = startSession(data);

        assertTrue(app.canonicalSession.getDataLoadError().isEmpty());
        assertEquals(7L, app.canonicalSession.getDataSnapshot().people().getLastAllocatedSequences()
                .get(PersonRole.PARENT));
        app.logic.execute("list r/parent");
        assertEquals(edited, Files.readString(data));
        app.logic.execute(ADD_COMMAND);
        app.stop();
        assertEquals("P8", app.logic.getPersonRecords().getFirst().getId().toString());
        assertEquals(app.canonicalSession.getDataSnapshot(), startSession(data).canonicalSession.getDataSnapshot());
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
        MainApp app = startSession(data);

        assertProtectedCommands(app);
        app.stop();
        assertTrue(Files.isSymbolicLink(data));
        assertEquals(target.getFileName(), Files.readSymbolicLink(data));
        assertTrue(Files.notExists(target, LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    public void startup_repairedFile_staysLockedUntilNewApplicationSession() throws Exception {
        Path data = testFolder.resolve("data.json");
        Files.writeString(data, "{broken");
        MainApp app = startSession(data);
        String originalError = app.canonicalSession.getDataLoadError().orElseThrow();
        String repaired = JsonPonHubDataCodec.encode(new PonHubData().exportState());
        Files.writeString(data, repaired);

        assertProtectedCommands(app);
        assertEquals(originalError, app.canonicalSession.getDataLoadError().orElseThrow());
        app.stop();
        assertEquals(repaired, Files.readString(data));
        MainApp restarted = startSession(data);
        assertTrue(restarted.canonicalSession.getDataLoadError().isEmpty());
        assertEquals(ProtectedPonHubDataStorage.LoadStatus.LOADED, restarted.canonicalSession.getLoadStatus());
        restarted.logic.execute(ADD_COMMAND);
        assertEquals(1, startSession(data).logic.getPersonRecords().size());
    }

    @Test
    public void startup_rejectedThenExternallyRemovedFile_doesNotUnlockSession() throws Exception {
        Path data = testFolder.resolve("data.json");
        Files.writeString(data, "null");
        MainApp app = startSession(data);
        Files.delete(data);

        assertProtectedCommands(app);
        app.stop();
        assertFalse(Files.exists(data));
    }

    @Test
    public void startup_defaultAndAlternateCanonicalPaths_keepLegacyStoreUntouched() throws Exception {
        Path legacy = testFolder.resolve("data/addressbook.json");
        Files.createDirectories(legacy.getParent());
        Files.writeString(legacy, "{\"persons\":[]}\n");
        byte[] original = Files.readAllBytes(legacy);
        Path defaultData = MainApp.selectDataPath(Map.of(), testFolder);
        assertEquals(testFolder.toRealPath().resolve("data/ponhub.json"), defaultData);
        MainApp defaultApp = startSession(defaultData);
        defaultApp.logic.execute(ADD_COMMAND);
        defaultApp.stop();
        byte[] canonical = Files.readAllBytes(defaultData);
        Path alternate = testFolder.resolve("store with spaces/selected.json");
        assertEquals(alternate, MainApp.selectDataPath(Map.of("data", alternate.toString()), testFolder));
        MainApp alternateApp = startSession(MainApp.selectDataPath(
                Map.of("data", "store with spaces/selected.json"), testFolder));

        assertTrue(alternateApp.logic.getPersonRecords().isEmpty());
        alternateApp.logic.execute("add r/student n/Anne l/P1 pp/81234567");
        alternateApp.stop();
        assertArrayEquals(original, Files.readAllBytes(legacy));
        assertArrayEquals(canonical, Files.readAllBytes(defaultData));
        assertEquals("S1", startSession(alternate).logic.getPersonRecords().getFirst().getId().toString());
        MainApp rejectedLegacy = startSession(MainApp.selectDataPath(Map.of("data", legacy.toString()), testFolder));
        assertEquals(ProtectedPonHubDataStorage.LoadStatus.LEGACY, rejectedLegacy.canonicalSession.getLoadStatus());
        assertTrue(rejectedLegacy.canonicalSession.getDataLoadError().orElseThrow().contains("manually re-enter"));
        assertProtectedCommands(rejectedLegacy);
        rejectedLegacy.stop();
        assertArrayEquals(original, Files.readAllBytes(legacy));
    }

    @Test
    public void startup_rejectedData_preferencesSaveAndReloadIndependently() throws Exception {
        Path data = testFolder.resolve("rejected.json");
        Files.writeString(data, "broken original");
        byte[] original = Files.readAllBytes(data);
        MainApp app = startSession(data);
        GuiSettings settings = new GuiSettings(900, 700, 20, 30);
        app.logic.setGuiSettings(settings);
        app.stop();

        MainApp restarted = startSession(data);
        assertEquals(settings, restarted.logic.getGuiSettings());
        assertProtectedCommands(restarted);
        restarted.stop();
        assertArrayEquals(original, Files.readAllBytes(data));
    }

    @Test
    public void startup_dataAndPreferencesAlias_rejectsBeforeEitherFileIsWritten() throws Exception {
        Path data = testFolder.resolve("data.json");
        Files.writeString(data, "preserve operational bytes");
        byte[] original = Files.readAllBytes(data);
        MainApp app = new MainApp();

        assertThrows(IllegalArgumentException.class, () -> app.initialize(data,
                testFolder.resolve("child/../data.json")));
        assertArrayEquals(original, Files.readAllBytes(data));
    }

    @Test
    public void startup_existingSymbolicLinkAlias_preservesOriginals() throws Exception {
        Path data = testFolder.resolve("data.json");
        Files.writeString(data, "preserve operational bytes");
        byte[] original = Files.readAllBytes(data);
        Path alias = testFolder.resolve("prefs-link.json");
        try {
            Files.createSymbolicLink(alias, data.getFileName());
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(false, "Symbolic links unavailable: " + exception);
        }
        assertThrows(IllegalArgumentException.class, () -> new MainApp().initialize(data, alias));
        assertArrayEquals(original, Files.readAllBytes(data));
    }

    @Test
    public void startup_dataAndPreferencesHardLinkAlias_preservesOriginalBytes() throws Exception {
        Path data = testFolder.resolve("canonical.json");
        Files.writeString(data, "preserve operational bytes");
        byte[] original = Files.readAllBytes(data);
        Path prefs = testFolder.resolve("preferences.json");
        try {
            Files.createLink(prefs, data);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(false, "Hard links unavailable: " + exception);
        }

        assertThrows(IllegalArgumentException.class, () -> new MainApp().initialize(data, prefs));
        assertArrayEquals(original, Files.readAllBytes(data));
        assertArrayEquals(original, Files.readAllBytes(prefs));
    }

    @Test
    public void startup_danglingAlias_rejectsBeforeCreatingDataOrPreferences() throws Exception {
        Path data = testFolder.resolve("data.json");
        Path prefs = testFolder.resolve("prefs.json");
        try {
            Files.createSymbolicLink(data, prefs.getFileName());
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(false, "Symbolic links unavailable: " + exception);
        }
        assertThrows(IllegalArgumentException.class, () -> new MainApp().initialize(data, prefs));
        assertTrue(Files.isSymbolicLink(data));
        assertFalse(Files.exists(prefs));

        Files.delete(data);
        Files.createSymbolicLink(prefs, data.getFileName());
        assertThrows(IllegalArgumentException.class, () -> new MainApp().initialize(data, prefs));
        assertTrue(Files.isSymbolicLink(prefs));
        assertFalse(Files.exists(data));
    }

    @Test
    public void startup_missingFilesUnderAliasedParents_rejectsBeforeCreatingFiles() throws Exception {
        Path realDirectory = testFolder.resolve("real");
        Files.createDirectory(realDirectory);
        Path aliasDirectory = testFolder.resolve("alias");
        try {
            Files.createSymbolicLink(aliasDirectory, realDirectory.getFileName());
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(false, "Symbolic links unavailable: " + exception);
        }
        Path data = realDirectory.resolve("new.json");
        Path prefs = aliasDirectory.resolve("new.json");

        assertThrows(IllegalArgumentException.class, () -> new MainApp().initialize(data, prefs));
        assertFalse(Files.exists(data));
        assertFalse(Files.exists(prefs));
    }

    @Test
    public void selectDataPath_blankArgument_rejectsBeforeStartup() {
        assertThrows(IllegalArgumentException.class, () -> MainApp.selectDataPath(Map.of("data", ""), testFolder));
        assertThrows(IllegalArgumentException.class, () -> MainApp.selectDataPath(Map.of("data", "   "), testFolder));
    }

    @Test
    public void selectDataPath_outsideApplicationDirectory_rejectsWithoutWriting() throws Exception {
        Path home = testFolder.resolve("app");
        Files.createDirectory(home);
        Path outside = testFolder.resolve("outside.json");
        Files.writeString(outside, "preserve");

        assertThrows(IllegalArgumentException.class, () -> MainApp.selectDataPath(
                Map.of("data", "../outside.json"), home));
        assertThrows(IllegalArgumentException.class, () -> MainApp.selectDataPath(
                Map.of("data", outside.toString()), home));
        assertEquals("preserve", Files.readString(outside));
    }

    @Test
    public void selectDataPath_symbolicLinkEscape_rejectsExistingAndMissingTargets() throws Exception {
        Path home = testFolder.resolve("app");
        Files.createDirectory(home);
        Path outside = testFolder.resolve("outside");
        Files.createDirectory(outside);
        Files.writeString(outside.resolve("existing.json"), "preserve");
        try {
            Files.createSymbolicLink(home.resolve("escape"), outside);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(false, "Symbolic links unavailable: " + exception);
        }

        assertThrows(IllegalArgumentException.class, () -> MainApp.selectDataPath(
                Map.of("data", "escape/existing.json"), home));
        assertThrows(IllegalArgumentException.class, () -> MainApp.selectDataPath(
                Map.of("data", "escape/new.json"), home));
        assertEquals("preserve", Files.readString(outside.resolve("existing.json")));
        assertFalse(Files.exists(outside.resolve("new.json")));
    }

    @Test
    public void selectDataPath_reservedLogNames_rejectsBeforeOriginalBytesAreChanged() throws Exception {
        assertReservedLogNames("addressbook.log");
    }

    @Test
    public void selectDataPath_reservedLogNames_rejectsBeforeOriginalBytesAreChangedPonHub() throws Exception {
        assertReservedLogNames("ponhub.log");
    }

    @Test
    public void selectDataPath_caseVariedLogNames_rejectsExistingAndMissingFiles() throws Exception {
        assertReservedLogNames("PonHub.LOG");
        assertReservedLogNames("AddressBook.LOG");
    }

    @Test
    public void selectDataPath_caseVariedHardLink_preservesOperationalBytes() throws Exception {
        assertLogHardLinkAlias("PonHub.LOG.0");
    }

    @Test
    public void startup_caseVariedPreferencesHardLink_preservesOriginalFiles() throws Exception {
        assertPreferencesHardLogAlias("AddressBook.LOG.0");
    }

    @Test
    public void startup_caseVariedPreferencesSymbolicLink_preservesExistingAndMissingTargets() throws Exception {
        assertPreferencesSymbolicLogAlias("PonHub.LOG.0");
    }

    private void assertReservedLogNames(String prefix) throws Exception {
        for (String suffix : List.of("", ".0", ".4", ".0.lck", ".0.1", ".future")) {
            String filename = prefix + suffix;
            Path data = testFolder.resolve(filename);
            Files.writeString(data, "rejected original " + filename);
            byte[] original = Files.readAllBytes(data);

            assertThrows(IllegalArgumentException.class, () ->
                    MainApp.selectDataPath(Map.of("data", filename), testFolder));
            assertArrayEquals(original, Files.readAllBytes(data));
        }
        assertThrows(IllegalArgumentException.class, () ->
                MainApp.selectDataPath(Map.of("data", prefix + ".2"), testFolder));
        assertFalse(Files.exists(testFolder.resolve(prefix + ".2")));
        assertFalse(Files.exists(testFolder.resolve("preferences.json")));
    }

    @Test
    public void selectDataPath_reservedLogSymbolicAlias_preservesOriginalAndLink() throws Exception {
        assertLogSymbolicAlias("addressbook.log.0");
    }

    @Test
    public void selectDataPath_reservedLogSymbolicAlias_preservesOriginalAndLinkPonHub() throws Exception {
        assertLogSymbolicAlias("ponhub.log.0");
    }

    private void assertLogSymbolicAlias(String filename) throws Exception {
        Path log = testFolder.resolve(filename);
        Files.writeString(log, "rejected original");
        byte[] original = Files.readAllBytes(log);
        Path alias = testFolder.resolve("selected.json");
        try {
            Files.createSymbolicLink(alias, log.getFileName());
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(false, "Symbolic links unavailable: " + exception);
        }

        assertThrows(IllegalArgumentException.class, () ->
                MainApp.selectDataPath(Map.of("data", alias.toString()), testFolder));
        assertTrue(Files.isSymbolicLink(alias));
        assertArrayEquals(original, Files.readAllBytes(log));
        Files.delete(log);
        assertThrows(IllegalArgumentException.class, () ->
                MainApp.selectDataPath(Map.of("data", alias.toString()), testFolder));
        assertTrue(Files.isSymbolicLink(alias));
        assertFalse(Files.exists(log));
    }

    @Test
    public void selectDataPath_reservedLogHardLinkAlias_preservesOperationalBytes() throws Exception {
        assertLogHardLinkAlias("addressbook.log.0");
    }

    @Test
    public void selectDataPath_reservedLogHardLinkAlias_preservesOperationalBytesPonHub() throws Exception {
        assertLogHardLinkAlias("ponhub.log.0");
    }

    private void assertLogHardLinkAlias(String filename) throws Exception {
        Path data = Files.createDirectory(testFolder.resolve("data")).resolve("selected.json");
        Files.writeString(data, "rejected original");
        byte[] original = Files.readAllBytes(data);
        Path log = testFolder.resolve(filename);
        try {
            Files.createLink(log, data);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(false, "Hard links unavailable: " + exception);
        }

        assertThrows(IllegalArgumentException.class, () ->
                MainApp.selectDataPath(Map.of("data", data.toString()), testFolder));
        assertArrayEquals(original, Files.readAllBytes(data));
        assertArrayEquals(original, Files.readAllBytes(log));
    }

    @Test
    public void startup_preferencesSymbolicAliasToLog_preservesBothOriginalFiles() throws Exception {
        assertPreferencesSymbolicLogAlias("addressbook.log.0");
    }

    @Test
    public void startup_preferencesSymbolicAliasToLog_preservesBothOriginalFilesPonHub() throws Exception {
        assertPreferencesSymbolicLogAlias("ponhub.log.0");
    }

    private void assertPreferencesSymbolicLogAlias(String filename) throws Exception {
        Path data = testFolder.resolve("canonical.json");
        Files.writeString(data, "rejected operational bytes");
        byte[] originalData = Files.readAllBytes(data);
        Path log = testFolder.resolve(filename);
        Files.writeString(log, "preserve log bytes");
        byte[] originalLog = Files.readAllBytes(log);
        Path prefs = testFolder.resolve("preferences.json");
        try {
            Files.createSymbolicLink(prefs, log.getFileName());
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(false, "Symbolic links unavailable: " + exception);
        }

        assertThrows(IllegalArgumentException.class, () -> new MainApp().initialize(data, prefs));
        assertArrayEquals(originalData, Files.readAllBytes(data));
        assertArrayEquals(originalLog, Files.readAllBytes(log));
        assertTrue(Files.isSymbolicLink(prefs));
        Files.delete(log);
        assertThrows(IllegalArgumentException.class, () -> new MainApp().initialize(data, prefs));
        assertArrayEquals(originalData, Files.readAllBytes(data));
        assertTrue(Files.isSymbolicLink(prefs));
        assertFalse(Files.exists(log));
    }

    @Test
    public void startup_preferencesHardLinkAliasToLog_preservesBothOriginalFiles() throws Exception {
        assertPreferencesHardLogAlias("addressbook.log.0");
    }

    @Test
    public void startup_preferencesHardLinkAliasToLog_preservesBothOriginalFilesPonHub() throws Exception {
        assertPreferencesHardLogAlias("ponhub.log.0");
    }

    private void assertPreferencesHardLogAlias(String filename) throws Exception {
        Path data = testFolder.resolve("canonical.json");
        Files.writeString(data, "rejected operational bytes");
        byte[] originalData = Files.readAllBytes(data);
        Path log = testFolder.resolve(filename);
        Files.writeString(log, "preserve log bytes");
        byte[] originalLog = Files.readAllBytes(log);
        Path prefs = testFolder.resolve("preferences.json");
        try {
            Files.createLink(prefs, log);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(false, "Hard links unavailable: " + exception);
        }

        assertThrows(IllegalArgumentException.class, () -> new MainApp().initialize(data, prefs));
        assertArrayEquals(originalData, Files.readAllBytes(data));
        assertArrayEquals(originalLog, Files.readAllBytes(log));
        assertArrayEquals(originalLog, Files.readAllBytes(prefs));
    }

    @Test
    public void selectDataPath_logLinksToNestedStore_rejectsExistingAndMissingTargets() throws Exception {
        for (String prefix : List.of("addressbook.log", "ponhub.log", "AddressBook.LOG", "PonHub.LOG")) {
            Path home = Files.createTempDirectory(testFolder, "log-home-");
            Path data = Files.createDirectory(home.resolve("data")).resolve("selected.json");
            Files.writeString(data, "Preserve nested operational data");
            Path log = home.resolve(prefix + ".0");
            try {
                Files.createSymbolicLink(log, home.relativize(data));
            } catch (IOException | UnsupportedOperationException | SecurityException exception) {
                assumeTrue(false, "Symbolic links unavailable: " + exception);
            }

            assertThrows(IllegalArgumentException.class, () ->
                    MainApp.selectDataPath(Map.of("data", "data/selected.json"), home));
            assertEquals("Preserve nested operational data", Files.readString(data));
            Files.delete(data);
            assertThrows(IllegalArgumentException.class, () ->
                    MainApp.selectDataPath(Map.of("data", "data/selected.json"), home));
            assertTrue(Files.isSymbolicLink(log));
            assertFalse(Files.exists(data));
            assertFalse(Files.exists(home.resolve("preferences.json")));
        }
    }

    private MainApp startSession(Path data) throws IOException {
        MainApp app = new MainApp();
        app.initialize(data, testFolder.resolve("prefs.json"));
        return app;
    }

    private void assertProtectedCommands(MainApp app) throws Exception {
        String recoveryMessage = app.canonicalSession.getDataLoadError().orElseThrow();
        assertTrue(recoveryMessage.contains(app.canonicalSession.getDataFilePath().toAbsolutePath().toString()));
        assertTrue(app.logic.getPersonRecords().isEmpty());
        assertTrue(app.logic.execute("help").getFeedbackToUser().contains(recoveryMessage));
        assertTrue(app.logic.execute("list").getFeedbackToUser().contains(recoveryMessage));
        assertTrue(app.logic.execute("exit").isExit());
        for (String command : new String[]{ADD_COMMAND, "delete 1", "clear", "undo"}) {
            CommandException failure = assertThrows(CommandException.class, () -> app.logic.execute(command));
            assertEquals(recoveryMessage, failure.getMessage());
        }
        assertEquals(new PonHubData().exportState(), app.canonicalSession.getDataSnapshot());
    }
}
