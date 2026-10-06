package seedu.address.logic;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.Command;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.commands.ExitCommand;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.parser.AddCommandParser;
import seedu.address.logic.parser.DeleteCommandParser;
import seedu.address.logic.parser.HelpCommandParser;
import seedu.address.logic.parser.Parser;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Registers active command parsers together with their user guidance.
 */
public final class CommandCatalog {
    private static final Map<String, Entry> ENTRIES = createEntries();

    private CommandCatalog() {
    }

    private static Map<String, Entry> createEntries() {
        Map<String, Entry> entries = new LinkedHashMap<>();
        entries.put(AddCommand.COMMAND_WORD, new Entry("Add a person", AddCommand.MESSAGE_USAGE,
                new AddCommandParser()));
        entries.put(DeleteCommand.COMMAND_WORD, new Entry("Delete a person by displayed index",
                DeleteCommand.MESSAGE_USAGE, new DeleteCommandParser()));
        entries.put(ListCommand.COMMAND_WORD, new Entry("Show all people", ListCommand.MESSAGE_USAGE,
                args -> parseWithoutArguments(args, ListCommand.MESSAGE_USAGE, new ListCommand())));
        entries.put(HelpCommand.COMMAND_WORD, new Entry("Show help or one command's guidance",
                HelpCommand.MESSAGE_USAGE, new HelpCommandParser()));
        entries.put(ExitCommand.COMMAND_WORD, new Entry("Close PonHub", ExitCommand.MESSAGE_USAGE,
                args -> parseWithoutArguments(args, ExitCommand.MESSAGE_USAGE, new ExitCommand())));
        return java.util.Collections.unmodifiableMap(entries);
    }

    private static Command parseWithoutArguments(String args, String usage, Command command) throws ParseException {
        if (!args.isBlank()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, usage));
        }
        return command;
    }

    /**
     * Returns active command words in catalogue order.
     */
    public static List<String> getCommandWords() {
        return List.copyOf(ENTRIES.keySet());
    }

    /**
     * Returns guidance for an exact lowercase command word, if it is active.
     */
    public static Optional<String> getUsage(String commandWord) {
        return Optional.ofNullable(ENTRIES.get(commandWord)).map(Entry::usage);
    }

    /**
     * Returns the inline overview of all active commands.
     */
    public static String getOverview() {
        StringBuilder overview = new StringBuilder("PonHub commands available in this build:\n");
        ENTRIES.forEach((word, entry) -> overview.append(word).append(" - ")
                .append(entry.description()).append('\n'));
        return overview.append("\nType help COMMAND for syntax and an example, e.g. help add.\n")
                .append("Scroll this Result Display to read long guidance.").toString();
    }

    /**
     * Parses arguments using the registered parser for an exact lowercase command word.
     */
    public static Command parse(String commandWord, String arguments) throws ParseException {
        Entry entry = ENTRIES.get(commandWord);
        if (entry == null) {
            throw new ParseException(Messages.MESSAGE_UNKNOWN_COMMAND);
        }
        return entry.parser().parse(arguments);
    }

    private record Entry(String description, String usage, Parser<? extends Command> parser) {
    }
}
