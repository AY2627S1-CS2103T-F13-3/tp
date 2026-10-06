package seedu.address.model.person;

import static seedu.address.commons.util.AppUtil.checkArgument;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Objects;

import seedu.address.commons.util.ToStringBuilder;

/**
 * An immutable parent record with a stable ID and required own phone.
 * Links to students are derived separately from matching parent phone values.
 */
public final class Parent implements PersonRecord {

    public static final String MESSAGE_ID_CONSTRAINTS = "Parent records require a parent person ID.";
    public static final String MESSAGE_PHONE_CONSTRAINTS = "Parent records require their own phone number.";

    private final PersonId id;
    private final ContactDetails contactDetails;

    /**
     * Creates a parent with a parent ID and contact details containing an own phone.
     *
     * @throws IllegalArgumentException if the ID is for another role or the own phone is absent.
     */
    public Parent(PersonId id, ContactDetails contactDetails) {
        requireAllNonNull(id, contactDetails);
        checkArgument(id.getRole() == PersonRole.PARENT, MESSAGE_ID_CONSTRAINTS);
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
        return other instanceof Parent otherParent
                && contactDetails.getNormalizedName().equals(otherParent.contactDetails.getNormalizedName())
                && getPhone().equals(otherParent.getPhone());
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof Parent otherParent)) {
            return false;
        }

        return id.equals(otherParent.id) && contactDetails.equals(otherParent.contactDetails);
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
