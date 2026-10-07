package seedu.address.logic.parser;

import static seedu.address.commons.util.AppUtil.checkArgument;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Optional;

import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Parent;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Tutor;

/**
 * Immutable, ID-free input for a future role-aware addition. It contains no registry or writable model state.
 * Contact values use the shared validators; duplicate checks and committed ID allocation belong to integration.
 */
public record PersonAdditionInput(PersonRole role, ContactDetails contactDetails,
        Optional<EducationLevel> level, Optional<Phone> parentPhone) {

    /**
     * Creates input with the required fields for its role and explicitly absent optional fields.
     *
     * @throws NullPointerException if any component is null.
     * @throws IllegalArgumentException if student fields are missing, supplied for another role,
     *     the parent phone exceeds 15 digits, or a tutor/parent has no own phone.
     */
    public PersonAdditionInput {
        requireAllNonNull(role, contactDetails, level, parentPhone);
        if (role == PersonRole.STUDENT) {
            checkArgument(level.isPresent(), "Student additions require an education level.");
            checkArgument(parentPhone.isPresent(), "Student additions require a parent phone number.");
            checkArgument(parentPhone.orElseThrow().value.length() <= ContactDetails.MAX_PHONE_LENGTH,
                    ContactDetails.MESSAGE_PHONE_CONSTRAINTS);
        } else {
            checkArgument(level.isEmpty() && parentPhone.isEmpty(),
                    "Only student additions accept an education level or parent phone number.");
            checkArgument(contactDetails.getPhone().isPresent(), role == PersonRole.TUTOR
                    ? Tutor.MESSAGE_PHONE_CONSTRAINTS : Parent.MESSAGE_PHONE_CONSTRAINTS);
        }
    }
}
