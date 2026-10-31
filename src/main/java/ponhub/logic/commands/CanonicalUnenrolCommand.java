package ponhub.logic.commands;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import ponhub.logic.CanonicalCommandExecutor;
import ponhub.logic.PersonIndexResolver;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.LessonMembershipInput;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.lesson.Lesson;
import ponhub.model.person.Student;

/**
 * Unenrolls the Student selected from the current people list while retaining dated attendance.
 */
public final class CanonicalUnenrolCommand implements CanonicalCommandExecutor.StagedCommand {
    public static final String MESSAGE_SUCCESS = "Student %s unenrolled from lesson %s.";

    private final LessonMembershipInput input;

    /**
     * Creates a staged unenrollment command from validated selectors.
     */
    public CanonicalUnenrolCommand(LessonMembershipInput input) {
        this.input = requireNonNull(input);
    }

    /**
     * Resolves the displayed Student once, then removes only their stable identity from the shared roster.
     *
     * @throws CommandException if the displayed person, lesson or current membership is invalid.
     */
    @Override
    public CommandResult execute(PonHubData staged, PeopleView stagedView) throws CommandException {
        requireAllNonNull(staged, stagedView);
        Student student = PersonIndexResolver.resolveStudent(input.studentIndex(), stagedView.getPeople());
        try {
            Lesson lesson = staged.unenrolStudent(student.getId(), input.lessonId());
            return new CommandResult(String.format(MESSAGE_SUCCESS, student.getId(), lesson.getId()));
        } catch (IllegalArgumentException failure) {
            throw new CommandException(failure.getMessage(), failure);
        }
    }
}
