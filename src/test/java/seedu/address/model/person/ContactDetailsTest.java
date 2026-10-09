package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

public class ContactDetailsTest {

    @Test
    public void constructor_missingOptionalFields_preservesAbsence() {
        ContactDetails details = new ContactDetails(new Name("Alex Tan"));

        assertTrue(details.getPhone().isEmpty());
        assertTrue(details.getEmail().isEmpty());
        assertTrue(details.getAddress().isEmpty());
    }

    @Test
    public void constructor_populatedOptionalFields_preservesContactDetails() {
        ContactDetails details = new ContactDetails(new Name("Alex Tan"), Optional.of(new Phone("0012345678")),
                Optional.of(new Email("alex@example.com")), Optional.of(new Address("12 Main Street")));

        assertEquals(new Name("Alex Tan"), details.getName());
        assertEquals("0012345678", details.getPhone().orElseThrow().value);
        assertEquals(Optional.of(new Email("alex@example.com")), details.getEmail());
        assertEquals(Optional.of(new Address("12 Main Street")), details.getAddress());
    }

    @Test
    public void constructor_nullFields_throwsNullPointerException() {
        Name name = new Name("Alex Tan");
        assertThrows(NullPointerException.class, () -> new ContactDetails(null));
        assertThrows(NullPointerException.class, () ->
                new ContactDetails(name, null, Optional.empty(), Optional.empty()));
        assertThrows(NullPointerException.class, () ->
                new ContactDetails(name, Optional.empty(), null, Optional.empty()));
        assertThrows(NullPointerException.class, () ->
                new ContactDetails(name, Optional.empty(), Optional.empty(), null));
    }

    @Test
    public void constructor_phoneBounds_preservesValidNumbersAndRejectsExcessLength() {
        Name name = new Name("Alex Tan");
        ContactDetails shortest = new ContactDetails(name, Optional.of(new Phone("000")),
                Optional.empty(), Optional.empty());
        ContactDetails longest = new ContactDetails(name, Optional.of(new Phone("000123456789012")),
                Optional.empty(), Optional.empty());

        assertEquals("000", shortest.getPhone().orElseThrow().value);
        assertEquals("000123456789012", longest.getPhone().orElseThrow().value);
        assertThrows(IllegalArgumentException.class, () ->
                new ContactDetails(name, Optional.of(new Phone("1234567890123456")),
                        Optional.empty(), Optional.empty()));
    }

    @Test
    public void getNormalizedName_caseAndSpacing_preservesDisplayCase() {
        ContactDetails details = new ContactDetails(new Name("Alex  TAN "));

        assertEquals("alex tan", details.getNormalizedName());
        assertEquals("Alex TAN", details.getName().fullName);
    }

    @Test
    public void equals_contactChanges_distinguishesStoredValues() {
        Name name = new Name("Alex Tan");
        ContactDetails original = new ContactDetails(name);
        ContactDetails same = new ContactDetails(new Name("Alex Tan"));
        ContactDetails withPhone = new ContactDetails(name, Optional.of(new Phone("91234567")),
                Optional.empty(), Optional.empty());
        ContactDetails withEmail = new ContactDetails(name, Optional.empty(),
                Optional.of(new Email("alex@example.com")), Optional.empty());
        ContactDetails withAddress = new ContactDetails(name, Optional.empty(), Optional.empty(),
                Optional.of(new Address("12 Main Street")));

        assertEquals(original, same);
        assertEquals(original.hashCode(), same.hashCode());
        assertNotEquals(original, withPhone);
        assertNotEquals(original, withEmail);
        assertNotEquals(original, withAddress);
        assertNotEquals(original, new ContactDetails(new Name("Alex TAN")));
        assertNotEquals(original, null);
        assertNotEquals(original, name);
    }

    @Test
    public void equalsAndHashCode_populatedContacts_matchesIndependentValues() {
        ContactDetails original = new ContactDetails(new Name("Alex Tan"), Optional.of(new Phone("0012345678")),
                Optional.of(new Email("alex@example.com")), Optional.of(new Address("12 Main Street")));
        ContactDetails same = new ContactDetails(new Name("Alex Tan"), Optional.of(new Phone("0012345678")),
                Optional.of(new Email("alex@example.com")), Optional.of(new Address("12 Main Street")));

        assertEquals(original, same);
        assertEquals(same, original);
        assertEquals(original.hashCode(), same.hashCode());
    }
}
