package seedu.address.model.person;

import static java.util.Objects.requireNonNull;

import java.util.Locale;

/**
 * The student, tutor, or parent role of a PonHub person record.
 */
public enum PersonRole {
    STUDENT("student", "S"),
    TUTOR("tutor", "T"),
    PARENT("parent", "P");

    public static final String MESSAGE_CONSTRAINTS = "Person roles must be student, tutor, or parent.";

    private final String value;
    private final String idPrefix;

    PersonRole(String value, String idPrefix) {
        this.value = value;
        this.idPrefix = idPrefix;
    }

    public String getValue() {
        return value;
    }

    public String getIdPrefix() {
        return idPrefix;
    }

    /**
     * Parses a role name, ignoring letter case and surrounding whitespace.
     *
     * @throws NullPointerException if {@code role} is null.
     * @throws IllegalArgumentException if {@code role} is not a supported role name.
     */
    public static PersonRole parse(String role) {
        requireNonNull(role);
        return switch (role.trim().toLowerCase(Locale.ROOT)) {
            case "student" -> STUDENT;
            case "tutor" -> TUTOR;
            case "parent" -> PARENT;
            default -> throw new IllegalArgumentException(MESSAGE_CONSTRAINTS);
        };
    }
}
