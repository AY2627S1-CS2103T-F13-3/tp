package seedu.address.model.lesson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class LessonIdTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new LessonId(null));
    }

    @Test
    public void constructor_invalidLessonId_throwsIllegalArgumentException() {
        String[] invalidIds = {"", "L", "L0", "L01", "1", "T1", "L-1", "L 1",
            "L9223372036854775808"};

        for (String invalidId : invalidIds) {
            assertThrows(IllegalArgumentException.class, () -> new LessonId(invalidId));
        }
    }

    @Test
    public void constructor_lowerCasePrefix_normalizesPrefix() {
        assertEquals("L12", new LessonId("l12").toString());
    }

    @Test
    public void isValidLessonId() {
        assertThrows(NullPointerException.class, () -> LessonId.isValidLessonId(null));

        assertFalse(LessonId.isValidLessonId("L0"));
        assertFalse(LessonId.isValidLessonId("L01"));
        assertFalse(LessonId.isValidLessonId("L9223372036854775808"));

        assertTrue(LessonId.isValidLessonId("L1"));
        assertTrue(LessonId.isValidLessonId("l42"));
        assertTrue(LessonId.isValidLessonId("L" + Long.MAX_VALUE));
    }

    @Test
    public void fromSequenceNumber_invalidNumber_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> LessonId.fromSequenceNumber(0));
        assertThrows(IllegalArgumentException.class, () -> LessonId.fromSequenceNumber(-1));
    }

    @Test
    public void next_validId_returnsNextId() {
        assertEquals(new LessonId("L2"), new LessonId("L1").next());
    }

    @Test
    public void next_maximumSequence_throwsIllegalStateException() {
        LessonId maximumId = LessonId.fromSequenceNumber(Long.MAX_VALUE);
        assertThrows(IllegalStateException.class, maximumId::next);
    }

    @Test
    public void compareTo_digitLengthAndLongBoundaries_ordersByNumericSequence() {
        LessonId two = new LessonId("L2");
        LessonId ten = new LessonId("L10");
        LessonId smallest = LessonId.fromSequenceNumber(1);
        LessonId penultimate = LessonId.fromSequenceNumber(Long.MAX_VALUE - 1);
        LessonId largest = LessonId.fromSequenceNumber(Long.MAX_VALUE);

        assertTrue(two.compareTo(ten) < 0);
        assertTrue(ten.compareTo(two) > 0);
        assertTrue(penultimate.compareTo(largest) < 0);
        assertTrue(largest.compareTo(penultimate) > 0);
        assertTrue(smallest.compareTo(largest) < 0);
        assertTrue(largest.compareTo(smallest) > 0);
        assertEquals(0, largest.compareTo(new LessonId("l" + Long.MAX_VALUE)));
        assertThrows(NullPointerException.class, () -> largest.compareTo(null));
    }

    @Test
    public void equals() {
        LessonId lessonId = new LessonId("L5");

        assertEquals(lessonId, lessonId);
        assertEquals(lessonId, new LessonId("l5"));
        assertNotEquals(lessonId, new LessonId("L6"));
        assertNotEquals(lessonId, null);
        assertNotEquals(lessonId, "L5");
    }
}
