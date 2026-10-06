package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.search.SearchCategory;
import seedu.address.model.search.SearchCriteria;
import seedu.address.model.search.SearchField;

/**
 * Parses search arguments into validated criteria independently of commands and the model runtime.
 * All prefix-shaped tokens are recognized so unknown prefixes cannot silently become part of another filter's text.
 */
public class SearchCriteriaParser {
    private static final Pattern PREFIX_PATTERN = Pattern.compile("(?<!\\S)([^\\s/]+/)");

    /**
     * Parses arguments of the form {@code c/CATEGORY [PREFIX/VALUE]...}, excluding the command word.
     * Prefixes are lowercase and may occur in any order. Category-only searches are valid.
     * @throws ParseException If the input contains missing, blank, repeated, unsupported or invalid fields.
     */
    public SearchCriteria parse(String args) throws ParseException {
        requireNonNull(args);
        if (args.contains("\n") || args.contains("\r")) {
            throw new ParseException("Search arguments must not contain line breaks.");
        }
        if (args.isBlank()) {
            throw new ParseException("Search category c/ is required.");
        }
        Matcher matcher = PREFIX_PATTERN.matcher(args);
        if (!matcher.find() || !args.substring(0, matcher.start()).isBlank()) {
            throw new ParseException("Search arguments must start with a prefix, for example c/student.");
        }

        String category = null;
        Map<SearchField, String> filters = new EnumMap<>(SearchField.class);
        boolean hasNext;
        do {
            String prefix = matcher.group(1);
            int valueStart = matcher.end();
            hasNext = matcher.find();
            int valueEnd = hasNext ? matcher.start() : args.length();
            String value = args.substring(valueStart, valueEnd).strip();
            if (value.isEmpty()) {
                throw new ParseException("Search prefix '" + prefix + "' requires a non-blank value.");
            }
            if (prefix.equals("c/")) {
                if (category != null) {
                    throw new ParseException("Repeated search prefix 'c/'.");
                }
                category = value;
            } else {
                SearchField field;
                try {
                    field = SearchField.fromPrefix(prefix);
                } catch (IllegalArgumentException e) {
                    throw new ParseException(e.getMessage(), e);
                }
                if (filters.putIfAbsent(field, value) != null) {
                    throw new ParseException("Repeated search prefix '" + prefix + "'.");
                }
            }
        } while (hasNext);

        if (category == null) {
            throw new ParseException("Search category c/ is required.");
        }
        try {
            return new SearchCriteria(SearchCategory.fromValue(category), filters);
        } catch (IllegalArgumentException e) {
            throw new ParseException(e.getMessage(), e);
        }
    }
}
