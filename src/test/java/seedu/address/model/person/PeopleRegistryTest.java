package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.exceptions.DuplicatePersonException;
import seedu.address.model.person.exceptions.PersonIdExhaustedException;
import seedu.address.model.person.exceptions.PersonNotFoundException;
import seedu.address.model.person.exceptions.ReferencedPersonException;

public class PeopleRegistryTest {

    @Test
    public void add_mixedRoles_preservesGlobalOrderAndAllocatesPerRole() {
        PeopleRegistry registry = new PeopleRegistry();
        assertTrue(registry.getPeople().isEmpty());
        assertEquals(counters(0, 0, 0), registry.exportState().getLastAllocatedSequences());

        Tutor tutor = registry.addTutor(details("Alex Tan", "00012345"));
        Student student = registry.addStudent(details("Bob Tan", null), new EducationLevel("P1"),
                new Phone("00987654"));
        Parent parent = registry.addParent(details("Carol Tan", "00987654"));
        Student sibling = registry.addStudent(details("Dan Tan", null), new EducationLevel("S2"),
                new Phone("00987654"));
        Tutor secondTutor = registry.addTutor(details("Eve Tan", "00123456"));

        assertEquals(List.of(tutor, student, parent, sibling, secondTutor), registry.getPeople());
        List<PersonId> ids = registry.getPeople().stream().map(PersonRecord::getId).toList();
        assertEquals(List.of(new PersonId("T1"), new PersonId("S1"), new PersonId("P1"),
                new PersonId("S2"), new PersonId("T2")), ids);
        assertEquals(counters(2, 2, 1), registry.exportState().getLastAllocatedSequences());
        for (PersonRecord record : registry.getPeople()) {
            assertEquals(Optional.of(record), registry.getPerson(record.getId()));
        }
        assertEquals(Optional.of(student), registry.getPerson(new PersonId(" s1 ")));
        assertTrue(registry.getPerson(new PersonId("S99")).isEmpty());
    }

    @Test
    public void add_businessDuplicates_rejectsOptionalChangesWithoutConsumingIds() {
        PeopleRegistry registry = new PeopleRegistry();
        registry.addStudent(details("Alex Tan", null), new EducationLevel("P1"), new Phone("00012345"));
        registry.addTutor(details("Alex Tan", "00012345"));
        registry.addParent(details("Alex Tan", "00012345"));
        PeopleRegistryState before = registry.exportState();
        ContactDetails changedOptionalFields = new ContactDetails(new Name("Alex  TAN "),
                Optional.of(new Phone("00012345")), Optional.of(new Email("alex@example.com")),
                Optional.of(new Address("12 Main Street")));

        assertThrows(DuplicatePersonException.class, () -> registry.addStudent(changedOptionalFields,
                new EducationLevel("JC2"), new Phone("00012345")));
        assertThrows(DuplicatePersonException.class, () -> registry.addTutor(changedOptionalFields));
        assertThrows(DuplicatePersonException.class, () -> registry.addParent(changedOptionalFields));
        assertEquals(before, registry.exportState());

        Student sibling = registry.addStudent(details("Bob Tan", null), new EducationLevel("P1"),
                new Phone("00012345"));
        Student otherParentPhone = registry.addStudent(details("Alex Tan", null), new EducationLevel("P1"),
                new Phone("0012345"));
        Tutor otherTutorPhone = registry.addTutor(details("Alex Tan", "0012345"));
        Parent otherParent = registry.addParent(details("Alex Tan", "0012345"));

        assertEquals(new PersonId("S2"), sibling.getId());
        assertEquals(new PersonId("S3"), otherParentPhone.getId());
        assertEquals(new PersonId("T2"), otherTutorPhone.getId());
        assertEquals(new PersonId("P2"), otherParent.getId());
    }

    @Test
    public void add_invalidRequiredFields_leavesRecordsAndCountersUnchanged() {
        PeopleRegistry registry = new PeopleRegistry();
        PeopleRegistryState before = registry.exportState();
        ContactDetails noPhone = details("Alex Tan", null);

        assertThrows(IllegalArgumentException.class, () -> registry.addTutor(noPhone));
        assertThrows(IllegalArgumentException.class, () -> registry.addParent(noPhone));
        assertThrows(IllegalArgumentException.class, () -> registry.addStudent(noPhone,
                new EducationLevel("S2"), new Phone("1234567890123456")));
        assertThrows(NullPointerException.class, () -> registry.addStudent(null,
                new EducationLevel("S2"), new Phone("00012345")));
        assertThrows(NullPointerException.class, () -> registry.addStudent(noPhone, null, new Phone("00012345")));
        assertThrows(NullPointerException.class, () -> registry.addStudent(noPhone, new EducationLevel("S2"), null));
        assertThrows(NullPointerException.class, () -> registry.addTutor(null));
        assertThrows(NullPointerException.class, () -> registry.addParent(null));
        assertEquals(before, registry.exportState());

        assertEquals(new PersonId("S1"), registry.addStudent(noPhone,
                new EducationLevel("S2"), new Phone("00012345")).getId());
        assertEquals(new PersonId("T1"), registry.addTutor(details("Alex Tan", "00012345")).getId());
        assertEquals(new PersonId("P1"), registry.addParent(details("Alex Tan", "00012345")).getId());
    }

    @Test
    public void remove_referencesAndCheckerFailure_preservesCompleteState() {
        PeopleRegistry registry = new PeopleRegistry();
        Tutor tutor = registry.addTutor(details("Alex Tan", "00012345"));
        Parent parent = registry.addParent(details("Bob Tan", "00987654"));
        PeopleRegistryState before = registry.exportState();

        assertThrows(ReferencedPersonException.class, () -> registry.remove(tutor.getId(), record -> {
            assertEquals(tutor, record);
            return true;
        }));
        IllegalStateException checkerFailure = new IllegalStateException("References are unavailable");
        assertEquals(checkerFailure, assertThrows(IllegalStateException.class, () ->
                registry.remove(parent.getId(), record -> {
                    throw checkerFailure;
                })));
        assertThrows(PersonNotFoundException.class, () -> registry.remove(new PersonId("P99"), record -> {
            throw new AssertionError("The checker should only receive an existing record");
        }));
        assertThrows(NullPointerException.class, () -> registry.remove(null, record -> false));
        assertThrows(NullPointerException.class, () -> registry.remove(tutor.getId(), null));
        assertThrows(NullPointerException.class, () -> registry.getPerson(null));
        assertEquals(before, registry.exportState());
    }

    @Test
    public void remove_existingRecord_retainsOrderAndNeverReusesCommittedId() {
        PeopleRegistry registry = new PeopleRegistry();
        Tutor first = registry.addTutor(details("Alex Tan", "00012345"));
        Parent parent = registry.addParent(details("Bob Tan", "00987654"));
        Tutor second = registry.addTutor(details("Carol Tan", "00123456"));

        assertEquals(first, registry.remove(first.getId(), record -> false));
        assertEquals(List.of(parent, second), registry.getPeople());
        assertTrue(registry.getPerson(first.getId()).isEmpty());
        PeopleRegistry restored = new PeopleRegistry(registry.exportState());
        Tutor replacement = restored.addTutor(first.getContactDetails());

        assertEquals(new PersonId("T3"), replacement.getId());
        assertEquals(List.of(parent, second, replacement), restored.getPeople());
        assertEquals(List.of(parent, second), registry.getPeople());
    }

    @Test
    public void snapshotsAndCopies_laterMutations_preservesIndependentState() {
        PeopleRegistry original = new PeopleRegistry();
        Tutor tutor = original.addTutor(details("Alex Tan", "00012345"));
        Student student = original.addStudent(details("Bob Tan", null), new EducationLevel("S2"),
                new Phone("00987654"));
        List<PersonRecord> snapshot = original.getPeople();
        PeopleRegistryState exported = original.exportState();
        PeopleRegistry copied = new PeopleRegistry(original);
        PeopleRegistry imported = new PeopleRegistry(exported);

        assertEquals(original, copied);
        assertEquals(original, imported);
        assertEquals(original.hashCode(), copied.hashCode());
        assertEquals(exported, copied.exportState());
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        assertNotEquals(original, new PeopleRegistry(new PeopleRegistryState(
                List.of(student, tutor), exported.getLastAllocatedSequences())));

        copied.remove(tutor.getId(), record -> false);
        original.addParent(details("Carol Tan", "00123456"));
        imported.addTutor(details("Dan Tan", "00111111"));

        assertEquals(List.of(tutor, student), snapshot);
        assertEquals(List.of(tutor, student), exported.getPeople());
        assertEquals(List.of(student), copied.getPeople());
        assertEquals(3, original.getPeople().size());
        assertEquals(new PersonId("T2"), imported.getPeople().get(2).getId());
        assertNotEquals(original, copied);
        assertNotEquals(original, imported);
    }

    @Test
    public void import_deletedIdsAndArbitraryRowOrder_preservesAllocationAndGlobalOrder() {
        Student laterId = new Student(new PersonId("S8"), details("Alex Tan", null),
                new EducationLevel("S2"), new Phone("00012345"));
        Tutor tutor = new Tutor(new PersonId("T2"), details("Bob Tan", "00987654"));
        Student earlierId = new Student(new PersonId("S2"), details("Carol Tan", null),
                new EducationLevel("P1"), new Phone("00012345"));
        PeopleRegistry registry = new PeopleRegistry(new PeopleRegistryState(
                List.of(laterId, tutor, earlierId), counters(10, 4, 7)));

        assertEquals(List.of(laterId, tutor, earlierId), registry.getPeople());
        assertEquals(new PersonId("S11"), registry.addStudent(details("Dan Tan", null),
                new EducationLevel("S2"), new Phone("00012345")).getId());
        assertEquals(new PersonId("T5"), registry.addTutor(details("Eve Tan", "00123456")).getId());
        assertEquals(new PersonId("P8"), registry.addParent(details("Frank Tan", "00234567")).getId());
        assertEquals(counters(11, 5, 8), registry.exportState().getLastAllocatedSequences());
    }

    @Test
    public void allocation_lastLongIdAndDeletion_preservesExhaustionAcrossImports() {
        for (PersonRole role : PersonRole.values()) {
            Map<PersonRole, Long> highWaterMarks = counters(0, 0, 0);
            highWaterMarks.put(role, Long.MAX_VALUE - 1);
            PeopleRegistry registry = new PeopleRegistry(new PeopleRegistryState(List.of(), highWaterMarks));
            PersonRecord last = addRecord(registry, role);
            assertEquals(PersonId.of(role, Long.MAX_VALUE), last.getId());

            registry.remove(last.getId(), record -> false);
            PeopleRegistry restored = new PeopleRegistry(registry.exportState());
            PeopleRegistryState before = restored.exportState();
            assertThrows(PersonIdExhaustedException.class, () -> addRecord(restored, role));
            assertEquals(before, restored.exportState());
            assertTrue(restored.getPeople().isEmpty());
            assertEquals(Long.MAX_VALUE, restored.exportState().getLastAllocatedSequences().get(role));

            PersonRole otherRole = role == PersonRole.STUDENT ? PersonRole.TUTOR : PersonRole.STUDENT;
            assertEquals(PersonId.of(otherRole, 1), addRecord(restored, otherRole).getId());
        }
    }

    @Test
    public void equality_samePeopleDifferentAllocationState_distinguishesDeletedIdHistory() {
        PeopleRegistry original = new PeopleRegistry();
        original.addTutor(details("Alex Tan", "00012345"));
        PeopleRegistry same = new PeopleRegistry(original.exportState());
        PeopleRegistry differentCounters = new PeopleRegistry(new PeopleRegistryState(
                original.getPeople(), counters(0, 2, 0)));

        assertEquals(original, original);
        assertEquals(original, same);
        assertEquals(same, original);
        assertEquals(original.hashCode(), same.hashCode());
        assertNotEquals(original, differentCounters);
        assertNotEquals(original, null);
        assertNotEquals(original, original.exportState());
        assertThrows(NullPointerException.class, () -> new PeopleRegistry((PeopleRegistry) null));
        assertThrows(NullPointerException.class, () -> new PeopleRegistry((PeopleRegistryState) null));
    }

    private static PersonRecord addRecord(PeopleRegistry registry, PersonRole role) {
        ContactDetails contactDetails = details("Alex Tan", "00012345");
        return switch (role) {
            case STUDENT -> registry.addStudent(contactDetails, new EducationLevel("S2"), new Phone("00987654"));
            case TUTOR -> registry.addTutor(contactDetails);
            case PARENT -> registry.addParent(contactDetails);
        };
    }

    private static ContactDetails details(String name, String phone) {
        return new ContactDetails(new Name(name), Optional.ofNullable(phone).map(Phone::new),
                Optional.empty(), Optional.empty());
    }

    private static Map<PersonRole, Long> counters(long student, long tutor, long parent) {
        Map<PersonRole, Long> highWaterMarks = new EnumMap<>(PersonRole.class);
        highWaterMarks.put(PersonRole.STUDENT, student);
        highWaterMarks.put(PersonRole.TUTOR, tutor);
        highWaterMarks.put(PersonRole.PARENT, parent);
        return highWaterMarks;
    }
}
