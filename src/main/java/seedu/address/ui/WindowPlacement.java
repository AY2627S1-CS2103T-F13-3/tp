package seedu.address.ui;

import java.awt.Point;
import java.util.List;

import javafx.geometry.Rectangle2D;
import seedu.address.commons.core.GuiSettings;

/**
 * Fits saved window preferences to the available logical desktop work areas.
 */
final class WindowPlacement {

    static final double MINIMUM_WIDTH = 450;
    static final double MINIMUM_HEIGHT = 360;

    private WindowPlacement() {
    }

    /**
     * Restores the window on its most-overlapped screen, or centers it on the primary screen.
     * Work areas already account for display scaling and desktop bars.
     */
    static Rectangle2D fit(GuiSettings settings, List<Rectangle2D> workAreas, Rectangle2D primaryWorkArea) {
        GuiSettings defaults = new GuiSettings();
        double requestedWidth = validDimension(settings.getWindowWidth(), defaults.getWindowWidth());
        double requestedHeight = validDimension(settings.getWindowHeight(), defaults.getWindowHeight());
        Point coordinates = settings.getWindowCoordinates();
        Rectangle2D workArea = primaryWorkArea;
        double largestOverlap = 0;

        if (coordinates != null) {
            Rectangle2D savedBounds = new Rectangle2D(coordinates.getX(), coordinates.getY(),
                    requestedWidth, requestedHeight);
            for (Rectangle2D candidate : workAreas) {
                double overlap = intersectionArea(savedBounds, candidate);
                if (overlap > largestOverlap) {
                    workArea = candidate;
                    largestOverlap = overlap;
                }
            }
        }

        double width = Math.min(workArea.getWidth(), Math.max(MINIMUM_WIDTH, requestedWidth));
        double height = Math.min(workArea.getHeight(), Math.max(MINIMUM_HEIGHT, requestedHeight));
        double x = workArea.getMinX() + (workArea.getWidth() - width) / 2;
        double y = workArea.getMinY() + (workArea.getHeight() - height) / 2;
        if (coordinates != null && largestOverlap > 0) {
            x = Math.clamp(coordinates.getX(), workArea.getMinX(), workArea.getMaxX() - width);
            y = Math.clamp(coordinates.getY(), workArea.getMinY(), workArea.getMaxY() - height);
        }
        return new Rectangle2D(x, y, width, height);
    }

    /**
     * Replaces invalid saved dimensions with the default dimension.
     */
    private static double validDimension(double dimension, double fallback) {
        return Double.isFinite(dimension) && dimension > 0 ? dimension : fallback;
    }

    /**
     * Returns the shared area of two rectangles, or zero when they do not overlap.
     */
    private static double intersectionArea(Rectangle2D first, Rectangle2D second) {
        double width = Math.max(0, Math.min(first.getMaxX(), second.getMaxX())
                - Math.max(first.getMinX(), second.getMinX()));
        double height = Math.max(0, Math.min(first.getMaxY(), second.getMaxY())
                - Math.max(first.getMinY(), second.getMinY()));
        return width * height;
    }
}
