package ponhub.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.FxTestUtil.callOnFxThread;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Exercises footer painting at the fractional widths produced by display scaling.
 */
public class StatusBarFooterTest {

    @Test
    public void longSaveLocation_fractionalWidths_keepsPaintWithinAllocatedWidth() throws Exception {
        callOnFxThread(() -> {
            Region footer = new StatusBarFooter(Path.of("data", "long-directory-".repeat(40), "ponhub.json"))
                    .getRoot();
            footer.setManaged(false);
            Scene scene = new Scene(new StackPane(footer), 800, 80);
            scene.getStylesheets().add(getClass().getResource("/view/DarkTheme.css").toExternalForm());
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.show();
            try {
                footer.applyCss();
                double onePixel = 1 / stage.getOutputScaleX();
                for (double width : new double[] {200.1, 435.3333435058594, 436.6666564941406, 700.2}) {
                    footer.resize(width, 30);
                    footer.layout();
                    Bounds painted = footer.getBoundsInLocal();
                    assertTrue(painted.getMinX() >= -onePixel
                                    && painted.getMaxX() <= width + onePixel + 0.0001,
                            "Footer paint must stay within one device pixel of its allocated width: "
                                    + width + "; painted=" + painted + "; scale=" + stage.getOutputScaleX());
                }
            } finally {
                stage.hide();
            }
            return null;
        });
    }
}
