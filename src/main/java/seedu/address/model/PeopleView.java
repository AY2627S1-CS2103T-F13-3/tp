package seedu.address.model;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;

/**
 * A read-only current people view derived from one supplied canonical data root.
 * Matching records retain global creation order; list positions are independent of stable IDs.
 * This prepared component does not activate canonical commands or own writable operational data.
 * Callers refresh it after committed changes and rollback on the application's model thread.
 */
public final class PeopleView {

    private final PonHubData source;
    private final ObservableList<PersonRecord> people = FXCollections.observableArrayList();
    private final ObservableList<PersonRecord> readOnlyPeople = FXCollections.unmodifiableObservableList(people);
    private Optional<PersonRole> roleFilter = Optional.empty();

    /**
     * Creates an all-roles projection of the supplied canonical root without copying its writable state.
     */
    public PeopleView(PonHubData source) {
        this.source = requireNonNull(source);
        refresh();
    }

    /**
     * Returns the same unmodifiable observable list across filter changes and refreshes.
     * Cards and person-index resolution must use this shared current view.
     */
    public ObservableList<PersonRecord> getPeople() {
        return readOnlyPeople;
    }

    public Optional<PersonRole> getRoleFilter() {
        return roleFilter;
    }

    /**
     * Selects all roles or one role and rebuilds the view without changing canonical data or counters.
     * An empty matching list is a successful result.
     */
    public void setRoleFilter(Optional<PersonRole> roleFilter) {
        this.roleFilter = requireNonNull(roleFilter);
        refresh();
    }

    /**
     * Rebuilds the current role-filtered projection from the same root after a committed change or rollback.
     * The source does not publish change events; its owner calls this method explicitly.
     * When attached to JavaFX controls, refreshes run on the JavaFX application thread.
     */
    public void refresh() {
        people.setAll(source.getPeople().stream()
                .filter(person -> roleFilter.isEmpty() || person.getRole() == roleFilter.get())
                .toList());
    }
}
