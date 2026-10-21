package ponhub.commons.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class LogsCenterTest {
    private static final String LOG_FILE_NAME = "ponhub.log.0";
    private static final String FIRST_MESSAGE = "First message after logging startup";
    private static final String REPEATED_MESSAGE = "Message after repeated logger lookup";
    private static final String OTHER_MESSAGE = "Message from another logger";

    @TempDir
    public Path temporaryDirectory;

    @Test
    public void getLogger_logFileAvailable_logsToConsoleAndFileOnce() throws Exception {
        String consoleOutput = runLoggingProcess();

        assertMessagesAppearOnce(consoleOutput);
        Path logFile = temporaryDirectory.resolve(LOG_FILE_NAME);
        assertTrue(Files.isRegularFile(logFile));
        assertMessagesAppearOnce(Files.readString(logFile));
    }

    @Test
    public void initializeFileLogging_existingLegacyLogs_createsPonHubLogWithoutChangingLegacyFiles() throws Exception {
        List<String> legacyNames = List.of("addressbook.log", "addressbook.log.0", "addressbook.log.4",
                "addressbook.log.0.lck", "addressbook.log.future");
        for (String name : legacyNames) {
            Files.writeString(temporaryDirectory.resolve(name), "Preserve " + name);
        }

        assertMessagesAppearOnce(runLoggingProcess());

        assertMessagesAppearOnce(Files.readString(temporaryDirectory.resolve(LOG_FILE_NAME)));
        for (String name : legacyNames) {
            assertEquals("Preserve " + name, Files.readString(temporaryDirectory.resolve(name)));
        }
    }

    @Test
    public void initializeFileLogging_percentTokensInHome_writesInsideLiteralDirectory() throws Exception {
        Path home = Files.createDirectory(temporaryDirectory.resolve("home-%t-%h-%g-%u-%%"));

        String consoleOutput = runLoggingProcess(home);

        assertFalse(consoleOutput.contains("Unable to initialize file logging"), consoleOutput);
        assertMessagesAppearOnce(consoleOutput);
        Path logFile = home.resolve(LOG_FILE_NAME);
        assertTrue(Files.isRegularFile(logFile));
        assertMessagesAppearOnce(Files.readString(logFile));
        try (var files = Files.list(home)) {
            assertEquals(List.of(logFile), files.toList());
        }
    }

    @Test
    public void getLogger_logFileUnavailable_logsWarningAndContinuesConsoleLogging() throws Exception {
        // A directory blocks file creation even when the tests run with elevated filesystem permissions.
        Files.createDirectory(temporaryDirectory.resolve(LOG_FILE_NAME));

        String consoleOutput = runLoggingProcess();

        assertEquals(1, countOccurrences(consoleOutput,
                "Unable to initialize file logging; continuing with console logging."), consoleOutput);
        assertTrue(consoleOutput.contains("java.io.FileNotFoundException"), consoleOutput);
        assertMessagesAppearOnce(consoleOutput);
    }

    @Test
    public void getLogger_beforeFileInitialization_doesNotCreateOrChangeLog() throws Exception {
        Path logFile = temporaryDirectory.resolve(LOG_FILE_NAME);
        assertMessagesAppearOnce(runLoggingProcess("console-only"));
        assertFalse(Files.exists(logFile));
        Files.writeString(logFile, "Preserve selected original data");

        assertMessagesAppearOnce(runLoggingProcess("console-only"));
        assertEquals("Preserve selected original data", Files.readString(logFile));
    }

    @Test
    public void initializeFileLogging_symbolicLogPath_preservesOutsideTargetAndUsesConsole() throws Exception {
        Path home = Files.createDirectory(temporaryDirectory.resolve("application-home"));
        Path outside = temporaryDirectory.resolve("protected-original.json");
        Files.writeString(outside, "Keep outside data unchanged");
        Path logPath = home.resolve(LOG_FILE_NAME);
        try {
            Files.createSymbolicLink(logPath, outside);
        } catch (IOException | UnsupportedOperationException exception) {
            assumeTrue(false, "Symbolic links unavailable: " + exception);
        }

        String consoleOutput = runLoggingProcess(home);

        assertEquals(1, countOccurrences(consoleOutput,
                "Unable to initialize file logging; continuing with console logging."), consoleOutput);
        assertTrue(consoleOutput.contains("Refusing a symbolic log path"), consoleOutput);
        assertMessagesAppearOnce(consoleOutput);
        assertTrue(Files.isSymbolicLink(logPath));
        assertEquals("Keep outside data unchanged", Files.readString(outside));
        assertFalse(Files.exists(home.resolve(LOG_FILE_NAME + ".lck")));
    }

    @Test
    public void initializeFileLogging_hardLinkedLegacyLogs_preservesBytesAndUsesConsole() throws Exception {
        for (String name : List.of("ponhub.log.0", "PonHub.LOG.4", "ponhub.log.0.lck")) {
            Path home = Files.createDirectory(temporaryDirectory.resolve(name + "-home"));
            Path legacy = home.resolve("AddressBook.LOG.0");
            Files.writeString(legacy, "Preserve legacy log bytes");
            Path current = home.resolve(name);
            try {
                Files.createLink(current, legacy);
            } catch (IOException | UnsupportedOperationException exception) {
                assumeTrue(false, "Hard links unavailable: " + exception);
            }

            String output = runLoggingProcess(home);

            assertMessagesAppearOnce(output);
            assertTrue(output.contains("Unable to initialize file logging"), output);
            assertEquals("Preserve legacy log bytes", Files.readString(legacy));
            assertEquals("Preserve legacy log bytes", Files.readString(current));
            try (var files = Files.list(home)) {
                assertEquals(2, files.count());
            }
        }
    }

    @Test
    public void initializeFileLogging_caseVariedSymbolicLog_preservesTargetAndUsesConsole() throws Exception {
        Path home = Files.createDirectory(temporaryDirectory.resolve("case-log-home"));
        Path target = temporaryDirectory.resolve("original.json");
        Files.writeString(target, "Preserve target bytes");
        try {
            Files.createSymbolicLink(home.resolve("PonHub.LOG.0"), target);
        } catch (IOException | UnsupportedOperationException exception) {
            assumeTrue(false, "Symbolic links unavailable: " + exception);
        }

        String output = runLoggingProcess(home);

        assertMessagesAppearOnce(output);
        assertTrue(output.contains("Unable to initialize file logging"), output);
        assertEquals("Preserve target bytes", Files.readString(target));
        try (var files = Files.list(home)) {
            assertEquals(1, files.count());
        }
    }

    private String runLoggingProcess(String... options) throws Exception {
        return runLoggingProcess(temporaryDirectory, options);
    }

    /**
     * Starts a fresh JVM so the test exercises static initialization without changing this JVM's global loggers.
     */
    private String runLoggingProcess(Path workingDirectory, String... options) throws Exception {
        String javaExecutableName = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path javaExecutable = Path.of(System.getProperty("java.home"), "bin", javaExecutableName);
        // Gradle's worker classpath does not contain the application/test classes; use their actual locations.
        String classPath = Path.of(LogsCenter.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                + File.pathSeparator
                + Path.of(LoggingStartup.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        List<String> commandArguments = new ArrayList<>();
        commandArguments.add(javaExecutable.toString());
        // Reuse the active JaCoCo agent's options so child executions contribute to the same coverage report.
        ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(argument -> argument.matches("-javaagent:(?:.*[/\\\\])?jacocoagent\\.jar(?:=.*)?"))
                .map(this::resolveJacocoDestination)
                .forEach(commandArguments::add);
        commandArguments.addAll(List.of("-cp", classPath, LoggingStartup.class.getName()));
        commandArguments.addAll(List.of(options));
        Path outputFile = temporaryDirectory.resolve("console-output.txt");
        Process process = new ProcessBuilder(commandArguments)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(true)
                .redirectOutput(outputFile.toFile())
                .start();

        try {
            assertTrue(process.waitFor(15, TimeUnit.SECONDS), "Logging startup process timed out.");
            String output = Files.readString(outputFile);
            assertEquals(0, process.exitValue(), output);
            return output;
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Preserves the active agent's destination when the child process changes its working directory.
     */
    private String resolveJacocoDestination(String argument) {
        Matcher destinationOption = Pattern.compile("([=,]destfile=)([^,]+)").matcher(argument);
        if (!destinationOption.find()) {
            return argument;
        }
        String absoluteDestination = Path.of(destinationOption.group(2)).toAbsolutePath().toString();
        String resolvedOption = destinationOption.group(1) + absoluteDestination;
        return destinationOption.replaceFirst(Matcher.quoteReplacement(resolvedOption));
    }

    private void assertMessagesAppearOnce(String output) {
        assertEquals(1, countOccurrences(output, FIRST_MESSAGE), output);
        assertEquals(1, countOccurrences(output, REPEATED_MESSAGE), output);
        assertEquals(1, countOccurrences(output, OTHER_MESSAGE), output);
    }

    private int countOccurrences(String output, String message) {
        return output.split(Pattern.quote(message), -1).length - 1;
    }

    /**
     * Child-process entry point for exercising logging startup and repeated logger lookup.
     */
    public static class LoggingStartup {
        public static void main(String[] args) {
            Logger firstLogger = LogsCenter.getLogger(LoggingStartup.class);
            Logger repeatedLogger = LogsCenter.getLogger(LoggingStartup.class);
            Logger otherLogger = LogsCenter.getLogger("AnotherStartupLogger");
            if (!firstLogger.getName().equals("ponhub.LoggingStartup")
                    || !otherLogger.getName().equals("ponhub.AnotherStartupLogger")) {
                throw new AssertionError("Application loggers must use the PonHub namespace.");
            }

            if (!List.of(args).contains("console-only")) {
                LogsCenter.initializeFileLogging();
                LogsCenter.initializeFileLogging();
            }

            firstLogger.info(FIRST_MESSAGE);
            repeatedLogger.warning(REPEATED_MESSAGE);
            otherLogger.info(OTHER_MESSAGE);
        }
    }
}
