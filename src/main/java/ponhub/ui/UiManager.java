package ponhub.ui;

import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import ponhub.MainApp;
import ponhub.commons.core.LogsCenter;
import ponhub.logic.Logic;

/**
 * The manager of the UI component.
 */
public class UiManager implements Ui {

    public static final String ALERT_DIALOG_PANE_FIELD_ID = "alertDialogPane";

    private static final Logger logger = LogsCenter.getLogger(UiManager.class);
    private static final String ICON_APPLICATION = "/images/ponhub_icon.png";

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

    /**
     * Creates a UI that also displays recovery guidance for a protected startup session.
     */
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
        DockIcon.install(ICON_APPLICATION);
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
        createAlert(owner, type, title, headerText, contentText).showAndWait();
    }

    /**
     * Builds a bounded, resizable dialog whose complete diagnostic text remains readable and copyable.
     */
    Alert createAlert(Stage owner, AlertType type, String title, String headerText, String contentText) {
        final Alert alert = new Alert(type);
        alert.getDialogPane().getStylesheets().add("view/DarkTheme.css");
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        Label summary = new Label(type == AlertType.WARNING
                ? "Your original file is unchanged. Operational writes are blocked. Back up the original "
                        + "and follow the recovery instructions below, then restart."
                : "Read the details below. You can select and copy the text.");
        summary.setWrapText(true);
        summary.setMaxWidth(Double.MAX_VALUE);
        TextArea details = new TextArea(contentText);
        details.setId("alertDetails");
        details.setAccessibleText("Recovery and diagnostic details");
        details.setEditable(false);
        details.setWrapText(true);
        details.setFocusTraversable(true);
        details.setMinHeight(60);
        details.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox content = new VBox(12, summary, details);
        VBox.setVgrow(details, Priority.ALWAYS);
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setId(ALERT_DIALOG_PANE_FIELD_ID);
        Rectangle2D workArea = (owner == null ? Screen.getPrimary() : Screen.getScreensForRectangle(
                owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight()).stream()
                        .findFirst().orElse(Screen.getPrimary())).getVisualBounds();
        alert.getDialogPane().setPrefSize(Math.max(1, Math.min(640, workArea.getWidth() - 40)),
                Math.max(1, Math.min(460, workArea.getHeight() - 80)));
        alert.setResizable(true);
        alert.setOnShown(event -> {
            details.requestFocus();
            details.positionCaret(0);
            details.setScrollTop(0);
        });
        return alert;
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
