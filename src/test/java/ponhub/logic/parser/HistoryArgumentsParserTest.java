package ponhub.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.attendance.AttendanceStatus;
import ponhub.model.lesson.LessonId;

public class HistoryArgumentsParserTest {
    private final HistoryArgumentsParser parser = new HistoryArgumentsParser();

    @Test
    public void parse_validIndexAndOptionalLesson() throws Exception {
        var all = parser.parse("  2  ");
        assertEquals(2, all.studentIndex().getOneBased());
        assertTrue(all.lessonId().isEmpty());
        assertTrue(all.date().isEmpty());
        assertTrue(all.status().isEmpty());
        var filtered = parser.parse("2 lid/l10");
        assertEquals(2, filtered.studentIndex().getOneBased());
        assertEquals(new LessonId("L10"), filtered.lessonId().orElseThrow());
    }

    @Test
    public void parse_optionalFiltersCombineInAnyOrder() throws Exception {
        String[] filters = {"lid/l10 d/2026-10-05 s/ABSENT", "lid/L10 s/absent d/2026-10-05",
            "d/2026-10-05 lid/L10 s/absent", "d/2026-10-05 s/absent lid/L10",
            "s/absent lid/L10 d/2026-10-05", "s/absent d/2026-10-05 lid/L10"};
        for (String filter : filters) {
            var input = parser.parse("2 " + filter);
            assertEquals(2, input.studentIndex().getOneBased());
            assertEquals(new LessonId("L10"), input.lessonId().orElseThrow());
            assertEquals(LocalDate.of(2026, 10, 5), input.date().orElseThrow());
            assertEquals(AttendanceStatus.ABSENT, input.status().orElseThrow());
        }
    }

    @Test
    public void parse_dateAndStatusWithoutLesson() throws Exception {
        assertEquals(LocalDate.of(2024, 2, 29), parser.parse("1 d/2024-02-29").date().orElseThrow());
        assertEquals(AttendanceStatus.PRESENT, parser.parse("1 s/Present").status().orElseThrow());
        var combined = parser.parse("1 s/absent d/2026-10-05");
        assertTrue(combined.lessonId().isEmpty());
        assertEquals(AttendanceStatus.ABSENT, combined.status().orElseThrow());
        assertEquals(LocalDate.of(2026, 10, 5), combined.date().orElseThrow());
    }

    @Test
    public void parse_invalidFormOrId_rejected() {
        String[] invalid = {"", "S2", "0", "-1", "1 lid/", "1 lid/L0", "1 x/value",
            "1 lid/L1 lid/L2", "1\nlid/L1", "1\tlid/L1", "1 lid/L1\u0000",
            "1 d/", "1 s/", "1 d/2026-02-29", "1 d/2026-2-01", "1 d/0000-01-01",
            "1 s/unrecorded", "1 s/present absent", "1 d/2026-10-05 d/2026-10-12",
            "1 s/present s/absent", "1 lid/L1 extra", "1 d/2026-10-05 extra", "1 s/present extra"};
        for (String input : invalid) {
            assertThrows(ParseException.class, () -> parser.parse(input), input);
        }
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
