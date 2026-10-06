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
        assertNotEquals(slot, null);
        assertNotEquals(slot, "Mon 0900-1000");
    }

    private static LessonTimeSlot slot(LessonDay day, String startTime, String endTime) {
        return new LessonTimeSlot(day, new LessonTime(startTime), new LessonTime(endTime));
    }
}
