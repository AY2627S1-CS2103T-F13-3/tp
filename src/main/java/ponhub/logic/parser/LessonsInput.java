package ponhub.logic.parser;

import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Optional;

import ponhub.commons.core.index.Index;
import ponhub.model.query.LessonOrder;

/** Validated optional current Student position and lesson presentation order. */
public record LessonsInput(Optional<Index> studentIndex, LessonOrder order) {
    public LessonsInput {
        requireAllNonNull(studentIndex, order);
    }
}
