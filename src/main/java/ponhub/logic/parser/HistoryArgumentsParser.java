package ponhub.logic.parser;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import ponhub.commons.core.index.Index;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.attendance.Attendance;
import ponhub.model.attendance.AttendanceStatus;
import ponhub.model.lesson.LessonId;

/**
 * Parses a current people-list Student index and optional Lesson, exact date and status filters.
 */
public final class HistoryArgumentsParser {
    public static final String MESSAGE_USAGE =
            "Format: history STUDENT_INDEX [lid/LESSON_ID] [d/YYYY-MM-DD] [s/present|absent]";
    private static final Set<String> PREFIXES = Set.of("lid/", "d/", "s/");

    /**
     * Returns a positive current people index and validated optional filters in any order.
     */
    public Input parse(String arguments) throws ParseException {
        requireNonNull(arguments);
        if (arguments.codePoints().anyMatch(Character::isISOControl)) {
            throw new ParseException(MESSAGE_USAGE);
        }
        String stripped = arguments.strip();
        if (stripped.matches("[0-9]+")) {
            return new Input(ParserUtil.parseIndex(stripped), Optional.empty(), Optional.empty(), Optional.empty());
        }
        AttendanceArguments parsed = AttendanceArguments.extract(stripped, "history", PREFIXES, MESSAGE_USAGE);
        Index studentIndex = ParserUtil.parseIndex(parsed.studentIndex());
        try {
            return new Input(studentIndex, parsed.optionalValue("lid/").map(LessonId::new),
                    parsed.optionalValue("d/").map(Attendance::parseDate),
                    parsed.optionalValue("s/").map(AttendanceStatus::parse));
        } catch (IllegalArgumentException exception) {
            throw new ParseException(exception.getMessage(), exception);
        }
    }

    /**
     * Parsed command selectors, prior to current-view role and reference validation.
     */
    public record Input(Index studentIndex, Optional<LessonId> lessonId, Optional<LocalDate> date,
            Optional<AttendanceStatus> status) {
    }
}
