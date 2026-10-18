package seedu.address.logic;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import seedu.address.logic.parser.PeopleListParser;

/**
 * Defines canonical command words, guidance and session eligibility in normal help order.
 * Every definition must have an explicit route in the session's exhaustive dispatch.
 */
public enum CanonicalCommandDefinition {
    ADD("add", "Add a student, tutor or parent", CanonicalCommandCatalog.MESSAGE_ADD_USAGE, -1),
    ADD_LESSON("addlesson", "Create an empty-roster shared lesson",
            CanonicalCommandCatalog.MESSAGE_ADD_LESSON_USAGE, -1),
    ENROL("enrol", "Enrol a Student in a shared lesson", CanonicalCommandCatalog.MESSAGE_ENROL_USAGE, -1),
    UNENROL("unenrol", "Unenrol a Student from a shared lesson", CanonicalCommandCatalog.MESSAGE_UNENROL_USAGE,
            -1),
    MARK("mark", "Record or correct dated lesson attendance", CanonicalCommandCatalog.MESSAGE_MARK_USAGE, -1),
    UNMARK("unmark", "Remove one dated lesson attendance record", CanonicalCommandCatalog.MESSAGE_UNMARK_USAGE,
            -1),
    HISTORY("history", "Show a Student's retained attendance", CanonicalCommandCatalog.MESSAGE_HISTORY_USAGE, -1),
    DELETE("delete", "Delete an unreferenced person", CanonicalCommandCatalog.MESSAGE_DELETE_USAGE, -1),
    DELETE_LESSON("deletelesson", "Delete an unreferenced shared lesson",
            CanonicalCommandCatalog.MESSAGE_DELETE_LESSON_USAGE, -1),
    LIST("list", "Show all people or one role", PeopleListParser.MESSAGE_USAGE, 1),
    LESSONS("lessons", "Show shared lessons or one student's enrolments",
            CanonicalCommandCatalog.MESSAGE_LESSONS_USAGE, -1),
    SHOW_LESSON("showlesson", "Show one lesson's roster and dated attendance",
            CanonicalCommandCatalog.MESSAGE_SHOW_LESSON_USAGE, -1),
    SEARCH("search", "Find people or shared lessons", CanonicalCommandCatalog.MESSAGE_SEARCH_USAGE, -1),
    HELP("help", "Show help or one command's guidance", CanonicalCommandCatalog.MESSAGE_HELP_USAGE, 0),
    EXIT("exit", "Close PonHub", CanonicalCommandCatalog.MESSAGE_EXIT_USAGE, 2);

    private final String word;
    private final String description;
    private final String usage;
    private final int protectedOrder;

    CanonicalCommandDefinition(String word, String description, String usage, int protectedOrder) {
        this.word = word;
        this.description = description;
        this.usage = usage;
        this.protectedOrder = protectedOrder;
    }

    public String getWord() {
        return word;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    /**
     * Returns whether this command remains available after rejected startup.
     */
    public boolean isAllowedWhenProtected() {
        return protectedOrder >= 0;
    }

    /**
     * Resolves an exact lowercase command word without changing the command's case policy.
     */
    public static Optional<CanonicalCommandDefinition> fromWord(String word) {
        requireNonNull(word);
        return Arrays.stream(values()).filter(definition -> definition.word.equals(word)).findFirst();
    }

    /**
     * Returns immutable definitions in normal or recovery help order for the session.
     */
    public static List<CanonicalCommandDefinition> forSession(boolean isProtected) {
        return Arrays.stream(values())
                .filter(definition -> !isProtected || definition.isAllowedWhenProtected())
                .sorted(Comparator.comparingInt(definition -> isProtected
                        ? definition.protectedOrder : definition.ordinal()))
                .toList();
    }
}
