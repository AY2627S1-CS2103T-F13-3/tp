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
    private static final String VALID_ARGUMENTS = "2 lid/L12 d/2026-10-05 s/present";
    private static final AttendanceMarkInput VALID_INPUT = new AttendanceMarkInput(
            Index.fromOneBased(2), new LessonId("L12"), LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT);

    private final AttendanceMarkParser parser = new AttendanceMarkParser();

    @Test
    public void parse_validArguments_returnsValidatedAttendanceInput() throws Exception {
        assertEquals(VALID_INPUT, parser.parse(" 2 lid/l12 d/2026-10-05 s/PrEsEnT "));
        assertEquals(new AttendanceMarkInput(Index.fromOneBased(3), new LessonId("L7"),
                LocalDate.of(2024, 2, 29), AttendanceStatus.ABSENT),
                parser.parse("3 s/absent lid/L7 d/2024-02-29"));
    }

    @Test
    public void parse_prefixesInAnyOrder_returnsEquivalentInput() throws Exception {
        for (String prefixedArguments : new String[]{
            "lid/L12 d/2026-10-05 s/present",
            "lid/L12 s/present d/2026-10-05",
            "d/2026-10-05 lid/L12 s/present",
            "d/2026-10-05 s/present lid/L12",
            "s/present lid/L12 d/2026-10-05",
            "s/present d/2026-10-05 lid/L12"
        }) {
            assertEquals(VALID_INPUT, parser.parse("2 " + prefixedArguments));
        }
    }

    @Test
    public void parse_unicodeBoundarySpaces_returnsEquivalentInput() throws Exception {
        for (String space : new String[]{"\u2003", "\u2002", "\u3000", "\u00A0", "\u202F"}) {
            String arguments = space + "2" + space + "lid/" + space + "L12"
                    + space + "d/" + space + "2026-10-05"
                    + space + "s/" + space + "present" + space;
            assertEquals(VALID_INPUT, parser.parse(arguments));
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
    public void parse_missingIndex_reportsArgumentsMessage() {
        for (String arguments : new String[]{"", "1", "lid/L12 d/2026-10-05 s/present"}) {
            assertThrows(ParseException.class, AttendanceMarkParser.MESSAGE_ARGUMENTS, () ->
                    parser.parse(arguments));
        }
    }

    @Test
    public void parse_missingRequiredPrefix_reportsMissingPrefix() {
        String[][] cases = {
            {"2 d/2026-10-05 s/present", "lid/"},
            {"2 lid/L12 s/present", "d/"},
            {"2 lid/L12 d/2026-10-05", "s/"}
        };
        for (String[] testCase : cases) {
            assertThrows(ParseException.class,
                    "Missing required mark prefix '" + testCase[1] + "'.", () -> parser.parse(testCase[0]));
        }
    }

    @Test
    public void parse_blankValues_reportsBlankPrefix() {
        String[][] cases = {
            {"2 lid/ d/2026-10-05 s/present", "lid/"},
            {"2 lid/L12 d/ s/present", "d/"},
            {"2 lid/L12 d/2026-10-05 s/", "s/"},
            {"2 lid/\u2003d/2026-10-05 s/present", "lid/"}
        };
        for (String[] testCase : cases) {
            assertThrows(ParseException.class,
                    "Mark prefix '" + testCase[1] + "' requires a non-blank value.", () ->
                            parser.parse(testCase[0]));
        }
    }

    @Test
    public void parse_repeatedPrefixes_rejectsEqualAndDifferentValues() {
        String[][] fields = {
            {"lid/", "L12", "L7"},
            {"d/", "2026-10-05", "2026-10-12"},
            {"s/", "present", "absent"}
        };
        for (String[] field : fields) {
            for (String value : new String[]{field[1], field[2]}) {
                assertThrows(ParseException.class, "Repeated mark prefix '" + field[0] + "'.", () ->
                        parser.parse(VALID_ARGUMENTS + " " + field[0] + value));
            }
        }
    }

    @Test
    public void parse_unknownAndUppercasePrefixes_reportsUnknownPrefix() {
        for (String prefix : new String[]{"lesson/", "date/", "status/", "LID/", "D/", "S/"}) {
            assertThrows(ParseException.class, "Unknown mark prefix '" + prefix + "'.", () ->
                    parser.parse(VALID_ARGUMENTS + " " + prefix + "value"));
        }
    }

    @Test
    public void parse_invalidIndex_reportsIndexConstraint() {
        for (String index : new String[]{"0", "-1", "S1", "2147483648"}) {
            assertThrows(ParseException.class, ParserUtil.MESSAGE_INVALID_INDEX, () ->
                    parser.parse(index + " lid/L12 d/2026-10-05 s/present"));
        }
    }

    @Test
    public void parse_invalidLessonDateOrStatus_reportsDomainConstraint() {
        assertThrows(ParseException.class, LessonId.MESSAGE_CONSTRAINTS, () ->
                parser.parse("2 lid/L01 d/2026-10-05 s/present"));
        for (String date : new String[]{"2025-02-29", "2026-02-30", "2026-04-31"}) {
            assertThrows(ParseException.class, Attendance.MESSAGE_DATE_CONSTRAINTS, () ->
                    parser.parse("2 lid/L12 d/" + date + " s/present"));
        }
        for (String status : new String[]{"unrecorded", "late", "present/absent"}) {
            assertThrows(ParseException.class, AttendanceStatus.MESSAGE_CONSTRAINTS, () ->
                    parser.parse("2 lid/L12 d/2026-10-05 s/" + status));
        }
    }

    @Test
    public void parse_lineBreakOrControlCharacter_reportsSingleLineMessageBeforeOtherErrors() {
        for (String control : new String[]{"\t", "\n", "\r", "\r\n", "\u0000", "\u001F", "\u007F",
            "\u0085", "\u2028", "\u2029"}) {
            for (String arguments : new String[]{control, control + VALID_ARGUMENTS,
                VALID_ARGUMENTS + control, "invalid unknown/value" + control, control + "0 x/value",
                "0 lid/L01" + control, "1 lid/L1" + control + "d/2026-10-05 s/present"}) {
                assertThrows(ParseException.class, AttendanceMarkParser.MESSAGE_SINGLE_LINE, () ->
                        parser.parse(arguments));
            }
        }
    }

    @Test
    public void parse_nullArguments_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
