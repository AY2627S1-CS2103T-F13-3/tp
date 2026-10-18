package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.model.lesson.LessonDay;
import seedu.address.model.lesson.LessonTime;
import seedu.address.model.lesson.Room;
import seedu.address.model.lesson.Subject;
import seedu.address.model.person.PersonId;

public class LessonEditInputTest {

    @Test
    public void constructor_mixedReplacements_preservesEachIntent() {
        LessonDay day = LessonDay.WEDNESDAY;
        LessonTime startTime = new LessonTime("0900");
        Subject subject = new Subject("Physics");
        PersonId tutorId = new PersonId("T2");
        LessonEditInput input = new LessonEditInput(Optional.of(day), Optional.of(startTime), Optional.empty(),
                Optional.of(subject), Optional.of(tutorId), Optional.empty());

        assertEquals(Optional.of(day), input.day());
        assertEquals(Optional.of(startTime), input.startTime());
        assertTrue(input.endTime().isEmpty());
        assertEquals(Optional.of(subject), input.subject());
        assertEquals(Optional.of(tutorId), input.tutorId());
        assertTrue(input.room().isEmpty());
    }

    @Test
    public void constructor_allFieldsOmitted_acceptsModelLevelNoOp() {
        LessonEditInput input = new LessonEditInput(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());

        assertTrue(input.day().isEmpty());
        assertTrue(input.startTime().isEmpty());
        assertTrue(input.endTime().isEmpty());
        assertTrue(input.subject().isEmpty());
        assertTrue(input.tutorId().isEmpty());
        assertTrue(input.room().isEmpty());
    }

    @Test
    public void constructor_nullComponent_throwsNullPointerException() {
        Optional<LessonDay> day = Optional.of(LessonDay.MONDAY);
        Optional<LessonTime> time = Optional.of(new LessonTime("0900"));
        Optional<Subject> subject = Optional.of(new Subject("Math"));
        Optional<PersonId> tutorId = Optional.of(new PersonId("T1"));
        Optional<Room> room = Optional.of(new Room("R1"));

        assertThrows(NullPointerException.class, () ->
                new LessonEditInput(null, time, time, subject, tutorId, room));
        assertThrows(NullPointerException.class, () ->
                new LessonEditInput(day, null, time, subject, tutorId, room));
        assertThrows(NullPointerException.class, () ->
                new LessonEditInput(day, time, null, subject, tutorId, room));
        assertThrows(NullPointerException.class, () ->
                new LessonEditInput(day, time, time, null, tutorId, room));
        assertThrows(NullPointerException.class, () ->
                new LessonEditInput(day, time, time, subject, null, room));
        assertThrows(NullPointerException.class, () ->
                new LessonEditInput(day, time, time, subject, tutorId, null));
    }
}
