package ponhub.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ponhub.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.lesson.LessonId;

public class DeleteLessonParserTest {
    private final DeleteLessonParser parser = new DeleteLessonParser();

    @Test
    public void parse_validArguments_returnsNormalizedLessonId() throws Exception {
        assertEquals(new LessonId("L12"), parser.parse("lid/L12"));
        assertEquals(new LessonId("L7"), parser.parse("  lid/l7\t"));
    }

    @Test
    public void parse_missingRepeatedUnknownOrExtraArguments_throwsUsage() {
        for (String arguments : new String[]{"", "L1", "1", "lid/", "lid/ L1", "LID/L1", "lesson/L1",
            "lid/L1 lid/L2", "lid/L1 extra", "extra lid/L1"}) {
            assertThrows(ParseException.class, DeleteLessonParser.MESSAGE_ARGUMENTS, () -> parser.parse(arguments));
        }
    }

    @Test
    public void parse_invalidLessonId_throwsLessonIdConstraint() {
        for (String lessonId : new String[]{"L0", "L01", "S1", "L9223372036854775808"}) {
            assertThrows(ParseException.class, LessonId.MESSAGE_CONSTRAINTS, () -> parser.parse(
                    "lid/" + lessonId));
        }
    }

    @Test
    public void parse_lineBreaks_rejects() {
        for (String arguments : new String[]{"lid/L1\n", "lid/\nL1", "lid/L1\r"}) {
            assertThrows(ParseException.class, DeleteLessonParser.MESSAGE_ARGUMENTS, () -> parser.parse(arguments));
        }
    }

    @Test
    public void parse_nullArguments_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
