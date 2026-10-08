package seedu.address.model.attendance;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Objects;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.person.PersonId;

/**
 * An immutable recorded attendance outcome for one student and one lesson occurrence.
 * Current lesson membership is deliberately not stored here.
 */
public final class Attendance {

    public static final String MESSAGE_DATE_CONSTRAINTS =
            "Attendance dates must be real calendar dates in YYYY-MM-DD format.";
    public static final String DATE_VALIDATION_REGEX = "[0-9]{4}-[0-9]{2}-[0-9]{2}";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("uuuu-MM-dd", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    private final AttendanceKey key;
    private final AttendanceStatus status;

    /**
     * Creates an attendance record from validated identity and date values.
     */
    public Attendance(PersonId studentId, LessonId lessonId, LocalDate date, AttendanceStatus status) {
        this(new AttendanceKey(studentId, lessonId, date), status);
    }

    /**
     * Creates an attendance record for an existing key.
     */
    public Attendance(AttendanceKey key, AttendanceStatus status) {
        requireAllNonNull(key, status);
        this.key = key;
        this.status = status;
    }

    /**
     * Creates an independent attendance value with the same key and status.
     */
    public Attendance(Attendance other) {
        this(requireNonNull(other).key, other.status);
    }

    /**
     * Parses the external date and status forms and creates an attendance record.
     */
    public static Attendance fromStrings(PersonId studentId, LessonId lessonId, String date, String status) {
        return new Attendance(studentId, lessonId, parseDate(date), AttendanceStatus.parse(status));
    }

    /**
     * Parses a real calendar date using the exact ASCII YYYY-MM-DD form.
     */
    public static LocalDate parseDate(String date) {
        requireNonNull(date);
        if (!date.matches(DATE_VALIDATION_REGEX)) {
            throw new IllegalArgumentException(MESSAGE_DATE_CONSTRAINTS);
        }
        try {
            LocalDate parsedDate = LocalDate.parse(date, DATE_FORMATTER);
            if (parsedDate.getYear() < 1) {
                throw new IllegalArgumentException(MESSAGE_DATE_CONSTRAINTS);
            }
            return parsedDate;
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(MESSAGE_DATE_CONSTRAINTS, exception);
        }
    }

    public AttendanceKey getKey() {
        return key;
    }

    public PersonId getStudentId() {
        return key.getStudentId();
    }

    public LessonId getLessonId() {
        return key.getLessonId();
    }

    public LocalDate getDate() {
        return key.getDate();
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    /**
     * Returns a replacement record for the same unique key with the supplied status.
     */
    public Attendance withStatus(AttendanceStatus replacementStatus) {
        return new Attendance(key, replacementStatus);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Attendance otherAttendance)) {
            return false;
        }
        return key.equals(otherAttendance.key) && status == otherAttendance.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, status);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("key", key)
                .add("status", status)
                .toString();
    }
}
