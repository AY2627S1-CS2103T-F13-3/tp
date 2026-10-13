package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static seedu.address.testutil.FxTestUtil.runOnFxThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.control.TextField;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;

public class CommandBoxTest {

    @Test
    public void enter_emptyInput_doesNotExecute() throws Exception {
        runOnFxThread(() -> {
            List<String> commands = new ArrayList<>();
            TextField input = createInput(command -> {
                commands.add(command);
                return new CommandResult("Success");
            });
            List<String> originalStyles = List.copyOf(input.getStyleClass());

            enter(input);
            enter(input);

            assertEquals(List.of(), commands);
            assertEquals("", input.getText());
            assertEquals(originalStyles, input.getStyleClass());
        });
    }

    @Test
    public void enter_success_forwardsUnchangedOnceAndClearsInput() throws Exception {
        runOnFxThread(() -> {
            List<String> commands = new ArrayList<>();
            TextField input = createInput(command -> {
                commands.add(command);
                return new CommandResult("Success");
            });
            List<String> originalStyles = List.copyOf(input.getStyleClass());
            String command = "  add r/student n/Zoë Tan l/P1 pp/91234567  ";
            input.setText(command);
            assertEquals(List.of(), commands, "Editing must not execute the draft.");

            enter(input);

            assertEquals(List.of(command), commands);
            assertEquals("", input.getText());
            assertEquals(originalStyles, input.getStyleClass());
            enter(input);
            assertEquals(List.of(command), commands, "Enter on the cleared field must not execute again.");
        });
    }

    @Test
    public void enter_parseFailure_repeatsAndRecoversAfterEdit() throws Exception {
        assertFailureAndEditing(command -> {
            throw new ParseException("Invalid command format");
        });
    }

    @Test
    public void enter_commandFailure_repeatsAndRecoversAfterEdit() throws Exception {
        assertFailureAndEditing(command -> {
            throw new CommandException("Unable to save changes");
        });
    }

    @Test
    public void enter_successfulRetry_clearsRetainedInputAndErrorStyle() throws Exception {
        runOnFxThread(() -> {
            List<String> commands = new ArrayList<>();
            TextField input = createInput(command -> {
                commands.add(command);
                if (commands.size() == 1) {
                    throw new CommandException("Unable to save changes");
                }
                return new CommandResult("Success");
            });
            List<String> originalStyles = List.copyOf(input.getStyleClass());
            String command = "add r/parent n/Pat Tan p/91234567";
            input.setText(command);
            enter(input);
            assertFailure(input, command, originalStyles);

            // Retry the same draft without a text change that could clear its error style.
            enter(input);

            assertEquals(List.of(command, command), commands);
            assertEquals("", input.getText());
            assertEquals(originalStyles, input.getStyleClass());
            enter(input);
            assertEquals(List.of(command, command), commands);
        });
    }

    private static void assertFailureAndEditing(CommandBox.CommandExecutor executor) throws Exception {
        runOnFxThread(() -> {
            List<String> commands = new ArrayList<>();
            String command = "  invalid command  ";
            TextField input = createInput(text -> {
                commands.add(text);
                if (text.equals(command)) {
                    return executor.execute(text);
                }
                return new CommandResult("Corrected command succeeded");
            });
            List<String> originalStyles = List.copyOf(input.getStyleClass());
            input.setText(command);

            for (int attempt = 1; attempt <= 3; attempt++) {
                enter(input);
                assertEquals(Collections.nCopies(attempt, command), commands,
                        "Every Enter must forward the unchanged failed draft exactly once.");
                assertFailure(input, command, originalStyles);
            }

            input.appendText(" corrected");
            String correctedCommand = command + " corrected";
            assertEquals(correctedCommand, input.getText());
            assertEquals(originalStyles, input.getStyleClass(), "Editing must clear only the error style.");
            assertEquals(Collections.nCopies(3, command), commands, "Editing must not execute a command.");

            enter(input);
            assertEquals(List.of(command, command, command, correctedCommand), commands);
            assertEquals("", input.getText(), "A successful corrected command must clear the draft.");
            assertEquals(originalStyles, input.getStyleClass());

            input.setText(command);
            enter(input);
            assertFailure(input, command, originalStyles);
            List<String> commandsBeforeClearing = List.copyOf(commands);
            input.clear();
            enter(input);
            assertEquals(originalStyles, input.getStyleClass());
            assertEquals(commandsBeforeClearing, commands, "Clearing and empty Enter must not execute.");
        });
    }

    private static TextField createInput(CommandBox.CommandExecutor executor) {
        CommandBox box = new CommandBox(executor);
        TextField input = (TextField) box.getRoot().lookup("#commandTextField");
        assertNotNull(input.getOnAction(), "The FXML must bind the Enter action.");
        input.getStyleClass().add("custom-command-style");
        return input;
    }

    private static void enter(TextField input) {
        Event.fireEvent(input, new ActionEvent());
    }

    private static void assertFailure(TextField input, String command, List<String> originalStyles) {
        assertEquals(command, input.getText(), "A failed command must remain available for correction or retry.");
        assertEquals(1, Collections.frequency(input.getStyleClass(), CommandBox.ERROR_STYLE_CLASS),
                "Repeated failure must retain exactly one error style.");
        List<String> remainingStyles = new ArrayList<>(input.getStyleClass());
        remainingStyles.remove(CommandBox.ERROR_STYLE_CLASS);
        assertEquals(originalStyles, remainingStyles, "Failure must preserve unrelated styles.");
    }
}
