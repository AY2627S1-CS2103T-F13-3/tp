package ponhub.model.lesson;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.AppUtil.checkArgument;

import java.text.Normalizer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Represents the subject taught in a lesson.
 * Guarantees: immutable; NFC- and whitespace-normalized; valid as declared in {@link #isValidSubject(String)}.
 */
public final class Subject {

    public static final String MESSAGE_CONSTRAINTS =
            "Subjects should contain 1 to 50 Unicode code points after normalization, including spaces and hyphens, "
                    + "using letters with attached combining marks or digits, separated by single spaces or hyphens.";
    public static final String VALIDATION_REGEX =
            "(?:\\p{L}\\p{M}*|\\p{N})+(?:[ -](?:\\p{L}\\p{M}*|\\p{N})+)*";

    private static final int MAX_LENGTH = 50;
    // A Unicode scalar decomposes into at most four code points in Java 25.
    // Bound canonical reordering work without rejecting a valid NFC subject's decomposed spelling.
    private static final int MAX_NORMALIZATION_CODE_POINTS = MAX_LENGTH * 4;
    // One letter with its attached marks, one digit, or one separator.
    private static final Pattern STORED_CHARACTER_PATTERN = Pattern.compile("\\p{L}\\p{M}*|\\p{N}|[ -]");

    private final String value;

    /**
     * Constructs an NFC-normalized {@code Subject}. Leading, trailing and repeated spaces are normalized.
     */
    public Subject(String subject) {
        this(subject, Subject::isNormalizedSubjectValid);
    }

    private Subject(String subject, Predicate<String> isValidNormalizedSubject) {
        requireNonNull(subject);
        String spacedSubject = normalizeSpaces(subject);
        checkArgument(isWithinNormalizationBudget(spacedSubject), MESSAGE_CONSTRAINTS);
        String normalizedSubject = Normalizer.normalize(spacedSubject, Normalizer.Form.NFC);
        checkArgument(isValidNormalizedSubject.test(normalizedSubject), MESSAGE_CONSTRAINTS);
        value = normalizedSubject;
    }

    /**
     * Returns a subject read from a saved data file.
     * Earlier builds allowed 50 letters or digits before NFC, which can expand one letter into a letter with
     * attached marks. A saved value may therefore hold 50 letters (each with its attached marks), digits, spaces
     * or hyphens, so those stores still load and re-save stably. New input keeps the constructor's limit.
     *
     * @throws IllegalArgumentException if the saved value is not a valid subject under that rule.
     */
    public static Subject fromStoredValue(String subject) {
        return new Subject(subject, Subject::isValidStoredSubject);
    }

    /**
     * Returns true if {@code test} is a valid subject after space and NFC normalization.
     */
    public static boolean isValidSubject(String test) {
        requireNonNull(test);
        String spacedSubject = normalizeSpaces(test);
        return isWithinNormalizationBudget(spacedSubject)
                && isNormalizedSubjectValid(Normalizer.normalize(spacedSubject, Normalizer.Form.NFC));
    }

    private static String normalizeSpaces(String subject) {
        return subject.strip().replaceAll(" +", " ");
    }

    private static boolean isWithinNormalizationBudget(String subject) {
        return subject.codePointCount(0, subject.length()) <= MAX_NORMALIZATION_CODE_POINTS;
    }

    private static boolean isNormalizedSubjectValid(String subject) {
        return subject.codePointCount(0, subject.length()) <= MAX_LENGTH && subject.matches(VALIDATION_REGEX);
    }

    private static boolean isValidStoredSubject(String subject) {
        // The saved value must also fit the next load's normalization budget, so it can be reopened.
        return isWithinNormalizationBudget(subject) && subject.matches(VALIDATION_REGEX)
                && STORED_CHARACTER_PATTERN.matcher(subject).results().count() <= MAX_LENGTH;
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
