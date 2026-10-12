package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.lesson.LessonDay;
import seedu.address.model.lesson.LessonTime;
import seedu.address.model.lesson.LessonTimeSlot;
import seedu.address.model.lesson.Room;
import seedu.address.model.lesson.Subject;
import seedu.address.model.person.Name;
import seedu.address.model.person.Phone;

public class AddLessonParserTest {
    private static final String REQUIRED_ARGUMENTS =
            "d/Mon st/0900 et/1030 s/Primary Math tu/Mei Lim rm/r1";

    private final AddLessonParser parser = new AddLessonParser();

    @Test
    public void parse_requiredFields_returnsNormalizedInputWithoutPhone() throws Exception {
        LessonCreationInput expected = new LessonCreationInput(
                new LessonTimeSlot(LessonDay.MONDAY, new LessonTime("0900"), new LessonTime("1030")),
                new Subject("Primary Math"), new Name("Mei Lim"), Optional.empty(), new Room("R1"));

        assertEquals(expected, parser.parse(REQUIRED_ARGUMENTS));
    }

    @Test
    public void parse_reorderedMixedCaseValuesAndPhone_preservesExactPhone() throws Exception {
        LessonCreationInput input = parser.parse(
                "tp/00123456 rm/room2 tu/mei   LIM s/English  Language et/1730 d/fRi st/1600");

        assertEquals(LessonDay.FRIDAY, input.timeSlot().getDay());
        assertEquals(new LessonTime("1600"), input.timeSlot().getStartTime());
        assertEquals(new LessonTime("1730"), input.timeSlot().getEndTime());
        assertEquals(new Subject("English Language"), input.subject());
        assertEquals(new Name("mei LIM"), input.tutorName());
        assertEquals(Optional.of(new Phone("00123456")), input.tutorPhone());
        assertEquals(new Room("ROOM2"), input.room());
    }

    @Test
    public void parse_unicodeBoundarySpaces_returnsEquivalentInput() throws Exception {
        for (String space : new String[]{"\u2003", "\u2002", "\u3000", "\u00A0", "\u202F"}) {
            String arguments = space + "d/" + space + "Mon" + space + "st/" + space + "0900"
                    + space + "et/" + space + "1030" + space + "s/" + space + "Primary Math"
                    + space + "tu/" + space + "Mei Lim" + space + "rm/" + space + "r1" + space;
            assertEquals(parser.parse(REQUIRED_ARGUMENTS), parser.parse(arguments));
        }
    }

    @Test
    public void parse_missingRequiredPrefixes_reportsMissingField() {
        String[][] cases = {
            {"", "d/"},
            {"st/0900 et/1030 s/Math tu/Mei Lim rm/R1", "d/"},
            {"d/Mon et/1030 s/Math tu/Mei Lim rm/R1", "st/"},
            {"d/Mon st/0900 s/Math tu/Mei Lim rm/R1", "et/"},
            {"d/Mon st/0900 et/1030 tu/Mei Lim rm/R1", "s/"},
            {"d/Mon st/0900 et/1030 s/Math rm/R1", "tu/"},
            {"d/Mon st/0900 et/1030 s/Math tu/Mei Lim", "rm/"}
        };
        for (String[] testCase : cases) {
            assertThrows(ParseException.class,
                    "Missing required addlesson prefix '" + testCase[1] + "'.", () ->
                            parser.parse(testCase[0]));
        }
    }

    @Test
    public void parse_blankValues_reportsBlankField() {
        String[][] cases = {
            {"d/ st/0900 et/1030 s/Math tu/Mei Lim rm/R1", "d/"},
            {"d/Mon st/ et/1030 s/Math tu/Mei Lim rm/R1", "st/"},
            {"d/Mon st/0900 et/ s/Math tu/Mei Lim rm/R1", "et/"},
            {"d/Mon st/0900 et/1030 s/ tu/Mei Lim rm/R1", "s/"},
            {"d/Mon st/0900 et/1030 s/Math tu/ rm/R1", "tu/"},
            {REQUIRED_ARGUMENTS + " tp/   ", "tp/"},
            {"d/Mon st/0900 et/1030 s/Math tu/Mei Lim rm/\u2003", "rm/"}
        };
        for (String[] testCase : cases) {
            assertThrows(ParseException.class,
                    "Addlesson prefix '" + testCase[1] + "' requires a non-blank value.", () ->
                            parser.parse(testCase[0]));
        }
    }

    @Test
    public void parse_repeatedPrefixes_rejectsEqualAndDifferentValues() {
        String[][] fields = {
            {"d/", "Mon"}, {"st/", "0900"}, {"et/", "1030"}, {"s/", "Primary Math"},
            {"tu/", "Mei Lim"}, {"tp/", "00123456"}, {"rm/", "R1"}
        };
        String complete = REQUIRED_ARGUMENTS + " tp/00123456";
        for (String[] field : fields) {
            for (String value : new String[]{field[1], "other"}) {
                assertThrows(ParseException.class, "Repeated addlesson prefix '" + field[0] + "'.", () ->
                        parser.parse(complete + " " + field[0] + value));
            }
        }
    }

    @Test
    public void parse_unknownUppercaseAndMalformedPrefixes_rejects() {
        for (String prefix : new String[]{"x/", "day/", "D/", "ST/", "TU/", "RM/"}) {
            assertThrows(ParseException.class, "Unknown addlesson prefix '" + prefix + "'.", () ->
                    parser.parse(REQUIRED_ARGUMENTS + " " + prefix + "value"));
        }
        for (String input : new String[]{"not-a-prefix", "lesson " + REQUIRED_ARGUMENTS,
            "addlesson " + REQUIRED_ARGUMENTS, "d /Mon st/0900 et/1030 s/Math tu/Mei Lim rm/R1"}) {
            assertThrows(ParseException.class, AddLessonParser.MESSAGE_PREAMBLE, () -> parser.parse(input));
        }
    }

    @Test
    public void parse_invalidDomainValues_throwsParseException() {
        String[][] replacements = {
            {"d/Mon", "d/Someday"},
            {"st/0900", "st/900"},
            {"st/0900", "st/2400"},
            {"et/1030", "et/0900"},
            {"s/Primary Math", "s/Math!"},
            {"tu/Mei Lim", "tu/123"},
            {"rm/r1", "rm/bad room"}
        };
        for (String[] replacement : replacements) {
            assertThrows(ParseException.class, () ->
                    parser.parse(REQUIRED_ARGUMENTS.replace(replacement[0], replacement[1])));
        }
        for (String phone : new String[]{"00", "001 234", "+001234", "0001234567890123"}) {
            assertThrows(ParseException.class, () -> parser.parse(REQUIRED_ARGUMENTS + " tp/" + phone));
        }
    }

    @Test
    public void parse_controlsAndLineBreaks_rejectsBeforeTrimming() {
        for (String control : new String[]{"\t", "\n", "\r", "\r\n", "\u0000", "\u001F", "\u007F",
            "\u0085", "\u2028", "\u2029"}) {
            for (String input : new String[]{control, control + REQUIRED_ARGUMENTS, REQUIRED_ARGUMENTS + control,
                REQUIRED_ARGUMENTS + " s/Math" + control}) {
                assertThrows(ParseException.class, AddLessonParser.MESSAGE_SINGLE_LINE, () -> parser.parse(input));
            }
        }
    }
}
