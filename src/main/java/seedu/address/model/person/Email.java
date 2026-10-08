package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * Represents a Person's email in the address book.
 * Guarantees: immutable; is valid as declared in {@link #isValidEmail(String)}
 */
public class Email {

    private static final String SPECIAL_CHARACTERS = "+_.-";
    public static final String MESSAGE_CONSTRAINTS = "Emails should be of the format local-part@domain "
            + "and adhere to the following constraints:\n"
            + "1. The local-part should only contain alphanumeric characters and these special characters, excluding "
            + "the parentheses, (" + SPECIAL_CHARACTERS + "). The local-part may not start or end with any special "
            + "characters.\n"
            + "2. The local-part is followed by an '@' and then a domain name. The domain name is made up of domain "
            + "labels separated by periods.\n"
            + "The domain name must:\n"
            + "    - end with a domain label at least 2 characters long\n"
            + "    - have each domain label start and end with alphanumeric characters\n"
            + "    - have each domain label consist of alphanumeric characters, separated only by hyphens, if any.";
    // alphanumeric and special characters
    private static final String ALPHANUMERIC_NO_UNDERSCORE = "[^\\W_]+"; // alphanumeric characters except underscore
    private static final String LOCAL_PART_REGEX = "^" + ALPHANUMERIC_NO_UNDERSCORE + "([" + SPECIAL_CHARACTERS + "]"
            + ALPHANUMERIC_NO_UNDERSCORE + ")*";
    private static final String DOMAIN_PART_REGEX = ALPHANUMERIC_NO_UNDERSCORE
            + "(-" + ALPHANUMERIC_NO_UNDERSCORE + ")*";
    private static final String DOMAIN_LAST_PART_REGEX = "(" + DOMAIN_PART_REGEX + "){2,}$"; // At least two chars
    private static final String DOMAIN_REGEX = "(" + DOMAIN_PART_REGEX + "\\.)*" + DOMAIN_LAST_PART_REGEX;
    public static final String VALIDATION_REGEX = LOCAL_PART_REGEX + "@" + DOMAIN_REGEX;

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
     * Returns whether a string satisfies the inherited email rules using constant stack space.
     */
    public static boolean isValidEmail(String test) {
        requireNonNull(test);
        int separatorIndex = test.indexOf('@');
        if (separatorIndex <= 0 || separatorIndex == test.length() - 1) {
            return false;
        }

        boolean wasAlphanumeric = false;
        for (int i = 0; i < separatorIndex; i++) {
            char character = test.charAt(i);
            boolean isAlphanumeric = isAsciiAlphanumeric(character);
            if (!isAlphanumeric && (!wasAlphanumeric || SPECIAL_CHARACTERS.indexOf(character) < 0)) {
                return false;
            }
            wasAlphanumeric = isAlphanumeric;
        }
        if (!wasAlphanumeric) {
            return false;
        }

        wasAlphanumeric = false;
        boolean hasAdjacentAlphanumeric = false;
        for (int i = separatorIndex + 1; i < test.length(); i++) {
            char character = test.charAt(i);
            if (isAsciiAlphanumeric(character)) {
                hasAdjacentAlphanumeric |= wasAlphanumeric;
                wasAlphanumeric = true;
            } else if ((character == '-' || character == '.') && wasAlphanumeric) {
                wasAlphanumeric = false;
                if (character == '.') {
                    hasAdjacentAlphanumeric = false;
                }
            } else {
                return false;
            }
        }

        // The legacy final-label regex repeats a whole label twice. Preserve its acceptance set:
        // a label can be split into two valid pieces only within a run of alphanumeric characters.
        return wasAlphanumeric && hasAdjacentAlphanumeric;
    }

    private static boolean isAsciiAlphanumeric(char character) {
        return character >= 'A' && character <= 'Z'
                || character >= 'a' && character <= 'z'
                || character >= '0' && character <= '9';
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
