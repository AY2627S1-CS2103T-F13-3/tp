package seedu.address.logic;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.List;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.Student;

/**
 * Resolves command indices through a supplied current people view to immutable canonical records.
 * Callers retain the resolved stable identity throughout the operation, even if the view later changes.
 * This foundation does not select a view, change operational data, or activate canonical commands.
 */
public final class PersonIndexResolver {

    public static final String MESSAGE_NOT_STUDENT =
            "The selected person is not a student. Use the current people-list index of a student.";

    private PersonIndexResolver() {
    }

    /**
     * Returns the record at the supplied index in the current filtered people list.
     * The position is never interpreted as a stable ID suffix or an unfiltered-list position.
     * The caller supplies the authoritative people view at execution and keeps it unchanged
     * during resolution.
     *
     * @throws NullPointerException if the index, view, or selected record is null.
     * @throws CommandException if the index is outside the supplied view.
     */
    public static PersonRecord resolve(Index index, List<? extends PersonRecord> visiblePeople)
            throws CommandException {
        requireAllNonNull(index, visiblePeople);
        if (index.getZeroBased() >= visiblePeople.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        }
        return requireNonNull(visiblePeople.get(index.getZeroBased()));
    }

    /**
     * Resolves the current people-list index once and returns its record only if it is a student.
     * A tutor or parent at that position is rejected without searching a separate student-only list.
     *
     * @throws NullPointerException if the index, view, or selected record is null.
     * @throws CommandException if the index is outside the supplied view or the selected person
     *     is not a student.
     */
    public static Student resolveStudent(Index index, List<? extends PersonRecord> visiblePeople)
            throws CommandException {
        PersonRecord person = resolve(index, visiblePeople);
        if (!(person instanceof Student student)) {
            throw new CommandException(MESSAGE_NOT_STUDENT);
        }
        return student;
    }
}
