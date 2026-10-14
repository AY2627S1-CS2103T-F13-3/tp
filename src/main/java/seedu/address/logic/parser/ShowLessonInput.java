package seedu.address.logic.parser;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.time.LocalDate;
import java.util.Optional;

import seedu.address.model.lesson.LessonId;

/** Validated stable lesson identity and optional occurrence date for a detail request. */
public record ShowLessonInput(LessonId lessonId, Optional<LocalDate> date) {
    public ShowLessonInput {
        requireAllNonNull(lessonId, date);
    }
}
