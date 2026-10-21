package ponhub.logic.commands;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Optional;

import ponhub.logic.CanonicalCommandExecutor;
import ponhub.logic.PersonIndexResolver;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.AttendanceUnmarkInput;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.attendance.Attendance;
import ponhub.model.person.Student;

/**
 * Removes one Student's attendance at a dated shared-lesson occurrence.
 */
public final class CanonicalUnmarkAttendanceCommand implements CanonicalCommandExecutor.StagedCommand {
    public static final String MESSAGE_SUCCESS =
            "Attendance for Student %s in lesson %s on %s removed.";
    public static final String MESSAGE_NO_CHANGE =
            "No attendance record exists for Student %s in lesson %s on %s. No changes made.";

    private final AttendanceUnmarkInput input;

    /**
     * Creates a staged attendance-removal command from validated selectors.
     */
    public CanonicalUnmarkAttendanceCommand(AttendanceUnmarkInput input) {
        this.input = requireNonNull(input);
    }

    /**
     * Resolves the current displayed Student once, then removes attendance using stable identities.
     *
     * @throws CommandException if the Student, Lesson or date is invalid.
     */
    @Override
    public CommandResult execute(PonHubData staged, PeopleView stagedView) throws CommandException {
        requireAllNonNull(staged, stagedView);
        Student student = PersonIndexResolver.resolveStudent(input.studentIndex(), stagedView.getPeople());
        try {
            Optional<Attendance> removed = staged.unmarkAttendance(student.getId(), input.lessonId(), input.date());
            String message = removed.isPresent() ? MESSAGE_SUCCESS : MESSAGE_NO_CHANGE;
            return new CommandResult(String.format(message, student.getId(), input.lessonId(), input.date()));
        } catch (IllegalArgumentException failure) {
            throw new CommandException(failure.getMessage(), failure);
        }
    }
}
