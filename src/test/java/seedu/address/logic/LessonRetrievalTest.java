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
}
