package ponhub.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.FxTestUtil.runOnFxThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import ponhub.logic.CanonicalCommandCatalog;
import ponhub.logic.commands.CommandResult;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.exceptions.ParseException;

public class CommandBoxTest {

    @Test
    public void tab_completesAndCyclesWithoutExecuting_enterSubmitsCompletedDraft() throws Exception {
        runOnFxThread(() -> {
            List<String> commands = new ArrayList<>();
            TextField input = createInput(command -> {
                commands.add(command);
                return new CommandResult("Success");
            });
            input.setText("  ad  r/student n/Alex  ");
            input.positionCaret(4);
            assertTab(input, "  add  r/student n/Alex  ", 5);
            assertTab(input, "  addlesson  r/student n/Alex  ", 11);
            assertTab(input, "  add  r/student n/Alex  ", 5);
            assertTrue(commands.isEmpty());
            enter(input);
            assertEquals(List.of("  add  r/student n/Alex  "), commands);
            assertEquals("", input.getText());
        });
    }

    @Test
    public void tab_editAndCaretMovement_resetOriginalPrefix() throws Exception {
        runOnFxThread(() -> {
            TextField input = createInput(command -> new CommandResult("Success"));
            input.setText("ad");
            input.end();
            assertTab(input, "add", 3);
            assertTab(input, "addlesson", 9);
            // Move away and back, ending at the previous completion: this must still reset cycling.
            input.positionCaret(8);
            input.end();
            assertTab(input, "addlesson", 9);
            input.setText("sho");
            input.end();
            assertTab(input, "showlesson", 10);
            input.setText("shoxxx lid/L1");
            input.positionCaret(3);
            assertTab(input, "showlesson lid/L1", 10);
            input.setText("ad");
            input.end();
            assertTab(input, "add", 3);
        });
    }

    @Test
    public void tab_ineligibleInputAndModifiers_leaveDraftAndEventForTraversal() throws Exception {
        runOnFxThread(() -> {
            TextField input = createInput(command -> {
                throw new AssertionError("Tab must never execute");
            });
            for (String draft : List.of("", "  ", "unknown", "SHO", "sho ", "help sho")) {
                input.setText(draft);
                input.end();
                assertFalse(tab(input, false, false, false, false));
                assertEquals(draft, input.getText());
            }
            input.setText("sho");
            input.end();
            assertFalse(tab(input, true, false, false, false));
            assertFalse(tab(input, false, true, false, false));
            assertFalse(tab(input, false, false, true, false));
            assertFalse(tab(input, false, false, false, true));
            assertEquals("sho", input.getText());
            input.selectAll();
            assertFalse(tab(input, false, false, false, false));
            assertEquals("sho", input.getSelectedText());
        });
    }

    @Test
    public void tab_failedDraft_recoversStyleOnReplacementAndRetainsUnmatchedError() throws Exception {
        runOnFxThread(() -> {
            TextField input = createInput(command -> {
                throw new ParseException("Invalid command");
            });
            input.setText("sho lid/L1");
            input.positionCaret(3);
            enter(input);
            assertTrue(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
            assertTab(input, "showlesson lid/L1", 10);
            assertFalse(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
            input.setText("unknown");
            input.end();
            enter(input);
            assertFalse(tab(input, false, false, false, false));
            assertEquals("unknown", input.getText());
            assertTrue(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
        });
    }

    private static void assertTab(TextField input, String expected, int caret) {
        assertTrue(tab(input, false, false, false, false), "A completion must consume Tab before focus traversal.");
        assertEquals(expected, input.getText());
        assertEquals(caret, input.getCaretPosition());
        assertEquals(caret, input.getAnchor());
    }

    private static boolean tab(TextField input, boolean shift, boolean control, boolean alt, boolean meta) {
        // Record consumption after CommandBox's filter but before the control's default traversal behavior.
        boolean[] consumed = {false};
        javafx.event.EventHandler<KeyEvent> observer = event -> consumed[0] = event.isConsumed();
        input.addEventFilter(KeyEvent.KEY_PRESSED, observer);
        Event.fireEvent(input, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB,
                shift, control, alt, meta));
        input.removeEventFilter(KeyEvent.KEY_PRESSED, observer);
        return consumed[0];
    }

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
        CommandBox box = new CommandBox(executor, CanonicalCommandCatalog.getCommandWords(false));
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
