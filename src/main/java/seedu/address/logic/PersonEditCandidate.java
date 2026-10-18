package seedu.address.logic;

import static seedu.address.commons.util.AppUtil.checkArgument;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Optional;

import seedu.address.logic.parser.PersonEditInput;
import seedu.address.logic.parser.PersonEditInput.Field;
import seedu.address.model.PonHubData;
import seedu.address.model.PonHubDataState;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.Parent;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Student;
import seedu.address.model.person.Tutor;
import seedu.address.model.person.exceptions.DuplicatePersonException;

/**
 * An immutable complete candidate for editing one person without changing live state or allocating an ID.
 * Command activation and persistence belong to the following increment; preparation never saves or publishes.
 */
public final class PersonEditCandidate {

    private final PonHubDataState candidateState;
    private final PersonRecord editedPerson;
    private final boolean isChanged;

    private PersonEditCandidate(PonHubDataState candidateState, PersonRecord editedPerson, boolean isChanged) {
        this.candidateState = candidateState;
        this.editedPerson = editedPerson;
        this.isChanged = isChanged;
    }

    /**
     * Prepares a replacement from one complete snapshot, a stable target ID and immutable field intent.
     * Preserves global order, role, identity, all allocation counters, Lessons, rosters and attendance.
     * Canonical constructors and whole-root validation reject invalid fields and duplicates of other records.
     * A complete-value no-op retains the source snapshot, including its display spelling.
     * Callers must prepare from a fresh snapshot within their single-threaded transaction boundary.
     *
     * @throws NullPointerException if any argument is null.
     * @throws IllegalArgumentException if the target is missing, a required field is cleared,
     *     Student-only fields are supplied for another role, or resulting field values are invalid.
     * @throws DuplicatePersonException if the resulting business identity duplicates another record.
     */
    public static PersonEditCandidate prepare(PonHubDataState source, PersonId targetId, PersonEditInput input) {
        requireAllNonNull(source, targetId, input);
        PonHubData staged = new PonHubData(source);
        PersonRecord original = staged.getPerson(targetId).orElseThrow(() ->
                new IllegalArgumentException("The person does not exist: " + targetId));
        if (original.getRole() != PersonRole.STUDENT) {
            checkArgument(!input.level().isSupplied() && !input.parentPhone().isSupplied(),
                    "Only Student edits accept an education level or parent phone number.");
        }
        ContactDetails contacts = original.getContactDetails();
        ContactDetails replacementContacts = new ContactDetails(
                requireValue(input.name(), contacts.getName(), "A person's name cannot be cleared."),
                input.phone().resolve(contacts.getPhone()), input.email().resolve(contacts.getEmail()),
                input.address().resolve(contacts.getAddress()));
        PersonRecord replacement = switch (original.getRole()) {
            case STUDENT -> {
                Student student = (Student) original;
                yield new Student(targetId, replacementContacts,
                        requireValue(input.level(), student.getLevel(), "A Student's level cannot be cleared."),
                        requireValue(input.parentPhone(), student.getParentPhone(),
                                "A Student's parent phone cannot be cleared."));
            }
            case TUTOR -> new Tutor(targetId, replacementContacts);
            case PARENT -> new Parent(targetId, replacementContacts);
        };
        PersonRecord editedPerson = staged.replacePerson(replacement);
        return new PersonEditCandidate(staged.exportState(), editedPerson, !original.equals(editedPerson));
    }

    public PonHubDataState getCandidateState() {
        return candidateState;
    }

    public PersonRecord getEditedPerson() {
        return editedPerson;
    }

    public boolean isChanged() {
        return isChanged;
    }

    private static <T> T requireValue(Field<T> field, T existing, String message) {
        return field.resolve(Optional.of(existing)).orElseThrow(() -> new IllegalArgumentException(message));
    }
}
