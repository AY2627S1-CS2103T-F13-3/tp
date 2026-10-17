package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.parser.ParserUtil.MESSAGE_INVALID_INDEX;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import org.junit.jupiter.api.Test;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.Address;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Phone;

public class ParserUtilTest {
    private static final String INVALID_NAME = "R@chel";
    private static final String INVALID_PHONE = "+651234";
    private static final String INVALID_ADDRESS = " ";
    private static final String INVALID_EMAIL = "example.com";

    private static final String VALID_NAME = "Rachel Walker";
    private static final String VALID_PHONE = "123456";
    private static final String VALID_ADDRESS = "123 Main Street #0505";
    private static final String VALID_EMAIL = "rachel@example.com";

    private static final String WHITESPACE = " \t\r\n";

    @Test
    public void stripUnicodeBoundaryWhitespace_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.stripUnicodeBoundaryWhitespace(null));
    }

    @Test
    public void stripUnicodeBoundaryWhitespace_emptyAndBlank_returnsEmptyString() {
        for (String value : new String[]{"", " ", "\u00A0", "\u202F", "\u3000", " \u2002\u2003\u2007"}) {
            assertEquals("", ParserUtil.stripUnicodeBoundaryWhitespace(value));
        }
    }

    @Test
    public void stripUnicodeBoundaryWhitespace_boundarySpaces_removesOnlyBoundaries() {
        for (String space : new String[]{" ", "\u00A0", "\u202F", "\u3000", "\u2002", "\u2003", "\u2007"}) {
            assertEquals("value", ParserUtil.stripUnicodeBoundaryWhitespace(space + "value"));
            assertEquals("value", ParserUtil.stripUnicodeBoundaryWhitespace("value" + space));
            assertEquals("value", ParserUtil.stripUnicodeBoundaryWhitespace(space + "value" + space));
        }
        assertEquals("value", ParserUtil.stripUnicodeBoundaryWhitespace(" \u00A0value\u202F\u3000"));
    }

    @Test
    public void stripUnicodeBoundaryWhitespace_internalText_preservesExactly() {
        String value = "Block  1\u00A0Unit\u202F2\u3000A\tB\nC";
        assertEquals(value, ParserUtil.stripUnicodeBoundaryWhitespace(value));
        assertEquals(value, ParserUtil.stripUnicodeBoundaryWhitespace("\u3000" + value + "\u00A0"));
    }

    @Test
    public void stripUnicodeBoundaryWhitespace_supplementaryCharacters_preservesSurrogatePairs() {
        String value = "\uD801\uDC00  \uD83D\uDE00";
        assertEquals(value, ParserUtil.stripUnicodeBoundaryWhitespace(value));
        assertEquals(value, ParserUtil.stripUnicodeBoundaryWhitespace("\u00A0" + value + "\u3000"));
    }

    @Test
    public void stripUnicodeBoundaryWhitespace_otherCharacters_stopsTrimming() {
        for (String boundary : new String[]{"\u0000", "\u007F", "\u0085", "\u200B", "\uFEFF"}) {
            String value = boundary + " value " + boundary;
            assertEquals(value, ParserUtil.stripUnicodeBoundaryWhitespace(" " + value + " "));
        }
    }

    @Test
    public void stripUnicodeBoundaryWhitespace_whitespaceControls_trimsWithoutValidating() {
        String controls = "\t\n\r\u001F\u2028\u2029";
        assertEquals("value", ParserUtil.stripUnicodeBoundaryWhitespace(controls + "value" + controls));
    }

    @Test
    public void parseIndex_invalidInput_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseIndex("10 a"));
    }

    @Test
    public void parseIndex_outOfRangeInput_throwsParseException() {
        assertThrows(ParseException.class, MESSAGE_INVALID_INDEX, ()
            -> ParserUtil.parseIndex(Long.toString(Integer.MAX_VALUE + 1)));
    }

    @Test
    public void parseIndex_validInput_success() throws Exception {
        // No whitespaces
        assertEquals(INDEX_FIRST_PERSON, ParserUtil.parseIndex("1"));

        // Leading and trailing whitespaces
        assertEquals(INDEX_FIRST_PERSON, ParserUtil.parseIndex("  1  "));
    }

    @Test
    public void parseName_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseName((String) null));
    }

    @Test
    public void parseName_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseName(INVALID_NAME));
    }

    @Test
    public void parseName_validValueWithoutWhitespace_returnsName() throws Exception {
        Name expectedName = new Name(VALID_NAME);
        assertEquals(expectedName, ParserUtil.parseName(VALID_NAME));
    }

    @Test
    public void parseName_validValueWithWhitespace_returnsTrimmedName() throws Exception {
        String nameWithWhitespace = "  Rachel   Walker  ";
        Name expectedName = new Name(VALID_NAME);
        assertEquals(expectedName, ParserUtil.parseName(nameWithWhitespace));
    }

    @Test
    public void parseName_controlCharacters_throwsParseException() {
        String[] invalidNames = {"\t" + VALID_NAME, VALID_NAME + "\n", VALID_NAME + "\r", VALID_NAME + "\0",
            "Rachel\tWalker", "A".repeat(101)};
        for (String invalidName : invalidNames) {
            assertThrows(ParseException.class, Name.MESSAGE_CONSTRAINTS, () -> ParserUtil.parseName(invalidName));
        }
    }

    @Test
    public void parsePhone_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parsePhone((String) null));
    }

    @Test
    public void parsePhone_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parsePhone(INVALID_PHONE));
    }

    @Test
    public void parsePhone_validValueWithoutWhitespace_returnsPhone() throws Exception {
        Phone expectedPhone = new Phone(VALID_PHONE);
        assertEquals(expectedPhone, ParserUtil.parsePhone(VALID_PHONE));
    }

    @Test
    public void parsePhone_validValueWithWhitespace_returnsTrimmedPhone() throws Exception {
        String phoneWithWhitespace = WHITESPACE + VALID_PHONE + WHITESPACE;
        Phone expectedPhone = new Phone(VALID_PHONE);
        assertEquals(expectedPhone, ParserUtil.parsePhone(phoneWithWhitespace));
    }

    @Test
    public void parseAddress_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseAddress((String) null));
    }

    @Test
    public void parseAddress_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseAddress(INVALID_ADDRESS));
    }

    @Test
    public void parseAddress_validValueWithoutWhitespace_returnsAddress() throws Exception {
        Address expectedAddress = new Address(VALID_ADDRESS);
        assertEquals(expectedAddress, ParserUtil.parseAddress(VALID_ADDRESS));
    }

    @Test
    public void parseAddress_validValueWithWhitespace_returnsTrimmedAddress() throws Exception {
        String addressWithWhitespace = "  123   Main Street #0505  ";
        Address expectedAddress = new Address(VALID_ADDRESS);
        assertEquals(expectedAddress, ParserUtil.parseAddress(addressWithWhitespace));
    }

    @Test
    public void parseAddress_controlCharacters_throwsParseException() {
        String[] invalidAddresses = {"\t" + VALID_ADDRESS, VALID_ADDRESS + "\n", VALID_ADDRESS + "\r",
            VALID_ADDRESS + "\0", "123\tMain Street", "Blk 12/34", "A".repeat(201)};
        for (String invalidAddress : invalidAddresses) {
            assertThrows(ParseException.class, Address.MESSAGE_CONSTRAINTS, ()
                    -> ParserUtil.parseAddress(invalidAddress));
        }
    }

    @Test
    public void parseEmail_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseEmail((String) null));
    }

    @Test
    public void parseEmail_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseEmail(INVALID_EMAIL));
    }

    @Test
    public void parseEmail_validValueWithoutWhitespace_returnsEmail() throws Exception {
        Email expectedEmail = new Email(VALID_EMAIL);
        assertEquals(expectedEmail, ParserUtil.parseEmail(VALID_EMAIL));
    }

    @Test
    public void parseEmail_validValueWithWhitespace_returnsTrimmedEmail() throws Exception {
        String emailWithWhitespace = WHITESPACE + VALID_EMAIL + WHITESPACE;
        Email expectedEmail = new Email(VALID_EMAIL);
        assertEquals(expectedEmail, ParserUtil.parseEmail(emailWithWhitespace));
    }
}
