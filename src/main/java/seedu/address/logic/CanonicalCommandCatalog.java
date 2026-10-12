package seedu.address.logic;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;

import seedu.address.logic.commands.ExitCommand;
import seedu.address.logic.parser.PeopleListParser;

/**
 * Authoritative command words and guidance for the canonical application's supported routes.
 * Rejected startup permits only recovery commands; inherited command parsers are a separate implementation.
 */
public final class CanonicalCommandCatalog {
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
    public static final String MESSAGE_SEARCH_USAGE =
            "Usage: search c/student [n/NAME] [l/LEVEL] [p/PHONE] [pp/PARENT_PHONE] [e/EMAIL] [a/ADDRESS]\n"
            + "       search c/tutor|parent [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS]\n"
            + "Category values ignore case. Supply each lowercase prefix at most once, with a nonblank value.\n"
            + "All filters must match the same person. Name, email and address match text fragments ignoring case;\n"
            + "name/address spaces are normalized. Level and phones match exactly; phone leading zeros matter.\n"
            + "Category-only searches include unlinked people. Missing optional contacts do not match filters.\n"
            + "Results replace the people view and renumber current positions without saving.\n"
            + "Relationship filters and c/lesson are not available yet. Use list to clear search filters.\n"
            + "Examples: search c/student, search c/student n/Alex l/P1 pp/91234567, search c/tutor n/Mei";

    private static final Entry ADD = new Entry("add", "Add a student, tutor or parent", MESSAGE_ADD_USAGE);
    private static final Entry LIST = new Entry("list", "Show all people or one role", PeopleListParser.MESSAGE_USAGE);
    private static final Entry SEARCH = new Entry("search", "Find people by their own contact fields",
            MESSAGE_SEARCH_USAGE);
    private static final Entry HELP = new Entry("help", "Show help or one command's guidance", MESSAGE_HELP_USAGE);
    private static final Entry EXIT = new Entry("exit", "Close PonHub", ExitCommand.MESSAGE_USAGE);
    private static final List<Entry> ENTRIES = List.of(ADD, LIST, SEARCH, HELP, EXIT);
    private static final List<Entry> PROTECTED_ENTRIES = List.of(HELP, LIST, EXIT);

    private CanonicalCommandCatalog() {
    }

    /**
     * Returns supported command words in help order, applying the protected session's restrictions.
     */
    public static List<String> getCommandWords(boolean isProtected) {
        return getEntries(isProtected).stream().map(Entry::word).toList();
    }

    /**
     * Returns guidance only for an exact lowercase command word allowed in this session.
     */
    public static Optional<String> getUsage(String word, boolean isProtected) {
        requireNonNull(word);
        return getEntries(isProtected).stream().filter(entry -> entry.word().equals(word))
                .map(Entry::usage).findFirst();
    }

    /**
     * Returns the supported command overview and retained startup recovery guidance when applicable.
     */
    public static String getOverview(Optional<String> loadError) {
        requireNonNull(loadError);
        StringBuilder overview = new StringBuilder("PonHub commands available in this build:\n");
        getEntries(loadError.isPresent()).forEach(entry -> overview.append(entry.word()).append(" - ")
                .append(entry.description()).append('\n').append(entry.usage()).append("\n\n"));
        overview.append("\nType help COMMAND for syntax and an example, e.g. help list.\n\n");
        loadError.ifPresent(error -> overview.append(error).append("\n\n"));
        return overview.append("Scroll this Result Display to read long guidance.").toString();
    }

    /**
     * Returns the command definitions permitted by the startup protection state.
     */
    private static List<Entry> getEntries(boolean isProtected) {
        return isProtected ? PROTECTED_ENTRIES : ENTRIES;
    }

    /**
     * Holds one supported command's user guidance.
     */
    private record Entry(String word, String description, String usage) {
    }
}
