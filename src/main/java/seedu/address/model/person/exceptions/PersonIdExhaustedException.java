package seedu.address.model.person.exceptions;

import seedu.address.model.person.PersonRole;

/**
 * Signals that every positive sequence number for a person role has already been allocated.
 */
public class PersonIdExhaustedException extends IllegalStateException {

    /**
     * Creates an exception identifying the exhausted role.
     */
    public PersonIdExhaustedException(PersonRole role) {
        super("No more person IDs are available for role " + role.getValue() + ".");
    }
}
