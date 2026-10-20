package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.FxTestUtil.runOnFxThread;

import org.junit.jupiter.api.Test;

import javafx.css.PseudoClass;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class ThemeMenuContrastTest {

    @Test
    public void popup_disabledAndFocusedItems_keepReadableText() throws Exception {
        runOnFxThread(() -> {
            TextField owner = new TextField();
            Scene scene = new Scene(new VBox(owner), 400, 200);
            scene.getStylesheets().add(MainWindow.class.getResource("/view/DarkTheme.css").toExternalForm());
            Stage stage = new Stage();
            stage.setScene(scene);
            MenuItem disabled = new MenuItem("Unavailable action");
            disabled.setDisable(true);
            ContextMenu popup = new ContextMenu(disabled, new MenuItem("Available action"));
            try {
                stage.show();
                popup.show(owner, Side.BOTTOM, 0, 0);
                Region root = (Region) popup.getScene().getRoot();
                root.applyCss();
                root.layout();
                Node unavailable = findItem(root, "Unavailable action");
                Node available = findItem(root, "Available action");
                assertReadable(unavailable);
                unavailable.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true);
                root.applyCss();
                assertReadable(unavailable);
                available.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true);
                root.applyCss();
                assertReadable(available);
            } finally {
                popup.hide();
                stage.close();
            }
        });
    }

    private static Node findItem(Region root, String text) {
        return root.lookupAll(".menu-item").stream()
                .filter(node -> node.lookup(".label") instanceof Label
                        && ((Label) node.lookup(".label")).getText().equals(text))
                .findFirst().orElseThrow();
    }

    private static void assertReadable(Node item) {
        Label label = (Label) item.lookup(".label");
        Color background = backgroundBehind(label);
        double opacity = 1;
        for (Node node = label; node != null; node = node.getParent()) {
            opacity *= node.getOpacity();
        }
        Color text = background.interpolate((Color) label.getTextFill(), opacity);
        double light = Math.max(luminance(text), luminance(background));
        double dark = Math.min(luminance(text), luminance(background));
        double ratio = (light + 0.05) / (dark + 0.05);
        assertTrue(ratio >= 4.5, () -> label.getText() + " has contrast " + ratio
                + " after inherited opacity; expected at least 4.5:1");
    }

    private static Color backgroundBehind(Node start) {
        for (Node node = start; node != null; node = node.getParent()) {
            if (node instanceof Region && ((Region) node).getBackground() != null) {
                var fills = ((Region) node).getBackground().getFills();
                for (int i = fills.size() - 1; i >= 0; i--) {
                    if (fills.get(i).getFill() instanceof Color) {
                        Color color = (Color) fills.get(i).getFill();
                        if (color.getOpacity() == 1) {
                            return color;
                        }
                    }
                }
            }
        }
        throw new AssertionError("Popup has no opaque background");
    }

    private static double luminance(Color color) {
        return 0.2126 * linear(color.getRed()) + 0.7152 * linear(color.getGreen())
                + 0.0722 * linear(color.getBlue());
    }

    private static double linear(double value) {
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }
}
