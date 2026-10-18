package seedu.address.logic.parser;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Optional;

import seedu.address.model.lesson.LessonDay;
import seedu.address.model.lesson.LessonTime;
import seedu.address.model.lesson.Room;
import seedu.address.model.lesson.Subject;
import seedu.address.model.person.PersonId;

/**
 * Immutable lesson-edit intent containing validated replacement values without a Lesson ID or writable model state.
 * An empty optional retains the corresponding existing value.
 */
public record LessonEditInput(Optional<LessonDay> day, Optional<LessonTime> startTime,
        Optional<LessonTime> endTime, Optional<Subject> subject, Optional<PersonId> tutorId,
        Optional<Room> room) {

    /**
     * Creates edit intent with one optional replacement for every editable Lesson field.
     * Keeping every field is permitted here; the command parser owns the supplied-prefix requirement.
     *
     * @throws NullPointerException if any optional is null.
     */
    public LessonEditInput {
        requireAllNonNull(day, startTime, endTime, subject, tutorId, room);
    }
}
