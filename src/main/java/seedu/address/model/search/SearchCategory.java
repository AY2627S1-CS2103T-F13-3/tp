package seedu.address.model.search;

import static java.util.Objects.requireNonNull;

import java.util.Locale;

/**
 * The four result categories supported by the search specification.
 */
public enum SearchCategory {
    STUDENT,
    PARENT,
    TUTOR,
    LESSON;

    /**
     * Parses a category keyword, ignoring surrounding whitespace and letter case.
     *
     * @throws IllegalArgumentException If the keyword is not a supported category.
     */
    public static SearchCategory fromValue(String value) {
        requireNonNull(value);
        try {
            return valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported search category '" + value
                    + "'. Use student, parent, tutor or lesson.", e);
        }
    }
}
