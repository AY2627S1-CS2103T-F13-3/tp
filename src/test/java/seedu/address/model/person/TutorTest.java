package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

public class TutorTest {

    @Test
    public void constructor_requiredFields_preservesPhoneBoundsAndOptionalAbsence() {
        Tutor shortest = new Tutor(new PersonId("T1"), createDetails("000"));
        Tutor longest = new Tutor(new PersonId("T2"), createDetails("000123456789012"));

        assertEquals("000", shortest.getPhone().value);
        assertEquals("000123456789012", longest.getPhone().value);
        assertTrue(shortest.getContactDetails().getEmail().isEmpty());
        assertTrue(shortest.getContactDetails().getAddress().isEmpty());
    }

    @Test
    public void constructor_nonTutorIds_throwsIllegalArgumentException() {
        ContactDetails details = createDetails("00012345");

        assertThrows(IllegalArgumentException.class, () -> new Tutor(new PersonId("S1"), details));
        assertThrows(IllegalArgumentException.class, () -> new Tutor(new PersonId("P1"), details));
    }

    @Test
    public void constructor_missingOwnPhone_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Tutor(new PersonId("T1"), new ContactDetails(new Name("Alex Tan"))));
    }

    @Test
    public void constructor_nullRequiredFields_throwsNullPointerException() {
        PersonId id = new PersonId("T1");
        ContactDetails details = createDetails("00012345");

        assertThrows(NullPointerException.class, () -> new Tutor(null, details));
        assertThrows(NullPointerException.class, () -> new Tutor(id, null));
    }

    private static ContactDetails createDetails(String phone) {
        return new ContactDetails(new Name("Alex Tan"), Optional.of(new Phone(phone)),
                Optional.empty(), Optional.empty());
    }
}
