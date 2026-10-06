package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

public class ParentTest {

    @Test
    public void constructor_requiredFields_preservesPhoneBoundsAndOptionalAbsence() {
        Parent shortest = new Parent(new PersonId("P1"), createDetails("000"));
        Parent longest = new Parent(new PersonId("P2"), createDetails("000123456789012"));

        assertEquals("000", shortest.getPhone().value);
        assertEquals("000123456789012", longest.getPhone().value);
        assertTrue(shortest.getContactDetails().getEmail().isEmpty());
        assertTrue(shortest.getContactDetails().getAddress().isEmpty());
    }

    @Test
    public void constructor_nonParentIds_throwsIllegalArgumentException() {
        ContactDetails details = createDetails("00012345");

        assertThrows(IllegalArgumentException.class, () -> new Parent(new PersonId("S1"), details));
        assertThrows(IllegalArgumentException.class, () -> new Parent(new PersonId("T1"), details));
    }

    @Test
    public void constructor_missingOwnPhone_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Parent(new PersonId("P1"), new ContactDetails(new Name("Alex Tan"))));
    }

    @Test
    public void constructor_nullRequiredFields_throwsNullPointerException() {
        PersonId id = new PersonId("P1");
        ContactDetails details = createDetails("00012345");

        assertThrows(NullPointerException.class, () -> new Parent(null, details));
        assertThrows(NullPointerException.class, () -> new Parent(id, null));
    }

    private static ContactDetails createDetails(String phone) {
        return new ContactDetails(new Name("Alex Tan"), Optional.of(new Phone(phone)),
                Optional.empty(), Optional.empty());
    }
}
