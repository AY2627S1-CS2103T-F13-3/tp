package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.List;
import java.util.Locale;

import seedu.address.logic.CanonicalCommandExecutor;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.LessonCreationInput;
import seedu.address.model.PeopleView;
import seedu.address.model.PonHubData;
import seedu.address.model.lesson.Lesson;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Tutor;

/**
 * Creates an empty-roster shared lesson on the executor's staged canonical root.
 * The application session saves the complete candidate before publishing it or reporting its stable ID.
 */
public final class CanonicalAddLessonCommand implements CanonicalCommandExecutor.StagedCommand {
    public static final String MESSAGE_SUCCESS = "New lesson added: %s.";
    public static final String MESSAGE_TUTOR_NOT_FOUND = "No tutor found with the full name '%s'.";
    public static final String MESSAGE_TUTOR_PHONE_NOT_FOUND =
            "No tutor named '%s' has the exact phone '%s'.";
    public static final String MESSAGE_AMBIGUOUS_TUTOR =
            "More than one tutor is named '%s'. Supply tp/TUTOR_PHONE to select one.";

    private final LessonCreationInput input;

    /**
     * Creates a staged command from validated, identity-free lesson input.
     */
    public CanonicalAddLessonCommand(LessonCreationInput input) {
        this.input = requireNonNull(input);
    }

    /**
     * Resolves one tutor and stages one empty-roster lesson without changing the people view.
     *
     * @throws CommandException if tutor selection, lesson allocation or schedule validation fails.
     */
    @Override
    public CommandResult execute(PonHubData staged, PeopleView stagedView) throws CommandException {
        requireAllNonNull(staged, stagedView);
        Tutor tutor = resolveTutor(staged);
        try {
            Lesson lesson = staged.addLesson(tutor.getId(), input.timeSlot(), input.subject(), input.room());
            return new CommandResult(String.format(MESSAGE_SUCCESS, lesson.getId()));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            throw new CommandException(failure.getMessage(), failure);
        }
    }

    /**
     * Resolves the normalized full name and applies an optional exact phone without fallback.
     */
    private Tutor resolveTutor(PonHubData staged) throws CommandException {
        String normalizedName = input.tutorName().fullName.toLowerCase(Locale.ROOT);
        List<Tutor> nameMatches = staged.getPeople().stream()
                .filter(Tutor.class::isInstance)
                .map(Tutor.class::cast)
                .filter(tutor -> tutor.getContactDetails().getNormalizedName().equals(normalizedName))
                .toList();
        if (nameMatches.isEmpty()) {
            throw new CommandException(String.format(MESSAGE_TUTOR_NOT_FOUND, input.tutorName()));
        }
        if (input.tutorPhone().isPresent()) {
            Phone phone = input.tutorPhone().orElseThrow();
            return nameMatches.stream().filter(tutor -> tutor.getPhone().equals(phone)).findFirst()
                    .orElseThrow(() -> new CommandException(String.format(MESSAGE_TUTOR_PHONE_NOT_FOUND,
                            input.tutorName(), phone)));
        }
        if (nameMatches.size() > 1) {
            throw new CommandException(String.format(MESSAGE_AMBIGUOUS_TUTOR, input.tutorName()));
        }
        return nameMatches.getFirst();
    }
}
