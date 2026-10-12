package seedu.address.logic;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.collections.ObservableList;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.util.StringUtil;
import seedu.address.logic.commands.CanonicalAddCommand;
import seedu.address.logic.commands.CanonicalAddLessonCommand;
import seedu.address.logic.commands.CanonicalListCommand;
import seedu.address.logic.commands.CanonicalSearchCommand;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.ExitCommand;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.AddLessonParser;
import seedu.address.logic.parser.PeopleListParser;
import seedu.address.logic.parser.RoleAwareAddParser;
import seedu.address.logic.parser.SearchCriteriaParser;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.PeopleView;
import seedu.address.model.PonHubData;
import seedu.address.model.PonHubDataState;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;
import seedu.address.model.query.LessonQueries;
import seedu.address.model.query.LessonQuerySource;
import seedu.address.storage.ProtectedPonHubDataStorage;

/**
 * The canonical application session sharing one root across protected commands, people and lesson projections.
 * Command execution and attached controls use the single model/JavaFX application thread.
 * Rejected startup exposes an explicitly protected empty view, never a writable replacement for rejected data.
 */
public final class CanonicalPeopleSession {
    public static final String MESSAGE_ADD_USAGE = CanonicalCommandCatalog.MESSAGE_ADD_USAGE;
    public static final String MESSAGE_ADD_LESSON_USAGE = CanonicalCommandCatalog.MESSAGE_ADD_LESSON_USAGE;
    public static final String MESSAGE_HELP_USAGE = CanonicalCommandCatalog.MESSAGE_HELP_USAGE;

    private static final Pattern BASIC_COMMAND_FORMAT =
            Pattern.compile("(?<commandWord>\\S+)(?<arguments>.*)", Pattern.DOTALL);
    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\R");

    private final ProtectedPonHubDataStorage storage;
    private final PonHubData data;
    private final PeopleView view;
    private final LessonQuerySource lessonQueries;
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
        view = new PeopleView(data);
        lessonQueries = new LessonQueries(data);
        executor = new CanonicalCommandExecutor(view, writer, storage::getDataLoadError);
    }

    public ObservableList<PersonRecord> getPeople() {
        return view.getPeople();
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
            case "list":
                CommandResult result = executor.execute(
                        new CanonicalListCommand(new PeopleListParser().parse(arguments)));
                return loadError.map(error -> new CommandResult(result.getFeedbackToUser() + "\n\n" + error))
                        .orElse(result);
            case "search":
                return executor.execute(new CanonicalSearchCommand(new SearchCriteriaParser().parse(arguments)));
            case "help":
                return new CommandResult(getHelp(arguments));
            case "exit":
                if (!arguments.isBlank()) {
                    throw new ParseException(String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT,
                            ExitCommand.MESSAGE_USAGE));
                }
                return new CommandResult(ExitCommand.MESSAGE_EXIT_ACKNOWLEDGEMENT, false, true);
            default:
                throw new ParseException(Messages.MESSAGE_UNKNOWN_COMMAND);
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
