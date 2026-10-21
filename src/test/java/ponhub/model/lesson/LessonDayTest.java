package ponhub.model.lesson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Map;

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

    @Test
    public void matches_allWeekdayPairs_matchesOnlyCorrespondingDate() {
        Map<LessonDay, LocalDate> datesByLessonDay = Map.of(
                LessonDay.MONDAY, LocalDate.of(2026, 10, 5),
                LessonDay.TUESDAY, LocalDate.of(2026, 10, 6),
                LessonDay.WEDNESDAY, LocalDate.of(2026, 10, 7),
                LessonDay.THURSDAY, LocalDate.of(2026, 10, 8),
                LessonDay.FRIDAY, LocalDate.of(2026, 10, 9),
                LessonDay.SATURDAY, LocalDate.of(2026, 10, 10),
                LessonDay.SUNDAY, LocalDate.of(2026, 10, 11));

        for (LessonDay lessonDay : LessonDay.values()) {
            for (Map.Entry<LessonDay, LocalDate> entry : datesByLessonDay.entrySet()) {
                assertEquals(lessonDay == entry.getKey(), lessonDay.matches(entry.getValue()));
            }
        }
    }

    @Test
    public void matches_dateBoundariesAndLeapDay_matchesTypedWeekday() {
        assertTrue(LessonDay.MONDAY.matches(LocalDate.of(1, 1, 1)));
        assertTrue(LessonDay.THURSDAY.matches(LocalDate.of(2024, 2, 29)));
        assertTrue(LessonDay.FRIDAY.matches(LocalDate.of(9999, 12, 31)));
    }

    @Test
    public void matches_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> LessonDay.MONDAY.matches(null));
    }
}
