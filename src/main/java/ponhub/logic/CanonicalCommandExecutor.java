package ponhub.logic;

import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Optional;
import java.util.function.Supplier;

import ponhub.logic.commands.CommandResult;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.model.CanonicalTransaction;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.PonHubDataState;

/**
 * Canonical command boundary for the application's single model thread.
 * The application session supplies protected guidance and a complete compatible snapshot writer.
 * It must still enforce the protected session's allowed-command catalogue before calling this boundary.
 */
public final class CanonicalCommandExecutor {
    public static final String FILE_OPS_ERROR_FORMAT = "Could not save data due to the following error: %s";
    public static final String FILE_OPS_PERMISSION_ERROR_FORMAT =
            "Could not save data to file %s due to insufficient permissions to write to the file or the folder.";

    private final PeopleView view;
    private final SnapshotWriter writer;
    private final Supplier<Optional<String>> loadError;

    /**
     * Connects one canonical view/root to persistence and the authoritative startup protection state.
     * The writer must report failures without silently dropping unsupported aggregate collections.
     */
    public CanonicalCommandExecutor(PeopleView view, SnapshotWriter writer, Supplier<Optional<String>> loadError) {
        requireAllNonNull(view, writer, loadError);
        this.view = view;
        this.writer = writer;
        this.loadError = loadError;
    }

    /**
     * Executes in isolation, persists changed state, then publishes data and view before returning success.
     * Commands and writers must not mutate live state or perform nested command execution.
     */
    public CommandResult execute(StagedCommand command) throws CommandException {
        requireAllNonNull(command);
        CanonicalTransaction transaction = view.beginTransaction();
        CommandResult result = command.execute(transaction.getStagedModel(), transaction.getStagedPeopleView());
        requireAllNonNull(result);
        if (transaction.hasOperationalChanges()) {
            Optional<String> error = loadError.get();
            if (error.isPresent()) {
                throw new CommandException(error.get());
            }
            try {
                writer.save(transaction.getStagedModel().exportState());
            } catch (AccessDeniedException failure) {
                throw new CommandException(String.format(CanonicalCommandExecutor.FILE_OPS_PERMISSION_ERROR_FORMAT,
                        failure.getMessage()), failure);
            } catch (IOException failure) {
                throw new CommandException(String.format(CanonicalCommandExecutor.FILE_OPS_ERROR_FORMAT,
                        failure.getMessage()), failure);
            }
        }
        transaction.commit();
        return result;
    }

    /**
     * A command operating exclusively on staged data and the current people projection.
     */
    @FunctionalInterface
    public interface StagedCommand {
        /**
         * Prepares changes and feedback, throwing on validation or execution failure.
         */
        CommandResult execute(PonHubData staged, PeopleView stagedView) throws CommandException;
    }

    /**
     * Persistence callback accepting the complete validated snapshot.
     */
    @FunctionalInterface
    public interface SnapshotWriter {
        /**
         * Saves the snapshot or throws without publishing any model changes.
         */
        void save(PonHubDataState state) throws IOException;
    }
}
