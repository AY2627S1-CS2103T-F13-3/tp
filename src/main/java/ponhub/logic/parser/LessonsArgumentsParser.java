package ponhub.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ponhub.commons.core.index.Index;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.query.LessonOrder;

/**
 * Parses optional Student and sort arguments for {@code lessons}, retaining the legacy {@code si/} alias.
 */
public final class LessonsArgumentsParser {
    public static final String MESSAGE_USAGE =
            "Format: lessons [STUDENT_INDEX] [sort/creation|schedule] (legacy selector: si/STUDENT_INDEX)";

    private static final Pattern STUDENT_INDEX_FORMAT = Pattern.compile("(?:si/)?([0-9]+)");
    private static final Pattern LINE_BREAK = Pattern.compile("\\R");

    /**
     * Returns an absent selector for the full catalogue, or a positive current people-list index.
     * Role and range are checked by the retrieval boundary at execution time.
     */
    public Optional<Index> parse(String arguments) throws ParseException {
        return parseInput(arguments).studentIndex();
    }

    /**
     * Parses the optional selector and trailing sort prefix, retaining creation order by default.
     */
    public LessonsInput parseInput(String arguments) throws ParseException {
        requireNonNull(arguments);
        if (LINE_BREAK.matcher(arguments).find()) {
            throw new ParseException(MESSAGE_USAGE);
        }
        String trimmed = ParserUtil.stripUnicodeBoundaryWhitespace(arguments);
        LessonOrder order = LessonOrder.CREATION;
        int sortStart = trimmed.indexOf("sort/");
        if (sortStart >= 0) {
            if (sortStart > 0 && !(Character.isWhitespace(trimmed.charAt(sortStart - 1))
                    || Character.isSpaceChar(trimmed.charAt(sortStart - 1)))) {
                throw new ParseException(MESSAGE_USAGE);
            }
            try {
                order = LessonOrder.fromValue(ParserUtil.stripUnicodeBoundaryWhitespace(
                        trimmed.substring(sortStart + "sort/".length())));
            } catch (IllegalArgumentException failure) {
                throw new ParseException(MESSAGE_USAGE, failure);
            }
            trimmed = ParserUtil.stripUnicodeBoundaryWhitespace(trimmed.substring(0, sortStart));
        }
        if (trimmed.isEmpty()) {
            return new LessonsInput(Optional.empty(), order);
        }
        Matcher matcher = STUDENT_INDEX_FORMAT.matcher(trimmed);
        if (!matcher.matches()) {
            throw new ParseException(MESSAGE_USAGE);
        }
        return new LessonsInput(Optional.of(ParserUtil.parseIndex(matcher.group(1))), order);
    }
}
