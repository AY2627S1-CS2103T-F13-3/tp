package ponhub.model.query;

import java.util.Optional;

import javafx.collections.ObservableList;
import ponhub.model.lesson.LessonId;
import ponhub.model.person.PersonId;

/**
 * Read-only retrieval boundary exposed by the canonical Model adapter to Logic.
 * Lists retain their identity across committed source changes and cannot be modified by callers.
 */
public interface LessonQuerySource {
    String MESSAGE_UNAVAILABLE = "Shared-lesson retrieval is not implemented yet";

    /** Returns every shared lesson once in creation order, including empty rosters. */
    ObservableList<LessonView> getLessonList();

    /** Returns the active lesson-filter results in catalogue order without changing the people view. */
    ObservableList<LessonView> getFilteredLessonList();

    /**
     * Looks up an immutable projection in the full catalogue, independently of filtering.
     *
     * @return empty for a valid unknown ID
     * @throws NullPointerException if the ID is null
     */
    Optional<LessonView> findLessonById(LessonId lessonId);

    /**
     * Returns current lessons derived from Lesson-owned roster IDs, in catalogue order.
     *
     * @throws NullPointerException if the ID is null
     * @throws IllegalArgumentException if the student is unknown or the ID has another role
     */
    ObservableList<LessonView> getStudentLessons(PersonId studentId);

    /**
     * Returns current student details in people creation order; empty rosters are valid.
     *
     * @throws NullPointerException if the ID is null
     * @throws IllegalArgumentException if the lesson is unknown
     */
    ObservableList<StudentView> getLessonRoster(LessonId lessonId);

    /**
     * Returns assigned lessons by Monday-first weekday, start time and numeric lesson ID.
     *
     * @throws NullPointerException if the ID is null
     * @throws IllegalArgumentException if the tutor is unknown or the ID has another role
     */
    ObservableList<LessonView> getTutorSchedule(PersonId tutorId);

    /** Returns retained dated records, newest first, then by Lesson ID. A missing record is unrecorded. */
    ObservableList<AttendanceHistoryEntry> getAttendanceHistory(PersonId studentId);

    /** Returns retained dated records for one existing lesson, including former membership. */
    ObservableList<AttendanceHistoryEntry> getAttendanceHistory(PersonId studentId, LessonId lessonId);
}
