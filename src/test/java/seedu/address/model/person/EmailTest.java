package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

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
