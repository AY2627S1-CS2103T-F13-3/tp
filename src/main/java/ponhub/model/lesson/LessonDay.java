package ponhub.model.lesson;

import static java.util.Objects.requireNonNull;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * A supported weekday for a weekly recurring lesson.
 */
public enum LessonDay {
    MONDAY("Mon", DayOfWeek.MONDAY),
    TUESDAY("Tue", DayOfWeek.TUESDAY),
    WEDNESDAY("Wed", DayOfWeek.WEDNESDAY),
    THURSDAY("Thu", DayOfWeek.THURSDAY),
    FRIDAY("Fri", DayOfWeek.FRIDAY),
    SATURDAY("Sat", DayOfWeek.SATURDAY),
    SUNDAY("Sun", DayOfWeek.SUNDAY);

    public static final String MESSAGE_CONSTRAINTS =
            "Lesson days should be one of Mon, Tue, Wed, Thu, Fri, Sat or Sun";

    private final String displayValue;
    private final DayOfWeek dayOfWeek;

    LessonDay(String displayValue, DayOfWeek dayOfWeek) {
        this.displayValue = displayValue;
        this.dayOfWeek = dayOfWeek;
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

    /**
     * Returns true if the date falls on this lesson day.
     */
    public boolean matches(LocalDate date) {
        requireNonNull(date);
        return dayOfWeek == date.getDayOfWeek();
    }

    @Override
    public String toString() {
        return displayValue;
    }
}
