package ponhub.model.person.exceptions;

import ponhub.model.person.PersonId;

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
