package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.lesson.LessonId;

public class HistoryArgumentsParserTest {
    private final HistoryArgumentsParser parser = new HistoryArgumentsParser();

    @Test
    public void parse_validIndexAndOptionalLesson() throws Exception {
        var all = parser.parse("  2  ");
        assertEquals(2, all.studentIndex().getOneBased());
        assertTrue(all.lessonId().isEmpty());
        var filtered = parser.parse("2 lid/l10");
        assertEquals(2, filtered.studentIndex().getOneBased());
        assertEquals(new LessonId("L10"), filtered.lessonId().orElseThrow());
    }

    @Test
    public void parse_invalidFormOrId_rejected() {
        String[] invalid = {"", "S2", "0", "-1", "1 lid/", "1 lid/L0", "1 d/2026-10-05",
            "1 lid/L1 lid/L2", "1 lid/L1 s/absent", "1\nlid/L1", "1\tlid/L1", "1 lid/L1\u0000"};
        for (String input : invalid) {
            assertThrows(ParseException.class, () -> parser.parse(input), input);
        }
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
