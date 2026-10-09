package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class PhoneTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Phone(null));
    }

    @Test
    public void constructor_invalidPhone_throwsIllegalArgumentException() {
        String[] invalidPhones = {"", " ", "12", "1234567890123456", "+651234", "9011p041", "9312 1534",
            "123-456", " 123", "123 ", "123\n", "\t123", "123\0", "\uff11\uff12\uff13",
            "\u0661\u0662\u0663"};
        for (String invalidPhone : invalidPhones) {
            assertFalse(Phone.isValidPhone(invalidPhone));
            assertThrows(IllegalArgumentException.class, Phone.MESSAGE_CONSTRAINTS, () -> new Phone(invalidPhone));
        }
    }

    @Test
    public void isValidPhone() {
        // null phone number
        assertThrows(NullPointerException.class, () -> Phone.isValidPhone(null));

        String[] validPhones = {"911", "000", "00123456", "93121534", "124293842033123"};
        for (String validPhone : validPhones) {
            assertTrue(Phone.isValidPhone(validPhone));
            assertEquals(validPhone, new Phone(validPhone).value);
        }
    }

    @Test
    public void constructor_leadingZeros_preservesPhone() {
        Phone phone = new Phone("00123456");
        assertEquals("00123456", phone.value);
        assertEquals("00123456", phone.toString());
        assertFalse(phone.equals(new Phone("123456")));
    }

    @Test
    public void equals() {
        Phone phone = new Phone("999");

        // same values -> returns true
        assertTrue(phone.equals(new Phone("999")));

        // same object -> returns true
        assertTrue(phone.equals(phone));

        // null -> returns false
        assertFalse(phone.equals(null));

        // different types -> returns false
        assertFalse(phone.equals(5.0f));

        // different values -> returns false
        assertFalse(phone.equals(new Phone("995")));
    }
}
