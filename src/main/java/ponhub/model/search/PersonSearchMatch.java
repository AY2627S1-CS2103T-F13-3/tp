package ponhub.model.search;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.AppUtil.checkArgument;

import java.util.Optional;

import ponhub.model.person.Student;
import ponhub.model.query.LessonView;

/**
 * Immutable relationship context for one matching person, separate from operational records and membership.
 */
public record PersonSearchMatch(Optional<Student> student, Optional<LessonView> lesson) {
    /**
     * Requires at least one associated record; absent context is represented by an empty match list.
     */
    public PersonSearchMatch {
        requireNonNull(student);
        requireNonNull(lesson);
        checkArgument(student.isPresent() || lesson.isPresent(),
                "A relationship match needs student or lesson context.");
    }
}
