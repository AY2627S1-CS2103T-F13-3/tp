package ponhub.model;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.PersonRole;
import ponhub.model.query.LessonQueries;
import ponhub.model.search.PersonRelationshipSearch;
import ponhub.model.search.PersonSearchMatch;
import ponhub.model.search.SearchCriteria;

/**
 * A read-only current people view derived from one supplied canonical data root.
 * Matching records retain global creation order; list positions are independent of stable IDs.
 * The application session owns this projection without exposing writable operational data.
 * Callers refresh it after committed changes and rollback on the application's model thread.
 */
public final class PeopleView {

    private final PonHubData source;
    private final ObservableList<PersonRecord> people = FXCollections.observableArrayList();
    private final ObservableList<PersonRecord> readOnlyPeople = FXCollections.unmodifiableObservableList(people);
    private Optional<PersonRole> roleFilter = Optional.empty();
    private Optional<SearchCriteria> searchCriteria = Optional.empty();
    private final LessonQueries queries;
    private final ReadOnlyStringWrapper resultContext = new ReadOnlyStringWrapper("All people");
    private final ReadOnlyObjectWrapper<Map<PersonId, List<PersonSearchMatch>>> contexts =
            new ReadOnlyObjectWrapper<>(Map.of());

    /**
     * Creates an all-roles projection of the supplied canonical root without copying its writable state.
     */
    public PeopleView(PonHubData source) {
        this(source, new LessonQueries(source));
    }

    /**
     * Shares the session's existing query indexes instead of building another live membership graph.
     */
    public PeopleView(PonHubData source, LessonQueries queries) {
        this.source = requireNonNull(source);
        this.queries = requireNonNull(queries);
        refresh();
    }

    /**
     * Returns the same unmodifiable observable list across filter changes and refreshes.
     * Cards and person-index resolution must use this shared current view.
     */
    public ObservableList<PersonRecord> getPeople() {
        return readOnlyPeople;
    }

    public ReadOnlyObjectProperty<Map<PersonId, List<PersonSearchMatch>>> searchContextProperty() {
        return contexts.getReadOnlyProperty();
    }

    /**
     * Starts a transaction over this view and its single canonical root.
     */
    public CanonicalTransaction beginTransaction() {
        return new CanonicalTransaction(source, this);
    }

    /**
     * Returns the active people scope and normalized search criteria independently of transient command feedback.
     */
    public ReadOnlyStringProperty resultContextProperty() {
        return resultContext.getReadOnlyProperty();
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
        searchCriteria = Optional.empty();
        refresh();
    }

    public Optional<SearchCriteria> getSearchCriteria() {
        return searchCriteria;
    }

    /**
     * Selects matching people from the complete root, validating before changing the current view.
     * Search criteria are immutable and retain the category and every supplied own-contact filter.
     */
    public void setSearchCriteria(SearchCriteria criteria) {
        var matches = PersonRelationshipSearch.search(source.getPeople(), queries, requireNonNull(criteria));
        roleFilter = criteria.getCategory().getPersonRole();
        searchCriteria = Optional.of(criteria);
        contexts.set(matches.contexts());
        updateResultContext();
        people.setAll(matches.people());
    }

    /**
     * Copies the complete filter into another projection over its own canonical root.
     */
    public void copyFilterFrom(PeopleView other) {
        requireNonNull(other);
        if (other.searchCriteria.isPresent()) {
            setSearchCriteria(other.searchCriteria.orElseThrow());
        } else {
            setRoleFilter(other.roleFilter);
        }
    }

    /**
     * Rebuilds the current role/search-filtered projection from the same root after a committed change or rollback.
     * This view's owner calls this method explicitly; lesson queries subscribe to the source's state property.
     * When attached to JavaFX controls, refreshes run on the JavaFX application thread.
     */
    public void refresh() {
        updateResultContext();
        if (searchCriteria.isPresent()) {
            var matches = PersonRelationshipSearch.search(source.getPeople(), queries, searchCriteria.orElseThrow());
            contexts.set(matches.contexts());
            people.setAll(matches.people());
            return;
        }
        contexts.set(Map.of());
        people.setAll(source.getPeople().stream()
                .filter(person -> roleFilter.isEmpty() || person.getRole() == roleFilter.get())
                .toList());
    }

    /**
     * Describes the selected query, including a category-only search and a search with zero matches.
     */
    private void updateResultContext() {
        if (searchCriteria.isPresent()) {
            SearchCriteria criteria = searchCriteria.orElseThrow();
            String category = criteria.getCategory().name().toLowerCase(Locale.ROOT);
            String filters = criteria.getFilters().entrySet().stream()
                    .map(entry -> " " + entry.getKey().getPrefix() + entry.getValue())
                    .collect(Collectors.joining());
            resultContext.set(Character.toUpperCase(category.charAt(0)) + category.substring(1)
                    + " search: c/" + category + filters);
        } else {
            resultContext.set(roleFilter.map(role -> {
                String roleName = role.name().toLowerCase(Locale.ROOT);
                return Character.toUpperCase(roleName.charAt(0)) + roleName.substring(1) + "s";
            }).orElse("All people"));
        }
    }
}
