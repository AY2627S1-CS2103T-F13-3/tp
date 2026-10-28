package ponhub.logic.parser;

import static java.util.Objects.requireNonNull;

import ponhub.commons.core.index.Index;
import ponhub.commons.util.StringUtil;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.person.Address;
import ponhub.model.person.Email;
import ponhub.model.person.Name;
import ponhub.model.person.Phone;

/**
 * Contains utility methods used for parsing strings in the various *Parser classes.
 */
public class ParserUtil {

    public static final String MESSAGE_INVALID_INDEX = "Index must be a positive integer.";
    public static final String MESSAGE_CONTACT_PHONE_CONSTRAINTS =
            "Contact phone numbers must contain between " + Phone.MIN_LENGTH + " and " + Phone.MAX_LENGTH + " digits.";

    /**
     * Removes boundary code points for which {@link Character#isWhitespace(int)} or
     * {@link Character#isSpaceChar(int)} is true, preserving all internal text.
     * Callers that forbid controls or line breaks must reject them before trimming;
     * this method does not validate the field and can remove whitespace controls.
     *
     * @throws NullPointerException if the value is null.
     */
    public static String stripUnicodeBoundaryWhitespace(String value) {
        return StringUtil.stripUnicodeBoundaryWhitespace(value);
    }

    /**
     * Parses {@code oneBasedIndex} into an {@code Index} and returns it. Rejects non-whitespace ISO controls
     * before trimming leading and trailing whitespace.
     *
     * @throws ParseException if the specified index is invalid (not a non-zero unsigned integer).
     */
    public static Index parseIndex(String oneBasedIndex) throws ParseException {
        if (oneBasedIndex.codePoints().anyMatch(codePoint ->
                Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint))) {
            throw new ParseException(MESSAGE_INVALID_INDEX);
        }

        String trimmedIndex = stripUnicodeBoundaryWhitespace(oneBasedIndex);
        if (!StringUtil.isNonZeroUnsignedInteger(trimmedIndex)) {
            throw new ParseException(MESSAGE_INVALID_INDEX);
        }
        return Index.fromOneBased(Integer.parseInt(trimmedIndex));
    }

    /**
     * Parses a {@code String name} into a {@code Name}.
     * Surrounding spaces are removed and repeated spaces are collapsed by {@code Name}.
     *
     * @throws ParseException if the given {@code name} is invalid.
     */
    public static Name parseName(String name) throws ParseException {
        requireNonNull(name);
        try {
            return new Name(name);
        } catch (IllegalArgumentException e) {
            throw new ParseException(Name.MESSAGE_CONSTRAINTS);
        }
    }

    /**
     * Parses a {@code String phone} into a {@code Phone}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code phone} is invalid.
     */
    public static Phone parsePhone(String phone) throws ParseException {
        requireNonNull(phone);
        String trimmedPhone = phone.trim();
        if (!Phone.isValidPhone(trimmedPhone)) {
            throw new ParseException(Phone.MESSAGE_CONSTRAINTS);
        }
        return new Phone(trimmedPhone);
    }

    /**
     * Parses a person's own or parent contact phone, preserving the add/edit error for overlong raw values.
     * The length error takes precedence over other invalid characters and trimming, as in those parsers.
     * Phone defines the shared bounds and validates the number itself.
     *
     * @throws NullPointerException if the phone is null.
     * @throws ParseException if the given phone is invalid.
     */
    public static Phone parseContactPhone(String phone) throws ParseException {
        requireNonNull(phone);
        if (phone.length() > Phone.MAX_LENGTH) {
            throw new ParseException(MESSAGE_CONTACT_PHONE_CONSTRAINTS);
        }
        return parsePhone(phone);
    }

    /**
     * Parses a {@code String address} into an {@code Address}.
     * Surrounding spaces are removed and repeated spaces are collapsed by {@code Address}.
     *
     * @throws ParseException if the given {@code address} is invalid.
     */
    public static Address parseAddress(String address) throws ParseException {
        requireNonNull(address);
        try {
            return new Address(address);
        } catch (IllegalArgumentException e) {
            throw new ParseException(Address.MESSAGE_CONSTRAINTS);
        }
    }

    /**
     * Parses a {@code String email} into an {@code Email}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code email} is invalid.
     */
    public static Email parseEmail(String email) throws ParseException {
        requireNonNull(email);
        String trimmedEmail = email.trim();
        if (!Email.isValidEmail(trimmedEmail)) {
            throw new ParseException(Email.MESSAGE_CONSTRAINTS);
        }
        return new Email(trimmedEmail);
    }
}
