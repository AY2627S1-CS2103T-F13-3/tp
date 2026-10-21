package ponhub;

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

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ponhub.commons.core.GuiSettings;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.model.PonHubData;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRole;
import ponhub.storage.JsonUserPrefsStorage;
import ponhub.storage.ProtectedPonHubDataStorage.LoadStatus;

/**
 * Exercises MainApp initialization and shutdown with a minimal stage in isolated process folders.
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
            boolean hasExited = process.waitFor(30, TimeUnit.SECONDS);
            String output = Files.readString(log);
            assertTrue(hasExited, "Bootstrap timed out; see " + log + "\n" + output);
            assertEquals(0, process.exitValue(), output);
            int previousPhase = -1;
            for (String phase : List.of("init verified", "stage shown", "pulse observed", "stop entered",
                    "stop verified", COMPLETION_MARKER)) {
                int position = output.indexOf(phase);
                assertTrue(position > previousPhase, "Missing or out-of-order phase: " + phase + "\n" + output);
                previousPhase = position;
            }
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
     * Supplies real JavaFX named parameters without substituting production initialization or storage factories.
     */
    public static final class StartupProbe {
        private static volatile Throwable failure;

        public static void main(String[] args) {
            Application.launch(BootstrapApplication.class, args);
            if (failure != null) {
                throw new AssertionError("Canonical bootstrap assertions failed", failure);
            }
            System.out.println(COMPLETION_MARKER);
        }

        private static void recordFailure(Throwable cause) {
            if (failure == null) {
                failure = cause;
            } else {
                failure.addSuppressed(cause);
            }
        }
    }

    /**
     * Retains production init/stop; a minimal stage observes a pulse before requesting toolkit exit.
     * This avoids terminating the fixture before it has exercised the native stage lifecycle.
     */
    public static class BootstrapApplication extends MainApp {
        private Path data;
        private byte[] original;
        private boolean hasObservedPulse;

        @Override
        public void init() throws Exception {
            // The Windows runner's working directory can use an 8.3 alias such as RUNNER~1.
            data = Path.of("").toRealPath().resolve(
                    getParameters().getNamed().getOrDefault("data", "data/ponhub.json")).normalize();
            original = Files.exists(data) ? Files.readAllBytes(data) : null;
            super.init();
            try {
                assertFalse(Platform.isFxApplicationThread());
                verifySession();
                System.out.println("Bootstrap phase: init verified");
            } catch (Throwable failure) {
                StartupProbe.recordFailure(failure);
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
            assertTrue(Platform.isFxApplicationThread());
            System.out.println("Bootstrap runtime: " + System.getProperty("os.name") + " / "
                    + System.getProperty("os.arch") + " / " + System.getProperty("java.vendor") + " / "
                    + System.getProperty("java.runtime.version") + " / JavaFX "
                    + System.getProperty("javafx.runtime.version"));
            primaryStage.setScene(new Scene(new StackPane(), 160, 100));
            primaryStage.show();
            assertTrue(primaryStage.isShowing());
            System.out.println("Bootstrap phase: stage shown");
            new AnimationTimer() {
                @Override
                public void handle(long now) {
                    stop();
                    try {
                        assertTrue(Platform.isFxApplicationThread());
                        assertTrue(primaryStage.isShowing());
                        hasObservedPulse = true;
                        System.out.println("Bootstrap phase: pulse observed");
                    } catch (Throwable failure) {
                        StartupProbe.recordFailure(failure);
                    } finally {
                        // Let this pulse finish before the launcher invokes production shutdown.
                        Platform.runLater(Platform::exit);
                    }
                }
            }.start();
        }

        @Override
        public void stop() {
            try {
                System.out.println("Bootstrap phase: stop entered");
                assertTrue(Platform.isFxApplicationThread());
                assertTrue(hasObservedPulse);
                super.stop();
                if (original == null) {
                    assertFalse(Files.exists(data));
                } else {
                    assertArrayEquals(original, Files.readAllBytes(data));
                }
                System.out.println("Bootstrap phase: stop verified");
            } catch (Throwable failure) {
                StartupProbe.recordFailure(failure);
            }
        }
    }
}
