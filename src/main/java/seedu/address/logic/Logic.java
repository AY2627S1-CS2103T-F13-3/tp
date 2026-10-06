package seedu.address.logic;

import java.util.Optional;

import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.Person;
import seedu.address.model.query.AttendanceHistoryEntry;
import seedu.address.model.query.LessonView;
import seedu.address.model.query.StudentView;

/**
 * API of the Logic component.
 * Canonical retrieval methods define contracts for the planned shared lesson model. Their current LogicManager
 * implementations throw UnsupportedOperationException until that model is integrated.
 * Once integrated, query lists are unmodifiable, observable and kept current after committed changes or rollback.
 * Queries preserve the current people filter and order and do not change stored data or identity allocation state.
 */
public interface Logic {
    /**
     * Executes the command and returns the result.
     * @param commandText The command as entered by the user.
     * @return the result of the command execution.
     * @throws CommandException If an error occurs during command execution.
     * @throws ParseException If an error occurs during parsing.
     */
    CommandResult execute(String commandText) throws CommandException, ParseException;

    /** Returns an unmodifiable view of the filtered list of persons */
    ObservableList<Person> getFilteredPersonList();

    /**
     * Returns all canonical shared lessons once each, including lessons with no enrolled students.
     * Results follow lesson creation order.
     * @throws UnsupportedOperationException While canonical retrieval is not implemented.
     */
    ObservableList<LessonView> getLessonList();

    /**
     * Returns shared lessons matching the active lesson filter in lesson creation order.
     * Each lesson appears once, including matching lessons with empty rosters. No matches produce an empty list.
     * @throws UnsupportedOperationException While canonical retrieval is not implemented.
     */
    ObservableList<LessonView> getFilteredLessonList();

    /**
     * Looks up a lesson by stable ID across the complete canonical catalogue, regardless of the active filter.
     * The returned projection describes the lesson at the time of lookup.
     * @param lessonId The stable lesson ID.
     * @return The matching lesson, or Optional.empty() if a valid ID has no matching lesson.
     * @throws NullPointerException If lessonId is null, once retrieval is implemented.
     * @throws IllegalArgumentException If lessonId is malformed, once retrieval is implemented.
     * @throws UnsupportedOperationException While canonical retrieval is not implemented.
     */
    Optional<LessonView> findLessonById(String lessonId);

    /**
     * Returns the student's current lessons in lesson creation order.
     * Membership is derived from Lesson-owned enrolled student IDs, independently of historical attendance.
     * @param studentId The stable ID of an existing student.
     * @throws NullPointerException If studentId is null, once retrieval is implemented.
     * @throws IllegalArgumentException If studentId is invalid or unknown, once retrieval is implemented.
     * @throws UnsupportedOperationException While canonical retrieval is not implemented.
     */
    ObservableList<LessonView> getStudentLessons(String studentId);

    /**
     * Returns the lesson's currently enrolled students in global person creation order.
     * Resolves Lesson-owned enrolled student IDs against the canonical people registry. An empty roster is valid.
     * @param lessonId The stable ID of an existing lesson.
     * @throws NullPointerException If lessonId is null, once retrieval is implemented.
     * @throws IllegalArgumentException If lessonId is invalid or unknown, once retrieval is implemented.
     * @throws UnsupportedOperationException While canonical retrieval is not implemented.
     */
    ObservableList<StudentView> getLessonRoster(String lessonId);

    /**
     * Returns the tutor's assigned shared lessons, including lessons with empty rosters.
     * Results are ordered by weekday (Monday first), start time and then stable lesson ID lexicographically.
     * @param tutorId The stable ID of an existing tutor.
     * @throws NullPointerException If tutorId is null, once retrieval is implemented.
     * @throws IllegalArgumentException If tutorId is invalid or unknown, once retrieval is implemented.
     * @throws UnsupportedOperationException While canonical retrieval is not implemented.
     */
    ObservableList<LessonView> getTutorSchedule(String tutorId);

    /**
     * Returns all recorded attendance for the student, including lessons from former enrolments.
     * Results are ordered by date (newest first) and then stable lesson ID lexicographically.
     * Each entry distinguishes current membership from retained history. No recorded attendance gives an empty list.
     * @param studentId The stable ID of an existing student.
     * @throws NullPointerException If studentId is null, once retrieval is implemented.
     * @throws IllegalArgumentException If studentId is invalid or unknown, once retrieval is implemented.
     * @throws UnsupportedOperationException While canonical retrieval is not implemented.
     */
    ObservableList<AttendanceHistoryEntry> getAttendanceHistory(String studentId);

    /**
     * Returns the student's recorded attendance for one lesson, including retained entries for former enrolments.
     * Uses the same ordering and membership distinction as getAttendanceHistory(studentId).
     * Current enrolment is not required; no matching attendance gives an empty list.
     * @param studentId The stable ID of an existing student.
     * @param lessonId The stable ID of an existing lesson.
     * @throws NullPointerException If either ID is null, once retrieval is implemented.
     * @throws IllegalArgumentException If either ID is invalid or unknown, once retrieval is implemented.
     * @throws UnsupportedOperationException While canonical retrieval is not implemented.
     */
    ObservableList<AttendanceHistoryEntry> getAttendanceHistory(String studentId, String lessonId);

    /**
     * Returns the user prefs' GUI settings.
     */
    GuiSettings getGuiSettings();

    /**
     * Set the user prefs' GUI settings.
     */
    void setGuiSettings(GuiSettings guiSettings);
}
