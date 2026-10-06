package seedu.address.model.person;

import static seedu.address.commons.util.AppUtil.checkArgument;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import seedu.address.commons.util.ToStringBuilder;

/**
 * Immutable contact details shared by PonHub person records.
 * A name is required; phone, email, and address may be omitted.
 */
public final class ContactDetails {

    public static final int MAX_PHONE_LENGTH = 15;
    public static final String MESSAGE_PHONE_CONSTRAINTS =
            "Contact phone numbers must contain between 3 and 15 digits.";

    private final Name name;
    private final Optional<Phone> phone;
    private final Optional<Email> email;
    private final Optional<Address> address;

    /**
     * Creates contact details with no optional contact fields.
     */
    public ContactDetails(Name name) {
        this(name, Optional.empty(), Optional.empty(), Optional.empty());
    }

    /**
     * Creates contact details from validated values and explicitly optional fields.
     *
     * @throws IllegalArgumentException if the phone contains more than 15 digits.
     */
    public ContactDetails(Name name, Optional<Phone> phone, Optional<Email> email, Optional<Address> address) {
        requireAllNonNull(name, phone, email, address);
        phone.ifPresent(value -> checkArgument(value.value.length() <= MAX_PHONE_LENGTH, MESSAGE_PHONE_CONSTRAINTS));
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
    }

    public Name getName() {
        return name;
    }

    public Optional<Phone> getPhone() {
        return phone;
    }

    public Optional<Email> getEmail() {
        return email;
    }

    public Optional<Address> getAddress() {
        return address;
    }

    /**
     * Returns the name normalized for duplicate matching while preserving its display spelling.
     */
    public String getNormalizedName() {
        return name.fullName.strip().replaceAll(" +", " ").toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof ContactDetails otherDetails)) {
            return false;
        }

        return name.equals(otherDetails.name)
                && phone.equals(otherDetails.phone)
                && email.equals(otherDetails.email)
                && address.equals(otherDetails.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, phone, email, address);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("name", name)
                .add("phone", phone)
                .add("email", email)
                .add("address", address)
                .toString();
    }
}
