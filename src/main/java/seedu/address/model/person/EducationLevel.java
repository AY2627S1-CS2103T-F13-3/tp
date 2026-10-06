package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.util.Locale;

/**
 * An immutable student education level from P1-P6, S1-S5, or JC1-JC2.
 */
public final class EducationLevel {
    public static final String MESSAGE_CONSTRAINTS = "Education levels must be P1-P6, S1-S5, JC1, or JC2.";

    private static final String VALIDATION_REGEX = "P[1-6]|S[1-5]|JC[12]";

    private final String value;

    /**
     * Creates an education level, normalizing letter case and surrounding whitespace.
     *
     * @param level A supported student education level.
     * @throws NullPointerException if {@code level} is null.
     * @throws IllegalArgumentException if {@code level} is not supported.
     */
    public EducationLevel(String level) {
        requireNonNull(level);
        String normalizedLevel = level.trim().toUpperCase(Locale.ROOT);
        checkArgument(isValid(normalizedLevel), MESSAGE_CONSTRAINTS);
        value = normalizedLevel;
    }

    /**
     * Returns whether the input represents a supported education level after normalization.
     */
    public static boolean isValid(String level) {
        return level != null && level.trim().toUpperCase(Locale.ROOT).matches(VALIDATION_REGEX);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof EducationLevel otherLevel)) {
            return false;
        }

        return value.equals(otherLevel.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
