package ponhub.logic.parser;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ponhub.commons.core.index.Index;
import ponhub.logic.parser.PersonEditInput.Field;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.person.EducationLevel;

/**
 * Parses a displayed people position and partial field operations without accessing the model.
 */
public final class PersonEditParser {
    public static final String MESSAGE_ARGUMENTS =
            "Supply INDEX and at least one editable field, for example 1 n/Alex Tan. Type 'help edit' for syntax.";
    public static final String MESSAGE_SINGLE_LINE =
            "Edit arguments must be on one line and use spaces rather than control characters.";

    private static final Set<String> PREFIXES = Set.of("n/", "p/", "e/", "a/", "l/", "pp/");
    private static final Pattern PREFIX_PATTERN = Pattern.compile("(?<!\\S)([^\\s/]+/)",
            Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\R");

    /**
     * Parses one position and distinct lowercase prefixes in any order. Blank values remain explicit clears;
     * the candidate validates required fields and Student-only fields against the selected person's role.
     *
     * @throws ParseException if the selector, prefixes or supplied values are invalid.
     */
    public Input parse(String arguments) throws ParseException {
        requireNonNull(arguments);
        if (LINE_BREAK_PATTERN.matcher(arguments).find()
                || arguments.codePoints().anyMatch(Character::isISOControl)) {
            throw new ParseException(MESSAGE_SINGLE_LINE);
        }
        Matcher matcher = PREFIX_PATTERN.matcher(arguments);
        if (!matcher.find()) {
            throw new ParseException(MESSAGE_ARGUMENTS);
        }
        Index index = ParserUtil.parseIndex(
                ParserUtil.stripUnicodeBoundaryWhitespace(arguments.substring(0, matcher.start())));
        Map<String, String> values = new LinkedHashMap<>();
        boolean hasNext;
        do {
            String prefix = matcher.group(1);
            int valueStart = matcher.end();
            hasNext = FieldValueTokenizer.findNextPrefix(matcher, arguments, prefix);
            int valueEnd = hasNext ? matcher.start() : arguments.length();
            if (!PREFIXES.contains(prefix)) {
                throw new ParseException("Unknown edit prefix '" + prefix + "'.");
            }
            if (values.containsKey(prefix)) {
                throw new ParseException("Repeated edit prefix '" + prefix + "'.");
            }
            values.put(prefix, ParserUtil.stripUnicodeBoundaryWhitespace(arguments.substring(valueStart, valueEnd)));
        } while (hasNext);
        try {
            return new Input(index, new PersonEditInput(field(values, "n/", ParserUtil::parseName),
                    field(values, "p/", ParserUtil::parseContactPhone), field(values, "e/", ParserUtil::parseEmail),
                    field(values, "a/", ParserUtil::parseAddress), field(values, "l/", EducationLevel::new),
                    field(values, "pp/", ParserUtil::parseContactPhone)));
        } catch (IllegalArgumentException failure) {
            throw new ParseException(failure.getMessage(), failure);
        }
    }

    private static <T> Field<T> field(Map<String, String> values, String prefix, ValueParser<T> parser)
            throws ParseException {
        if (!values.containsKey(prefix)) {
            return Field.keep();
        }
        String value = values.get(prefix);
        return value.isEmpty() ? Field.clear() : Field.set(parser.parse(value));
    }

    /**
     * Immutable edit request retaining the displayed selector until staged command execution.
     */
    public record Input(Index index, PersonEditInput edit) {
        public Input {
            requireAllNonNull(index, edit);
        }
    }

    /**
     * Converts one supplied edit value, reporting invalid input as a parse failure.
     */
    @FunctionalInterface
    private interface ValueParser<T> {
        T parse(String value) throws ParseException;
    }
}
