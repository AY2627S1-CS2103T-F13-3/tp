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
    private static Logger logger = LogsCenter.getLogger(Main.class);

    /**
     * Launches the JavaFX application with the supplied command-line arguments.
     *
     * @throws IOException if startup file paths cannot be inspected safely.
     */
    public static void main(String[] args) throws IOException {
        Map<String, String> namedParameters = new HashMap<>();
        for (String argument : args) {
            if (argument != null && argument.startsWith("--data=")) {
                namedParameters.put("data", argument.substring("--data=".length()));
            }
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
}
