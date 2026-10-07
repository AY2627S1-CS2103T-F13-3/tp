package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.Address;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Parent;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Student;
import seedu.address.model.person.Tutor;

public class PersonRecordCardDataTest {

    @Test
    public void constructor_studentWithoutOptionalContacts_labelsRequiredAndMissingFields() {
        Student student = new Student(new PersonId("S8"), new ContactDetails(new Name("Alex Tan")),
                new EducationLevel("P1"), new Phone("00012345"));
        PersonRecordCardData data = new PersonRecordCardData(student, 2);

        assertEquals("2. Alex Tan", data.getHeading());
        assertEquals("Student · S8", data.getIdentity());
        assertEquals(List.of("Level: P1", "Parent phone: 00012345", "Own phone: Not provided",
                "Email: Not provided", "Address: Not provided"), data.getDetailLines());
    }

    @Test
    public void constructor_populatedStudent_preservesExactDisplayValuesAndSeparatesPhones() {
        ContactDetails contacts = populatedContacts("Alex  TAN ");
        Student student = new Student(new PersonId("S12"), contacts, new EducationLevel("JC2"),
                new Phone("00987654"));
        PersonRecordCardData data = new PersonRecordCardData(student, 4);

        assertEquals("4. " + contacts.getName().fullName, data.getHeading());
        assertEquals("Student · S12", data.getIdentity());
        assertEquals(List.of("Level: JC2", "Parent phone: 00987654", "Own phone: 00012345",
                "Email: Alex.Tan@Example.com",
                "Address: " + contacts.getAddress().orElseThrow().value), data.getDetailLines());
        assertEquals(contacts, student.getContactDetails());
        assertEquals(contacts.getName().fullName, student.getName().fullName);
        assertEquals("00987654", student.getParentPhone().value);
    }

    @Test
    public void constructor_tutorAndParentWithoutOptionalContacts_showsRequiredOwnPhone() {
        ContactDetails contacts = new ContactDetails(new Name("Alex TAN"), Optional.of(new Phone("00012345")),
                Optional.empty(), Optional.empty());
        List<PersonRecord> records = List.of(new Tutor(new PersonId("T41"), contacts),
                new Parent(new PersonId("P11"), contacts));
        List<String> identities = List.of("Tutor · T41", "Parent · P11");

        for (int i = 0; i < records.size(); i++) {
            PersonRecordCardData data = new PersonRecordCardData(records.get(i), 7);

            assertEquals("7. Alex TAN", data.getHeading());
            assertEquals(identities.get(i), data.getIdentity());
            assertEquals(List.of("Phone: 00012345", "Email: Not provided", "Address: Not provided"),
                    data.getDetailLines());
        }
    }

    @Test
    public void constructor_populatedTutorAndParent_preservesOptionalValuesWithoutStudentFields() {
        ContactDetails contacts = populatedContacts("Alex  TAN ");
        List<PersonRecord> records = List.of(new Tutor(new PersonId("T1"), contacts),
                new Parent(new PersonId("P1"), contacts));

        for (PersonRecord record : records) {
            PersonRecordCardData data = new PersonRecordCardData(record, 1);

            assertEquals("1. " + contacts.getName().fullName, data.getHeading());
            assertEquals(List.of("Phone: 00012345", "Email: Alex.Tan@Example.com",
                    "Address: " + contacts.getAddress().orElseThrow().value), data.getDetailLines());
            assertEquals(contacts, record.getContactDetails());
        }
    }

    @Test
    public void constructor_sameStableIdDifferentPositions_changesOnlyDisplayedPosition() {
        Student student = new Student(PersonId.of(PersonRole.STUDENT, Long.MAX_VALUE),
                new ContactDetails(new Name("Alex Tan")), new EducationLevel("S2"), new Phone("00012345"));
        PersonRecordCardData firstPosition = new PersonRecordCardData(student, 1);
        PersonRecordCardData laterPosition = new PersonRecordCardData(student, 42);

        assertEquals("1. Alex Tan", firstPosition.getHeading());
        assertEquals("42. Alex Tan", laterPosition.getHeading());
        assertEquals("Student · S9223372036854775807", firstPosition.getIdentity());
        assertEquals(firstPosition.getIdentity(), laterPosition.getIdentity());
        assertEquals(firstPosition.getDetailLines(), laterPosition.getDetailLines());
        assertEquals(Long.MAX_VALUE, student.getId().getSequence());
    }

    @Test
    public void getDetailLines_attemptedMutation_preservesProjectionAndRecord() {
        Student student = new Student(new PersonId("S1"), populatedContacts("Alex Tan"),
                new EducationLevel("S2"), new Phone("00987654"));
        Student before = new Student(new PersonId("S1"), populatedContacts("Alex Tan"),
                new EducationLevel("S2"), new Phone("00987654"));
        PersonRecordCardData data = new PersonRecordCardData(student, 3);
        List<String> expectedLines = List.copyOf(data.getDetailLines());

        assertThrows(UnsupportedOperationException.class, () -> data.getDetailLines().clear());
        assertThrows(UnsupportedOperationException.class, () ->
                data.getDetailLines().set(0, "Level: P1"));
        assertEquals(expectedLines, data.getDetailLines());
        assertEquals(before, student);
        assertEquals(before.hashCode(), student.hashCode());
    }

    @Test
    public void constructor_longDisplayValues_preservesTextForWrapping() {
        String longName = "A".repeat(100);
        String longAddress = "12 " + "Long road ".repeat(18).strip();
        ContactDetails contacts = new ContactDetails(new Name(longName), Optional.of(new Phone("000123456789012")),
                Optional.of(new Email("alex.long.contact@example.com")), Optional.of(new Address(longAddress)));
        Tutor tutor = new Tutor(new PersonId("T1"), contacts);
        PersonRecordCardData data = new PersonRecordCardData(tutor, 1);

        assertEquals("1. " + longName, data.getHeading());
        assertEquals(List.of("Phone: 000123456789012", "Email: alex.long.contact@example.com",
                "Address: " + longAddress), data.getDetailLines());
        assertEquals(longAddress, tutor.getContactDetails().getAddress().orElseThrow().value);
    }

    @Test
    public void constructor_invalidRecordOrPosition_rejectsUnsupportedInput() {
        Tutor tutor = new Tutor(new PersonId("T1"), populatedContacts("Alex Tan"));

        assertThrows(NullPointerException.class, () -> new PersonRecordCardData(null, 1));
        for (int index : new int[] {0, -1, Integer.MIN_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> new PersonRecordCardData(tutor, index));
        }
        assertThrows(IllegalArgumentException.class, () ->
                new PersonRecordCardData(new UnsupportedRecord(), 1));
    }

    private static ContactDetails populatedContacts(String name) {
        return new ContactDetails(new Name(name), Optional.of(new Phone("00012345")),
                Optional.of(new Email("Alex.Tan@Example.com")), Optional.of(new Address("12  Main Street ")));
    }

    private static class UnsupportedRecord implements PersonRecord {

        @Override
        public PersonId getId() {
            return new PersonId("S1");
        }

        @Override
        public ContactDetails getContactDetails() {
            return new ContactDetails(new Name("Alex Tan"));
        }

        @Override
        public boolean isDuplicateOf(PersonRecord other) {
            return false;
        }
    }
}
