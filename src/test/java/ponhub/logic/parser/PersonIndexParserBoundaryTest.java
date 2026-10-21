package ponhub.logic.parser;

import static ponhub.testutil.Assert.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import ponhub.logic.parser.exceptions.ParseException;

/**
 * Preserves the active index consumers' own syntax and control-error precedence.
 */
public class PersonIndexParserBoundaryTest {
    @Test
    public void parse_controlBearingSelectors_preservesConsumerFeedback() {
        for (String control : List.of("\0", "\b", "\u001b", "\u007f", "\u0080", "\u009f")) {
            for (String index : List.of(control + "1", "1" + control, "1" + control + "2")) {
                assertThrows(ParseException.class, LessonMembershipParser.MESSAGE_ARGUMENTS, () ->
                        new LessonMembershipParser().parse(index + " lid/L10"));
                assertThrows(ParseException.class, LessonsArgumentsParser.MESSAGE_USAGE, () ->
                        new LessonsArgumentsParser().parse("si/" + index));
                assertThrows(ParseException.class, HistoryArgumentsParser.MESSAGE_USAGE, () ->
                        new HistoryArgumentsParser().parse(index + " lid/L10"));
                assertThrows(ParseException.class, AttendanceMarkParser.MESSAGE_SINGLE_LINE, () ->
                        new AttendanceMarkParser().parse(index + " lid/L10 d/2026-10-05 s/present"));
                assertThrows(ParseException.class, AttendanceUnmarkParser.MESSAGE_SINGLE_LINE, () ->
                        new AttendanceUnmarkParser().parse(index + " lid/L10 d/2026-10-05"));
            }
        }
    }

    @Test
    public void parse_invalidPositiveRange_keepsIndexFeedbackBeforeOtherValues() {
        for (String index : List.of("0", "2147483648", "999999999999999999999")) {
            assertThrows(ParseException.class, ParserUtil.MESSAGE_INVALID_INDEX, () ->
                    new LessonMembershipParser().parse(index + " lid/L0"));
            assertThrows(ParseException.class, ParserUtil.MESSAGE_INVALID_INDEX, () ->
                    new LessonsArgumentsParser().parse("si/" + index));
            assertThrows(ParseException.class, ParserUtil.MESSAGE_INVALID_INDEX, () ->
                    new HistoryArgumentsParser().parse(index + " lid/L0"));
            assertThrows(ParseException.class, ParserUtil.MESSAGE_INVALID_INDEX, () ->
                    new AttendanceMarkParser().parse(index + " lid/L0 d/invalid s/invalid"));
            assertThrows(ParseException.class, ParserUtil.MESSAGE_INVALID_INDEX, () ->
                    new AttendanceUnmarkParser().parse(index + " lid/L0 d/invalid"));
        }
    }
}
