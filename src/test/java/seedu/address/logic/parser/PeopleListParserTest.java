package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.PersonRole;

public class PeopleListParserTest {

    private final PeopleListParser parser = new PeopleListParser();

    @Test
    public void parse_omittedRole_returnsAllRoles() throws Exception {
        for (String args : new String[]{"", " ", "   ", "\t", " \t  "}) {
            assertEquals(Optional.empty(), parser.parse(args));
        }
    }

    @Test
    public void parse_singularRoles_acceptsEveryRoleIgnoringCase() throws Exception {
        assertEquals(Optional.of(PersonRole.STUDENT), parser.parse("r/student"));
        assertEquals(Optional.of(PersonRole.TUTOR), parser.parse("r/TUTOR"));
        assertEquals(Optional.of(PersonRole.PARENT), parser.parse("r/PaReNt"));
        assertEquals(Optional.of(PersonRole.STUDENT), parser.parse(" \t r/StUdEnT  \t"));
        assertEquals(Optional.of(PersonRole.TUTOR), parser.parse("r/  \t TuToR"));
    }

    @Test
    public void parse_invalidRoleOrBlankValue_throwsParseException() {
        String[] inputs = {"r/", "r/ ", "r/\t", "r/all", "r/students", "r/tutors", "r/parents",
            "r/contact", "r/1", "r/student/tutor"};
        for (String input : inputs) {
            assertThrows(ParseException.class, () -> parser.parse(input));
        }
    }

    @Test
    public void parse_malformedPrefixPreambleOrExtraArguments_throwsParseException() {
        String[] inputs = {"student", "list", "list r/student", "anything r/student", "R/student",
            "role/student", "r /student", "n/Alex", "r/student extra", "r/student r/student",
            "r/student r/tutor", "r/student n/Alex", "r/student t/friends", "r/student xyz/value"};
        for (String input : inputs) {
            assertThrows(ParseException.class, () -> parser.parse(input));
        }
    }

    @Test
    public void parse_lineBreaks_throwsParseExceptionBeforeOmittedRoleHandling() {
        for (String lineBreak : new String[]{"\n", "\r", "\r\n", "\u2028", "\u2029"}) {
            assertThrows(ParseException.class, () -> parser.parse(lineBreak));
            assertThrows(ParseException.class, () -> parser.parse("r/student" + lineBreak));
            assertThrows(ParseException.class, () -> parser.parse("r/" + lineBreak + "student"));
        }
    }

    @Test
    public void parse_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }

    @Test
    public void parseCommand_roleListing_remainsDormantInLegacyRuntime() throws Exception {
        AddressBookParser commandParser = new AddressBookParser();
        assertTrue(commandParser.parseCommand("list") instanceof ListCommand);
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, ListCommand.MESSAGE_USAGE);
        assertThrows(ParseException.class, expectedMessage, () -> commandParser.parseCommand("list r/student"));
    }
}
