package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.lesson.LessonId;

/**
 * Parses a current people-list Student index and an optional stable Lesson ID.
 */
public final class HistoryArgumentsParser {
    public static final String MESSAGE_USAGE = "Format: history STUDENT_INDEX [lid/LESSON_ID]";
    private static final Pattern FORMAT = Pattern.compile("([0-9]+)(?: +lid/([^\\s]+))?");

    /** Returns a positive current people index and optional validated Lesson ID. */
    public Input parse(String arguments) throws ParseException {
        requireNonNull(arguments);
        if (arguments.codePoints().anyMatch(Character::isISOControl)) {
            throw new ParseException(MESSAGE_USAGE);
        }
        Matcher matcher = FORMAT.matcher(arguments.strip());
        if (!matcher.matches()) {
            throw new ParseException(MESSAGE_USAGE);
        }
        Index studentIndex = ParserUtil.parseIndex(matcher.group(1));
        try {
            return new Input(studentIndex, matcher.group(2) == null
                    ? Optional.empty() : Optional.of(new LessonId(matcher.group(2))));
        } catch (IllegalArgumentException exception) {
            throw new ParseException(exception.getMessage(), exception);
        }
    }

    /** Parsed command selectors, prior to current-view role and reference validation. */
    public record Input(Index studentIndex, Optional<LessonId> lessonId) {
    }
}
