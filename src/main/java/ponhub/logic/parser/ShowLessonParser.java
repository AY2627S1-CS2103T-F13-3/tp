package ponhub.logic.parser;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.attendance.Attendance;
import ponhub.model.lesson.LessonId;

/**
 * Parses one stable lesson ID and an optional real occurrence date.
 */
public final class ShowLessonParser {
    public static final String MESSAGE_USAGE = "Usage: showlesson lid/LESSON_ID [d/YYYY-MM-DD]";

    private static final Pattern ARGUMENTS = Pattern.compile(
            "(?:lid/(\\S+)(?:\\p{Zs}+d/(\\S+))?|d/(\\S+)\\p{Zs}+lid/(\\S+))", Pattern.UNICODE_CHARACTER_CLASS);

    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\R");

    /**
     * Rejects missing, repeated or unknown fields before the selected view changes.
     */
    public ShowLessonInput parse(String arguments) throws ParseException {
        requireNonNull(arguments);
        if (LINE_BREAK_PATTERN.matcher(arguments).find()
                || arguments.codePoints().anyMatch(Character::isISOControl)) {
            throw new ParseException(MESSAGE_USAGE);
        }
        Matcher matcher = ARGUMENTS.matcher(ParserUtil.stripUnicodeBoundaryWhitespace(arguments));
        if (!matcher.matches()) {
            throw new ParseException(MESSAGE_USAGE);
        }
        try {
            String suppliedId = matcher.group(1) != null ? matcher.group(1) : matcher.group(4);
            LessonId lessonId = new LessonId(suppliedId);
            String suppliedDate = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            Optional<LocalDate> date = suppliedDate == null ? Optional.empty()
                    : Optional.of(Attendance.parseDate(suppliedDate));
            return new ShowLessonInput(lessonId, date);
        } catch (IllegalArgumentException exception) {
            throw new ParseException(exception.getMessage(), exception);
        }
    }
}
