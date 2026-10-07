package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.person.exceptions.DuplicatePersonException;

/**
 * An immutable, validated snapshot of ordered people and their per-role allocation history.
 * Allocation counters include successfully allocated IDs whose people have since been removed.
 */
public final class PeopleRegistryState {

    private final List<PersonRecord> people;
    private final Map<PersonRole, Long> lastAllocatedSequences;

    /**
     * Creates a snapshot, defensively copying both collections and preserving the supplied people order.
     * Each role needs a nonnegative counter at least as large as every retained ID for that role.
     * Zero means no ID has been allocated; {@link Long#MAX_VALUE} means the role is exhausted.
     * Only the immutable student, tutor, and parent record types are supported.
     *
     * @throws NullPointerException if a collection, record, role, or counter is null.
     * @throws IllegalArgumentException if counters are incomplete, negative, or below a retained ID,
     *     or a record has an unsupported implementation.
     * @throws DuplicatePersonException if IDs or role-specific duplicate keys repeat.
     */
    public PeopleRegistryState(List<? extends PersonRecord> people, Map<PersonRole, Long> lastAllocatedSequences) {
        requireNonNull(people);
        requireNonNull(lastAllocatedSequences);
        this.people = List.copyOf(people);
        this.lastAllocatedSequences = Map.copyOf(lastAllocatedSequences);
        validateCounters();
        validatePeople();
    }

    /**
     * Returns the immutable people snapshot in the supplied global creation order.
     */
    public List<PersonRecord> getPeople() {
        return people;
    }

    /**
     * Returns an immutable map containing the last successfully allocated sequence for every role.
     */
    public Map<PersonRole, Long> getLastAllocatedSequences() {
        return lastAllocatedSequences;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof PeopleRegistryState otherState)) {
            return false;
        }

        return people.equals(otherState.people) && lastAllocatedSequences.equals(otherState.lastAllocatedSequences);
    }

    @Override
    public int hashCode() {
        return Objects.hash(people, lastAllocatedSequences);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("people", people)
                .add("lastAllocatedSequences", lastAllocatedSequences)
                .toString();
    }

    /**
     * Rejects incomplete or negative allocation history without deriving it from retained people.
     */
    private void validateCounters() {
        checkArgument(lastAllocatedSequences.size() == PersonRole.values().length,
                "Allocation state must contain every person role.");
        for (PersonRole role : PersonRole.values()) {
            Long sequence = requireNonNull(lastAllocatedSequences.get(role));
            checkArgument(sequence >= 0, "Last allocated sequences must be nonnegative.");
        }
    }

    /**
     * Rejects unsupported records, duplicate identities or keys, and IDs beyond the supplied allocation history.
     */
    private void validatePeople() {
        Set<PersonId> ids = new HashSet<>();
        for (int i = 0; i < people.size(); i++) {
            PersonRecord person = people.get(i);
            checkArgument(person instanceof Student || person instanceof Tutor || person instanceof Parent,
                    "Registry snapshots support only immutable student, tutor, and parent records.");
            if (!ids.add(person.getId())) {
                throw new DuplicatePersonException();
            }
            for (int j = 0; j < i; j++) {
                if (person.isDuplicateOf(people.get(j))) {
                    throw new DuplicatePersonException();
                }
            }
            checkArgument(person.getId().getSequence() <= lastAllocatedSequences.get(person.getRole()),
                    "Allocation state must include every retained person ID.");
        }
    }
}
