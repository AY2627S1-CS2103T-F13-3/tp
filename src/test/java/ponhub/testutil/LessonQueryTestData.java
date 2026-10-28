package ponhub.testutil;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import ponhub.model.PonHubData;
import ponhub.model.PonHubDataState;
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
import ponhub.model.person.PersonId;
import ponhub.model.person.Phone;

/**
 * Shared-query fixtures with duplicate names, mixed people order, shared and empty lessons.
 */
public final class LessonQueryTestData {
    public static final PersonId STUDENT_ONE = new PersonId("S1");
    public static final PersonId STUDENT_TWO = new PersonId("S2");
    public static final PersonId STUDENT_WITHOUT_LESSONS = new PersonId("S3");
    public static final PersonId TUTOR_ONE = new PersonId("T1");
    public static final PersonId TUTOR_TWO = new PersonId("T2");
    public static final PersonId TUTOR_WITHOUT_LESSONS = new PersonId("T3");
    public static final LessonId SHARED_LESSON = new LessonId("L10");
    public static final LessonId EMPTY_LESSON = new LessonId("L3");

    private LessonQueryTestData() {
    }

    /**
     * Catalogue order deliberately differs from numeric ID and schedule order.
     */
    public static PonHubData createData() {
        PeopleRegistry people = PeopleSampleDataUtil.getSamplePeopleRegistry();
        people.addTutor(new ContactDetails(new Name("Unassigned Tutor"), Optional.of(new Phone("12345678")),
                Optional.empty(), Optional.empty()));
        List<Lesson> lessons = List.of(
                lesson("L2", TUTOR_ONE, "Tue", "0900", "1000", "Science", "R1", Set.of(STUDENT_ONE)),
                lesson("L10", TUTOR_ONE, "Mon", "0900", "1000", "Math", "R1",
                        Set.of(STUDENT_TWO, STUDENT_ONE)),
                lesson("L3", TUTOR_TWO, "Mon", "0900", "1000", "Science", "R2", Set.of()),
                lesson("L4", TUTOR_ONE, "Mon", "1000", "1100", "Math", "R1", Set.of()),
                lesson("L11", TUTOR_TWO, "Wed", "0000", "2359", "中文", "R10", Set.of(STUDENT_TWO)));
        return new PonHubData(new PonHubDataState(people.exportState(), lessons, List.of(), 11));
    }

    /**
     * Builds a validated immutable lesson.
     */
    public static Lesson lesson(String id, PersonId tutor, String day, String start, String end,
            String subject, String room, Set<PersonId> students) {
        return new Lesson(new LessonId(id), tutor, new LessonTimeSlot(LessonDay.fromString(day),
                new LessonTime(start), new LessonTime(end)), new Subject(subject), new Room(room), students);
    }
}
