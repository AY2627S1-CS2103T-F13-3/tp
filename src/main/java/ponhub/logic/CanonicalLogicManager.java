package ponhub.logic;

import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.collections.ObservableList;
import ponhub.commons.core.GuiSettings;
import ponhub.commons.core.LogsCenter;
import ponhub.commons.util.StringUtil;
import ponhub.logic.commands.CommandResult;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.UserPrefs;
import ponhub.model.lesson.LessonId;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.query.AttendanceHistoryEntry;
import ponhub.model.query.LessonView;
import ponhub.model.query.StudentView;
import ponhub.model.search.PersonSearchMatch;

/**
 * Application logic over one canonical session and separately persisted GUI preferences.
 * Commands, person cards, lesson retrieval and attendance history share the session's canonical data root.
 */
public final class CanonicalLogicManager implements Logic {
    private static final Pattern COMMAND_WORD = Pattern.compile("[\\p{javaWhitespace}\\p{Zs}]*(?<word>\\S+).*",
            Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS);
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
        requireAllNonNull(commandText);
        var matcher = COMMAND_WORD.matcher(StringUtil.stripUnicodeBoundarySpaces(commandText));
        String command = matcher.matches() ? CanonicalCommandDefinition.fromWord(matcher.group("word"))
                .map(CanonicalCommandDefinition::getWord).orElse("unknown") : "unknown";
        try {
            CommandResult result = session.execute(commandText);
            logger.info("Command type=" + command + " outcome=success");
            return result;
        } catch (ParseException failure) {
            logRejection(command, "parse_rejected", failure);
            throw failure;
        } catch (CommandException failure) {
            logRejection(command, "command_rejected", failure);
            throw failure;
        }
    }

    /**
     * Records cause types only: exception messages and thrown objects can contain command or file payloads.
     */
    private void logRejection(String command, String outcome, Exception failure) {
        StringJoiner causes = new StringJoiner(">");
        Level level = Level.INFO;
        Throwable cause = failure;
        // Bound traversal even if a programmer has accidentally constructed a cyclic cause chain.
        for (int depth = 0; cause != null && depth < 8; depth++, cause = cause.getCause()) {
            causes.add(cause.getClass().getSimpleName());
            if (cause instanceof java.io.IOException) {
                level = Level.WARNING;
            }
        }
        logger.log(level, "Command type=" + command + " outcome=" + outcome + " cause=" + causes);
    }

    @Override
    public List<String> getCommandWords() {
        return CanonicalCommandCatalog.getCommandWords(session.getDataLoadError().isPresent());
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
    public ReadOnlyStringProperty peopleResultContextProperty() {
        return session.peopleResultContextProperty();
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
    public ReadOnlyObjectProperty<LessonRetrieval.ResultKind> lessonResultKindProperty() {
        return session.lessonResultKindProperty();
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
