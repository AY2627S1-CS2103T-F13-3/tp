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
 * Enrols the Student selected from the current people list in one existing shared lesson.
 */
public final class CanonicalEnrolCommand implements CanonicalCommandExecutor.StagedCommand {
    public static final String MESSAGE_SUCCESS = "Student %s enrolled in lesson %s.";

    private final LessonMembershipInput input;

    /**
     * Creates a staged enrolment command from validated selectors.
     */
    public CanonicalEnrolCommand(LessonMembershipInput input) {
        this.input = requireNonNull(input);
    }

    /**
     * Resolves the displayed Student once, then stores only stable identities in the shared roster.
     *
     * @throws CommandException if the displayed person, lesson or proposed membership is invalid.
     */
    @Override
    public CommandResult execute(PonHubData staged, PeopleView stagedView) throws CommandException {
        requireAllNonNull(staged, stagedView);
        Student student = PersonIndexResolver.resolveStudent(input.studentIndex(), stagedView.getPeople());
        try {
            Lesson lesson = staged.enrolStudent(student.getId(), input.lessonId());
            return new CommandResult(String.format(MESSAGE_SUCCESS, student.getId(), lesson.getId()));
        } catch (IllegalArgumentException failure) {
            throw new CommandException(failure.getMessage(), failure);
        }
    }
}
