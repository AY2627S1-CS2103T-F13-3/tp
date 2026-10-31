package ponhub.logic;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.collections.ObservableList;
import ponhub.commons.core.GuiSettings;
import ponhub.logic.commands.CommandResult;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.lesson.LessonId;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.query.AttendanceHistoryEntry;
import ponhub.model.query.LessonView;
import ponhub.model.query.StudentView;
import ponhub.model.search.PersonSearchMatch;

/**
 * API of the Logic component.
 * People, lesson and attendance retrieval share the canonical session's query source.
 * Supplied retrieval lists are unmodifiable observable views that retain their identity across refreshes.
 * Queries preserve the people view and stored data; committed changes and rollback refresh subscribed lists.
 * Weekdays sort Monday first and lesson IDs sort by their numeric sequence, not by their display strings.
 */
public interface Logic {
    /**
     * Executes the command and returns the result.
     *
     * @param commandText The command as entered by the user.
     * @return the result of the command execution.
     * @throws CommandException If an error occurs during command execution.
     * @throws ParseException If an error occurs during parsing.
     */
    CommandResult execute(String commandText) throws CommandException, ParseException;

    /**
     * Returns immutable command words in catalogue order, restricted to this session's available routes.
     */
    List<String> getCommandWords();

    /**
     * Returns the current canonical people view used by cards and displayed-index resolution.
     * The same unmodifiable observable list survives filtering and committed data changes.
     */
    ObservableList<PersonRecord> getPersonRecords();

    /**
     * Returns every shared lesson once in creation order, including lessons with empty rosters.
     * The returned list is unmodifiable, observable and remains subscribed to committed model changes.
     */
    ObservableList<LessonView> getLessonList();

    /**
     * Returns immutable matching relationship context for the current people search, keyed by stable person ID.
     */
    ReadOnlyObjectProperty<Map<PersonId, List<PersonSearchMatch>>> personSearchContextProperty();

    /**
     * Returns the persistent people scope and normalized active search criteria for the UI.
     */
    ReadOnlyStringProperty peopleResultContextProperty();

    /**
     * Returns lessons matching the active lesson filter, without changing the people view.
     * Results are distinct and use the requested order; catalogue order is the default.
     * A valid filter with no matches returns an empty list.
     */
    ObservableList<LessonView> getFilteredLessonList();

    /**
     * Returns the live results selected by the latest lessons or lesson-search command, initially the catalogue.
     * This read-only observable list retains its identity even when the selected query changes.
     * People filters and positions are independent of this view.
     */
    ObservableList<LessonView> getDisplayedLessons();

    /**
     * Returns a live read-only description of the selected lesson results for the UI.
     */
    ReadOnlyStringProperty lessonResultContextProperty();

    /**
     * Returns the authoritative selected lesson source, retained while attendance history is visible.
     */
    ReadOnlyObjectProperty<LessonRetrieval.ResultKind> lessonResultKindProperty();

    /**
     * Looks up a lesson in the complete catalogue, independently of the active lesson filter.
     * The returned projection is a snapshot of the lesson at lookup time.
     *
     * @param lessonId stable lesson identity.
     * @return the lesson, or empty if the ID is valid but no lesson exists
     * @throws NullPointerException if {@code lessonId} is null
     */
    Optional<LessonView> findLessonById(LessonId lessonId);

    /**
     * Returns a student's current lessons in creation order. Membership comes from Lesson-owned student IDs.
     * Past attendance does not imply current membership.
     *
     * @param studentId stable identity of an existing student.
     * @throws NullPointerException if {@code studentId} is null
     * @throws IllegalArgumentException if the ID has the wrong role or is unknown
     */
    ObservableList<LessonView> getStudentLessons(PersonId studentId);

    /**
     * Returns the current roster in people creation order, resolved from Lesson-owned student IDs.
     * An existing lesson with no students returns an empty list.
     *
     * @param lessonId stable identity of an existing lesson.
     * @throws NullPointerException if {@code lessonId} is null
     * @throws IllegalArgumentException if the ID is unknown
     */
    ObservableList<StudentView> getLessonRoster(LessonId lessonId);

    /**
     * Returns the tutor's lessons, including empty lessons, by weekday, start time and lesson ID.
     *
     * @param tutorId stable identity of an existing tutor.
     * @throws NullPointerException if {@code tutorId} is null
     * @throws IllegalArgumentException if the ID has the wrong role or is unknown
     */
    ObservableList<LessonView> getTutorSchedule(PersonId tutorId);

    /**
     * Returns the student's recorded attendance, including entries retained after unenrolment.
     * Results are ordered by date, newest first, then by lesson ID. Missing entries are unrecorded.
     *
     * @param studentId stable identity of an existing student.
     * @throws NullPointerException if {@code studentId} is null
     * @throws IllegalArgumentException if the ID has the wrong role or is unknown
     */
    ObservableList<AttendanceHistoryEntry> getAttendanceHistory(PersonId studentId);

    /**
     * Returns the student's recorded attendance for one lesson, including former enrolments.
     * An existing student and lesson with no matching records produce an empty list.
     *
     * @param studentId stable identity of an existing student.
     * @param lessonId stable identity of an existing lesson.
     * @throws NullPointerException if either ID is null
     * @throws IllegalArgumentException if an ID has the wrong role or is unknown
     */
    ObservableList<AttendanceHistoryEntry> getAttendanceHistory(PersonId studentId, LessonId lessonId);

    /**
     * Returns the active attendance result list in the canonical UI.
     */
    ObservableList<AttendanceHistoryEntry> getDisplayedHistory();

    /**
     * Describes the active attendance selector.
     */
    ReadOnlyStringProperty historyResultContextProperty();

    /**
     * Indicates whether the right result pane shows attendance history.
     */
    javafx.beans.property.ReadOnlyBooleanProperty historyViewActiveProperty();

    /**
     * Returns the user prefs' GUI settings.
     */
    GuiSettings getGuiSettings();

    /**
     * Sets the user prefs' GUI settings.
     */
    void setGuiSettings(GuiSettings guiSettings);
}
