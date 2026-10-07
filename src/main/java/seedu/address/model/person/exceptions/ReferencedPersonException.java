package seedu.address.model.person.exceptions;

import seedu.address.model.person.PersonId;

/**
 * Signals that a person cannot be removed while the caller reports references to that record.
 */
public class ReferencedPersonException extends IllegalStateException {

    /**
     * Creates an exception identifying the referenced person.
     */
    public ReferencedPersonException(PersonId id) {
        super("Person " + id + " cannot be removed while references remain.");
    }
}
