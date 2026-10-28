package ponhub.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ponhub.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import ponhub.logic.parser.exceptions.ParseException;

/**
 * Checks the audited command grammars' horizontal Unicode boundaries without changing field interiors.
 */
public class UnicodeCommandBoundaryTest {
    @Test
    public void parse_horizontalUnicodeSpaces_preservesSelectorsAndFilters() throws Exception {
        for (String space : new String[]{" ", "\u2003", "\u00a0", "\u202f"}) {
            assertEquals(new PeopleListParser().parse("r/student"),
                    new PeopleListParser().parse(space + "r/" + space + "student" + space));
            assertEquals(new PeopleListParser().parse(""), new PeopleListParser().parse(space));
            assertEquals(new LessonsArgumentsParser().parseInput("1 sort/schedule"),
                    new LessonsArgumentsParser().parseInput(space + "1" + space + "sort/" + space
                            + "schedule" + space));
            assertEquals(new HistoryArgumentsParser().parse("1"),
                    new HistoryArgumentsParser().parse(space + "1" + space));
            assertEquals(new HistoryArgumentsParser().parse("1 lid/L1 d/2026-10-05 s/absent"),
                    new HistoryArgumentsParser().parse(space + "1" + space + "lid/L1" + space
                            + "d/2026-10-05" + space + "s/absent" + space));
            assertEquals(new LessonMembershipParser().parse("1 lid/L1"),
                    new LessonMembershipParser().parse(space + "1" + space + "lid/L1" + space));
            assertEquals(new DeleteLessonParser().parse("lid/L1"),
                    new DeleteLessonParser().parse(space + "lid/L1" + space));
            assertEquals(new ShowLessonParser().parse("lid/L1 d/2026-10-05"),
                    new ShowLessonParser().parse(space + "lid/L1" + space + "d/2026-10-05" + space));
            assertEquals(new ShowLessonParser().parse("d/2026-10-05 lid/L1"),
                    new ShowLessonParser().parse(space + "d/2026-10-05" + space + "lid/L1" + space));
            assertEquals(ParserUtil.parseIndex("1"), ParserUtil.parseIndex(space + "1" + space));
        }
    }

    @Test
    public void parse_lineBreaksAndControls_remainInvalid() {
        for (String invalid : new String[]{"\n", "\r", "\u2028", "\u2029", "\u0000", "\u001b"}) {
            assertThrows(ParseException.class, () -> new PeopleListParser().parse(invalid + "r/student"));
            assertThrows(ParseException.class, () -> new LessonsArgumentsParser().parseInput(invalid + "1"));
            assertThrows(ParseException.class, () -> new HistoryArgumentsParser().parse(invalid + "1"));
            assertThrows(ParseException.class, () -> new LessonMembershipParser().parse(invalid + "1 lid/L1"));
            assertThrows(ParseException.class, () -> new DeleteLessonParser().parse(invalid + "lid/L1"));
            assertThrows(ParseException.class, () -> new ShowLessonParser().parse(invalid + "lid/L1"));
        }
    }
}
