package ponhub;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import javafx.stage.Window;
import ponhub.commons.core.GuiSettings;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.storage.JsonPonHubDataStorage;
import ponhub.storage.JsonUserPrefsStorage;
import ponhub.storage.ProtectedPonHubDataStorage.LoadStatus;
import ponhub.testutil.LessonQueryTestData;
import ponhub.ui.UiManager;

/**
 * Starts a fresh JavaFX application process to exercise real named arguments, window startup and shutdown.
 * Test-only callbacks dismiss the recovery dialog and exit the disposable window without user interaction.
 */
public class MainAppLifecycleTest {
    private static final GuiSettings EXPECTED_GUI_SETTINGS = new GuiSettings(950, 700, 30, 40);
    private static final String COMPLETION_MARKER = "Application lifecycle completed";

    @TempDir
    Path directory;

    @Test
    public void launch_defaultMissingData_startsProductionWindowAndSavesOnlyPreferences() throws Exception {
        runApplication("missing");

        assertFalse(Files.exists(directory.resolve("data/ponhub.json")));
        assertFalse(Files.exists(directory.resolve("data")));
        assertSavedPreferences();
        assertTrue(Files.isRegularFile(directory.resolve("ponhub.log.0")));
        assertFalse(Files.exists(directory.resolve("addressbook.log.0")));
    }

    @Test
    public void launch_namedPathWithSpaces_loadsCanonicalRecordsWithoutRewritingStore() throws Exception {
        Path selected = directory.resolve("selected folder/people store.json");
        new JsonPonHubDataStorage(selected).saveData(LessonQueryTestData.createData().exportState());
        byte[] original = Files.readAllBytes(selected);

        runApplication("loaded", "--data=selected folder/people store.json");

        assertArrayEquals(original, Files.readAllBytes(selected));
        assertFalse(Files.exists(directory.resolve("data/ponhub.json")));
        assertSavedPreferences();
    }

    @Test
    public void launch_rejectedStore_showsRecoveryAndPreservesOriginalThroughShutdown() throws Exception {
        Path selected = directory.resolve("rejected store.json");
        Files.writeString(selected, "{preserve rejected original");
        byte[] original = Files.readAllBytes(selected);

        runApplication("rejected", "--data=rejected store.json");

        assertArrayEquals(original, Files.readAllBytes(selected));
        assertSavedPreferences();
    }

    private void assertSavedPreferences() throws Exception {
        assertEquals(EXPECTED_GUI_SETTINGS, new JsonUserPrefsStorage(directory.resolve("preferences.json"))
                .readUserPrefs().orElseThrow().getGuiSettings());
    }

    /**
     * Bounds each native application process and retains its output when launch or lifecycle assertions fail.
     */
    private void runApplication(String scenario, String... arguments) throws Exception {
        String executableName = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        List<String> commandArguments = new ArrayList<>();
        commandArguments.add(Path.of(System.getProperty("java.home"), "bin", executableName).toString());
        ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(argument -> argument.matches("-javaagent:(?:.*[/\\\\])?jacocoagent\\.jar(?:=.*)?"))
                .map(this::resolveJacocoDestination).forEach(commandArguments::add);
        commandArguments.addAll(List.of("-cp", getChildClassPath(), LifecycleLauncher.class.getName(),
                "--lifecycle=" + scenario));
        commandArguments.addAll(List.of(arguments));
        Path outputPath = directory.resolve("application-output.txt");
        Process process = new ProcessBuilder(commandArguments).directory(directory.toFile())
                .redirectErrorStream(true).redirectOutput(outputPath.toFile()).start();
        try {
            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            String output = Files.readString(outputPath);
            assertTrue(completed, "Application lifecycle timed out:\n" + output);
            assertEquals(0, process.exitValue(), output);
            assertTrue(output.contains(COMPLETION_MARKER), output);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Includes Gradle's isolated test/runtime URLs and makes every entry absolute before changing directories.
     */
    private String getChildClassPath() throws Exception {
        Set<String> entries = new LinkedHashSet<>();
        for (ClassLoader loader = getClass().getClassLoader(); loader != null; loader = loader.getParent()) {
            if (loader instanceof URLClassLoader urls) {
                for (URL url : urls.getURLs()) {
                    if (url.getProtocol().equals("file")) {
                        entries.add(Path.of(url.toURI()).toAbsolutePath().toString());
                    }
                }
            }
        }
        for (Class<?> source : List.of(MainApp.class, LifecycleLauncher.class)) {
            entries.add(Path.of(source.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .toAbsolutePath().toString());
        }
        for (String entry : System.getProperty("java.class.path").split(Pattern.quote(File.pathSeparator))) {
            entries.add(Path.of(entry).toAbsolutePath().toString());
        }
        return String.join(File.pathSeparator, entries);
    }

    /**
     * Preserves the active JaCoCo destination so child executions join this test task's coverage report.
     */
    private String resolveJacocoDestination(String argument) {
        Matcher option = Pattern.compile("([=,]destfile=)([^,]+)").matcher(argument);
        if (!option.find()) {
            return argument;
        }
        String replacement = option.group(1) + Path.of(option.group(2)).toAbsolutePath();
        return option.replaceFirst(Matcher.quoteReplacement(replacement));
    }

    /**
     * Launches through a non-Application entry point using the same workaround as the production Main class.
     */
    public static class LifecycleLauncher {
        private static Throwable failure;

        public static void main(String[] args) {
            Application.launch(LifecycleApplication.class, args);
            if (failure != null) {
                throw new AssertionError("Application lifecycle assertions failed", failure);
            }
        }
    }

    /**
     * Retains production init/start/stop while adding assertions and automatic cleanup for disposable test windows.
     */
    public static class LifecycleApplication extends MainApp {
        @Override
        public void start(Stage primaryStage) {
            String scenario = getParameters().getNamed().get("lifecycle");
            if (scenario.equals("rejected")) {
                // showAndWait enters a nested event loop that runs this queued test-only dismissal.
                Platform.runLater(() -> dismissRecoveryDialog(primaryStage));
            }
            try {
                super.start(primaryStage);
                assertTrue(primaryStage.isShowing());
                ListView<?> cards = (ListView<?>) primaryStage.getScene().lookup("#personListView");
                assertSame(logic.getPersonRecords(), cards.getItems());
                switch (scenario) {
                    case "missing":
                        assertEquals(LoadStatus.MISSING, canonicalSession.getLoadStatus());
                        assertTrue(logic.getPersonRecords().isEmpty());
                        break;
                    case "loaded":
                        assertEquals(LoadStatus.LOADED, canonicalSession.getLoadStatus());
                        assertEquals(LessonQueryTestData.createData().exportState(),
                                canonicalSession.getDataSnapshot());
                        break;
                    case "rejected":
                        assertEquals(LoadStatus.CORRUPT, canonicalSession.getLoadStatus());
                        assertTrue(logic.getPersonRecords().isEmpty());
                        assertThrows(CommandException.class, () -> logic.execute("add"));
                        assertTrue(logic.execute("list").getFeedbackToUser()
                                .contains("An empty view is not your saved data"));
                        break;
                    default:
                        throw new AssertionError("Unknown lifecycle scenario: " + scenario);
                }
                logic.execute("help");
                logic.execute("list");
                logic.setGuiSettings(EXPECTED_GUI_SETTINGS);
            } catch (Throwable failure) {
                LifecycleLauncher.failure = failure;
            } finally {
                Platform.runLater(Platform::exit);
            }
        }

        private void dismissRecoveryDialog(Stage primaryStage) {
            try {
                List<Window> dialogs = List.copyOf(Window.getWindows()).stream()
                        .filter(window -> window != primaryStage && window.getScene() != null)
                        .filter(window -> window.getScene().lookup("#" + UiManager.ALERT_DIALOG_PANE_FIELD_ID) != null)
                        .toList();
                assertEquals(1, dialogs.size(), "Expected one protected-startup recovery dialog");
                dialogs.getFirst().hide();
            } catch (Throwable failure) {
                LifecycleLauncher.failure = failure;
                Platform.exit();
            }
        }

        @Override
        public void stop() {
            super.stop();
            System.out.println(COMPLETION_MARKER);
        }
    }
}
