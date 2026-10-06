package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.logic.CommandCatalog;
import seedu.address.model.Model;

/**
 * Displays the active command catalogue or one command's guidance inline.
 */
public class HelpCommand extends Command {

    public static final String COMMAND_WORD = "help";

    public static final String MESSAGE_USAGE = "Usage: help [COMMAND]\n"
            + "Shows available commands, or syntax and an example for one command.\n"
            + "COMMAND is optional and case-insensitive. Supply at most one command word.\n"
            + "Examples: help, help add";

    public static final String MESSAGE_UNKNOWN_TOPIC =
            "Unknown command: '%s'. Type 'help' to see available commands.";

    private final String topic;

    public HelpCommand() {
        topic = "";
    }

    /**
     * Creates help for an active lowercase command word.
     */
    public HelpCommand(String topic) {
        requireNonNull(topic);
        if (CommandCatalog.getUsage(topic).isEmpty()) {
            throw new IllegalArgumentException("Help topic must be an active command word.");
        }
        this.topic = topic;
    }

    @Override
    public CommandResult execute(Model model) {
        String guidance = topic.isEmpty() ? CommandCatalog.getOverview() : CommandCatalog.getUsage(topic).orElseThrow();
        return new CommandResult(guidance);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || (other instanceof HelpCommand otherHelp && topic.equals(otherHelp.topic));
    }

    @Override
    public int hashCode() {
        return topic.hashCode();
    }
}
