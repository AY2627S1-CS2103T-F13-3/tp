package ponhub.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ponhub.commons.core.index.Index;
import ponhub.logic.parser.exceptions.ParseException;

/**
 * Parses the {@code lessons [STUDENT_INDEX]} argument format and its legacy {@code si/} selector alias.
 */
public final class LessonsArgumentsParser {
    public static final String MESSAGE_USAGE =
            "Format: lessons [STUDENT_INDEX] (legacy alias: lessons si/STUDENT_INDEX)";

    private static final Pattern STUDENT_INDEX_FORMAT = Pattern.compile("(?:si/)?([0-9]+)");
    private static final Pattern LINE_BREAK = Pattern.compile("\\R");

    /**
     * Returns an absent selector for the full catalogue, or a positive current people-list index.
     * Role and range are checked by the retrieval boundary at execution time.
     */
    public Optional<Index> parse(String arguments) throws ParseException {
        requireNonNull(arguments);
        if (LINE_BREAK.matcher(arguments).find()) {
            throw new ParseException(MESSAGE_USAGE);
        }
        String trimmed = arguments.strip();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        Matcher matcher = STUDENT_INDEX_FORMAT.matcher(trimmed);
        if (!matcher.matches()) {
            throw new ParseException(MESSAGE_USAGE);
        }
        return Optional.of(ParserUtil.parseIndex(matcher.group(1)));
    }
}
