package seedu.address.model.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.LessonQueryTestData.SHARED_LESSON;
import static seedu.address.testutil.LessonQueryTestData.STUDENT_TWO;
import static seedu.address.testutil.LessonQueryTestData.createData;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.logic.parser.SearchCriteriaParser;
import seedu.address.model.PeopleView;
import seedu.address.model.PonHubDataState;
import seedu.address.model.lesson.Lesson;
import seedu.address.model.lesson.LessonDay;
import seedu.address.model.lesson.LessonTime;
import seedu.address.model.lesson.LessonTimeSlot;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.PeopleRegistryState;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Student;
import seedu.address.model.query.LessonQueries;

/** Verifies exact search keys against replacement records and schedules without adding editing routes. */
public class SearchValueRefreshTest {
    private final SearchCriteriaParser parser = new SearchCriteriaParser();

    @Test
    public void refresh_changedPhoneAndLevel_rechecksExactValuesAndKeepsStablePersonIdentity() throws Exception {
        var data = createData();
        var before = data.exportState();
        PeopleView view = new PeopleView(data);
        var criteria = parser.parse("c/student l/s2 p/00012345 pp/00987654");
        view.setSearchCriteria(criteria);
        var people = view.getPeople();
        assertEquals(List.of(STUDENT_TWO), people.stream().map(person -> person.getId()).toList());
        Student oldStudent = (Student) people.getFirst();
        ContactDetails oldContact = oldStudent.getContactDetails();
        Student changed = new Student(STUDENT_TWO, new ContactDetails(oldContact.getName(),
                Optional.of(new Phone("00098765")), oldContact.getEmail(), oldContact.getAddress()),
                new EducationLevel("jc2"), new Phone("00045678"));
        var replacements = before.people().getPeople().stream()
                .map(person -> person.getId().equals(STUDENT_TWO) ? changed : person).toList();
        data.resetData(new PonHubDataState(new PeopleRegistryState(replacements,
                before.people().getLastAllocatedSequences()), before.lessons(), before.attendance(),
                before.lastAllocatedLessonSequence()));
        view.refresh();
        assertTrue(people.isEmpty());
        assertSame(criteria, view.getSearchCriteria().orElseThrow());
        view.setSearchCriteria(parser.parse("c/student l/JC2 p/00098765 pp/00045678"));
        assertEquals(List.of(changed), people);
        assertEquals(Optional.of(PersonRole.STUDENT), view.getRoleFilter());
        assertEquals("S2", oldStudent.getLevel().getValue());
        data.resetData(before);
        view.refresh();
        assertTrue(people.isEmpty());
        view.setSearchCriteria(criteria);
        assertSame(people, view.getPeople());
        assertEquals(List.of(oldStudent), people);
    }

    @Test
    public void refresh_changedDayAndTimes_rebuildsExactBucketsAndRelationshipContext() throws Exception {
        var data = createData();
        var before = data.exportState();
        LessonQueries queries = new LessonQueries(data);
        PeopleView view = new PeopleView(data, queries);
        var criteria = parser.parse("c/student d/mON st/0900 et/1000 s/ath");
        view.setSearchCriteria(criteria);
        var people = view.getPeople();
        var contexts = view.searchContextProperty();
        var oldContext = contexts.get().get(STUDENT_TWO).getFirst().lesson().orElseThrow();
        queries.setLessonFilter(parser.parse("c/lesson d/MON st/0900 et/1000 s/ath"));
        var lessons = queries.getFilteredLessonList();
        Lesson oldLesson = data.getLesson(SHARED_LESSON).orElseThrow();
        Lesson changed = new Lesson(SHARED_LESSON, oldLesson.getTutorId(), new LessonTimeSlot(LessonDay.SUNDAY,
                new LessonTime("0000"), new LessonTime("0001")), oldLesson.getSubject(), oldLesson.getRoom(),
                oldLesson.getEnrolledStudentIds());
        var replacements = before.lessons().stream()
                .map(lesson -> lesson.getId().equals(SHARED_LESSON) ? changed : lesson).toList();
        data.resetData(new PonHubDataState(before.people(), replacements, before.attendance(),
                before.lastAllocatedLessonSequence()));
        view.refresh();
        assertTrue(people.isEmpty());
        assertTrue(contexts.get().isEmpty());
        assertTrue(lessons.isEmpty());
        assertSame(criteria, view.getSearchCriteria().orElseThrow());
        queries.setLessonFilter(parser.parse("c/lesson d/sUN st/0000 et/0001 s/ath"));
        view.setSearchCriteria(parser.parse("c/student d/sUN st/0000 et/0001 s/ath"));
        assertEquals(List.of(new PersonId("S1"), STUDENT_TWO),
                people.stream().map(person -> person.getId()).toList());
        assertEquals(List.of(SHARED_LESSON), lessons.stream().map(lesson -> lesson.getLessonId()).toList());
        assertEquals("Sun 0000-0001", contexts.get().get(STUDENT_TWO).getFirst().lesson().orElseThrow()
                .getTimeSlot().toString());
        assertEquals("Mon 0900-1000", oldContext.getTimeSlot().toString());
        data.resetData(before);
        view.refresh();
        assertTrue(people.isEmpty());
        assertTrue(lessons.isEmpty());
        view.setSearchCriteria(criteria);
        assertEquals(List.of(new PersonId("S1"), STUDENT_TWO),
                people.stream().map(person -> person.getId()).toList());
        assertSame(people, view.getPeople());
        assertSame(contexts, view.searchContextProperty());
        assertSame(lessons, queries.getFilteredLessonList());
    }
}
