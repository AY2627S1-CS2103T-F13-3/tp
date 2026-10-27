package ponhub.logic;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.nio.file.Path;
import java.time.Clock;
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
import ponhub.commons.exceptions.DataLoadingException;
import ponhub.commons.util.StringUtil;
import ponhub.logic.commands.CanonicalAddCommand;
import ponhub.logic.commands.CanonicalAddLessonCommand;
import ponhub.logic.commands.CanonicalDeleteCommand;
import ponhub.logic.commands.CanonicalDeleteLessonCommand;
import ponhub.logic.commands.CanonicalEditCommand;
import ponhub.logic.commands.CanonicalEditLessonCommand;
import ponhub.logic.commands.CanonicalEnrolCommand;
import ponhub.logic.commands.CanonicalListCommand;
import ponhub.logic.commands.CanonicalMarkAttendanceCommand;
import ponhub.logic.commands.CanonicalSearchCommand;
import ponhub.logic.commands.CanonicalUnenrolCommand;
import ponhub.logic.commands.CanonicalUnmarkAttendanceCommand;
import ponhub.logic.commands.CommandResult;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.AddLessonParser;
import ponhub.logic.parser.AttendanceMarkParser;
import ponhub.logic.parser.AttendanceUnmarkParser;
import ponhub.logic.parser.DeleteLessonParser;
import ponhub.logic.parser.HistoryArgumentsParser;
import ponhub.logic.parser.LessonEditParser;
import ponhub.logic.parser.LessonMembershipParser;
import ponhub.logic.parser.LessonsArgumentsParser;
import ponhub.logic.parser.ParserUtil;
import ponhub.logic.parser.PeopleListParser;
import ponhub.logic.parser.PersonEditParser;
import ponhub.logic.parser.RoleAwareAddParser;
import ponhub.logic.parser.SearchCriteriaParser;
import ponhub.logic.parser.ShowLessonParser;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.PonHubDataState;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.PersonRole;
import ponhub.model.query.AttendanceHistoryEntry;
import ponhub.model.query.LessonQueries;
import ponhub.model.query.LessonQuerySource;
import ponhub.model.query.LessonView;
import ponhub.model.search.PersonSearchMatch;
import ponhub.model.search.SearchCategory;
import ponhub.model.search.SearchCriteria;
import ponhub.storage.ProtectedPonHubDataStorage;

/**
 * The canonical application session sharing one root across protected commands, people and lesson projections.
 * Command execution and attached controls use the single model/JavaFX application thread.
 * Rejected startup exposes an explicitly protected empty view, never a writable replacement for rejected data.
 */
public final class CanonicalPeopleSession {
    public static final String MESSAGE_ADD_USAGE = CanonicalCommandCatalog.MESSAGE_ADD_USAGE;
    public static final String MESSAGE_ADD_LESSON_USAGE = CanonicalCommandCatalog.MESSAGE_ADD_LESSON_USAGE;
    public static final String MESSAGE_EDIT_LESSON_USAGE = CanonicalCommandCatalog.MESSAGE_EDIT_LESSON_USAGE;
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
    private final Clock clock;
    private PersonRecord pendingDeletion;

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
        this(storage, writer, Clock.systemDefaultZone());
    }

    /**
     * Connects a snapshot writer and clock seam for deterministic command integration tests.
     */
    CanonicalPeopleSession(ProtectedPonHubDataStorage storage, CanonicalCommandExecutor.SnapshotWriter writer,
            Clock clock) {
        requireAllNonNull(storage, writer, clock);
        this.storage = storage;
        this.clock = clock;
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

    /**
     * Returns the selected lesson source independently of people selection and the visible history pane.
     */
    public ReadOnlyObjectProperty<LessonRetrieval.ResultKind> lessonResultKindProperty() {
        return lessonRetrieval.resultKindProperty();
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
        // Consume before parsing: even rejected or empty input must not leave an old confirmation armed.
        PersonRecord confirmationTarget = pendingDeletion;
        pendingDeletion = null;
        Matcher matcher = BASIC_COMMAND_FORMAT.matcher(StringUtil.trimSpaces(commandText.stripLeading()));
        if (!matcher.matches()) {
            throw new ParseException(String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT, MESSAGE_HELP_USAGE));
        }
        String word = matcher.group("commandWord");
        String arguments = matcher.group("arguments");
        Optional<CanonicalCommandDefinition> definition = CanonicalCommandDefinition.fromWord(word);
        Optional<String> loadError = getDataLoadError();
        if (loadError.isPresent()
                && definition.filter(CanonicalCommandDefinition::isAllowedWhenProtected).isEmpty()) {
            throw new CommandException(loadError.get());
        }
        if (LINE_BREAK_PATTERN.matcher(commandText).find()) {
            throw new ParseException("Commands must be on one line.");
        }

        CanonicalCommandDefinition command = definition.orElseThrow(() ->
                new ParseException(String.format(MESSAGE_UNKNOWN_COMMAND_FORMAT, word)));
        return switch (command) {
            case ADD -> executor.execute(new CanonicalAddCommand(new RoleAwareAddParser().parse(arguments)));
            case EDIT -> executor.execute(new CanonicalEditCommand(new PersonEditParser().parse(arguments)));
            case ADD_LESSON -> executor.execute(new CanonicalAddLessonCommand(new AddLessonParser().parse(arguments)));
            case EDIT_LESSON -> executor.execute(new CanonicalEditLessonCommand(
                    new LessonEditParser().parse(arguments)));
            case ENROL -> executor.execute(new CanonicalEnrolCommand(new LessonMembershipParser().parse(arguments)));
            case UNENROL -> executor.execute(new CanonicalUnenrolCommand(
                    new LessonMembershipParser().parse(arguments)));
            case MARK -> executor.execute(new CanonicalMarkAttendanceCommand(new AttendanceMarkParser().parse(
                    arguments), clock));
            case UNMARK -> executor.execute(new CanonicalUnmarkAttendanceCommand(new AttendanceUnmarkParser().parse(
                    arguments)));
            case HISTORY -> new CommandResult(String.format("Showing %d attendance record(s).",
                    attendanceRetrieval.show(new HistoryArgumentsParser().parse(arguments)).size()));
            case DELETE -> executeDelete(arguments, confirmationTarget);
            case DELETE_LESSON -> executor.execute(new CanonicalDeleteLessonCommand(
                    new DeleteLessonParser().parse(arguments)));
            case LIST -> {
                CommandResult result = executor.execute(
                        new CanonicalListCommand(new PeopleListParser().parse(arguments)));
                yield loadError.map(error -> new CommandResult(result.getFeedbackToUser() + "\n\n" + error))
                        .orElse(result);
            }
            case SEARCH -> {
                SearchCriteria criteria = new SearchCriteriaParser().parse(arguments);
                if (criteria.getCategory() == SearchCategory.LESSON) {
                    int count = lessonRetrieval.search(criteria).size();
                    attendanceRetrieval.hide();
                    yield new CommandResult(String.format("Found %d lesson(s).", count));
                }
                yield new CanonicalSearchCommand(criteria).execute(data, view);
            }
            case LESSONS -> {
                int lessonCount = lessonRetrieval.list(new LessonsArgumentsParser().parse(arguments)).size();
                attendanceRetrieval.hide();
                yield new CommandResult(String.format("Showing %d lesson(s).", lessonCount));
            }
            case SHOW_LESSON -> {
                var detail = lessonRetrieval.show(new ShowLessonParser().parse(arguments));
                attendanceRetrieval.hide();
                yield new CommandResult("Showing lesson " + detail.getLessonId()
                        + detail.date().map(date -> " for " + date).orElse("") + ".");
            }
            case HELP -> new CommandResult(getHelp(arguments));
            case EXIT -> {
                if (!arguments.isBlank()) {
                    throw new ParseException(String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT,
                            CanonicalCommandCatalog.MESSAGE_EXIT_USAGE));
                }
                yield new CommandResult(CanonicalCommandCatalog.MESSAGE_EXIT_ACKNOWLEDGEMENT, true);
            }
        };
    }

    /**
     * Previews an eligible current-index target without publishing or saving, or consumes its confirmation.
     * Every intervening command cancels the pending request; a failed save also requires a fresh preview.
     */
    private CommandResult executeDelete(String arguments, PersonRecord confirmationTarget)
            throws ParseException, CommandException {
        String action = arguments.strip();
        if (action.equals("confirm")) {
            if (confirmationTarget == null) {
                throw new CommandException("No person deletion is awaiting confirmation. Run delete INDEX first.");
            }
            return executor.execute(new CanonicalDeleteCommand(confirmationTarget));
        }
        if (action.equals("cancel")) {
            return new CommandResult(confirmationTarget == null ? "No person deletion is awaiting confirmation."
                    : "Person deletion cancelled. No records were changed.");
        }
        PersonRecord person = PersonIndexResolver.resolve(ParserUtil.parseIndex(arguments), view.getPeople());
        // Reuse the aggregate's guards on a disposable snapshot; the live root and views stay untouched.
        try {
            new PonHubData(data.exportState()).removePerson(person.getId());
        } catch (IllegalArgumentException failure) {
            throw new CommandException(failure.getMessage(), failure);
        }
        pendingDeletion = person;
        return new CommandResult(String.format("Delete %s: %s (%s)? This permanently removes the record.\n"
                + "Enter delete confirm to proceed or delete cancel to keep it.\n"
                + "Any other command cancels this request.", person.getRole().getValue(),
                person.getContactDetails().getName(), person.getId()));
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
                new ParseException(String.format(CanonicalCommandCatalog.MESSAGE_UNKNOWN_HELP_TOPIC, topic)));
    }
}
