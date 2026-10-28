package ponhub.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.search.SearchCategory;
import ponhub.model.search.SearchCriteria;
import ponhub.model.search.SearchField;

/**
 * Parses search arguments into validated criteria independently of commands and the model runtime.
 * All prefix-shaped tokens are recognized so unknown prefixes cannot silently become part of another filter's text.
 */
public class SearchCriteriaParser {
    private static final Pattern PREFIX_PATTERN = Pattern.compile("(?<!\\S)([^\\s/]+/)",
            Pattern.UNICODE_CHARACTER_CLASS);

    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\R");

    /**
     * Parses arguments of the form {@code c/CATEGORY [PREFIX/VALUE]...}, excluding the command word.
     * Prefixes are lowercase and may occur in any order. Category-only searches are valid.
     *
     * @throws ParseException If the input contains missing, blank, repeated, unsupported or invalid fields.
     */
    public SearchCriteria parse(String args) throws ParseException {
        requireNonNull(args);
        if (LINE_BREAK_PATTERN.matcher(args).find()) {
            throw new ParseException("Search arguments must not contain line breaks.");
        }
        if (ParserUtil.stripUnicodeBoundaryWhitespace(args).isEmpty()) {
            throw new ParseException("Search category c/ is required.");
        }
        Matcher matcher = PREFIX_PATTERN.matcher(args);
        if (!matcher.find()
                || !ParserUtil.stripUnicodeBoundaryWhitespace(args.substring(0, matcher.start())).isEmpty()) {
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
            String value = ParserUtil.stripUnicodeBoundaryWhitespace(args.substring(valueStart, valueEnd));
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
