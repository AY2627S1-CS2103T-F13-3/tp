package ponhub.logic.parser;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.lesson.LessonDay;
import ponhub.model.lesson.LessonId;
import ponhub.model.lesson.LessonTime;
import ponhub.model.lesson.Room;
import ponhub.model.lesson.Subject;
import ponhub.model.person.Name;
import ponhub.model.person.Phone;

/** Parses a stable Lesson selector and partial replacement fields without accessing model state. */
public final class LessonEditParser {
    public static final String MESSAGE_ARGUMENTS =
            "Supply lid/LESSON_ID and at least one editable field. Type 'help editlesson' for syntax.";
    public static final String MESSAGE_SINGLE_LINE =
            "Editlesson arguments must be on one line and use spaces rather than control characters.";
    public static final String MESSAGE_TUTOR_PHONE =
            "Prefix 'tp/' can only be used together with 'tu/' when editing a lesson tutor.";

    private static final Set<String> PREFIXES = Set.of("lid/", "d/", "st/", "et/", "s/", "tu/", "tp/", "rm/");
    private static final Pattern PREFIX_PATTERN = Pattern.compile("(?<!\\S)([^\\s/]+/)",
            Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\R");

    /**
     * Parses distinct lowercase prefixes in any order. Every supplied value must be nonblank.
     * A single time endpoint remains valid parser input because the retained endpoint is checked by the candidate.
     *
     * @throws ParseException if the selector, prefixes or supplied values are invalid.
     */
    public Input parse(String arguments) throws ParseException {
        requireNonNull(arguments);
        if (LINE_BREAK_PATTERN.matcher(arguments).find()
                || arguments.codePoints().anyMatch(Character::isISOControl)) {
            throw new ParseException(MESSAGE_SINGLE_LINE);
        }
        Map<String, String> values = extractValues(arguments);
        if (!values.containsKey("lid/") || values.size() == 1) {
            throw new ParseException(MESSAGE_ARGUMENTS);
        }
        if (values.containsKey("tp/") && !values.containsKey("tu/")) {
            throw new ParseException(MESSAGE_TUTOR_PHONE);
        }
        try {
            return new Input(new LessonId(values.get("lid/")), optional(values, "d/", LessonDay::fromString),
                    optional(values, "st/", LessonTime::new), optional(values, "et/", LessonTime::new),
                    optional(values, "s/", Subject::new), optional(values, "tu/", Name::new),
                    optional(values, "tp/", Phone::new), optional(values, "rm/", Room::new));
        } catch (IllegalArgumentException failure) {
            throw new ParseException(failure.getMessage(), failure);
        }
    }

    private static Map<String, String> extractValues(String arguments) throws ParseException {
        Map<String, String> values = new LinkedHashMap<>();
        Matcher matcher = PREFIX_PATTERN.matcher(arguments);
        if (!matcher.find()
                || !ParserUtil.stripUnicodeBoundaryWhitespace(arguments.substring(0, matcher.start())).isEmpty()) {
            throw new ParseException(MESSAGE_ARGUMENTS);
        }
        boolean hasNext;
        do {
            String prefix = matcher.group(1);
            int valueStart = matcher.end();
            hasNext = matcher.find();
            int valueEnd = hasNext ? matcher.start() : arguments.length();
            if (!PREFIXES.contains(prefix)) {
                throw new ParseException("Unknown editlesson prefix '" + prefix + "'.");
            }
            if (values.containsKey(prefix)) {
                throw new ParseException("Repeated editlesson prefix '" + prefix + "'.");
            }
            String value = ParserUtil.stripUnicodeBoundaryWhitespace(arguments.substring(valueStart, valueEnd));
            if (value.isEmpty()) {
                throw new ParseException("Editlesson prefix '" + prefix + "' requires a non-blank value.");
            }
            values.put(prefix, value);
        } while (hasNext);
        return values;
    }

    private static <T> Optional<T> optional(Map<String, String> values, String prefix, ValueParser<T> parser) {
        return values.containsKey(prefix) ? Optional.of(parser.parse(values.get(prefix))) : Optional.empty();
    }

    /** Immutable request retaining tutor lookup values until staged command execution. */
    public record Input(LessonId lessonId, Optional<LessonDay> day, Optional<LessonTime> startTime,
            Optional<LessonTime> endTime, Optional<Subject> subject, Optional<Name> tutorName,
            Optional<Phone> tutorPhone, Optional<Room> room) {
        public Input {
            requireAllNonNull(lessonId, day, startTime, endTime, subject, tutorName, tutorPhone, room);
        }
    }

    @FunctionalInterface
    private interface ValueParser<T> {
        T parse(String value);
    }
}
