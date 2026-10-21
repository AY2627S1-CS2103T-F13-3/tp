package ponhub.model.attendance;

import static ponhub.commons.util.AppUtil.checkArgument;
import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.time.LocalDate;
import java.util.Objects;

import ponhub.commons.util.ToStringBuilder;
import ponhub.model.lesson.LessonId;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRole;

/**
 * The immutable unique key for one student's attendance at one dated lesson occurrence.
 */
public final class AttendanceKey {

    public static final String MESSAGE_STUDENT_ID_CONSTRAINTS = "Attendance requires a student person ID.";

    private final PersonId studentId;
    private final LessonId lessonId;
    private final LocalDate date;

    /**
     * Creates an attendance key.
     *
     * @throws IllegalArgumentException if {@code studentId} is not a student-role ID
     */
    public AttendanceKey(PersonId studentId, LessonId lessonId, LocalDate date) {
        requireAllNonNull(studentId, lessonId, date);
        checkArgument(studentId.getRole() == PersonRole.STUDENT, MESSAGE_STUDENT_ID_CONSTRAINTS);
        this.studentId = studentId;
        this.lessonId = lessonId;
        this.date = date;
    }

    public PersonId getStudentId() {
        return studentId;
    }

    public LessonId getLessonId() {
        return lessonId;
    }

    public LocalDate getDate() {
        return date;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof AttendanceKey otherKey)) {
            return false;
        }
        return studentId.equals(otherKey.studentId)
                && lessonId.equals(otherKey.lessonId)
                && date.equals(otherKey.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId, lessonId, date);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("studentId", studentId)
                .add("lessonId", lessonId)
                .add("date", date)
                .toString();
    }
}
