package ponhub.model.util;

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

import ponhub.commons.core.index.Index;
import ponhub.logic.PersonIndexResolver;
import ponhub.logic.parser.PeopleListParser;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.PonHubDataState;
import ponhub.model.person.ContactDetails;
import ponhub.model.person.Name;
import ponhub.model.person.Parent;
import ponhub.model.person.PeopleRegistry;
import ponhub.model.person.PeopleRegistryState;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.PersonRole;
import ponhub.model.person.Phone;
import ponhub.model.person.Student;
import ponhub.model.person.Tutor;
import ponhub.testutil.PeopleSampleDataUtil;
import ponhub.ui.PersonRecordCardData;

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
        PonHubData source = new PonHubData(new PonHubDataState(before, List.of(), List.of(), 0));
        PeopleView view = new PeopleView(source);
        view.setRoleFilter(new PeopleListParser().parse(arguments));
        List<PersonRecord> visiblePeople = view.getPeople();
        assertEquals(expectedIds, getIds(visiblePeople));
        for (int i = 0; i < expectedIds.size(); i++) {
            PersonRecord person = getPerson(people, expectedIds.get(i));
            assertSame(person, PersonIndexResolver.resolve(Index.fromOneBased(i + 1), visiblePeople));
            if (person instanceof Student student) {
                assertSame(student, PersonIndexResolver.resolveStudent(Index.fromOneBased(i + 1), visiblePeople));
            }
            PersonRecordCardData card = new PersonRecordCardData(visiblePeople.get(i), i + 1);
            assertEquals((i + 1) + ". " + person.getName().fullName, card.getHeading());
            assertTrue(card.getIdentity().endsWith(" · " + expectedIds.get(i)));
        }
        assertSame(before, source.exportState().people());
        assertEquals(before, people.exportState());
    }
}
