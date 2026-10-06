package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

public class StudentTest {

    @Test
    public void constructor_requiredStudentFields_preservesIdentityAndParentContact() {
        Student student = createStudent("S1", "Alex Tan", "S2", "00012345");

        assertEquals(new PersonId("S1"), student.getId());
        assertEquals("Alex Tan", student.getName().fullName);
        assertEquals(new EducationLevel("S2"), student.getLevel());
        assertEquals("00012345", student.getParentPhone().value);
        assertTrue(student.getContactDetails().getPhone().isEmpty());
    }

    @Test
    public void constructor_optionalContactFields_preservesStudentContactDetails() {
        Student student = createStudentWithContactDetails("00012345", "alex@example.com", "12 Main Street");

        assertEquals(Optional.of(new Phone("00012345")), student.getContactDetails().getPhone());
        assertEquals(Optional.of(new Email("alex@example.com")), student.getContactDetails().getEmail());
        assertEquals(Optional.of(new Address("12 Main Street")), student.getContactDetails().getAddress());
        assertEquals(new Phone("91234567"), student.getParentPhone());
    }

    @Test
    public void constructor_nonStudentIds_throwsIllegalArgumentException() {
        ContactDetails details = new ContactDetails(new Name("Alex Tan"));
        EducationLevel level = new EducationLevel("S2");
        Phone parentPhone = new Phone("91234567");

        assertThrows(IllegalArgumentException.class, () ->
                new Student(new PersonId("T1"), details, level, parentPhone));
        assertThrows(IllegalArgumentException.class, () ->
                new Student(new PersonId("P1"), details, level, parentPhone));
    }

    @Test
    public void constructor_nullRequiredFields_throwsNullPointerException() {
        PersonId id = new PersonId("S1");
        ContactDetails details = new ContactDetails(new Name("Alex Tan"));
        EducationLevel level = new EducationLevel("S2");
        Phone parentPhone = new Phone("91234567");

        assertThrows(NullPointerException.class, () -> new Student(null, details, level, parentPhone));
        assertThrows(NullPointerException.class, () -> new Student(id, null, level, parentPhone));
        assertThrows(NullPointerException.class, () -> new Student(id, details, null, parentPhone));
        assertThrows(NullPointerException.class, () -> new Student(id, details, level, null));
    }

    @Test
    public void constructor_parentPhoneBounds_acceptsDocumentedBounds() {
        assertEquals("000", createStudent("S1", "Alex Tan", "P1", "000").getParentPhone().value);
        assertEquals("123456789012345",
                createStudent("S1", "Alex Tan", "JC2", "123456789012345").getParentPhone().value);
        assertThrows(IllegalArgumentException.class, () ->
                createStudent("S1", "Alex Tan", "S2", "1234567890123456"));
    }

    @Test
    public void isDuplicateOf_nameAndParentContact_ignoresIdLevelAndOptionalFields() {
        Student original = createStudent("S1", "Alex Tan", "S2", "91234567");
        ContactDetails differentDetails = new ContactDetails(new Name("Alex  TAN "),
                Optional.of(new Phone("92345678")), Optional.of(new Email("alex@example.com")),
                Optional.of(new Address("12 Main Street")));
        Student duplicate = new Student(new PersonId("S2"), differentDetails,
                new EducationLevel("JC1"), new Phone("91234567"));

        assertTrue(original.isDuplicateOf(original));
        assertTrue(original.isDuplicateOf(duplicate));
        assertTrue(duplicate.isDuplicateOf(original));
        assertNotEquals(original, duplicate);
    }

    @Test
    public void isDuplicateOf_siblingsOrDifferentParentPhones_returnsFalse() {
        Student original = createStudent("S1", "Alex Tan", "S2", "91234567");

        assertFalse(original.isDuplicateOf(createStudent("S2", "Bob Tan", "S2", "91234567")));
        assertFalse(original.isDuplicateOf(createStudent("S2", "Alex Tan", "S2", "92345678")));
        assertFalse(original.isDuplicateOf(null));
    }

    @Test
    public void equals_recordChanges_distinguishesStoredState() {
        Student original = createStudent("S1", "Alex Tan", "S2", "91234567");
        Student same = createStudent("s1", "Alex Tan", "s2", "91234567");

        assertEquals(original, same);
        assertEquals(original.hashCode(), same.hashCode());
        assertNotEquals(original, createStudent("S2", "Alex Tan", "S2", "91234567"));
        assertNotEquals(original, createStudent("S1", "Alex TAN", "S2", "91234567"));
        assertNotEquals(original, createStudent("S1", "Alex Tan", "S3", "91234567"));
        assertNotEquals(original, createStudent("S1", "Alex Tan", "S2", "92345678"));
        assertNotEquals(original, null);
        assertNotEquals(original, original.getContactDetails());
    }

    @Test
    public void equals_ownPhoneChange_distinguishesStateWithoutChangingDuplicateKey() {
        Student original = createStudentWithContactDetails("91234567", "alex@example.com", "12 Main Street");
        Student changed = createStudentWithContactDetails("92345678", "alex@example.com", "12 Main Street");

        assertNotEquals(original, changed);
        assertTrue(original.isDuplicateOf(changed));
        assertTrue(changed.isDuplicateOf(original));
    }

    @Test
    public void equals_emailChange_distinguishesStateWithoutChangingDuplicateKey() {
        Student original = createStudentWithContactDetails("91234567", "alex@example.com", "12 Main Street");
        Student changed = createStudentWithContactDetails("91234567", "alex.tan@example.com", "12 Main Street");

        assertNotEquals(original, changed);
        assertTrue(original.isDuplicateOf(changed));
        assertTrue(changed.isDuplicateOf(original));
    }

    @Test
    public void equals_addressChange_distinguishesStateWithoutChangingDuplicateKey() {
        Student original = createStudentWithContactDetails("91234567", "alex@example.com", "12 Main Street");
        Student changed = createStudentWithContactDetails("91234567", "alex@example.com", "34 Main Street");

        assertNotEquals(original, changed);
        assertTrue(original.isDuplicateOf(changed));
        assertTrue(changed.isDuplicateOf(original));
    }

    private static Student createStudent(String id, String name, String level, String parentPhone) {
        return new Student(new PersonId(id), new ContactDetails(new Name(name)),
                new EducationLevel(level), new Phone(parentPhone));
    }

    private static Student createStudentWithContactDetails(String phone, String email, String address) {
        ContactDetails details = new ContactDetails(new Name("Alex Tan"), Optional.of(new Phone(phone)),
                Optional.of(new Email(email)), Optional.of(new Address(address)));
        return new Student(new PersonId("S1"), details, new EducationLevel("S2"), new Phone("91234567"));
    }
}
