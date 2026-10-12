package seedu.address.logic;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import javafx.collections.ObservableList;
import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.PeopleView;
import seedu.address.model.query.LessonQueries;
import seedu.address.model.query.LessonView;
import seedu.address.model.search.SearchCriteria;

/**
 * Prepared lesson-result boundary using the canonical query source and current people view.
 * The runtime adapter supplies both from the same canonical root when lesson commands activate.
 */
public final class LessonRetrieval {
    private final LessonQueries queries;
    private final PeopleView peopleView;

    /** Uses the query and people views supplied by the same canonical runtime adapter. */
    public LessonRetrieval(LessonQueries queries, PeopleView peopleView) {
        this.queries = requireNonNull(queries);
        this.peopleView = requireNonNull(peopleView);
    }

    /**
     * Returns the full catalogue or one current-view student's lessons.
     * A supplied index is resolved once against the current people list, then converted to a stable ID.
     * The returned observable result is read-only and remains current after canonical publication.
     */
    public ObservableList<LessonView> list(Optional<Index> studentIndex) throws CommandException {
        requireNonNull(studentIndex);
        if (studentIndex.isEmpty()) {
            return queries.getLessonList();
        }
        var student = PersonIndexResolver.resolveStudent(studentIndex.get(), peopleView.getPeople());
        return queries.getStudentLessons(student.getId());
    }

    /**
     * Applies lesson search to a separate read-only result view without changing the people view or stored data.
     * An invalid category leaves the previous lesson filter intact.
     */
    public ObservableList<LessonView> search(SearchCriteria criteria) {
        queries.setLessonFilter(requireNonNull(criteria));
        return queries.getFilteredLessonList();
    }
}
