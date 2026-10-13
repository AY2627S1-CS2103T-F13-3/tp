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
            + "add r/tutor n/Mei Lim p/92345678 e/mei@example.com\n"
            + "add r/parent n/Sam Tan p/91234567 a/10 Main Road";
    public static final String MESSAGE_HELP_USAGE = "Usage: help [COMMAND]\n"
            + "Shows the commands available in this session, or syntax and an example for one command.\n"
            + "COMMAND is optional and case-insensitive. Supply at most one command word.\n"
            + "Examples: help, help list";
    public static final String MESSAGE_SEARCH_USAGE =
            "Usage: search c/student [n/NAME] [l/LEVEL] [p/PHONE] [pp/PARENT_PHONE] [e/EMAIL] [a/ADDRESS]\n"
            + "       [d/DAY] [st/HHMM] [et/HHMM] [s/SUBJECT] [tu/TUTOR_NAME] [rm/ROOM]\n"
            + "       search c/tutor|parent [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS]\n"
            + "       search c/lesson [sn/STUDENT_NAME] [d/DAY] [st/HHMM] [et/HHMM] [s/SUBJECT]"
            + " [tu/TUTOR_NAME] [rm/ROOM]\n"
            + "Category values ignore case. Supply each lowercase prefix at most once, with a nonblank value.\n"
            + "All filters must match the same person. Name, email and address match text fragments ignoring case;\n"
            + "name/address spaces are normalized. Level and phones match exactly; phone leading zeros matter.\n"
            + "Category-only searches include unlinked people. Missing optional contacts do not match filters.\n"
            + "People results replace the people view and renumber current positions without saving.\n"
            + "Lesson results use a separate panel and preserve people filters and positions.\n"
            + "All lesson filters must match one lesson; sn/ matches its current roster. Text ignores case;\n"
            + "tu/ matches tutor names. Day and HHMM times match exactly. When both times are given, end > start.\n"
            + "Empty lessons are included unless sn/ is supplied. Zero matches display an empty lesson panel.\n"
            + "Student lesson filters match one current enrolment and show its context below the student card.\n"
            + "Parent/Tutor relationship filters are not available yet. Use list or lessons to restore each view.\n"
            + "Examples: search c/student n/Alex l/P1 pp/91234567, search c/lesson tu/mei d/Mon s/Math";
    public static final String MESSAGE_LESSONS_USAGE = "Usage: lessons [si/STUDENT_INDEX]\n"
            + "Shows each shared lesson once, in creation order, including empty rosters.\n"
            + "si/ is an optional positive position in the current people list and must select a student.\n"
            + "It shows only that student's current enrolments; past attendance does not imply membership.\n"
            + "Lesson cards show stable IDs, schedule, tutor, room and roster size. Scroll to see more results.\n"
            + "People filters and command positions are preserved. This command does not save data.\n"
            + "Examples: lessons; after list r/student, lessons si/1 (if position 1 is the intended student)";
    public static final String MESSAGE_ADD_LESSON_USAGE =
            "Usage: addlesson d/DAY st/HHMM et/HHMM s/SUBJECT tu/TUTOR_NAME [tp/TUTOR_PHONE] rm/ROOM\n"
            + "DAY is Mon-Sun; times use four-digit 24-hour HHMM and start before end.\n"
            + "Tutor matching uses the normalized full name. Supply the exact phone to disambiguate.\n"
            + "Example: addlesson d/Mon st/1600 et/1730 s/Math tu/Mei Lim rm/R1";
    public static final String MESSAGE_ENROL_USAGE = "Usage: enrol STUDENT_INDEX lid/LESSON_ID\n"
            + "STUDENT_INDEX is the current people-list position of a Student. LESSON_ID is stable.\n"
            + "Example: enrol 1 lid/L1";
    public static final String MESSAGE_UNENROL_USAGE = "Usage: unenrol STUDENT_INDEX lid/LESSON_ID\n"
            + "Removes current membership only; retained dated attendance is unchanged.\n"
            + "Example: unenrol 1 lid/L1";
    public static final String MESSAGE_MARK_USAGE =
            "Usage: mark STUDENT_INDEX lid/LESSON_ID d/YYYY-MM-DD s/present|absent\n"
                    + "The date must be real and match the lesson weekday. New records require current enrolment;\n"
                    + "existing history can be corrected after unenrolment.\n"
                    + "Example: mark 1 lid/L1 d/2026-10-05 s/present";
    public static final String MESSAGE_DELETE_USAGE = "Usage: delete INDEX\n"
            + "INDEX is the person's current position in the displayed people list.\n"
            + "Students with enrolments or retained attendance and tutors with assigned lessons cannot be deleted.\n"
            + "Deleting a separate parent preserves students' parent-phone values.\n"
            + "Example: list r/parent, then delete 1";

    private static final Entry ADD = new Entry("add", "Add a student, tutor or parent", MESSAGE_ADD_USAGE);
    private static final Entry ADD_LESSON = new Entry("addlesson", "Create an empty-roster shared lesson",
            MESSAGE_ADD_LESSON_USAGE);
    private static final Entry ENROL = new Entry("enrol", "Enrol a Student in a shared lesson", MESSAGE_ENROL_USAGE);
    private static final Entry UNENROL = new Entry("unenrol", "Unenrol a Student from a shared lesson",
            MESSAGE_UNENROL_USAGE);
    private static final Entry MARK = new Entry("mark", "Record or correct dated lesson attendance",
            MESSAGE_MARK_USAGE);
    private static final Entry DELETE = new Entry("delete", "Delete an unreferenced person", MESSAGE_DELETE_USAGE);
    private static final Entry LIST = new Entry("list", "Show all people or one role", PeopleListParser.MESSAGE_USAGE);
    private static final Entry LESSONS = new Entry("lessons", "Show shared lessons or one student's enrolments",
            MESSAGE_LESSONS_USAGE);
    private static final Entry SEARCH = new Entry("search", "Find people or shared lessons",
            MESSAGE_SEARCH_USAGE);
    private static final Entry HELP = new Entry("help", "Show help or one command's guidance", MESSAGE_HELP_USAGE);
    private static final Entry EXIT = new Entry("exit", "Close PonHub", ExitCommand.MESSAGE_USAGE);
    private static final List<Entry> ENTRIES =
            List.of(ADD, ADD_LESSON, ENROL, UNENROL, MARK, DELETE, LIST, LESSONS, SEARCH, HELP, EXIT);
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
