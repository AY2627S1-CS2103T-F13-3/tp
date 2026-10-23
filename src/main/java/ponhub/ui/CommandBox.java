package ponhub.ui;

import java.util.List;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
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
    private static final String FXML = "CommandBox.fxml";

    private final CommandExecutor commandExecutor;
    private final CommandWordCompleter completer;
    private boolean isCompleting;

    @FXML
    private TextField commandTextField;

    /**
     * Creates a command input with the executor and the active session's immutable command catalogue.
     */
    public CommandBox(CommandExecutor commandExecutor, List<String> commandWords) {
        super(FXML);
        this.commandExecutor = commandExecutor;
        completer = new CommandWordCompleter(commandWords);
        // calls #setStyleToDefault() whenever there is a change to the text of the command box.
        commandTextField.textProperty().addListener((unused1, unused2, unused3) -> setStyleToDefault());
        commandTextField.textProperty().addListener((observable, previous, current) -> resetCompletion());
        commandTextField.caretPositionProperty().addListener((observable, previous, current) -> resetCompletion());
        commandTextField.anchorProperty().addListener((observable, previous, current) -> resetCompletion());
        commandTextField.focusedProperty().addListener((observable, previous, current) -> resetCompletion());
        commandTextField.addEventFilter(KeyEvent.KEY_PRESSED, this::handleCompletion);
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
        String commandText = commandTextField.getText();
        if (commandText.equals("")) {
            return;
        }

        try {
            commandExecutor.execute(commandText);
            commandTextField.setText("");
        } catch (CommandException | ParseException e) {
            setStyleToIndicateCommandFailure();
        }
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
