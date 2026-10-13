package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.LessonQueryTestData.createData;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.collections.ObservableList;
import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.SearchCriteriaParser;
import seedu.address.model.PeopleView;
import seedu.address.model.PonHubData;
import seedu.address.model.PonHubDataState;
import seedu.address.model.attendance.Attendance;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRole;
import seedu.address.model.query.LessonQueries;
import seedu.address.model.query.LessonView;
import seedu.address.model.search.SearchCategory;
import seedu.address.model.search.SearchCriteria;
import seedu.address.model.search.SearchField;

public class LessonRetrievalTest {
    private PonHubData data;
    private PeopleView peopleView;
    private LessonQueries queries;
    private LessonRetrieval retrieval;

    @BeforeEach
    public void setUp() {
        data = createData();
        peopleView = new PeopleView(data);
        queries = new LessonQueries(data);
        retrieval = new LessonRetrieval(queries, peopleView);
    }

    @Test
    public void list_fullAndCurrentViewStudentResultsUseStableIdentity() throws CommandException {
        PonHubDataState before = data.exportState();
        assertIds(retrieval.list(Optional.empty()), "L2", "L10", "L3", "L4", "L11");
        assertIds(retrieval.list(Optional.of(Index.fromOneBased(2))), "L2", "L10");
        assertIds(retrieval.list(Optional.of(Index.fromOneBased(4))), "L10", "L11");
        assertTrue(retrieval.list(Optional.of(Index.fromOneBased(6))).isEmpty());
        peopleView.setRoleFilter(Optional.of(PersonRole.STUDENT));
        assertIds(retrieval.list(Optional.of(Index.fromOneBased(1))), "L2", "L10");
        assertIds(retrieval.list(Optional.of(Index.fromOneBased(2))), "L10", "L11");
        assertEquals(Optional.of(PersonRole.STUDENT), peopleView.getRoleFilter());
        assertSame(before, data.exportState());
    }

    @Test
    public void list_rejectsWrongRoleAndOutOfRangeWithoutChangingViews() throws CommandException {
        ObservableList<LessonView> catalogue = retrieval.list(Optional.empty());
        var people = peopleView.getPeople();
        assertThrows(CommandException.class, () -> retrieval.list(Optional.of(Index.fromOneBased(1))));
        assertThrows(CommandException.class, () -> retrieval.list(Optional.of(Index.fromOneBased(3))));
        assertThrows(CommandException.class, () -> retrieval.list(Optional.of(Index.fromOneBased(8))));
        assertSame(people, peopleView.getPeople());
        assertSame(catalogue, retrieval.list(Optional.empty()));
        assertIds(catalogue, "L2", "L10", "L3", "L4", "L11");
    }

    @Test
    public void search_filtersSameLessonAndPreservesCurrentPeopleView() throws Exception {
        peopleView.setRoleFilter(Optional.of(PersonRole.STUDENT));
        var people = peopleView.getPeople();
        var before = List.copyOf(people);
        PonHubDataState state = data.exportState();
        SearchCriteria criteria = new SearchCriteriaParser().parse("c/lesson d/Mon s/Math sn/Alex");
        ObservableList<LessonView> filtered = retrieval.search(criteria);
        assertSame(queries.getFilteredLessonList(), filtered);
        assertIds(filtered, "L10");
        assertEquals(before, people);
        assertSame(people, peopleView.getPeople());
        assertIds(retrieval.list(Optional.of(Index.fromOneBased(2))), "L10", "L11");
        assertSame(state, data.exportState());
        assertThrows(UnsupportedOperationException.class, () -> filtered.remove(0));

        assertSame(filtered, retrieval.search(new SearchCriteria(SearchCategory.LESSON,
                Map.of(SearchField.STUDENT_NAME, "Nobody"))));
        assertTrue(filtered.isEmpty());
        assertIds(retrieval.search(new SearchCriteria(SearchCategory.LESSON, Map.of())),
                "L2", "L10", "L3", "L4", "L11");
    }

    @Test
    public void heldResults_refreshAfterPublicationAndRollback() throws CommandException {
        PonHubDataState before = data.exportState();
        var catalogue = retrieval.list(Optional.empty());
        var student = retrieval.list(Optional.of(Index.fromOneBased(2)));
        var filtered = retrieval.search(new SearchCriteria(SearchCategory.LESSON,
                Map.of(SearchField.SUBJECT, "Math")));
        var people = peopleView.getPeople();
        data.resetData(new PonHubDataState(before.people(), List.of(), List.of(),
                before.lastAllocatedLessonSequence()));
        assertTrue(catalogue.isEmpty());
        assertTrue(student.isEmpty());
        assertTrue(filtered.isEmpty());
        data.resetData(before);
        assertIds(catalogue, "L2", "L10", "L3", "L4", "L11");
        assertIds(student, "L2", "L10");
        assertIds(filtered, "L10", "L4");
        assertSame(people, peopleView.getPeople());
    }

    @Test
    public void invalidArguments_leaveExistingResultsIntact() throws CommandException {
        assertThrows(NullPointerException.class, () -> new LessonRetrieval(null, peopleView));
        assertThrows(NullPointerException.class, () -> new LessonRetrieval(queries, null));
        assertThrows(NullPointerException.class, () -> retrieval.list(null));
        assertThrows(NullPointerException.class, () -> retrieval.search(null));
        SearchCriteria valid = new SearchCriteria(SearchCategory.LESSON, Map.of(SearchField.DAY, "Mon"));
        var filtered = retrieval.search(valid);
        assertThrows(IllegalArgumentException.class, () -> retrieval.search(
                new SearchCriteria(SearchCategory.STUDENT, Map.of())));
        assertSame(valid, queries.getLessonFilter());
        assertIds(filtered, "L10", "L3", "L4");
    }

    private static void assertIds(List<LessonView> lessons, String... expected) {
        assertEquals(List.of(expected), lessons.stream().map(lesson -> lesson.getLessonId().toString()).toList());
    }

    @Test
    public void context_identifiesSelectedStudentOrFiltersAndInvalidRequestsPreserveIt() throws Exception {
        var context = retrieval.contextProperty();
        assertEquals("All lessons", context.get());
        retrieval.list(Optional.of(Index.fromOneBased(4)));
        assertEquals("Student S2 · current lessons", context.get());
        peopleView.setRoleFilter(Optional.of(PersonRole.TUTOR));
        assertThrows(CommandException.class, () -> retrieval.list(Optional.of(Index.fromOneBased(1))));
        assertEquals("Student S2 · current lessons", context.get());
        retrieval.search(new SearchCriteriaParser().parse("c/lesson tu/MEI d/mon s/Math"));
        assertEquals("Lesson search: d/mon, s/math, tu/mei", context.get());
        assertThrows(IllegalArgumentException.class, () -> retrieval.search(
                new SearchCriteria(SearchCategory.STUDENT, Map.of())));
        assertEquals("Lesson search: d/mon, s/math, tu/mei", context.get());
        retrieval.search(new SearchCriteriaParser().parse("c/lesson"));
        assertEquals("Lesson search: all lessons", context.get());
        retrieval.list(Optional.empty());
        assertEquals("All lessons", context.get());
        assertSame(context, retrieval.contextProperty());
    }

    @Test
    public void selectedResults_keepIdentityAndFollowOnlySelectedSourceAcrossSwitches() throws Exception {
        var displayed = retrieval.getDisplayedLessons();
        assertIds(displayed, "L2", "L10", "L3", "L4", "L11");
        assertThrows(UnsupportedOperationException.class, displayed::clear);
        SearchCriteria math = new SearchCriteriaParser().parse("c/lesson s/Math");
        retrieval.search(math);
        assertIds(displayed, "L10", "L4");
        retrieval.search(math); // Reusing the source must not duplicate its subscription.
        retrieval.list(Optional.of(Index.fromOneBased(4)));
        queries.setLessonFilter(new SearchCriteriaParser().parse("c/lesson s/Science"));
        assertIds(displayed, "L10", "L11"); // The old search source is detached.
        retrieval.list(Optional.empty());
        queries.setLessonFilter(math);
        assertIds(displayed, "L2", "L10", "L3", "L4", "L11");
        assertSame(displayed, retrieval.getDisplayedLessons());
    }

    @Test
    public void selectedStudentAndSearch_refreshMembershipAttendanceDeletionAndRestore() throws Exception {
        PonHubDataState original = data.exportState();
        var displayed = retrieval.getDisplayedLessons();
        retrieval.list(Optional.of(Index.fromOneBased(2)));
        peopleView.setRoleFilter(Optional.of(PersonRole.TUTOR));
        PersonId student = new PersonId("S1");
        var withoutStudent = original.lessons().stream().map(lesson -> lesson.withoutEnrolledStudent(student)).toList();
        Attendance historical = Attendance.fromStrings(student, new LessonId("L10"), "2026-10-05", "present");
        data.resetData(new PonHubDataState(original.people(), withoutStudent, List.of(historical), 11));
        assertTrue(displayed.isEmpty()); // Former attendance must never become current membership.
        retrieval.search(new SearchCriteriaParser().parse("c/lesson sn/Alex"));
        assertTrue(displayed.isEmpty());
        var enrolled = withoutStudent.stream().map(lesson -> lesson.getId().equals(new LessonId("L3"))
                ? lesson.withEnrolledStudent(student) : lesson).toList();
        data.resetData(new PonHubDataState(original.people(), enrolled, List.of(historical), 11));
        assertIds(displayed, "L3");
        assertEquals(1, displayed.getFirst().getRosterSize());
        data.resetData(new PonHubDataState(original.people(), enrolled, List.of(), 11)); // Unmark.
        assertIds(displayed, "L3");
        retrieval.list(Optional.empty());
        data.resetData(new PonHubDataState(original.people(), List.of(), List.of(), 11)); // Catalogue removal.
        assertTrue(displayed.isEmpty());
        data.resetData(original); // Restore the complete prior state.
        assertIds(displayed, "L2", "L10", "L3", "L4", "L11");
        assertSame(displayed, retrieval.getDisplayedLessons());
    }
}
