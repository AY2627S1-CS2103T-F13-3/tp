package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class NameTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Name(null));
    }

    @Test
    public void constructor_invalidName_throwsIllegalArgumentException() {
        String[] invalidNames = {"", " ", "' - .", "12345", "Peter the 2nd", "Peter*", "Peter/Jack",
            "Peter\tJack", "Peter\nJack", "\rPeter", "Peter\0", "Peter\u007f", "Jos\u00e9", "Peter\u00a0Jack",
            "\u674e", "A".repeat(101)};
        for (String invalidName : invalidNames) {
            assertFalse(Name.isValidName(invalidName));
            assertThrows(IllegalArgumentException.class, Name.MESSAGE_CONSTRAINTS, () -> new Name(invalidName));
        }
    }

    @Test
    public void isValidName() {
        // null name
        assertThrows(NullPointerException.class, () -> Name.isValidName(null));

        String[] validNames = {"A", "peter jack", "Capital Tan", "Anne-Marie O'Neill Jr.", "  Peter   Jack  ",
            "A".repeat(100), "  " + "A  ".repeat(49) + "AA  "};
        for (String validName : validNames) {
            assertTrue(Name.isValidName(validName));
            assertDoesNotThrow(() -> new Name(validName));
        }
    }

    @Test
    public void constructor_repeatedSpaces_normalizesName() {
        Name name = new Name("  Anne-Marie   O'Neill Jr.  ");
        assertEquals("Anne-Marie O'Neill Jr.", name.fullName);
        assertEquals("Anne-Marie O'Neill Jr.", name.toString());
    }

    @Test
    public void equals_normalizedNames_returnsTrue() {
        Name normalizedName = new Name("Anne-Marie O'Neill Jr.");
        Name unnormalizedName = new Name("  Anne-Marie   O'Neill Jr.  ");
        assertEquals(normalizedName, unnormalizedName);
        assertEquals(normalizedName.hashCode(), unnormalizedName.hashCode());
        assertFalse(normalizedName.equals(new Name("anne-marie o'neill jr.")));
    }

    @Test
    public void equals() {
        Name name = new Name("Valid Name");

        // same values -> returns true
        assertTrue(name.equals(new Name("Valid Name")));

        // same object -> returns true
        assertTrue(name.equals(name));

        // null -> returns false
        assertFalse(name.equals(null));

        // different types -> returns false
        assertFalse(name.equals(5.0f));

        // different values -> returns false
        assertFalse(name.equals(new Name("Other Valid Name")));
    }
}
