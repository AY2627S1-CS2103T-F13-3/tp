package ponhub.logic.parser;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.Set;
import java.util.regex.Pattern;

import ponhub.commons.core.index.Index;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.attendance.Attendance;
import ponhub.model.attendance.AttendanceStatus;
import ponhub.model.lesson.LessonId;

/**
 * Parses the arguments for recording or correcting one Student's dated lesson attendance.
 */
public final class AttendanceMarkParser {
    public static final String MESSAGE_ARGUMENTS =
            "Supply STUDENT_INDEX lid/LESSON_ID d/YYYY-MM-DD s/present|absent [ack/future], "
                    + "for example 1 lid/L1 d/2026-10-05 s/present.";
    public static final String MESSAGE_SINGLE_LINE =
            "Mark arguments must be on one line and use spaces rather than control characters.";
    public static final String MESSAGE_ACKNOWLEDGEMENT =
            "Attendance acknowledgement must be ack/future.";

    private static final Set<String> PREFIXES = Set.of("lid/", "d/", "s/", "ack/");
    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\R");

    /**
     * Parses a current people-list position and one value for each required attendance prefix.
     * Prefixes are lowercase and may occur once in any order.
     *
     * @throws NullPointerException if the arguments are null.
     * @throws ParseException if a selector or value is missing, repeated, unsupported or invalid.
     */
    public AttendanceMarkInput parse(String arguments) throws ParseException {
        requireNonNull(arguments);
        if (LINE_BREAK_PATTERN.matcher(arguments).find()
                || arguments.codePoints().anyMatch(Character::isISOControl)) {
            throw new ParseException(MESSAGE_SINGLE_LINE);
        }

        AttendanceArguments parsed = AttendanceArguments.extract(arguments, "mark", PREFIXES, MESSAGE_ARGUMENTS);
        Index studentIndex = ParserUtil.parseIndex(parsed.studentIndex());
        try {
            LessonId lessonId = new LessonId(parsed.requireValue("lid/"));
            LocalDate date = Attendance.parseDate(parsed.requireValue("d/"));
            AttendanceStatus status = AttendanceStatus.parse(parsed.requireValue("s/"));
            boolean futureAcknowledged = parsed.optionalValue("ack/").map(value -> {
                if (!value.equalsIgnoreCase("future")) {
                    throw new IllegalArgumentException(MESSAGE_ACKNOWLEDGEMENT);
                }
                return true;
            }).orElse(false);
            return new AttendanceMarkInput(studentIndex, lessonId, date, status, futureAcknowledged);
        } catch (IllegalArgumentException exception) {
            throw new ParseException(exception.getMessage(), exception);
        }
    }
}
