package ponhub.model.query;

import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.time.LocalDate;

import ponhub.model.attendance.Attendance;
import ponhub.model.attendance.AttendanceStatus;
import ponhub.model.lesson.LessonId;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRole;

/**
 * Immutable attendance projection with membership taken from the current lesson roster.
 */
public record AttendanceHistoryEntryData(PersonId studentId, LessonId lessonId, LocalDate date,
        Status status, boolean currentlyEnrolled) implements AttendanceHistoryEntry {

    /**
     * Validates the immutable public projection fields.
     */
    public AttendanceHistoryEntryData {
        requireAllNonNull(studentId, lessonId, date, status);
        if (studentId.getRole() != PersonRole.STUDENT) {
            throw new IllegalArgumentException("Attendance history requires a Student ID.");
        }
    }

    /**
     * Copies a stored record and adds membership from the current Lesson roster.
     */
    public AttendanceHistoryEntryData(Attendance attendance, boolean currentlyEnrolled) {
        this(attendance.getStudentId(), attendance.getLessonId(), attendance.getDate(),
                attendance.getStatus() == AttendanceStatus.PRESENT ? Status.PRESENT : Status.ABSENT,
                currentlyEnrolled);
    }

    @Override
    public PersonId getStudentId() {
        return studentId;
    }

    @Override
    public LessonId getLessonId() {
        return lessonId;
    }

    @Override
    public LocalDate getDate() {
        return date;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public boolean isCurrentlyEnrolled() {
        return currentlyEnrolled;
    }
}
