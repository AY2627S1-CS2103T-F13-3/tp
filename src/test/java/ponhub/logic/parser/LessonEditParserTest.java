package ponhub.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ponhub.testutil.Assert.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.lesson.LessonDay;
import ponhub.model.lesson.LessonId;
import ponhub.model.lesson.LessonTime;
import ponhub.model.lesson.Room;
import ponhub.model.lesson.Subject;
import ponhub.model.person.Name;
import ponhub.model.person.Phone;

public class LessonEditParserTest {
    private final LessonEditParser parser = new LessonEditParser();

    @Test
    public void parse_eachSingleEditableField_retainsAllOtherFields() throws Exception {
        Object[][] cases = {
            {"d/tue", LessonDay.TUESDAY},
            {"st/0930", new LessonTime("0930")},
            {"et/1100", new LessonTime("1100")},
            {"s/Advanced Math", new Subject("Advanced Math")},
            {"tu/Mei Lim", new Name("Mei Lim")},
            {"rm/room2", new Room("ROOM2")}
        };
        for (int index = 0; index < cases.length; index++) {
            LessonEditParser.Input input = parser.parse("lid/l12 " + cases[index][0]);
            assertEquals(new LessonId("L12"), input.lessonId());
            assertEquals(index == 0 ? Optional.of(cases[index][1]) : Optional.empty(), input.day());
            assertEquals(index == 1 ? Optional.of(cases[index][1]) : Optional.empty(), input.startTime());
            assertEquals(index == 2 ? Optional.of(cases[index][1]) : Optional.empty(), input.endTime());
            assertEquals(index == 3 ? Optional.of(cases[index][1]) : Optional.empty(), input.subject());
            assertEquals(index == 4 ? Optional.of(cases[index][1]) : Optional.empty(), input.tutorName());
            assertEquals(index == 5 ? Optional.of(cases[index][1]) : Optional.empty(), input.room());
            assertEquals(Optional.empty(), input.tutorPhone());
        }
    }

    @Test
    public void parse_reorderedAllFieldsAndUnicodeBoundaries_returnsNormalizedInput() throws Exception {
        String space = "\u2003";
        LessonEditParser.Input input = parser.parse(space + "tp/00123456" + space + "rm/r2" + space
                + "tu/mei   LIM" + space + "s/English  Language" + space + "et/1730" + space
                + "lid/l7" + space + "d/fRi" + space + "st/1600" + space);

        assertEquals(new LessonId("L7"), input.lessonId());
        assertEquals(Optional.of(LessonDay.FRIDAY), input.day());
        assertEquals(Optional.of(new LessonTime("1600")), input.startTime());
        assertEquals(Optional.of(new LessonTime("1730")), input.endTime());
        assertEquals(Optional.of(new Subject("English Language")), input.subject());
        assertEquals(Optional.of(new Name("mei LIM")), input.tutorName());
        assertEquals(Optional.of(new Phone("00123456")), input.tutorPhone());
        assertEquals(Optional.of(new Room("R2")), input.room());
    }

    @Test
    public void parse_missingSelectorOrEditField_rejects() {
        for (String arguments : new String[]{"", "lid/L1", "d/Mon", "L1 s/Math", "editlesson lid/L1 s/Math"}) {
            assertThrows(ParseException.class, LessonEditParser.MESSAGE_ARGUMENTS, () -> parser.parse(arguments));
        }
    }

    @Test
    public void parse_blankRepeatedUnknownAndOrphanPhone_rejects() {
        String[][] fields = {
            {"lid/", "L1"}, {"d/", "Mon"}, {"st/", "0900"}, {"et/", "1000"},
            {"s/", "Math"}, {"tu/", "Mei Lim"}, {"tp/", "00123456"}, {"rm/", "R1"}
        };
        for (String[] field : fields) {
            String prefix = field[0];
            assertThrows(ParseException.class,
                    "Editlesson prefix '" + prefix + "' requires a non-blank value.", () ->
                            parser.parse(prefix.equals("lid/") ? "s/Math lid/" : "lid/L1 " + prefix));
            assertThrows(ParseException.class, "Repeated editlesson prefix '" + prefix + "'.", () ->
                    parser.parse((prefix.equals("lid/") ? "s/Math " : "lid/L1 s/Math ")
                            + prefix + field[1] + " " + prefix + "value"));
        }
        for (String prefix : new String[]{"x/", "D/", "LID/", "subject/"}) {
            assertThrows(ParseException.class, "Unknown editlesson prefix '" + prefix + "'.", () ->
                    parser.parse("lid/L1 s/Math " + prefix + "value"));
        }
        assertThrows(ParseException.class, LessonEditParser.MESSAGE_TUTOR_PHONE, () ->
                parser.parse("lid/L1 tp/00123456"));
    }

    @Test
    public void parse_invalidDomainValues_rejects() throws Exception {
        for (String field : new String[]{"lid/L0", "d/Someday", "st/900", "et/2400", "s/Math!", "tu/123",
            "tu/Mei Lim tp/12", "rm/bad room"}) {
            assertThrows(ParseException.class, () -> parser.parse("lid/L1 s/Math " + field));
        }
        // Each endpoint is valid on its own; the staged candidate checks it against the retained endpoint.
        assertEquals(Optional.of(new LessonTime("2300")), parser.parse("lid/L1 st/2300").startTime());
    }

    @Test
    public void parse_controlsAndLineBreaks_rejectsBeforeTrimming() {
        for (String control : new String[]{"\t", "\n", "\r", "\r\n", "\u0000", "\u001F", "\u007F",
            "\u0085", "\u2028", "\u2029"}) {
            for (String arguments : new String[]{control, control + "lid/L1 s/Math",
                "lid/L1 s/Math" + control, "lid/L1 s/Math" + control + "rm/R1"}) {
                assertThrows(ParseException.class, LessonEditParser.MESSAGE_SINGLE_LINE, () ->
                        parser.parse(arguments));
            }
        }
    }
}
