package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.lesson.LessonDay;
import seedu.address.model.lesson.LessonTime;
import seedu.address.model.lesson.LessonTimeSlot;
import seedu.address.model.lesson.Room;
import seedu.address.model.lesson.Subject;
import seedu.address.model.person.Name;
import seedu.address.model.person.Phone;

/**
 * Parses shared-lesson creation arguments without resolving tutors or allocating lesson IDs.
 * All prefix-shaped tokens are recognized so unsupported prefixes cannot be absorbed into a field value.
 */
public final class AddLessonParser {

    public static final String MESSAGE_SINGLE_LINE =
            "Addlesson arguments must be on one line and use spaces rather than control characters.";
    public static final String MESSAGE_PREAMBLE =
            "Addlesson arguments must start with a prefix, for example d/Mon.";

    private static final Set<String> PREFIXES = Set.of("d/", "st/", "et/", "s/", "tu/", "tp/", "rm/");
    private static final Pattern PREFIX_PATTERN = Pattern.compile("(?<!\\S)([^\\s/]+/)",
            Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\R");

    /**
     * Parses arguments after {@code addlesson} into validated lesson-creation input.
     * Prefixes are lowercase and may occur once in any order. Day and tutor-name matching ignore letter case.
     *
     * @throws NullPointerException if the arguments are null.
     * @throws ParseException if a field is unsupported, repeated, missing, blank or invalid.
     */
    public LessonCreationInput parse(String arguments) throws ParseException {
        requireNonNull(arguments);
        if (LINE_BREAK_PATTERN.matcher(arguments).find()
                || arguments.codePoints().anyMatch(Character::isISOControl)) {
            throw new ParseException(MESSAGE_SINGLE_LINE);
        }
        Map<String, String> values = extractValues(arguments);
        try {
            LessonDay day = LessonDay.fromString(requireValue(values, "d/"));
            LessonTime startTime = new LessonTime(requireValue(values, "st/"));
            LessonTime endTime = new LessonTime(requireValue(values, "et/"));
            LessonTimeSlot timeSlot = new LessonTimeSlot(day, startTime, endTime);
            Subject subject = new Subject(requireValue(values, "s/"));
            Name tutorName = new Name(requireValue(values, "tu/"));
            Optional<Phone> tutorPhone = values.containsKey("tp/")
                    ? Optional.of(new Phone(values.get("tp/"))) : Optional.empty();
            Room room = new Room(requireValue(values, "rm/"));
            return new LessonCreationInput(timeSlot, subject, tutorName, tutorPhone, room);
        } catch (IllegalArgumentException exception) {
            throw new ParseException(exception.getMessage(), exception);
        }
    }

    /**
     * Extracts one nonblank value per supported prefix.
     */
    private static Map<String, String> extractValues(String arguments) throws ParseException {
        Map<String, String> values = new LinkedHashMap<>();
        Matcher matcher = PREFIX_PATTERN.matcher(arguments);
        if (!matcher.find()) {
            if (stripBoundaryWhitespace(arguments).isEmpty()) {
                return values;
            }
            throw new ParseException(MESSAGE_PREAMBLE);
        }
        if (!stripBoundaryWhitespace(arguments.substring(0, matcher.start())).isEmpty()) {
            throw new ParseException(MESSAGE_PREAMBLE);
        }
        boolean hasNext;
        do {
            String prefix = matcher.group(1);
            int valueStart = matcher.end();
            hasNext = matcher.find();
            int valueEnd = hasNext ? matcher.start() : arguments.length();
            if (!PREFIXES.contains(prefix)) {
                throw new ParseException("Unknown addlesson prefix '" + prefix + "'.");
            }
            if (values.containsKey(prefix)) {
                throw new ParseException("Repeated addlesson prefix '" + prefix + "'.");
            }
            String value = stripBoundaryWhitespace(arguments.substring(valueStart, valueEnd));
            if (value.isEmpty()) {
                throw new ParseException("Addlesson prefix '" + prefix + "' requires a non-blank value.");
            }
            values.put(prefix, value);
        } while (hasNext);
        return values;
    }

    /**
     * Returns a required value or reports its missing prefix.
     */
    private static String requireValue(Map<String, String> values, String prefix) throws ParseException {
        String value = values.get(prefix);
        if (value == null) {
            throw new ParseException("Missing required addlesson prefix '" + prefix + "'.");
        }
        return value;
    }

    /**
     * Strips Unicode whitespace at a field boundary while preserving spaces within a value.
     */
    private static String stripBoundaryWhitespace(String value) {
        int start = 0;
        while (start < value.length()) {
            int codePoint = value.codePointAt(start);
            if (!isBoundaryWhitespace(codePoint)) {
                break;
            }
            start += Character.charCount(codePoint);
        }

        int end = value.length();
        while (end > start) {
            int codePoint = value.codePointBefore(end);
            if (!isBoundaryWhitespace(codePoint)) {
                break;
            }
            end -= Character.charCount(codePoint);
        }
        return value.substring(start, end);
    }

    private static boolean isBoundaryWhitespace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }
}
