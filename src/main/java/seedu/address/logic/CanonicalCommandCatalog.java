package seedu.address.logic;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;

/**
 * Authoritative command words and guidance for the canonical application's supported routes.
 * Rejected startup permits only recovery commands.
 */
public final class CanonicalCommandCatalog {
    public static final String MESSAGE_EDIT_USAGE =
            "Usage: edit INDEX [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [l/LEVEL] [pp/PARENT_PHONE]\n"
            + "Use the current displayed people position and at least one field; omitted fields stay unchanged.\n"
            + "Only Students accept l/ and pp/. Blank e/ or a/ clears that value; "
            + "blank p/ clears only a Student's phone.\n"
            + "Names, Student levels/parent phones and Tutor/Parent phones cannot be cleared. Role and ID stay fixed.\n"
            + "Filters are retained and search results refresh. Recheck displayed positions after editing.\n"
            + "An identical edit makes no write. Example: list r/student, then edit 1 l/P2 e/";
    public static final String MESSAGE_UNKNOWN_HELP_TOPIC =
            "Unknown command: '%s'. Type 'help' to see available commands.";
    public static final String MESSAGE_EXIT_USAGE = "Usage: exit\n"
            + "Closes PonHub. No parameters.\n"
            + "Example: exit";
    public static final String MESSAGE_EXIT_ACKNOWLEDGEMENT = "Exiting PonHub as requested ...";

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
            + "       search c/parent [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS]\n"
            + "       [sn/STUDENT_NAME] [d/DAY] [st/HHMM] [et/HHMM] [s/SUBJECT] [tu/TUTOR_NAME] [rm/ROOM]\n"
            + "       search c/tutor [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS]\n"
            + "       [sn/STUDENT_NAME] [d/DAY] [st/HHMM] [et/HHMM] [s/SUBJECT] [rm/ROOM]\n"
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
            + "Parent links use exact phone equality; sn/ and every lesson condition must match the same child.\n"
            + "Tutor conditions must match one assigned lesson and its current roster. Use n/ for tutor names.\n"
            + "Use list or lessons to restore each view.\n"
            + "Examples: search c/student n/Alex l/P1 pp/91234567, search c/lesson tu/mei d/Mon s/Math";
    public static final String MESSAGE_LESSONS_USAGE = "Usage: lessons [si/STUDENT_INDEX]\n"
            + "Shows each shared lesson once, in creation order, including empty rosters.\n"
            + "si/ is an optional positive position in the current people list and must select a student.\n"
            + "It shows only that student's current enrolments; past attendance does not imply membership.\n"
            + "Lesson cards show stable IDs, schedule, tutor, room and roster size. Scroll to see more results.\n"
            + "People filters and command positions are preserved. This command does not save data.\n"
            + "Examples: lessons; after list r/student, lessons si/1 (if position 1 is the intended student)";
    public static final String MESSAGE_SHOW_LESSON_USAGE = "Usage: showlesson lid/LESSON_ID [d/YYYY-MM-DD]\n"
            + "Shows one stable shared lesson, its tutor, schedule, room and current Student roster.\n"
            + "The two prefixes may appear in either order when a date is supplied.\n"
            + "A real date must match the lesson weekday; current members show present, absent or unrecorded.\n"
            + "Former members' retained attendance appears separately. People positions are preserved.\n"
            + "This read-only view refreshes after saved changes. Example: showlesson lid/L1 d/2026-10-05";
    public static final String MESSAGE_ADD_LESSON_USAGE =
            "Usage: addlesson d/DAY st/HHMM et/HHMM s/SUBJECT tu/TUTOR_NAME [tp/TUTOR_PHONE] rm/ROOM\n"
            + "DAY is Mon-Sun; times use four-digit 24-hour HHMM and start before end.\n"
            + "Tutor matching uses the normalized full name. Supply the exact phone to disambiguate.\n"
            + "Example: addlesson d/Mon st/1600 et/1730 s/Math tu/Mei Lim rm/R1";
    public static final String MESSAGE_EDIT_LESSON_USAGE =
            "Usage: editlesson lid/LESSON_ID [d/DAY] [st/HHMM] [et/HHMM] [s/SUBJECT] "
                    + "[tu/TUTOR_NAME] [tp/TUTOR_PHONE] [rm/ROOM]\n"
            + "Supply a stable Lesson ID and at least one field; omitted fields stay unchanged.\n"
            + "Supplied values must be nonblank; Lesson fields cannot be cleared.\n"
            + "Use tp/ only with tu/. Tutor matching uses normalized full name and optional exact phone.\n"
            + "The complete edited slot must have start before end and must not clash for its tutor, room "
                    + "or Students.\n"
            + "A weekday change cannot invalidate retained attendance; unmark incompatible records explicitly first.\n"
            + "An identical edit makes no write. Example: editlesson lid/L1 st/1630 s/Advanced Math";
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
    public static final String MESSAGE_UNMARK_USAGE =
            "Usage: unmark STUDENT_INDEX lid/LESSON_ID d/YYYY-MM-DD\n"
                    + "Removes only that dated attendance record, including retained history after unenrolment.\n"
                    + "Example: unmark 1 lid/L1 d/2026-10-05";
    public static final String MESSAGE_HISTORY_USAGE = "Usage: history STUDENT_INDEX [lid/LESSON_ID]\n"
            + "Shows retained dated attendance, newest first, then by stable Lesson ID.\n"
            + "STUDENT_INDEX selects a Student in the current people list.\n"
            + "An optional existing Lesson ID filters records.\n"
            + "A missing record is unrecorded, not absent. Current and former membership are shown separately.\n"
            + "The people list and operational data remain unchanged.\n"
            + "Example: after list r/student, history 1 lid/L1";
    public static final String MESSAGE_DELETE_USAGE = "Usage: delete INDEX\n"
            + "INDEX is the person's current position in the displayed people list.\n"
            + "Students with enrolments or retained attendance and tutors with assigned lessons cannot be deleted.\n"
            + "Deleting a separate parent preserves students' parent-phone values.\n"
            + "Example: list r/parent, then delete 1";
    public static final String MESSAGE_DELETE_LESSON_USAGE = "Usage: deletelesson lid/LESSON_ID\n"
            + "Deletes only an empty Lesson with no retained attendance. LESSON_ID is stable.\n"
            + "Unenrol every Student and unmark every attendance record before deleting the Lesson.\n"
            + "The Lesson ID is never reused. Example: deletelesson lid/L1";

    private CanonicalCommandCatalog() {
    }

    /**
     * Returns supported command words in help order, applying the protected session's restrictions.
     */
    public static List<String> getCommandWords(boolean isProtected) {
        return CanonicalCommandDefinition.forSession(isProtected).stream()
                .map(CanonicalCommandDefinition::getWord).toList();
    }

    /**
     * Returns guidance only for an exact lowercase command word allowed in this session.
     */
    public static Optional<String> getUsage(String word, boolean isProtected) {
        requireNonNull(word);
        return CanonicalCommandDefinition.fromWord(word)
                .filter(definition -> !isProtected || definition.isAllowedWhenProtected())
                .map(CanonicalCommandDefinition::getUsage);
    }

    /**
     * Returns the supported command overview and retained startup recovery guidance when applicable.
     */
    public static String getOverview(Optional<String> loadError) {
        requireNonNull(loadError);
        StringBuilder overview = new StringBuilder("PonHub commands available in this build:\n");
        CanonicalCommandDefinition.forSession(loadError.isPresent()).forEach(definition ->
                overview.append(definition.getWord()).append(" - ").append(definition.getDescription())
                        .append('\n').append(definition.getUsage()).append("\n\n"));
        overview.append("\nType help COMMAND for syntax and an example, e.g. help list.\n\n");
        loadError.ifPresent(error -> overview.append(error).append("\n\n"));
        return overview.append("Scroll this Result Display to read long guidance.").toString();
    }

}
