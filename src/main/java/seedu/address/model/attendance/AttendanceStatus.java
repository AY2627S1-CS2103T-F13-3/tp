package seedu.address.model.attendance;

import static java.util.Objects.requireNonNull;

import java.util.Locale;

/**
 * A recorded attendance outcome. An unrecorded occurrence has no attendance record.
 */
public enum AttendanceStatus {
    PRESENT("present"),
    ABSENT("absent");

    public static final String MESSAGE_CONSTRAINTS = "Attendance status must be present or absent.";

    private final String value;

    AttendanceStatus(String value) {
        this.value = value;
    }

    /**
     * Parses a status while ignoring surrounding whitespace and letter case.
     *
     * @throws IllegalArgumentException if the value is not present or absent
     */
    public static AttendanceStatus parse(String value) {
        requireNonNull(value);
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "present" -> PRESENT;
            case "absent" -> ABSENT;
            default -> throw new IllegalArgumentException(MESSAGE_CONSTRAINTS);
        };
    }

    @Override
    public String toString() {
        return value;
    }
}
