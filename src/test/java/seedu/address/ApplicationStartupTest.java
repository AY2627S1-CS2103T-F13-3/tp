package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import seedu.address.commons.core.GuiSettings;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.PonHubData;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRole;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.ProtectedPonHubDataStorage.LoadStatus;

/**
 * Exercises complete nonvisual MainApp initialization and shutdown in isolated process folders.
 * Ernest used OpenAI Codex for these bootstrap/path/preferences regression tests and guide notes.
 */
public class ApplicationStartupTest {
    private static final String VALID_DATA = """
            {"schemaVersion":1,"people":[
              {"id":"T7","role":"TUTOR","name":"Anne-Marie  O'Neil","phone":"00123456",
               "email":"Anne%School@Example.COM","address":"Blk 10,  #01-02"}],
             "personCounters":{"STUDENT":0,"TUTOR":7,"PARENT":0},
             "lastAllocatedLessonSequence":0,"lessons":[],"attendance":[]}
            """;
    private static final GuiSettings SAVED_GUI = new GuiSettings(910, 710, 21, 31);
    private static final String COMPLETION_MARKER = "PASS: canonical MainApp init/stop and preferences";

    @TempDir
    Path directory;

    @Test
    public void init_missingData_keepsReadsUnwrittenAndRestoresPreferences() throws Exception {
        Path folder = Files.createDirectory(directory.resolve("fresh application folder with spaces"));
        Path legacy = folder.resolve("data/addressbook.json");
        Files.createDirectories(legacy.getParent());
        Files.writeString(legacy, "{\"persons\":[]}");
        byte[] originalLegacy = Files.readAllBytes(legacy);
        runBootstrap(folder, LoadStatus.MISSING, 0, false);
        runBootstrap(folder, LoadStatus.MISSING, 0, true);
        assertFalse(Files.exists(folder.resolve("data/ponhub.json")));
        assertArrayEquals(originalLegacy, Files.readAllBytes(legacy));
    }

    @Test
    public void init_rejectedData_preservesOriginalAcrossCommandsShutdownAndRestart() throws Exception {
        List<RejectedData> rejected = List.of(new RejectedData("{broken", LoadStatus.CORRUPT),
                new RejectedData("null", LoadStatus.CORRUPT),
                new RejectedData("{\"persons\":[null]}", LoadStatus.LEGACY),
                new RejectedData(VALID_DATA.replace("\"schemaVersion\":1", "\"schemaVersion\":99"),
                        LoadStatus.UNSUPPORTED_VERSION),
                new RejectedData(VALID_DATA.replace("\"people\":[", "\"people\":[null,"), LoadStatus.CORRUPT),
                new RejectedData(VALID_DATA.replace("\"TUTOR\":7", "\"TUTOR\":0"), LoadStatus.CORRUPT));
        for (int i = 0; i < rejected.size(); i++) {
            Path folder = Files.createDirectory(directory.resolve("rejected startup " + i));
            Path data = folder.resolve("data/ponhub.json");
            Files.createDirectories(data.getParent());
            Files.writeString(data, rejected.get(i).json());
            byte[] original = Files.readAllBytes(data);
            runBootstrap(folder, rejected.get(i).status(), 0, false);
            runBootstrap(folder, rejected.get(i).status(), 0, true);
            assertArrayEquals(original, Files.readAllBytes(data));
        }
    }

    @Test
    public void init_supportedHumanEdit_loadsFromLaunchFolderWithoutRewriting() throws Exception {
        Path folder = Files.createDirectory(directory.resolve("supported application folder with spaces"));
        Path data = folder.resolve("data/ponhub.json");
        Files.createDirectories(data.getParent());
        Files.writeString(data, VALID_DATA);
        byte[] original = Files.readAllBytes(data);
        runBootstrap(folder, LoadStatus.LOADED, 1, false);
        runBootstrap(folder, LoadStatus.LOADED, 1, true);
        assertArrayEquals(original, Files.readAllBytes(data));
    }

    @Test
    public void init_repairedSupportedFile_revalidatesOnNewApplicationStartup() throws Exception {
        Path folder = Files.createDirectory(directory.resolve("repaired application folder"));
        Path data = folder.resolve("data/ponhub.json");
        Files.createDirectories(data.getParent());
        Files.writeString(data, "{broken");
        runBootstrap(folder, LoadStatus.CORRUPT, 0, false);
        Files.writeString(data, VALID_DATA);
        runBootstrap(folder, LoadStatus.LOADED, 1, true);
        assertEquals(VALID_DATA, Files.readString(data));
    }

    @Test
    public void init_namedPathWithSpaces_usesSelectedStoreAndSharedPreferences() throws Exception {
        Path folder = Files.createDirectory(directory.resolve("alternate application folder with spaces"));
        Path selected = folder.resolve("selected folder/people store.json");
        Files.createDirectories(selected.getParent());
        Files.writeString(selected, VALID_DATA);
        byte[] original = Files.readAllBytes(selected);
        runBootstrap(folder, LoadStatus.LOADED, 1, false, "--data=selected folder/people store.json");
        runBootstrap(folder, LoadStatus.LOADED, 1, true, "--data=selected folder/people store.json");
        assertArrayEquals(original, Files.readAllBytes(selected));
        assertFalse(Files.exists(folder.resolve("data/ponhub.json")));
        runBootstrap(folder, LoadStatus.MISSING, 0, true);
        assertArrayEquals(original, Files.readAllBytes(selected));
        assertFalse(Files.exists(folder.resolve("data/ponhub.json")));
    }

    /**
     * Runs real initialization in a child JVM so relative application paths cannot touch the checkout.
     */
    private void runBootstrap(Path folder, LoadStatus status, int peopleCount, boolean restoreGui,
            String... arguments) throws Exception {
        String runtimeClasspath = System.getProperty("ponhub.test.runtimeClasspath");
        assertNotNull(runtimeClasspath, "Run this integration test through the Gradle test task.");
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", executable);
        Path log = folder.resolve("bootstrap.log");
        List<String> command = new ArrayList<>(List.of(java.toString(), "-cp", runtimeClasspath,
                StartupProbe.class.getName(), "--status=" + status, "--people=" + peopleCount,
                "--restore=" + restoreGui));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command)
                .directory(folder.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Bootstrap timed out; see " + log);
            assertEquals(0, process.exitValue(), Files.readString(log));
            assertTrue(Files.readString(log).contains(COMPLETION_MARKER), Files.readString(log));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
            }
        }
        var preferences = new JsonUserPrefsStorage(folder.resolve("preferences.json"));
        assertEquals(SAVED_GUI, preferences.readUserPrefs().orElseThrow().getGuiSettings());
    }

    private record RejectedData(String json, LoadStatus status) {
    }

    /**
     * Supplies real JavaFX named parameters without showing a window or substituting production factories.
     */
    public static final class StartupProbe {
        private static Throwable failure;

        public static void main(String[] args) {
            Application.launch(BootstrapApplication.class, args);
            if (failure != null) {
                throw new AssertionError("Canonical bootstrap assertions failed", failure);
            }
            System.out.println(COMPLETION_MARKER);
        }
    }

    /**
     * Retains production init/stop; the start callback exits the toolkit without displaying native controls.
     */
    public static class BootstrapApplication extends MainApp {
        private Path data;
        private byte[] original;

        @Override
        public void init() throws Exception {
            // The Windows runner's working directory can use an 8.3 alias such as RUNNER~1.
            data = Path.of("").toRealPath().resolve(
                    getParameters().getNamed().getOrDefault("data", "data/ponhub.json")).normalize();
            original = Files.exists(data) ? Files.readAllBytes(data) : null;
            super.init();
            try {
                verifySession();
            } catch (Throwable failure) {
                StartupProbe.failure = failure;
            }
        }

        private void verifySession() throws Exception {
            LoadStatus status = LoadStatus.valueOf(getParameters().getNamed().get("status"));
            int peopleCount = Integer.parseInt(getParameters().getNamed().get("people"));
            boolean restoreGui = Boolean.parseBoolean(getParameters().getNamed().get("restore"));
            boolean protectedSession = status != LoadStatus.MISSING && status != LoadStatus.LOADED;
            assertNotNull(ui);
            assertEquals(data, canonicalSession.getDataFilePath());
            assertEquals(status, canonicalSession.getLoadStatus());
            assertEquals(protectedSession, canonicalSession.getDataLoadError().isPresent());
            assertEquals(peopleCount, logic.getPersonRecords().size());
            assertEquals(peopleCount, canonicalSession.getDataSnapshot().people().getPeople().size());
            if (peopleCount == 1) {
                var person = logic.getPersonRecords().getFirst();
                assertEquals(new PersonId("T7"), person.getId());
                assertEquals(PersonRole.TUTOR, person.getRole());
                assertEquals("Anne-Marie O'Neil", person.getContactDetails().getName().fullName);
                assertEquals("00123456", person.getContactDetails().getPhone().orElseThrow().value);
                assertEquals("Blk 10, #01-02", person.getContactDetails().getAddress().orElseThrow().value);
            }
            if (restoreGui) {
                assertEquals(SAVED_GUI, logic.getGuiSettings());
            }
            String help = logic.execute("help").getFeedbackToUser();
            String list = logic.execute("list").getFeedbackToUser();
            assertTrue(logic.execute("exit").isExit());
            if (protectedSession) {
                String guidance = canonicalSession.getDataLoadError().orElseThrow();
                assertTrue(guidance.contains(data.toAbsolutePath().toString()));
                assertTrue(guidance.contains("restart"));
                assertTrue(help.contains(guidance));
                assertTrue(list.contains(guidance));
                for (String command : List.of("add r/parent n/Alex p/91234567", "delete 1")) {
                    CommandException failure = assertThrows(CommandException.class, () -> logic.execute(command));
                    assertEquals(guidance, failure.getMessage());
                }
                assertEquals(new PonHubData().exportState(), canonicalSession.getDataSnapshot());
            }
            logic.setGuiSettings(SAVED_GUI);
        }

        @Override
        public void start(Stage primaryStage) {
            Platform.runLater(Platform::exit);
        }

        @Override
        public void stop() {
            try {
                super.stop();
                if (original == null) {
                    assertFalse(Files.exists(data));
                } else {
                    assertArrayEquals(original, Files.readAllBytes(data));
                }
            } catch (Throwable failure) {
                if (StartupProbe.failure == null) {
                    StartupProbe.failure = failure;
                } else {
                    StartupProbe.failure.addSuppressed(failure);
                }
            }
        }
    }
}
