package seedu.address.model.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.LessonQueryTestData.createData;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;

import seedu.address.model.PonHubData;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.Student;
import seedu.address.model.query.LessonQueries;

public class PersonRelationshipSearchTest {
    private final PonHubData data = createData();
    private final LessonQueries queries = new LessonQueries(data);

    @Test
    public void students_combineOwnFieldsAndOneCurrentLessonWithDistinctStableIdentities() {
        assertIds(search(Map.of()), "S1", "S2", "S3");
        assertIds(search(Map.of(SearchField.NAME, "Alex")), "S1", "S3");
        var result = search(Map.of(SearchField.DAY, "mon", SearchField.SUBJECT, "ath",
                SearchField.TUTOR_NAME, "MEI", SearchField.ROOM, "r1",
                SearchField.START_TIME, "0900", SearchField.END_TIME, "1000"));
        assertIds(result, "S1", "S2");
        assertEquals(List.of("L10"), result.contexts().get(new PersonId("S1")).stream()
                .map(match -> match.lesson().orElseThrow().getLessonId().toString()).toList());
        assertIds(search(Map.of(SearchField.DAY, "Tue", SearchField.SUBJECT, "Math")));
        assertIds(search(Map.of(SearchField.DAY, "Mon", SearchField.SUBJECT, "Math", SearchField.PHONE, "00012345")),
                "S2");
        assertIds(search(Map.of(SearchField.DAY, "Mon", SearchField.PHONE, "99999999")));
        assertIds(search(Map.of(SearchField.DAY, "Wed", SearchField.SUBJECT, "中文")), "S2");
        assertIds(search(Map.of(SearchField.SUBJECT, "Math", SearchField.EMAIL, "@example")), "S2");
        assertIds(search(Map.of(SearchField.SUBJECT, "Math", SearchField.LEVEL, "P3")), "S1");
        assertIds(search(Map.of(SearchField.SUBJECT, "Math", SearchField.PARENT_PHONE, "00888888")));
        assertIds(search(Map.of(SearchField.TUTOR_NAME, "T1")));
    }

    @Test
    public void indexedResults_matchIndependentRawRecordScanForSeededConjunctions() {
        Random random = new Random(94);
        Map<SearchField, List<String>> values = Map.of(
                SearchField.NAME, List.of("al", "Jamie", "nobody"),
                SearchField.DAY, List.of("Mon", "Tue", "Wed", "Sun"),
                SearchField.SUBJECT, List.of("math", "science", "中文", "missing"),
                SearchField.TUTOR_NAME, List.of("mei", "T1", "assigned"),
                SearchField.ROOM, List.of("r1", "R2", "0"),
                SearchField.START_TIME, List.of("0000", "0900", "1000"),
                SearchField.END_TIME, List.of("1000", "1100", "2359"));
        var before = data.exportState();
        var lessonFilter = queries.getLessonFilter();
        for (int trial = 0; trial < 1000; trial++) {
            Map<SearchField, String> fields = new EnumMap<>(SearchField.class);
            values.forEach((field, options) -> {
                if (random.nextBoolean()) {
                    fields.put(field, options.get(random.nextInt(options.size())));
                }
            });
            if (fields.containsKey(SearchField.START_TIME) && fields.containsKey(SearchField.END_TIME)) {
                fields.put(SearchField.END_TIME, "2359");
            }
            SearchCriteria criteria = new SearchCriteria(SearchCategory.STUDENT, fields);
            List<String> expected = data.getPeople().stream().filter(Student.class::isInstance).map(Student.class::cast)
                    .filter(student -> criteria.matches(SearchField.NAME, student.getName().fullName))
                    .filter(student -> fields.keySet().stream().allMatch(PersonContactSearch.OWN_FIELDS::contains)
                            || data.getLessons().stream().anyMatch(lesson -> lesson.hasEnrolledStudent(student.getId())
                            && criteria.matches(SearchField.DAY, lesson.getTimeSlot().getDay().toString())
                            && criteria.matches(SearchField.START_TIME, lesson.getTimeSlot().getStartTime().toString())
                            && criteria.matches(SearchField.END_TIME, lesson.getTimeSlot().getEndTime().toString())
                            && criteria.matches(SearchField.SUBJECT, lesson.getSubject().toString())
                            && criteria.matches(SearchField.ROOM, lesson.getRoom().toString())
                            && criteria.matches(SearchField.TUTOR_NAME,
                                    data.getPerson(lesson.getTutorId()).orElseThrow().getName().fullName)))
                    .map(student -> student.getId().toString()).toList();
            assertEquals(expected, PersonRelationshipSearch.search(data.getPeople(), queries, criteria).people()
                    .stream().map(person -> person.getId().toString()).toList(), fields.toString());
        }
        assertSame(before, data.exportState());
        assertSame(lessonFilter, queries.getLessonFilter());
    }

    @Test
    public void resultsAndContext_areDefensivelyImmutableAndRejectInvalidArguments() {
        var result = search(Map.of(SearchField.SUBJECT, "Math"));
        assertThrows(UnsupportedOperationException.class, result.people()::clear);
        assertThrows(UnsupportedOperationException.class, result.contexts()::clear);
        assertThrows(UnsupportedOperationException.class, () -> result.contexts().get(new PersonId("S1")).clear());
        var student = (Student) data.getPerson(new PersonId("S1")).orElseThrow();
        var match = new PersonSearchMatch(Optional.of(student), Optional.empty());
        var mutable = new ArrayList<>(List.of(match));
        var copied = new PersonSearchResult(List.of(student), Map.of(student.getId(), mutable));
        mutable.clear();
        assertEquals(List.of(match), copied.contexts().get(student.getId()));
        assertThrows(NullPointerException.class, () -> new PersonSearchMatch(null, Optional.empty()));
        assertThrows(NullPointerException.class, () -> new PersonSearchMatch(Optional.empty(), null));
        assertThrows(IllegalArgumentException.class, () -> new PersonSearchMatch(Optional.empty(), Optional.empty()));
        assertThrows(NullPointerException.class, () -> new PersonSearchResult(null, Map.of()));
        assertThrows(NullPointerException.class, () -> new PersonSearchResult(List.of(), null));
        SearchCriteria criteria = new SearchCriteria(SearchCategory.STUDENT, Map.of());
        assertThrows(NullPointerException.class, () ->
                PersonRelationshipSearch.search(null, queries, criteria));
        assertThrows(NullPointerException.class, () ->
                PersonRelationshipSearch.search(data.getPeople(), null, criteria));
        assertThrows(NullPointerException.class, () ->
                PersonRelationshipSearch.search(data.getPeople(), queries, null));
        for (SearchCategory category : List.of(SearchCategory.TUTOR)) {
            assertThrows(IllegalArgumentException.class, () ->
                    PersonRelationshipSearch.search(data.getPeople(), queries,
                            new SearchCriteria(category, Map.of(SearchField.DAY, "Mon"))));
        }
        assertTrue(search(Map.of(SearchField.NAME, "no-match")).contexts().isEmpty());
    }

    private PersonSearchResult search(Map<SearchField, String> fields) {
        return PersonRelationshipSearch.search(data.getPeople(), queries,
                new SearchCriteria(SearchCategory.STUDENT, fields));
    }

    private static void assertIds(PersonSearchResult result, String... expected) {
        assertEquals(List.of(expected), result.people().stream().map(person -> person.getId().toString()).toList());
    }
}
