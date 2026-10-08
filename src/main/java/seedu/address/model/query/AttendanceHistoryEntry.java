package seedu.address.model.query;

import java.time.LocalDate;

import seedu.address.model.lesson.LessonId;
import seedu.address.model.person.PersonId;

/**
 * Read-only projection of one recorded student, lesson and date attendance key.
 * Current membership is derived separately from the lesson roster; retained history survives unenrolment.
 * An unrecorded occurrence has no history entry.
 * Implementations expose non-null, immutable values and a student-role ID. Changes replace the whole entry.
 */
public interface AttendanceHistoryEntry {
    /** Returns the student's stable identity. */
    PersonId getStudentId();

    /** Returns the lesson's stable identity. */
    LessonId getLessonId();

    /** Returns the date of this recorded occurrence. */
    LocalDate getDate();

    /** Returns the recorded attendance status. */
    Status getStatus();

    /** Returns whether the student is currently enrolled in this lesson. */
    boolean isCurrentlyEnrolled();

    /** The recorded statuses exposed by the history view. */
    enum Status {
        PRESENT,
        ABSENT
    }
}
