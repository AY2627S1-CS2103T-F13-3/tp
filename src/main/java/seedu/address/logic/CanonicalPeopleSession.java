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
import seedu.address.logic.commands.CanonicalListCommand;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.ExitCommand;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.PeopleListParser;
import seedu.address.logic.parser.RoleAwareAddParser;
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
 * A prepared canonical session sharing one root across protected commands, people and lesson projections.
 * This single-model-thread adapter does not activate MainApp, the legacy catalogue or the card panel.
 * Rejected startup exposes an explicitly protected empty view, never a writable replacement for rejected data.
 */
public final class CanonicalPeopleSession {
    public static final String MESSAGE_ADD_USAGE =
            "Usage: add r/student n/NAME l/LEVEL pp/PARENT_PHONE [p/PHONE] [e/EMAIL] [a/ADDRESS]\n"
            + "       add r/tutor|parent n/NAME p/PHONE [e/EMAIL] [a/ADDRESS]\n"
            + "Roles and education levels are case-insensitive. Supply each lowercase prefix at most once.\n"
            + "Optional values must be nonblank; l/ and pp/ are only allowed for students.\n"
            + "Examples:\n"
            + "add r/student n/Alex Tan l/P1 pp/91234567\n"
            + "add r/tutor n/Mei Lee p/81234567 e/mei@example.com\n"
            + "add r/parent n/Sam Tan p/91234567 a/10 Main Road";
    public static final String MESSAGE_HELP_USAGE = "Usage: help [COMMAND]\n"
            + "Shows the commands available in this session, or syntax and an example for one command.\n"
            + "COMMAND is optional and case-insensitive. Supply at most one command word.\n"
            + "Examples: help, help list";

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
        return getDataLoadError().isPresent() ? List.of("help", "list", "exit")
                : List.of("add", "list", "help", "exit");
    }

    /**
     * Routes prepared canonical commands without consulting or activating the legacy command catalogue.
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
            case "list":
                CommandResult result = executor.execute(
                        new CanonicalListCommand(new PeopleListParser().parse(arguments)));
                return loadError.map(error -> new CommandResult(result.getFeedbackToUser() + "\n\n" + error))
                        .orElse(result);
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
            StringBuilder overview = new StringBuilder("PonHub commands available in this canonical session:\n");
            for (String word : getCommandWords()) {
                overview.append(word).append(" - ").append(getUsage(word).orElseThrow()).append("\n\n");
            }
            getDataLoadError().ifPresent(error -> overview.append(error).append("\n\n"));
            return overview.append("Scroll this Result Display to read long guidance.").toString();
        }
        if (topic.chars().anyMatch(Character::isWhitespace)) {
            throw new ParseException(String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT, MESSAGE_HELP_USAGE));
        }
        return getUsage(topic).orElseThrow(() ->
                new ParseException(String.format(HelpCommand.MESSAGE_UNKNOWN_TOPIC, topic)));
    }

    private Optional<String> getUsage(String word) {
        if (!getCommandWords().contains(word)) {
            return Optional.empty();
        }
        return Optional.of(switch (word) {
            case "add" -> MESSAGE_ADD_USAGE;
            case "list" -> PeopleListParser.MESSAGE_USAGE;
            case "help" -> MESSAGE_HELP_USAGE;
            case "exit" -> ExitCommand.MESSAGE_USAGE;
            default -> throw new IllegalArgumentException("Unsupported canonical command word: " + word);
        });
    }
}
