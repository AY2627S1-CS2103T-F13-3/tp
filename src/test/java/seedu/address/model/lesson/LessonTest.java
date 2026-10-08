package seedu.address.model.lesson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.PersonId;

public class LessonTest {

    private static final LessonId LESSON_ID = new LessonId("L1");
    private static final PersonId TUTOR_ID = new PersonId("T1");
    private static final LessonTimeSlot TIME_SLOT = new LessonTimeSlot(
            LessonDay.MONDAY, new LessonTime("0900"), new LessonTime("1030"));
    private static final Subject SUBJECT = new Subject("Mathematics");
    private static final Room ROOM = new Room("R1");

    @Test
    public void constructor_emptyAndMultipleStudentRosters_preservesAllFields() {
        Lesson emptyLesson = createLesson(Set.of());
        Set<PersonId> students = new LinkedHashSet<>(Set.of(new PersonId("S1"), new PersonId("S2")));
        Lesson sharedLesson = createLesson(students);

        assertEquals(LESSON_ID, emptyLesson.getId());
        assertEquals(TUTOR_ID, emptyLesson.getTutorId());
        assertEquals(TIME_SLOT, emptyLesson.getTimeSlot());
        assertEquals(SUBJECT, emptyLesson.getSubject());
        assertEquals(ROOM, emptyLesson.getRoom());
        assertTrue(emptyLesson.getEnrolledStudentIds().isEmpty());
        assertEquals(students, sharedLesson.getEnrolledStudentIds());
    }

    @Test
    public void constructor_wrongRoleIds_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Lesson(LESSON_ID, new PersonId("S1"), TIME_SLOT,
                SUBJECT, ROOM, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Lesson(LESSON_ID, new PersonId("P1"), TIME_SLOT,
                SUBJECT, ROOM, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> createLesson(Set.of(new PersonId("T2"))));
        assertThrows(IllegalArgumentException.class, () -> createLesson(Set.of(new PersonId("P2"))));
    }

    @Test
    public void constructor_nullRequiredValues_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Lesson(null, TUTOR_ID, TIME_SLOT, SUBJECT, ROOM, Set.of()));
        assertThrows(NullPointerException.class, () -> new Lesson(LESSON_ID, null, TIME_SLOT, SUBJECT, ROOM, Set.of()));
        assertThrows(NullPointerException.class, () -> new Lesson(LESSON_ID, TUTOR_ID, null, SUBJECT, ROOM, Set.of()));
        assertThrows(NullPointerException.class, () ->
                new Lesson(LESSON_ID, TUTOR_ID, TIME_SLOT, null, ROOM, Set.of()));
        assertThrows(NullPointerException.class, () -> new Lesson(LESSON_ID, TUTOR_ID, TIME_SLOT, SUBJECT, null,
                Set.of()));
        assertThrows(NullPointerException.class, () -> new Lesson(LESSON_ID, TUTOR_ID, TIME_SLOT, SUBJECT, ROOM,
                null));

        Set<PersonId> rosterWithNull = new LinkedHashSet<>();
        rosterWithNull.add(null);
        assertThrows(NullPointerException.class, () -> createLesson(rosterWithNull));
    }

    @Test
    public void roster_accessAndReplacement_areDefensiveAndImmutable() {
        Set<PersonId> sourceRoster = new LinkedHashSet<>(Set.of(new PersonId("S1")));
        Lesson original = createLesson(sourceRoster);
        sourceRoster.add(new PersonId("S2"));

        assertEquals(Set.of(new PersonId("S1")), original.getEnrolledStudentIds());
        assertThrows(UnsupportedOperationException.class, () ->
                original.getEnrolledStudentIds().add(new PersonId("S2")));

        Lesson enrolled = original.withEnrolledStudent(new PersonId("S2"));
        Lesson unenrolled = enrolled.withoutEnrolledStudent(new PersonId("S1"));
        Lesson replaced = unenrolled.withEnrolledStudentIds(Set.of(new PersonId("S3"), new PersonId("S4")));

        assertEquals(LESSON_ID, enrolled.getId());
        assertEquals(Set.of(new PersonId("S1")), original.getEnrolledStudentIds());
        assertEquals(Set.of(new PersonId("S1"), new PersonId("S2")), enrolled.getEnrolledStudentIds());
        assertEquals(Set.of(new PersonId("S2")), unenrolled.getEnrolledStudentIds());
        assertEquals(Set.of(new PersonId("S3"), new PersonId("S4")), replaced.getEnrolledStudentIds());
        assertTrue(enrolled.hasEnrolledStudent(new PersonId("S2")));
        assertFalse(unenrolled.hasEnrolledStudent(new PersonId("S1")));
        assertThrows(IllegalArgumentException.class, () -> original.withEnrolledStudent(new PersonId("T2")));
        assertThrows(IllegalArgumentException.class, () -> original.withoutEnrolledStudent(new PersonId("P2")));
        assertThrows(IllegalArgumentException.class, () -> original.hasEnrolledStudent(new PersonId("T2")));
    }

    @Test
    public void copyConstructor_andIdempotentReplacement_preserveValueWithoutSharingRoster() {
        Lesson original = createLesson(Set.of(new PersonId("S1")));
        Lesson copy = new Lesson(original);
        Lesson duplicateEnrolment = original.withEnrolledStudent(new PersonId("S1"));
        Lesson missingUnenrolment = original.withoutEnrolledStudent(new PersonId("S2"));

        assertEquals(original, copy);
        assertEquals(original.hashCode(), copy.hashCode());
        assertNotSame(original.getEnrolledStudentIds(), copy.getEnrolledStudentIds());
        assertEquals(original, duplicateEnrolment);
        assertEquals(original, missingUnenrolment);
    }

    @Test
    public void equals_eachPersistedFieldAndMembership_distinguishesState() {
        Lesson original = createLesson(Set.of(new PersonId("S1")));
        Lesson same = createLesson(Set.of(new PersonId("S1")));

        assertTrue(original.equals(original));
        assertEquals(original, same);
        assertEquals(original.hashCode(), same.hashCode());
        assertNotEquals(original, new Lesson(new LessonId("L2"), TUTOR_ID, TIME_SLOT, SUBJECT, ROOM,
                Set.of(new PersonId("S1"))));
        assertNotEquals(original, new Lesson(LESSON_ID, new PersonId("T2"), TIME_SLOT, SUBJECT, ROOM,
                Set.of(new PersonId("S1"))));
        assertNotEquals(original, new Lesson(LESSON_ID, TUTOR_ID,
                new LessonTimeSlot(LessonDay.TUESDAY, new LessonTime("0900"), new LessonTime("1030")),
                SUBJECT, ROOM, Set.of(new PersonId("S1"))));
        assertNotEquals(original, new Lesson(LESSON_ID, TUTOR_ID, TIME_SLOT, new Subject("Physics"), ROOM,
                Set.of(new PersonId("S1"))));
        assertNotEquals(original, new Lesson(LESSON_ID, TUTOR_ID, TIME_SLOT, SUBJECT, new Room("R2"),
                Set.of(new PersonId("S1"))));
        assertNotEquals(original, createLesson(Set.of(new PersonId("S2"))));
        assertNotEquals(original, null);
        assertNotEquals(original, LESSON_ID);
    }

    @Test
    public void toString_allFields_returnsFormattedString() {
        Lesson lesson = createLesson(Set.of(new PersonId("S1")));
        String expected = Lesson.class.getCanonicalName()
                + "{id=" + LESSON_ID + ", tutorId=" + TUTOR_ID + ", timeSlot=" + TIME_SLOT
                + ", subject=" + SUBJECT + ", room=" + ROOM + ", enrolledStudentIds=[S1]}";

        assertEquals(expected, lesson.toString());
    }

    private static Lesson createLesson(Set<PersonId> enrolledStudentIds) {
        return new Lesson(LESSON_ID, TUTOR_ID, TIME_SLOT, SUBJECT, ROOM, enrolledStudentIds);
    }
}
