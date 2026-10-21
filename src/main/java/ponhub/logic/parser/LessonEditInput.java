package ponhub.logic.parser;

import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Optional;

import ponhub.model.lesson.LessonDay;
import ponhub.model.lesson.LessonTime;
import ponhub.model.lesson.Room;
import ponhub.model.lesson.Subject;
import ponhub.model.person.PersonId;

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
