package seedu.address.model.lesson;

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
        String[] invalidSubjects = {"", "   ", "Math!", "Math/Science", "Math\tScience",
            "A".repeat(51)};

        for (String invalidSubject : invalidSubjects) {
            assertThrows(IllegalArgumentException.class, () -> new Subject(invalidSubject));
        }
    }

    @Test
    public void constructor_repeatedSpaces_normalizesSpaces() {
        assertEquals("Primary 5 Math", new Subject("  Primary   5  Math  ").toString());
    }

    @Test
    public void isValidSubject() {
        assertThrows(NullPointerException.class, () -> Subject.isValidSubject(null));

        assertFalse(Subject.isValidSubject(""));
        assertFalse(Subject.isValidSubject("Math!"));
        assertFalse(Subject.isValidSubject("A".repeat(51)));

        assertTrue(Subject.isValidSubject("Math"));
        assertTrue(Subject.isValidSubject("Primary 5 Math"));
        assertTrue(Subject.isValidSubject("A".repeat(50)));
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
