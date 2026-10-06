package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import seedu.address.logic.CommandCatalog;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.parser.exceptions.ParseException;

public class HelpCommandParserTest {
    private final HelpCommandParser parser = new HelpCommandParser();

    @Test
    public void parse_emptyArguments_returnsOverview() throws Exception {
        assertEquals(new HelpCommand(), parser.parse(""));
        assertEquals(new HelpCommand(), parser.parse(" \t\n "));
    }

    @Test
    public void parse_activeTopics_acceptsCaseAndWhitespace() throws Exception {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            for (String topic : CommandCatalog.getCommandWords()) {
                assertEquals(new HelpCommand(topic), parser.parse(" \t" + topic.toUpperCase(Locale.ROOT) + " \n"));
            }
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    public void parse_unknownOrWithdrawnTopic_reportsOriginalInput() {
        for (String topic : new String[]{"REMOVE", "edit", "clear", "find", "addlesson", "mark", "history"}) {
            assertThrows(ParseException.class, String.format(HelpCommand.MESSAGE_UNKNOWN_TOPIC, topic), ()
                    -> parser.parse(topic));
        }
    }

    @Test
    public void parse_excessArguments_reportsUsage() {
        for (String args : new String[]{"add delete", "add\tdelete", "add\ndelete", "unknown extra"}) {
            String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE);
            assertThrows(ParseException.class, expectedMessage, () -> parser.parse(args));
        }
    }
}
