package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Optional;

import seedu.address.logic.CanonicalCommandExecutor;
import seedu.address.logic.PersonIndexResolver;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.AttendanceMarkInput;
import seedu.address.model.PeopleView;
import seedu.address.model.PonHubData;
import seedu.address.model.attendance.Attendance;
import seedu.address.model.attendance.AttendanceKey;
import seedu.address.model.person.Student;

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
