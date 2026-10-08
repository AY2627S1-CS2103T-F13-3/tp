package seedu.address.ui;

import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import seedu.address.MainApp;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.Logic;

/**
 * The manager of the UI component.
 */
public class UiManager implements Ui {

    public static final String ALERT_DIALOG_PANE_FIELD_ID = "alertDialogPane";

    private static final Logger logger = LogsCenter.getLogger(UiManager.class);
    private static final String ICON_APPLICATION = "/images/address_book_32.png";

    private Logic logic;
    private Path dataFilePath;
    private final Optional<String> dataLoadError;

    /**
     * Creates a {@code UiManager} with the given {@code Logic} and the data file path
     * to show in the status bar.
     */
    public UiManager(Logic logic, Path dataFilePath) {
        this(logic, dataFilePath, Optional.empty());
    }

    /** Creates a UI that also displays recovery guidance for a protected startup session. */
    public UiManager(Logic logic, Path dataFilePath, Optional<String> dataLoadError) {
        this.logic = logic;
        this.dataFilePath = dataFilePath;
        this.dataLoadError = dataLoadError;
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting UI...");

        try {
            initializeMainWindow(primaryStage);
            if (dataLoadError.isPresent()) {
                showAlertDialogAndWait(primaryStage, AlertType.WARNING, "Data file protected",
                        "Your saved data could not be loaded", dataLoadError.get());
            }
        } catch (Throwable e) {
            showFatalErrorDialogAndShutdown(primaryStage, "Fatal error during initializing", e);
        }
    }

    /**
     * Creates and displays the main window and its inner parts.
     */
    void initializeMainWindow(Stage primaryStage) {
        primaryStage.getIcons().add(getImage(ICON_APPLICATION));
        MainWindow mainWindow = new MainWindow(primaryStage, logic, dataFilePath);
        mainWindow.show(); //This should be called before creating other UI parts
        mainWindow.fillInnerParts();
    }

    private Image getImage(String imagePath) {
        return new Image(MainApp.class.getResourceAsStream(imagePath));
    }

    /**
     * Shows an alert dialog on {@code owner} with the given parameters.
     * This method only returns after the user has closed the alert dialog.
     */
    void showAlertDialogAndWait(Stage owner, AlertType type, String title, String headerText,
            String contentText) {
        final Alert alert = new Alert(type);
        alert.getDialogPane().getStylesheets().add("view/DarkTheme.css");
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.getDialogPane().setId(ALERT_DIALOG_PANE_FIELD_ID);
        alert.showAndWait();
    }

    /**
     * Shows an error alert dialog with {@code title} and error message, {@code e},
     * and exits the application after the user has closed the alert dialog.
     */
    private void showFatalErrorDialogAndShutdown(Stage owner, String title, Throwable e) {
        logger.log(Level.SEVERE, title, e);
        try {
            showAlertDialogAndWait(owner, AlertType.ERROR, title, e.getMessage(), e.toString());
        } catch (Throwable dialogFailure) {
            logger.log(Level.SEVERE, "Unable to show the fatal error dialog", dialogFailure);
        } finally {
            shutdown();
        }
    }

    /**
     * Stops JavaFX and exits the process after a fatal startup error.
     */
    void shutdown() {
        Platform.exit();
        System.exit(1);
    }

}
