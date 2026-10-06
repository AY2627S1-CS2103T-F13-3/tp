package seedu.address.model.lesson;

import static java.util.Objects.requireNonNull;

/**
 * A supported weekday for a weekly recurring lesson.
 */
public enum LessonDay {
    MONDAY("Mon"),
    TUESDAY("Tue"),
    WEDNESDAY("Wed"),
    THURSDAY("Thu"),
    FRIDAY("Fri"),
    SATURDAY("Sat"),
    SUNDAY("Sun");

    public static final String MESSAGE_CONSTRAINTS =
            "Lesson days should be one of Mon, Tue, Wed, Thu, Fri, Sat or Sun";

    private final String displayValue;

    LessonDay(String displayValue) {
        this.displayValue = displayValue;
    }

    /**
     * Parses a case-insensitive three-letter weekday.
     *
     * @throws IllegalArgumentException if {@code value} is not a supported weekday
     */
    public static LessonDay fromString(String value) {
        requireNonNull(value);
        for (LessonDay day : values()) {
            if (day.displayValue.equalsIgnoreCase(value)) {
                return day;
            }
        }
        throw new IllegalArgumentException(MESSAGE_CONSTRAINTS);
    }

    /**
     * Returns true if {@code test} is a supported three-letter weekday.
     */
    public static boolean isValidLessonDay(String test) {
        requireNonNull(test);
        for (LessonDay day : values()) {
            if (day.displayValue.equalsIgnoreCase(test)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return displayValue;
    }
}
