package ponhub.logic.commands;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import ponhub.commons.core.index.Index;
import ponhub.logic.CanonicalCommandExecutor;
import ponhub.logic.PersonIndexResolver;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.person.PersonRecord;

/**
 * Deletes one unreferenced person selected from the current filtered people list.
 */
public final class CanonicalDeleteCommand implements CanonicalCommandExecutor.StagedCommand {
    public static final String MESSAGE_SUCCESS = "Deleted %s: %s (%s).";

    private final Index index;

    /**
     * Creates a guarded deletion for a validated current people-list position.
     */
    public CanonicalDeleteCommand(Index index) {
        this.index = requireNonNull(index);
    }

    /**
     * Resolves the person once and removes only that stable identity, preserving the current filter.
     *
     * @throws CommandException if the displayed position is invalid or the person is referenced.
     */
    @Override
    public CommandResult execute(PonHubData staged, PeopleView stagedView) throws CommandException {
        requireAllNonNull(staged, stagedView);
        PersonRecord person = PersonIndexResolver.resolve(index, stagedView.getPeople());
        try {
            staged.removePerson(person.getId());
            return new CommandResult(String.format(MESSAGE_SUCCESS, person.getRole().getValue(),
                    person.getContactDetails().getName(), person.getId()));
        } catch (IllegalArgumentException failure) {
            throw new CommandException(failure.getMessage(), failure);
        }
    }
}
