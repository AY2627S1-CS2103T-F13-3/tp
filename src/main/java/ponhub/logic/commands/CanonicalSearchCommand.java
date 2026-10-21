package ponhub.logic.commands;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Locale;

import ponhub.logic.CanonicalCommandExecutor;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.search.SearchCategory;
import ponhub.model.search.SearchCriteria;

/**
 * Searches validated own and relationship fields in the canonical people catalogue without writing operational data.
 */
public final class CanonicalSearchCommand implements CanonicalCommandExecutor.StagedCommand {
    public static final String MESSAGE_SUCCESS = "Found %d %s(s).";
    public static final String MESSAGE_LESSONS_UNAVAILABLE = "Lesson search is not available yet.";

    private final SearchCriteria criteria;

    public CanonicalSearchCommand(SearchCriteria criteria) {
        this.criteria = requireNonNull(criteria);
    }

    @Override
    public CommandResult execute(PonHubData staged, PeopleView stagedView) throws CommandException {
        requireAllNonNull(staged, stagedView);
        if (criteria.getCategory() == SearchCategory.LESSON) {
            throw new CommandException(MESSAGE_LESSONS_UNAVAILABLE);
        }
        try {
            stagedView.setSearchCriteria(criteria);
        } catch (IllegalArgumentException failure) {
            throw new CommandException(failure.getMessage(), failure);
        }
        return new CommandResult(String.format(MESSAGE_SUCCESS, stagedView.getPeople().size(),
                criteria.getCategory().name().toLowerCase(Locale.ROOT)));
    }
}
