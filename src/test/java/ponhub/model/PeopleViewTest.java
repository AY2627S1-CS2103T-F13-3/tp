package ponhub.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import ponhub.commons.core.index.Index;
import ponhub.logic.PersonIndexResolver;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.PeopleListParser;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.person.ContactDetails;
import ponhub.model.person.EducationLevel;
import ponhub.model.person.Name;
import ponhub.model.person.Parent;
import ponhub.model.person.PeopleRegistryState;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.PersonRole;
import ponhub.model.person.Phone;
import ponhub.model.person.Student;
import ponhub.model.person.Tutor;
import ponhub.model.search.SearchCategory;
import ponhub.model.search.SearchCriteria;
import ponhub.model.search.SearchField;

public class PeopleViewTest {

    private final Tutor tutor = new Tutor(new PersonId("T7"), createContacts("Mei Lim"));
    private final Student firstStudent = createStudent("S9", "Alex Tan");
    private final Parent parent = new Parent(new PersonId("P2"), createContacts("Pat Tan"));
    private final Student secondStudent = createStudent("S3", "Jamie Tan");
    private final List<PersonRecord> originalPeople = List.of(tutor, firstStudent, parent, secondStudent);
    private final PonHubDataState originalState = createState(originalPeople);

    @Test
    public void constructor_allRoles_preservesCreationOrderWithoutChangingRoot() {
        PonHubData source = new PonHubData(originalState);
        PeopleView view = new PeopleView(source);

        assertEquals(originalPeople, view.getPeople());
        assertEquals(Optional.empty(), view.getRoleFilter());
        assertSame(originalState, source.exportState());
    }

    @Test
    public void setRoleFilter_eachRole_preservesRelativeOrderAndCurrentIndexSource() throws CommandException {
        PonHubData source = new PonHubData(originalState);
        PeopleView view = new PeopleView(source);

        view.setRoleFilter(Optional.of(PersonRole.STUDENT));
        assertEquals(List.of(firstStudent, secondStudent), view.getPeople());
        assertEquals(Optional.of(PersonRole.STUDENT), view.getRoleFilter());
        assertSame(secondStudent, PersonIndexResolver.resolveStudent(Index.fromOneBased(2), view.getPeople()));
        assertEquals(new PersonId("S3"), view.getPeople().get(1).getId());

        view.setRoleFilter(Optional.of(PersonRole.TUTOR));
        assertEquals(List.of(tutor), view.getPeople());
        assertSame(tutor, PersonIndexResolver.resolve(Index.fromOneBased(1), view.getPeople()));
        assertThrows(CommandException.class, () ->
                PersonIndexResolver.resolveStudent(Index.fromOneBased(1), view.getPeople()));

        view.setRoleFilter(Optional.of(PersonRole.PARENT));
        assertEquals(List.of(parent), view.getPeople());
        view.setRoleFilter(Optional.empty());
        assertEquals(originalPeople, view.getPeople());
        assertSame(originalState, source.exportState());
    }

    @Test
    public void getPeople_filterAndRefresh_keepObservableListAndSubscribers() {
        PonHubData source = new PonHubData(originalState);
        PeopleView view = new PeopleView(source);
        ObservableList<PersonRecord> subscribedPeople = view.getPeople();
        List<List<PersonRecord>> observedPeople = new ArrayList<>();
        subscribedPeople.addListener((ListChangeListener<PersonRecord>) change ->
                observedPeople.add(List.copyOf(change.getList())));

        view.setRoleFilter(Optional.of(PersonRole.STUDENT));
        assertSame(subscribedPeople, view.getPeople());
        assertEquals(List.of(firstStudent, secondStudent), observedPeople.getLast());

        source.resetData(createState(List.of(parent, secondStudent)));
        view.refresh();
        assertSame(subscribedPeople, view.getPeople());
        assertEquals(List.of(secondStudent), observedPeople.getLast());

        source.resetData(originalState);
        view.refresh();
        assertSame(subscribedPeople, view.getPeople());
        assertEquals(List.of(firstStudent, secondStudent), observedPeople.getLast());
    }

    @Test
    public void refresh_replacementAndRollback_preserveFilterAndRenumberCurrentSelection() throws CommandException {
        PonHubData source = new PonHubData(originalState);
        PeopleView view = new PeopleView(source);
        view.setRoleFilter(Optional.of(PersonRole.STUDENT));
        PersonRecord resolvedBeforeChange = PersonIndexResolver.resolve(Index.fromOneBased(2), view.getPeople());

        source.resetData(createState(List.of(tutor, parent, secondStudent)));
        assertEquals(List.of(firstStudent, secondStudent), view.getPeople());
        view.refresh();
        assertEquals(List.of(secondStudent), view.getPeople());
        assertSame(secondStudent, PersonIndexResolver.resolve(Index.fromOneBased(1), view.getPeople()));
        assertSame(secondStudent, resolvedBeforeChange);
        assertEquals(Optional.of(PersonRole.STUDENT), view.getRoleFilter());

        source.resetData(originalState);
        view.refresh();
        assertEquals(List.of(firstStudent, secondStudent), view.getPeople());
        assertSame(secondStudent, PersonIndexResolver.resolve(Index.fromOneBased(2), view.getPeople()));
        assertEquals(Optional.of(PersonRole.STUDENT), view.getRoleFilter());
        assertSame(originalState, source.exportState());
    }

    @Test
    public void setRoleFilter_emptyRootOrNoMatches_succeedsWithEmptyView() {
        PeopleView empty = new PeopleView(new PonHubData());
        assertTrue(empty.getPeople().isEmpty());
        for (PersonRole role : PersonRole.values()) {
            empty.setRoleFilter(Optional.of(role));
            assertTrue(empty.getPeople().isEmpty());
            assertEquals(Optional.of(role), empty.getRoleFilter());
        }

        PonHubData source = new PonHubData(createState(List.of(tutor, parent)));
        PeopleView view = new PeopleView(source);
        view.setRoleFilter(Optional.of(PersonRole.STUDENT));
        assertTrue(view.getPeople().isEmpty());
        assertEquals(2, source.getPeople().size());
    }

    @Test
    public void getPeople_attemptedMutation_cannotChangeViewOrRoot() {
        PonHubData source = new PonHubData(originalState);
        PeopleView view = new PeopleView(source);

        assertThrows(UnsupportedOperationException.class, () -> view.getPeople().clear());
        assertThrows(UnsupportedOperationException.class, () -> view.getPeople().add(parent));
        assertThrows(UnsupportedOperationException.class, () -> view.getPeople().set(0, parent));
        assertEquals(originalPeople, view.getPeople());
        assertSame(originalState, source.exportState());
    }

    @Test
    public void constructorOrSetRoleFilter_null_rejectsWithoutChangingViewOrRoot() {
        assertThrows(NullPointerException.class, () -> new PeopleView(null));
        PonHubData source = new PonHubData(originalState);
        PeopleView view = new PeopleView(source);
        view.setRoleFilter(Optional.of(PersonRole.PARENT));

        assertThrows(NullPointerException.class, () -> view.setRoleFilter(null));
        assertEquals(Optional.of(PersonRole.PARENT), view.getRoleFilter());
        assertEquals(List.of(parent), view.getPeople());
        assertSame(originalState, source.exportState());
    }

    @Test
    public void parsedRoleSelection_validAndInvalidArguments_preserveOperationalData() throws ParseException {
        PonHubData source = new PonHubData(originalState);
        PeopleView view = new PeopleView(source);
        PeopleListParser parser = new PeopleListParser();

        view.setRoleFilter(parser.parse(" r/STUDENT "));
        assertEquals(List.of(firstStudent, secondStudent), view.getPeople());
        for (String invalid : List.of("r/", "r/all", "r/student r/parent", "n/Alex")) {
            assertThrows(ParseException.class, () -> view.setRoleFilter(parser.parse(invalid)));
            assertEquals(Optional.of(PersonRole.STUDENT), view.getRoleFilter());
            assertEquals(List.of(firstStudent, secondStudent), view.getPeople());
        }
        view.setRoleFilter(parser.parse(""));
        assertEquals(originalPeople, view.getPeople());
        assertSame(originalState, source.exportState());
    }

    @Test
    public void setSearchCriteria_eachPersonCategory_setsTypedRoleAndPreservesCurrentList() {
        PonHubData source = new PonHubData(originalState);
        PeopleView view = new PeopleView(source);
        var people = view.getPeople();
        Map<SearchCategory, PersonRole> roles = Map.of(SearchCategory.STUDENT, PersonRole.STUDENT,
                SearchCategory.PARENT, PersonRole.PARENT, SearchCategory.TUTOR, PersonRole.TUTOR);
        roles.forEach((category, role) -> {
            view.setSearchCriteria(new SearchCriteria(category, Map.of()));
            assertEquals(Optional.of(role), view.getRoleFilter());
            assertEquals(originalPeople.stream().filter(person -> person.getRole() == role).toList(), people);
            assertSame(people, view.getPeople());
        });
        assertSame(originalState, source.exportState());
    }

    @Test
    public void setSearchCriteria_lessonCategory_rejectsBeforeChangingPeopleFilterOrContext() {
        PonHubData source = new PonHubData(originalState);
        PeopleView view = new PeopleView(source);
        SearchCriteria criteria = new SearchCriteria(SearchCategory.STUDENT, Map.of(SearchField.NAME, "Alex"));
        view.setSearchCriteria(criteria);
        var context = view.searchContextProperty().get();

        for (Map<SearchField, String> filters : List.of(Map.<SearchField, String>of(),
                Map.of(SearchField.DAY, "Mon"))) {
            assertThrows(IllegalArgumentException.class, () ->
                    view.setSearchCriteria(new SearchCriteria(SearchCategory.LESSON, filters)));
            assertEquals(List.of(firstStudent), view.getPeople());
            assertEquals(Optional.of(PersonRole.STUDENT), view.getRoleFilter());
            assertSame(criteria, view.getSearchCriteria().orElseThrow());
            assertSame(context, view.searchContextProperty().get());
            assertSame(originalState, source.exportState());
        }
    }

    private static PonHubDataState createState(List<PersonRecord> people) {
        PeopleRegistryState state = new PeopleRegistryState(people,
                Map.of(PersonRole.STUDENT, 9L, PersonRole.TUTOR, 7L, PersonRole.PARENT, 2L));
        return new PonHubDataState(state, List.of(), List.of(), 0);
    }

    private static Student createStudent(String id, String name) {
        return new Student(new PersonId(id), new ContactDetails(new Name(name)),
                new EducationLevel("S2"), new Phone("00123456"));
    }

    private static ContactDetails createContacts(String name) {
        return new ContactDetails(new Name(name), Optional.of(new Phone("00123456")),
                Optional.empty(), Optional.empty());
    }
}
