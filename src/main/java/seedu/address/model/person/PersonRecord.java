package seedu.address.model.person;

/**
 * A read-only view of a student, tutor, or parent record with stable identity and composed contacts.
 */
public interface PersonRecord {

    PersonId getId();

    ContactDetails getContactDetails();

    default Name getName() {
        return getContactDetails().getName();
    }

    default PersonRole getRole() {
        return getId().getRole();
    }

    /**
     * Returns whether another record has the same role, normalized name, and identifying phone.
     * Students use their parent phone; tutors and parents use their own phone.
     * IDs and other fields do not affect duplicate matching. Null or a different role returns false.
     */
    boolean isDuplicateOf(PersonRecord other);
}
