package seedu.address.logic;

import java.util.Optional;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.query.AttendanceHistoryEntry;
import seedu.address.model.query.LessonView;
import seedu.address.model.query.StudentView;

/**
 * API of the Logic component.
 * Active lesson retrieval delegates to the canonical session's query source; legacy adapters report it as unavailable.
 * Attendance-history declarations remain reserved for their canonical integration.
 * Supplied retrieval lists are unmodifiable observable views that retain their identity across refreshes.
 * Queries preserve the people view and stored data; committed changes and rollback refresh subscribed lists.
 * Weekdays sort Monday first and lesson IDs sort by their numeric sequence, not by their display strings.
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

    /**
     * Returns the inherited implementation's unmodifiable filtered people view.
     *
     * @throws UnsupportedOperationException if this adapter owns canonical records instead.
     */
    ObservableList<Person> getFilteredPersonList();

    /**
     * Returns the current canonical people view used by cards and displayed-index resolution.
     * The same unmodifiable observable list survives filtering and committed data changes.
     *
     * @throws UnsupportedOperationException if this adapter still uses inherited person records.
     */
    default ObservableList<PersonRecord> getPersonRecords() {
        throw new UnsupportedOperationException("Canonical people records are unavailable in this legacy adapter");
    }

    /**
     * Returns every shared lesson once in creation order, including lessons with empty rosters.
     * The returned list is unmodifiable, observable and remains subscribed to committed model changes.
     * @throws UnsupportedOperationException if this adapter does not provide canonical retrieval
     */
    ObservableList<LessonView> getLessonList();

    /**
     * Returns lessons matching the active lesson filter, without changing the people view.
     * Results are distinct and retain catalogue order. A valid filter with no matches returns an empty list.
     * @throws UnsupportedOperationException if this adapter does not provide canonical retrieval
     */
    ObservableList<LessonView> getFilteredLessonList();

    /**
     * Returns the live results selected by the latest lessons or lesson-search command, initially the catalogue.
     * This read-only observable list retains its identity even when the selected query changes.
     * People filters and positions are independent of this view.
     * @throws UnsupportedOperationException if this adapter does not provide canonical retrieval
     */
    ObservableList<LessonView> getDisplayedLessons();

    /**
     * Returns a live read-only description of the selected lesson results for the UI.
     */
    ReadOnlyStringProperty lessonResultContextProperty();

    /**
     * Looks up a lesson in the complete catalogue, independently of the active lesson filter.
     * The returned projection is a snapshot of the lesson at lookup time.
     * @param lessonId stable lesson identity
     * @return the lesson, or empty if the ID is valid but no lesson exists
     * @throws NullPointerException if {@code lessonId} is null, once integrated
     * @throws UnsupportedOperationException if this adapter does not provide canonical retrieval
     */
    Optional<LessonView> findLessonById(LessonId lessonId);

    /**
     * Returns a student's current lessons in creation order. Membership comes from Lesson-owned student IDs.
     * Past attendance does not imply current membership.
     * @param studentId stable identity of an existing student
     * @throws NullPointerException if {@code studentId} is null, once integrated
     * @throws IllegalArgumentException if the ID has the wrong role or is unknown, once integrated
     * @throws UnsupportedOperationException if this adapter does not provide canonical retrieval
     */
    ObservableList<LessonView> getStudentLessons(PersonId studentId);

    /**
     * Returns the current roster in people creation order, resolved from Lesson-owned student IDs.
     * An existing lesson with no students returns an empty list.
     * @param lessonId stable identity of an existing lesson
     * @throws NullPointerException if {@code lessonId} is null, once integrated
     * @throws IllegalArgumentException if the ID is unknown, once integrated
     * @throws UnsupportedOperationException if this adapter does not provide canonical retrieval
     */
    ObservableList<StudentView> getLessonRoster(LessonId lessonId);

    /**
     * Returns the tutor's lessons, including empty lessons, by weekday, start time and lesson ID.
     * @param tutorId stable identity of an existing tutor
     * @throws NullPointerException if {@code tutorId} is null, once integrated
     * @throws IllegalArgumentException if the ID has the wrong role or is unknown, once integrated
     * @throws UnsupportedOperationException if this adapter does not provide canonical retrieval
     */
    ObservableList<LessonView> getTutorSchedule(PersonId tutorId);

    /**
     * Returns the student's recorded attendance, including entries retained after unenrolment.
     * Results are ordered by date, newest first, then by lesson ID. Missing entries are unrecorded.
     * @param studentId stable identity of an existing student
     * @throws NullPointerException if {@code studentId} is null, once integrated
     * @throws IllegalArgumentException if the ID has the wrong role or is unknown, once integrated
     * @throws UnsupportedOperationException until canonical retrieval is integrated
     */
    ObservableList<AttendanceHistoryEntry> getAttendanceHistory(PersonId studentId);

    /**
     * Returns the student's recorded attendance for one lesson, including former enrolments.
     * An existing student and lesson with no matching records produce an empty list.
     * @param studentId stable identity of an existing student
     * @param lessonId stable identity of an existing lesson
     * @throws NullPointerException if either ID is null, once integrated
     * @throws IllegalArgumentException if an ID has the wrong role or is unknown, once integrated
     * @throws UnsupportedOperationException until canonical retrieval is integrated
     */
    ObservableList<AttendanceHistoryEntry> getAttendanceHistory(PersonId studentId, LessonId lessonId);

    /**
     * Returns the user prefs' GUI settings.
     */
    GuiSettings getGuiSettings();

    /**
     * Set the user prefs' GUI settings.
     */
    void setGuiSettings(GuiSettings guiSettings);
}
