package ponhub.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import ponhub.logic.parser.PersonEditInput.Field;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.person.Email;
import ponhub.model.person.Name;
import ponhub.model.person.Phone;

public class PersonEditParserTest {
    private final PersonEditParser parser = new PersonEditParser();

    @Test
    public void parse_partialAndClear_preservesDistinctOperations() throws Exception {
        var parsed = parser.parse(" 2 n/ Alex   Tan e/ p/00123456 a/ ");
        assertEquals(2, parsed.index().getOneBased());
        assertEquals(Field.set(new Name("Alex Tan")), parsed.edit().name());
        assertEquals(Field.set(new Phone("00123456")), parsed.edit().phone());
        assertEquals(Field.clear(), parsed.edit().email());
        assertEquals(Field.clear(), parsed.edit().address());
        assertEquals(Field.keep(), parsed.edit().level());
        assertEquals(Field.keep(), parsed.edit().parentPhone());
        assertEquals(Field.clear(), parser.parse("1 pp/").edit().parentPhone());
    }

    @Test
    public void parse_unicodeBoundariesAndOrder_returnsEquivalentIntent() throws Exception {
        for (String space : List.of("\u2003", "\u00a0", "\u202f", "\u3000")) {
            assertEquals(parser.parse("1 l/P2 e/ pp/001"),
                    parser.parse(space + "1" + space + "pp/001" + space + "e/" + space + "l/p2" + space));
            assertThrows(ParseException.class, () -> parser.parse("1 a/Block" + space + "x/value"));
        }
    }

    @Test
    public void parse_invalidSelectorsOrMissingFields_rejects() {
        for (String input : List.of("", "1", "0 n/Alex", "-1 n/Alex", "+1 n/Alex", "S1 n/Alex",
                "2147483648 n/Alex", "1 2 n/Alex", "n/Alex", "1 n /Alex", "1 nAlex")) {
            assertThrows(ParseException.class, () -> parser.parse(input), input);
        }
    }

    @Test
    public void parse_repeatedUnknownAndImmutablePrefixes_rejects() {
        for (String prefix : List.of("n/", "p/", "e/", "a/", "l/", "pp/")) {
            assertThrows(ParseException.class, () -> parser.parse("1 " + prefix + " " + prefix));
        }
        for (String prefix : List.of("r/", "id/", "sid/", "x/", "N/", "PP/")) {
            assertThrows(ParseException.class, () -> parser.parse("1 a/Block " + prefix + "value"));
        }
    }

    @Test
    public void parse_invalidFieldsAndControls_rejects() throws Exception {
        for (String field : List.of("n/@Alex", "p/12", "p/1234567890123456", "p/+123", "pp/12",
                "pp/1234567890123456", "e/invalid", "l/P7", "l/JC3")) {
            assertThrows(ParseException.class, () -> parser.parse("1 " + field), field);
        }
        for (String control : List.of("\t", "\n", "\r", "\u0000", "\u0085", "\u2028", "\u2029")) {
            assertThrows(ParseException.class, () -> parser.parse("1 a/Block" + control));
        }
        for (String phone : List.of("001", "000123456789012")) {
            assertEquals(Field.set(new Phone(phone)), parser.parse("1 pp/" + phone).edit().parentPhone());
        }
    }

    @Test
    public void parse_phoneBounds_preservesOwnAndParentNumbers() throws Exception {
        for (String phone : List.of("001", "000123456789012")) {
            var parsed = parser.parse("1 p/" + phone + " pp/" + phone);
            assertEquals(Field.set(new Phone(phone)), parsed.edit().phone());
            assertEquals(Field.set(new Phone(phone)), parsed.edit().parentPhone());
        }
    }

    @Test
    public void parse_invalidPhoneValues_preservesOwnAndParentErrorText() {
        String lengthMessage = "Contact phone numbers must contain between 3 and 15 digits.";
        String phoneMessage = "Phone numbers must contain 3 to 15 digits, without spaces or punctuation.";
        String[][] cases = {
            {"1234567890123456", lengthMessage}, {"123456789012345x", lengthMessage},
            {"12", phoneMessage}, {"+123", phoneMessage}, {"\uff11\uff12\uff13", phoneMessage}
        };
        for (String[] testCase : cases) {
            for (String prefix : List.of("p/", "pp/")) {
                assertEquals(testCase[1], assertThrows(ParseException.class, () ->
                        parser.parse("1 " + prefix + testCase[0])).getMessage());
            }
        }
    }

    @Test
    public void parse_phoneAndOtherInvalidFields_preservesValidationOrder() {
        String longPhone = "1234567890123456";
        assertEquals(Name.MESSAGE_CONSTRAINTS, assertThrows(ParseException.class, () ->
                parser.parse("1 pp/" + longPhone + " p/" + longPhone + " n/@Alex")).getMessage());
        assertEquals("Contact phone numbers must contain between 3 and 15 digits.",
                assertThrows(ParseException.class, () ->
                        parser.parse("1 pp/12 e/invalid p/" + longPhone)).getMessage());
        assertEquals(Email.MESSAGE_CONSTRAINTS, assertThrows(ParseException.class, () ->
                parser.parse("1 pp/" + longPhone + " e/invalid")).getMessage());
        assertEquals("Repeated edit prefix 'p/'.", assertThrows(ParseException.class, () ->
                parser.parse("1 p/" + longPhone + " p/001")).getMessage());
    }
}
