package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class EducationLevelTest {
    @Test
    public void constructor_normalizedLevel_usesCanonicalValue() {
        EducationLevel level = new EducationLevel(" jc2 ");
        assertEquals(new EducationLevel("JC2"), level);
        assertEquals("JC2", level.getValue());
        assertEquals("JC2", level.toString());
    }

    @Test
    public void isValid_allSupportedLevels_acceptsOnlyConfiguredRanges() {
        String[] validLevels = {
            "P1", "P2", "P3", "P4", "P5", "P6", "S1", "S2", "S3", "S4", "S5", "JC1", "JC2",
            " p1 ", "s5", "\tjc1\n"
        };
        for (String validLevel : validLevels) {
            assertTrue(EducationLevel.isValid(validLevel), validLevel);
            assertEquals(new EducationLevel(validLevel),
                    new EducationLevel(validLevel.trim().toUpperCase(Locale.ROOT)));
        }
    }

    @Test
    public void constructor_invalidLevel_throwsUsefulException() {
        String[] invalidLevels = {
            "", " ", "P0", "P7", "S0", "S6", "JC0", "JC3", "P01", "S01", "JC01", "J1", "Primary1",
            "P 1", "JC 1", "P1 S1", "P-1", "P\u0661"
        };
        for (String invalidLevel : invalidLevels) {
            assertFalse(EducationLevel.isValid(invalidLevel), invalidLevel);
            IllegalArgumentException exception =
                    assertThrows(IllegalArgumentException.class, () -> new EducationLevel(invalidLevel));
            assertEquals(EducationLevel.MESSAGE_CONSTRAINTS, exception.getMessage());
        }

        assertFalse(EducationLevel.isValid(null));
        assertThrows(NullPointerException.class, () -> new EducationLevel(null));
    }

    @Test
    public void equalsAndHashCode_normalization_deduplicatesOnlySameLevel() {
        EducationLevel level = new EducationLevel("S2");
        EducationLevel normalizedLevel = new EducationLevel(" s2 ");
        EducationLevel otherLevel = new EducationLevel("S3");
        assertEquals(level, level);
        assertEquals(level, normalizedLevel);
        assertEquals(normalizedLevel, level);
        assertEquals(level.hashCode(), normalizedLevel.hashCode());
        assertNotEquals(level, otherLevel);
        assertNotEquals(level, null);
        assertNotEquals(level, "S2");

        Set<EducationLevel> levels = new HashSet<>(List.of(level, normalizedLevel, otherLevel));
        assertEquals(2, levels.size());
    }
}
