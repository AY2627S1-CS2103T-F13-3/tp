package seedu.address.logic;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import seedu.address.logic.parser.LessonEditInput;
import seedu.address.model.PonHubData;
import seedu.address.model.PonHubDataState;
import seedu.address.model.lesson.Lesson;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.lesson.LessonTimeSlot;

/**
 * An immutable complete candidate for editing one Lesson without changing live state or allocating an ID.
 * Preparation never saves or publishes; the active command installs it only within the staged transaction.
 */
public final class LessonEditCandidate {

    private final PonHubDataState candidateState;
    private final Lesson editedLesson;
    private final boolean isChanged;

    private LessonEditCandidate(PonHubDataState candidateState, Lesson editedLesson, boolean isChanged) {
        this.candidateState = candidateState;
        this.editedLesson = editedLesson;
        this.isChanged = isChanged;
    }

    /**
     * Prepares a replacement from one complete snapshot, a stable Lesson ID and immutable field intent.
     * Preserves catalogue position, identity, roster order and membership, attendance, and allocation history.
     * Canonical constructors and whole-root validation reject invalid slots, references and schedule conflicts.
     * A complete-value no-op retains the source snapshot and original Lesson instance.
     * Callers must prepare from a fresh snapshot within their single-threaded transaction boundary.
     *
     * @throws NullPointerException if any argument is null.
     * @throws IllegalArgumentException if the target is missing or the resulting aggregate is invalid.
     */
    public static LessonEditCandidate prepare(PonHubDataState source, LessonId targetId, LessonEditInput input) {
        requireAllNonNull(source, targetId, input);
        PonHubData staged = new PonHubData(source);
        Lesson original = staged.getLesson(targetId).orElseThrow(() ->
                new IllegalArgumentException("The lesson does not exist: " + targetId));
        LessonTimeSlot originalSlot = original.getTimeSlot();
        LessonTimeSlot replacementSlot = new LessonTimeSlot(
                input.day().orElse(originalSlot.getDay()),
                input.startTime().orElse(originalSlot.getStartTime()),
                input.endTime().orElse(originalSlot.getEndTime()));
        Lesson replacement = new Lesson(targetId,
                input.tutorId().orElse(original.getTutorId()),
                replacementSlot,
                input.subject().orElse(original.getSubject()),
                input.room().orElse(original.getRoom()),
                original.getEnrolledStudentIds());
        Lesson editedLesson = staged.replaceLesson(replacement);
        return new LessonEditCandidate(staged.exportState(), editedLesson, !original.equals(editedLesson));
    }

    public PonHubDataState getCandidateState() {
        return candidateState;
    }

    public Lesson getEditedLesson() {
        return editedLesson;
    }

    public boolean isChanged() {
        return isChanged;
    }
}
