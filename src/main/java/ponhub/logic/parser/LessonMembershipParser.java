package ponhub.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ponhub.commons.core.index.Index;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.lesson.LessonId;

/**
 * Parses the shared {@code STUDENT_INDEX lid/LESSON_ID} membership command arguments.
 */
public final class LessonMembershipParser {
    public static final String MESSAGE_ARGUMENTS =
            "Supply STUDENT_INDEX lid/LESSON_ID, for example 1 lid/L1.";

    private static final Pattern ARGUMENTS_FORMAT =
            Pattern.compile("[\\p{Zs}\\t]*([0-9]+)[\\p{Zs}\\t]+lid/([^\\p{Zs}\\t]+)[\\p{Zs}\\t]*");
    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\R");

    /**
     * Parses one current people-list index and one stable Lesson ID without reading operational state.
     *
     * @throws NullPointerException if the arguments are null.
     * @throws ParseException if the arguments do not use the exact supported format or contain invalid selectors.
     */
    public LessonMembershipInput parse(String arguments) throws ParseException {
        requireNonNull(arguments);
        if (LINE_BREAK_PATTERN.matcher(arguments).find()) {
            throw new ParseException(MESSAGE_ARGUMENTS);
        }
        Matcher matcher = ARGUMENTS_FORMAT.matcher(arguments);
        if (!matcher.matches()) {
            throw new ParseException(MESSAGE_ARGUMENTS);
        }
        Index studentIndex = ParserUtil.parseIndex(matcher.group(1));
        try {
            return new LessonMembershipInput(studentIndex, new LessonId(matcher.group(2)));
        } catch (IllegalArgumentException exception) {
            throw new ParseException(exception.getMessage(), exception);
        }
    }
}
