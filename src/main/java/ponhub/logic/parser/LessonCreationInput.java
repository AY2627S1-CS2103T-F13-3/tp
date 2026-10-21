package ponhub.logic.parser;

import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Optional;

import ponhub.model.lesson.LessonTimeSlot;
import ponhub.model.lesson.Room;
import ponhub.model.lesson.Subject;
import ponhub.model.person.Name;
import ponhub.model.person.Phone;

/**
 * Validated, identity-free input for creating one shared recurring lesson.
 * Tutor resolution and lesson ID allocation occur only at the staged command boundary.
 *
 * @param timeSlot Recurring weekday and half-open time range.
 * @param subject Subject taught in the lesson.
 * @param tutorName Full tutor name used for normalized matching.
 * @param tutorPhone Optional exact phone used to disambiguate the tutor.
 * @param room Normalized lesson room.
 */
public record LessonCreationInput(LessonTimeSlot timeSlot, Subject subject, Name tutorName,
        Optional<Phone> tutorPhone, Room room) {

    /**
     * Rejects null record components before the input reaches operational state.
     */
    public LessonCreationInput {
        requireAllNonNull(timeSlot, subject, tutorName, tutorPhone, room);
    }
}
