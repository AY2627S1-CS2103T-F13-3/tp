package ponhub.model.person;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class NameTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Name(null));
    }

    @Test
    public void constructor_invalidName_throwsIllegalArgumentException() {
        String[] invalidNames = {"", " ", "' - .", "12345", "Peter the 2nd", "Peter*", "Peter/Jack",
            "Peter\tJack", "Peter\nJack", "\rPeter", "Peter\0", "Peter\u007f", "Peter\u00a0Jack",
            "\u0301Peter", "Peter \u0301Jack", "Peter-\u0301Jack", "Peter😀", "A".repeat(101),
            "\u2028Peter", "Peter\u2029", "Peter\u200b", "\u202ePeter", "Peter\ud800", "\udc00Peter"};
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
            "A".repeat(100), "  " + "A  ".repeat(49) + "AA  ", "José Santos", "Jose\u0301 Santos",
            "李明", "தமிழ்", "Anne-Marie O’Neill", "\u00a0José\u2003", "\uD801\uDC00".repeat(100)};
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
    public void constructor_canonicalUnicodeAndBoundarySpaces_normalizesDisplayAndEquality() {
        Name composed = new Name("José Santos");
        Name decomposed = new Name("\u00a0Jose\u0301   Santos\u2003");
        assertEquals("José Santos", decomposed.fullName);
        assertEquals(composed, decomposed);
        assertEquals(composed.hashCode(), decomposed.hashCode());
        assertFalse(composed.equals(new Name("Jose Santos")));
    }

    @Test
    public void constructor_enclosingMark_requiresAttachedLetter() {
        String attachedMark = "A\u20dd";
        assertTrue(Name.isValidName(attachedMark));
        assertEquals(attachedMark, new Name(attachedMark).fullName);

        for (String unattachedMark : new String[] {"\u20ddA", "A \u20dd", "A-\u20dd"}) {
            assertFalse(Name.isValidName(unattachedMark));
            assertThrows(IllegalArgumentException.class, Name.MESSAGE_CONSTRAINTS, () -> new Name(unattachedMark));
        }
    }

    @Test
    public void constructor_normalizedCodePointLimit_countsCompositionAndSupplementaryLetters() {
        assertEquals("é".repeat(100), new Name("e\u0301".repeat(100)).fullName);
        assertFalse(Name.isValidName("\uD801\uDC00".repeat(101)));
        assertFalse(Name.isValidName("A" + "\u0301".repeat(101)));
        assertEquals("A B", new Name(" ".repeat(100_000) + "A" + " ".repeat(100_000) + "B").fullName);
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
