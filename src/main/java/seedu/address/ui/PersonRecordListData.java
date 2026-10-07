package seedu.address.ui;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import seedu.address.model.person.Parent;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Student;
import seedu.address.model.person.Tutor;

/**
 * An immutable people-view snapshot with an optional role filter and current-view positions.
 * The supplied order is preserved; positions are separate from each record's stable identity.
 * This projection does not own operational data or update the active people view.
 */
public final class PersonRecordListData {

    private final List<Entry> entries;
    private final Optional<PersonRole> roleFilter;

    /**
     * Creates a view snapshot from ordered immutable records, validating the entire input before filtering.
     * Later changes to the supplied collection do not affect this snapshot.
     *
     * @throws NullPointerException if the collection, filter, or any record is null.
     * @throws IllegalArgumentException if any record has an unsupported implementation or a duplicate ID.
     */
    public PersonRecordListData(List<? extends PersonRecord> people, Optional<PersonRole> roleFilter) {
        requireNonNull(people);
        requireNonNull(roleFilter);
        List<? extends PersonRecord> peopleSnapshot = List.copyOf(people);
        Set<PersonId> seenIds = new HashSet<>();
        List<Entry> selectedEntries = new ArrayList<>();

        for (PersonRecord person : peopleSnapshot) {
            requireSupportedRecord(person);
            checkArgument(seenIds.add(person.getId()), "People-view snapshots require unique person IDs.");
            if (roleFilter.isEmpty() || person.getRole() == roleFilter.get()) {
                selectedEntries.add(new Entry(person, selectedEntries.size() + 1));
            }
        }

        entries = List.copyOf(selectedEntries);
        this.roleFilter = roleFilter;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public int getCount() {
        return entries.size();
    }

    public Optional<PersonRole> getRoleFilter() {
        return roleFilter;
    }

    /**
     * Returns listing feedback with the displayed count, the selected role, and an explicit empty-state message.
     */
    public String getSummary() {
        String summary = "Listed " + getCount() + " person(s)"
                + roleFilter.map(role -> " with role: " + role.getValue()).orElse("") + ".";
        return entries.isEmpty() ? summary + " No persons to display." : summary;
    }

    /**
     * Rejects implementations whose mutable state could change a supposedly immutable view snapshot.
     */
    private static void requireSupportedRecord(PersonRecord person) {
        requireNonNull(person);
        checkArgument(person instanceof Student || person instanceof Tutor || person instanceof Parent,
                "People-view snapshots support only immutable Student, Tutor, and Parent records.");
    }

    /**
     * An immutable record paired with its positive one-based position in the displayed view.
     */
    public record Entry(PersonRecord person, int displayedIndex) {

        /**
         * Creates an entry suitable for a person card without interpreting the stable ID as a view position.
         *
         * @throws NullPointerException if the record is null.
         * @throws IllegalArgumentException if the record implementation is unsupported or the position is not positive.
         */
        public Entry {
            requireSupportedRecord(person);
            checkArgument(displayedIndex > 0, "Displayed person positions must be positive.");
        }
    }
}
