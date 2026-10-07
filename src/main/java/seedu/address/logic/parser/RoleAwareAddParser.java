package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.Address;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Phone;

/**
 * A detached parser for planned role-aware add arguments. It does not route commands or allocate person IDs.
 * All prefix-shaped tokens are recognized so unsupported prefixes cannot be absorbed into another field's text.
 */
public final class RoleAwareAddParser {

    public static final String MESSAGE_SINGLE_LINE =
            "Add arguments must be on one line and use spaces rather than control characters.";
    public static final String MESSAGE_PREAMBLE =
            "Add arguments must start with a prefix, for example r/student.";

    private static final Set<String> PREFIXES = Set.of("r/", "n/", "l/", "pp/", "p/", "e/", "a/");
    private static final Pattern PREFIX_PATTERN = Pattern.compile("(?<!\\S)([^\\s/]+/)");
    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\R");

    /**
     * Parses arguments after the command word into an immutable addition input, without accessing operational state.
     * Prefixes are lowercase and may occur once in any order; role and education-level values ignore letter case.
     * Supplied optional fields must be nonblank. Contact validation delegates to the existing shared value types.
     *
     * @throws NullPointerException if the arguments are null.
     * @throws ParseException if the arguments have unsupported, repeated, missing, blank or invalid fields.
     */
    public PersonAdditionInput parse(String arguments) throws ParseException {
        requireNonNull(arguments);
        if (LINE_BREAK_PATTERN.matcher(arguments).find()
                || arguments.codePoints().anyMatch(Character::isISOControl)) {
            throw new ParseException(MESSAGE_SINGLE_LINE);
        }
        Map<String, String> values = extractValues(arguments);
        try {
            PersonRole role = PersonRole.parse(requireValue(values, "r/"));
            if (role != PersonRole.STUDENT) {
                for (String prefix : values.keySet()) {
                    if (prefix.equals("l/") || prefix.equals("pp/")) {
                        throw new ParseException("Add prefix '" + prefix + "' is only allowed for students.");
                    }
                }
                requireValue(values, "p/");
            }

            Name name = ParserUtil.parseName(requireValue(values, "n/"));
            Optional<Phone> phone = values.containsKey("p/")
                    ? Optional.of(parsePhone(values.get("p/"))) : Optional.empty();
            Optional<Email> email = values.containsKey("e/")
                    ? Optional.of(ParserUtil.parseEmail(values.get("e/"))) : Optional.empty();
            Optional<Address> address = values.containsKey("a/")
                    ? Optional.of(ParserUtil.parseAddress(values.get("a/"))) : Optional.empty();
            ContactDetails contacts = new ContactDetails(name, phone, email, address);
            Optional<EducationLevel> level = role == PersonRole.STUDENT
                    ? Optional.of(new EducationLevel(requireValue(values, "l/"))) : Optional.empty();
            Optional<Phone> parentPhone = role == PersonRole.STUDENT
                    ? Optional.of(parsePhone(requireValue(values, "pp/"))) : Optional.empty();
            return new PersonAdditionInput(role, contacts, level, parentPhone);
        } catch (IllegalArgumentException exception) {
            throw new ParseException(exception.getMessage(), exception);
        }
    }

    /**
     * Extracts single values while rejecting every unknown, repeated or blank prefix and a nonblank preamble.
     */
    private static Map<String, String> extractValues(String arguments) throws ParseException {
        Map<String, String> values = new LinkedHashMap<>();
        Matcher matcher = PREFIX_PATTERN.matcher(arguments);
        if (!matcher.find()) {
            if (arguments.isBlank()) {
                return values;
            }
            throw new ParseException(MESSAGE_PREAMBLE);
        }
        if (!arguments.substring(0, matcher.start()).isBlank()) {
            throw new ParseException(MESSAGE_PREAMBLE);
        }
        boolean hasNext;
        do {
            String prefix = matcher.group(1);
            int valueStart = matcher.end();
            hasNext = matcher.find();
            int valueEnd = hasNext ? matcher.start() : arguments.length();
            if (!PREFIXES.contains(prefix)) {
                throw new ParseException("Unknown add prefix '" + prefix + "'.");
            }
            if (values.containsKey(prefix)) {
                throw new ParseException("Repeated add prefix '" + prefix + "'.");
            }
            String value = arguments.substring(valueStart, valueEnd).trim();
            if (value.isBlank()) {
                throw new ParseException("Add prefix '" + prefix + "' requires a non-blank value.");
            }
            values.put(prefix, value);
        } while (hasNext);
        return values;
    }

    /**
     * Returns a required supplied value with a field-specific error when it is absent.
     */
    private static String requireValue(Map<String, String> values, String prefix) throws ParseException {
        String value = values.get(prefix);
        if (value == null) {
            throw new ParseException("Missing required add prefix '" + prefix + "'.");
        }
        return value;
    }

    /**
     * Parses either own or parent contact phone within the shared role-record length bound.
     */
    private static Phone parsePhone(String value) throws ParseException {
        if (value.length() > ContactDetails.MAX_PHONE_LENGTH) {
            throw new ParseException(ContactDetails.MESSAGE_PHONE_CONSTRAINTS);
        }
        return ParserUtil.parsePhone(value);
    }
}
