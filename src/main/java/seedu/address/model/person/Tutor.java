package seedu.address.model.person;

import static seedu.address.commons.util.AppUtil.checkArgument;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Objects;

import seedu.address.commons.util.ToStringBuilder;

/**
 * An immutable tutor record with a stable ID and required own phone.
 * Assigned lessons are maintained separately from the tutor record.
 */
public final class Tutor implements PersonRecord {

    public static final String MESSAGE_ID_CONSTRAINTS = "Tutor records require a tutor person ID.";
    public static final String MESSAGE_PHONE_CONSTRAINTS = "Tutor records require their own phone number.";

    private final PersonId id;
    private final ContactDetails contactDetails;

    /**
     * Creates a tutor with a tutor ID and contact details containing an own phone.
     *
     * @throws IllegalArgumentException if the ID is for another role or the own phone is absent.
     */
    public Tutor(PersonId id, ContactDetails contactDetails) {
        requireAllNonNull(id, contactDetails);
        checkArgument(id.getRole() == PersonRole.TUTOR, MESSAGE_ID_CONSTRAINTS);
        checkArgument(contactDetails.getPhone().isPresent(), MESSAGE_PHONE_CONSTRAINTS);
        this.id = id;
        this.contactDetails = contactDetails;
    }

    @Override
    public PersonId getId() {
        return id;
    }

    @Override
    public ContactDetails getContactDetails() {
        return contactDetails;
    }

    public Phone getPhone() {
        return contactDetails.getPhone().orElseThrow();
    }

    @Override
    public boolean isDuplicateOf(PersonRecord other) {
        return other instanceof Tutor otherTutor
                && contactDetails.getNormalizedName().equals(otherTutor.contactDetails.getNormalizedName())
                && getPhone().equals(otherTutor.getPhone());
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof Tutor otherTutor)) {
            return false;
        }

        return id.equals(otherTutor.id) && contactDetails.equals(otherTutor.contactDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, contactDetails);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("id", id)
                .add("contactDetails", contactDetails)
                .toString();
    }
}
