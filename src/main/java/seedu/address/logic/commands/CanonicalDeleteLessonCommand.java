package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import seedu.address.logic.CanonicalCommandExecutor;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.PeopleView;
import seedu.address.model.PonHubData;
import seedu.address.model.lesson.Lesson;
import seedu.address.model.lesson.LessonId;

/**
 * Deletes one shared lesson after verifying that no roster or attendance dependency remains.
 */
public final class CanonicalDeleteLessonCommand implements CanonicalCommandExecutor.StagedCommand {
    public static final String MESSAGE_SUCCESS = "Deleted lesson: %s.";

    private final LessonId lessonId;

    /**
     * Creates a guarded deletion for one validated stable Lesson ID.
     */
    public CanonicalDeleteLessonCommand(LessonId lessonId) {
        this.lessonId = requireNonNull(lessonId);
    }

    /**
     * Removes only the selected unreferenced Lesson without changing the people view.
     *
     * @throws CommandException if the Lesson is missing or still has roster or attendance dependencies.
     */
    @Override
    public CommandResult execute(PonHubData staged, PeopleView stagedView) throws CommandException {
        requireAllNonNull(staged, stagedView);
        try {
            Lesson lesson = staged.removeLesson(lessonId);
            return new CommandResult(String.format(MESSAGE_SUCCESS, lesson.getId()));
        } catch (IllegalArgumentException failure) {
            throw new CommandException(failure.getMessage(), failure);
        }
    }
}
