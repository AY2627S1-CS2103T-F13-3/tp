package seedu.address.model.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.RelationshipSearchTestData.createData;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import seedu.address.model.PeopleView;
import seedu.address.model.PonHubData;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.person.Parent;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Student;
import seedu.address.model.query.LessonQueries;

public class ParentRelationshipSearchTest {
    private final PonHubData data = createData();
    private final LessonQueries queries = new LessonQueries(data);

    @Test
    public void exactPhone_linksKeepZerosAndNeverInferRelationshipsFromNames() {
        assertIds(search(Map.of()), "P1", "P2", "P3", "P4");
        assertIds(search(Map.of(SearchField.NAME, "Pat")), "P1", "P2");
        assertIds(search(Map.of(SearchField.NAME, "Pat", SearchField.STUDENT_NAME, "Alex")), "P1");
        assertIds(search(Map.of(SearchField.PHONE, "0987654", SearchField.STUDENT_NAME, "Alex")));
        assertIds(search(Map.of(SearchField.PHONE, "00987654", SearchField.STUDENT_NAME, "Jamie")), "P1", "P4");
        assertIds(search(Map.of(SearchField.STUDENT_NAME, "Alex")), "P1", "P3", "P4");
        assertIds(search(Map.of(SearchField.STUDENT_NAME, "Alex", SearchField.SUBJECT, "Math")), "P1", "P4");
        assertIds(search(Map.of(SearchField.EMAIL, "@EXAMPLE", SearchField.DAY, "Mon")), "P1");
        assertIds(search(Map.of(SearchField.ADDRESS, "missing", SearchField.DAY, "Mon")));
        var children = queries.getStudentsByParentPhone(new Phone("00987654"));
        assertEquals(List.of(new PersonId("S1"), new PersonId("S2")), children.stream().map(Student::getId).toList());
        assertTrue(queries.getStudentsByParentPhone(new Phone("0987654")).isEmpty());
        assertThrows(UnsupportedOperationException.class, children::clear);
        assertThrows(NullPointerException.class, () -> queries.getStudentsByParentPhone(null));
    }

    @Test
    public void sameChildAndLesson_counterexamplesCannotMixSiblingsOrTimetables() {
        assertIds(search(Map.of(SearchField.STUDENT_NAME, "Jamie", SearchField.DAY, "Tue",
                SearchField.SUBJECT, "Science")));
        assertIds(search(Map.of(SearchField.STUDENT_NAME, "Alex", SearchField.DAY, "Wed",
                SearchField.SUBJECT, "中文")));
        assertIds(search(Map.of(SearchField.STUDENT_NAME, "Alex", SearchField.DAY, "Tue",
                SearchField.SUBJECT, "Math")));
        assertIds(search(Map.of(SearchField.DAY, "Mon", SearchField.SUBJECT, "Science")));
        var result = search(Map.of(SearchField.STUDENT_NAME, "Alex", SearchField.DAY, "Mon",
                SearchField.SUBJECT, "Math",
                SearchField.TUTOR_NAME, "MEI", SearchField.ROOM, "r1", SearchField.START_TIME, "0900",
                SearchField.END_TIME, "1000"));
        assertIds(result, "P1", "P4");
        var match = result.contexts().get(new PersonId("P1")).getFirst();
        assertEquals(new PersonId("S1"), match.student().orElseThrow().getId());
        assertEquals("L10", match.lesson().orElseThrow().getLessonId().toString());
        assertEquals(1, result.contexts().get(new PersonId("P1")).size());
    }

    @Test
    public void phoneIndexAndActiveProjection_refreshOnMembershipDeletionAndRestoreWithoutWriting() {
        var before = data.exportState();
        PeopleView view = new PeopleView(data, queries);
        var criteria = new SearchCriteria(SearchCategory.PARENT,
                Map.of(SearchField.STUDENT_NAME, "Alex", SearchField.SUBJECT, "Math"));
        view.setSearchCriteria(criteria);
        var people = view.getPeople();
        var context = view.searchContextProperty();
        assertEquals(2, people.size());
        data.unenrolStudent(new PersonId("S1"), new LessonId("L10"));
        view.refresh();
        assertTrue(people.isEmpty());
        assertTrue(context.get().isEmpty());
        data.resetData(before);
        view.refresh();
        assertEquals(2, people.size());
        var unassignedChildren = queries.getStudentsByParentPhone(new Phone("00888888"));
        data.removePerson(new PersonId("S3"));
        assertTrue(queries.getStudentsByParentPhone(new Phone("00888888")).isEmpty());
        assertEquals(1, unassignedChildren.size()); // Earlier snapshot remains immutable.
        data.resetData(before);
        assertEquals(1, queries.getStudentsByParentPhone(new Phone("00888888")).size());
        assertSame(people, view.getPeople());
        assertSame(context, view.searchContextProperty());
        assertSame(criteria, view.getSearchCriteria().orElseThrow());
    }

    @Test
    public void indexedParentQueries_matchIndependentRawSiblingAndLessonScan() {
        Random random = new Random(95);
        Map<SearchField, List<String>> values = Map.of(
                SearchField.NAME, List.of("Pat", "Other", "missing"),
                SearchField.PHONE, List.of("00987654", "0987654", "00888888"),
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
            SearchCriteria criteria = new SearchCriteria(SearchCategory.PARENT, fields);
            boolean related = fields.keySet().stream()
                    .anyMatch(field -> !PersonContactSearch.OWN_FIELDS.contains(field));
            boolean lessonFields = fields.keySet().stream()
                    .anyMatch(field -> !PersonContactSearch.OWN_FIELDS.contains(field)
                    && field != SearchField.STUDENT_NAME);
            var expected = data.getPeople().stream().filter(Parent.class::isInstance).map(Parent.class::cast)
                    .filter(parent -> criteria.matches(SearchField.NAME, parent.getName().fullName)
                            && criteria.matches(SearchField.PHONE,
                                    parent.getContactDetails().getPhone().orElseThrow().value))
                    .filter(parent -> !related || data.getPeople().stream().filter(Student.class::isInstance)
                            .map(Student.class::cast).anyMatch(student -> student.getParentPhone().value
                            .equals(parent.getContactDetails().getPhone().orElseThrow().value)
                            && criteria.matches(SearchField.STUDENT_NAME, student.getName().fullName)
                            && (!lessonFields || data.getLessons().stream().anyMatch(lesson ->
                            lesson.hasEnrolledStudent(student.getId())
                            && criteria.matches(SearchField.DAY, lesson.getTimeSlot().getDay().toString())
                            && criteria.matches(SearchField.SUBJECT, lesson.getSubject().toString())
                            && criteria.matches(SearchField.ROOM, lesson.getRoom().toString())))))
                    .map(parent -> parent.getId().toString()).toList();
            assertEquals(expected, search(fields).people().stream().map(person -> person.getId().toString()).toList(),
                    fields.toString());
        }
        assertSame(before, data.exportState());
    }

    private PersonSearchResult search(Map<SearchField, String> fields) {
        return PersonRelationshipSearch.search(data.getPeople(), queries,
                new SearchCriteria(SearchCategory.PARENT, fields));
    }

    private static void assertIds(PersonSearchResult result, String... ids) {
        assertEquals(List.of(ids), result.people().stream().map(parent -> parent.getId().toString()).toList());
    }
}
