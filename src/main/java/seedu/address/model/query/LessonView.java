package seedu.address.model.query;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Read-only projection of one shared recurring lesson for query results.
 * Implementations expose non-null values from the canonical lesson and resolve tutor details by stable identity.
 * Changed lesson or tutor details are represented by replacement entries in the observable result lists.
 */
public interface LessonView {
    /** Returns the stable lesson ID, such as {@code L1}. */
    String getLessonId();

    /** Returns the assigned tutor's stable ID, such as {@code T1}. */
    String getTutorId();

    /** Returns the tutor's current name from the canonical people registry. */
    String getTutorName();

    /** Returns the recurring lesson's weekday. */
    DayOfWeek getDay();

    /** Returns the lesson's start time. */
    LocalTime getStartTime();

    /** Returns the lesson's end time. */
    LocalTime getEndTime();

    /** Returns the lesson's subject. */
    String getSubject();

    /** Returns the lesson's room. */
    String getRoom();
}
