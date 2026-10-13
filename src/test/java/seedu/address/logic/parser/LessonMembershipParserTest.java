package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.lesson.LessonId;

public class LessonMembershipParserTest {
    private final LessonMembershipParser parser = new LessonMembershipParser();

    @Test
    public void parse_validArguments_returnsCurrentIndexAndNormalizedLessonId() throws Exception {
        assertEquals(new LessonMembershipInput(Index.fromOneBased(2), new LessonId("L12")),
                parser.parse("2 lid/L12"));
        assertEquals(new LessonMembershipInput(Index.fromOneBased(3), new LessonId("L7")),
                parser.parse("  3\tlid/l7  "));
    }

    @Test
    public void parse_missingRepeatedUnknownOrMisorderedArguments_throwsUsage() {
        for (String arguments : new String[]{"", "1", "lid/L1", "lid/L1 1", "S1 lid/L1",
            "si/1 lid/L1", "1 LID/L1", "1 lesson/L1", "1 lid/L1 lid/L2", "1 lid/L1 extra"}) {
            assertThrows(ParseException.class, LessonMembershipParser.MESSAGE_ARGUMENTS, () -> parser.parse(
                    arguments));
        }
    }

    @Test
    public void parse_invalidIndex_throwsIndexConstraint() {
        assertThrows(ParseException.class, ParserUtil.MESSAGE_INVALID_INDEX, () -> parser.parse("0 lid/L1"));
        for (String arguments : new String[]{"-1 lid/L1", "2147483648 lid/L1"}) {
            assertThrows(ParseException.class, () -> parser.parse(arguments));
        }
    }

    @Test
    public void parse_invalidLessonId_throwsLessonIdConstraint() {
        for (String lessonId : new String[]{"L0", "L01", "S1", "L9223372036854775808"}) {
            assertThrows(ParseException.class, LessonId.MESSAGE_CONSTRAINTS, () -> parser.parse(
                    "1 lid/" + lessonId));
        }
    }

    @Test
    public void parse_lineBreaks_rejects() {
        for (String arguments : new String[]{"1 lid/L1\n", "1\nlid/L1", "1 lid/L1\r"}) {
            assertThrows(ParseException.class, LessonMembershipParser.MESSAGE_ARGUMENTS, () -> parser.parse(
                    arguments));
        }
    }

    @Test
    public void parse_nullArguments_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
