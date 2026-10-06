package seedu.address.model.lesson;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * Identifies one lesson using a stable, positive, L-prefixed sequence number.
 * Guarantees: immutable; normalized; valid as declared in {@link #isValidLessonId(String)}.
 */
public final class LessonId implements Comparable<LessonId> {

    public static final String MESSAGE_CONSTRAINTS =
            "Lesson IDs should start with L followed by a positive number without leading zeros";
    public static final String MESSAGE_SEQUENCE_EXHAUSTED = "No further Lesson ID can be allocated";
    public static final String VALIDATION_REGEX = "[Ll][1-9]\\d*";

    private final long sequenceNumber;

    /**
     * Constructs a {@code LessonId} from its external representation.
     * The prefix is normalized to upper case.
     *
     * @param lessonId A valid lesson ID.
     */
    public LessonId(String lessonId) {
        requireNonNull(lessonId);
        checkArgument(isValidLessonId(lessonId), MESSAGE_CONSTRAINTS);
        sequenceNumber = Long.parseLong(lessonId.substring(1));
    }

    /**
     * Creates a lesson ID from a positive sequence number.
     */
    public static LessonId fromSequenceNumber(long sequenceNumber) {
        checkArgument(sequenceNumber > 0, MESSAGE_CONSTRAINTS);
        return new LessonId("L" + sequenceNumber);
    }

    /**
     * Returns true if {@code test} is an L-prefixed positive number that fits in a {@code long}.
     */
    public static boolean isValidLessonId(String test) {
        requireNonNull(test);
        if (!test.matches(VALIDATION_REGEX)) {
            return false;
        }

        try {
            Long.parseLong(test.substring(1));
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Returns the ID immediately after this ID.
     *
     * @throws IllegalStateException if the positive {@code long} sequence has been exhausted
     */
    public LessonId next() {
        if (sequenceNumber == Long.MAX_VALUE) {
            throw new IllegalStateException(MESSAGE_SEQUENCE_EXHAUSTED);
        }
        return fromSequenceNumber(sequenceNumber + 1);
    }

    @Override
    public int compareTo(LessonId other) {
        requireNonNull(other);
        return Long.compare(sequenceNumber, other.sequenceNumber);
    }

    @Override
    public String toString() {
        return "L" + sequenceNumber;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof LessonId otherLessonId)) {
            return false;
        }
        return sequenceNumber == otherLessonId.sequenceNumber;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(sequenceNumber);
    }
}
