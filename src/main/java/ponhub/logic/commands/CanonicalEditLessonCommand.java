package ponhub.logic.commands;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Optional;

import ponhub.logic.CanonicalCommandExecutor;
import ponhub.logic.LessonEditCandidate;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.LessonEditInput;
import ponhub.logic.parser.LessonEditParser;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.attendance.Attendance;
import ponhub.model.lesson.Lesson;
import ponhub.model.lesson.LessonDay;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;

/**
 * Edits one stable Lesson solely within the executor's staged transaction.
 */
public final class CanonicalEditLessonCommand implements CanonicalCommandExecutor.StagedCommand {
    public static final String MESSAGE_SUCCESS = "Edited lesson: %s.";
    public static final String MESSAGE_NO_CHANGE = "No changes to lesson: %s.";
    public static final String MESSAGE_RETAINED_ATTENDANCE =
            "Cannot change lesson %s to %s because retained attendance for Student %s (%s) on %s "
                    + "must match its weekday. Run 'showlesson lid/%s d/%s' to inspect the blocking records. "
                    + "Before using unmark, obtain the Student's current people-list position.";

    private final LessonEditParser.Input input;

    public CanonicalEditLessonCommand(LessonEditParser.Input input) {
        this.input = requireNonNull(input);
    }

    @Override
    public CommandResult execute(PonHubData staged, PeopleView stagedView) throws CommandException {
        requireAllNonNull(staged, stagedView);
        Lesson original = staged.getLesson(input.lessonId()).orElseThrow(() ->
                new CommandException("The lesson does not exist: " + input.lessonId()));
        rejectIncompatibleAttendance(staged, original);
        Optional<PersonId> tutorId = input.tutorName().isEmpty() ? Optional.empty()
                : Optional.of(TutorResolver.resolve(staged, input.tutorName().orElseThrow(), input.tutorPhone())
                        .getId());
        LessonEditInput edit = new LessonEditInput(input.day(), input.startTime(), input.endTime(), input.subject(),
                tutorId, input.room());
        try {
            LessonEditCandidate candidate = LessonEditCandidate.prepare(staged.exportState(), input.lessonId(), edit);
            if (candidate.isChanged()) {
                staged.resetData(candidate.getCandidateState());
            }
            return new CommandResult(String.format(candidate.isChanged() ? MESSAGE_SUCCESS : MESSAGE_NO_CHANGE,
                    candidate.getEditedLesson().getId()));
        } catch (IllegalArgumentException failure) {
            throw new CommandException(failure.getMessage(), failure);
        }
    }

    private void rejectIncompatibleAttendance(PonHubData staged, Lesson original) throws CommandException {
        if (input.day().isEmpty() || input.day().orElseThrow() == original.getTimeSlot().getDay()) {
            return;
        }
        LessonDay replacementDay = input.day().orElseThrow();
        Optional<Attendance> incompatible = staged.getAttendanceForLesson(original.getId()).stream()
                .filter(record -> !replacementDay.matches(record.getDate())).findFirst();
        if (incompatible.isPresent()) {
            Attendance blockingAttendance = incompatible.orElseThrow();
            PersonRecord blockingStudent = staged.getPerson(blockingAttendance.getStudentId()).orElseThrow();
            throw new CommandException(String.format(MESSAGE_RETAINED_ATTENDANCE,
                    original.getId(), replacementDay, blockingStudent.getId(), blockingStudent.getName(),
                    blockingAttendance.getDate(), original.getId(), blockingAttendance.getDate()));
        }
    }
}
