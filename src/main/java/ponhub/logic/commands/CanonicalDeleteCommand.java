package ponhub.logic.commands;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import ponhub.logic.CanonicalCommandExecutor;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.person.PersonRecord;

/**
 * Deletes the exact unreferenced person whose immutable record was shown for confirmation.
 */
public final class CanonicalDeleteCommand implements CanonicalCommandExecutor.StagedCommand {
    public static final String MESSAGE_SUCCESS = "Deleted %s: %s (%s).";

    private final PersonRecord person;

    /**
     * Creates a guarded deletion for the person shown in the pending confirmation.
     */
    public CanonicalDeleteCommand(PersonRecord person) {
        this.person = requireNonNull(person);
    }

    /**
     * Rechecks the confirmed record and removes only its stable identity, preserving the current filter.
     *
     * @throws CommandException if the person has changed, is missing or is referenced.
     */
    @Override
    public CommandResult execute(PonHubData staged, PeopleView stagedView) throws CommandException {
        requireAllNonNull(staged, stagedView);
        if (staged.getPerson(person.getId()).filter(person::equals).isEmpty()) {
            throw new CommandException("The person has changed. Run delete INDEX again to check the target.");
        }
        try {
            staged.removePerson(person.getId());
            return new CommandResult(String.format(MESSAGE_SUCCESS, person.getRole().getValue(),
                    person.getContactDetails().getName(), person.getId()));
        } catch (IllegalArgumentException failure) {
            throw new CommandException(failure.getMessage(), failure);
        }
    }
}
