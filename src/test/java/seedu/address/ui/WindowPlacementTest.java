package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.geometry.Rectangle2D;
import seedu.address.commons.core.GuiSettings;

public class WindowPlacementTest {

    private static final Rectangle2D PRIMARY = new Rectangle2D(0, 30, 1920, 1010);
    private static final Rectangle2D LEFT = new Rectangle2D(-1536, 0, 1536, 864);
    private static final Rectangle2D ABOVE = new Rectangle2D(0, -864, 1536, 864);

    @Test
    public void fit_validSavedWindow_preservesSizeAndPosition() {
        Rectangle2D actual = fit(new GuiSettings(740, 600, 100, 120), List.of(PRIMARY));

        assertEquals(new Rectangle2D(100, 120, 740, 600), actual);
    }

    @Test
    public void fit_freshWindow_centersWithinWorkArea() {
        Rectangle2D actual = fit(new GuiSettings(), List.of(PRIMARY));

        assertEquals(new Rectangle2D(590, 235, 740, 600), actual);
        assertTrue(PRIMARY.contains(actual));
    }

    @Test
    public void fit_smallScaledWorkArea_keepsWholeWindowWithinDesktopBars() {
        Rectangle2D compact = new Rectangle2D(0, 30, 853, 410);
        Rectangle2D actual = WindowPlacement.fit(new GuiSettings(), List.of(compact), compact);

        assertEquals(new Rectangle2D(56.5, 30, 740, 410), actual);
        assertTrue(compact.contains(actual));
    }

    @Test
    public void fit_oversizedPreferences_fitAvailableWorkArea() {
        Rectangle2D actual = fit(new GuiSettings(3000, 2000, 100, 120), List.of(PRIMARY));

        assertEquals(PRIMARY, actual);
    }

    @Test
    public void fit_removedScreen_recentersOnPrimary() {
        Rectangle2D actual = fit(new GuiSettings(740, 600, -1400, 100), List.of(PRIMARY));

        assertEquals(new Rectangle2D(590, 235, 740, 600), actual);
    }

    @Test
    public void fit_secondaryScreens_preservesValidNegativeCoordinates() {
        List<Rectangle2D> screens = List.of(PRIMARY, LEFT, ABOVE);

        assertEquals(new Rectangle2D(-1400, 100, 740, 600),
                fit(new GuiSettings(740, 600, -1400, 100), screens));
        assertEquals(new Rectangle2D(100, -800, 740, 600),
                fit(new GuiSettings(740, 600, 100, -800), screens));
    }

    @Test
    public void fit_windowAcrossScreens_restoresToGreatestOverlap() {
        Rectangle2D right = new Rectangle2D(1920, 30, 1536, 834);
        Rectangle2D actual = fit(new GuiSettings(740, 600, 1800, 120), List.of(PRIMARY, right));

        assertEquals(new Rectangle2D(1920, 120, 740, 600), actual);
    }

    @Test
    public void fit_partiallyVisibleWindow_clampsPositionWithinSelectedScreen() {
        Rectangle2D actual = fit(new GuiSettings(740, 600, 1800, 900), List.of(PRIMARY));

        assertEquals(new Rectangle2D(1180, 440, 740, 600), actual);
        assertTrue(PRIMARY.contains(actual));
    }

    @Test
    public void fit_invalidDimensions_usesDefaultsAndMinimumUsableSize() {
        assertEquals(new Rectangle2D(100, 120, 740, 600),
                fit(new GuiSettings(Double.NaN, Double.POSITIVE_INFINITY, 100, 120), List.of(PRIMARY)));
        assertEquals(new Rectangle2D(100, 120, 740, 600),
                fit(new GuiSettings(0, -1, 100, 120), List.of(PRIMARY)));
        assertEquals(new Rectangle2D(100, 120, 450, 360),
                fit(new GuiSettings(1, 1, 100, 120), List.of(PRIMARY)));
    }

    private static Rectangle2D fit(GuiSettings settings, List<Rectangle2D> screens) {
        return WindowPlacement.fit(settings, screens, PRIMARY);
    }
}
