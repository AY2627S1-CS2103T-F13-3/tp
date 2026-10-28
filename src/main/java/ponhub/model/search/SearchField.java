package ponhub.model.search;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.AppUtil.checkArgument;
import static ponhub.model.search.SearchCategory.LESSON;
import static ponhub.model.search.SearchCategory.PARENT;
import static ponhub.model.search.SearchCategory.STUDENT;
import static ponhub.model.search.SearchCategory.TUTOR;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import ponhub.commons.util.StringUtil;
import ponhub.model.lesson.LessonDay;
import ponhub.model.lesson.LessonTime;
import ponhub.model.person.EducationLevel;
import ponhub.model.person.Phone;
import ponhub.model.query.LessonOrder;

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
    FROM_TIME("from/", LESSON),
    TO_TIME("to/", LESSON),
    SUBJECT("s/", STUDENT, PARENT, TUTOR, LESSON),
    TUTOR_NAME("tu/", STUDENT, PARENT, LESSON),
    TUTOR_PHONE("tp/", LESSON),
    ROOM("rm/", STUDENT, PARENT, TUTOR, LESSON),
    LESSON_ORDER("sort/", LESSON);

    private final String prefix;
    private final Set<SearchCategory> supportedCategories;

    SearchField(String prefix, SearchCategory... categories) {
        this.prefix = prefix;
        supportedCategories = Set.of(categories);
    }

    public String getPrefix() {
        return prefix;
    }

    /**
     * Returns an immutable set of categories accepting this filter.
     */
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
     *
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
     * Normalizes exact values through their domain types and leaves text queries as permissive fragments.
     * Search-specific errors and canonical query strings remain independent of domain display messages.
     */
    String normalizeValue(String value) {
        requireNonNull(value);
        String trimmed = value.strip();
        if (trimmed.isEmpty() || value.contains("/") || value.contains("\n") || value.contains("\r")) {
            throw new IllegalArgumentException("Search filter '" + prefix
                    + "' requires a non-blank value without slashes or line breaks.");
        }

        return switch (this) {
            case LESSON_ORDER -> LessonOrder.fromValue(trimmed).name().toLowerCase(Locale.ROOT);
            case PHONE, PARENT_PHONE, TUTOR_PHONE -> {
                try {
                    yield new Phone(trimmed).value;
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Search filter '" + prefix + "' requires 3-15 digits.");
                }
            }
            case LEVEL -> {
                try {
                    String level = new EducationLevel(trimmed).getValue();
                    // Domain trimming must not accept controls outside the search whitespace boundary.
                    checkArgument(level.equals(trimmed.toUpperCase(Locale.ROOT)), EducationLevel.MESSAGE_CONSTRAINTS);
                    yield level;
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Search filter 'l/' requires P1-P6, S1-S5, JC1 or JC2.");
                }
            }
            case DAY -> {
                try {
                    String day = LessonDay.fromString(trimmed).toString().toLowerCase(Locale.ROOT);
                    // Retain the canonical query spelling even for Unicode case-folding aliases.
                    checkArgument(day.equals(trimmed.toLowerCase(Locale.ROOT)), LessonDay.MESSAGE_CONSTRAINTS);
                    yield day;
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Search filter 'd/' requires "
                            + "Mon, Tue, Wed, Thu, Fri, Sat or Sun.");
                }
            }
            case START_TIME, END_TIME, FROM_TIME, TO_TIME -> {
                try {
                    yield new LessonTime(trimmed).toString();
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Search filter '" + prefix
                            + "' requires a valid four-digit HHMM.");
                }
            }
            default -> normalizeTextCase(StringUtil.normalizeSpaces(trimmed));
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
            case PHONE, PARENT_PHONE, TUTOR_PHONE, LEVEL, DAY, START_TIME, END_TIME, FROM_TIME, TO_TIME -> {
                try {
                    yield query.equals(normalizeValue(candidate));
                } catch (IllegalArgumentException e) {
                    yield false;
                }
            }
            default -> normalizeTextCase(candidate).contains(query);
        };
    }

    /**
     * Retains locale-independent lowercase comparison while treating both lowercase sigma forms alike.
     * This search-only policy does not expand sharp S, strip accents or normalize canonical spelling.
     */
    private static String normalizeTextCase(String value) {
        return value.toLowerCase(Locale.ROOT).replace('ς', 'σ');
    }
}
