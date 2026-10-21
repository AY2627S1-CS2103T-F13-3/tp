package ponhub.logic.parser;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ponhub.logic.parser.exceptions.ParseException;

/**
 * Extracts the shared Student selector and prefixed values for attendance commands.
 */
final class AttendanceArguments {
    private static final Pattern PREFIX_PATTERN = Pattern.compile("(?<!\\S)([^\\s/]+/)",
            Pattern.UNICODE_CHARACTER_CLASS);

    private final String commandName;
    private final String studentIndex;
    private final Map<String, String> values;

    private AttendanceArguments(String commandName, String studentIndex, Map<String, String> values) {
        this.commandName = commandName;
        this.studentIndex = studentIndex;
        this.values = values;
    }

    /**
     * Extracts a leading Student index and one nonblank value per supported prefix.
     */
    static AttendanceArguments extract(String arguments, String commandName, Set<String> allowedPrefixes,
            String argumentsMessage) throws ParseException {
        Map<String, String> values = new LinkedHashMap<>();
        Matcher matcher = PREFIX_PATTERN.matcher(arguments);
        if (!matcher.find()) {
            throw new ParseException(argumentsMessage);
        }
        String studentIndex = ParserUtil.stripUnicodeBoundaryWhitespace(arguments.substring(0, matcher.start()));
        if (studentIndex.isEmpty()) {
            throw new ParseException(argumentsMessage);
        }

        boolean hasNext;
        do {
            String prefix = matcher.group(1);
            int valueStart = matcher.end();
            hasNext = matcher.find();
            int valueEnd = hasNext ? matcher.start() : arguments.length();
            if (!allowedPrefixes.contains(prefix)) {
                throw new ParseException("Unknown " + commandName + " prefix '" + prefix + "'.");
            }
            if (values.containsKey(prefix)) {
                throw new ParseException("Repeated " + commandName + " prefix '" + prefix + "'.");
            }
            String value = ParserUtil.stripUnicodeBoundaryWhitespace(arguments.substring(valueStart, valueEnd));
            if (value.isEmpty()) {
                throw new ParseException(commandNameCapitalized(commandName) + " prefix '" + prefix
                        + "' requires a non-blank value.");
            }
            values.put(prefix, value);
        } while (hasNext);
        return new AttendanceArguments(commandName, studentIndex, values);
    }

    String studentIndex() {
        return studentIndex;
    }

    /**
     * Returns a required value or reports its missing prefix.
     */
    String requireValue(String prefix) throws ParseException {
        String value = values.get(prefix);
        if (value == null) {
            throw new ParseException("Missing required " + commandName + " prefix '" + prefix + "'.");
        }
        return value;
    }

    private static String commandNameCapitalized(String commandName) {
        return Character.toUpperCase(commandName.charAt(0)) + commandName.substring(1);
    }
}
