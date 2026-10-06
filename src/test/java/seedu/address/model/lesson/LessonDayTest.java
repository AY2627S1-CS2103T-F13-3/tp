package seedu.address.model.lesson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class LessonDayTest {

    @Test
    public void fromString_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> LessonDay.fromString(null));
    }

    @Test
    public void fromString_invalidDay_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> LessonDay.fromString("Monday"));
        assertThrows(IllegalArgumentException.class, () -> LessonDay.fromString(""));
        assertThrows(IllegalArgumentException.class, () -> LessonDay.fromString(" Mon "));
    }

    @Test
    public void fromString_supportedDay_parsesCaseInsensitively() {
        assertEquals(LessonDay.MONDAY, LessonDay.fromString("Mon"));
        assertEquals(LessonDay.TUESDAY, LessonDay.fromString("tUE"));
        assertEquals("Sun", LessonDay.fromString("SUN").toString());
    }

    @Test
    public void isValidLessonDay() {
        assertThrows(NullPointerException.class, () -> LessonDay.isValidLessonDay(null));

        assertFalse(LessonDay.isValidLessonDay("Monday"));
        assertFalse(LessonDay.isValidLessonDay(""));
        assertTrue(LessonDay.isValidLessonDay("Fri"));
        assertTrue(LessonDay.isValidLessonDay("sAt"));
    }
}
