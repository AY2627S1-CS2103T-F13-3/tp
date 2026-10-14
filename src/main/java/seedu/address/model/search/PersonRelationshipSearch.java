package seedu.address.model.search;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.query.LessonQueries;

/**
 * Combines own-field matches with the existing indexed lesson search and authoritative current rosters.
 * Parent and Tutor relationships activate in their separate increments.
 */
public final class PersonRelationshipSearch {
    private PersonRelationshipSearch() {
    }

    /**
     * Returns distinct canonical people in creation order and only the lessons satisfying every supplied condition.
     * It leaves the lesson filter, people records, allocation counters and attendance unchanged.
     */
    public static PersonSearchResult search(List<PersonRecord> people, LessonQueries queries, SearchCriteria criteria) {
        requireNonNull(people);
        requireNonNull(queries);
        requireNonNull(criteria);
        if (criteria.getFilters().keySet().stream().allMatch(PersonContactSearch.OWN_FIELDS::contains)) {
            return new PersonSearchResult(PersonContactSearch.search(people, criteria), Map.of());
        }
        checkArgument(criteria.getCategory() == SearchCategory.STUDENT,
                "Relationship search is not available for " + criteria.getCategory().name().toLowerCase(Locale.ROOT)
                        + " yet.");
        List<PersonRecord> ownMatches = PersonContactSearch.search(people, new SearchCriteria(criteria.getCategory(),
                fields(criteria, true)));
        Set<PersonId> eligible = ownMatches.stream().map(PersonRecord::getId).collect(Collectors.toSet());
        Map<PersonId, List<PersonSearchMatch>> contexts = new LinkedHashMap<>();
        var lessonCriteria = new SearchCriteria(SearchCategory.LESSON, fields(criteria, false));
        for (var lesson : queries.searchLessons(lessonCriteria)) {
            for (var student : queries.getLessonRoster(lesson.getLessonId())) {
                if (eligible.contains(student.getStudentId())) {
                    contexts.computeIfAbsent(student.getStudentId(), id -> new ArrayList<>())
                            .add(new PersonSearchMatch(Optional.empty(), Optional.of(lesson)));
                }
            }
        }
        return new PersonSearchResult(ownMatches.stream()
                .filter(person -> contexts.containsKey(person.getId())).toList(),
                contexts);
    }

    private static Map<SearchField, String> fields(SearchCriteria criteria, boolean own) {
        return criteria.getFilters().entrySet().stream()
                .filter(entry -> PersonContactSearch.OWN_FIELDS.contains(entry.getKey()) == own)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
