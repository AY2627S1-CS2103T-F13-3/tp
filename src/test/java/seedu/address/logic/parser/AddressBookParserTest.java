package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.commands.ExitCommand;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.Address;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;
import seedu.address.testutil.PersonUtil;

public class AddressBookParserTest {

    private final AddressBookParser parser = new AddressBookParser();

    @Test
    public void parseCommand_add() throws Exception {
        Person person = new PersonBuilder().build();
        AddCommand command = (AddCommand) parser.parseCommand(PersonUtil.getAddCommand(person));
        assertEquals(new AddCommand(person), command);
    }

    @Test
    public void parseCommand_normalizedContactDetails_success() throws Exception {
        Person person = new PersonBuilder().withName("Anne-Marie O'Neill Jr.").withPhone("00123456")
                .withEmail("Anne+School@Example.COM").withAddress("Blk 456, Den Road, #01-355").withTags().build();
        String contactDetails = " n/  Anne-Marie   O'Neill Jr.  p/00123456 e/Anne+School@Example.COM"
                + " a/  Blk 456,   Den Road, #01-355  ";

        assertEquals(new AddCommand(person), parser.parseCommand("add" + contactDetails));
    }

    @Test
    public void parseCommand_nameBoundaryControls_throwsParseException() {
        String[] invalidNames = {"\tAnne-Marie", "Anne-Marie\n", "Anne-Marie\r", "Anne-Marie\0"};
        for (String invalidName : invalidNames) {
            assertThrows(ParseException.class, Name.MESSAGE_CONSTRAINTS, ()
                    -> parser.parseCommand("add p/00123456 e/anne@example.com a/123 Main Street n/" + invalidName));
        }
    }

    @Test
    public void parseCommand_addressBoundaryControls_throwsParseException() {
        String[] invalidAddresses = {"\t123 Main Street", "123 Main Street\n", "123 Main Street\r",
            "123 Main Street\0"};
        for (String invalidAddress : invalidAddresses) {
            assertThrows(ParseException.class, Address.MESSAGE_CONSTRAINTS, ()
                    -> parser.parseCommand("add n/Anne-Marie p/00123456 e/anne@example.com a/" + invalidAddress));
        }
    }

    @Test
    public void parseCommand_withdrawnCommands_throwsParseException() {
        for (String input : new String[]{"clear", "clear 3", "edit 1 n/Alex", "find Alex"}) {
            assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () -> parser.parseCommand(input));
        }
    }

    @Test
    public void parseCommand_delete() throws Exception {
        DeleteCommand command = (DeleteCommand) parser.parseCommand(
                DeleteCommand.COMMAND_WORD + " " + INDEX_FIRST_PERSON.getOneBased());
        assertEquals(new DeleteCommand(INDEX_FIRST_PERSON), command);
    }

    @Test
    public void parseCommand_exit() throws Exception {
        assertTrue(parser.parseCommand(ExitCommand.COMMAND_WORD) instanceof ExitCommand);
        assertThrows(ParseException.class, String.format(MESSAGE_INVALID_COMMAND_FORMAT, ExitCommand.MESSAGE_USAGE), ()
                -> parser.parseCommand("exit 3"));
    }

    @Test
    public void parseCommand_help() throws Exception {
        assertTrue(parser.parseCommand(HelpCommand.COMMAND_WORD) instanceof HelpCommand);
        assertEquals(new HelpCommand("add"), parser.parseCommand("help ADD"));
        assertThrows(ParseException.class, String.format(HelpCommand.MESSAGE_UNKNOWN_TOPIC, "3"), ()
                -> parser.parseCommand("help 3"));
        assertThrows(ParseException.class, String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE), ()
                -> parser.parseCommand("help add delete"));
    }

    @Test
    public void parseCommand_list() throws Exception {
        assertTrue(parser.parseCommand(ListCommand.COMMAND_WORD) instanceof ListCommand);
        assertThrows(ParseException.class, String.format(MESSAGE_INVALID_COMMAND_FORMAT, ListCommand.MESSAGE_USAGE), ()
                -> parser.parseCommand("list 3"));
    }

    @Test
    public void parseCommand_unrecognisedInput_throwsParseException() {
        assertThrows(ParseException.class, String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE), ()
            -> parser.parseCommand(""));
    }

    @Test
    public void parseCommand_unknownCommand_throwsParseException() {
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () -> parser.parseCommand("unknownCommand"));
    }
}
