package seedu.address.model.lesson;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.util.Locale;

/**
 * Represents a lesson room identifier.
 * Guarantees: immutable; case-normalized; valid as declared in {@link #isValidRoom(String)}.
 */
public final class Room {

    public static final String MESSAGE_CONSTRAINTS =
            "Rooms should contain 1 to 10 letters or digits without spaces or punctuation";
    public static final String VALIDATION_REGEX = "[A-Z0-9]{1,10}";

    private final String value;

    /**
     * Constructs a {@code Room}. Surrounding whitespace is removed and letters are normalized to upper case.
     */
    public Room(String room) {
        requireNonNull(room);
        String normalizedRoom = normalize(room);
        checkArgument(isNormalizedRoomValid(normalizedRoom), MESSAGE_CONSTRAINTS);
        value = normalizedRoom;
    }

    /**
     * Returns true if {@code test} is a valid room after normalization.
     */
    public static boolean isValidRoom(String test) {
        requireNonNull(test);
        return isNormalizedRoomValid(normalize(test));
    }

    private static String normalize(String room) {
        return room.strip().toUpperCase(Locale.ROOT);
    }

    private static boolean isNormalizedRoomValid(String room) {
        return room.matches(VALIDATION_REGEX);
    }

    public String getValue() {
        return value;
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
        if (!(other instanceof Room otherRoom)) {
            return false;
        }
        return value.equals(otherRoom.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
