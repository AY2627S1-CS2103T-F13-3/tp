package ponhub.logic.commands;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.util.List;

import ponhub.logic.CanonicalCommandExecutor;
import ponhub.logic.PersonEditCandidate;
import ponhub.logic.PersonIndexResolver;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.PersonEditParser;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.person.Parent;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.PersonRole;
import ponhub.model.person.Student;
import ponhub.model.person.exceptions.DuplicatePersonException;

/**
 * Edits one stable person solely within the executor's staged transaction.
 */
public final class CanonicalEditCommand implements CanonicalCommandExecutor.StagedCommand {
    public static final String MESSAGE_SUCCESS = "Edited %s: %s (%s).";
    public static final String MESSAGE_NO_CHANGE = "No changes to %s: %s (%s).";
    public static final String MESSAGE_DUPLICATE = "A %s with the same normalized name and %s already exists. "
            + "Change n/ or %s to a unique combination.";

    public static final String MESSAGE_DISCONNECTED_LINKS = "Warning: Parent-Student links removed: %s. "
            + "Update the corresponding phone values to restore a link.";

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
            PersonRecord edited = candidate.getEditedPerson();
            List<String> disconnected = disconnectedLinks(staged.getPeople(), original, edited);
            if (candidate.isChanged()) {
                staged.resetData(candidate.getCandidateState());
            }
            String feedback = String.format(candidate.isChanged() ? MESSAGE_SUCCESS : MESSAGE_NO_CHANGE,
                    edited.getRole().getValue(), edited.getName(), edited.getId());
            if (!disconnected.isEmpty()) {
                feedback += "\n" + String.format(MESSAGE_DISCONNECTED_LINKS, String.join(", ", disconnected));
            }
            return new CommandResult(feedback);
        } catch (DuplicatePersonException failure) {
            boolean isStudent = original.getRole() == PersonRole.STUDENT;
            String identityPhone = isStudent ? "parent phone (pp/)" : "phone (p/)";
            throw new CommandException(String.format(MESSAGE_DUPLICATE, original.getRole().getValue(),
                    identityPhone, isStudent ? "pp/" : "p/"), failure);
        } catch (IllegalArgumentException failure) {
            throw new CommandException(failure.getMessage(), failure);
        }
    }

    /**
     * Identifies lost exact-phone links against the complete pre-edit snapshot, including hidden people.
     * Only the edited endpoint can change, so new links do not hide the loss of existing links.
     */
    private static List<String> disconnectedLinks(List<PersonRecord> people, PersonRecord original,
            PersonRecord edited) {
        if (original instanceof Parent parent && edited instanceof Parent replacement
                && !parent.getPhone().equals(replacement.getPhone())) {
            return people.stream().filter(Student.class::isInstance).map(Student.class::cast)
                    .filter(student -> student.getParentPhone().equals(parent.getPhone()))
                    .map(student -> parent.getId() + " / " + student.getId()).toList();
        }
        if (original instanceof Student student && edited instanceof Student replacement
                && !student.getParentPhone().equals(replacement.getParentPhone())) {
            return people.stream().filter(Parent.class::isInstance).map(Parent.class::cast)
                    .filter(parent -> parent.getPhone().equals(student.getParentPhone()))
                    .map(parent -> parent.getId() + " / " + student.getId()).toList();
        }
        return List.of();
    }
}
