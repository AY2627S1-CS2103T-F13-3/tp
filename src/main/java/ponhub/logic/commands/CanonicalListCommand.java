package ponhub.logic.commands;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Optional;

import ponhub.logic.CanonicalCommandExecutor;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.person.PersonRole;

/**
 * A read-only people-list command using the executor's authoritative staged people view.
 * The application session routes this command without changing operational records and counters.
 */
public final class CanonicalListCommand implements CanonicalCommandExecutor.StagedCommand {
    public static final String MESSAGE_ALL_SUCCESS = "Showing %d person(s).";
    public static final String MESSAGE_ROLE_SUCCESS = "Showing %d %s(s).";

    private final Optional<PersonRole> roleFilter;

    /**
     * Creates a staged listing for all roles or one already validated role.
     */
    public CanonicalListCommand(Optional<PersonRole> roleFilter) {
        this.roleFilter = requireNonNull(roleFilter);
    }

    /**
     * Selects the requested view and reports its count, including a successful empty result.
     * The executor publishes the filter change without saving operational data.
     */
    @Override
    public CommandResult execute(PonHubData staged, PeopleView stagedView) {
        requireAllNonNull(staged, stagedView);
        stagedView.setRoleFilter(roleFilter);
        int count = stagedView.getPeople().size();
        String feedback = roleFilter.map(role -> String.format(MESSAGE_ROLE_SUCCESS, count, role.getValue()))
                .orElseGet(() -> String.format(MESSAGE_ALL_SUCCESS, count));
        return new CommandResult(feedback);
    }
}
