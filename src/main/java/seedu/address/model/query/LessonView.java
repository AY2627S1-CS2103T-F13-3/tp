package seedu.address.model.query;

import seedu.address.model.lesson.LessonId;
import seedu.address.model.lesson.LessonTimeSlot;
import seedu.address.model.lesson.Room;
import seedu.address.model.lesson.Subject;
import seedu.address.model.person.PersonId;

/**
 * Read-only projection of one shared lesson for catalogue, search and schedule results.
 * Values describe a single state of the canonical lesson and its assigned tutor.
 * Implementations expose non-null, immutable values and a tutor-role ID.
 * A changed lesson or tutor is represented by a replacement entry in observable lists.
 */
public interface LessonView {
    /** Returns the stable identity of this lesson. */
    LessonId getLessonId();

    /** Returns the stable identity of its assigned tutor. */
    PersonId getTutorId();

    /** Returns the assigned tutor's current display name. */
    String getTutorName();

    /** Returns the weekly day and time range. */
    LessonTimeSlot getTimeSlot();

    /** Returns the lesson's subject. */
    Subject getSubject();

    /** Returns the lesson's room. */
    Room getRoom();

    /** Returns the number of currently enrolled students, including zero for an empty roster. */
    int getRosterSize();
}
