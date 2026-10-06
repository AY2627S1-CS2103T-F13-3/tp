package seedu.address.model.person;

import static seedu.address.commons.util.AppUtil.checkArgument;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Objects;

import seedu.address.commons.util.ToStringBuilder;

/**
 * An immutable student record with a stable ID, education level, and required parent contact.
 * Shared lesson membership and dated attendance are maintained separately from the student record.
 */
public final class Student {

    public static final String MESSAGE_ID_CONSTRAINTS = "Student records require a student person ID.";

    private final PersonId id;
    private final ContactDetails contactDetails;
    private final EducationLevel level;
    private final Phone parentPhone;

    /**
     * Creates a student with a student ID and the fields needed for student administration.
     *
     * @throws IllegalArgumentException if the ID is for another role or the parent phone exceeds 15 digits.
     */
    public Student(PersonId id, ContactDetails contactDetails, EducationLevel level, Phone parentPhone) {
        requireAllNonNull(id, contactDetails, level, parentPhone);
        checkArgument(id.getRole() == PersonRole.STUDENT, MESSAGE_ID_CONSTRAINTS);
        checkArgument(parentPhone.value.length() <= ContactDetails.MAX_PHONE_LENGTH,
                ContactDetails.MESSAGE_PHONE_CONSTRAINTS);
        this.id = id;
        this.contactDetails = contactDetails;
        this.level = level;
        this.parentPhone = parentPhone;
    }

    public PersonId getId() {
        return id;
    }

    public ContactDetails getContactDetails() {
        return contactDetails;
    }

    public Name getName() {
        return contactDetails.getName();
    }

    public EducationLevel getLevel() {
        return level;
    }

    public Phone getParentPhone() {
        return parentPhone;
    }

    /**
     * Returns whether another student has the same normalized name and parent phone.
     * Stable IDs, education levels, and optional contact fields do not change this duplicate key.
     */
    public boolean isDuplicateOf(Student other) {
        return other != null
                && contactDetails.getNormalizedName().equals(other.contactDetails.getNormalizedName())
                && parentPhone.equals(other.parentPhone);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof Student otherStudent)) {
            return false;
        }

        return id.equals(otherStudent.id)
                && contactDetails.equals(otherStudent.contactDetails)
                && level.equals(otherStudent.level)
                && parentPhone.equals(otherStudent.parentPhone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, contactDetails, level, parentPhone);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("id", id)
                .add("contactDetails", contactDetails)
                .add("level", level)
                .add("parentPhone", parentPhone)
                .toString();
    }
}
