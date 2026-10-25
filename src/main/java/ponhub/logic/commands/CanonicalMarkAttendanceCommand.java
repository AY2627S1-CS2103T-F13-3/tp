package ponhub.logic.commands;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

import ponhub.logic.CanonicalCommandExecutor;
import ponhub.logic.PersonIndexResolver;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.AttendanceMarkInput;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.attendance.Attendance;
import ponhub.model.attendance.AttendanceKey;
import ponhub.model.attendance.AttendanceStatus;
import ponhub.model.person.Student;

/**
 * Records or corrects one Student's attendance at a dated shared-lesson occurrence.
 */
public final class CanonicalMarkAttendanceCommand implements CanonicalCommandExecutor.StagedCommand {
    public static final String MESSAGE_SUCCESS =
            "Attendance for Student %s in lesson %s on %s marked %s.";
    public static final String MESSAGE_NO_CHANGE =
            "Attendance for Student %s in lesson %s on %s is already marked %s. No changes made.";
    public static final String MESSAGE_FUTURE_PRESENT =
            "Attendance date %s is after today (%s). To confirm a future present outcome, repeat the command "
                    + "with ack/future. No attendance was saved.";

    private final AttendanceMarkInput input;
    private final Clock clock;

    /**
     * Creates a staged attendance command using the supplied clock for future-date safeguards.
     */
    public CanonicalMarkAttendanceCommand(AttendanceMarkInput input, Clock clock) {
        this.input = requireNonNull(input);
        this.clock = requireNonNull(clock);
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
        Attendance attendance;
        try {
            attendance = staged.markAttendance(student.getId(), input.lessonId(), input.date(), input.status());
        } catch (IllegalArgumentException failure) {
            throw new CommandException(failure.getMessage(), failure);
        }
        LocalDate today = LocalDate.now(clock);
        if (input.status() == AttendanceStatus.PRESENT
                && input.date().isAfter(today) && !input.futureAcknowledged()) {
            throw new CommandException(String.format(MESSAGE_FUTURE_PRESENT, input.date(), today));
        }
        String message = existing.isPresent() && existing.get().getStatus() == input.status()
                ? MESSAGE_NO_CHANGE : MESSAGE_SUCCESS;
        return new CommandResult(String.format(message, student.getId(), input.lessonId(), input.date(),
                attendance.getStatus()));
    }
}
