package seedu.address.model.search;

import static java.util.Objects.requireNonNull;

import java.util.Locale;
import java.util.Optional;

import seedu.address.model.person.PersonRole;

/**
 * The four result categories supported by the search specification.
 */
public enum SearchCategory {
    STUDENT,
    PARENT,
    TUTOR,
    LESSON;

    /**
     * Returns the person role selected by this category; lessons have no person role.
     */
    public Optional<PersonRole> getPersonRole() {
        return switch (this) {
            case STUDENT -> Optional.of(PersonRole.STUDENT);
            case PARENT -> Optional.of(PersonRole.PARENT);
            case TUTOR -> Optional.of(PersonRole.TUTOR);
            case LESSON -> Optional.empty();
        };
    }

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
