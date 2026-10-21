package ponhub.commons.core;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import ponhub.commons.util.ApplicationPaths;

/**
 * Configures and manages loggers and handlers, including their logging level.
 * Named {@link Logger}s can be obtained from this class.<br>
 * Loggers initially output messages to the console at the {@code INFO} level. File logging starts explicitly
 *   after the application validates its data paths. A new {@code .log} file is created after the log
 *   file reaches 5MB in size, up to a maximum of 5 files.<br>
 * If the log file cannot be opened, logging continues through the console.
 * Packaged logs stay in the JAR's folder even when launched from another working directory.
 */
public class LogsCenter {
    public static final String LOG_FILE_PREFIX = "ponhub.log";
    private static final String LEGACY_LOG_FILE_PREFIX = "addressbook.log";

    // Change this to a lower level (e.g., Level.FINE) to enable more detailed log messages
    private static final Level LOG_LEVEL = Level.INFO;

    private static final int MAX_FILE_COUNT = 5;
    private static final int MAX_FILE_SIZE_IN_BYTES = (int) (Math.pow(2, 20) * 5); // 5MB
    private static Logger baseLogger; // to be used as the parent of all other loggers created by this class.
    private static boolean isFileLoggingInitialized;

    // This static block ensures essential loggers are created early
    static {
        setBaseLogger();
    }

    /**
     * Reserves current and legacy log names, including rotation, lock and future suffixes.
     * Case variants are reserved on every platform so data paths remain safe when moved between filesystems.
     * Legacy logs remain reserved for compatibility but are never opened by this logger.
     */
    public static boolean isReservedLogFileName(String name) {
        requireNonNull(name);
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.startsWith(LOG_FILE_PREFIX) || normalized.startsWith(LEGACY_LOG_FILE_PREFIX);
    }

    /**
     * Creates a logger with the given name prefixed by the {@code baseLogger}'s name so that the created logger
     * becomes a descendant of the {@code baseLogger}. Furthermore, the returned logger will have the same log handlers
     * as the {@code baseLogger}.
     */
    public static Logger getLogger(String name) {
        // Java organizes loggers into a hierarchy based on their names (using '.' as a separator, similar to how Java
        // packages form a hierarchy). Furthermore, loggers without a level inherit the level of their parent logger.
        // By prefixing names of all loggers with baseLogger's name + ".", we make the baseLogger the parent of all
        // loggers. This allows us to change the level of all loggers simply by changing the baseLogger level.
        Logger logger = Logger.getLogger(baseLogger.getName() + "." + name);
        removeHandlers(logger);
        logger.setUseParentHandlers(true);
        return logger;
    }

    /**
     * Creates a Logger for the given class name.
     */
    public static <T> Logger getLogger(Class<T> clazz) {
        requireNonNull(clazz);
        return getLogger(clazz.getSimpleName());
    }

    /**
     * Attempts file logging once after the application has validated its operational and preference paths.
     * Existing symbolic log paths and aliases to legacy logs disable file logging to preserve their targets.
     * Failures retain the existing console handler and recovery warning.
     */
    public static synchronized void initializeFileLogging() {
        if (isFileLoggingInitialized) {
            return;
        }
        isFileLoggingInitialized = true;
        try {
            Path home = ApplicationPaths.getHomeDirectory();
            rejectUnsafeLogs(home);
            // FileHandler interprets percent tokens, so retain literal characters in the application folder.
            String filePattern = home.resolve(LOG_FILE_PREFIX).toString().replace("%", "%%");
            FileHandler fileHandler = new FileHandler(filePattern, MAX_FILE_SIZE_IN_BYTES, MAX_FILE_COUNT, true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);
            baseLogger.addHandler(fileHandler);
        } catch (IOException e) {
            baseLogger.log(Level.WARNING, "Unable to initialize file logging; continuing with console logging.", e);
        }
    }

    /**
     * Checks every current log name before FileHandler can open, lock or rotate files. Case variants
     * are included, and file identity detects hard links to legacy logs as well as symbolic aliases.
     */
    private static void rejectUnsafeLogs(Path home) throws IOException {
        List<Path> currentLogs = new ArrayList<>();
        List<Path> legacyLogs = new ArrayList<>();
        try (DirectoryStream<Path> logPaths = Files.newDirectoryStream(home,
                path -> isReservedLogFileName(path.getFileName().toString()))) {
            for (Path logPath : logPaths) {
                String name = logPath.getFileName().toString().toLowerCase(Locale.ROOT);
                if (name.startsWith(LOG_FILE_PREFIX)) {
                    if (Files.isSymbolicLink(logPath)) {
                        throw new IOException("Refusing a symbolic log path: " + logPath);
                    }
                    currentLogs.add(logPath);
                } else {
                    legacyLogs.add(logPath);
                }
            }
        }
        for (Path current : currentLogs) {
            for (Path legacy : legacyLogs) {
                if (!Files.notExists(legacy) && Files.isSameFile(current, legacy)) {
                    throw new IOException("Refusing a log path aliased to a legacy log: " + current);
                }
            }
        }
    }

    /**
     * Removes all handlers from the {@code logger}.
     */
    private static void removeHandlers(Logger logger) {
        Arrays.stream(logger.getHandlers())
                .forEach(logger::removeHandler);
    }

    /**
     * Creates a logger named 'ponhub' with console output, leaving file output until explicit initialization.
     * Sets it as the {@code baseLogger}, to be used as the parent logger of all other loggers.
     */
    private static void setBaseLogger() {
        baseLogger = Logger.getLogger("ponhub");
        baseLogger.setUseParentHandlers(false);
        removeHandlers(baseLogger);
        // set the level of the baseLogger, which will be inherited by other loggers
        baseLogger.setLevel(LOG_LEVEL);

        // Level.ALL is used as the level for the handlers because the baseLogger filters the log messages by level
        // already; there is no need to control log message level of the handlers.

        // add a ConsoleHandler to log to the console
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        baseLogger.addHandler(consoleHandler);

    }


}
