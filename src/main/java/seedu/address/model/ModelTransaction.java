package seedu.address.model;

/**
 * An isolated working model for one command on the application's model thread.
 * Discarding a transaction must leave live data, counters and views untouched.
 *
 * @param <T> The working model type.
 */
public interface ModelTransaction<T> {
    /**
     * Returns the working model against which the command executes.
     */
    T getStagedModel();

    /**
     * Returns whether complete operational state differs from the pre-command snapshot.
     * View-only changes do not require persistence.
     */
    boolean hasOperationalChanges();

    /**
     * Publishes the staged data and views while preserving observable list identities.
     * Call only after successful persistence, or when operational data is unchanged.
     * Implementations must validate during staging so publication needs no further validation or I/O.
     */
    void commit();
}
