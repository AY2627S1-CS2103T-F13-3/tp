package seedu.address.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import seedu.address.model.lesson.Lesson;
import seedu.address.model.lesson.LessonDay;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.lesson.LessonTime;
import seedu.address.model.lesson.LessonTimeSlot;
import seedu.address.model.lesson.Room;
import seedu.address.model.lesson.Subject;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.Name;
import seedu.address.model.person.PeopleRegistryState;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Tutor;

public class PonHubDataLessonEditTest {

    @Test
    public void replaceLesson_equalAndChangedValues_publishesOnlyChangedCompleteSnapshot() {
        PonHubDataState source = sourceWithTwoLessons();
        PonHubData data = new PonHubData(source);
        AtomicInteger notifications = new AtomicInteger();
        data.stateProperty().addListener((observable, oldState, newState) -> {
            notifications.incrementAndGet();
            assertSame(source, oldState);
            assertSame(data.exportState(), newState);
            assertEquals(source.people(), newState.people());
            assertEquals(source.attendance(), newState.attendance());
        });
        Lesson original = source.lessons().getFirst();
        Lesson equalReplacement = new Lesson(original);

        assertSame(original, data.replaceLesson(equalReplacement));
        assertSame(source, data.exportState());
        assertEquals(0, notifications.get());

        Lesson changed = new Lesson(original.getId(), original.getTutorId(), original.getTimeSlot(),
                new Subject("Advanced Math"), original.getRoom(), original.getEnrolledStudentIds());
        assertSame(changed, data.replaceLesson(changed));
        assertEquals(1, notifications.get());
        assertSame(changed, data.getLessons().getFirst());
        assertEquals(source.lessons().get(1), data.getLessons().get(1));
        assertEquals(source.lastAllocatedLessonSequence(), data.exportState().lastAllocatedLessonSequence());
    }

    @Test
    public void replaceLesson_nullMissingAndConflictingValues_preservesInstalledSnapshot() {
        PonHubDataState source = sourceWithTwoLessons();
        PonHubData data = new PonHubData(source);
        AtomicInteger notifications = new AtomicInteger();
        data.stateProperty().addListener((observable, oldState, newState) -> notifications.incrementAndGet());

        assertThrows(NullPointerException.class, () -> data.replaceLesson(null));
        Lesson missing = lesson("L99", "T1", LessonDay.WEDNESDAY, "0900", "1000", "English", "R3");
        assertThrows(IllegalArgumentException.class, () -> data.replaceLesson(missing));
        Lesson conflict = lesson("L2", "T1", LessonDay.MONDAY, "0900", "1000", "Science", "R2");
        assertThrows(IllegalArgumentException.class, () -> data.replaceLesson(conflict));

        assertSame(source, data.exportState());
        assertEquals(0, notifications.get());
    }

    private static PonHubDataState sourceWithTwoLessons() {
        List<PersonRecord> people = List.of(tutor("T1", "Tutor One", "00111111"),
                tutor("T2", "Tutor Two", "00222222"));
        PeopleRegistryState peopleState = new PeopleRegistryState(people,
                Map.of(PersonRole.STUDENT, 0L, PersonRole.TUTOR, 2L, PersonRole.PARENT, 0L));
        Lesson first = lesson("L1", "T1", LessonDay.MONDAY, "0900", "1000", "Math", "R1");
        Lesson second = lesson("L2", "T2", LessonDay.MONDAY, "1000", "1100", "Science", "R2");
        return new PonHubDataState(peopleState, List.of(first, second), List.of(), 8);
    }

    private static Lesson lesson(String id, String tutorId, LessonDay day, String startTime, String endTime,
            String subject, String room) {
        return new Lesson(new LessonId(id), new PersonId(tutorId),
                new LessonTimeSlot(day, new LessonTime(startTime), new LessonTime(endTime)),
                new Subject(subject), new Room(room), Set.of());
    }

    private static Tutor tutor(String id, String name, String phone) {
        return new Tutor(new PersonId(id), new ContactDetails(new Name(name), Optional.of(new Phone(phone)),
                Optional.empty(), Optional.empty()));
    }
}
