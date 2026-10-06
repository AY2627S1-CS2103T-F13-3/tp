package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.PersonRole;

/**
 * A detached parser for the proposed people-list arguments, without command routing or view changes.
 */
public final class PeopleListParser {

    public static final String MESSAGE_USAGE = "Use list or list r/student|tutor|parent, with at most one role filter.";

    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\R");
    private static final Pattern ROLE_FILTER_PATTERN = Pattern.compile("[ \\t]*r/[ \\t]*([A-Za-z]+)[ \\t]*");

    /**
     * Parses arguments after the command word into an optional role filter.
     * A blank single-line input selects all roles; a filter uses lowercase {@code r/} and a case-insensitive role.
     *
     * @throws NullPointerException if the arguments are null.
     * @throws ParseException if the arguments contain line breaks or anything other than one supported role filter.
     */
    public Optional<PersonRole> parse(String arguments) throws ParseException {
        requireNonNull(arguments);
        if (LINE_BREAK_PATTERN.matcher(arguments).find()) {
            throw new ParseException("People-list arguments must be on one line. " + MESSAGE_USAGE);
        }

        if (arguments.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = ROLE_FILTER_PATTERN.matcher(arguments);
        if (!matcher.matches()) {
            throw new ParseException(MESSAGE_USAGE);
        }

        try {
            return Optional.of(PersonRole.parse(matcher.group(1)));
        } catch (IllegalArgumentException exception) {
            throw new ParseException(PersonRole.MESSAGE_CONSTRAINTS + " " + MESSAGE_USAGE, exception);
        }
    }
}
