package seedu.address.model.search;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import seedu.address.commons.util.ToStringBuilder;

/**
 * Immutable, validated category and filters for a future search command.
 * This value object contains no records or writable model state. Relationship traversal is left to query integration.
 */
public final class SearchCriteria {
    private final SearchCategory category;
    private final Map<SearchField, String> filters;

    /**
     * Constructs criteria with a defensive copy of normalized filters.
     *
     * @throws IllegalArgumentException If a filter is incompatible, invalid or has an invalid time range.
     */
    public SearchCriteria(SearchCategory category, Map<SearchField, String> filters) {
        this.category = requireNonNull(category);
        requireNonNull(filters);
        Map<SearchField, String> normalized = new EnumMap<>(SearchField.class);
        filters.forEach((field, value) -> {
            requireNonNull(field);
            if (!field.isSupportedBy(category)) {
                throw new IllegalArgumentException("Search prefix '" + field.getPrefix()
                        + "' is not supported for " + category + ".");
            }
            normalized.put(field, field.normalizeValue(value));
        });
        String start = normalized.get(SearchField.START_TIME);
        String end = normalized.get(SearchField.END_TIME);
        // Validated four-digit HHMM values have the same lexicographic and chronological order.
        if (start != null && end != null && end.compareTo(start) <= 0) {
            throw new IllegalArgumentException("Search end time must be later than start time on the same day.");
        }
        this.filters = Collections.unmodifiableMap(normalized);
    }

    public SearchCategory getCategory() {
        return category;
    }

    /** Returns the immutable map of supplied filters and their normalized values. */
    public Map<SearchField, String> getFilters() {
        return filters;
    }

    /** Returns a filter's normalized query value, if supplied. */
    public Optional<String> getValue(SearchField field) {
        return Optional.ofNullable(filters.get(requireNonNull(field)));
    }

    /**
     * Tests one candidate field using its partial-text or exact-value matching rule.
     * An omitted filter imposes no restriction. Callers must combine supplied conditions against the same record,
     * linked student and lesson as appropriate; this helper does not traverse relationships.
     */
    public boolean matches(SearchField field, String candidate) {
        String query = filters.get(requireNonNull(field));
        return query == null || field.matchesValue(query, candidate);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof SearchCriteria otherCriteria)) {
            return false;
        }
        return category == otherCriteria.category && filters.equals(otherCriteria.filters);
    }

    @Override
    public int hashCode() {
        return 31 * category.hashCode() + filters.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("category", category).add("filters", filters).toString();
    }
}
