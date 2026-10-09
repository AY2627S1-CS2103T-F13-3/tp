package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Name;
import seedu.address.model.person.Parent;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Tutor;

public class PersonAdditionInputTest {

    private static final ContactDetails NAME_ONLY = new ContactDetails(new Name("Alex Tan"));
    private static final ContactDetails OWN_CONTACTS = new ContactDetails(new Name("Mei Lim"),
            Optional.of(new Phone("00987654")), Optional.empty(), Optional.empty());
    private static final EducationLevel LEVEL = new EducationLevel("S2");
    private static final Phone PARENT_PHONE = new Phone("00123456");

    @Test
    public void constructor_studentWithoutOwnContacts_preservesComponents() {
        PersonAdditionInput input = new PersonAdditionInput(PersonRole.STUDENT, NAME_ONLY,
                Optional.of(LEVEL), Optional.of(PARENT_PHONE));
        assertEquals(PersonRole.STUDENT, input.role());
        assertEquals(NAME_ONLY, input.contactDetails());
        assertEquals(Optional.of(LEVEL), input.level());
        assertEquals(Optional.of(PARENT_PHONE), input.parentPhone());
        assertEquals(Optional.empty(), input.contactDetails().getPhone());
    }

    @Test
    public void constructor_tutorAndParentWithOwnPhone_acceptsAbsentStudentFields() {
        for (PersonRole role : new PersonRole[]{PersonRole.TUTOR, PersonRole.PARENT}) {
            PersonAdditionInput input = new PersonAdditionInput(role, OWN_CONTACTS, Optional.empty(), Optional.empty());
            assertEquals(role, input.role());
            assertEquals(OWN_CONTACTS, input.contactDetails());
            assertEquals(Optional.empty(), input.level());
            assertEquals(Optional.empty(), input.parentPhone());
        }
    }

    @Test
    public void constructor_nullComponent_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                new PersonAdditionInput(null, NAME_ONLY, Optional.of(LEVEL), Optional.of(PARENT_PHONE)));
        assertThrows(NullPointerException.class, () ->
                new PersonAdditionInput(PersonRole.STUDENT, null, Optional.of(LEVEL), Optional.of(PARENT_PHONE)));
        assertThrows(NullPointerException.class, () ->
                new PersonAdditionInput(PersonRole.STUDENT, NAME_ONLY, null, Optional.of(PARENT_PHONE)));
        assertThrows(NullPointerException.class, () ->
                new PersonAdditionInput(PersonRole.STUDENT, NAME_ONLY, Optional.of(LEVEL), null));
    }

    @Test
    public void constructor_missingStudentFields_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, "Student additions require an education level.", () ->
                new PersonAdditionInput(PersonRole.STUDENT, NAME_ONLY, Optional.empty(), Optional.of(PARENT_PHONE)));
        assertThrows(IllegalArgumentException.class, "Student additions require a parent phone number.", () ->
                new PersonAdditionInput(PersonRole.STUDENT, NAME_ONLY, Optional.of(LEVEL), Optional.empty()));
    }

    @Test
    public void constructor_studentParentPhoneTooLong_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new PersonAdditionInput(PersonRole.STUDENT, NAME_ONLY,
                Optional.of(LEVEL), Optional.of(new Phone("0001234567890123"))));
    }

    @Test
    public void constructor_missingTutorOrParentOwnPhone_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, Tutor.MESSAGE_PHONE_CONSTRAINTS, () ->
                new PersonAdditionInput(PersonRole.TUTOR, NAME_ONLY, Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, Parent.MESSAGE_PHONE_CONSTRAINTS, () ->
                new PersonAdditionInput(PersonRole.PARENT, NAME_ONLY, Optional.empty(), Optional.empty()));
    }

    @Test
    public void constructor_studentFieldsForOtherRoles_throwsIllegalArgumentException() {
        for (PersonRole role : new PersonRole[]{PersonRole.TUTOR, PersonRole.PARENT}) {
            assertThrows(IllegalArgumentException.class, () ->
                    new PersonAdditionInput(role, OWN_CONTACTS, Optional.of(LEVEL), Optional.empty()));
            assertThrows(IllegalArgumentException.class, () ->
                    new PersonAdditionInput(role, OWN_CONTACTS, Optional.empty(), Optional.of(PARENT_PHONE)));
            assertThrows(IllegalArgumentException.class, () ->
                    new PersonAdditionInput(role, OWN_CONTACTS, Optional.of(LEVEL), Optional.of(PARENT_PHONE)));
        }
    }

}
