package ponhub.logic.parser;

import static ponhub.commons.util.AppUtil.checkArgument;
import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Optional;

import ponhub.model.person.ContactDetails;
import ponhub.model.person.EducationLevel;
import ponhub.model.person.Parent;
import ponhub.model.person.PersonRole;
import ponhub.model.person.Phone;
import ponhub.model.person.Tutor;

/**
 * Immutable, ID-free fields for a role-aware person addition, with no writable model state.
 * Shared contact validators and this record enforce field requirements. Candidate preparation checks
 * duplicates and allocates an ID without changing the live registry.
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
