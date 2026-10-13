package seedu.address.model.lesson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class LessonTimeSlotTest {

    private static final LessonTime NINE_AM = new LessonTime("0900");
    private static final LessonTime TEN_AM = new LessonTime("1000");
    private static final LessonTime ELEVEN_AM = new LessonTime("1100");

    @Test
    public void constructor_nullValue_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new LessonTimeSlot(null, NINE_AM, TEN_AM));
        assertThrows(NullPointerException.class, () -> new LessonTimeSlot(LessonDay.MONDAY, null, TEN_AM));
        assertThrows(NullPointerException.class, () -> new LessonTimeSlot(LessonDay.MONDAY, NINE_AM, null));
    }

    @Test
    public void constructor_endNotAfterStart_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> slot(LessonDay.MONDAY, "0900", "0900"));
        assertThrows(IllegalArgumentException.class, () -> slot(LessonDay.MONDAY, "1000", "0900"));
    }

    @Test
    public void overlaps_sharedTimeOnSameDay_returnsTrue() {
        LessonTimeSlot first = slot(LessonDay.MONDAY, "0900", "1030");
        LessonTimeSlot second = slot(LessonDay.MONDAY, "1000", "1100");
        LessonTimeSlot same = slot(LessonDay.MONDAY, "0900", "1030");

        assertTrue(first.overlaps(second));
        assertTrue(second.overlaps(first));
        assertTrue(first.overlaps(same));
    }

    // OpenAI Codex assisted Zhu Zhi Yu with these boundary regressions (#198).
    @Test
    public void overlaps_strictContainment_returnsTrueInBothDirections() {
        LessonTimeSlot containing = slot(LessonDay.MONDAY, "0900", "1200");
        LessonTimeSlot contained = slot(LessonDay.MONDAY, "1000", "1100");

        assertTrue(containing.overlaps(contained));
        assertTrue(contained.overlaps(containing));
    }

    @Test
    public void overlaps_separatedSlots_returnsFalseInBothDirections() {
        LessonTimeSlot earlier = slot(LessonDay.MONDAY, "0900", "1000");
        LessonTimeSlot later = slot(LessonDay.MONDAY, "1100", "1200");

        assertFalse(earlier.overlaps(later));
        assertFalse(later.overlaps(earlier));
    }

    @Test
    public void overlaps_oneMinuteAtMidnight_distinguishesSharedTimeFromAdjacency() {
        LessonTimeSlot firstMinute = slot(LessonDay.MONDAY, "0000", "0001");
        LessonTimeSlot nextMinute = slot(LessonDay.MONDAY, "0001", "0002");
        LessonTimeSlot firstTwoMinutes = slot(LessonDay.MONDAY, "0000", "0002");

        assertFalse(firstMinute.overlaps(nextMinute));
        assertFalse(nextMinute.overlaps(firstMinute));
        assertTrue(firstMinute.overlaps(firstTwoMinutes));
        assertTrue(firstTwoMinutes.overlaps(firstMinute));
    }

    @Test
    public void overlaps_oneMinuteAtDayEnd_distinguishesSharedTimeFromAdjacency() {
        LessonTimeSlot earlierMinute = slot(LessonDay.SUNDAY, "2357", "2358");
        LessonTimeSlot lastMinute = slot(LessonDay.SUNDAY, "2358", "2359");
        LessonTimeSlot lastTwoMinutes = slot(LessonDay.SUNDAY, "2357", "2359");

        assertFalse(earlierMinute.overlaps(lastMinute));
        assertFalse(lastMinute.overlaps(earlierMinute));
        assertTrue(lastMinute.overlaps(lastTwoMinutes));
        assertTrue(lastTwoMinutes.overlaps(lastMinute));
    }

    @Test
    public void constructor_dayEndCannotWrapToMidnight_rejectsOvernightSlot() {
        assertThrows(IllegalArgumentException.class, () -> slot(LessonDay.SUNDAY, "2359", "0000"));
    }

    @Test
    public void overlaps_adjacentSlots_returnsFalse() {
        LessonTimeSlot first = new LessonTimeSlot(LessonDay.MONDAY, NINE_AM, TEN_AM);
        LessonTimeSlot second = new LessonTimeSlot(LessonDay.MONDAY, TEN_AM, ELEVEN_AM);

        assertFalse(first.overlaps(second));
        assertFalse(second.overlaps(first));
    }

    @Test
    public void overlaps_sameTimeOnDifferentDays_returnsFalse() {
        LessonTimeSlot monday = new LessonTimeSlot(LessonDay.MONDAY, NINE_AM, TEN_AM);
        LessonTimeSlot tuesday = new LessonTimeSlot(LessonDay.TUESDAY, NINE_AM, TEN_AM);

        assertFalse(monday.overlaps(tuesday));
        assertFalse(tuesday.overlaps(monday));
    }

    @Test
    public void overlaps_null_throwsNullPointerException() {
        LessonTimeSlot slot = new LessonTimeSlot(LessonDay.MONDAY, NINE_AM, TEN_AM);
        assertThrows(NullPointerException.class, () -> slot.overlaps(null));
    }

    @Test
    public void equals() {
        LessonTimeSlot slot = new LessonTimeSlot(LessonDay.MONDAY, NINE_AM, TEN_AM);

        assertEquals(slot, slot);
        assertEquals(slot, new LessonTimeSlot(LessonDay.MONDAY, NINE_AM, TEN_AM));
        assertNotEquals(slot, new LessonTimeSlot(LessonDay.TUESDAY, NINE_AM, TEN_AM));
        assertNotEquals(slot, slot(LessonDay.MONDAY, "0901", "1000"));
        assertNotEquals(slot, slot(LessonDay.MONDAY, "0900", "1001"));
        assertNotEquals(slot, null);
        assertNotEquals(slot, "Mon 0900-1000");
    }

    private static LessonTimeSlot slot(LessonDay day, String startTime, String endTime) {
        return new LessonTimeSlot(day, new LessonTime(startTime), new LessonTime(endTime));
    }
}
