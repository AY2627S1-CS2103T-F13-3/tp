package seedu.address.commons.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
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
    private static final String LOG_FILE_NAME = "addressbook.log.0";
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
    public void getLogger_logFileUnavailable_logsWarningAndContinuesConsoleLogging() throws Exception {
        // A directory blocks file creation even when the tests run with elevated filesystem permissions.
        Files.createDirectory(temporaryDirectory.resolve(LOG_FILE_NAME));

        String consoleOutput = runLoggingProcess();

        assertEquals(1, countOccurrences(consoleOutput,
                "Unable to initialize file logging; continuing with console logging."), consoleOutput);
        assertTrue(consoleOutput.contains("java.io.FileNotFoundException"), consoleOutput);
        assertMessagesAppearOnce(consoleOutput);
    }

    /**
     * Starts a fresh JVM so the test exercises static initialization without changing this JVM's global loggers.
     */
    private String runLoggingProcess() throws Exception {
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
        Path outputFile = temporaryDirectory.resolve("console-output.txt");
        Process process = new ProcessBuilder(commandArguments)
                .directory(temporaryDirectory.toFile())
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

            firstLogger.info(FIRST_MESSAGE);
            repeatedLogger.warning(REPEATED_MESSAGE);
            otherLogger.info(OTHER_MESSAGE);
        }
    }
}
