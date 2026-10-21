package ponhub.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ponhub.model.lesson.Lesson;
import ponhub.model.lesson.LessonDay;
import ponhub.model.lesson.LessonId;
import ponhub.model.lesson.LessonTime;
import ponhub.model.lesson.LessonTimeSlot;
import ponhub.model.lesson.Room;
import ponhub.model.lesson.Subject;
import ponhub.model.person.ContactDetails;
import ponhub.model.person.Name;
import ponhub.model.person.PeopleRegistry;
import ponhub.model.person.PeopleRegistryState;
import ponhub.model.person.Phone;
import ponhub.model.person.Tutor;

public class LessonAllocationBoundaryTest {
    private PeopleRegistryState people;
    private Tutor tutor;

    @BeforeEach
    public void setUp() {
        PeopleRegistry registry = new PeopleRegistry();
        tutor = registry.addTutor(new ContactDetails(new Name("Tutor"), Optional.of(new Phone("90000001")),
                Optional.empty(), Optional.empty()));
        people = registry.exportState();
    }

    @Test
    public void addLesson_penultimateCounter_allocatesFinalIdThenRejectsWithoutPublishing() {
        PonHubData data = new PonHubData(new PonHubDataState(people, List.of(), List.of(), Long.MAX_VALUE - 1));
        AtomicInteger publications = new AtomicInteger();
        data.stateProperty().addListener((observable, oldState, newState) -> publications.incrementAndGet());

        Lesson finalLesson = addLesson(data, LessonDay.MONDAY);

        assertEquals(LessonId.fromSequenceNumber(Long.MAX_VALUE), finalLesson.getId());
        assertEquals(List.of(finalLesson), data.getLessons());
        assertEquals(Long.MAX_VALUE, data.exportState().lastAllocatedLessonSequence());
        PonHubDataState exhausted = data.exportState();
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                addLesson(data, LessonDay.TUESDAY));
        assertEquals(LessonId.MESSAGE_SEQUENCE_EXHAUSTED, exception.getMessage());
        assertSame(exhausted, data.exportState());
        assertEquals(1, publications.get());

        PonHubData restored = new PonHubData(exhausted);
        assertEquals(exhausted, restored.exportState());
        assertEquals(Optional.of(finalLesson), restored.getLesson(finalLesson.getId()));
        assertThrows(IllegalStateException.class, () -> addLesson(restored, LessonDay.TUESDAY));
        assertSame(exhausted, restored.exportState());
    }

    @Test
    public void restore_removedLatestLesson_allocatesAboveHistoryInsteadOfRetainedIds() {
        Lesson retained = retainedLesson();
        PonHubDataState removedLatest = new PonHubDataState(people, List.of(retained), List.of(), 7);
        PonHubData data = new PonHubData(removedLatest);
        PonHubData restored = new PonHubData(data.exportState());

        Lesson next = addLesson(restored, LessonDay.TUESDAY);

        assertEquals(new LessonId("L8"), next.getId());
        assertEquals(List.of(retained, next), restored.getLessons());
        assertEquals(8, restored.exportState().lastAllocatedLessonSequence());
        assertTrue(restored.getLesson(new LessonId("L7")).isEmpty());
        assertSame(removedLatest, data.exportState());
    }

    @Test
    public void restore_removedMaximumLesson_keepsExhaustionDespiteLowerRetainedId() {
        Lesson retained = retainedLesson();
        PonHubData data = new PonHubData(new PonHubDataState(people, List.of(retained), List.of(), Long.MAX_VALUE - 1));
        Lesson finalLesson = addLesson(data, LessonDay.TUESDAY);
        PonHubDataState allocated = data.exportState();
        // The model has no lesson-deletion API yet; restore the snapshot left after removing the final record.
        PonHubDataState removedMaximum = new PonHubDataState(allocated.people(), List.of(retained),
                allocated.attendance(), allocated.lastAllocatedLessonSequence());
        data.resetData(removedMaximum);
        PonHubData restored = new PonHubData(data.exportState());
        AtomicInteger publications = new AtomicInteger();
        restored.stateProperty().addListener((observable, oldState, newState) -> publications.incrementAndGet());

        assertTrue(restored.getLesson(finalLesson.getId()).isEmpty());
        assertEquals(List.of(retained), restored.getLessons());
        assertEquals(Long.MAX_VALUE, restored.exportState().lastAllocatedLessonSequence());
        assertThrows(IllegalStateException.class, () -> addLesson(restored, LessonDay.WEDNESDAY));
        assertSame(removedMaximum, restored.exportState());
        assertEquals(0, publications.get());
        assertEquals(List.of(retained, finalLesson), allocated.lessons());
    }

    private Lesson addLesson(PonHubData data, LessonDay day) {
        return data.addLesson(tutor.getId(), slot(day), new Subject("Mathematics"), new Room("R1"));
    }

    private Lesson retainedLesson() {
        return new Lesson(new LessonId("L1"), tutor.getId(), slot(LessonDay.MONDAY),
                new Subject("Mathematics"), new Room("R1"), Set.of());
    }

    private LessonTimeSlot slot(LessonDay day) {
        return new LessonTimeSlot(day, new LessonTime("0900"), new LessonTime("1000"));
    }
}
