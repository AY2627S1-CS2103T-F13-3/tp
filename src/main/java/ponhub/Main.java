package ponhub;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javafx.application.Application;
import ponhub.commons.core.LogsCenter;
import ponhub.commons.util.ApplicationPaths;

/**
 * The main entry point to the application.
 *
 * This is a workaround for the following error when MainApp is made the
 * entry point of the application:
 *
 *     Error: JavaFX runtime components are missing, and are required to run this application
 *
 * The reason is that MainApp extends Application. In that case, the
 * LauncherHelper will check for the javafx.graphics module to be present
 * as a named module. We don't use JavaFX via the module system so it can't
 * find the javafx.graphics module, and so the launch is aborted.
 *
 * By having a separate main class (Main) that doesn't extend Application
 * to be the entry point of the application, we avoid this issue.
 */
public class Main {
    private static final String DATA_OPTION = "--data=";
    /** Prefix of the process serial number that older macOS Finder launches may append. */
    private static final String MAC_PROCESS_SERIAL_PREFIX = "-psn_";

    private static Logger logger = LogsCenter.getLogger(Main.class);

    /**
     * Launches the JavaFX application with the supplied command-line arguments.
     *
     * @throws IOException if startup file paths cannot be inspected safely.
     */
    public static void main(String[] args) throws IOException {
        Map<String, String> namedParameters;
        try {
            namedParameters = parseLaunchArguments(args);
        } catch (IllegalArgumentException failure) {
            System.err.println("Cannot start PonHub: " + failure.getMessage());
            System.exit(1);
            return;
        }
        try {
            MainApp.validateStartupPaths(namedParameters, ApplicationPaths.getHomeDirectory());
        } catch (IllegalArgumentException failure) {
            String selected = namedParameters.getOrDefault("data", "data/ponhub.json");
            System.err.println(("Cannot start PonHub with --data=\"" + selected + "\": "
                    + failure.getMessage() + " Choose a different data file within the application folder.")
                    .replaceAll("\\R", " "));
            System.exit(1);
            return;
        }

        logger.warning("The warnings about a 'restricted method in java.lang.System' "
            + "and 'enabling native access' appearing below (if any) can be ignored.");
        Application.launch(MainApp.class, args);
    }

    /**
     * Returns the named launch parameters, accepting only {@code --data=PATH}.
     * A mistyped option is rejected so that PonHub never silently opens the default data file instead.
     *
     * @throws IllegalArgumentException if any argument is not a {@code --data=PATH} option.
     */
    static Map<String, String> parseLaunchArguments(String... args) {
        Map<String, String> namedParameters = new HashMap<>();
        for (String argument : args) {
            if (argument.startsWith(DATA_OPTION)) {
                namedParameters.put("data", argument.substring(DATA_OPTION.length()));
            } else if (!argument.startsWith(MAC_PROCESS_SERIAL_PREFIX)) {
                throw new IllegalArgumentException(("Unrecognized launch argument \"" + argument + "\". "
                        + "Use --data=PATH (with an equals sign) to choose a data file, "
                        + "or launch without arguments to use data/ponhub.json.").replaceAll("\\R", " "));
            }
        }
        return namedParameters;
    }
}
