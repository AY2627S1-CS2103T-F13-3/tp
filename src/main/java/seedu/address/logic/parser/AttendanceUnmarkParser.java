package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.Set;
import java.util.regex.Pattern;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.attendance.Attendance;
import seedu.address.model.lesson.LessonId;

/**
 * Parses the arguments for removing one Student's dated lesson attendance.
 */
public final class AttendanceUnmarkParser {
    public static final String MESSAGE_ARGUMENTS =
            "Supply STUDENT_INDEX lid/LESSON_ID d/YYYY-MM-DD, "
                    + "for example 1 lid/L1 d/2026-10-05.";
    public static final String MESSAGE_SINGLE_LINE =
            "Unmark arguments must be on one line and use spaces rather than control characters.";

    private static final Set<String> PREFIXES = Set.of("lid/", "d/");
    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\R");

    /**
     * Parses a current people-list position, stable Lesson ID and attendance date.
     * Prefixes are lowercase and may occur once in any order.
     *
     * @throws NullPointerException if the arguments are null.
     * @throws ParseException if a selector or value is missing, repeated, unsupported or invalid.
     */
    public AttendanceUnmarkInput parse(String arguments) throws ParseException {
        requireNonNull(arguments);
        if (LINE_BREAK_PATTERN.matcher(arguments).find()
                || arguments.codePoints().anyMatch(Character::isISOControl)) {
            throw new ParseException(MESSAGE_SINGLE_LINE);
        }

        AttendanceArguments parsed = AttendanceArguments.extract(arguments, "unmark", PREFIXES, MESSAGE_ARGUMENTS);
        Index studentIndex = ParserUtil.parseIndex(parsed.studentIndex());
        try {
            LessonId lessonId = new LessonId(parsed.requireValue("lid/"));
            LocalDate date = Attendance.parseDate(parsed.requireValue("d/"));
            return new AttendanceUnmarkInput(studentIndex, lessonId, date);
        } catch (IllegalArgumentException exception) {
            throw new ParseException(exception.getMessage(), exception);
        }
    }
}
