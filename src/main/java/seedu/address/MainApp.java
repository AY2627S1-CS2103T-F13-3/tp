package seedu.address;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.stage.Stage;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.util.ApplicationPaths;
import seedu.address.commons.util.StringUtil;
import seedu.address.logic.CanonicalLogicManager;
import seedu.address.logic.CanonicalPeopleSession;
import seedu.address.logic.Logic;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.ui.Ui;
import seedu.address.ui.UiManager;

/**
 * Runs the application.
 */
public class MainApp extends Application {

    public static final String VERSION = "v1.4";

    private static final Logger logger = LogsCenter.getLogger(MainApp.class);
    private static final Path USER_PREFS_FILE_PATH = Paths.get("preferences.json");
    private static final Path CANONICAL_DATA_FILE_PATH = Paths.get("data", "ponhub.json");

    protected Ui ui;
    protected Logic logic;
    protected CanonicalPeopleSession canonicalSession;

    private JsonUserPrefsStorage userPrefsStorage;
    private UserPrefs userPrefs;

    @Override
    public void init() throws Exception {
        logger.info("=============================[ Initializing PonHub ]===========================");
        super.init();

        Path applicationDirectory = ApplicationPaths.getHomeDirectory();
        initialize(selectDataPath(getParameters().getNamed(), applicationDirectory),
                requireWithinApplicationDirectory(USER_PREFS_FILE_PATH, applicationDirectory));
        LogsCenter.initializeFileLogging();
        ui = new UiManager(logic, canonicalSession.getDataFilePath(), canonicalSession.getDataLoadError());
    }

    /**
     * Initializes the application session and independent preferences before constructing native UI controls.
     * Missing canonical data starts empty and stays unwritten until a successful mutation.
     * Rejected data exposes a protected session without replacing its original bytes.
     *
     * @throws IOException if existing data and preference paths cannot be checked for aliasing.
     * @throws IllegalArgumentException if both paths identify the same file.
     */
    protected void initialize(Path dataPath, Path prefsPath) throws IOException {
        if (resolveStoragePath(dataPath.toAbsolutePath(), 0).equals(resolveStoragePath(prefsPath.toAbsolutePath(), 0))
                || (Files.exists(dataPath) && Files.exists(prefsPath) && Files.isSameFile(dataPath, prefsPath))) {
            throw new IllegalArgumentException("Canonical data and preferences must use separate files.");
        }
        Path applicationDirectory = resolveStoragePath(prefsPath.toAbsolutePath().getParent(), 0);
        rejectLogAlias(dataPath.toAbsolutePath(), applicationDirectory);
        rejectLogAlias(prefsPath.toAbsolutePath(), applicationDirectory);

        logger.info("Using canonical data file : " + dataPath);
        canonicalSession = new CanonicalPeopleSession(dataPath);
        canonicalSession.getDataLoadError().ifPresent(logger::warning);
        userPrefsStorage = new JsonUserPrefsStorage(prefsPath);
        userPrefs = initPrefs(userPrefsStorage);
        logic = new CanonicalLogicManager(canonicalSession, userPrefs);
    }

    /**
     * Selects a canonical path within the application's directory from the named {@code --data=PATH} argument.
     * Relative paths resolve beside the JAR; symbolic links cannot escape that directory.
     */
    static Path selectDataPath(Map<String, String> namedParameters, Path applicationDirectory) throws IOException {
        if (namedParameters.containsKey("data") && namedParameters.get("data").isBlank()) {
            throw new IllegalArgumentException("The --data=PATH argument must specify a nonblank file path.");
        }
        Path selected = namedParameters.containsKey("data") ? Paths.get(namedParameters.get("data"))
                : CANONICAL_DATA_FILE_PATH;
        Path dataPath = requireWithinApplicationDirectory(selected, applicationDirectory);
        rejectLogAlias(dataPath, applicationDirectory.toRealPath());
        return dataPath;
    }

    /**
     * Rejects log rotation names and their aliases before storage initialization or logging changes file bytes.
     */
    private static void rejectLogAlias(Path storagePath, Path applicationDirectory) throws IOException {
        Path resolvedPath = resolveStoragePath(storagePath, 0);
        if (applicationDirectory.equals(resolvedPath.getParent())
                && LogsCenter.isReservedLogFileName(resolvedPath.getFileName().toString())) {
            throw new IllegalArgumentException("Data and preferences must use separate files from application logs.");
        }
        if (Files.notExists(applicationDirectory)) {
            return;
        }
        try (var logFiles = Files.newDirectoryStream(applicationDirectory,
                path -> LogsCenter.isReservedLogFileName(path.getFileName().toString()))) {
            for (Path logFile : logFiles) {
                boolean isSameExistingFile = Files.exists(storagePath) && Files.exists(logFile)
                        && Files.isSameFile(storagePath, logFile);
                if (resolvedPath.equals(resolveStoragePath(logFile, 0)) || isSameExistingFile) {
                    throw new IllegalArgumentException("Data and preferences must use separate files from"
                            + " application logs.");
                }
            }
        }
    }

    /**
     * Rejects storage paths escaping the application's directory before loading operational data or preferences.
     */
    private static Path requireWithinApplicationDirectory(Path selected, Path applicationDirectory) throws IOException {
        Path home = applicationDirectory.toRealPath();
        Path storagePath = (selected.isAbsolute() ? selected : home.resolve(selected)).normalize();
        if (!resolveStoragePath(storagePath, 0).startsWith(home)) {
            throw new IllegalArgumentException("Data and preferences must stay within the application's directory: "
                    + home);
        }
        return storagePath;
    }

    /**
     * Resolves existing ancestors and dangling links before missing storage files can be created.
     */
    private static Path resolveStoragePath(Path path, int followedLinks) throws IOException {
        if (Files.isSymbolicLink(path)) {
            if (followedLinks >= 40) {
                throw new IOException("Too many symbolic links while checking storage path " + path);
            }
            Path target = Files.readSymbolicLink(path);
            return resolveStoragePath(target.isAbsolute() ? target : path.getParent().resolve(target),
                    followedLinks + 1);
        }
        if (Files.exists(path)) {
            return path.toRealPath();
        }
        Path parent = path.getParent();
        return parent == null ? path.normalize()
                : resolveStoragePath(parent, followedLinks).resolve(path.getFileName()).normalize();
    }

    /**
     * Returns a {@code UserPrefs} using the file at {@code storage}'s user prefs file path,
     * or a new {@code UserPrefs} with default configuration if errors occur when
     * reading from the file.
     */
    protected UserPrefs initPrefs(JsonUserPrefsStorage storage) {
        Path prefsFilePath = storage.getUserPrefsFilePath();
        logger.info("Using preference file : " + prefsFilePath);

        UserPrefs initializedPrefs;
        try {
            Optional<UserPrefs> prefsOptional = storage.readUserPrefs();
            if (prefsOptional.isEmpty()) {
                logger.info("Creating new preference file " + prefsFilePath);
            }
            initializedPrefs = prefsOptional.orElse(new UserPrefs());
        } catch (DataLoadingException e) {
            logger.warning("Preference file at " + prefsFilePath + " could not be loaded."
                    + " Using default preferences.");
            initializedPrefs = new UserPrefs();
        }

        //Update prefs file in case it was missing to begin with or there are new/unused fields
        try {
            storage.saveUserPrefs(initializedPrefs);
        } catch (IOException e) {
            logger.warning("Failed to save preference file : " + StringUtil.getDetails(e));
        }

        return initializedPrefs;
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting PonHub " + MainApp.VERSION);
        ui.start(primaryStage);
    }

    @Override
    public void stop() {
        logger.info("============================ [ Stopping PonHub ] =============================");
        try {
            userPrefsStorage.saveUserPrefs(userPrefs);
        } catch (IOException e) {
            logger.severe("Failed to save preferences " + StringUtil.getDetails(e));
        }
    }
}
