package ponhub;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
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
    public void main_invalidDataPath_rejectsBeforeStartupWithoutChangingFiles() throws Exception {
        Path outside = directory.resolve("outside.json");
        Files.writeString(outside, "preserve outside data");
        // Blank, escaping and reserved paths cover the documented rejection categories.
        for (String selected : List.of("", "   ", "../outside.json", outside.toString(),
                "preferences.json", "ponhub.log.0", "addressbook.log.1")) {
            Path folder = Files.createTempDirectory(directory, "rejected-path-");
            Path preferences = folder.resolve("preferences.json");
            Files.writeString(preferences, "preserve preferences");
            String output = runMain(folder, "--data=data/valid.json", "--data=" + selected);
            assertEquals(1, output.lines().count(), output);
            assertTrue(output.startsWith("Cannot start PonHub with --data=\"" + selected + "\":"), output);
            assertTrue(output.contains("Choose a different data file within the application folder."), output);
            assertFalse(output.contains("Exception"), output);
            assertFalse(output.contains("at ponhub."), output);
            assertEquals("preserve preferences", Files.readString(preferences));
            try (var files = Files.list(folder)) {
                assertEquals(List.of(preferences), files.toList());
            }
            assertEquals("preserve outside data", Files.readString(outside));
        }
    }

    @Test
    public void main_reservedLockPath_rejectsMissingAndExistingFilesWithoutWrites() throws Exception {
        for (String selected : List.of("x.ponhub-lock", "nested/X.PONHUB-LOCK")) {
            Path folder = Files.createTempDirectory(directory, "reserved-lock-");
            Path data = folder.resolve(selected);
            Files.createDirectories(data.getParent());
            for (String contents : List.of("missing", "", VALID_DATA)) {
                if (!contents.equals("missing")) {
                    Files.writeString(data, contents);
                }
                String output = runMain(folder, "--data=" + selected);
                assertEquals(1, output.lines().count(), output);
                assertTrue(output.startsWith("Cannot start PonHub with --data="), output);
                assertTrue(output.contains("reserved for save coordination files"), output);
                assertFalse(Files.exists(folder.resolve("preferences.json")));
                assertFalse(Files.exists(folder.resolve("ponhub.log.0")));
                assertFalse(Files.exists(data.resolveSibling(data.getFileName() + ".ponhub-lock")));
                if (contents.equals("missing")) {
                    assertFalse(Files.exists(data));
                } else {
                    assertEquals(contents, Files.readString(data));
                }
            }
        }
    }

    @Test
    public void main_uncheckableDataPath_preservesUnexpectedFailureDiagnostics() throws Exception {
        Path folder = Files.createTempDirectory(directory, "uncheckable-path-");
        try {
            Files.createSymbolicLink(folder.resolve("cycle"), Path.of("cycle"));
        } catch (IOException | UnsupportedOperationException | SecurityException failure) {
            assumeTrue(false, "Symbolic links unavailable: " + failure);
        }
        // A filesystem inspection failure must not be disguised as ordinary invalid input.
        String output = runMain(folder, "--data=cycle");
        assertTrue(output.contains("java.io.IOException: Too many symbolic links"), output);
        assertFalse(output.contains("Cannot start PonHub with --data="), output);
        assertFalse(Files.exists(folder.resolve("preferences.json")));
    }

    /**
     * Runs the real entry point in an isolated folder; captures rejection before native UI startup.
     */
    private String runMain(Path folder, String... arguments) throws Exception {
        String runtimeClasspath = System.getProperty("ponhub.test.runtimeClasspath");
        assertNotNull(runtimeClasspath, "Run this integration test through the Gradle test task.");
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", executable);
        Path log = Files.createTempFile(directory, "entry-point-", ".log");
        List<String> command = new ArrayList<>(List.of(java.toString(), "-cp", runtimeClasspath,
                Main.class.getName()));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command)
                .directory(folder.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            boolean hasExited = process.waitFor(30, TimeUnit.SECONDS);
            String output = Files.readString(log);
            assertTrue(hasExited, "Entry point timed out; see " + log + "\n" + output);
            assertEquals(1, process.exitValue(), output);
            return output;
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
            }
        }
    }

    @Test
    public void init_excessiveNesting_opensProtectedStageAndRecoversAfterRestart() throws Exception {
        for (String value : List.of("[".repeat(50_000) + "0" + "]".repeat(50_000),
                "{\"schemaVersion\":1,\"people\":" + "[".repeat(50_000)
                        + "0" + "]".repeat(50_000) + "}")) {
            Path folder = Files.createTempDirectory(directory, "deep-startup-");
            Path selected = folder.resolve("object-nested.json");
            Files.writeString(selected, value);
            byte[] original = Files.readAllBytes(selected);
            runBootstrap(folder, LoadStatus.CORRUPT, 0, false, "--data=object-nested.json");
            assertArrayEquals(original, Files.readAllBytes(selected));
            Files.writeString(selected, VALID_DATA);
            runBootstrap(folder, LoadStatus.LOADED, 1, true, "--data=object-nested.json");
            assertEquals(VALID_DATA, Files.readString(selected));
        }
    }

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
                new RejectedData("\uFEFF" + VALID_DATA, LoadStatus.CORRUPT),
                new RejectedData(VALID_DATA.replace("\"role\":\"TUTOR\"", "\"role\":\"tutor\""), LoadStatus.CORRUPT),
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
    private void runBootstrap(Path folder, LoadStatus status, int peopleCount, boolean shouldRestoreGui,
            String... arguments) throws Exception {
        String runtimeClasspath = System.getProperty("ponhub.test.runtimeClasspath");
        assertNotNull(runtimeClasspath, "Run this integration test through the Gradle test task.");
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", executable);
        Path log = folder.resolve("bootstrap.log");
        List<String> command = new ArrayList<>(List.of(java.toString(), "-cp", runtimeClasspath,
                StartupProbe.class.getName(), "--status=" + status, "--people=" + peopleCount,
                "--restore=" + shouldRestoreGui));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command)
                .directory(folder.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            boolean hasExited = process.waitFor(30, TimeUnit.SECONDS);
            String output = Files.readString(log);
            assertTrue(hasExited, "Bootstrap timed out; see " + log + "\n" + output);
            assertEquals(0, process.exitValue(), output);
            int previousPhase = -1;
            for (String phase : List.of("init verified", "stage shown", "pulse observed", "stage hidden",
                    "stop entered", "stop verified", COMPLETION_MARKER)) {
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
     * Retains production init/stop; a minimal stage observes a pulse and hides before requesting toolkit exit.
     * This avoids terminating the fixture before it has exercised the native stage lifecycle.
     */
    public static class BootstrapApplication extends MainApp {
        private Path data;
        private byte[] original;
        private boolean hasObservedPulse;
        private boolean hasHiddenStage;

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
            boolean shouldRestoreGui = Boolean.parseBoolean(getParameters().getNamed().get("restore"));
            boolean isProtectedSession = status != LoadStatus.MISSING && status != LoadStatus.LOADED;
            assertNotNull(ui);
            assertEquals(data, canonicalSession.getDataFilePath());
            assertEquals(status, canonicalSession.getLoadStatus());
            assertEquals(isProtectedSession, canonicalSession.getDataLoadError().isPresent());
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
            if (shouldRestoreGui) {
                assertEquals(SAVED_GUI, logic.getGuiSettings());
            }
            String help = logic.execute("help").getFeedbackToUser();
            String list = logic.execute("list").getFeedbackToUser();
            assertTrue(logic.execute("exit").isExit());
            if (isProtectedSession) {
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
            // Own the teardown order instead of letting last-window shutdown race an explicit toolkit exit.
            Platform.setImplicitExit(false);
            primaryStage.setOnHidden(event -> {
                try {
                    assertTrue(Platform.isFxApplicationThread());
                    assertTrue(hasObservedPulse);
                    assertFalse(primaryStage.isShowing());
                    hasHiddenStage = true;
                    System.out.println("Bootstrap phase: stage hidden");
                } catch (Throwable failure) {
                    StartupProbe.recordFailure(failure);
                } finally {
                    Platform.runLater(Platform::exit);
                }
            });
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
                        // Finish this pulse, then hide the native window before exiting the toolkit.
                        Platform.runLater(primaryStage::hide);
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
                assertTrue(hasHiddenStage);
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
