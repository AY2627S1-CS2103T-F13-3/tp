package ponhub.model.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ponhub.model.person.Address;
import ponhub.model.person.ContactDetails;
import ponhub.model.person.EducationLevel;
import ponhub.model.person.Email;
import ponhub.model.person.Name;
import ponhub.model.person.Parent;
import ponhub.model.person.PeopleRegistry;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.Phone;
import ponhub.model.person.Student;
import ponhub.model.person.Tutor;

public class PersonContactSearchTest {
    private PeopleRegistry registry;
    private Student alex;
    private Student jamie;
    private Parent parent;
    private Tutor tutor;

    @BeforeEach
    public void setUp() {
        registry = new PeopleRegistry();
        tutor = registry.addTutor(contact("Mei Lim", "0012345", "Mei@example.com", "1 Jurong Road"));
        alex = registry.addStudent(contact("Alex Tan", null, null, null),
                new EducationLevel("S2"), new Phone("0098765"));
        parent = registry.addParent(contact("Pat Tan", "0098765", null, "2 Jurong Road"));
        jamie = registry.addStudent(contact("Jamie Tan", "0001234", "Jamie@example.com", "3 Jurong Road"),
                new EducationLevel("S3"), new Phone("0098765"));
    }

    @Test
    public void categoryOnly_selectsEachRoleInGlobalOrder() {
        assertEquals(List.of(alex, jamie), search(SearchCategory.STUDENT, Map.of()));
        assertEquals(List.of(parent), search(SearchCategory.PARENT, Map.of()));
        assertEquals(List.of(tutor), search(SearchCategory.TUTOR, Map.of()));
    }

    @Test
    public void studentFields_applyTogetherWithExactPhonesAndLevel() {
        assertEquals(List.of(jamie), search(SearchCategory.STUDENT, Map.of(
                SearchField.NAME, "TaN", SearchField.LEVEL, "s3", SearchField.PHONE, "0001234",
                SearchField.PARENT_PHONE, "0098765", SearchField.EMAIL, "@EXAMPLE",
                SearchField.ADDRESS, "jurong")));
        assertEquals(List.of(), search(SearchCategory.STUDENT, Map.of(SearchField.PHONE, "0012345")));
        assertEquals(List.of(), search(SearchCategory.STUDENT, Map.of(SearchField.PARENT_PHONE, "098765")));
        assertEquals(List.of(), search(SearchCategory.STUDENT, Map.of(
                SearchField.NAME, "Alex", SearchField.LEVEL, "S3")));
        assertEquals(List.of(), search(SearchCategory.STUDENT, Map.of(
                SearchField.NAME, "Jamie", SearchField.PARENT_PHONE, "0001234")));
    }

    @Test
    public void optionalFields_absentOnlyFailsWhenFiltered() {
        assertEquals(List.of(alex), search(SearchCategory.STUDENT, Map.of(SearchField.NAME, "Alex")));
        assertEquals(List.of(), search(SearchCategory.STUDENT, Map.of(
                SearchField.NAME, "Alex", SearchField.PHONE, "0001234")));
        assertEquals(List.of(), search(SearchCategory.STUDENT, Map.of(
                SearchField.NAME, "Alex", SearchField.EMAIL, "@example")));
        assertEquals(List.of(), search(SearchCategory.STUDENT, Map.of(
                SearchField.NAME, "Alex", SearchField.ADDRESS, "Jurong")));
        assertEquals(List.of(), search(SearchCategory.PARENT, Map.of(SearchField.EMAIL, "@example")));
    }

    @Test
    public void parentAndTutor_useOwnContactsAndPreserveLeadingZeros() {
        assertEquals(List.of(parent), search(SearchCategory.PARENT, Map.of(
                SearchField.NAME, "pat", SearchField.PHONE, "0098765", SearchField.ADDRESS, "2 Jurong")));
        assertEquals(List.of(), search(SearchCategory.PARENT, Map.of(SearchField.PHONE, "098765")));
        assertEquals(List.of(tutor), search(SearchCategory.TUTOR, Map.of(
                SearchField.NAME, "mei", SearchField.PHONE, "0012345", SearchField.EMAIL, "@example",
                SearchField.ADDRESS, "jurong")));
        assertEquals(List.of(), search(SearchCategory.TUTOR, Map.of(SearchField.NAME, "Pat")));
    }

    @Test
    public void repeatedSpaceQuery_matchesNormalizedStoredNameAndAddress() {
        assertEquals(List.of(tutor), search(SearchCategory.TUTOR, Map.of(
                SearchField.NAME, "  mEi   lIm  ", SearchField.ADDRESS, " Jurong   Road ")));
    }

    @Test
    public void result_isImmutableSnapshotAndSearchDoesNotChangeRegistry() {
        List<PersonRecord> before = registry.getPeople();
        List<PersonRecord> result = search(SearchCategory.STUDENT, Map.of());
        assertThrows(UnsupportedOperationException.class, () -> result.remove(0));
        assertEquals(before, registry.getPeople());
        registry.addStudent(contact("New Student", null, null, null),
                new EducationLevel("P1"), new Phone("1234567"));
        assertEquals(List.of(alex, jamie), result);
        assertEquals(List.of(alex, jamie, registry.getPeople().get(4)),
                search(SearchCategory.STUDENT, Map.of()));
    }

    @Test
    public void invalidCategoryAndRelationshipFilters_rejectBeforeFiltering() {
        List<PersonRecord> people = new ArrayList<>(registry.getPeople());
        assertThrows(IllegalArgumentException.class, () -> PersonContactSearch.search(people,
                new SearchCriteria(SearchCategory.LESSON, Map.of())));
        SearchCriteria relationshipCriteria = new SearchCriteria(SearchCategory.STUDENT,
                Map.of(SearchField.DAY, "Mon"));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                PersonContactSearch.search(people, relationshipCriteria));
        assertTrue(exception.getMessage().contains("d/"));
        assertEquals(registry.getPeople(), people);
        assertThrows(NullPointerException.class, () -> PersonContactSearch.search(null,
                new SearchCriteria(SearchCategory.STUDENT, Map.of())));
        assertThrows(NullPointerException.class, () -> PersonContactSearch.search(people, null));
    }

    private List<PersonRecord> search(SearchCategory category, Map<SearchField, String> filters) {
        return PersonContactSearch.search(registry.getPeople(), new SearchCriteria(category, filters));
    }

    private static ContactDetails contact(String name, String phone, String email, String address) {
        return new ContactDetails(new Name(name), optional(phone).map(Phone::new),
                optional(email).map(Email::new), optional(address).map(Address::new));
    }

    private static Optional<String> optional(String value) {
        return Optional.ofNullable(value);
    }
}
