package seedu.address.logic;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.UserPrefs;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.query.AttendanceHistoryEntry;
import seedu.address.model.query.LessonView;
import seedu.address.model.query.StudentView;
import seedu.address.model.search.PersonSearchMatch;

/**
 * Application logic over one canonical session and separately persisted GUI preferences.
 * Commands, person cards, lesson retrieval and attendance history share the session's canonical data root.
 */
public final class CanonicalLogicManager implements Logic {
    private final Logger logger = LogsCenter.getLogger(CanonicalLogicManager.class);

    private final CanonicalPeopleSession session;
    private final UserPrefs userPrefs;

    /**
     * Connects the application's canonical session and the shared preferences saved independently at shutdown.
     */
    public CanonicalLogicManager(CanonicalPeopleSession session, UserPrefs userPrefs) {
        requireAllNonNull(session, userPrefs);
        this.session = session;
        this.userPrefs = userPrefs;
    }

    @Override
    public CommandResult execute(String commandText) throws CommandException, ParseException {
        logger.info("----------------[USER COMMAND][" + commandText + "]");
        return session.execute(commandText);
    }

    @Override
    public ObservableList<PersonRecord> getPersonRecords() {
        return session.getPeople();
    }

    @Override
    public ReadOnlyObjectProperty<Map<PersonId, List<PersonSearchMatch>>> personSearchContextProperty() {
        return session.personSearchContextProperty();
    }

    @Override
    public ObservableList<LessonView> getLessonList() {
        return session.getLessonQueries().getLessonList();
    }

    @Override
    public ObservableList<LessonView> getFilteredLessonList() {
        return session.getLessonQueries().getFilteredLessonList();
    }

    @Override
    public ObservableList<LessonView> getDisplayedLessons() {
        return session.getDisplayedLessons();
    }

    @Override
    public ReadOnlyStringProperty lessonResultContextProperty() {
        return session.lessonResultContextProperty();
    }

    @Override
    public Optional<LessonView> findLessonById(LessonId lessonId) {
        return session.getLessonQueries().findLessonById(lessonId);
    }

    @Override
    public ObservableList<LessonView> getStudentLessons(PersonId studentId) {
        return session.getLessonQueries().getStudentLessons(studentId);
    }

    @Override
    public ObservableList<StudentView> getLessonRoster(LessonId lessonId) {
        return session.getLessonQueries().getLessonRoster(lessonId);
    }

    @Override
    public ObservableList<LessonView> getTutorSchedule(PersonId tutorId) {
        return session.getLessonQueries().getTutorSchedule(tutorId);
    }

    @Override
    public ObservableList<AttendanceHistoryEntry> getAttendanceHistory(PersonId studentId) {
        return session.getLessonQueries().getAttendanceHistory(studentId);
    }

    @Override
    public ObservableList<AttendanceHistoryEntry> getAttendanceHistory(PersonId studentId, LessonId lessonId) {
        return session.getLessonQueries().getAttendanceHistory(studentId, lessonId);
    }

    @Override
    public ObservableList<AttendanceHistoryEntry> getDisplayedHistory() {
        return session.getDisplayedHistory();
    }

    @Override
    public ReadOnlyStringProperty historyResultContextProperty() {
        return session.historyResultContextProperty();
    }

    @Override
    public ReadOnlyBooleanProperty historyViewActiveProperty() {
        return session.historyViewActiveProperty();
    }

    @Override
    public GuiSettings getGuiSettings() {
        return userPrefs.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        userPrefs.setGuiSettings(guiSettings);
    }
}
