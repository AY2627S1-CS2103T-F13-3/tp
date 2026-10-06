package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.util.Locale;

import seedu.address.logic.CommandCatalog;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses an optional command topic for inline help.
 */
public class HelpCommandParser implements Parser<HelpCommand> {
    @Override
    public HelpCommand parse(String args) throws ParseException {
        String trimmedArgs = args.strip();
        if (trimmedArgs.isEmpty()) {
            return new HelpCommand();
        }
        String[] topics = trimmedArgs.split("\\s+");
        if (topics.length != 1) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE));
        }
        String topic = topics[0].toLowerCase(Locale.ROOT);
        if (CommandCatalog.getUsage(topic).isEmpty()) {
            throw new ParseException(String.format(HelpCommand.MESSAGE_UNKNOWN_TOPIC, topics[0]));
        }
        return new HelpCommand(topic);
    }
}
