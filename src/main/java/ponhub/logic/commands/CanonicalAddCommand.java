package ponhub.logic.commands;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Optional;

import ponhub.logic.CanonicalCommandExecutor;
import ponhub.logic.PersonAdditionCandidate;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.PersonAdditionInput;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.PersonRole;
import ponhub.model.person.exceptions.DuplicatePersonException;
import ponhub.model.person.exceptions.PersonIdExhaustedException;

/**
 * A role-aware addition that operates only on the executor's staged canonical root and view.
 * The application session persists the candidate before publishing the shared people view.
 */
public final class CanonicalAddCommand implements CanonicalCommandExecutor.StagedCommand {
    public static final String MESSAGE_SUCCESS = "New %s added: %s (%s).";
    public static final String MESSAGE_DUPLICATE = "A %s with the same name and %s already exists.";

    private final PersonAdditionInput input;

    /**
     * Creates a staged command from already validated, ID-free role-aware arguments.
     */
    public CanonicalAddCommand(PersonAdditionInput input) {
        this.input = requireNonNull(input);
    }

    /**
     * Prepares the complete addition and all-role view without touching live data or saving.
     * The executor persists before publishing this state and returning the prepared success result.
     *
     * @throws CommandException if the role-specific identity is duplicated or its IDs are exhausted.
     */
    @Override
    public CommandResult execute(PonHubData staged, PeopleView stagedView) throws CommandException {
        requireAllNonNull(staged, stagedView);
        PersonAdditionCandidate candidate;
        try {
            candidate = PersonAdditionCandidate.prepare(staged.exportState(), input);
        } catch (DuplicatePersonException failure) {
            String identityPhone = input.role() == PersonRole.STUDENT ? "parent phone" : "phone";
            throw new CommandException(String.format(MESSAGE_DUPLICATE, input.role().getValue(), identityPhone),
                    failure);
        } catch (PersonIdExhaustedException failure) {
            throw new CommandException(failure.getMessage(), failure);
        }
        staged.resetData(candidate.getCandidateState());
        stagedView.setRoleFilter(Optional.empty());
        PersonRecord added = candidate.getAddedPerson();
        return new CommandResult(String.format(MESSAGE_SUCCESS, added.getRole().getValue(),
                added.getContactDetails().getName(), added.getId()));
    }
}
