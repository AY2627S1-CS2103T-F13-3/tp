package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.parser.exceptions.ParseException;

/** Parses the prepared {@code lessons [si/STUDENT_INDEX]} argument format. */
public final class LessonsArgumentsParser {
    public static final String MESSAGE_USAGE = "Format: lessons [si/STUDENT_INDEX]";

    private static final Pattern STUDENT_INDEX_FORMAT = Pattern.compile("si/([0-9]+)");

    /**
     * Returns an absent selector for the full catalogue, or a positive current people-list index.
     * Role and range are checked by the retrieval boundary at execution time.
     */
    public Optional<Index> parse(String arguments) throws ParseException {
        requireNonNull(arguments);
        if (arguments.contains("\n") || arguments.contains("\r")) {
            throw new ParseException(MESSAGE_USAGE);
        }
        String trimmed = arguments.trim();
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
