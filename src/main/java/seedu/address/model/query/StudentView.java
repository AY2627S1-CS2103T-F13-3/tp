package seedu.address.model.query;

import java.util.Optional;

import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.PersonId;

/**
 * Read-only projection of a current student in a lesson roster.
 * Details come from the canonical people registry. Changed details replace this entry in observable lists.
 * Implementations expose non-null, immutable values and a student-role ID; absent contacts use empty optionals.
 */
public interface StudentView {
    /** Returns the student's stable identity. */
    PersonId getStudentId();

    /** Returns the student's current display name. */
    String getName();

    /** Returns the student's education level. */
    EducationLevel getEducationLevel();

    /** Returns the student's required parent phone number. */
    String getParentPhone();

    /** Returns the student's own phone number, if supplied. */
    Optional<String> getPhone();

    /** Returns the student's email address, if supplied. */
    Optional<String> getEmail();

    /** Returns the student's postal address, if supplied. */
    Optional<String> getAddress();
}
