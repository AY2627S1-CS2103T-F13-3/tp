package seedu.address.model.query;

import java.util.Optional;

/**
 * Read-only projection of a current student record for lesson roster results.
 * Implementations resolve details from the canonical people registry and expose non-null values.
 * Optional contact fields use {@link Optional#empty()} when absent.
 * Changed student details are represented by replacement entries in the observable roster.
 */
public interface StudentView {
    /** Returns the student's stable ID, such as {@code S1}. */
    String getStudentId();

    /** Returns the student's current name. */
    String getName();

    /** Returns the student's education level in its canonical uppercase form. */
    String getEducationLevel();

    /** Returns the student's required parent contact number. */
    String getParentPhone();

    /** Returns the student's own phone number, when supplied. */
    Optional<String> getPhone();

    /** Returns the student's email address, when supplied. */
    Optional<String> getEmail();

    /** Returns the student's postal address, when supplied. */
    Optional<String> getAddress();
}
