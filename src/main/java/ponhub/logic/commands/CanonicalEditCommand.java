package ponhub.logic.commands;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import ponhub.logic.CanonicalCommandExecutor;
import ponhub.logic.PersonEditCandidate;
import ponhub.logic.PersonIndexResolver;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.PersonEditParser;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.exceptions.DuplicatePersonException;

/**
 * Edits one stable person solely within the executor's staged transaction.
 */
public final class CanonicalEditCommand implements CanonicalCommandExecutor.StagedCommand {
    public static final String MESSAGE_SUCCESS = "Edited %s: %s (%s).";
    public static final String MESSAGE_NO_CHANGE = "No changes to %s: %s (%s).";

    private final PersonEditParser.Input input;

    public CanonicalEditCommand(PersonEditParser.Input input) {
        this.input = requireNonNull(input);
    }

    @Override
    public CommandResult execute(PonHubData staged, PeopleView stagedView) throws CommandException {
        requireAllNonNull(staged, stagedView);
        PersonRecord original = PersonIndexResolver.resolve(input.index(), stagedView.getPeople());
        try {
            PersonEditCandidate candidate = PersonEditCandidate.prepare(staged.exportState(), original.getId(),
                    input.edit());
            if (candidate.isChanged()) {
                staged.resetData(candidate.getCandidateState());
            }
            PersonRecord edited = candidate.getEditedPerson();
            return new CommandResult(String.format(candidate.isChanged() ? MESSAGE_SUCCESS : MESSAGE_NO_CHANGE,
                    edited.getRole().getValue(), edited.getName(), edited.getId()));
        } catch (IllegalArgumentException | DuplicatePersonException failure) {
            throw new CommandException(failure.getMessage(), failure);
        }
    }
}
