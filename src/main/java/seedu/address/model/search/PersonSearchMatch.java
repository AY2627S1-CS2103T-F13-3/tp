package seedu.address.model.search;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.util.Optional;

import seedu.address.model.person.Student;
import seedu.address.model.query.LessonView;

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
