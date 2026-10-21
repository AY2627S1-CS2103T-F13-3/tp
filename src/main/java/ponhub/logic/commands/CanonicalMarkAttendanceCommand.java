package ponhub.logic.commands;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Optional;

import ponhub.logic.CanonicalCommandExecutor;
import ponhub.logic.PersonIndexResolver;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.AttendanceMarkInput;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.attendance.Attendance;
import ponhub.model.attendance.AttendanceKey;
import ponhub.model.person.Student;

/**
 * Records or corrects one Student's attendance at a dated shared-lesson occurrence.
 */
public final class CanonicalMarkAttendanceCommand implements CanonicalCommandExecutor.StagedCommand {
    public static final String MESSAGE_SUCCESS =
            "Attendance for Student %s in lesson %s on %s marked %s.";
    public static final String MESSAGE_NO_CHANGE =
            "Attendance for Student %s in lesson %s on %s is already marked %s. No changes made.";

    private final AttendanceMarkInput input;

    /**
     * Creates a staged attendance command from validated selectors and values.
     */
    public CanonicalMarkAttendanceCommand(AttendanceMarkInput input) {
        this.input = requireNonNull(input);
    }

    /**
     * Resolves the current displayed Student once, then records attendance using stable identities.
     *
     * @throws CommandException if the Student, Lesson, date or requested attendance transition is invalid.
     */
    @Override
    public CommandResult execute(PonHubData staged, PeopleView stagedView) throws CommandException {
        requireAllNonNull(staged, stagedView);
        Student student = PersonIndexResolver.resolveStudent(input.studentIndex(), stagedView.getPeople());
        AttendanceKey key = new AttendanceKey(student.getId(), input.lessonId(), input.date());
        Optional<Attendance> existing = staged.getAttendance(key);
        try {
            Attendance attendance = staged.markAttendance(student.getId(), input.lessonId(), input.date(),
                    input.status());
            String message = existing.isPresent() && existing.get().getStatus() == input.status()
                    ? MESSAGE_NO_CHANGE : MESSAGE_SUCCESS;
            return new CommandResult(String.format(message, student.getId(), input.lessonId(), input.date(),
                    attendance.getStatus()));
        } catch (IllegalArgumentException failure) {
            throw new CommandException(failure.getMessage(), failure);
        }
    }
}
