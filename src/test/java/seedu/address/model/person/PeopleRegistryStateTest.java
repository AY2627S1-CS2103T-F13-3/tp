package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.exceptions.DuplicatePersonException;

public class PeopleRegistryStateTest {

    @Test
    public void constructor_mutableInputsAndReturnedCollections_cannotChangeSnapshot() {
        Student student = student("S2", "Alex Tan");
        Tutor tutor = new Tutor(new PersonId("T1"), details("Bob Tan"));
        List<PersonRecord> rows = new ArrayList<>(List.of(student, tutor));
        Map<PersonRole, Long> highWaterMarks = counters(5, 2, 3);
        PeopleRegistryState state = new PeopleRegistryState(rows, highWaterMarks);

        rows.clear();
        highWaterMarks.put(PersonRole.STUDENT, 99L);

        assertEquals(List.of(student, tutor), state.getPeople());
        assertEquals(counters(5, 2, 3), state.getLastAllocatedSequences());
        assertThrows(UnsupportedOperationException.class, () -> state.getPeople().clear());
        assertThrows(UnsupportedOperationException.class, () ->
                state.getLastAllocatedSequences().put(PersonRole.TUTOR, 99L));
    }

    @Test
    public void constructor_invalidHighWaterMarks_rejectsIncompleteNegativeAndUnderstatedState() {
        Student student = student("S2", "Alex Tan");
        Map<PersonRole, Long> incomplete = counters(2, 0, 0);
        incomplete.remove(PersonRole.PARENT);

        assertThrows(IllegalArgumentException.class, () -> new PeopleRegistryState(List.of(student), incomplete));
        assertThrows(IllegalArgumentException.class, () ->
                new PeopleRegistryState(List.of(), counters(0, -1, 0)));
        assertThrows(IllegalArgumentException.class, () ->
                new PeopleRegistryState(List.of(student), counters(1, 0, 0)));

        Map<PersonRole, Long> nullCounter = counters(2, 0, 0);
        nullCounter.put(PersonRole.PARENT, null);
        assertThrows(NullPointerException.class, () -> new PeopleRegistryState(List.of(student), nullCounter));
    }

    @Test
    public void constructor_nullInputsAndRows_rejectsMissingRequiredState() {
        assertThrows(NullPointerException.class, () -> new PeopleRegistryState(null, counters(0, 0, 0)));
        assertThrows(NullPointerException.class, () -> new PeopleRegistryState(List.of(), null));
        List<PersonRecord> missingRow = new ArrayList<>();
        missingRow.add(null);
        assertThrows(NullPointerException.class, () -> new PeopleRegistryState(missingRow, counters(0, 0, 0)));
    }

    @Test
    public void constructor_duplicateIdsOrBusinessKeys_rejectsAmbiguousRecords() {
        Student first = student("S1", "Alex Tan");
        Student sameIdDifferentContact = student("S1", "Bob Tan");
        assertThrows(DuplicatePersonException.class, () -> new PeopleRegistryState(
                List.of(first, sameIdDifferentContact), counters(1, 0, 0)));

        for (PersonRole role : PersonRole.values()) {
            PersonRecord original = record(role, 1, "Alex Tan");
            PersonRecord duplicate = record(role, 2, "Alex  TAN ");
            assertThrows(DuplicatePersonException.class, () -> new PeopleRegistryState(
                    List.of(original, duplicate), counters(2, 2, 2)));
        }
    }

    @Test
    public void constructor_sameContactAcrossRoles_preservesDistinctRecords() {
        List<PersonRecord> records = List.of(record(PersonRole.PARENT, 1, "Alex Tan"),
                record(PersonRole.STUDENT, 1, "Alex Tan"), record(PersonRole.TUTOR, 1, "Alex Tan"));

        assertEquals(records, new PeopleRegistryState(records, counters(1, 1, 1)).getPeople());
    }

    @Test
    public void constructor_customMutableImplementation_rejectsUntrustedRecord() {
        MutableRecord custom = new MutableRecord(new PersonId("S1"), details("Alex Tan"));

        assertThrows(IllegalArgumentException.class, () ->
                new PeopleRegistryState(List.of(custom), counters(1, 0, 0)));
    }

    @Test
    public void equals_persistedRowsOrderAndCounters_comparesCompleteState() {
        Student student = student("S1", "Alex Tan");
        Tutor tutor = new Tutor(new PersonId("T1"), details("Bob Tan"));
        PeopleRegistryState state = new PeopleRegistryState(List.of(student, tutor), counters(2, 2, 1));
        PeopleRegistryState same = new PeopleRegistryState(
                List.of(student("S1", "Alex Tan"), new Tutor(new PersonId("T1"), details("Bob Tan"))),
                counters(2, 2, 1));

        assertEquals(state, state);
        assertEquals(state, same);
        assertEquals(same, state);
        assertEquals(state.hashCode(), same.hashCode());
        assertNotEquals(state, new PeopleRegistryState(List.of(tutor, student), counters(2, 2, 1)));
        assertNotEquals(state, new PeopleRegistryState(List.of(student, tutor), counters(3, 2, 1)));
        assertNotEquals(state, new PeopleRegistryState(
                List.of(student("S1", "Carol Tan"), tutor), counters(2, 2, 1)));
        assertNotEquals(state, null);
        assertNotEquals(state, state.getPeople());
    }

    private static Student student(String id, String name) {
        return new Student(new PersonId(id), details(name), new EducationLevel("S2"), new Phone("00012345"));
    }

    private static PersonRecord record(PersonRole role, long sequence, String name) {
        PersonId id = PersonId.of(role, sequence);
        return switch (role) {
            case STUDENT -> student(id.toString(), name);
            case TUTOR -> new Tutor(id, details(name));
            case PARENT -> new Parent(id, details(name));
        };
    }

    private static ContactDetails details(String name) {
        return new ContactDetails(new Name(name), Optional.of(new Phone("00012345")),
                Optional.empty(), Optional.empty());
    }

    private static Map<PersonRole, Long> counters(long student, long tutor, long parent) {
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
