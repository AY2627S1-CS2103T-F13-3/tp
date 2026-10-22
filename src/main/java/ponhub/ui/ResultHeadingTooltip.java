package ponhub.ui;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

/**
 * Keeps shortened result headings inspectable without taking space from the records.
 * Developed by Ernest Chua with OpenAI Codex assistance for #369.
 */
final class ResultHeadingTooltip {
    private static final double MAX_WIDTH = 320;

    private ResultHeadingTooltip() {
    }

    static void install(Label... headings) {
        for (Label heading : headings) {
            Tooltip tooltip = new Tooltip();
            tooltip.textProperty().bind(heading.textProperty());
            tooltip.setWrapText(true);
            tooltip.setMaxWidth(MAX_WIDTH);
            tooltip.setShowDuration(Duration.INDEFINITE);
            heading.setTooltip(tooltip);
        }
    }
}
