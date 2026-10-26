package ponhub.model.lesson;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.AppUtil.checkArgument;

/**
 * Represents the subject taught in a lesson.
 * Guarantees: immutable; whitespace-normalized; valid as declared in {@link #isValidSubject(String)}.
 */
public final class Subject {

    public static final String MESSAGE_CONSTRAINTS =
            "Subjects should contain 1 to 50 letters or digits, separated by single spaces or hyphens";
    public static final String VALIDATION_REGEX = "[\\p{L}\\p{N}]+(?:[ -][\\p{L}\\p{N}]+)*";

    private final String value;

    /**
     * Constructs a {@code Subject}. Leading, trailing and repeated spaces are normalized.
     */
    public Subject(String subject) {
        requireNonNull(subject);
        String normalizedSubject = normalize(subject);
        checkArgument(isNormalizedSubjectValid(normalizedSubject), MESSAGE_CONSTRAINTS);
        value = normalizedSubject;
    }

    /**
     * Returns true if {@code test} is a valid subject after space normalization.
     */
    public static boolean isValidSubject(String test) {
        requireNonNull(test);
        return isNormalizedSubjectValid(normalize(test));
    }

    private static String normalize(String subject) {
        return subject.strip().replaceAll(" +", " ");
    }

    private static boolean isNormalizedSubjectValid(String subject) {
        return subject.codePointCount(0, subject.length()) <= 50 && subject.matches(VALIDATION_REGEX);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Subject otherSubject)) {
            return false;
        }
        return value.equals(otherSubject.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
