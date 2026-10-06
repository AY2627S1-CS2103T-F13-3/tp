package seedu.address.model.lesson;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * A time of day represented externally using four-digit 24-hour HHMM format.
 * Guarantees: immutable; valid as declared in {@link #isValidLessonTime(String)}.
 */
public final class LessonTime implements Comparable<LessonTime> {

    public static final String MESSAGE_CONSTRAINTS =
            "Lesson times should use four-digit 24-hour HHMM format from 0000 to 2359";
    public static final String VALIDATION_REGEX = "\\d{4}";

    private static final int MINUTES_PER_HOUR = 60;

    private final int minutesFromMidnight;

    /**
     * Constructs a {@code LessonTime}.
     *
     * @param time A valid time in HHMM format.
     */
    public LessonTime(String time) {
        requireNonNull(time);
        checkArgument(isValidLessonTime(time), MESSAGE_CONSTRAINTS);
        int hour = Integer.parseInt(time.substring(0, 2));
        int minute = Integer.parseInt(time.substring(2));
        minutesFromMidnight = hour * MINUTES_PER_HOUR + minute;
    }

    /**
     * Returns true if {@code test} is a valid four-digit 24-hour time.
     */
    public static boolean isValidLessonTime(String test) {
        requireNonNull(test);
        if (!test.matches(VALIDATION_REGEX)) {
            return false;
        }
        int hour = Integer.parseInt(test.substring(0, 2));
        int minute = Integer.parseInt(test.substring(2));
        return hour < 24 && minute < MINUTES_PER_HOUR;
    }

    public int getMinutesFromMidnight() {
        return minutesFromMidnight;
    }

    @Override
    public int compareTo(LessonTime other) {
        requireNonNull(other);
        return Integer.compare(minutesFromMidnight, other.minutesFromMidnight);
    }

    @Override
    public String toString() {
        int hour = minutesFromMidnight / MINUTES_PER_HOUR;
        int minute = minutesFromMidnight % MINUTES_PER_HOUR;
        return String.format("%02d%02d", hour, minute);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof LessonTime otherLessonTime)) {
            return false;
        }
        return minutesFromMidnight == otherLessonTime.minutesFromMidnight;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(minutesFromMidnight);
    }
}
