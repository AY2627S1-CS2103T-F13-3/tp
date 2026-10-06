package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * Represents a Person's address in the address book.
 * Guarantees: immutable; is valid as declared in {@link #isValidAddress(String)}
 */
public class Address {

    public static final int MAX_LENGTH = 200;
    public static final String MESSAGE_CONSTRAINTS =
            "Addresses must contain 1 to 200 printable ASCII characters after trimming and collapsing spaces, "
            + "without slashes, tabs or line breaks.";
    public static final String VALIDATION_REGEX = "[\\x20-\\x2E\\x30-\\x7E]+";

    public final String value;

    /**
     * Creates an {@code Address} with surrounding spaces removed and repeated spaces collapsed.
     *
     * @param address A valid address.
     */
    public Address(String address) {
        requireNonNull(address);
        checkArgument(isValidAddress(address), MESSAGE_CONSTRAINTS);
        value = normalize(address);
    }

    /**
     * Returns whether a string satisfies the address contract after space normalization.
     */
    public static boolean isValidAddress(String test) {
        String normalizedAddress = normalize(test);
        return normalizedAddress.length() <= MAX_LENGTH && normalizedAddress.matches(VALIDATION_REGEX);
    }

    private static String normalize(String address) {
        return address.replaceAll("^ +| +$", "").replaceAll(" +", " ");
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
