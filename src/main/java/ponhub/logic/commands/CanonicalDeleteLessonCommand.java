package ponhub.logic.commands;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import ponhub.logic.CanonicalCommandExecutor;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.lesson.Lesson;
import ponhub.model.lesson.LessonId;

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
