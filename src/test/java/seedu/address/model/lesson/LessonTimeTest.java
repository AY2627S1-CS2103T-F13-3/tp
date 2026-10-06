package seedu.address.model.lesson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.Test;

public class LessonTimeTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new LessonTime(null));
    }

    @Test
    public void constructor_invalidTime_throwsIllegalArgumentException() {
        String[] invalidTimes = {"", "900", "09000", "09:00", "2400", "1260", "-100", "abcd"};

        for (String invalidTime : invalidTimes) {
            assertThrows(IllegalArgumentException.class, () -> new LessonTime(invalidTime));
        }
    }

    @Test
    public void isValidLessonTime() {
        assertThrows(NullPointerException.class, () -> LessonTime.isValidLessonTime(null));

        assertFalse(LessonTime.isValidLessonTime("2400"));
        assertFalse(LessonTime.isValidLessonTime("1260"));
        assertFalse(LessonTime.isValidLessonTime("930"));

        assertTrue(LessonTime.isValidLessonTime("0000"));
        assertTrue(LessonTime.isValidLessonTime("0930"));
        assertTrue(LessonTime.isValidLessonTime("2359"));
    }

    @Test
    public void toString_earlyTime_retainsLeadingZeros() {
        assertEquals("0030", new LessonTime("0030").toString());
        assertEquals("0905", new LessonTime("0905").toString());
    }

    @Test
    public void toString_nonLatinDefaultLocale_usesAsciiDigitsAndRoundTrips() {
        Locale originalFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag("ar-EG"));
            LessonTime time = new LessonTime("0905");

            assertEquals("0905", time.toString());
            assertEquals(time, new LessonTime(time.toString()));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, originalFormatLocale);
        }
    }

    @Test
    public void compareTo() {
        LessonTime early = new LessonTime("0900");
        LessonTime late = new LessonTime("1000");

        assertTrue(early.compareTo(late) < 0);
        assertEquals(0, early.compareTo(new LessonTime("0900")));
        assertTrue(late.compareTo(early) > 0);
        assertThrows(NullPointerException.class, () -> early.compareTo(null));
    }

    @Test
    public void equals() {
        LessonTime time = new LessonTime("0900");

        assertEquals(time, time);
        assertEquals(time, new LessonTime("0900"));
        assertNotEquals(time, new LessonTime("0901"));
        assertNotEquals(time, null);
        assertNotEquals(time, "0900");
    }
}
