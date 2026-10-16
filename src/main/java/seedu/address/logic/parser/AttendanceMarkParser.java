package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.attendance.Attendance;
import seedu.address.model.attendance.AttendanceStatus;
import seedu.address.model.lesson.LessonId;

/**
 * Parses the arguments for recording or correcting one Student's dated lesson attendance.
 */
public final class AttendanceMarkParser {
    public static final String MESSAGE_ARGUMENTS =
            "Supply STUDENT_INDEX lid/LESSON_ID d/YYYY-MM-DD s/present|absent, "
                    + "for example 1 lid/L1 d/2026-10-05 s/present.";
    public static final String MESSAGE_SINGLE_LINE =
            "Mark arguments must be on one line and use spaces rather than control characters.";

    private static final Set<String> PREFIXES = Set.of("lid/", "d/", "s/");
    private static final Pattern PREFIX_PATTERN = Pattern.compile("(?<!\\S)([^\\s/]+/)",
            Pattern.UNICODE_CHARACTER_CLASS);
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

        ParsedArguments parsed = extractValues(arguments);
        Index studentIndex = ParserUtil.parseIndex(parsed.studentIndex());
        try {
            LessonId lessonId = new LessonId(requireValue(parsed.values(), "lid/"));
            LocalDate date = Attendance.parseDate(requireValue(parsed.values(), "d/"));
            AttendanceStatus status = AttendanceStatus.parse(requireValue(parsed.values(), "s/"));
            return new AttendanceMarkInput(studentIndex, lessonId, date, status);
        } catch (IllegalArgumentException exception) {
            throw new ParseException(exception.getMessage(), exception);
        }
    }

    /**
     * Extracts the leading Student index and one nonblank value per supported prefix.
     */
    private static ParsedArguments extractValues(String arguments) throws ParseException {
        Map<String, String> values = new LinkedHashMap<>();
        Matcher matcher = PREFIX_PATTERN.matcher(arguments);
        if (!matcher.find()) {
            throw new ParseException(MESSAGE_ARGUMENTS);
        }
        String studentIndex = ParserUtil.stripUnicodeBoundaryWhitespace(arguments.substring(0, matcher.start()));
        if (studentIndex.isEmpty()) {
            throw new ParseException(MESSAGE_ARGUMENTS);
        }

        boolean hasNext;
        do {
            String prefix = matcher.group(1);
            int valueStart = matcher.end();
            hasNext = matcher.find();
            int valueEnd = hasNext ? matcher.start() : arguments.length();
            if (!PREFIXES.contains(prefix)) {
                throw new ParseException("Unknown mark prefix '" + prefix + "'.");
            }
            if (values.containsKey(prefix)) {
                throw new ParseException("Repeated mark prefix '" + prefix + "'.");
            }
            String value = ParserUtil.stripUnicodeBoundaryWhitespace(arguments.substring(valueStart, valueEnd));
            if (value.isEmpty()) {
                throw new ParseException("Mark prefix '" + prefix + "' requires a non-blank value.");
            }
            values.put(prefix, value);
        } while (hasNext);
        return new ParsedArguments(studentIndex, values);
    }

    /**
     * Returns a required value or reports its missing prefix.
     */
    private static String requireValue(Map<String, String> values, String prefix) throws ParseException {
        String value = values.get(prefix);
        if (value == null) {
            throw new ParseException("Missing required mark prefix '" + prefix + "'.");
        }
        return value;
    }

    /**
     * Parsed raw attendance arguments before value-object validation.
     */
    private record ParsedArguments(String studentIndex, Map<String, String> values) {
    }
}
