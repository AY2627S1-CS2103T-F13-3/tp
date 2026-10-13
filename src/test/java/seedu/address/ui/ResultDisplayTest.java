package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.FxTestUtil.callOnFxThread;
import static seedu.address.testutil.FxTestUtil.runOnFxThread;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import seedu.address.logic.CanonicalCommandCatalog;
import seedu.address.logic.CanonicalLogicManager;
import seedu.address.logic.CanonicalPeopleSession;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.PersonRecord;

public class ResultDisplayTest {

    private static final String LONG_FEEDBACK = "Original feedback with complete Unicode text: Zoë Tan.\n".repeat(100)
            + "Original final marker.";

    @TempDir
    public Path directory;

    @Test
    public void setFeedbackToUser_shortAndEmptyFeedback_preservesCompleteReadOnlyContent() throws Exception {
        runOnFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            TextArea area = (TextArea) display.getRoot().lookup("#resultDisplay");
            String feedback = "First line: Zoë Tan\nSecond line: 中文\nFinal marker.";

            display.setFeedbackToUser(feedback);

            assertEquals(feedback, area.getText());
            assertFalse(area.isEditable());
            assertTrue(area.isWrapText());
            display.setFeedbackToUser("");
            assertEquals("", area.getText());
            assertEquals(0, area.getCaretPosition());
        });
    }

    @Test
    public void setFeedbackToUser_replacesLongScrolledFeedback_resetsCaretAndViewport() throws Exception {
        assertScrolledFeedbackReset("Replacement feedback is also longer than the viewport.\n".repeat(80)
                + "Replacement final marker.");
    }

    @Test
    public void setFeedbackToUser_repeatsIdenticalScrolledFeedback_resetsCaretAndViewport() throws Exception {
        assertScrolledFeedbackReset(LONG_FEEDBACK);
    }

    @Test
    public void help_f1FromResultArea_preservesDraftSelectionAndOperationalBytes() throws Exception {
        callOnFxThread(() -> {
            Path dataPath = directory.resolve("ponhub.json");
            CanonicalPeopleSession session = new CanonicalPeopleSession(dataPath);
            session.execute("add r/student n/Alex Tan l/P1 pp/91234567");
            session.execute("add r/student n/Jamie Tan l/P2 pp/91234567");
            session.execute("add r/tutor n/Mei Lim p/92345678");
            session.execute("list r/student");
            byte[] originalBytes = Files.readAllBytes(dataPath);
            CanonicalLogicManager logic = new CanonicalLogicManager(session, new UserPrefs());
            Stage stage = new Stage();
            try {
                MainWindow window = new MainWindow(stage, logic, dataPath);
                window.fillInnerParts();
                Region root = (Region) stage.getScene().getRoot();
                // SplitPane content enters the scene graph when its skin is initialized.
                root.applyCss();
                root.resize(853, 600);
                root.layout();
                TextField input = (TextField) root.lookup("#commandTextField");
                TextArea area = (TextArea) root.lookup("#resultDisplay");
                // Select a real people card while help is requested from the other text control.
                @SuppressWarnings("unchecked")
                ListView<PersonRecord> list = (ListView<PersonRecord>) root.lookup("#personListView");
                list.getSelectionModel().select(1);
                PersonRecord selected = list.getSelectionModel().getSelectedItem();
                String draft = "add r/student n/Unfinished";
                input.setText(draft);
                area.setText("Previous command feedback");

                Event.fireEvent(area, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F1,
                        false, false, false, false));

                assertEquals(CanonicalCommandCatalog.getOverview(Optional.empty()), area.getText());
                assertEquals(draft, input.getText());
                assertSame(selected, list.getSelectionModel().getSelectedItem());
                assertEquals(1, list.getSelectionModel().getSelectedIndex());
                assertEquals(2, list.getItems().size());
                assertArrayEquals(originalBytes, Files.readAllBytes(dataPath));
            } finally {
                stage.hide();
            }
            return null;
        });
    }

    private static void assertScrolledFeedbackReset(String nextFeedback) throws Exception {
        FeedbackFixture fixture = callOnFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            Region root = display.getRoot();
            new Scene(root, 300, 120);
            TextArea area = (TextArea) root.lookup("#resultDisplay");
            display.setFeedbackToUser(LONG_FEEDBACK);
            FeedbackFixture result = new FeedbackFixture(display, root, area);
            result.layout();
            return result;
        });
        runOnFxThread(fixture::layout);
        runOnFxThread(() -> {
            fixture.layout();
            ScrollBar bar = fixture.verticalScrollBar();
            assertTrue(bar.isVisible(), "The fixture must have overflowing feedback.");
            fixture.area.positionCaret(LONG_FEEDBACK.length());
            bar.setValue(bar.getMax());
        });
        runOnFxThread(() -> {
            fixture.layout();
            assertTrue(fixture.area.getScrollTop() > 0, "The old feedback must actually be scrolled.");
            assertEquals(LONG_FEEDBACK.length(), fixture.area.getCaretPosition());

            fixture.display.setFeedbackToUser(nextFeedback);

            assertEquals(nextFeedback, fixture.area.getText());
            assertEquals(0, fixture.area.getCaretPosition());
            assertEquals(0, fixture.area.getScrollTop(), 0.01);
        });
        runOnFxThread(fixture::layout);
        runOnFxThread(() -> {
            fixture.layout();
            assertEquals(nextFeedback, fixture.area.getText(), "Layout must retain the complete new feedback.");
            assertEquals(0, fixture.area.getCaretPosition());
            assertEquals(0, fixture.area.getScrollTop(), 0.01,
                    "Deferred layout must keep the new feedback at the top.");
            ScrollBar bar = fixture.verticalScrollBar();
            assertTrue(bar.isVisible(), "The new feedback must still overflow the viewport.");
            assertEquals(bar.getMin(), bar.getValue(), 0.01);
        });
    }

    private record FeedbackFixture(ResultDisplay display, Region root, TextArea area) {
        private void layout() {
            root.applyCss();
            root.layout();
            area.layout();
        }

        private ScrollBar verticalScrollBar() {
            return area.lookupAll(".scroll-bar").stream()
                    .filter(ScrollBar.class::isInstance)
                    .map(ScrollBar.class::cast)
                    .filter(bar -> bar.getOrientation() == Orientation.VERTICAL)
                    .findFirst().orElseThrow();
        }
    }
}
