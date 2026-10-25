package ponhub.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.LessonQueryTestData.createData;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.collections.ObservableList;
import ponhub.commons.core.index.Index;
import ponhub.logic.LessonRetrieval.ResultKind;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.SearchCriteriaParser;
import ponhub.logic.parser.ShowLessonInput;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.PonHubDataState;
import ponhub.model.attendance.Attendance;
import ponhub.model.attendance.AttendanceStatus;
import ponhub.model.lesson.LessonId;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRole;
import ponhub.model.query.LessonDetailView;
import ponhub.model.query.LessonQueries;
import ponhub.model.query.LessonView;
import ponhub.model.search.SearchCategory;
import ponhub.model.search.SearchCriteria;
import ponhub.model.search.SearchField;

public class LessonRetrievalTest {
    private PonHubData data;
    private PeopleView peopleView;
    private LessonQueries queries;
    private LessonRetrieval retrieval;

    @BeforeEach
    public void setUp() {
        data = createData();
        peopleView = new PeopleView(data);
        queries = new LessonQueries(data);
        retrieval = new LessonRetrieval(queries, peopleView, data);
    }

    @Test
    public void list_fullAndCurrentViewStudentResultsUseStableIdentity() throws CommandException {
        PonHubDataState before = data.exportState();
        assertIds(retrieval.list(Optional.empty()), "L2", "L10", "L3", "L4", "L11");
        assertIds(retrieval.list(Optional.of(Index.fromOneBased(2))), "L2", "L10");
        assertIds(retrieval.list(Optional.of(Index.fromOneBased(4))), "L10", "L11");
        assertTrue(retrieval.list(Optional.of(Index.fromOneBased(6))).isEmpty());
        peopleView.setRoleFilter(Optional.of(PersonRole.STUDENT));
        assertIds(retrieval.list(Optional.of(Index.fromOneBased(1))), "L2", "L10");
        assertIds(retrieval.list(Optional.of(Index.fromOneBased(2))), "L10", "L11");
        assertEquals(Optional.of(PersonRole.STUDENT), peopleView.getRoleFilter());
        assertSame(before, data.exportState());
    }

    @Test
    public void list_rejectsWrongRoleAndOutOfRangeWithoutChangingViews() throws CommandException {
        ObservableList<LessonView> catalogue = retrieval.list(Optional.empty());
        var people = peopleView.getPeople();
        assertThrows(CommandException.class, () -> retrieval.list(Optional.of(Index.fromOneBased(1))));
        assertThrows(CommandException.class, () -> retrieval.list(Optional.of(Index.fromOneBased(3))));
        assertThrows(CommandException.class, () -> retrieval.list(Optional.of(Index.fromOneBased(8))));
        assertSame(people, peopleView.getPeople());
        assertSame(catalogue, retrieval.list(Optional.empty()));
        assertIds(catalogue, "L2", "L10", "L3", "L4", "L11");
    }

    @Test
    public void search_filtersSameLessonAndPreservesCurrentPeopleView() throws Exception {
        peopleView.setRoleFilter(Optional.of(PersonRole.STUDENT));
        var people = peopleView.getPeople();
        var before = List.copyOf(people);
        PonHubDataState state = data.exportState();
        SearchCriteria criteria = new SearchCriteriaParser().parse("c/lesson d/Mon s/Math sn/Alex");
        ObservableList<LessonView> filtered = retrieval.search(criteria);
        assertSame(queries.getFilteredLessonList(), filtered);
        assertIds(filtered, "L10");
        assertEquals(before, people);
        assertSame(people, peopleView.getPeople());
        assertIds(retrieval.list(Optional.of(Index.fromOneBased(2))), "L10", "L11");
        assertSame(state, data.exportState());
        assertThrows(UnsupportedOperationException.class, () -> filtered.remove(0));

        assertSame(filtered, retrieval.search(new SearchCriteria(SearchCategory.LESSON,
                Map.of(SearchField.STUDENT_NAME, "Nobody"))));
        assertTrue(filtered.isEmpty());
        assertIds(retrieval.search(new SearchCriteria(SearchCategory.LESSON, Map.of())),
                "L2", "L10", "L3", "L4", "L11");
    }

    @Test
    public void heldResults_refreshAfterPublicationAndRollback() throws CommandException {
        PonHubDataState before = data.exportState();
        var catalogue = retrieval.list(Optional.empty());
        var student = retrieval.list(Optional.of(Index.fromOneBased(2)));
        var filtered = retrieval.search(new SearchCriteria(SearchCategory.LESSON,
                Map.of(SearchField.SUBJECT, "Math")));
        var people = peopleView.getPeople();
        data.resetData(new PonHubDataState(before.people(), List.of(), List.of(),
                before.lastAllocatedLessonSequence()));
        assertTrue(catalogue.isEmpty());
        assertTrue(student.isEmpty());
        assertTrue(filtered.isEmpty());
        data.resetData(before);
        assertIds(catalogue, "L2", "L10", "L3", "L4", "L11");
        assertIds(student, "L2", "L10");
        assertIds(filtered, "L10", "L4");
        assertSame(people, peopleView.getPeople());
    }

    @Test
    public void invalidArguments_leaveExistingResultsIntact() throws CommandException {
        assertThrows(NullPointerException.class, () -> new LessonRetrieval(null, peopleView, data));
        assertThrows(NullPointerException.class, () -> new LessonRetrieval(queries, null, data));
        assertThrows(NullPointerException.class, () -> new LessonRetrieval(queries, peopleView, null));
        assertThrows(NullPointerException.class, () -> retrieval.list(null));
        assertThrows(NullPointerException.class, () -> retrieval.search(null));
        SearchCriteria valid = new SearchCriteria(SearchCategory.LESSON, Map.of(SearchField.DAY, "Mon"));
        var filtered = retrieval.search(valid);
        assertThrows(IllegalArgumentException.class, () -> retrieval.search(
                new SearchCriteria(SearchCategory.STUDENT, Map.of())));
        assertSame(valid, queries.getLessonFilter());
        assertIds(filtered, "L10", "L3", "L4");
    }

    private static void assertIds(List<LessonView> lessons, String... expected) {
        assertEquals(List.of(expected), lessons.stream().map(lesson -> lesson.getLessonId().toString()).toList());
    }

    @Test
    public void resultKind_identifiesSuccessfulSourcesIncludingEmptyAndRepeatedSelections() throws Exception {
        var kind = retrieval.resultKindProperty();
        assertSame(kind, retrieval.resultKindProperty());
        assertEquals(ResultKind.ALL_LESSONS, kind.get());
        retrieval.list(Optional.empty());
        retrieval.list(Optional.empty());
        assertEquals(ResultKind.ALL_LESSONS, kind.get());

        retrieval.list(Optional.of(Index.fromOneBased(6)));
        assertTrue(retrieval.getDisplayedLessons().isEmpty());
        assertEquals(ResultKind.STUDENT_LESSONS, kind.get());
        retrieval.list(Optional.of(Index.fromOneBased(2)));
        retrieval.list(Optional.of(Index.fromOneBased(2)));
        assertEquals(ResultKind.STUDENT_LESSONS, kind.get());

        SearchCriteria emptySearch = new SearchCriteriaParser().parse("c/lesson s/Missing");
        retrieval.search(emptySearch);
        retrieval.search(emptySearch);
        assertTrue(retrieval.getDisplayedLessons().isEmpty());
        assertEquals(ResultKind.SEARCH, kind.get());
        retrieval.search(new SearchCriteriaParser().parse("c/lesson"));
        assertEquals(ResultKind.SEARCH, kind.get());

        ShowLessonInput detail = new ShowLessonInput(new LessonId("L3"), Optional.empty());
        retrieval.show(detail);
        retrieval.show(detail);
        assertEquals(ResultKind.DETAIL, kind.get());
        retrieval.show(new ShowLessonInput(new LessonId("L10"), Optional.of(LocalDate.of(2026, 10, 5))));
        assertEquals(ResultKind.DETAIL, kind.get());
        retrieval.list(Optional.empty());
        assertEquals(ResultKind.ALL_LESSONS, kind.get());
    }

    @Test
    public void resultKind_invalidSelectorsFiltersAndDatesPreservePreviousSource() throws Exception {
        retrieval.list(Optional.of(Index.fromOneBased(4)));
        var displayed = List.copyOf(retrieval.getDisplayedLessons());
        String context = retrieval.contextProperty().get();
        for (int index : List.of(1, 3, 8)) {
            assertThrows(CommandException.class, () -> retrieval.list(Optional.of(Index.fromOneBased(index))));
            assertEquals(ResultKind.STUDENT_LESSONS, retrieval.resultKindProperty().get());
        }
        assertThrows(NullPointerException.class, () -> retrieval.list(null));
        assertThrows(NullPointerException.class, () -> retrieval.search(null));
        assertThrows(IllegalArgumentException.class, () -> retrieval.search(
                new SearchCriteria(SearchCategory.STUDENT, Map.of())));
        assertThrows(NullPointerException.class, () -> retrieval.show(null));
        assertThrows(CommandException.class, () -> retrieval.show(new ShowLessonInput(new LessonId("L99"),
                Optional.empty())));
        assertThrows(CommandException.class, () -> retrieval.show(new ShowLessonInput(new LessonId("L10"),
                Optional.of(LocalDate.of(2026, 10, 6)))));
        peopleView.setRoleFilter(Optional.of(PersonRole.TUTOR));
        assertEquals(ResultKind.STUDENT_LESSONS, retrieval.resultKindProperty().get());
        assertEquals(context, retrieval.contextProperty().get());
        assertEquals(displayed, retrieval.getDisplayedLessons());
    }

    @Test
    public void context_identifiesSelectedStudentOrFiltersAndInvalidRequestsPreserveIt() throws Exception {
        var context = retrieval.contextProperty();
        assertEquals("All lessons", context.get());
        retrieval.list(Optional.of(Index.fromOneBased(4)));
        assertEquals("Student S2 · current lessons", context.get());
        peopleView.setRoleFilter(Optional.of(PersonRole.TUTOR));
        assertThrows(CommandException.class, () -> retrieval.list(Optional.of(Index.fromOneBased(1))));
        assertEquals("Student S2 · current lessons", context.get());
        retrieval.search(new SearchCriteriaParser().parse("c/lesson tu/MEI d/mon s/Math"));
        assertEquals("Lesson search: d/mon, s/math, tu/mei", context.get());
        assertThrows(IllegalArgumentException.class, () -> retrieval.search(
                new SearchCriteria(SearchCategory.STUDENT, Map.of())));
        assertEquals("Lesson search: d/mon, s/math, tu/mei", context.get());
        retrieval.search(new SearchCriteriaParser().parse("c/lesson"));
        assertEquals("Lesson search: all lessons", context.get());
        retrieval.list(Optional.empty());
        assertEquals("All lessons", context.get());
        assertSame(context, retrieval.contextProperty());
    }

    @Test
    public void selectedResults_keepIdentityAndFollowOnlySelectedSourceAcrossSwitches() throws Exception {
        var displayed = retrieval.getDisplayedLessons();
        assertIds(displayed, "L2", "L10", "L3", "L4", "L11");
        assertThrows(UnsupportedOperationException.class, displayed::clear);
        SearchCriteria math = new SearchCriteriaParser().parse("c/lesson s/Math");
        retrieval.search(math);
        assertIds(displayed, "L10", "L4");
        retrieval.search(math); // Reusing the source must not duplicate its subscription.
        retrieval.list(Optional.of(Index.fromOneBased(4)));
        queries.setLessonFilter(new SearchCriteriaParser().parse("c/lesson s/Science"));
        assertIds(displayed, "L10", "L11"); // The old search source is detached.
        retrieval.list(Optional.empty());
        queries.setLessonFilter(math);
        assertIds(displayed, "L2", "L10", "L3", "L4", "L11");
        assertEquals(ResultKind.ALL_LESSONS, retrieval.resultKindProperty().get());
        assertSame(displayed, retrieval.getDisplayedLessons());
    }

    @Test
    public void selectedStudentAndSearch_refreshMembershipAttendanceDeletionAndRestore() throws Exception {
        PonHubDataState original = data.exportState();
        var displayed = retrieval.getDisplayedLessons();
        retrieval.list(Optional.of(Index.fromOneBased(2)));
        peopleView.setRoleFilter(Optional.of(PersonRole.TUTOR));
        PersonId student = new PersonId("S1");
        var withoutStudent = original.lessons().stream().map(lesson -> lesson.withoutEnrolledStudent(student)).toList();
        Attendance historical = Attendance.fromStrings(student, new LessonId("L10"), "2026-10-05", "present");
        data.resetData(new PonHubDataState(original.people(), withoutStudent, List.of(historical), 11));
        assertTrue(displayed.isEmpty()); // Former attendance must never become current membership.
        assertEquals(ResultKind.STUDENT_LESSONS, retrieval.resultKindProperty().get());
        retrieval.search(new SearchCriteriaParser().parse("c/lesson sn/Alex"));
        assertTrue(displayed.isEmpty());
        var enrolled = withoutStudent.stream().map(lesson -> lesson.getId().equals(new LessonId("L3"))
                ? lesson.withEnrolledStudent(student) : lesson).toList();
        data.resetData(new PonHubDataState(original.people(), enrolled, List.of(historical), 11));
        assertIds(displayed, "L3");
        assertEquals(ResultKind.SEARCH, retrieval.resultKindProperty().get());
        assertEquals(1, displayed.getFirst().getRosterSize());
        data.resetData(new PonHubDataState(original.people(), enrolled, List.of(), 11)); // Unmark.
        assertIds(displayed, "L3");
        retrieval.list(Optional.empty());
        data.resetData(new PonHubDataState(original.people(), List.of(), List.of(), 11)); // Catalogue removal.
        assertTrue(displayed.isEmpty());
        data.resetData(original); // Restore the complete prior state.
        assertIds(displayed, "L2", "L10", "L3", "L4", "L11");
        assertEquals(ResultKind.ALL_LESSONS, retrieval.resultKindProperty().get());
        assertSame(displayed, retrieval.getDisplayedLessons());
    }

    @Test
    public void show_dateUsesCurrentRosterAndSeparatesRetainedFormerAttendance() throws Exception {
        PonHubDataState original = data.exportState();
        LocalDate monday = LocalDate.of(2026, 10, 5);
        PersonId studentOne = new PersonId("S1");
        PersonId studentTwo = new PersonId("S2");
        LessonId lessonId = new LessonId("L10");
        Attendance present = new Attendance(studentOne, lessonId, monday, AttendanceStatus.PRESENT);
        Attendance absent = new Attendance(studentTwo, lessonId, monday, AttendanceStatus.ABSENT);
        Attendance nextWeek = Attendance.fromStrings(studentTwo, lessonId, "2026-10-12", "present");
        data.resetData(new PonHubDataState(original.people(), original.lessons(),
                List.of(present, absent, nextWeek), 11));
        peopleView.setRoleFilter(Optional.of(PersonRole.STUDENT));
        var people = peopleView.getPeople();
        var beforePeople = List.copyOf(people);
        var displayed = retrieval.getDisplayedLessons();

        LessonDetailView result = retrieval.show(new ShowLessonInput(lessonId, Optional.of(monday)));
        assertSame(result, displayed.getFirst());
        assertEquals(List.of(studentOne, studentTwo), result.currentRoster().stream()
                .map(entry -> entry.student().getId()).toList());
        assertEquals(List.of(Optional.of(AttendanceStatus.PRESENT), Optional.of(AttendanceStatus.ABSENT)),
                result.currentRoster().stream().map(LessonDetailView.RosterStatus::status).toList());
        assertTrue(result.formerAttendance().isEmpty());
        assertEquals(beforePeople, people);
        assertSame(people, peopleView.getPeople());
        assertThrows(UnsupportedOperationException.class, displayed::clear);
        assertThrows(UnsupportedOperationException.class, result.currentRoster()::clear);

        var withoutStudentTwo = original.lessons().stream().map(lesson -> lesson.getId().equals(lessonId)
                ? lesson.withoutEnrolledStudent(studentTwo) : lesson).toList();
        data.resetData(new PonHubDataState(original.people(), withoutStudentTwo,
                List.of(present, absent, nextWeek), 11));
        LessonDetailView refreshed = (LessonDetailView) displayed.getFirst();
        assertEquals(List.of(studentOne), refreshed.currentRoster().stream()
                .map(entry -> entry.student().getId()).toList());
        assertEquals(List.of(studentTwo), refreshed.formerAttendance().stream()
                .map(entry -> entry.student().getId()).toList());
        assertEquals(AttendanceStatus.ABSENT, refreshed.formerAttendance().getFirst().status());

        data.resetData(new PonHubDataState(original.people(), withoutStudentTwo, List.of(absent), 11));
        assertEquals(Optional.empty(), ((LessonDetailView) displayed.getFirst()).currentRoster().getFirst().status());
        data.resetData(new PonHubDataState(original.people(), withoutStudentTwo, List.of(), 11));
        assertTrue(((LessonDetailView) displayed.getFirst()).formerAttendance().isEmpty());
        assertEquals(beforePeople, people);
    }

    @Test
    public void show_withoutDateShowsEmptyRosterAndAllFormerDatesAndValidatesBeforeSwitch() throws Exception {
        PonHubDataState original = data.exportState();
        PersonId student = new PersonId("S2");
        LessonId lessonId = new LessonId("L10");
        var withoutStudent = original.lessons().stream().map(lesson -> lesson.getId().equals(lessonId)
                ? lesson.withoutEnrolledStudent(student) : lesson).toList();
        Attendance older = Attendance.fromStrings(student, lessonId, "2026-10-05", "absent");
        Attendance newer = Attendance.fromStrings(student, lessonId, "2026-10-12", "present");
        Attendance sameDay = Attendance.fromStrings(new PersonId("S3"), lessonId, "2026-10-12", "absent");
        data.resetData(new PonHubDataState(original.people(), withoutStudent, List.of(older, newer, sameDay), 11));
        retrieval.show(new ShowLessonInput(lessonId, Optional.empty()));
        LessonDetailView detail = (LessonDetailView) retrieval.getDisplayedLessons().getFirst();
        assertEquals(List.of(newer.getDate(), sameDay.getDate(), older.getDate()), detail.formerAttendance().stream()
                .map(LessonDetailView.FormerAttendance::date).toList());
        assertEquals(List.of(new PersonId("S2"), new PersonId("S3"), new PersonId("S2")),
                detail.formerAttendance().stream().map(entry -> entry.student().getId()).toList());
        assertEquals(Optional.empty(), detail.currentRoster().getFirst().status());

        var displayed = retrieval.getDisplayedLessons();
        assertThrows(CommandException.class, () -> retrieval.show(new ShowLessonInput(new LessonId("L99"),
                Optional.empty())));
        assertThrows(CommandException.class, () -> retrieval.show(new ShowLessonInput(lessonId,
                Optional.of(LocalDate.of(2026, 10, 6)))));
        assertThrows(NullPointerException.class, () -> retrieval.show(null));
        assertSame(detail, displayed.getFirst());

        retrieval.show(new ShowLessonInput(new LessonId("L3"), Optional.empty()));
        assertTrue(((LessonDetailView) displayed.getFirst()).currentRoster().isEmpty());
        var withoutEmptyLesson = original.lessons().stream()
                .filter(lesson -> !lesson.getId().equals(new LessonId("L3"))).toList();
        data.resetData(new PonHubDataState(original.people(), withoutEmptyLesson, List.of(older, newer), 11));
        assertTrue(displayed.isEmpty());
        assertEquals(ResultKind.DETAIL, retrieval.resultKindProperty().get());
        assertTrue(retrieval.contextProperty().get().contains("no longer available"));
        retrieval.list(Optional.empty());
        assertIds(displayed, "L2", "L10", "L4", "L11");
    }
}
