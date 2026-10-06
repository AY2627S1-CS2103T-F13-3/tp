package seedu.address.model.lesson;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.util.Objects;

/**
 * A same-day time range for a weekly recurring lesson.
 * The range is half-open: it includes the start time but excludes the end time.
 */
public final class LessonTimeSlot {

    public static final String MESSAGE_CONSTRAINTS =
            "A lesson end time should be later than its start time on the same day";

    private final LessonDay day;
    private final LessonTime startTime;
    private final LessonTime endTime;

    /**
     * Constructs a {@code LessonTimeSlot}.
     */
    public LessonTimeSlot(LessonDay day, LessonTime startTime, LessonTime endTime) {
        requireNonNull(day);
        requireNonNull(startTime);
        requireNonNull(endTime);
        checkArgument(startTime.compareTo(endTime) < 0, MESSAGE_CONSTRAINTS);
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public LessonDay getDay() {
        return day;
    }

    public LessonTime getStartTime() {
        return startTime;
    }

    public LessonTime getEndTime() {
        return endTime;
    }

    /**
     * Returns true if this slot and {@code other} share any time on the same weekday.
     * Adjacent slots do not overlap.
     */
    public boolean overlaps(LessonTimeSlot other) {
        requireNonNull(other);
        return day == other.day
                && startTime.compareTo(other.endTime) < 0
                && other.startTime.compareTo(endTime) < 0;
    }

    @Override
    public String toString() {
        return day + " " + startTime + "-" + endTime;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof LessonTimeSlot otherLessonTimeSlot)) {
            return false;
        }
        return day == otherLessonTimeSlot.day
                && startTime.equals(otherLessonTimeSlot.startTime)
                && endTime.equals(otherLessonTimeSlot.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(day, startTime, endTime);
    }
}
