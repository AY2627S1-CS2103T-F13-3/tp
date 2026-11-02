package ponhub.model.lesson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SubjectTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Subject(null));
    }

    @Test
    public void constructor_invalidSubject_throwsIllegalArgumentException() {
        String[] invalidSubjects = {"", "   ", "-Math", "Math-", "A--Math", "A - Math", "Math!",
            "Math/Science", "Math\tScience", "A".repeat(51)};

        for (String invalidSubject : invalidSubjects) {
            assertThrows(IllegalArgumentException.class, () -> new Subject(invalidSubject));
        }
    }

    @Test
    public void constructor_repeatedSpaces_normalizesSpaces() {
        assertEquals("Primary 5 Math", new Subject("  Primary   5  Math  ").toString());
    }

    @Test
    public void constructor_unicodeLettersAndAttachedMarks_normalizesToNfc() {
        for (String subject : new String[] {"தமிழ்", "हिंदी", "q\u0301", "\uD840\uDC00\u0301"}) {
            assertTrue(Subject.isValidSubject(subject));
            assertEquals(subject, new Subject(subject).getValue());
        }
        Subject composed = new Subject("Français");
        Subject decomposed = new Subject("  Franc\u0327ais  ");
        assertTrue(Subject.isValidSubject("Franc\u0327ais"));
        assertEquals("Français", decomposed.getValue());
        assertEquals(composed, decomposed);
        assertEquals(composed.hashCode(), decomposed.hashCode());
    }

    @Test
    public void constructor_normalizedLength_countsSeparatorsAndCombiningMarks() {
        for (String subject : new String[] {"A ".repeat(24) + "AB", "A-".repeat(24) + "AB",
            "q\u0301".repeat(25), "\uD840\uDC00".repeat(50), "e\u0301".repeat(50),
            "\u03b1\u0313\u0342\u0345".repeat(50)}) {
            assertTrue(Subject.isValidSubject(subject));
            String value = new Subject(subject).getValue();
            assertEquals(50, value.codePointCount(0, value.length()));
            assertFalse(Subject.isValidSubject(subject + "B"));
            assertThrows(IllegalArgumentException.class, () -> new Subject(subject + "B"));
        }
        assertFalse(Subject.isValidSubject("A ".repeat(25) + "A"));
    }

    @Test
    public void fromStoredValue_nfcExpandedOlderSubject_acceptedOnlyFromStorage() {
        // New input keeps the 50-code-point limit after NFC; saved values count a letter with its marks once.
        String olderSubject = "\u0958".repeat(26);
        assertFalse(Subject.isValidSubject(olderSubject));
        assertThrows(IllegalArgumentException.class, () -> new Subject(olderSubject));
        Subject stored = Subject.fromStoredValue(olderSubject);
        assertEquals("\u0915\u093c".repeat(26), stored.getValue());
        assertEquals(stored, Subject.fromStoredValue(stored.getValue()));
        assertEquals("\u0915\u093c".repeat(50), Subject.fromStoredValue("\u0958".repeat(50)).getValue());
        assertEquals(new Subject("Math"), Subject.fromStoredValue(" Math "));
        for (String invalid : new String[] {"\u0958".repeat(51), "A".repeat(51), "", "\u0301Math", "Math/Science",
            "\u0958\u0301\u0301\u0301".repeat(50)}) {
            assertThrows(IllegalArgumentException.class, () -> Subject.fromStoredValue(invalid), invalid);
        }
    }

    @Test
    public void constructor_excessiveDecomposedInput_rejectsBeforeCanonicalReordering() {
        String subject = "a" + "\u0315".repeat(20_000) + "\u0300".repeat(20_000);
        assertFalse(Subject.isValidSubject(subject));
        assertThrows(IllegalArgumentException.class, () -> new Subject(subject));
    }

    @Test
    public void constructor_unattachedMarksAndUnsupportedCharacters_rejects() {
        for (String subject : new String[] {"\u0301", "\u0301Math", "Math \u0301", "Math-\u0301",
            "1\u0301", "A1\u0301", "Math\u00a0Science", "Math\u200dScience", "Math\u202eScience",
            "Math\u0000Science", "Math\uD800", "Math\uDC00"}) {
            assertFalse(Subject.isValidSubject(subject));
            assertThrows(IllegalArgumentException.class, () -> new Subject(subject));
        }
    }

    @Test
    public void isValidSubject() {
        String supplementaryLetter = new String(Character.toChars(0x20000));

        assertThrows(NullPointerException.class, () -> Subject.isValidSubject(null));

        assertFalse(Subject.isValidSubject(""));
        assertFalse(Subject.isValidSubject("-Math"));
        assertFalse(Subject.isValidSubject("Math-"));
        assertFalse(Subject.isValidSubject("A--Math"));
        assertFalse(Subject.isValidSubject("Math!"));
        assertFalse(Subject.isValidSubject("A".repeat(51)));
        assertFalse(Subject.isValidSubject(supplementaryLetter.repeat(51)));

        assertTrue(Subject.isValidSubject("Math"));
        assertTrue(Subject.isValidSubject("A-Math"));
        assertTrue(Subject.isValidSubject("H2-Math 1"));
        assertTrue(Subject.isValidSubject("Primary 5 Math"));
        assertTrue(Subject.isValidSubject("A".repeat(50)));
        assertTrue(Subject.isValidSubject(supplementaryLetter.repeat(50)));
    }

    @Test
    public void equals_usesNormalizedValue() {
        Subject subject = new Subject("Primary 5 Math");

        assertEquals(subject, new Subject("  Primary   5 Math "));
        assertNotEquals(subject, new Subject("Science"));
        assertNotEquals(subject, null);
        assertNotEquals(subject, "Primary 5 Math");
    }
}
