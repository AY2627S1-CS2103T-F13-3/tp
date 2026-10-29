package ponhub.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import ponhub.logic.commands.CommandResult;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.exceptions.ParseException;

/**
 * The UI component that is responsible for receiving user command inputs.
 */
public class CommandBox extends UiPart<Region> {

    public static final String ERROR_STYLE_CLASS = "error";
    public static final String MESSAGE_REJECTED_INPUT = "Input rejected: commands must be on one line without tabs "
            + "or control characters. Your draft was kept; edit it before submitting.";
    static final int HISTORY_LIMIT = 50;
    private static final String FXML = "CommandBox.fxml";

    private final CommandExecutor commandExecutor;
    private final CommandWordCompleter completer;
    private boolean isCompleting;
    private final List<String> submittedCommands = new ArrayList<>();
    private int historyPosition;
    private Draft unfinishedDraft;
    private boolean isRecalling;
    private boolean inputRejected;
    private final Consumer<String> inputFeedback;

    @FXML
    private TextField commandTextField;

    /**
     * Creates a command input with the executor and the active session's immutable command catalogue.
     */
    public CommandBox(CommandExecutor commandExecutor, List<String> commandWords, Consumer<String> inputFeedback) {
        super(FXML);
        this.commandExecutor = commandExecutor;
        this.inputFeedback = java.util.Objects.requireNonNull(inputFeedback);
        completer = new CommandWordCompleter(commandWords);
        // TextFormatter receives raw replacements before TextField silently strips controls.
        commandTextField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getText().codePoints().anyMatch(codePoint -> Character.isISOControl(codePoint)
                    || Character.getType(codePoint) == Character.LINE_SEPARATOR
                    || Character.getType(codePoint) == Character.PARAGRAPH_SEPARATOR)) {
                rejectInput();
                return null;
            }
            return change;
        }));
        // calls #setStyleToDefault() whenever there is a change to the text of the command box.
        commandTextField.textProperty().addListener((unused1, unused2, unused3) -> {
            inputRejected = false;
            setStyleToDefault();
        });
        commandTextField.textProperty().addListener((observable, previous, current) -> resetCompletion());
        commandTextField.textProperty().addListener((observable, previous, current) -> {
            if (!isRecalling) {
                resetRecall();
            }
        });
        commandTextField.caretPositionProperty().addListener((observable, previous, current) -> resetCompletion());
        commandTextField.anchorProperty().addListener((observable, previous, current) -> resetCompletion());
        commandTextField.focusedProperty().addListener((observable, previous, current) -> resetCompletion());
        commandTextField.addEventFilter(KeyEvent.KEY_PRESSED, this::handleCompletion);
        commandTextField.addEventFilter(KeyEvent.KEY_PRESSED, this::handleRecall);
    }

    private void resetRecall() {
        historyPosition = submittedCommands.size();
        unfinishedDraft = null;
    }

    private void handleRecall(KeyEvent event) {
        if ((event.getCode() != KeyCode.UP && event.getCode() != KeyCode.DOWN) || event.isShiftDown()
                || event.isControlDown() || event.isAltDown() || event.isMetaDown() || submittedCommands.isEmpty()) {
            return;
        }
        int newest = submittedCommands.size();
        if (event.getCode() == KeyCode.DOWN && historyPosition == newest) {
            return;
        }
        if (historyPosition == newest) {
            unfinishedDraft = new Draft(commandTextField.getText(), commandTextField.getAnchor(),
                    commandTextField.getCaretPosition());
        }
        historyPosition = event.getCode() == KeyCode.UP ? Math.max(0, historyPosition - 1) : historyPosition + 1;
        Draft recalled = historyPosition == newest ? unfinishedDraft
                : new Draft(submittedCommands.get(historyPosition), submittedCommands.get(historyPosition).length(),
                        submittedCommands.get(historyPosition).length());
        isRecalling = true;
        try {
            commandTextField.setText(recalled.text());
            commandTextField.selectRange(recalled.anchor(), recalled.caret());
        } finally {
            isRecalling = false;
        }
        event.consume();
    }

    private void rememberSubmission(String command) {
        if (!command.isBlank()) {
            submittedCommands.add(command);
            if (submittedCommands.size() > HISTORY_LIMIT) {
                submittedCommands.removeFirst();
            }
        }
        resetRecall();
    }

    /**
     * The original draft includes its selection direction as well as its text.
     */
    private record Draft(String text, int anchor, int caret) {
    }

    private void resetCompletion() {
        if (!isCompleting) {
            completer.reset();
        }
    }

    private void handleCompletion(KeyEvent event) {
        if (event.getCode() != KeyCode.TAB || event.isShiftDown() || event.isControlDown()
                || event.isAltDown() || event.isMetaDown() || commandTextField.getSelection().getLength() != 0) {
            return;
        }
        completer.complete(commandTextField.getText(), commandTextField.getCaretPosition()).ifPresent(completion -> {
            isCompleting = true;
            try {
                commandTextField.setText(completion.text());
                commandTextField.positionCaret(completion.caret());
            } finally {
                isCompleting = false;
            }
            event.consume();
        });
    }

    /**
     * Gives keyboard focus to the command input.
     */
    void requestFocus() {
        commandTextField.requestFocus();
    }

    /**
     * Handles the Enter button pressed event.
     */
    @FXML
    private void handleCommandEntered() {
        if (inputRejected) {
            rejectInput();
            return;
        }
        String commandText = commandTextField.getText();
        if (commandText.equals("")) {
            return;
        }

        rememberSubmission(commandText);
        try {
            commandExecutor.execute(commandText);
            commandTextField.setText("");
        } catch (CommandException | ParseException e) {
            setStyleToIndicateCommandFailure();
        }
    }

    /**
     * Keeps a rejected replacement from turning an existing draft into an unintended submission.
     */
    private void rejectInput() {
        inputRejected = true;
        inputFeedback.accept(MESSAGE_REJECTED_INPUT);
        setStyleToIndicateCommandFailure();
    }

    /**
     * Sets the command box style to use the default style.
     */
    private void setStyleToDefault() {
        commandTextField.getStyleClass().remove(ERROR_STYLE_CLASS);
    }

    /**
     * Sets the command box style to indicate a failed command.
     */
    private void setStyleToIndicateCommandFailure() {
        ObservableList<String> styleClass = commandTextField.getStyleClass();

        if (styleClass.contains(ERROR_STYLE_CLASS)) {
            return;
        }

        styleClass.add(ERROR_STYLE_CLASS);
    }

    /**
     * Represents a function that can execute commands.
     */
    @FunctionalInterface
    public interface CommandExecutor {
        /**
         * Executes the command and returns the result.
         *
         * @see ponhub.logic.Logic#execute(String)
         */
        CommandResult execute(String commandText) throws CommandException, ParseException;
    }

}
