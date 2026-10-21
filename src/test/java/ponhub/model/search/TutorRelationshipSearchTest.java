package ponhub.model.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.RelationshipSearchTestData.createData;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.lesson.LessonId;
import ponhub.model.person.PersonId;
import ponhub.model.person.Tutor;
import ponhub.model.query.LessonQueries;

public class TutorRelationshipSearchTest {
    private final PonHubData data = createData();
    private final LessonQueries queries = new LessonQueries(data);

    @Test
    public void tutorQueries_useStableAssignmentAndOneLessonWithSameRosterStudent() {
        assertIds(search(Map.of()), "T1", "T2", "T3");
        assertIds(search(Map.of(SearchField.NAME, "Mei")), "T1", "T2");
        assertIds(search(Map.of(SearchField.DAY, "Mon", SearchField.SUBJECT, "Science")), "T2");
        assertIds(search(Map.of(SearchField.DAY, "Mon", SearchField.SUBJECT, "Science",
                SearchField.STUDENT_NAME, "Alex")));
        assertIds(search(Map.of(SearchField.DAY, "Tue", SearchField.SUBJECT, "Math")));
        assertIds(search(Map.of(SearchField.STUDENT_NAME, "Jamie", SearchField.DAY, "Mon",
                SearchField.SUBJECT, "Science")));
        assertIds(search(Map.of(SearchField.DAY, "Mon", SearchField.SUBJECT, "Math",
                SearchField.EMAIL, "@EXAMPLE")), "T1");
        assertIds(search(Map.of(SearchField.DAY, "Wed", SearchField.SUBJECT, "中文",
                SearchField.EMAIL, "@EXAMPLE")));
        assertIds(search(Map.of(SearchField.PHONE, "00999888", SearchField.DAY, "Mon")), "T2");
        assertIds(search(Map.of(SearchField.NAME, "Unassigned", SearchField.DAY, "Mon")));
        var result = search(Map.of(SearchField.STUDENT_NAME, "a", SearchField.DAY, "Mon",
                SearchField.SUBJECT, "Math", SearchField.ROOM, "r1",
                SearchField.START_TIME, "0900", SearchField.END_TIME, "1000"));
        assertIds(result, "T1");
        var matches = result.contexts().get(new PersonId("T1"));
        assertEquals(List.of(new PersonId("S1"), new PersonId("S2")), matches.stream()
                .map(match -> match.student().orElseThrow().getId()).toList());
        assertTrue(matches.stream()
                .allMatch(match -> match.lesson().orElseThrow().getLessonId().equals(new LessonId("L10"))));
        assertThrows(IllegalArgumentException.class, () -> new SearchCriteria(SearchCategory.TUTOR,
                Map.of(SearchField.TUTOR_NAME, "Mei")));
    }

    @Test
    public void activeTutorProjection_refreshesCurrentRosterAndImmutableStudentLookup() {
        var before = data.exportState();
        PeopleView view = new PeopleView(data, queries);
        var criteria = new SearchCriteria(SearchCategory.TUTOR,
                Map.of(SearchField.STUDENT_NAME, "Alex", SearchField.DAY, "Mon", SearchField.SUBJECT, "Math"));
        view.setSearchCriteria(criteria);
        var people = view.getPeople();
        assertEquals(1, people.size());
        data.unenrolStudent(new PersonId("S1"), new LessonId("L10"));
        view.refresh();
        assertTrue(people.isEmpty());
        data.resetData(before);
        view.refresh();
        assertEquals(1, people.size());
        assertSame(people, view.getPeople());
        assertSame(data.getPerson(new PersonId("S1")).orElseThrow(), queries.getStudentRecord(new PersonId("S1")));
        assertThrows(IllegalArgumentException.class, () -> queries.getStudentRecord(new PersonId("T1")));
        assertThrows(IllegalArgumentException.class, () -> queries.getStudentRecord(new PersonId("S99")));
        assertThrows(NullPointerException.class, () -> queries.getStudentRecord(null));
    }

    @Test
    public void indexedTutorQueries_matchIndependentAssignedLessonAndRosterScan() {
        Random random = new Random(96);
        Map<SearchField, List<String>> values = Map.of(
                SearchField.NAME, List.of("Mei", "Unassigned", "missing"),
                SearchField.PHONE, List.of("00112233", "00999888", "12345678"),
                SearchField.STUDENT_NAME, List.of("Alex", "Jamie", "missing"),
                SearchField.DAY, List.of("Mon", "Tue", "Wed"),
                SearchField.SUBJECT, List.of("Math", "Science", "中文"),
                SearchField.ROOM, List.of("r1", "R2", "R10"));
        var before = data.exportState();
        for (int trial = 0; trial < 1000; trial++) {
            Map<SearchField, String> fields = new EnumMap<>(SearchField.class);
            values.forEach((field, options) -> {
                if (random.nextBoolean()) {
                    fields.put(field, options.get(random.nextInt(options.size())));
                }
            });
            SearchCriteria criteria = new SearchCriteria(SearchCategory.TUTOR, fields);
            boolean related = fields.keySet().stream()
                    .anyMatch(field -> !PersonContactSearch.OWN_FIELDS.contains(field));
            var expected = data.getPeople().stream().filter(Tutor.class::isInstance).map(Tutor.class::cast)
                    .filter(tutor -> criteria.matches(SearchField.NAME, tutor.getName().fullName)
                            && criteria.matches(SearchField.PHONE,
                                    tutor.getContactDetails().getPhone().orElseThrow().value))
                    .filter(tutor -> !related || data.getLessons().stream().anyMatch(lesson ->
                            lesson.getTutorId().equals(tutor.getId())
                            && criteria.matches(SearchField.DAY, lesson.getTimeSlot().getDay().toString())
                            && criteria.matches(SearchField.SUBJECT, lesson.getSubject().toString())
                            && criteria.matches(SearchField.ROOM, lesson.getRoom().toString())
                            && (!fields.containsKey(SearchField.STUDENT_NAME) || data.getLessonRoster(lesson.getId())
                            .stream().anyMatch(student -> criteria.matches(SearchField.STUDENT_NAME,
                                    student.getName().fullName)))))
                    .map(tutor -> tutor.getId().toString()).toList();
            assertEquals(expected, search(fields).people().stream().map(person -> person.getId().toString()).toList(),
                    fields.toString());
        }
        assertSame(before, data.exportState());
    }

    private PersonSearchResult search(Map<SearchField, String> fields) {
        return PersonRelationshipSearch.search(data.getPeople(), queries,
                new SearchCriteria(SearchCategory.TUTOR, fields));
    }

    private static void assertIds(PersonSearchResult result, String... ids) {
        assertEquals(List.of(ids), result.people().stream().map(tutor -> tutor.getId().toString()).toList());
    }
}
