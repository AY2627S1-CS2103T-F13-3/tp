package ponhub.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ponhub.model.person.exceptions.DuplicatePersonException;

public class PeopleRegistryStateTest {

    @Test
    public void constructor_mutableInputsAndReturnedCollections_cannotChangeSnapshot() {
        Student student = createStudent("S2", "Alex Tan");
        Tutor tutor = new Tutor(new PersonId("T1"), createContactDetails("Bob Tan"));
        List<PersonRecord> rows = new ArrayList<>(List.of(student, tutor));
        Map<PersonRole, Long> highWaterMarks = createCounters(5, 2, 3);
        PeopleRegistryState state = new PeopleRegistryState(rows, highWaterMarks);

        rows.clear();
        highWaterMarks.put(PersonRole.STUDENT, 99L);

        assertEquals(List.of(student, tutor), state.getPeople());
        assertEquals(createCounters(5, 2, 3), state.getLastAllocatedSequences());
        assertThrows(UnsupportedOperationException.class, () -> state.getPeople().clear());
        assertThrows(UnsupportedOperationException.class, () ->
                state.getLastAllocatedSequences().put(PersonRole.TUTOR, 99L));
    }

    @Test
    public void constructor_invalidHighWaterMarks_rejectsIncompleteNegativeAndUnderstatedState() {
        Student student = createStudent("S2", "Alex Tan");
        Map<PersonRole, Long> incomplete = createCounters(2, 0, 0);
        incomplete.remove(PersonRole.PARENT);

        assertThrows(IllegalArgumentException.class, () -> new PeopleRegistryState(List.of(student), incomplete));
        assertThrows(IllegalArgumentException.class, () ->
                new PeopleRegistryState(List.of(), createCounters(0, -1, 0)));
        assertThrows(IllegalArgumentException.class, () ->
                new PeopleRegistryState(List.of(student), createCounters(1, 0, 0)));

        Map<PersonRole, Long> nullCounter = createCounters(2, 0, 0);
        nullCounter.put(PersonRole.PARENT, null);
        assertThrows(NullPointerException.class, () -> new PeopleRegistryState(List.of(student), nullCounter));
    }

    @Test
    public void constructor_nullInputsAndRows_rejectsMissingRequiredState() {
        assertThrows(NullPointerException.class, () -> new PeopleRegistryState(null, createCounters(0, 0, 0)));
        assertThrows(NullPointerException.class, () -> new PeopleRegistryState(List.of(), null));
        List<PersonRecord> missingRow = new ArrayList<>();
        missingRow.add(null);
        assertThrows(NullPointerException.class, () -> new PeopleRegistryState(missingRow, createCounters(0, 0, 0)));
    }

    @Test
    public void constructor_duplicateIdsOrBusinessKeys_rejectsAmbiguousRecords() {
        Student first = createStudent("S1", "Alex Tan");
        Student sameIdDifferentContact = createStudent("S1", "Bob Tan");
        assertThrows(DuplicatePersonException.class, () -> new PeopleRegistryState(
                List.of(first, sameIdDifferentContact), createCounters(1, 0, 0)));

        for (PersonRole role : PersonRole.values()) {
            PersonRecord original = createPerson(role, 1, "Alex Tan");
            PersonRecord duplicate = createPerson(role, 2, "Alex  TAN ");
            assertThrows(DuplicatePersonException.class, () -> new PeopleRegistryState(
                    List.of(original, duplicate), createCounters(2, 2, 2)));
        }
    }

    @Test
    public void constructor_sameContactAcrossRoles_preservesDistinctRecords() {
        List<PersonRecord> records = List.of(createPerson(PersonRole.PARENT, 1, "Alex Tan"),
                createPerson(PersonRole.STUDENT, 1, "Alex Tan"), createPerson(PersonRole.TUTOR, 1, "Alex Tan"));

        assertEquals(records, new PeopleRegistryState(records, createCounters(1, 1, 1)).getPeople());
    }

    @Test
    public void constructor_unicodeNamesAndOptionalContacts_preservesDuplicateRulesForEveryRole() {
        for (PersonRole role : PersonRole.values()) {
            PersonRecord original = createPerson(role, 1, "José Santos");
            ContactDetails changedContacts = new ContactDetails(new Name("\u00a0JOSE\u0301   SANTOS\u2003"),
                    Optional.of(new Phone("00012345")), Optional.of(new Email("changed@example.com")),
                    Optional.of(new Address("Changed address")));
            PersonRecord duplicate = switch (role) {
                case STUDENT -> new Student(PersonId.of(role, 2), changedContacts,
                        new EducationLevel("JC2"), new Phone("00012345"));
                case TUTOR -> new Tutor(PersonId.of(role, 2), changedContacts);
                case PARENT -> new Parent(PersonId.of(role, 2), changedContacts);
            };

            assertThrows(DuplicatePersonException.class, () -> new PeopleRegistryState(
                    List.of(original, duplicate), createCounters(2, 2, 2)), role.toString());
            PersonRecord unaccented = createPerson(role, 2, "Jose Santos");
            List<PersonRecord> distinctNames = List.of(original, unaccented);
            assertEquals(distinctNames, new PeopleRegistryState(distinctNames, createCounters(2, 2, 2)).getPeople());
        }
    }

    @Test
    public void constructor_studentOwnPhoneAndLevel_doNotDistinguishDuplicateIdentity() {
        Student original = createStudent("S1", "Alex Tan");
        ContactDetails changedContacts = new ContactDetails(new Name("Alex Tan"),
                Optional.of(new Phone("99912345")), Optional.empty(), Optional.empty());
        Student duplicate = new Student(new PersonId("S2"), changedContacts,
                new EducationLevel("JC2"), original.getParentPhone());

        assertThrows(DuplicatePersonException.class, () -> new PeopleRegistryState(
                List.of(original, duplicate), createCounters(2, 0, 0)));
    }

    @Test
    public void constructor_differentRelevantPhones_preservesLeadingZerosForEveryRole() {
        for (PersonRole role : PersonRole.values()) {
            PersonRecord original = createPerson(role, 1, "Alex Tan");
            ContactDetails changedContacts = new ContactDetails(new Name("Alex Tan"),
                    Optional.of(new Phone("12345")), Optional.empty(), Optional.empty());
            PersonRecord differentPhone = switch (role) {
                case STUDENT -> new Student(PersonId.of(role, 2), createContactDetails("Alex Tan"),
                        new EducationLevel("S2"), new Phone("12345"));
                case TUTOR -> new Tutor(PersonId.of(role, 2), changedContacts);
                case PARENT -> new Parent(PersonId.of(role, 2), changedContacts);
            };
            List<PersonRecord> distinctPhones = List.of(original, differentPhone);

            assertEquals(distinctPhones, new PeopleRegistryState(distinctPhones, createCounters(2, 2, 2)).getPeople());
        }
    }

    @Test
    public void constructor_largeRegistry_preservesOrderAndHistoryAndRejectsLateDuplicate() {
        List<PersonRecord> rows = new ArrayList<>();
        for (int sequence = 1; sequence <= 1000; sequence++) {
            String suffix = "" + (char) ('A' + sequence / (26 * 26))
                    + (char) ('A' + sequence / 26 % 26) + (char) ('A' + sequence % 26);
            for (PersonRole role : PersonRole.values()) {
                rows.add(createPerson(role, sequence, "Load Test " + suffix));
            }
        }
        Map<PersonRole, Long> allocationHistory = createCounters(2000, 3000, 4000);
        PeopleRegistryState state = new PeopleRegistryState(rows, allocationHistory);
        assertEquals(rows, state.getPeople());
        assertEquals(allocationHistory, state.getLastAllocatedSequences());

        rows.add(createPerson(PersonRole.STUDENT, 2000, "LOAD TEST AAB"));
        assertThrows(DuplicatePersonException.class, () -> new PeopleRegistryState(rows, allocationHistory));
        assertEquals(3000, state.getPeople().size());
        assertEquals(allocationHistory, state.getLastAllocatedSequences());
    }

    @Test
    public void constructor_customMutableImplementation_rejectsUntrustedRecord() {
        MutableRecord custom = new MutableRecord(new PersonId("S1"), createContactDetails("Alex Tan"));

        assertThrows(IllegalArgumentException.class, () ->
                new PeopleRegistryState(List.of(custom), createCounters(1, 0, 0)));
    }

    @Test
    public void equals_persistedRowsOrderAndCounters_comparesCompleteState() {
        Student student = createStudent("S1", "Alex Tan");
        Tutor tutor = new Tutor(new PersonId("T1"), createContactDetails("Bob Tan"));
        PeopleRegistryState state = new PeopleRegistryState(List.of(student, tutor), createCounters(2, 2, 1));
        PeopleRegistryState same = new PeopleRegistryState(
                List.of(createStudent("S1", "Alex Tan"),
                        new Tutor(new PersonId("T1"), createContactDetails("Bob Tan"))),
                createCounters(2, 2, 1));

        assertEquals(state, state);
        assertEquals(state, same);
        assertEquals(same, state);
        assertEquals(state.hashCode(), same.hashCode());
        assertNotEquals(state, new PeopleRegistryState(List.of(tutor, student), createCounters(2, 2, 1)));
        assertNotEquals(state, new PeopleRegistryState(List.of(student, tutor), createCounters(3, 2, 1)));
        assertNotEquals(state, new PeopleRegistryState(
                List.of(createStudent("S1", "Carol Tan"), tutor), createCounters(2, 2, 1)));
        assertNotEquals(state, null);
        assertNotEquals(state, state.getPeople());
    }

    private static Student createStudent(String id, String name) {
        return new Student(new PersonId(id), createContactDetails(name), new EducationLevel("S2"),
                new Phone("00012345"));
    }

    private static PersonRecord createPerson(PersonRole role, long sequence, String name) {
        PersonId id = PersonId.of(role, sequence);
        return switch (role) {
            case STUDENT -> createStudent(id.toString(), name);
            case TUTOR -> new Tutor(id, createContactDetails(name));
            case PARENT -> new Parent(id, createContactDetails(name));
        };
    }

    private static ContactDetails createContactDetails(String name) {
        return new ContactDetails(new Name(name), Optional.of(new Phone("00012345")),
                Optional.empty(), Optional.empty());
    }

    private static Map<PersonRole, Long> createCounters(long student, long tutor, long parent) {
        Map<PersonRole, Long> highWaterMarks = new EnumMap<>(PersonRole.class);
        highWaterMarks.put(PersonRole.STUDENT, student);
        highWaterMarks.put(PersonRole.TUTOR, tutor);
        highWaterMarks.put(PersonRole.PARENT, parent);
        return highWaterMarks;
    }

    private static class MutableRecord implements PersonRecord {

        private PersonId id;
        private ContactDetails contactDetails;

        MutableRecord(PersonId id, ContactDetails contactDetails) {
            this.id = id;
            this.contactDetails = contactDetails;
        }

        @Override
        public PersonId getId() {
            return id;
        }

        @Override
        public ContactDetails getContactDetails() {
            return contactDetails;
        }

        @Override
        public boolean isDuplicateOf(PersonRecord other) {
            return false;
        }
    }
}
