package ponhub.model.person;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.AppUtil.checkArgument;

/**
 * Represents a person's postal address in PonHub contact details.
 * Guarantees: immutable; is valid as declared in {@link #isValidAddress(String)}
 */
public final class Address {

    public static final int MAX_LENGTH = 200;
    public static final String MESSAGE_CONSTRAINTS =
            "Addresses must contain 1 to 200 printable ASCII characters after trimming and collapsing spaces, "
            + "with each slash immediately between digits; tabs and line breaks are not allowed.";
    public static final String VALIDATION_REGEX = "[\\x20-\\x7E]+";

    public final String value;

    /**
     * Creates an {@code Address}, trimming Unicode boundary spaces and collapsing ASCII spaces.
     * Rejects controls, format characters, line/paragraph separators and lone surrogates before trimming.
     *
     * @param address A valid address.
     */
    public Address(String address) {
        requireNonNull(address);
        checkArgument(!ContactText.hasForbiddenCodePoints(address), MESSAGE_CONSTRAINTS);
        String normalizedAddress = ContactText.normalizeSpaces(address);
        checkArgument(isValidNormalizedAddress(normalizedAddress), MESSAGE_CONSTRAINTS);
        value = normalizedAddress;
    }

    /**
     * Returns whether a string satisfies the address contract after space normalization.
     */
    public static boolean isValidAddress(String test) {
        requireNonNull(test);
        return !ContactText.hasForbiddenCodePoints(test)
                && isValidNormalizedAddress(ContactText.normalizeSpaces(test));
    }

    private static boolean isValidNormalizedAddress(String address) {
        if (address.length() > MAX_LENGTH || !address.matches(VALIDATION_REGEX)) {
            return false;
        }
        for (int index = 0; index < address.length(); index++) {
            if (address.charAt(index) == '/' && (index == 0 || index + 1 == address.length()
                    || !isAsciiDigit(address.charAt(index - 1)) || !isAsciiDigit(address.charAt(index + 1)))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAsciiDigit(char character) {
        return character >= '0' && character <= '9';
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
        if (!(other instanceof Address otherAddress)) {
            return false;
        }

        return value.equals(otherAddress.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
