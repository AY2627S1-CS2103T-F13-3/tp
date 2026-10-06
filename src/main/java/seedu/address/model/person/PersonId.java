package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.util.Locale;

/**
 * An immutable person identifier containing a role prefix and a positive sequence number.
 */
public final class PersonId {
    public static final String MESSAGE_CONSTRAINTS =
            "Person IDs must contain S, T, or P followed by a positive number without leading zeros, "
                    + "up to " + Long.MAX_VALUE + ".";

    private static final String VALIDATION_REGEX = "[STP][1-9][0-9]*";

    private final String value;
    private final PersonRole role;
    private final long sequence;

    /**
     * Creates an identifier, normalizing letter case and surrounding whitespace.
     *
     * @param id A role-prefixed positive identifier.
     * @throws NullPointerException if {@code id} is null.
     * @throws IllegalArgumentException if {@code id} is invalid or its sequence is too large.
     */
    public PersonId(String id) {
        requireNonNull(id);
        String normalizedId = id.trim().toUpperCase(Locale.ROOT);
        checkArgument(isValid(normalizedId), MESSAGE_CONSTRAINTS);
        value = normalizedId;
        sequence = Long.parseLong(normalizedId.substring(1));
        role = switch (normalizedId.charAt(0)) {
            case 'S' -> PersonRole.STUDENT;
            case 'T' -> PersonRole.TUTOR;
            case 'P' -> PersonRole.PARENT;
            default -> throw new IllegalArgumentException(MESSAGE_CONSTRAINTS);
        };
    }

    /**
     * Creates an identifier for the given role and positive sequence number.
     *
     * @throws NullPointerException if {@code role} is null.
     * @throws IllegalArgumentException if {@code sequence} is not positive.
     */
    public static PersonId of(PersonRole role, long sequence) {
        requireNonNull(role);
        checkArgument(sequence > 0, MESSAGE_CONSTRAINTS);
        return new PersonId(role.getIdPrefix() + sequence);
    }

    /**
     * Returns whether the input can represent a valid identifier after normalization.
     */
    public static boolean isValid(String id) {
        if (id == null) {
            return false;
        }

        String normalizedId = id.trim().toUpperCase(Locale.ROOT);
        if (!normalizedId.matches(VALIDATION_REGEX)) {
            return false;
        }

        try {
            Long.parseLong(normalizedId.substring(1));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public PersonRole getRole() {
        return role;
    }

    public long getSequence() {
        return sequence;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof PersonId otherId)) {
            return false;
        }

        return value.equals(otherId.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
