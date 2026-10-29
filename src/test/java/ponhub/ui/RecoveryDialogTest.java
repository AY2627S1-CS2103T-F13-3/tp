package ponhub.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.FxTestUtil.runOnFxThread;

import org.junit.jupiter.api.Test;

import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Screen;

public class RecoveryDialogTest {
    @Test
    public void shownWarning_longAndOrdinaryDetailsStayCompleteWithinWorkArea() throws Exception {
        for (String detailsText : new String[]{"Could not load /test/data.json. Back up the original, then restart.",
            "Could not load /test/data.json. Unknown field: " + "X".repeat(5_000) + " END OF DIAGNOSTIC"}) {
            runOnFxThread(() -> {
                Alert alert = new UiManager(null, null).createAlert(null, AlertType.WARNING,
                        "Data file protected", "Your saved data could not be loaded", detailsText);
                try {
                    alert.show();
                    alert.getDialogPane().applyCss();
                    alert.getDialogPane().layout();
                    TextArea details = (TextArea) alert.getDialogPane().lookup("#alertDetails");
                    assertEquals(detailsText, details.getText());
                    assertFalse(details.isEditable());
                    assertTrue(details.isFocusTraversable());
                    assertTrue(alert.isResizable());
                    var workArea = Screen.getPrimary().getVisualBounds();
                    var bounds = alert.getDialogPane().localToScreen(alert.getDialogPane().getBoundsInLocal());
                    assertTrue(bounds.getHeight() < workArea.getHeight());
                    assertTrue(bounds.getWidth() < workArea.getWidth());
                    Button ok = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
                    assertTrue(ok.isVisible());
                    var buttonBounds = ok.localToScreen(ok.getBoundsInLocal());
                    assertTrue(workArea.contains(buttonBounds.getMinX(), buttonBounds.getMinY(),
                            buttonBounds.getWidth(), buttonBounds.getHeight()));
                    details.selectAll();
                    assertEquals(detailsText, details.getSelectedText());
                    details.positionCaret(details.getLength());
                    if (detailsText.length() > 5_000) {
                        assertTrue(details.lookupAll(".scroll-bar").stream()
                                .filter(ScrollBar.class::isInstance).map(ScrollBar.class::cast)
                                .anyMatch(bar -> bar.getOrientation() == Orientation.VERTICAL && bar.isVisible()));
                    }
                    Event.fireEvent(details, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE,
                            false, false, false, false));
                    assertFalse(alert.isShowing(), "Escape must dismiss even with details focused.");
                } finally {
                    alert.close();
                }
            });
        }
    }
}
