package seedu.address.model.query;

import java.time.LocalDate;

/**
 * Read-only projection of a recorded attendance key and its status.
 * Implementations expose non-null values and derive current membership from the canonical lesson's roster.
 * Historical entries remain available after unenrolment. Changes to attendance or membership are represented by
 * replacement entries in the observable history. An unrecorded occurrence has no history entry.
 */
public interface AttendanceHistoryEntry {
    /** Returns the student's stable ID. */
    String getStudentId();

    /** Returns the shared lesson's stable ID. */
    String getLessonId();

    /** Returns the date of the recorded lesson occurrence. */
    LocalDate getDate();

    /** Returns the recorded attendance status. */
    Status getStatus();

    /** Returns whether the student is currently enrolled in this entry's lesson. */
    boolean isCurrentlyEnrolled();

    /**
     * The two recorded statuses supported by the attendance history view.
     * This view enum is mapped from the canonical attendance record during query integration.
     */
    enum Status {
        PRESENT,
        ABSENT
    }
}
