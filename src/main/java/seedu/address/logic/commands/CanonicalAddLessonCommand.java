package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import seedu.address.logic.CanonicalCommandExecutor;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.LessonCreationInput;
import seedu.address.model.PeopleView;
import seedu.address.model.PonHubData;
import seedu.address.model.lesson.Lesson;
import seedu.address.model.person.Tutor;

/**
 * Creates an empty-roster shared lesson on the executor's staged canonical root.
 * The application session saves the complete candidate before publishing it or reporting its stable ID.
 */
public final class CanonicalAddLessonCommand implements CanonicalCommandExecutor.StagedCommand {
    public static final String MESSAGE_SUCCESS = "New lesson added: %s.";
    public static final String MESSAGE_TUTOR_NOT_FOUND = TutorResolver.MESSAGE_TUTOR_NOT_FOUND;
    public static final String MESSAGE_TUTOR_PHONE_NOT_FOUND =
            TutorResolver.MESSAGE_TUTOR_PHONE_NOT_FOUND;
    public static final String MESSAGE_AMBIGUOUS_TUTOR =
            TutorResolver.MESSAGE_AMBIGUOUS_TUTOR;

    private final LessonCreationInput input;

    /**
     * Creates a staged command from validated, identity-free lesson input.
     */
    public CanonicalAddLessonCommand(LessonCreationInput input) {
        this.input = requireNonNull(input);
    }

    /**
     * Resolves one tutor and stages one empty-roster lesson without changing the people view.
     *
     * @throws CommandException if tutor selection, lesson allocation or schedule validation fails.
     */
    @Override
    public CommandResult execute(PonHubData staged, PeopleView stagedView) throws CommandException {
        requireAllNonNull(staged, stagedView);
        Tutor tutor = TutorResolver.resolve(staged, input.tutorName(), input.tutorPhone());
        try {
            Lesson lesson = staged.addLesson(tutor.getId(), input.timeSlot(), input.subject(), input.room());
            return new CommandResult(String.format(MESSAGE_SUCCESS, lesson.getId()));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            throw new CommandException(failure.getMessage(), failure);
        }
    }

}
