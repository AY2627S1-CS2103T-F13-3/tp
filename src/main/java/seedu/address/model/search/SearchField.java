package seedu.address.model.search;

import static java.util.Objects.requireNonNull;
import static seedu.address.model.search.SearchCategory.LESSON;
import static seedu.address.model.search.SearchCategory.PARENT;
import static seedu.address.model.search.SearchCategory.STUDENT;
import static seedu.address.model.search.SearchCategory.TUTOR;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/**
 * Search prefixes, their category compatibility and their field-level matching rules.
 * Query text accepts fragments rather than requiring a complete valid person or lesson field.
 */
public enum SearchField {
    NAME("n/", STUDENT, PARENT, TUTOR),
    LEVEL("l/", STUDENT),
    PHONE("p/", STUDENT, PARENT, TUTOR),
    PARENT_PHONE("pp/", STUDENT),
    EMAIL("e/", STUDENT, PARENT, TUTOR),
    ADDRESS("a/", STUDENT, PARENT, TUTOR),
    STUDENT_NAME("sn/", PARENT, TUTOR, LESSON),
    DAY("d/", STUDENT, PARENT, TUTOR, LESSON),
    START_TIME("st/", STUDENT, PARENT, TUTOR, LESSON),
    END_TIME("et/", STUDENT, PARENT, TUTOR, LESSON),
    SUBJECT("s/", STUDENT, PARENT, TUTOR, LESSON),
    TUTOR_NAME("tu/", STUDENT, PARENT, LESSON),
    ROOM("rm/", STUDENT, PARENT, TUTOR, LESSON);

    private final String prefix;
    private final Set<SearchCategory> supportedCategories;

    SearchField(String prefix, SearchCategory... categories) {
        this.prefix = prefix;
        supportedCategories = Set.of(categories);
    }

    public String getPrefix() {
        return prefix;
    }

    /** Returns an immutable set of categories accepting this filter. */
    public Set<SearchCategory> getSupportedCategories() {
        return supportedCategories;
    }

    /**
     * Returns whether this filter is supported by the selected result category.
     */
    public boolean isSupportedBy(SearchCategory category) {
        return supportedCategories.contains(requireNonNull(category));
    }

    /**
     * Resolves a lowercase search prefix, including its slash.
     * @throws IllegalArgumentException If the prefix is unsupported.
     */
    public static SearchField fromPrefix(String prefix) {
        requireNonNull(prefix);
        return Arrays.stream(values())
                .filter(field -> field.prefix.equals(prefix))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported search prefix '" + prefix + "'."));
    }

    /**
     * Validates and normalizes one query value without depending on unfinished domain record types.
     */
    String normalizeValue(String value) {
        requireNonNull(value);
        String trimmed = value.strip();
        if (trimmed.isEmpty() || value.contains("/") || value.contains("\n") || value.contains("\r")) {
            throw new IllegalArgumentException("Search filter '" + prefix
                    + "' requires a non-blank value without slashes or line breaks.");
        }

        return switch (this) {
            case PHONE, PARENT_PHONE -> {
                if (!trimmed.matches("[0-9]{3,15}")) {
                    throw new IllegalArgumentException("Search filter '" + prefix + "' requires 3-15 digits.");
                }
                yield trimmed;
            }
            case LEVEL -> {
                String level = trimmed.toUpperCase(Locale.ROOT);
                if (!level.matches("P[1-6]|S[1-5]|JC[12]")) {
                    throw new IllegalArgumentException("Search filter 'l/' requires P1-P6, S1-S5, JC1 or JC2.");
                }
                yield level;
            }
            case DAY -> {
                String day = trimmed.toLowerCase(Locale.ROOT);
                if (!Set.of("mon", "tue", "wed", "thu", "fri", "sat", "sun").contains(day)) {
                    throw new IllegalArgumentException("Search filter 'd/' requires "
                            + "Mon, Tue, Wed, Thu, Fri, Sat or Sun.");
                }
                yield day;
            }
            case START_TIME, END_TIME -> {
                if (!trimmed.matches("([01][0-9]|2[0-3])[0-5][0-9]")) {
                    throw new IllegalArgumentException("Search filter '" + prefix
                            + "' requires a valid four-digit HHMM.");
                }
                yield trimmed;
            }
            default -> trimmed.toLowerCase(Locale.ROOT);
        };
    }

    /**
     * Matches a normalized query against one field value; absent optional values do not match a supplied filter.
     */
    boolean matchesValue(String query, String candidate) {
        if (candidate == null) {
            return false;
        }
        return switch (this) {
            case PHONE, PARENT_PHONE, LEVEL, DAY, START_TIME, END_TIME -> {
                try {
                    yield query.equals(normalizeValue(candidate));
                } catch (IllegalArgumentException e) {
                    yield false;
                }
            }
            default -> candidate.toLowerCase(Locale.ROOT).contains(query);
        };
    }
}
