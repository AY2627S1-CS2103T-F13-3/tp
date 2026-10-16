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
    private static final String VALID_ARGUMENTS = "2 lid/L12 d/2026-10-05";
    private static final AttendanceUnmarkInput VALID_INPUT = new AttendanceUnmarkInput(
            Index.fromOneBased(2), new LessonId("L12"), LocalDate.of(2026, 10, 5));

    private final AttendanceUnmarkParser parser = new AttendanceUnmarkParser();

    @Test
    public void parse_validArguments_returnsValidatedAttendanceInput() throws Exception {
        assertEquals(VALID_INPUT, parser.parse(" 2 lid/l12 d/2026-10-05 "));
        assertEquals(new AttendanceUnmarkInput(Index.fromOneBased(3), new LessonId("L7"),
                LocalDate.of(2024, 2, 29)), parser.parse("3 d/2024-02-29 lid/L7"));
    }

    @Test
    public void parse_prefixesInAnyOrder_returnsEquivalentInput() throws Exception {
        assertEquals(VALID_INPUT, parser.parse("2 lid/L12 d/2026-10-05"));
        assertEquals(VALID_INPUT, parser.parse("2 d/2026-10-05 lid/L12"));
    }

    @Test
    public void parse_unicodeBoundarySpaces_returnsEquivalentInput() throws Exception {
        for (String space : new String[]{"\u2003", "\u2002", "\u3000", "\u00A0", "\u202F"}) {
            String arguments = space + "2" + space + "lid/" + space + "L12"
                    + space + "d/" + space + "2026-10-05" + space;
            assertEquals(VALID_INPUT, parser.parse(arguments));
        }
    }

    @Test
    public void parse_missingIndex_reportsArgumentsMessage() {
        for (String arguments : new String[]{"", "1", "lid/L12 d/2026-10-05"}) {
            assertThrows(ParseException.class, AttendanceUnmarkParser.MESSAGE_ARGUMENTS, () ->
                    parser.parse(arguments));
        }
    }

    @Test
    public void parse_missingRequiredPrefix_reportsMissingPrefix() {
        String[][] cases = {
            {"2 d/2026-10-05", "lid/"},
            {"2 lid/L12", "d/"}
        };
        for (String[] testCase : cases) {
            assertThrows(ParseException.class,
                    "Missing required unmark prefix '" + testCase[1] + "'.", () -> parser.parse(testCase[0]));
        }
    }

    @Test
    public void parse_blankValues_reportsBlankPrefix() {
        String[][] cases = {
            {"2 lid/ d/2026-10-05", "lid/"},
            {"2 lid/L12 d/", "d/"},
            {"2 lid/\u00A0d/2026-10-05", "lid/"}
        };
        for (String[] testCase : cases) {
            assertThrows(ParseException.class,
                    "Unmark prefix '" + testCase[1] + "' requires a non-blank value.", () ->
                            parser.parse(testCase[0]));
        }
    }

    @Test
    public void parse_repeatedPrefixes_rejectsEqualAndDifferentValues() {
        String[][] fields = {
            {"lid/", "L12", "L7"},
            {"d/", "2026-10-05", "2026-10-12"}
        };
        for (String[] field : fields) {
            for (String value : new String[]{field[1], field[2]}) {
                assertThrows(ParseException.class, "Repeated unmark prefix '" + field[0] + "'.", () ->
                        parser.parse(VALID_ARGUMENTS + " " + field[0] + value));
            }
        }
    }

    @Test
    public void parse_unknownStatusAndUppercasePrefixes_reportsUnknownPrefix() {
        for (String prefix : new String[]{"s/", "lesson/", "date/", "LID/", "D/"}) {
            assertThrows(ParseException.class, "Unknown unmark prefix '" + prefix + "'.", () ->
                    parser.parse(VALID_ARGUMENTS + " " + prefix + "value"));
        }
    }

    @Test
    public void parse_invalidIndex_reportsIndexConstraint() {
        for (String index : new String[]{"0", "-1", "S1", "2147483648"}) {
            assertThrows(ParseException.class, ParserUtil.MESSAGE_INVALID_INDEX, () ->
                    parser.parse(index + " lid/L12 d/2026-10-05"));
        }
    }

    @Test
    public void parse_invalidLessonOrDate_reportsDomainConstraint() {
        assertThrows(ParseException.class, LessonId.MESSAGE_CONSTRAINTS, () ->
                parser.parse("2 lid/L01 d/2026-10-05"));
        for (String date : new String[]{"2025-02-29", "2026-02-30", "2026-04-31"}) {
            assertThrows(ParseException.class, Attendance.MESSAGE_DATE_CONSTRAINTS, () ->
                    parser.parse("2 lid/L12 d/" + date));
        }
    }

    @Test
    public void parse_lineBreakOrControlCharacter_reportsSingleLineMessageBeforeOtherErrors() {
        for (String control : new String[]{"\t", "\n", "\r", "\r\n", "\u0000", "\u001F", "\u007F",
            "\u0085", "\u2028", "\u2029"}) {
            for (String arguments : new String[]{control, control + VALID_ARGUMENTS,
                VALID_ARGUMENTS + control, "invalid unknown/value" + control}) {
                assertThrows(ParseException.class, AttendanceUnmarkParser.MESSAGE_SINGLE_LINE, () ->
                        parser.parse(arguments));
            }
        }
    }

    @Test
    public void parse_nullArguments_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
