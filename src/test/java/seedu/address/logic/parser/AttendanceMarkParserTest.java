package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.attendance.Attendance;
import seedu.address.model.attendance.AttendanceStatus;
import seedu.address.model.lesson.LessonId;

public class AttendanceMarkParserTest {
    private final AttendanceMarkParser parser = new AttendanceMarkParser();

    @Test
    public void parse_validArguments_returnsValidatedAttendanceInput() throws Exception {
        assertEquals(new AttendanceMarkInput(Index.fromOneBased(2), new LessonId("L12"),
                LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT),
                parser.parse(" 2 lid/l12 d/2026-10-05 s/PrEsEnT "));
        assertEquals(new AttendanceMarkInput(Index.fromOneBased(3), new LessonId("L7"),
                LocalDate.of(2024, 2, 29), AttendanceStatus.ABSENT),
                parser.parse("3 s/absent lid/L7 d/2024-02-29"));
    }

    @Test
    public void parse_unicodeBoundarySpaces_returnsEquivalentInput() throws Exception {
        AttendanceMarkInput expected = parser.parse("2 lid/L12 d/2026-10-05 s/present");
        for (String space : new String[]{"\u00A0", "\u202F", "\u3000"}) {
            assertEquals(expected, parser.parse(space + "2" + space + "lid/" + space + "L12"
                    + space + "d/" + space + "2026-10-05" + space + "s/" + space + "present" + space));
        }
    }

    @Test
    public void parse_invalidFieldsWithUnicodeSpaces_preservesErrorPrecedence() {
        assertThrows(ParseException.class, "Mark prefix 'lid/' requires a non-blank value.", () -> parser.parse(
                "0 lid/\u202F d/2026-10-05 s/present"));
        assertThrows(ParseException.class, "Repeated mark prefix 'lid/'.", () -> parser.parse(
                "0 lid/L1\u00A0lid/\u3000 d/2026-10-05 s/present"));
        assertThrows(ParseException.class, "Unknown mark prefix 'x/'.", () -> parser.parse(
                "0 lid/L1 d/2026-10-05 s/present\u3000x/\u202F"));
        assertThrows(ParseException.class, ParserUtil.MESSAGE_INVALID_INDEX, () -> parser.parse(
                "\u00A00\u202F lid/L01 d/invalid s/invalid"));
    }

    @Test
    public void parse_missingIndexOrPrefix_throwsParseException() {
        for (String arguments : new String[]{"", "1", "lid/L1 d/2026-10-05 s/present",
            "1 d/2026-10-05 s/present", "1 lid/L1 s/present", "1 lid/L1 d/2026-10-05"}) {
            assertThrows(ParseException.class, () -> parser.parse(arguments));
        }
    }

    @Test
    public void parse_repeatedUnknownOrInvalidPreamble_throwsParseException() {
        for (String arguments : new String[]{"S1 lid/L1 d/2026-10-05 s/present",
            "1 lid/L1 lid/L2 d/2026-10-05 s/present", "1 lid/L1 d/2026-10-05 d/2026-10-12 s/present",
            "1 lid/L1 d/2026-10-05 s/present s/absent", "1 lesson/L1 d/2026-10-05 s/present",
            "1 lid/L1 date/2026-10-05 s/present", "1 lid/L1 d/2026-10-05 status/present"}) {
            assertThrows(ParseException.class, () -> parser.parse(arguments));
        }
    }

    @Test
    public void parse_invalidIndexLessonDateOrStatus_throwsSpecificConstraint() {
        assertThrows(ParseException.class, ParserUtil.MESSAGE_INVALID_INDEX, () -> parser.parse(
                "0 lid/L1 d/2026-10-05 s/present"));
        assertThrows(ParseException.class, LessonId.MESSAGE_CONSTRAINTS, () -> parser.parse(
                "1 lid/L01 d/2026-10-05 s/present"));
        assertThrows(ParseException.class, Attendance.MESSAGE_DATE_CONSTRAINTS, () -> parser.parse(
                "1 lid/L1 d/2026-02-30 s/present"));
        assertThrows(ParseException.class, AttendanceStatus.MESSAGE_CONSTRAINTS, () -> parser.parse(
                "1 lid/L1 d/2026-10-05 s/unrecorded"));
    }

    @Test
    public void parse_lineBreakOrControlCharacter_throwsSingleLineMessage() {
        for (String control : new String[]{"\t", "\n", "\r", "\r\n", "\u0000", "\u001F", "\u007F",
            "\u0085", "\u2028", "\u2029"}) {
            for (String arguments : new String[]{control, control + "0 x/value", "0 lid/L01" + control,
                "1 lid/L1" + control + "d/2026-10-05 s/present"}) {
                assertThrows(ParseException.class, AttendanceMarkParser.MESSAGE_SINGLE_LINE, () -> parser.parse(
                        arguments));
            }
        }
    }

    @Test
    public void parse_nullArguments_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
