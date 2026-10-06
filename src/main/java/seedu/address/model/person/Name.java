package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * Represents a Person's name in the address book.
 * Guarantees: immutable; is valid as declared in {@link #isValidName(String)}
 */
public class Name {

    public static final int MAX_LENGTH = 100;
    public static final String MESSAGE_CONSTRAINTS =
            "Names must contain 1 to 100 characters after trimming and collapsing spaces, "
            + "using English letters, spaces, apostrophes, hyphens or periods, with at least one letter.";
    public static final String VALIDATION_REGEX = "(?=.*[A-Za-z])[A-Za-z .'-]+";

    public final String fullName;

    /**
     * Creates a {@code Name} with surrounding spaces removed and repeated spaces collapsed.
     *
     * @param name A valid name.
     */
    public Name(String name) {
        requireNonNull(name);
        checkArgument(isValidName(name), MESSAGE_CONSTRAINTS);
        fullName = normalize(name);
    }

    /**
     * Returns whether a string satisfies the name contract after space normalization.
     */
    public static boolean isValidName(String test) {
        String normalizedName = normalize(test);
        return normalizedName.length() <= MAX_LENGTH && normalizedName.matches(VALIDATION_REGEX);
    }

    private static String normalize(String name) {
        return name.replaceAll("^ +| +$", "").replaceAll(" +", " ");
    }

    @Override
    public String toString() {
        return fullName;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Name otherName)) {
            return false;
        }

        return fullName.equals(otherName.fullName);
    }

    @Override
    public int hashCode() {
        return fullName.hashCode();
    }

}
