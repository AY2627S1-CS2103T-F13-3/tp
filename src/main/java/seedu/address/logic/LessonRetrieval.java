package seedu.address.logic;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.PeopleView;
import seedu.address.model.query.LessonQueries;
import seedu.address.model.query.LessonView;
import seedu.address.model.search.SearchCriteria;

/**
 * Selects live lesson results from the canonical query source without changing the people view.
 * The UI keeps one read-only list while commands switch its source between catalogue, student and search results.
 */
public final class LessonRetrieval {
    private final LessonQueries queries;
    private final PeopleView peopleView;
    private final ObservableList<LessonView> displayed = FXCollections.observableArrayList();
    private final ObservableList<LessonView> readOnlyDisplayed = FXCollections.unmodifiableObservableList(displayed);
    private final ListChangeListener<LessonView> refresh = change -> displayed.setAll(change.getList());
    private ObservableList<LessonView> selected;

    /**
     * Uses the query and people views supplied by the same canonical runtime adapter.
     * Initially displays the complete catalogue.
     */
    public LessonRetrieval(LessonQueries queries, PeopleView peopleView) {
        this.queries = requireNonNull(queries);
        this.peopleView = requireNonNull(peopleView);
        selected = queries.getLessonList();
        selected.addListener(refresh);
        displayed.setAll(selected);
    }

    /**
     * Returns the same read-only UI list across commands, committed changes and rollback.
     */
    public ObservableList<LessonView> getDisplayedLessons() {
        return readOnlyDisplayed;
    }

    /**
     * Returns the full catalogue or one current-view student's lessons.
     * A supplied index is resolved once against the current people list, then converted to a stable ID.
     * The returned observable result is read-only and remains current after canonical publication.
     */
    public ObservableList<LessonView> list(Optional<Index> studentIndex) throws CommandException {
        requireNonNull(studentIndex);
        if (studentIndex.isEmpty()) {
            return select(queries.getLessonList());
        }
        var student = PersonIndexResolver.resolveStudent(studentIndex.get(), peopleView.getPeople());
        return select(queries.getStudentLessons(student.getId()));
    }

    /**
     * Applies lesson search to a separate read-only result view without changing the people view or stored data.
     * An invalid category leaves the previous lesson filter intact.
     */
    public ObservableList<LessonView> search(SearchCriteria criteria) {
        queries.setLessonFilter(requireNonNull(criteria));
        return select(queries.getFilteredLessonList());
    }

    /**
     * Detaches the previous source so unrelated query updates cannot replace the selected results.
     */
    private ObservableList<LessonView> select(ObservableList<LessonView> source) {
        if (selected != source) {
            selected.removeListener(refresh);
            selected = source;
            selected.addListener(refresh);
            displayed.setAll(selected);
        }
        return source;
    }
}
