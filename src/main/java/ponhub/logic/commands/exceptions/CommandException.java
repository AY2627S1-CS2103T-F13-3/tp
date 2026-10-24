package ponhub.logic.commands.exceptions;

/**
 * Represents an error during execution of a {@link ponhub.logic.CanonicalCommandExecutor.StagedCommand}.
 */
public class CommandException extends Exception {
    public CommandException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code CommandException} with the specified detail {@code message} and {@code cause}.
     */
    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
