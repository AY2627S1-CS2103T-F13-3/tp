package ponhub.logic.parser;

import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import ponhub.commons.core.index.Index;
import ponhub.model.lesson.LessonId;

/**
 * Validated selectors for changing one student's current membership in one shared lesson.
 * The displayed index is resolved to a stable Student ID only at the staged command boundary.
 *
 * @param studentIndex One-based position in the current people list.
 * @param lessonId Stable identity of the selected shared lesson.
 */
public record LessonMembershipInput(Index studentIndex, LessonId lessonId) {

    /**
     * Rejects null selectors before command execution.
     */
    public LessonMembershipInput {
        requireAllNonNull(studentIndex, lessonId);
    }
}
