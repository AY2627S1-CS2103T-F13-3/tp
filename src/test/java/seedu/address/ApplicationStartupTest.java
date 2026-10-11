package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.core.GuiSettings;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.storage.JsonUserPrefsStorage;

/**
 * Exercises complete nonvisual MainApp initialization and shutdown in isolated process folders.
 * Ernest used OpenAI Codex for these bootstrap/path/preferences regression tests and guide notes.
 */
public class ApplicationStartupTest {
    private static final String VALID_DATA = "{\"_comment\":\"Manually checked contacts\",\"persons\":["
            + "{\"name\":\"Anne-Marie  O'Neil\",\"phone\":\"00123456\","
            + "\"email\":\"Anne%School@Example.COM\",\"address\":\"Blk 10,  #01-02\",\"tags\":[]}]}";
    private static final GuiSettings SAVED_GUI = new GuiSettings(910, 710, 21, 31);

    @TempDir
    Path directory;

    @Test
    public void init_missingData_keepsReadsUnwrittenAndRestoresPreferences() throws Exception {
        Path folder = Files.createDirectory(directory.resolve("fresh application folder with spaces"));
        runBootstrap(folder, false, -1, false);
        assertFalse(Files.exists(folder.resolve("data/addressbook.json")));
        runBootstrap(folder, false, -1, true);
        assertFalse(Files.exists(folder.resolve("data/addressbook.json")));
    }

    @Test
    public void init_rejectedData_preservesOriginalAcrossCommandsShutdownAndRestart() throws Exception {
        List<String> rejected = List.of("{broken", "null", "{\"persons\":[null]}",
                "{\"schemaVersion\":99,\"persons\":[],\"lessons\":[]}",
                "{\"schemaVersion\":1,\"people\":[],\"lessons\":[],\"attendance\":[]}");
        for (int i = 0; i < rejected.size(); i++) {
            Path folder = Files.createDirectory(directory.resolve("rejected startup " + i));
            Path data = folder.resolve("data/addressbook.json");
            Files.createDirectories(data.getParent());
            Files.writeString(data, rejected.get(i));
            byte[] original = Files.readAllBytes(data);
            runBootstrap(folder, true, 0, false);
            runBootstrap(folder, true, 0, true);
            assertArrayEquals(original, Files.readAllBytes(data));
        }
    }

    @Test
    public void init_supportedHumanEdit_loadsFromLaunchFolderWithoutRewriting() throws Exception {
        Path folder = Files.createDirectory(directory.resolve("supported application folder with spaces"));
        Path data = folder.resolve("data/addressbook.json");
        Files.createDirectories(data.getParent());
        Files.writeString(data, VALID_DATA);
        byte[] original = Files.readAllBytes(data);
        runBootstrap(folder, false, 1, false);
        runBootstrap(folder, false, 1, true);
        assertArrayEquals(original, Files.readAllBytes(data));
    }

    @Test
    public void init_repairedSupportedFile_revalidatesOnNewApplicationStartup() throws Exception {
        Path folder = Files.createDirectory(directory.resolve("repaired application folder"));
        Path data = folder.resolve("data/addressbook.json");
        Files.createDirectories(data.getParent());
        Files.writeString(data, "{broken");
        runBootstrap(folder, true, 0, false);
        Files.writeString(data, VALID_DATA);
        runBootstrap(folder, false, 1, true);
        assertEquals(VALID_DATA, Files.readString(data));
    }

    /**
     * Runs real initialization in a child JVM so relative application paths cannot touch the checkout.
     */
    private void runBootstrap(Path folder, boolean protectedSession, int peopleCount, boolean restoreGui)
            throws Exception {
        String runtimeClasspath = System.getProperty("ponhub.test.runtimeClasspath");
        assertNotNull(runtimeClasspath, "Run this integration test through the Gradle test task.");
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", executable);
        Path log = folder.resolve("bootstrap.log");
        Process process = new ProcessBuilder(java.toString(), "-cp", runtimeClasspath,
                StartupProbe.class.getName(), Boolean.toString(protectedSession), Integer.toString(peopleCount),
                Boolean.toString(restoreGui))
                .directory(folder.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Bootstrap timed out; see " + log);
            assertEquals(0, process.exitValue(), Files.readString(log));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
            }
        }
        var preferences = new JsonUserPrefsStorage(folder.resolve("preferences.json"));
        assertEquals(SAVED_GUI, preferences.readUserPrefs().orElseThrow().getGuiSettings());
    }

    /**
     * Runs the production bootstrap without showing a window or substituting its storage/model factories.
     */
    public static final class StartupProbe {
        public static void main(String[] args) throws Exception {
            boolean protectedSession = Boolean.parseBoolean(args[0]);
            int peopleCount = Integer.parseInt(args[1]);
            boolean restoreGui = Boolean.parseBoolean(args[2]);
            Path data = Path.of("data", "addressbook.json");
            byte[] original = Files.exists(data) ? Files.readAllBytes(data) : null;
            MainApp app = new MainApp();
            app.init();
            assertNotNull(app.ui);
            assertEquals(data, app.storage.getAddressBookFilePath());
            assertEquals(Path.of("preferences.json"), app.storage.getUserPrefsFilePath());
            assertEquals(protectedSession, app.storage.getDataLoadError().isPresent());
            if (peopleCount < 0) {
                assertFalse(app.model.getAddressBook().getPersonList().isEmpty());
            } else {
                assertEquals(peopleCount, app.model.getAddressBook().getPersonList().size());
                if (peopleCount == 1) {
                    var person = app.model.getAddressBook().getPersonList().getFirst();
                    assertEquals("Anne-Marie O'Neil", person.getName().fullName);
                    assertEquals("00123456", person.getPhone().value);
                }
            }
            if (restoreGui) {
                assertEquals(SAVED_GUI, app.model.getGuiSettings());
            }
            app.logic.execute("help");
            app.logic.execute("list");
            assertTrue(app.logic.execute("exit").isExit());
            if (protectedSession) {
                String guidance = app.storage.getDataLoadError().orElseThrow();
                assertTrue(guidance.contains(data.toAbsolutePath().toString()));
                assertTrue(guidance.contains("restart"));
                for (String command : List.of("add n/Alex p/91234567 e/alex@example.com a/Somewhere", "delete 1")) {
                    CommandException failure = assertThrows(CommandException.class, () -> app.logic.execute(command));
                    assertEquals(guidance, failure.getMessage());
                }
                assertThrows(IOException.class, () -> app.storage.saveAddressBook(app.model.getAddressBook()));
            }
            app.model.setGuiSettings(SAVED_GUI);
            app.stop();
            if (original == null) {
                assertFalse(Files.exists(data));
            } else {
                assertArrayEquals(original, Files.readAllBytes(data));
            }
            System.out.println("PASS: full MainApp init/stop, relative paths, command protection and preferences");
        }
    }
}
