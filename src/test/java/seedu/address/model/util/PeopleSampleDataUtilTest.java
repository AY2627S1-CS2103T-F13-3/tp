package seedu.address.model.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.PersonIndexResolver;
import seedu.address.logic.parser.PeopleListParser;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.Name;
import seedu.address.model.person.Parent;
import seedu.address.model.person.PeopleRegistry;
import seedu.address.model.person.PeopleRegistryState;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Student;
import seedu.address.model.person.Tutor;
import seedu.address.ui.PersonRecordCardData;
import seedu.address.ui.PersonRecordListData;

public class PeopleSampleDataUtilTest {

    @Test
    public void getSamplePeopleRegistry_independentCalls_preservesDeterministicState() {
        PeopleRegistry first = PeopleSampleDataUtil.getSamplePeopleRegistry();
        PeopleRegistry second = PeopleSampleDataUtil.getSamplePeopleRegistry();
        PeopleRegistryState initialState = first.exportState();

        assertNotSame(first, second);
        assertEquals(initialState, second.exportState());
        assertEquals(List.of("T1", "S1", "P1", "S2", "T2", "S3"), getIds(first.getPeople()));
        assertEquals(Map.of(PersonRole.STUDENT, 3L, PersonRole.TUTOR, 2L, PersonRole.PARENT, 1L),
                initialState.getLastAllocatedSequences());

        first.remove(new PersonId("P1"), person -> false);
        assertNotEquals(initialState, first.exportState());
        assertEquals(initialState, second.exportState());
        assertEquals(initialState, PeopleSampleDataUtil.getSamplePeopleRegistry().exportState());
    }

    @Test
    public void getSamplePeopleRegistry_siblingsAndContacts_preservesRepresentativeValues() {
        PeopleRegistry people = PeopleSampleDataUtil.getSamplePeopleRegistry();
        Student alex = (Student) getPerson(people, "S1");
        Student jamie = (Student) getPerson(people, "S2");
        Parent parent = (Parent) getPerson(people, "P1");
        Tutor tutor = (Tutor) getPerson(people, "T1");

        assertEquals("P3", alex.getLevel().toString());
        assertEquals("S2", jamie.getLevel().toString());
        assertEquals("00987654", alex.getParentPhone().value);
        assertEquals(alex.getParentPhone(), jamie.getParentPhone());
        assertEquals(alex.getParentPhone(), parent.getPhone());
        assertTrue(alex.getContactDetails().getPhone().isEmpty());
        assertTrue(alex.getContactDetails().getEmail().isEmpty());
        assertTrue(alex.getContactDetails().getAddress().isEmpty());
        assertEquals("00012345", jamie.getContactDetails().getPhone().orElseThrow().value);
        assertEquals("Jamie.Tan@example.com", jamie.getContactDetails().getEmail().orElseThrow().value);
        assertEquals("8 Tampines Avenue 2", jamie.getContactDetails().getAddress().orElseThrow().value);
        assertEquals("00112233", tutor.getPhone().value);
        assertEquals("Mei.Lim@example.com", tutor.getContactDetails().getEmail().orElseThrow().value);
        assertTrue(parent.getContactDetails().getAddress().isEmpty());

        assertEquals(List.of("Level: P3", "Parent phone: 00987654", "Own phone: Not provided",
                "Email: Not provided", "Address: Not provided"),
                new PersonRecordCardData(alex, 2).getDetailLines());
        assertEquals(List.of("Phone: 00999888", "Email: Not provided", "Address: Not provided"),
                new PersonRecordCardData(getPerson(people, "T2"), 5).getDetailLines());
    }

    @Test
    public void getSamplePeopleRegistry_sameNames_retainsDistinctPhoneIdentities() {
        PeopleRegistry people = PeopleSampleDataUtil.getSamplePeopleRegistry();
        Tutor firstTutor = (Tutor) getPerson(people, "T1");
        Tutor secondTutor = (Tutor) getPerson(people, "T2");
        Student firstAlex = (Student) getPerson(people, "S1");
        Student secondAlex = (Student) getPerson(people, "S3");

        assertEquals(firstTutor.getName(), secondTutor.getName());
        assertNotEquals(firstTutor.getPhone(), secondTutor.getPhone());
        assertFalse(firstTutor.isDuplicateOf(secondTutor));
        assertTrue(secondTutor.getContactDetails().getEmail().isEmpty());
        assertTrue(secondTutor.getContactDetails().getAddress().isEmpty());
        assertEquals(firstAlex.getName(), secondAlex.getName());
        assertNotEquals(firstAlex.getParentPhone(), secondAlex.getParentPhone());
        assertFalse(firstAlex.isDuplicateOf(secondAlex));
    }

    @Test
    public void sample_exportImportAndRemoval_retainsSiblingsOrderAndAllocationHistory() {
        PeopleRegistry original = PeopleSampleDataUtil.getSamplePeopleRegistry();
        PeopleRegistry restored = new PeopleRegistry(original.exportState());
        PeopleRegistryState before = original.exportState();
        Parent parent = (Parent) getPerson(restored, "P1");
        Student jamie = (Student) getPerson(restored, "S2");

        restored.remove(parent.getId(), person -> false);
        assertEquals(parent.getPhone(), ((Student) getPerson(restored, "S1")).getParentPhone());
        assertEquals(parent.getPhone(), jamie.getParentPhone());
        restored.remove(jamie.getId(), person -> false);
        restored = new PeopleRegistry(restored.exportState());
        Student replacement = restored.addStudent(jamie.getContactDetails(), jamie.getLevel(), jamie.getParentPhone());
        Parent replacementParent = restored.addParent(parent.getContactDetails());
        Tutor extraTutor = restored.addTutor(new ContactDetails(new Name("Sam Lee"),
                Optional.of(new Phone("00777777")), Optional.empty(), Optional.empty()));

        assertEquals(new PersonId("S4"), replacement.getId());
        assertEquals(new PersonId("P2"), replacementParent.getId());
        assertEquals(new PersonId("T3"), extraTutor.getId());
        assertEquals(List.of("T1", "S1", "T2", "S3", "S4", "P2", "T3"), getIds(restored.getPeople()));
        assertEquals(before, original.exportState());
    }

    @Test
    public void sample_listArgumentsAndCards_resolvesCurrentViewRatherThanStableIdSuffix() throws Exception {
        PeopleRegistry people = PeopleSampleDataUtil.getSamplePeopleRegistry();
        PeopleRegistryState before = people.exportState();
        assertView(people, "", List.of("T1", "S1", "P1", "S2", "T2", "S3"));
        assertView(people, "r/STUDENT", List.of("S1", "S2", "S3"));
        assertView(people, "r/tutor", List.of("T1", "T2"));
        assertView(people, "r/parent", List.of("P1"));

        people.remove(new PersonId("S1"), person -> false);
        assertView(people, "r/student", List.of("S2", "S3"));
        assertEquals(before.getLastAllocatedSequences(), people.exportState().getLastAllocatedSequences());
    }

    private static PersonRecord getPerson(PeopleRegistry people, String id) {
        return people.getPerson(new PersonId(id)).orElseThrow();
    }

    private static List<String> getIds(List<PersonRecord> people) {
        return people.stream().map(person -> person.getId().toString()).toList();
    }

    private static void assertView(PeopleRegistry people, String arguments, List<String> expectedIds) throws Exception {
        PeopleRegistryState before = people.exportState();
        PersonRecordListData view = new PersonRecordListData(people.getPeople(),
                new PeopleListParser().parse(arguments));
        List<PersonRecord> visiblePeople = view.getEntries().stream().map(PersonRecordListData.Entry::person).toList();
        assertEquals(expectedIds, getIds(visiblePeople));
        for (int i = 0; i < expectedIds.size(); i++) {
            PersonRecordListData.Entry entry = view.getEntries().get(i);
            PersonRecord person = getPerson(people, expectedIds.get(i));
            assertEquals(i + 1, entry.displayedIndex());
            assertSame(person, PersonIndexResolver.resolve(Index.fromOneBased(i + 1), visiblePeople));
            if (person instanceof Student student) {
                assertSame(student, PersonIndexResolver.resolveStudent(Index.fromOneBased(i + 1), visiblePeople));
            }
            PersonRecordCardData card = new PersonRecordCardData(entry.person(), entry.displayedIndex());
            assertEquals((i + 1) + ". " + person.getName().fullName, card.getHeading());
            assertTrue(card.getIdentity().endsWith(" · " + expectedIds.get(i)));
        }
        assertEquals(before, people.exportState());
    }
}
