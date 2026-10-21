package ponhub.model.search;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;

/**
 * One immutable people search snapshot with context keyed by stable person ID.
 */
public record PersonSearchResult(List<PersonRecord> people, Map<PersonId, List<PersonSearchMatch>> contexts) {
    /**
     * Copies both the result list and every context list so callers cannot change a published projection.
     */
    public PersonSearchResult {
        people = List.copyOf(requireNonNull(people));
        contexts = requireNonNull(contexts).entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }
}
