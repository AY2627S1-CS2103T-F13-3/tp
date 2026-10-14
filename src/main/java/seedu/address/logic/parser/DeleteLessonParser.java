package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.lesson.LessonId;

/**
 * Parses the {@code lid/LESSON_ID} shared-lesson deletion arguments.
 */
public final class DeleteLessonParser {
    public static final String MESSAGE_ARGUMENTS =
            "Supply lid/LESSON_ID, for example lid/L1.";

    private static final Pattern ARGUMENTS_FORMAT = Pattern.compile("[ \\t]*lid/([^ \\t]+)[ \\t]*");
    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\R");

    /**
     * Parses one stable Lesson ID without reading operational state.
     *
     * @throws NullPointerException if the arguments are null.
     * @throws ParseException if the arguments do not use the exact supported format or the ID is invalid.
     */
    public LessonId parse(String arguments) throws ParseException {
        requireNonNull(arguments);
        if (LINE_BREAK_PATTERN.matcher(arguments).find()) {
            throw new ParseException(MESSAGE_ARGUMENTS);
        }
        Matcher matcher = ARGUMENTS_FORMAT.matcher(arguments);
        if (!matcher.matches()) {
            throw new ParseException(MESSAGE_ARGUMENTS);
        }
        try {
            return new LessonId(matcher.group(1));
        } catch (IllegalArgumentException exception) {
            throw new ParseException(exception.getMessage(), exception);
        }
    }
}
