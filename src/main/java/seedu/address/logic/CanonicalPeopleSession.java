package seedu.address.logic;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.collections.ObservableList;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.util.StringUtil;
import seedu.address.logic.commands.CanonicalAddCommand;
import seedu.address.logic.commands.CanonicalAddLessonCommand;
import seedu.address.logic.commands.CanonicalDeleteCommand;
import seedu.address.logic.commands.CanonicalDeleteLessonCommand;
import seedu.address.logic.commands.CanonicalEnrolCommand;
import seedu.address.logic.commands.CanonicalListCommand;
import seedu.address.logic.commands.CanonicalMarkAttendanceCommand;
import seedu.address.logic.commands.CanonicalSearchCommand;
import seedu.address.logic.commands.CanonicalUnenrolCommand;
import seedu.address.logic.commands.CanonicalUnmarkAttendanceCommand;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.ExitCommand;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.AddLessonParser;
import seedu.address.logic.parser.AttendanceMarkParser;
import seedu.address.logic.parser.AttendanceUnmarkParser;
import seedu.address.logic.parser.DeleteLessonParser;
import seedu.address.logic.parser.HistoryArgumentsParser;
import seedu.address.logic.parser.LessonMembershipParser;
import seedu.address.logic.parser.LessonsArgumentsParser;
import seedu.address.logic.parser.ParserUtil;
import seedu.address.logic.parser.PeopleListParser;
import seedu.address.logic.parser.RoleAwareAddParser;
import seedu.address.logic.parser.SearchCriteriaParser;
import seedu.address.logic.parser.ShowLessonParser;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.PeopleView;
import seedu.address.model.PonHubData;
import seedu.address.model.PonHubDataState;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;
import seedu.address.model.query.AttendanceHistoryEntry;
import seedu.address.model.query.LessonQueries;
import seedu.address.model.query.LessonQuerySource;
import seedu.address.model.query.LessonView;
import seedu.address.model.search.PersonSearchMatch;
import seedu.address.model.search.SearchCategory;
import seedu.address.model.search.SearchCriteria;
import seedu.address.storage.ProtectedPonHubDataStorage;

/**
 * The canonical application session sharing one root across protected commands, people and lesson projections.
 * Command execution and attached controls use the single model/JavaFX application thread.
 * Rejected startup exposes an explicitly protected empty view, never a writable replacement for rejected data.
 */
public final class CanonicalPeopleSession {
    public static final String MESSAGE_ADD_USAGE = CanonicalCommandCatalog.MESSAGE_ADD_USAGE;
    public static final String MESSAGE_ADD_LESSON_USAGE = CanonicalCommandCatalog.MESSAGE_ADD_LESSON_USAGE;
    public static final String MESSAGE_ENROL_USAGE = CanonicalCommandCatalog.MESSAGE_ENROL_USAGE;
    public static final String MESSAGE_UNENROL_USAGE = CanonicalCommandCatalog.MESSAGE_UNENROL_USAGE;
    public static final String MESSAGE_MARK_USAGE = CanonicalCommandCatalog.MESSAGE_MARK_USAGE;
    public static final String MESSAGE_UNMARK_USAGE = CanonicalCommandCatalog.MESSAGE_UNMARK_USAGE;
    public static final String MESSAGE_HISTORY_USAGE = CanonicalCommandCatalog.MESSAGE_HISTORY_USAGE;
    public static final String MESSAGE_DELETE_USAGE = CanonicalCommandCatalog.MESSAGE_DELETE_USAGE;
    public static final String MESSAGE_SHOW_LESSON_USAGE = CanonicalCommandCatalog.MESSAGE_SHOW_LESSON_USAGE;
    public static final String MESSAGE_DELETE_LESSON_USAGE = CanonicalCommandCatalog.MESSAGE_DELETE_LESSON_USAGE;
    public static final String MESSAGE_HELP_USAGE = CanonicalCommandCatalog.MESSAGE_HELP_USAGE;
    public static final String MESSAGE_UNKNOWN_COMMAND_FORMAT =
            "Unknown command: '%s'. Type 'help' to see available commands.";

    private static final Pattern BASIC_COMMAND_FORMAT =
            Pattern.compile("(?<commandWord>\\S+)(?<arguments>.*)", Pattern.DOTALL);
    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\R");

    private final ProtectedPonHubDataStorage storage;
    private final PonHubData data;
    private final PeopleView view;
    private final LessonQuerySource lessonQueries;
    private final LessonRetrieval lessonRetrieval;
    private final AttendanceRetrieval attendanceRetrieval;
    private final CanonicalCommandExecutor executor;

    /**
     * Loads the configured canonical path once and connects its protected snapshot writer.
     * Missing data remains unwritten until the first successful mutation.
     */
    public CanonicalPeopleSession(Path dataPath) {
        this(new ProtectedPonHubDataStorage(dataPath));
    }

    private CanonicalPeopleSession(ProtectedPonHubDataStorage storage) {
        this(storage, storage::saveData);
    }

    /**
     * Connects a snapshot writer seam while retaining real startup validation and session protection.
     * The writer must persist the complete snapshot or throw, without mutating or reentering this session.
     */
    CanonicalPeopleSession(ProtectedPonHubDataStorage storage, CanonicalCommandExecutor.SnapshotWriter writer) {
        requireAllNonNull(storage, writer);
        this.storage = storage;
        data = new PonHubData();
        try {
            data.resetData(storage.load());
        } catch (DataLoadingException failure) {
            // Storage retains the authoritative rejection guidance and a session-long write lock.
        }
        LessonQueries queries = new LessonQueries(data);
        view = new PeopleView(data, queries);
        lessonQueries = queries;
        lessonRetrieval = new LessonRetrieval(queries, view, data);
        attendanceRetrieval = new AttendanceRetrieval(queries, view, data);
        executor = new CanonicalCommandExecutor(view, writer, storage::getDataLoadError);
    }

    public ObservableList<PersonRecord> getPeople() {
        return view.getPeople();
    }

    public ReadOnlyObjectProperty<Map<PersonId, List<PersonSearchMatch>>> personSearchContextProperty() {
        return view.searchContextProperty();
    }

    public Optional<PersonRole> getRoleFilter() {
        return view.getRoleFilter();
    }

    public PonHubDataState getDataSnapshot() {
        return data.exportState();
    }

    public LessonQuerySource getLessonQueries() {
        return lessonQueries;
    }

    public ObservableList<LessonView> getDisplayedLessons() {
        return lessonRetrieval.getDisplayedLessons();
    }

    public ReadOnlyStringProperty lessonResultContextProperty() {
        return lessonRetrieval.contextProperty();
    }

    public ObservableList<AttendanceHistoryEntry> getDisplayedHistory() {
        return attendanceRetrieval.getDisplayedHistory();
    }

    public ReadOnlyStringProperty historyResultContextProperty() {
        return attendanceRetrieval.contextProperty();
    }

    public ReadOnlyBooleanProperty historyViewActiveProperty() {
        return attendanceRetrieval.activeProperty();
    }

    public ProtectedPonHubDataStorage.LoadStatus getLoadStatus() {
        return storage.getLoadStatus();
    }

    public Optional<String> getDataLoadError() {
        return storage.getDataLoadError();
    }

    public Path getDataFilePath() {
        return storage.getDataFilePath();
    }

    /**
     * Returns only the routes available in this session, in help overview order.
     */
    public List<String> getCommandWords() {
        return CanonicalCommandCatalog.getCommandWords(getDataLoadError().isPresent());
    }

    /**
     * Routes supported canonical commands using this session's authoritative command catalogue.
     * Protection is checked before mutation parsing; read commands never persist operational data.
     *
     * @throws ParseException if a supported command has invalid arguments or an unprotected command is unknown.
     * @throws CommandException if startup protection, domain validation or persistence rejects the command.
     */
    public CommandResult execute(String commandText) throws ParseException, CommandException {
        requireNonNull(commandText);
        Matcher matcher = BASIC_COMMAND_FORMAT.matcher(StringUtil.trimSpaces(commandText.stripLeading()));
        if (!matcher.matches()) {
            throw new ParseException(String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT, MESSAGE_HELP_USAGE));
        }
        String word = matcher.group("commandWord");
        String arguments = matcher.group("arguments");
        Optional<String> loadError = getDataLoadError();
        if (loadError.isPresent() && !getCommandWords().contains(word)) {
            throw new CommandException(loadError.get());
        }
        if (LINE_BREAK_PATTERN.matcher(commandText).find()) {
            throw new ParseException("Commands must be on one line.");
        }

        switch (word) {
            case "add":
                return executor.execute(new CanonicalAddCommand(new RoleAwareAddParser().parse(arguments)));
            case "addlesson":
                return executor.execute(new CanonicalAddLessonCommand(new AddLessonParser().parse(arguments)));
            case "enrol":
                return executor.execute(new CanonicalEnrolCommand(new LessonMembershipParser().parse(arguments)));
            case "unenrol":
                return executor.execute(new CanonicalUnenrolCommand(new LessonMembershipParser().parse(arguments)));
            case "mark":
                return executor.execute(new CanonicalMarkAttendanceCommand(new AttendanceMarkParser().parse(
                        arguments)));
            case "unmark":
                return executor.execute(new CanonicalUnmarkAttendanceCommand(new AttendanceUnmarkParser().parse(
                        arguments)));
            case "history":
                return new CommandResult(String.format("Showing %d attendance record(s).",
                        attendanceRetrieval.show(new HistoryArgumentsParser().parse(arguments)).size()));
            case "delete":
                return executor.execute(new CanonicalDeleteCommand(ParserUtil.parseIndex(arguments)));
            case "deletelesson":
                return executor.execute(new CanonicalDeleteLessonCommand(new DeleteLessonParser().parse(arguments)));
            case "list":
                CommandResult result = executor.execute(
                        new CanonicalListCommand(new PeopleListParser().parse(arguments)));
                return loadError.map(error -> new CommandResult(result.getFeedbackToUser() + "\n\n" + error))
                        .orElse(result);
            case "search":
                SearchCriteria criteria = new SearchCriteriaParser().parse(arguments);
                if (criteria.getCategory() == SearchCategory.LESSON) {
                    int count = lessonRetrieval.search(criteria).size();
                    attendanceRetrieval.hide();
                    return new CommandResult(String.format("Found %d lesson(s).", count));
                }
                return new CanonicalSearchCommand(criteria).execute(data, view);
            case "lessons":
                int lessonCount = lessonRetrieval.list(new LessonsArgumentsParser().parse(arguments)).size();
                attendanceRetrieval.hide();
                return new CommandResult(String.format("Showing %d lesson(s).", lessonCount));
            case "showlesson":
                var detail = lessonRetrieval.show(new ShowLessonParser().parse(arguments));
                attendanceRetrieval.hide();
                return new CommandResult("Showing lesson " + detail.getLessonId()
                        + detail.date().map(date -> " for " + date).orElse("") + ".");
            case "help":
                return new CommandResult(getHelp(arguments));
            case "exit":
                if (!arguments.isBlank()) {
                    throw new ParseException(String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT,
                            ExitCommand.MESSAGE_USAGE));
                }
                return new CommandResult(ExitCommand.MESSAGE_EXIT_ACKNOWLEDGEMENT, false, true);
            default:
                throw new ParseException(String.format(MESSAGE_UNKNOWN_COMMAND_FORMAT, word));
        }
    }

    /**
     * Resolves help against this session's routes, including its protected startup restrictions.
     */
    private String getHelp(String arguments) throws ParseException {
        String topic = arguments.strip().toLowerCase(Locale.ROOT);
        if (topic.isEmpty()) {
            return CanonicalCommandCatalog.getOverview(getDataLoadError());
        }
        if (topic.chars().anyMatch(Character::isWhitespace)) {
            throw new ParseException(String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT, MESSAGE_HELP_USAGE));
        }
        return CanonicalCommandCatalog.getUsage(topic, getDataLoadError().isPresent()).orElseThrow(() ->
                new ParseException(String.format(HelpCommand.MESSAGE_UNKNOWN_TOPIC, topic)));
    }
}
