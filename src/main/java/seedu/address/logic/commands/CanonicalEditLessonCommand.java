package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Optional;

import seedu.address.logic.CanonicalCommandExecutor;
import seedu.address.logic.LessonEditCandidate;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.LessonEditInput;
import seedu.address.logic.parser.LessonEditParser;
import seedu.address.model.PeopleView;
import seedu.address.model.PonHubData;
import seedu.address.model.attendance.Attendance;
import seedu.address.model.lesson.Lesson;
import seedu.address.model.lesson.LessonDay;
import seedu.address.model.person.PersonId;

/** Edits one stable Lesson solely within the executor's staged transaction. */
public final class CanonicalEditLessonCommand implements CanonicalCommandExecutor.StagedCommand {
    public static final String MESSAGE_SUCCESS = "Edited lesson: %s.";
    public static final String MESSAGE_NO_CHANGE = "No changes to lesson: %s.";
    public static final String MESSAGE_RETAINED_ATTENDANCE =
            "Cannot change lesson %s to %s because retained attendance on %s must match its weekday. "
                    + "Keep the current weekday or unmark the incompatible attendance record first.";

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
            throw new CommandException(String.format(MESSAGE_RETAINED_ATTENDANCE, original.getId(), replacementDay,
                    incompatible.orElseThrow().getDate()));
        }
    }
}
