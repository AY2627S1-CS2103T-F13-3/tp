package ponhub.commons.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.awt.Point;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public void equals_dimensionsAndCoordinates_obeyValueEquality() {
        GuiSettings settings = new GuiSettings(950, 700, 10, 20);
        GuiSettings equal = new GuiSettings(950, 700, 10, 20);
        assertEquals(settings, equal);
        assertEquals(equal, settings);
        assertEquals(settings.hashCode(), equal.hashCode());
        assertEquals(1, new HashSet<>(List.of(settings, equal)).size());
        assertNotEquals(settings, new GuiSettings(951, 700, 10, 20));
        assertNotEquals(settings, new GuiSettings(950, 701, 10, 20));
        assertNotEquals(settings, new GuiSettings(950, 700, 11, 20));
        assertNotEquals(settings, null);
        assertNotEquals(settings, new Object());
        assertEquals(new GuiSettings(), new GuiSettings());
    }

    @Test
    public void equals_signedZeros_areDistinctInBothDimensionsAndCollections() {
        GuiSettings positive = new GuiSettings(0.0, 0.0, 10, 20);
        GuiSettings negativeWidth = new GuiSettings(-0.0, 0.0, 10, 20);
        GuiSettings negativeHeight = new GuiSettings(0.0, -0.0, 10, 20);
        assertNotEquals(positive, negativeWidth);
        assertNotEquals(negativeWidth, positive);
        assertNotEquals(positive, negativeHeight);
        assertEquals(3, new HashSet<>(List.of(positive, negativeWidth, negativeHeight)).size());
    }

    @Test
    public void equals_nanRepresentations_areEqualWithMatchingHashes() {
        GuiSettings first = new GuiSettings(Double.NaN, Double.NaN, 10, 20);
        double otherNan = Double.longBitsToDouble(0x7ff8000000000001L);
        GuiSettings second = new GuiSettings(otherNan, Double.NaN, 10, 20);
        GuiSettings third = new GuiSettings(Double.NaN, otherNan, 10, 20);
        assertEquals(first, second);
        assertEquals(second, third);
        assertEquals(first, third);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(second.hashCode(), third.hashCode());
        assertEquals(Set.of(first), new HashSet<>(List.of(first, second, third)));
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
