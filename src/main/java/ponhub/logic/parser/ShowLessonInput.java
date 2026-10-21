package ponhub.logic.parser;

import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.time.LocalDate;
import java.util.Optional;

import ponhub.model.lesson.LessonId;

/** Validated stable lesson identity and optional occurrence date for a detail request. */
public record ShowLessonInput(LessonId lessonId, Optional<LocalDate> date) {
    public ShowLessonInput {
        requireAllNonNull(lessonId, date);
    }
}
