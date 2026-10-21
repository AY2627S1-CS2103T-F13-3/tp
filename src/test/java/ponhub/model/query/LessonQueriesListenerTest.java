package ponhub.model.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.LessonQueryTestData.EMPTY_LESSON;
import static ponhub.testutil.LessonQueryTestData.SHARED_LESSON;
import static ponhub.testutil.LessonQueryTestData.STUDENT_ONE;
import static ponhub.testutil.LessonQueryTestData.STUDENT_TWO;
import static ponhub.testutil.LessonQueryTestData.STUDENT_WITHOUT_LESSONS;
import static ponhub.testutil.LessonQueryTestData.TUTOR_ONE;
import static ponhub.testutil.LessonQueryTestData.TUTOR_TWO;
import static ponhub.testutil.LessonQueryTestData.TUTOR_WITHOUT_LESSONS;
import static ponhub.testutil.LessonQueryTestData.createData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import ponhub.logic.parser.SearchCriteriaParser;
import ponhub.model.PonHubData;
import ponhub.model.PonHubDataState;
import ponhub.model.lesson.Lesson;
import ponhub.model.lesson.LessonId;
import ponhub.model.person.PersonId;

/** Regressions for read-only callbacks that subscribe to more query results during source publication. */
public class LessonQueriesListenerTest {
    private PonHubData data;
    private LessonQueries queries;
    private PonHubDataState original;
    private ObservableList<StudentView> heldRoster;
    private ObservableList<LessonView> heldFiltered;

    @BeforeEach
    public void setUp() throws Exception {
        data = createData();
        original = data.exportState();
        queries = new LessonQueries(data);
        heldRoster = queries.getLessonRoster(SHARED_LESSON);
        queries.setLessonFilter(new SearchCriteriaParser().parse("c/lesson sn/Alex"));
        heldFiltered = queries.getFilteredLessonList();
    }

    @Test
    public void studentListener_newSubscriptionsDoNotInterruptUnenrolmentAndRestore() {
        var heldStudent = queries.getStudentLessons(STUDENT_ONE);
        AtomicReference<ObservableList<LessonView>> newStudent = new AtomicReference<>();
        AtomicReference<ObservableList<LessonView>> unassigned = new AtomicReference<>();
        heldStudent.addListener((ListChangeListener<LessonView>) change -> {
            newStudent.set(queries.getStudentLessons(STUDENT_TWO));
            unassigned.set(queries.getStudentLessons(STUDENT_WITHOUT_LESSONS));
        });

        publishWithoutListenerErrors(withRoster(original, SHARED_LESSON, Set.of(STUDENT_TWO)));

        assertUnenrolledResults();
        assertEquals(List.of(new LessonId("L2")), lessonIds(heldStudent));
        var subscribedStudent = newStudent.get();
        assertEquals(List.of(SHARED_LESSON, new LessonId("L11")), lessonIds(subscribedStudent));
        assertEquals(1, subscribedStudent.get(0).getRosterSize());
        assertSame(subscribedStudent, queries.getStudentLessons(STUDENT_TWO));
        assertTrue(unassigned.get().isEmpty());

        publishWithoutListenerErrors(original);

        assertSame(subscribedStudent, newStudent.get());
        assertEquals(2, subscribedStudent.get(0).getRosterSize());
        assertSame(heldStudent, queries.getStudentLessons(STUDENT_ONE));
        assertEquals(List.of(new LessonId("L2"), SHARED_LESSON), lessonIds(heldStudent));
        assertEquals(2, heldRoster.size());
        assertEquals(List.of(new LessonId("L2"), SHARED_LESSON), lessonIds(heldFiltered));
    }

    @Test
    public void tutorListener_newSubscriptionsDoNotInterruptLaterRosterAndFilterRefresh() {
        var heldTutor = queries.getTutorSchedule(TUTOR_ONE);
        AtomicReference<ObservableList<LessonView>> newTutor = new AtomicReference<>();
        AtomicReference<ObservableList<LessonView>> unassigned = new AtomicReference<>();
        heldTutor.addListener((ListChangeListener<LessonView>) change -> {
            newTutor.set(queries.getTutorSchedule(TUTOR_TWO));
            unassigned.set(queries.getTutorSchedule(TUTOR_WITHOUT_LESSONS));
        });

        publishWithoutListenerErrors(withRoster(original, SHARED_LESSON, Set.of(STUDENT_TWO)));

        assertUnenrolledResults();
        assertEquals(1, heldTutor.get(0).getRosterSize());
        var subscribedTutor = newTutor.get();
        assertEquals(List.of(EMPTY_LESSON, new LessonId("L11")), lessonIds(subscribedTutor));
        assertSame(subscribedTutor, queries.getTutorSchedule(TUTOR_TWO));
        assertTrue(unassigned.get().isEmpty());

        publishWithoutListenerErrors(withRoster(original, new LessonId("L11"), Set.of()));

        assertSame(subscribedTutor, newTutor.get());
        assertEquals(0, subscribedTutor.get(1).getRosterSize());
        assertSame(heldTutor, queries.getTutorSchedule(TUTOR_ONE));
        assertEquals(2, heldRoster.size());
        assertEquals(List.of(new LessonId("L2"), SHARED_LESSON), lessonIds(heldFiltered));
    }

    @Test
    public void rosterListener_newSubscriptionsStartCurrentAndKeepRefreshing() {
        AtomicReference<ObservableList<StudentView>> newRoster = new AtomicReference<>();
        AtomicReference<ObservableList<StudentView>> emptyRoster = new AtomicReference<>();
        heldRoster.addListener((ListChangeListener<StudentView>) change -> {
            newRoster.set(queries.getLessonRoster(new LessonId("L2")));
            emptyRoster.set(queries.getLessonRoster(EMPTY_LESSON));
        });

        publishWithoutListenerErrors(withRoster(original, SHARED_LESSON, Set.of(STUDENT_TWO)));

        assertUnenrolledResults();
        var subscribedRoster = newRoster.get();
        assertEquals(List.of(STUDENT_ONE), subscribedRoster.stream().map(StudentView::getStudentId).toList());
        assertSame(subscribedRoster, queries.getLessonRoster(new LessonId("L2")));
        assertTrue(emptyRoster.get().isEmpty());

        publishWithoutListenerErrors(withRoster(original, new LessonId("L2"), Set.of()));

        assertSame(subscribedRoster, newRoster.get());
        assertTrue(subscribedRoster.isEmpty());
        assertSame(heldRoster, queries.getLessonRoster(SHARED_LESSON));
        assertEquals(2, heldRoster.size());
        assertEquals(List.of(SHARED_LESSON), lessonIds(heldFiltered));
    }

    private void assertUnenrolledResults() {
        assertEquals(Set.of(STUDENT_TWO), data.getLesson(SHARED_LESSON).orElseThrow().getEnrolledStudentIds());
        assertEquals(1, queries.findLessonById(SHARED_LESSON).orElseThrow().getRosterSize());
        assertEquals(List.of(STUDENT_TWO), heldRoster.stream().map(StudentView::getStudentId).toList());
        assertSame(heldRoster, queries.getLessonRoster(SHARED_LESSON));
        assertSame(heldFiltered, queries.getFilteredLessonList());
        assertEquals(List.of(new LessonId("L2")), lessonIds(heldFiltered));
    }

    /** JavaFX forwards callback errors to this handler instead of throwing them from resetData. */
    private void publishWithoutListenerErrors(PonHubDataState replacement) {
        Thread thread = Thread.currentThread();
        Thread.UncaughtExceptionHandler previous = thread.getUncaughtExceptionHandler();
        List<Throwable> failures = new ArrayList<>();
        thread.setUncaughtExceptionHandler((ignored, failure) -> failures.add(failure));
        try {
            data.resetData(replacement);
        } finally {
            thread.setUncaughtExceptionHandler(previous);
        }
        assertEquals(List.of(), failures);
    }

    private static PonHubDataState withRoster(PonHubDataState state, LessonId lessonId, Set<PersonId> students) {
        List<Lesson> lessons = state.lessons().stream().map(lesson -> lesson.getId().equals(lessonId)
                ? lesson.withEnrolledStudentIds(students) : lesson).toList();
        return new PonHubDataState(state.people(), lessons, state.attendance(), state.lastAllocatedLessonSequence());
    }

    private static List<LessonId> lessonIds(List<LessonView> lessons) {
        return lessons.stream().map(LessonView::getLessonId).toList();
    }
}
