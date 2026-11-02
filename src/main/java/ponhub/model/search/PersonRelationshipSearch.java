package ponhub.model.search;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.AppUtil.checkArgument;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import ponhub.model.lesson.LessonId;
import ponhub.model.person.Parent;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.Phone;
import ponhub.model.query.LessonQueries;
import ponhub.model.query.LessonView;

/**
 * Combines own-field matches with the existing indexed lesson search and authoritative current rosters.
 * Parent links use exact phone equality; Tutor matches use stable assignment and that same lesson's current roster.
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
        checkArgument(criteria.getCategory() != SearchCategory.LESSON,
                "Relationship search is not available for " + criteria.getCategory().name().toLowerCase(Locale.ROOT)
                        + ".");
        List<PersonRecord> ownMatches = PersonContactSearch.search(people, new SearchCriteria(criteria.getCategory(),
                selectFields(criteria, true)));
        if (criteria.getCategory() == SearchCategory.PARENT) {
            return searchParents(ownMatches, queries, criteria);
        }
        if (criteria.getCategory() == SearchCategory.TUTOR) {
            return searchTutors(ownMatches, queries, criteria);
        }
        Set<PersonId> eligibleIds = ownMatches.stream().map(PersonRecord::getId).collect(Collectors.toSet());
        Map<PersonId, List<PersonSearchMatch>> contexts = new LinkedHashMap<>();
        var lessonCriteria = new SearchCriteria(SearchCategory.LESSON, selectFields(criteria, false));
        for (var lesson : queries.searchLessons(lessonCriteria)) {
            for (var student : queries.getLessonRoster(lesson.getLessonId())) {
                if (eligibleIds.contains(student.getStudentId())) {
                    contexts.computeIfAbsent(student.getStudentId(), id -> new ArrayList<>())
                            .add(new PersonSearchMatch(Optional.empty(), Optional.of(lesson)));
                }
            }
        }
        return new PersonSearchResult(ownMatches.stream()
                .filter(person -> contexts.containsKey(person.getId())).toList(),
                contexts);
    }

    private static Map<SearchField, String> selectFields(SearchCriteria criteria, boolean isOwnField) {
        Map<SearchField, String> selectedFields = new EnumMap<>(SearchField.class);
        criteria.getFilters().forEach((field, value) -> {
            if (PersonContactSearch.OWN_FIELDS.contains(field) == isOwnField) {
                selectedFields.put(field, value);
            }
        });
        return selectedFields;
    }

    private static PersonSearchResult searchTutors(List<PersonRecord> tutors, LessonQueries queries,
            SearchCriteria criteria) {
        Set<PersonId> eligibleIds = tutors.stream().map(PersonRecord::getId).collect(Collectors.toSet());
        Map<PersonId, List<PersonSearchMatch>> contexts = new LinkedHashMap<>();
        boolean hasStudentFilter = criteria.getValue(SearchField.STUDENT_NAME).isPresent();
        var lessonCriteria = new SearchCriteria(SearchCategory.LESSON, selectFields(criteria, false));
        for (var lesson : queries.searchLessons(lessonCriteria)) {
            if (!eligibleIds.contains(lesson.getTutorId())) {
                continue;
            }
            var matches = contexts.computeIfAbsent(lesson.getTutorId(), unused -> new ArrayList<>());
            if (!hasStudentFilter) {
                matches.add(new PersonSearchMatch(Optional.empty(), Optional.of(lesson)));
                continue;
            }
            for (var student : queries.getLessonRoster(lesson.getLessonId())) {
                if (criteria.matches(SearchField.STUDENT_NAME, student.getName())) {
                    matches.add(new PersonSearchMatch(Optional.of(queries.getStudentRecord(student.getStudentId())),
                            Optional.of(lesson)));
                }
            }
        }
        return new PersonSearchResult(tutors.stream().filter(tutor -> contexts.containsKey(tutor.getId())).toList(),
                contexts);
    }

    /**
     * Memoizes matching relationships per exact phone so parents sharing a number reuse the same derived context.
     */
    private static PersonSearchResult searchParents(List<PersonRecord> parents, LessonQueries queries,
            SearchCriteria criteria) {
        Map<SearchField, String> lessonFields = selectFields(criteria, false);
        lessonFields.remove(SearchField.STUDENT_NAME);
        boolean hasLessonFilters = !lessonFields.isEmpty();
        Map<LessonId, LessonView> lessons = hasLessonFilters
                ? queries.searchLessons(new SearchCriteria(SearchCategory.LESSON, lessonFields)).stream()
                        .collect(Collectors.toMap(LessonView::getLessonId, lesson -> lesson)) : Map.of();
        Map<String, List<PersonSearchMatch>> phoneMatches = new LinkedHashMap<>();
        Map<PersonId, List<PersonSearchMatch>> contexts = new LinkedHashMap<>();
        for (PersonRecord record : parents) {
            Parent parent = (Parent) record;
            Phone phone = parent.getContactDetails().getPhone().orElseThrow();
            List<PersonSearchMatch> matches = phoneMatches.computeIfAbsent(phone.value, unused -> {
                List<PersonSearchMatch> linkedMatches = new ArrayList<>();
                for (var student : queries.getStudentsByParentPhone(phone)) {
                    if (!criteria.matches(SearchField.STUDENT_NAME, student.getName().fullName)) {
                        continue;
                    }
                    if (!hasLessonFilters) {
                        linkedMatches.add(new PersonSearchMatch(Optional.of(student), Optional.empty()));
                        continue;
                    }
                    for (var lesson : queries.getStudentLessons(student.getId())) {
                        if (lessons.containsKey(lesson.getLessonId())) {
                            linkedMatches.add(new PersonSearchMatch(Optional.of(student), Optional.of(lesson)));
                        }
                    }
                }
                return List.copyOf(linkedMatches);
            });
            if (!matches.isEmpty()) {
                contexts.put(parent.getId(), matches);
            }
        }
        return new PersonSearchResult(parents.stream().filter(parent -> contexts.containsKey(parent.getId())).toList(),
                contexts);
    }
}
