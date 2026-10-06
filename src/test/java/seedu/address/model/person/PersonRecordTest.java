package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

public class PersonRecordTest {

    @Test
    public void getters_mixedRoles_preservesIdentityRoleAndContacts() {
        ContactDetails details = createDetails("Alex  TAN ", "00012345", "alex@example.com", "12 Main Street");
        List<PersonRecord> records = List.of(
                new Student(new PersonId("S1"), details, new EducationLevel("S2"), new Phone("00987654")),
                new Tutor(new PersonId("T1"), details),
                new Parent(new PersonId("P1"), details));

        for (PersonRecord record : records) {
            assertEquals(PersonId.of(record.getRole(), 1), record.getId());
            assertEquals(details, record.getContactDetails());
            assertEquals(details.getName(), record.getName());
            assertEquals("alex tan", record.getContactDetails().getNormalizedName());
            assertEquals(Optional.of(new Phone("00012345")), record.getContactDetails().getPhone());
            assertEquals(Optional.of(new Email("alex@example.com")), record.getContactDetails().getEmail());
            assertEquals(Optional.of(new Address("12 Main Street")), record.getContactDetails().getAddress());
        }
        assertEquals(PersonRole.STUDENT, records.get(0).getRole());
        assertEquals(PersonRole.TUTOR, records.get(1).getRole());
        assertEquals(PersonRole.PARENT, records.get(2).getRole());
    }

    @Test
    public void isDuplicateOf_crossRoleRecords_returnsFalseSymmetrically() {
        ContactDetails details = createDetails("Alex Tan", "00012345", null, null);
        List<PersonRecord> records = List.of(
                new Student(new PersonId("S1"), details, new EducationLevel("S2"), new Phone("00012345")),
                new Tutor(new PersonId("T1"), details),
                new Parent(new PersonId("P1"), details));

        for (int i = 0; i < records.size(); i++) {
            for (int j = i + 1; j < records.size(); j++) {
                PersonRecord first = records.get(i);
                PersonRecord second = records.get(j);
                assertFalse(first.isDuplicateOf(second));
                assertFalse(second.isDuplicateOf(first));
                assertNotEquals(first, second);
                assertNotEquals(second, first);
            }
        }
    }

    @Test
    public void isDuplicateOf_students_usesParentPhoneThroughCommonInterface() {
        PersonRecord original = createStudent("S1", "00012345", "00987654");
        PersonRecord differentOwnPhone = createStudent("S2", "00123456", "00987654");
        PersonRecord differentParentPhone = createStudent("S3", "00012345", "00876543");
        PersonRecord parentWithSameOwnPhone = new Parent(new PersonId("P1"),
                createDetails("Alex Tan", "00987654", null, null));

        assertTrue(original.isDuplicateOf(differentOwnPhone));
        assertTrue(differentOwnPhone.isDuplicateOf(original));
        assertFalse(original.isDuplicateOf(differentParentPhone));
        assertFalse(differentParentPhone.isDuplicateOf(original));
        assertFalse(original.isDuplicateOf(parentWithSameOwnPhone));
        assertFalse(parentWithSameOwnPhone.isDuplicateOf(original));
        assertFalse(original.isDuplicateOf(null));
    }

    @Test
    public void isDuplicateOf_tutorAndParentKeys_ignoresIdAndOptionalFields() {
        for (PersonRole role : List.of(PersonRole.TUTOR, PersonRole.PARENT)) {
            PersonRecord original = createRecord(role, 1,
                    createDetails("Alex  TAN ", "00012345", null, null));
            PersonRecord duplicate = createRecord(role, 2,
                    createDetails("Alex Tan", "00012345", "alex@example.com", "12 Main Street"));

            assertTrue(original.isDuplicateOf(original));
            assertTrue(original.isDuplicateOf(duplicate));
            assertTrue(duplicate.isDuplicateOf(original));
            assertNotEquals(original, duplicate);
            assertEquals(new Name("Alex  TAN "), original.getName());
        }
    }

    @Test
    public void isDuplicateOf_differentTutorAndParentKeys_preservesExactPhones() {
        for (PersonRole role : List.of(PersonRole.TUTOR, PersonRole.PARENT)) {
            PersonRecord original = createRecord(role, 1,
                    createDetails("Alex Tan", "00012345", null, null));
            PersonRecord differentName = createRecord(role, 2,
                    createDetails("Bob Tan", "00012345", null, null));
            PersonRecord fewerLeadingZeros = createRecord(role, 2,
                    createDetails("Alex Tan", "0012345", null, null));

            assertFalse(original.isDuplicateOf(differentName));
            assertFalse(differentName.isDuplicateOf(original));
            assertFalse(original.isDuplicateOf(fewerLeadingZeros));
            assertFalse(fewerLeadingZeros.isDuplicateOf(original));
            assertFalse(original.isDuplicateOf(null));
        }
    }

    @Test
    public void equalsAndHashCode_sameTutorAndParentState_matchesIndependentRecords() {
        for (PersonRole role : List.of(PersonRole.TUTOR, PersonRole.PARENT)) {
            PersonRecord original = createRecord(role, 1,
                    createDetails("Alex Tan", "00012345", "alex@example.com", "12 Main Street"));
            PersonRecord same = createRecord(role, 1,
                    createDetails("Alex Tan", "00012345", "alex@example.com", "12 Main Street"));

            assertEquals(original, original);
            assertEquals(original, same);
            assertEquals(same, original);
            assertEquals(original.hashCode(), same.hashCode());
            assertNotEquals(original, null);
            assertNotEquals(original, original.getContactDetails());
        }
    }

    @Test
    public void equals_tutorAndParentRecordChanges_distinguishesEveryStoredField() {
        for (PersonRole role : List.of(PersonRole.TUTOR, PersonRole.PARENT)) {
            PersonRecord original = createRecord(role, 1,
                    createDetails("Alex Tan", "00012345", "alex@example.com", "12 Main Street"));
            assertNotEquals(original, createRecord(role, 2,
                    createDetails("Alex Tan", "00012345", "alex@example.com", "12 Main Street")));
            assertNotEquals(original, createRecord(role, 1,
                    createDetails("Alex TAN", "00012345", "alex@example.com", "12 Main Street")));
            assertNotEquals(original, createRecord(role, 1,
                    createDetails("Alex Tan", "00123456", "alex@example.com", "12 Main Street")));
            assertNotEquals(original, createRecord(role, 1,
                    createDetails("Alex Tan", "00012345", "alex.tan@example.com", "12 Main Street")));
            assertNotEquals(original, createRecord(role, 1,
                    createDetails("Alex Tan", "00012345", "alex@example.com", "34 Main Street")));
        }
    }

    @Test
    public void equals_optionalContactRemoval_changesStateWithoutChangingDuplicateKey() {
        for (PersonRole role : List.of(PersonRole.TUTOR, PersonRole.PARENT)) {
            PersonRecord original = createRecord(role, 1,
                    createDetails("Alex Tan", "00012345", "alex@example.com", "12 Main Street"));
            PersonRecord withoutEmail = createRecord(role, 1,
                    createDetails("Alex Tan", "00012345", null, "12 Main Street"));
            PersonRecord withoutAddress = createRecord(role, 1,
                    createDetails("Alex Tan", "00012345", "alex@example.com", null));

            assertNotEquals(original, withoutEmail);
            assertNotEquals(original, withoutAddress);
            assertTrue(original.isDuplicateOf(withoutEmail));
            assertTrue(withoutEmail.isDuplicateOf(original));
            assertTrue(original.isDuplicateOf(withoutAddress));
            assertTrue(withoutAddress.isDuplicateOf(original));
        }
    }

    private static PersonRecord createRecord(PersonRole role, long sequence, ContactDetails details) {
        PersonId id = PersonId.of(role, sequence);
        return switch (role) {
            case TUTOR -> new Tutor(id, details);
            case PARENT -> new Parent(id, details);
            default -> throw new IllegalArgumentException("This factory creates tutors and parents only.");
        };
    }

    private static Student createStudent(String id, String phone, String parentPhone) {
        return new Student(new PersonId(id), createDetails("Alex Tan", phone, null, null),
                new EducationLevel("S2"), new Phone(parentPhone));
    }

    private static ContactDetails createDetails(String name, String phone, String email, String address) {
        return new ContactDetails(new Name(name), Optional.of(new Phone(phone)),
                Optional.ofNullable(email).map(Email::new), Optional.ofNullable(address).map(Address::new));
    }
}
