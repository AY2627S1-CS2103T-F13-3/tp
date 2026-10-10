package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class EmailTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Email(null));
    }

    @Test
    public void constructor_invalidEmail_throwsIllegalArgumentException() {
        String[] invalidEmails = {"", " ", "peterjack", "@example.com", "peterjack@", "test@localhost",
            "123@145", "peter jack@example.com", "peter@example com", " peter@example.com", "peter@example.com ",
            "peter@@example.com", "peter@jack@example.com", ".peter@example.com", "peter.@example.com",
            "peter..jack@example.com", "peter!jack@example.com", "peter@example..com", "peter@.example.com",
            "peter@example.com.", "peter@-example.com", "peter@example-.com", "peter@exam_ple.com",
            "peter@example.c", "peter@example.c1", "peter@example.c-m", "peter@example." + "a".repeat(64),
            "peter\t@example.com", "peter@example.com\n", "peter\0@example.com", "jos\u00e9@example.com",
            "peter@ex\u00e4mple.com", "a".repeat(243) + "@example.com"};
        for (String invalidEmail : invalidEmails) {
            assertFalse(Email.isValidEmail(invalidEmail));
            assertThrows(IllegalArgumentException.class, Email.MESSAGE_CONSTRAINTS, () -> new Email(invalidEmail));
        }
    }

    @Test
    public void isValidEmail() {
        assertThrows(NullPointerException.class, () -> Email.isValidEmail(null));

        String[] validEmails = {"a@b.co", "PeterJack_1190@example.com", "PeterJack.1190@example.com",
            "PeterJack+1190@example.com", "PeterJack-1190@example.com", "Peter%Jack@example.com",
            "-peterjack-@example.com", "_+%--_%@example.com", "peter_jack@very--long-example.com",
            "123@145.co", "e1234567@u.nus.edu", "peter@example." + "A".repeat(63),
            "a".repeat(242) + "@example.com", "a@" + "b".repeat(249) + ".co"};
        for (String validEmail : validEmails) {
            assertTrue(Email.isValidEmail(validEmail));
            assertEquals(validEmail, new Email(validEmail).value);
        }
    }

    @Test
    public void constructor_mixedCase_preservesEmail() {
        Email email = new Email("Peter.Jack+School@Example.COM");
        assertEquals("Peter.Jack+School@Example.COM", email.value);
        assertEquals("Peter.Jack+School@Example.COM", email.toString());
    }

    @Test
    public void isValidEmail_shortInputs_matchesFormatRegex() {
        assertFormatValidationMatches("a0%+_.-", 5, "", "@ab.co");
        assertFormatValidationMatches("a0.-", 7, "a@", ".co");
        for (String email : new String[]{"A0_b+C-d.E@A0-bC.DE", "a@a-b", "a@a-b-c", "a@ab-c", "a@a-bc",
            "a@a-b.c-d", "a@aa.b-cd", "a@bc\n", "a\n@bc", "é@bc", "a@éé", "a@ａｂ", "a@b_c",
            "a@bc@de", "a@bc\r", "a@bc\u0000", "a@bc\u2028", "a@bc\u0085"}) {
            assertEquals(email.matches(Email.VALIDATION_REGEX), Email.isValidEmail(email), email);
        }
    }

    private void assertFormatValidationMatches(String alphabet, int maximumLength, String prefix, String suffix) {
        Pattern formatPattern = Pattern.compile(Email.VALIDATION_REGEX);
        int combinations = 1;
        for (int length = 0; length <= maximumLength; length++) {
            for (int combination = 0; combination < combinations; combination++) {
                StringBuilder candidate = new StringBuilder(prefix);
                int remaining = combination;
                for (int i = 0; i < length; i++) {
                    candidate.append(alphabet.charAt(remaining % alphabet.length()));
                    remaining /= alphabet.length();
                }
                String email = candidate.append(suffix).toString();
                assertEquals(formatPattern.matcher(email).matches(), Email.isValidEmail(email), email);
            }
            combinations *= alphabet.length();
        }
    }

    @Test
    public void equals() {
        Email email = new Email("valid@example.com");

        // same values -> returns true
        assertTrue(email.equals(new Email("valid@example.com")));
        assertEquals(email.hashCode(), new Email("valid@example.com").hashCode());

        // same object -> returns true
        assertTrue(email.equals(email));

        // null -> returns false
        assertFalse(email.equals(null));

        // different types -> returns false
        assertFalse(email.equals(5.0f));

        // different values -> returns false
        assertFalse(email.equals(new Email("other.valid@example.com")));
    }
}
