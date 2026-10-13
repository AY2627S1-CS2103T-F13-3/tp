package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.attendance.Attendance;
import seedu.address.model.lesson.LessonId;

/** Parses one stable lesson ID and an optional real occurrence date. */
public final class ShowLessonParser {
    public static final String MESSAGE_USAGE = "Usage: showlesson lid/LESSON_ID [d/YYYY-MM-DD]";

    private static final Pattern ARGUMENTS = Pattern.compile("lid/(\\S+)(?: +d/(\\S+))?");

    /** Rejects missing, repeated or unknown fields before the selected view changes. */
    public ShowLessonInput parse(String arguments) throws ParseException {
        requireNonNull(arguments);
        if (arguments.codePoints().anyMatch(Character::isISOControl)) {
            throw new ParseException(MESSAGE_USAGE);
        }
        Matcher matcher = ARGUMENTS.matcher(arguments.strip());
        if (!matcher.matches()) {
            throw new ParseException(MESSAGE_USAGE);
        }
        try {
            LessonId lessonId = new LessonId(matcher.group(1));
            String suppliedDate = matcher.group(2);
            Optional<LocalDate> date = suppliedDate == null ? Optional.empty()
                    : Optional.of(Attendance.parseDate(suppliedDate));
            return new ShowLessonInput(lessonId, date);
        } catch (IllegalArgumentException exception) {
            throw new ParseException(exception.getMessage(), exception);
        }
    }
}
