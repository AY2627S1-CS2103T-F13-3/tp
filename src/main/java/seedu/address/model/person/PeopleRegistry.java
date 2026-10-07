package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.person.exceptions.DuplicatePersonException;
import seedu.address.model.person.exceptions.PersonIdExhaustedException;
import seedu.address.model.person.exceptions.PersonNotFoundException;
import seedu.address.model.person.exceptions.ReferencedPersonException;

/**
 * A canonical ordered registry of immutable student, tutor, and parent records.
 * IDs increase independently per role and are never reclaimed by removal. Read APIs return immutable snapshots.
 * This model foundation is not yet connected to the inherited application's commands, UI, or storage.
 */
public final class PeopleRegistry {

    private final Map<PersonId, PersonRecord> people = new LinkedHashMap<>();
    private final Map<PersonRole, Long> lastAllocatedSequences = new EnumMap<>(PersonRole.class);

    /**
     * Creates an empty registry whose first ID for each role has sequence one.
     */
    public PeopleRegistry() {
        for (PersonRole role : PersonRole.values()) {
            lastAllocatedSequences.put(role, 0L);
        }
    }

    /**
     * Creates an independent registry with the same ordered people and complete allocation history.
     * Records can be shared because the supported record types are immutable.
     */
    public PeopleRegistry(PeopleRegistry other) {
        this(requireNonNull(other).exportState());
    }

    /**
     * Creates an independent registry from a validated snapshot without reallocating or sorting IDs.
     * Persisted counters must include deleted IDs; deriving counters from remaining people would reuse identities.
     */
    public PeopleRegistry(PeopleRegistryState state) {
        requireNonNull(state);
        for (PersonRecord person : state.getPeople()) {
            people.put(person.getId(), person);
        }
        lastAllocatedSequences.putAll(state.getLastAllocatedSequences());
    }

    /**
     * Adds a student at the end of the global order and returns the record with its allocated ID.
     * Invalid fields or a duplicate do not consume an ID or change the registry.
     *
     * @throws DuplicatePersonException if a student has the same normalized name and parent phone.
     * @throws PersonIdExhaustedException if all positive student sequences have been allocated.
     */
    public Student addStudent(ContactDetails contactDetails, EducationLevel level, Phone parentPhone) {
        requireAllNonNull(contactDetails, level, parentPhone);
        Student student = new Student(getNextId(PersonRole.STUDENT), contactDetails, level, parentPhone);
        commitAddition(student);
        return student;
    }

    /**
     * Adds a tutor at the end of the global order and returns the record with its allocated ID.
     * Invalid fields or a duplicate do not consume an ID or change the registry.
     *
     * @throws DuplicatePersonException if a tutor has the same normalized name and own phone.
     * @throws PersonIdExhaustedException if all positive tutor sequences have been allocated.
     */
    public Tutor addTutor(ContactDetails contactDetails) {
        requireNonNull(contactDetails);
        Tutor tutor = new Tutor(getNextId(PersonRole.TUTOR), contactDetails);
        commitAddition(tutor);
        return tutor;
    }

    /**
     * Adds a parent at the end of the global order and returns the record with its allocated ID.
     * Invalid fields or a duplicate do not consume an ID or change the registry.
     *
     * @throws DuplicatePersonException if a parent has the same normalized name and own phone.
     * @throws PersonIdExhaustedException if all positive parent sequences have been allocated.
     */
    public Parent addParent(ContactDetails contactDetails) {
        requireNonNull(contactDetails);
        Parent parent = new Parent(getNextId(PersonRole.PARENT), contactDetails);
        commitAddition(parent);
        return parent;
    }

    /**
     * Returns an immutable snapshot of all roles in global creation order.
     * Later additions or removals do not change a previously returned snapshot.
     */
    public List<PersonRecord> getPeople() {
        return List.copyOf(people.values());
    }

    /**
     * Returns the immutable record with the supplied stable ID, or an empty optional if absent.
     */
    public Optional<PersonRecord> getPerson(PersonId id) {
        requireNonNull(id);
        return Optional.ofNullable(people.get(id));
    }

    /**
     * Removes and returns the record only if the caller's canonical relationship query reports no references.
     * The query receives the stored immutable record and must not mutate this registry.
     * Missing records, references, or query exceptions leave the registry unchanged; removal never lowers counters.
     * The registry stores no lessons or attendance and cannot determine these references itself.
     *
     * @throws NullPointerException if the ID or relationship query is null.
     * @throws PersonNotFoundException if the ID is absent, before the relationship query is invoked.
     * @throws ReferencedPersonException if the relationship query reports references.
     */
    public PersonRecord remove(PersonId id, Predicate<PersonRecord> isReferenced) {
        requireAllNonNull(id, isReferenced);
        PersonRecord person = people.get(id);
        if (person == null) {
            throw new PersonNotFoundException();
        }
        if (isReferenced.test(person)) {
            throw new ReferencedPersonException(id);
        }
        people.remove(id);
        return person;
    }

    /**
     * Returns an immutable snapshot containing both the ordered people and every role's allocation history.
     * Storage and rollback consumers must retain both parts, including counters for empty roles.
     */
    public PeopleRegistryState exportState() {
        return new PeopleRegistryState(getPeople(), lastAllocatedSequences);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof PeopleRegistry otherRegistry)) {
            return false;
        }

        return getPeople().equals(otherRegistry.getPeople())
                && lastAllocatedSequences.equals(otherRegistry.lastAllocatedSequences);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPeople(), lastAllocatedSequences);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("people", getPeople())
                .add("lastAllocatedSequences", lastAllocatedSequences)
                .toString();
    }

    /**
     * Returns an uncommitted candidate ID, checking exhaustion before incrementing the counter.
     */
    private PersonId getNextId(PersonRole role) {
        long lastSequence = lastAllocatedSequences.get(role);
        if (lastSequence == Long.MAX_VALUE) {
            throw new PersonIdExhaustedException(role);
        }
        return PersonId.of(role, lastSequence + 1);
    }

    /**
     * Rejects duplicate IDs and role-specific keys before committing the record and allocation counter together.
     */
    private void commitAddition(PersonRecord person) {
        if (people.containsKey(person.getId()) || people.values().stream().anyMatch(person::isDuplicateOf)) {
            throw new DuplicatePersonException();
        }
        people.put(person.getId(), person);
        lastAllocatedSequences.put(person.getRole(), person.getId().getSequence());
    }
}
