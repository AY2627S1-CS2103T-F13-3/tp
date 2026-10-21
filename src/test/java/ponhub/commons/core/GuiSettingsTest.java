package ponhub.commons.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Point;

import org.junit.jupiter.api.Test;

public class GuiSettingsTest {
    @Test
    public void getWindowCoordinates_mutatedCopy_keepsSettingsAndHashCodeUnchanged() {
        GuiSettings settings = new GuiSettings(950, 700, -1000, -200);
        GuiSettings expected = new GuiSettings(950, 700, -1000, -200);
        int originalHashCode = settings.hashCode();
        Point coordinates = settings.getWindowCoordinates();
        coordinates.translate(100, 100);

        assertEquals(expected, settings);
        assertEquals(originalHashCode, settings.hashCode());
        assertEquals(new Point(-1000, -200), settings.getWindowCoordinates());
    }

    @Test
    public void toStringMethod() {
        GuiSettings guiSettings = new GuiSettings();
        String expected = GuiSettings.class.getCanonicalName() + "{windowWidth=" + guiSettings.getWindowWidth()
                + ", windowHeight=" + guiSettings.getWindowHeight() + ", windowCoordinates="
                + guiSettings.getWindowCoordinates() + "}";
        assertEquals(expected, guiSettings.toString());
    }
}
