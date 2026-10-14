package seedu.address.logic.parser;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.time.LocalDate;

import seedu.address.commons.core.index.Index;
import seedu.address.model.lesson.LessonId;

/**
 * Validated selectors for removing one dated lesson attendance outcome.
 *
 * @param studentIndex One-based position in the current people list.
 * @param lessonId Stable identity of the selected shared lesson.
 * @param date Date of the recurring lesson occurrence.
 */
public record AttendanceUnmarkInput(Index studentIndex, LessonId lessonId, LocalDate date) {

    /**
     * Rejects null attendance input before command execution.
     */
    public AttendanceUnmarkInput {
        requireAllNonNull(studentIndex, lessonId, date);
    }
}
