package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.attendance.Attendance;
import seedu.address.model.lesson.LessonId;

public class AttendanceUnmarkParserTest {
    private final AttendanceUnmarkParser parser = new AttendanceUnmarkParser();

    @Test
    public void parse_validArguments_returnsValidatedAttendanceInput() throws Exception {
        assertEquals(new AttendanceUnmarkInput(Index.fromOneBased(2), new LessonId("L12"),
                LocalDate.of(2026, 10, 5)), parser.parse(" 2 lid/l12 d/2026-10-05 "));
        assertEquals(new AttendanceUnmarkInput(Index.fromOneBased(3), new LessonId("L7"),
                LocalDate.of(2024, 2, 29)), parser.parse("3 d/2024-02-29 lid/L7"));
        assertEquals(new AttendanceUnmarkInput(Index.fromOneBased(2), new LessonId("L12"),
                LocalDate.of(2026, 10, 5)),
                parser.parse("\u00A02\u00A0lid/\u00A0L12\u00A0d/\u00A02026-10-05\u00A0"));
    }

    @Test
    public void parse_missingIndexOrPrefix_throwsParseException() {
        for (String arguments : new String[]{"", "1", "lid/L1 d/2026-10-05",
            "1 d/2026-10-05", "1 lid/L1"}) {
            assertThrows(ParseException.class, () -> parser.parse(arguments));
        }
    }

    @Test
    public void parse_statusRepeatedUnknownOrInvalidPreamble_throwsParseException() {
        assertThrows(ParseException.class, "Unknown unmark prefix 's/'.", () -> parser.parse(
                "1 lid/L1 d/2026-10-05 s/present"));
        assertThrows(ParseException.class, "Unmark prefix 'lid/' requires a non-blank value.", () -> parser.parse(
                "1 lid/\u00A0 d/2026-10-05"));
        for (String arguments : new String[]{"S1 lid/L1 d/2026-10-05",
            "1 lid/L1 lid/L2 d/2026-10-05", "1 lid/L1 d/2026-10-05 d/2026-10-12",
            "1 lesson/L1 d/2026-10-05", "1 lid/L1 date/2026-10-05"}) {
            assertThrows(ParseException.class, () -> parser.parse(arguments));
        }
    }

    @Test
    public void parse_invalidIndexLessonOrDate_throwsSpecificConstraint() {
        assertThrows(ParseException.class, ParserUtil.MESSAGE_INVALID_INDEX, () -> parser.parse(
                "0 lid/L1 d/2026-10-05"));
        assertThrows(ParseException.class, LessonId.MESSAGE_CONSTRAINTS, () -> parser.parse(
                "1 lid/L01 d/2026-10-05"));
        assertThrows(ParseException.class, Attendance.MESSAGE_DATE_CONSTRAINTS, () -> parser.parse(
                "1 lid/L1 d/2026-02-30"));
    }

    @Test
    public void parse_lineBreakOrControlCharacter_throwsSingleLineMessage() {
        for (String arguments : new String[]{"1 lid/L1 d/2026-10-05\n",
            "1 lid/L1\rd/2026-10-05", "1\tlid/L1 d/2026-10-05"}) {
            assertThrows(ParseException.class, AttendanceUnmarkParser.MESSAGE_SINGLE_LINE, () -> parser.parse(
                    arguments));
        }
    }

    @Test
    public void parse_nullArguments_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
