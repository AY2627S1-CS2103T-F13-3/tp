package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class AddressTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Address(null));
    }

    @Test
    public void constructor_invalidAddress_throwsIllegalArgumentException() {
        String[] invalidAddresses = {"", " ", "Blk 12/34", "123\tMain Street", "123\nMain Street",
            "\r123 Main Street", "123 Main Street\0", "123 Main Street\u007f", "Caf\u00e9 Road",
            "123\u00a0Main Street", "\u7a9d\u5c45", "A".repeat(201)};
        for (String invalidAddress : invalidAddresses) {
            assertFalse(Address.isValidAddress(invalidAddress));
            assertThrows(IllegalArgumentException.class, Address.MESSAGE_CONSTRAINTS, ()
                    -> new Address(invalidAddress));
        }
    }

    @Test
    public void isValidAddress() {
        // null address
        assertThrows(NullPointerException.class, () -> Address.isValidAddress(null));

        String[] validAddresses = {"-", "Blk 456, Den Road, #01-355", "St. John's Road (East); Unit #01-02",
            "Block A\\B; \"Unit\" #01-02", "  123   Main Street  ", "A".repeat(200),
            "  " + "A  ".repeat(99) + "AA  "};
        for (String validAddress : validAddresses) {
            assertTrue(Address.isValidAddress(validAddress));
            assertDoesNotThrow(() -> new Address(validAddress));
        }
    }

    @Test
    public void constructor_repeatedSpaces_normalizesAddress() {
        Address address = new Address("  Blk 456,   Den Road, #01-355  ");
        assertEquals("Blk 456, Den Road, #01-355", address.value);
        assertEquals("Blk 456, Den Road, #01-355", address.toString());
    }

    @Test
    public void equals_normalizedAddresses_returnsTrue() {
        Address normalizedAddress = new Address("Blk 456, Den Road, #01-355");
        Address unnormalizedAddress = new Address("  Blk 456,   Den Road, #01-355  ");
        assertEquals(normalizedAddress, unnormalizedAddress);
        assertEquals(normalizedAddress.hashCode(), unnormalizedAddress.hashCode());
        assertFalse(normalizedAddress.equals(new Address("blk 456, den road, #01-355")));
    }

    @Test
    public void equals() {
        Address address = new Address("Valid Address");

        // same values -> returns true
        assertTrue(address.equals(new Address("Valid Address")));

        // same object -> returns true
        assertTrue(address.equals(address));

        // null -> returns false
        assertFalse(address.equals(null));

        // different types -> returns false
        assertFalse(address.equals(5.0f));

        // different values -> returns false
        assertFalse(address.equals(new Address("Other Valid Address")));
    }
}
