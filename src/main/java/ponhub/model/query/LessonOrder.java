package ponhub.model.query;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Optional presentation order for catalogue, Student-enrollment and lesson-search results.
 */
public enum LessonOrder {
    CREATION, SCHEDULE;

    private static final Comparator<LessonView> SCHEDULE_ORDER = Comparator
            .comparing((LessonView lesson) -> lesson.getTimeSlot().getDay())
            .thenComparing(lesson -> lesson.getTimeSlot().getStartTime())
            .thenComparing(LessonView::getLessonId);

    /**
     * Returns the requested order, ignoring case and surrounding whitespace.
     */
    public static LessonOrder fromValue(String value) {
        requireNonNull(value);
        try {
            return valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Lesson sort order must be creation or schedule.", failure);
        }
    }

    /**
     * Returns an immutable ordered snapshot without mutating the source or its stable identities.
     */
    public List<LessonView> apply(List<LessonView> lessons) {
        requireNonNull(lessons);
        return this == CREATION ? List.copyOf(lessons) : lessons.stream().sorted(SCHEDULE_ORDER).toList();
    }
}
