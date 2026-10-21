package ponhub.logic.parser;

import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.time.LocalDate;

import ponhub.commons.core.index.Index;
import ponhub.model.attendance.AttendanceStatus;
import ponhub.model.lesson.LessonId;

/**
 * Validated selectors and values for recording one dated lesson attendance outcome.
 *
 * @param studentIndex One-based position in the current people list.
 * @param lessonId Stable identity of the selected shared lesson.
 * @param date Date of the recurring lesson occurrence.
 * @param status Recorded present or absent outcome.
 */
public record AttendanceMarkInput(Index studentIndex, LessonId lessonId, LocalDate date,
        AttendanceStatus status) {

    /**
     * Rejects null attendance input before command execution.
     */
    public AttendanceMarkInput {
        requireAllNonNull(studentIndex, lessonId, date, status);
    }
}
