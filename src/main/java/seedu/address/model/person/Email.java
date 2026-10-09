package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * Represents a Person's email in the address book.
 * Guarantees: immutable; is valid as declared in {@link #isValidEmail(String)}
 */
public class Email {

    public static final int MAX_LENGTH = 254;
    public static final String MESSAGE_CONSTRAINTS =
            "Emails must use local-part@domain with at most 254 characters and no spaces. "
            + "The local part may contain English letters, digits, periods, underscores, %, + and -, "
            + "but no leading, trailing or consecutive periods. "
            + "The domain must have at least two dot-separated labels containing English letters, digits or hyphens, "
            + "with no leading or trailing hyphens. The final label must contain 2 to 63 English letters.";
    public static final String VALIDATION_REGEX =
            "[A-Za-z0-9_%+-]+(?:\\.[A-Za-z0-9_%+-]+)*@"
            + "(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}";

    public final String value;

    /**
     * Constructs an {@code Email}.
     *
     * @param email A valid email address.
     */
    public Email(String email) {
        requireNonNull(email);
        checkArgument(isValidEmail(email), MESSAGE_CONSTRAINTS);
        value = email;
    }

    /**
     * Returns whether a string satisfies the email format and length constraints using constant stack space.
     */
    public static boolean isValidEmail(String test) {
        requireNonNull(test);
        if (test.length() > MAX_LENGTH) {
            return false;
        }
        int separatorIndex = test.indexOf('@');
        if (separatorIndex <= 0 || separatorIndex == test.length() - 1) {
            return false;
        }

        boolean wasPeriod = true;
        for (int i = 0; i < separatorIndex; i++) {
            char character = test.charAt(i);
            if (character == '.') {
                if (wasPeriod) {
                    return false;
                }
            } else if (!isAsciiAlphanumeric(character) && "_%+-".indexOf(character) < 0) {
                return false;
            }
            wasPeriod = character == '.';
        }
        if (wasPeriod) {
            return false;
        }

        int labelStart = separatorIndex + 1;
        boolean hasDomainPeriod = false;
        for (int i = separatorIndex + 1; i < test.length(); i++) {
            char character = test.charAt(i);
            if (character == '.') {
                if (i == labelStart || test.charAt(i - 1) == '-') {
                    return false;
                }
                hasDomainPeriod = true;
                labelStart = i + 1;
            } else if (!isAsciiAlphanumeric(character) && (character != '-' || i == labelStart)) {
                return false;
            }
        }

        int finalLabelLength = test.length() - labelStart;
        if (!hasDomainPeriod || finalLabelLength < 2 || finalLabelLength > 63) {
            return false;
        }
        for (int i = labelStart; i < test.length(); i++) {
            if (!isAsciiLetter(test.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAsciiAlphanumeric(char character) {
        return isAsciiLetter(character) || character >= '0' && character <= '9';
    }

    private static boolean isAsciiLetter(char character) {
        return character >= 'A' && character <= 'Z' || character >= 'a' && character <= 'z';
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Email otherEmail)) {
            return false;
        }

        return value.equals(otherEmail.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
