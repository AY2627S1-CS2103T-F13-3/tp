package ponhub.ui;

import java.nio.file.Path;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import ponhub.commons.core.GuiSettings;
import ponhub.logic.LessonRetrieval.ResultKind;
import ponhub.logic.Logic;
import ponhub.logic.commands.CommandResult;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.person.PersonRole;

/**
 * The Main Window. Provides the basic application layout containing
 * a menu bar and space where other JavaFX elements can be placed.
 */
public class MainWindow extends UiPart<Stage> {

    private static final String FXML = "MainWindow.fxml";
    private static final int CREDITS_CLICK_THRESHOLD = 69;

    private Stage primaryStage;
    private Logic logic;
    private Path dataFilePath;
    private final LogoSoundPlayer logoSound;
    private int logoClicks;
    private CreditsDialog creditsDialog;
    private SequentialTransition badgeReset;

    // Independent Ui parts residing in this Ui container
    private PersonRecordListPanel personListPanel;
    private ResultDisplay resultDisplay;

    @FXML
    private HBox ponhubWordmark;

    @FXML
    private Button lessonsButton;

    @FXML
    private Button studentLessonsButton;

    @FXML
    private Button attendanceButton;

    @FXML
    private StackPane commandBoxPlaceholder;

    @FXML
    private MenuItem helpMenuItem;

    @FXML
    private StackPane personListPanelPlaceholder;

    @FXML
    private StackPane lessonListPanelPlaceholder;

    @FXML
    private StackPane resultDisplayPlaceholder;

    @FXML
    private StackPane statusbarPlaceholder;

    /**
     * Creates a {@code MainWindow} with the given {@code Stage}, {@code Logic},
     * and the data file path to show in the status bar.
     */
    public MainWindow(Stage primaryStage, Logic logic, Path dataFilePath) {
        this(primaryStage, logic, dataFilePath, new LogoSoundPlayer());
    }

    MainWindow(Stage primaryStage, Logic logic, Path dataFilePath, LogoSoundPlayer logoSound) {
        super(FXML, primaryStage);
        this.logoSound = logoSound;

        // Set dependencies
        this.primaryStage = primaryStage;
        this.logic = logic;
        this.dataFilePath = dataFilePath;

        // Configure the UI
        setWindowDefaultSize(logic.getGuiSettings());

        setAccelerators();
        configureLogoSound();

    }

    /**
     * Makes the existing wordmark clickable without focusing or changing the command field.
     */
    private void configureLogoSound() {
        Label badge = (Label) ponhubWordmark.lookup(".wordmark-hub");
        ponhubWordmark.applyCss();
        double badgeWidth = badge.prefWidth(-1);
        badge.setMinWidth(badgeWidth);
        badge.setPrefWidth(badgeWidth);
        badge.setMaxWidth(badgeWidth);
        badge.setTooltip(new Tooltip("Curiosity has consequences."));
        ScaleTransition pulse = new ScaleTransition(Duration.millis(110), badge);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.08);
        pulse.setToY(1.08);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        ponhubWordmark.setMouseTransparent(false);
        ponhubWordmark.setCursor(Cursor.HAND);
        ponhubWordmark.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                stopBadgeReset(badge);
                logoSound.play();
                logoClicks++;
                badge.setText(Integer.toString(logoClicks));
                badge.setAccessibleText("Mysterious counter: " + logoClicks);
                pulse.playFromStart();
                startBadgeReset(badge);
                if (logoClicks == CREDITS_CLICK_THRESHOLD) {
                    logoClicks = 0;
                    showCredits(badge);
                }
                event.consume();
            }
        });
        primaryStage.addEventHandler(WindowEvent.WINDOW_HIDDEN, event -> {
            stopBadgeReset(badge);
            pulse.stop();
            badge.setScaleX(1);
            badge.setScaleY(1);
            logoSound.close();
            if (creditsDialog != null) {
                creditsDialog.close();
            }
        });
    }

    private void showCredits(Label badge) {
        if (creditsDialog != null && creditsDialog.isShowing()) {
            return;
        }
        Node previousFocus = primaryStage.getScene().getFocusOwner();
        TextInputControl draft = previousFocus instanceof TextInputControl ? (TextInputControl) previousFocus : null;
        int anchor = draft == null ? 0 : draft.getAnchor();
        int caret = draft == null ? 0 : draft.getCaretPosition();
        creditsDialog = new CreditsDialog(primaryStage);
        creditsDialog.setOnHidden(event -> {
            restoreBadge(badge);
            Platform.runLater(() -> {
                if (primaryStage.isShowing() && previousFocus != null) {
                    previousFocus.requestFocus();
                    if (draft != null) {
                        draft.selectRange(anchor, caret);
                    }
                }
            });
        });
        creditsDialog.show();
    }

    private void stopBadgeReset(Label badge) {
        if (badgeReset != null) {
            badgeReset.stop();
        }
        badge.setOpacity(1);
    }

    private void restoreBadge(Label badge) {
        stopBadgeReset(badge);
        if (!primaryStage.isShowing()) {
            badge.setText("hub");
            badge.setAccessibleText("hub");
            return;
        }
        startBadgeReset(badge);
    }

    private void startBadgeReset(Label badge) {
        PauseTransition hold = new PauseTransition(Duration.millis(200));
        FadeTransition fadeOut = new FadeTransition(Duration.millis(650), badge);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> {
            badge.setText("hub");
            badge.setAccessibleText("hub");
        });
        FadeTransition fadeIn = new FadeTransition(Duration.millis(650), badge);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        badgeReset = new SequentialTransition(hold, fadeOut, fadeIn);
        badgeReset.playFromStart();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    private void setAccelerators() {
        setAccelerator(helpMenuItem, KeyCombination.valueOf("F1"));
    }

    /**
     * Sets the accelerator of a MenuItem.
     *
     * @param menuItem The menu item to activate.
     * @param keyCombination The key combination of the accelerator.
     */
    private void setAccelerator(MenuItem menuItem, KeyCombination keyCombination) {
        menuItem.setAccelerator(keyCombination);

        /*
         * TODO: the code below can be removed once the bug reported here
         * https://bugs.openjdk.java.net/browse/JDK-8131666
         * is fixed in a later version of the SDK.
         *
         * According to the bug report, TextInputControl (TextField, TextArea) will
         * consume function-key events. Because CommandBox contains a TextField and
         * ResultDisplay contains a TextArea, some accelerators (e.g., F1) will
         * not work when the focus is in them because the key event is consumed by
         * the TextInputControl(s).
         *
         * For now, we add the following event filter to capture such key events and open
         * inline help so as to support accelerators even when focus is
         * in CommandBox or ResultDisplay.
         */
        getRoot().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getTarget() instanceof TextInputControl && keyCombination.match(event)) {
                menuItem.getOnAction().handle(new ActionEvent());
                event.consume();
            }
        });
    }

    /**
     * Fills up all the placeholders of this window.
     */
    void fillInnerParts() {
        personListPanel = new PersonRecordListPanel(logic.getPersonRecords(), logic.personSearchContextProperty());
        personListPanelPlaceholder.getChildren().add(personListPanel.getRoot());

        LessonListPanel lessonListPanel = new LessonListPanel(logic.getDisplayedLessons(),
                logic.lessonResultContextProperty());
        HistoryListPanel historyListPanel = new HistoryListPanel(logic.getDisplayedHistory(),
                logic.historyResultContextProperty());
        Region lessonRoot = lessonListPanel.getRoot();
        Region historyRoot = historyListPanel.getRoot();
        lessonRoot.visibleProperty().bind(logic.historyViewActiveProperty().not());
        lessonRoot.managedProperty().bind(lessonRoot.visibleProperty());
        historyRoot.visibleProperty().bind(logic.historyViewActiveProperty());
        historyRoot.managedProperty().bind(historyRoot.visibleProperty());
        lessonListPanelPlaceholder.getChildren().addAll(lessonRoot, historyRoot);
        logic.historyViewActiveProperty().addListener((observable, previous, current) -> updateNavigation());
        logic.lessonResultKindProperty().addListener((observable, previous, current) -> updateNavigation());
        updateNavigation();

        resultDisplay = new ResultDisplay();
        resultDisplayPlaceholder.getChildren().add(resultDisplay.getRoot());

        StatusBarFooter statusBarFooter = new StatusBarFooter(dataFilePath);
        statusbarPlaceholder.getChildren().add(statusBarFooter.getRoot());

        CommandBox commandBox = new CommandBox(this::executeCommand, logic.getCommandWords());
        commandBoxPlaceholder.getChildren().add(commandBox.getRoot());
        commandBox.requestFocus();
    }

    private void updateNavigation() {
        boolean isHistoryActive = logic.historyViewActiveProperty().get();
        ResultKind visibleLessonKind = isHistoryActive ? null : logic.lessonResultKindProperty().get();
        PseudoClass active = PseudoClass.getPseudoClass("active");
        lessonsButton.pseudoClassStateChanged(active, visibleLessonKind == ResultKind.ALL_LESSONS);
        studentLessonsButton.pseudoClassStateChanged(active, visibleLessonKind == ResultKind.STUDENT_LESSONS);
        attendanceButton.pseudoClassStateChanged(active, isHistoryActive);
    }

    @FXML
    private void handleLessons() {
        executeViewingCommand("lessons");
    }

    @FXML
    private void handleStudentLessons() {
        executeStudentViewingCommand("lessons ");
    }

    @FXML
    private void handleAttendance() {
        executeStudentViewingCommand("history ");
    }

    /**
     * Resolves the selected identity against current displayed positions immediately before execution.
     */
    private void executeStudentViewingCommand(String commandPrefix) {
        var selectedId = personListPanel.getSelectedPersonId();
        var people = logic.getPersonRecords();
        for (int index = 0; index < people.size(); index++) {
            var person = people.get(index);
            if (selectedId.filter(person.getId()::equals).isPresent()) {
                if (person.getId().getRole() != PersonRole.STUDENT) {
                    resultDisplay.setFeedbackToUser("Select a Student card for this view, or enter "
                            + "lessons STUDENT_INDEX or history STUDENT_INDEX.");
                    return;
                }
                executeViewingCommand(commandPrefix + (index + 1));
                return;
            }
        }
        resultDisplay.setFeedbackToUser("No current Student card is selected. Select a Student card, or enter "
                + "lessons STUDENT_INDEX or history STUDENT_INDEX using the current people list.");
    }

    /**
     * Runs optional viewing controls through the same validation and feedback route as typed commands.
     */
    private void executeViewingCommand(String command) {
        try {
            executeCommand(command);
        } catch (CommandException | ParseException e) {
            // executeCommand has displayed the error; the logic boundary records its safe category.
        }
    }

    /**
     * Restores preferences within the available desktop work area.
     */
    private void setWindowDefaultSize(GuiSettings guiSettings) {
        Rectangle2D bounds = WindowPlacement.fit(guiSettings,
                Screen.getScreens().stream().map(Screen::getVisualBounds).toList(),
                Screen.getPrimary().getVisualBounds());
        primaryStage.setMinWidth(Math.min(WindowPlacement.MINIMUM_WIDTH, bounds.getWidth()));
        primaryStage.setMinHeight(Math.min(WindowPlacement.MINIMUM_HEIGHT, bounds.getHeight()));
        primaryStage.setWidth(bounds.getWidth());
        primaryStage.setHeight(bounds.getHeight());
        primaryStage.setX(bounds.getMinX());
        primaryStage.setY(bounds.getMinY());
    }

    /**
     * Displays the same inline guidance as the help command without changing command-box input.
     */
    @FXML
    public void handleHelp() {
        try {
            executeCommand("help");
        } catch (CommandException | ParseException e) {
            // executeCommand has displayed the error; the logic boundary records its safe category.
        }
    }

    void show() {
        primaryStage.show();
    }

    /**
     * Closes the application.
     */
    @FXML
    private void handleExit() {
        GuiSettings guiSettings = new GuiSettings(primaryStage.getWidth(), primaryStage.getHeight(),
                (int) primaryStage.getX(), (int) primaryStage.getY());
        logic.setGuiSettings(guiSettings);
        primaryStage.hide();
    }

    /**
     * Executes the command and returns the result.
     *
     * @see ponhub.logic.Logic#execute(String)
     */
    private CommandResult executeCommand(String commandText) throws CommandException, ParseException {
        try {
            var selectedPerson = personListPanel.getSelectedPersonId();
            CommandResult commandResult = logic.execute(commandText);
            selectedPerson.ifPresent(personListPanel::restoreSelection);
            resultDisplay.setFeedbackToUser(commandResult.getFeedbackToUser());

            if (commandResult.isExit()) {
                handleExit();
            }

            return commandResult;
        } catch (CommandException | ParseException e) {
            resultDisplay.setFeedbackToUser(e.getMessage());
            throw e;
        }
    }
}
