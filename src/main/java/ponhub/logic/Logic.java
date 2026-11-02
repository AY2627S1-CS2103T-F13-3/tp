package ponhub.logic;

import java.util.List;
import java.util.Map;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.collections.ObservableList;
import ponhub.commons.core.GuiSettings;
import ponhub.logic.commands.CommandResult;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.query.AttendanceHistoryEntry;
import ponhub.model.query.LessonView;
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
     * Returns immutable matching relationship context for the current people search, keyed by stable person ID.
     */
    ReadOnlyObjectProperty<Map<PersonId, List<PersonSearchMatch>>> personSearchContextProperty();

    /**
     * Returns the persistent people scope and normalized active search criteria for the UI.
     */
    ReadOnlyStringProperty peopleResultContextProperty();

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
