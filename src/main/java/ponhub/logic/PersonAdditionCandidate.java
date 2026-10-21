package ponhub.logic;

import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import ponhub.logic.parser.PersonAdditionInput;
import ponhub.model.PonHubDataState;
import ponhub.model.person.PeopleRegistry;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.exceptions.DuplicatePersonException;
import ponhub.model.person.exceptions.PersonIdExhaustedException;

/**
 * An immutable complete candidate for a role-aware person addition.
 * Preparation changes no live data and reserves no identity in the source allocation history.
 * The add command installs this state into its staged model; the transaction saves it before publication.
 */
public final class PersonAdditionCandidate {

    private final PonHubDataState candidateState;
    private final PersonRecord addedPerson;

    private PersonAdditionCandidate(PonHubDataState candidateState, PersonRecord addedPerson) {
        this.candidateState = candidateState;
        this.addedPerson = addedPerson;
    }

    /**
     * Prepares an addition from validated input and one complete source snapshot.
     * Uses the registry's role-specific validation, duplicate rules, allocation history, and global order.
     * Lessons, rosters, retained attendance, and lesson allocation history are preserved unchanged.
     * Callers must use a fresh source snapshot within their single-threaded transaction boundary.
     *
     * @throws NullPointerException if the source or input is null.
     * @throws DuplicatePersonException if the role-specific business identity already exists.
     * @throws PersonIdExhaustedException if every positive ID for the selected role has been allocated.
     */
    public static PersonAdditionCandidate prepare(PonHubDataState source, PersonAdditionInput input) {
        requireAllNonNull(source, input);
        PeopleRegistry people = new PeopleRegistry(source.people());
        PersonRecord addedPerson = switch (input.role()) {
            case STUDENT -> people.addStudent(input.contactDetails(), input.level().orElseThrow(),
                    input.parentPhone().orElseThrow());
            case TUTOR -> people.addTutor(input.contactDetails());
            case PARENT -> people.addParent(input.contactDetails());
        };
        PonHubDataState candidateState = new PonHubDataState(people.exportState(), source.lessons(),
                source.attendance(), source.lastAllocatedLessonSequence());
        return new PersonAdditionCandidate(candidateState, addedPerson);
    }

    public PonHubDataState getCandidateState() {
        return candidateState;
    }

    public PersonRecord getAddedPerson() {
        return addedPerson;
    }
}
