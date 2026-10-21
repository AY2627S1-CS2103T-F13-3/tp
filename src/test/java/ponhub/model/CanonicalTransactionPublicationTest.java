package ponhub.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import ponhub.logic.CanonicalCommandExecutor;
import ponhub.logic.commands.CommandResult;
import ponhub.model.attendance.Attendance;
import ponhub.model.attendance.AttendanceStatus;
import ponhub.model.lesson.Lesson;
import ponhub.model.lesson.LessonDay;
import ponhub.model.lesson.LessonId;
import ponhub.model.lesson.LessonTime;
import ponhub.model.lesson.LessonTimeSlot;
import ponhub.model.lesson.Room;
import ponhub.model.lesson.Subject;
import ponhub.model.person.ContactDetails;
import ponhub.model.person.EducationLevel;
import ponhub.model.person.Name;
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

/**
 * Observer regressions for the save-before-publication and unchanged-view transaction contracts.
 */
public class CanonicalTransactionPublicationTest {
    @Test
    public void execute_changedAggregate_savesBeforeOneCompletePublication() throws Exception {
        PonHubDataState before = fixture(false);
        PonHubDataState candidate = fixture(true);
        PonHubData live = new PonHubData(before);
        PeopleView view = new PeopleView(live);
        view.setRoleFilter(Optional.of(PersonRole.STUDENT));
        PublicationProbe probe = new PublicationProbe(live, view);
        List<PersonRecord> expectedPeople = candidate.people().getPeople().subList(1, 3);

        CommandResult result = probe.executor.execute((staged, stagedView) -> {
            staged.resetData(candidate);
            return new CommandResult("Committed");
        });

        assertEquals("Committed", result.getFeedbackToUser());
        assertEquals(List.of(candidate), probe.savedStates);
        assertEquals(List.of(before), probe.statesAtSave);
        assertEquals(List.of(candidate), probe.invalidatedStates);
        assertEquals(List.of(new RootPublication(before, candidate, candidate, candidate)), probe.rootPublications);
        assertEquals(List.of(new ViewPublication(expectedPeople, candidate, candidate)), probe.viewPublications);
        assertEquals(List.of("save", "root", "people"), probe.events);
        assertSame(probe.heldPeople, view.getPeople());
        assertEquals(expectedPeople, probe.heldPeople);
        assertEquals(Optional.of(PersonRole.STUDENT), view.getRoleFilter());
        assertEquals(candidate, live.exportState());
    }

    @Test
    public void execute_equalRebuiltSnapshot_keepsRootAndHeldListSilent() throws Exception {
        PonHubDataState before = fixture(false);
        PonHubDataState rebuilt = fixture(false);
        assertEquals(before, rebuilt);
        assertNotSame(before, rebuilt);
        assertNotSame(before.people(), rebuilt.people());
        assertNotSame(before.people().getPeople().getFirst(), rebuilt.people().getPeople().getFirst());
        assertNotSame(before.lessons().getFirst(), rebuilt.lessons().getFirst());
        assertNotSame(before.attendance().getFirst(), rebuilt.attendance().getFirst());
        PonHubData live = new PonHubData(before);
        PeopleView view = new PeopleView(live);
        view.setRoleFilter(Optional.of(PersonRole.STUDENT));
        PublicationProbe probe = new PublicationProbe(live, view);

        probe.executor.execute((staged, stagedView) -> {
            staged.resetData(rebuilt);
            stagedView.setRoleFilter(Optional.of(PersonRole.STUDENT));
            return new CommandResult("Unchanged");
        });

        probe.assertRootSilent();
        assertTrue(probe.viewPublications.isEmpty());
        assertTrue(probe.events.isEmpty());
        assertSame(before, live.exportState());
        assertSame(probe.heldPeople, view.getPeople());
        assertEquals(before.people().getPeople().subList(1, 3), probe.heldPeople);
        assertEquals(Optional.of(PersonRole.STUDENT), view.getRoleFilter());
    }

    @Test
    public void execute_roleFilterOnly_refreshesHeldListAndSkipsIdenticalFilter() throws Exception {
        PonHubDataState before = fixture(false);
        PonHubData live = new PonHubData(before);
        PeopleView view = new PeopleView(live);
        PublicationProbe probe = new PublicationProbe(live, view);
        CanonicalCommandExecutor.StagedCommand listStudents = (staged, stagedView) -> {
            stagedView.setRoleFilter(Optional.of(PersonRole.STUDENT));
            return new CommandResult("Listed students");
        };

        probe.executor.execute(listStudents);
        assertEquals(List.of(new ViewPublication(before.people().getPeople().subList(1, 3), before, null)),
                probe.viewPublications);
        assertEquals(List.of("people"), probe.events);
        assertEquals(Optional.of(PersonRole.STUDENT), view.getRoleFilter());
        probe.executor.execute(listStudents);

        probe.assertRootSilent();
        assertEquals(1, probe.viewPublications.size());
        assertEquals(List.of("people"), probe.events);
        assertSame(before, live.exportState());
        assertSame(probe.heldPeople, view.getPeople());
        assertEquals(before.people().getPeople().subList(1, 3), probe.heldPeople);
    }

    @Test
    public void execute_searchFilterOnly_refreshesHeldListAndSkipsEqualCriteria() throws Exception {
        PonHubDataState before = fixture(false);
        PonHubData live = new PonHubData(before);
        PeopleView view = new PeopleView(live);
        view.setRoleFilter(Optional.of(PersonRole.STUDENT));
        PublicationProbe probe = new PublicationProbe(live, view);
        SearchCriteria criteria = searchAlex();

        probe.executor.execute((staged, stagedView) -> {
            stagedView.setSearchCriteria(criteria);
            return new CommandResult("Found Alex");
        });
        List<PersonRecord> expectedPeople = List.of(before.people().getPeople().get(1));
        assertEquals(List.of(new ViewPublication(expectedPeople, before, null)), probe.viewPublications);
        assertEquals(Optional.of(criteria), view.getSearchCriteria());
        SearchCriteria equalCriteria = searchAlex();
        assertEquals(criteria, equalCriteria);
        assertNotSame(criteria, equalCriteria);
        probe.executor.execute((staged, stagedView) -> {
            stagedView.setSearchCriteria(equalCriteria);
            return new CommandResult("Found Alex again");
        });

        probe.assertRootSilent();
        assertEquals(1, probe.viewPublications.size());
        assertEquals(List.of("people"), probe.events);
        assertEquals(Optional.of(criteria), view.getSearchCriteria());
        assertSame(before, live.exportState());
        assertSame(probe.heldPeople, view.getPeople());
        assertEquals(expectedPeople, probe.heldPeople);
    }

    @Test
    public void execute_clearSearchKeepingRole_refreshesHeldListWithoutPublishingRoot() throws Exception {
        PonHubDataState before = fixture(false);
        PonHubData live = new PonHubData(before);
        PeopleView view = new PeopleView(live);
        view.setSearchCriteria(searchAlex());
        PublicationProbe probe = new PublicationProbe(live, view);

        probe.executor.execute((staged, stagedView) -> {
            stagedView.setRoleFilter(Optional.of(PersonRole.STUDENT));
            return new CommandResult("Listed all students");
        });

        probe.assertRootSilent();
        assertEquals(List.of(new ViewPublication(before.people().getPeople().subList(1, 3), before, null)),
                probe.viewPublications);
        assertEquals(List.of("people"), probe.events);
        assertEquals(Optional.of(PersonRole.STUDENT), view.getRoleFilter());
        assertEquals(Optional.empty(), view.getSearchCriteria());
        assertSame(before, live.exportState());
        assertSame(probe.heldPeople, view.getPeople());
        assertEquals(before.people().getPeople().subList(1, 3), probe.heldPeople);
    }

    private static SearchCriteria searchAlex() {
        return new SearchCriteria(SearchCategory.STUDENT, Map.of(SearchField.NAME, "Alex"));
    }

    /**
     * Builds independent snapshots; the changed snapshot replaces a person, roster, attendance and both counters.
     */
    private static PonHubDataState fixture(boolean isChanged) {
        Tutor tutor = new Tutor(new PersonId("T2"), new ContactDetails(new Name("Tutor"),
                Optional.of(new Phone("92345678")), Optional.empty(), Optional.empty()));
        Student alex = new Student(new PersonId("S7"),
                new ContactDetails(new Name(isChanged ? "Alex Updated" : "Alex")),
                new EducationLevel("P1"), new Phone("91234567"));
        Student jamie = new Student(new PersonId("S8"), new ContactDetails(new Name("Jamie")),
                new EducationLevel("P2"), new Phone("93456789"));
        PeopleRegistryState people = new PeopleRegistryState(List.of(tutor, alex, jamie),
                Map.of(PersonRole.TUTOR, 2L, PersonRole.STUDENT, isChanged ? Long.MAX_VALUE : 8L,
                        PersonRole.PARENT, 0L));
        Lesson lesson = new Lesson(new LessonId("L3"), tutor.getId(), new LessonTimeSlot(LessonDay.MONDAY,
                new LessonTime("0900"), new LessonTime("1000")), new Subject("Math"), new Room("R1"),
                isChanged ? Set.of(alex.getId()) : Set.of(alex.getId(), jamie.getId()));
        Attendance attendance = new Attendance(jamie.getId(), lesson.getId(), LocalDate.of(2026, 10, 5),
                isChanged ? AttendanceStatus.ABSENT : AttendanceStatus.PRESENT);
        return new PonHubDataState(people, List.of(lesson), List.of(attendance), isChanged ? Long.MAX_VALUE : 3L);
    }

    /**
     * Captures immutable callback-time observations so assertions do not depend on listener exception handling.
     */
    private static final class PublicationProbe {
        private final ObservableList<PersonRecord> heldPeople;
        private final List<PonHubDataState> savedStates = new ArrayList<>();
        private final List<PonHubDataState> statesAtSave = new ArrayList<>();
        private final List<PonHubDataState> invalidatedStates = new ArrayList<>();
        private final List<RootPublication> rootPublications = new ArrayList<>();
        private final List<ViewPublication> viewPublications = new ArrayList<>();
        private final List<String> events = new ArrayList<>();
        private final CanonicalCommandExecutor executor;

        private PublicationProbe(PonHubData live, PeopleView view) {
            heldPeople = view.getPeople();
            live.stateProperty().addListener((InvalidationListener) observable ->
                    invalidatedStates.add(live.exportState()));
            live.stateProperty().addListener((observable, previous, current) -> {
                rootPublications.add(new RootPublication(previous, current, live.exportState(), lastSavedState()));
                events.add("root");
            });
            heldPeople.addListener((ListChangeListener<PersonRecord>) change -> {
                viewPublications.add(new ViewPublication(List.copyOf(change.getList()),
                        live.exportState(), lastSavedState()));
                events.add("people");
            });
            executor = new CanonicalCommandExecutor(view, state -> {
                statesAtSave.add(live.exportState());
                savedStates.add(state);
                events.add("save");
            }, Optional::empty);
        }

        private PonHubDataState lastSavedState() {
            return savedStates.isEmpty() ? null : savedStates.getLast();
        }

        private void assertRootSilent() {
            assertTrue(savedStates.isEmpty());
            assertTrue(invalidatedStates.isEmpty());
            assertTrue(rootPublications.isEmpty());
        }
    }

    private record RootPublication(PonHubDataState previous, PonHubDataState published,
            PonHubDataState live, PonHubDataState saved) {
    }

    private record ViewPublication(List<PersonRecord> people, PonHubDataState live, PonHubDataState saved) {
    }
}
