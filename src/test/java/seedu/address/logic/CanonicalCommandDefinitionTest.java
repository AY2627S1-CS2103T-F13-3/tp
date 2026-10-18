package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;

public class CanonicalCommandDefinitionTest {
    @TempDir
    Path directory;

    @Test
    public void definitions_uniqueWords_resolveExactlyAndSupplyGuidance() {
        var words = new HashSet<String>();
        for (var definition : CanonicalCommandDefinition.values()) {
            String word = definition.getWord();
            assertTrue(words.add(word), word);
            assertEquals(definition, CanonicalCommandDefinition.fromWord(word).orElseThrow());
            assertTrue(CanonicalCommandDefinition.fromWord(word.toUpperCase(Locale.ROOT)).isEmpty());
            assertFalse(definition.getDescription().isBlank());
            assertEquals(definition.getUsage(), CanonicalCommandCatalog.getUsage(word, false).orElseThrow());
            assertEquals(definition.isAllowedWhenProtected(), CanonicalCommandCatalog.getUsage(word, true).isPresent());
        }
        assertTrue(CanonicalCommandDefinition.fromWord("unknown").isEmpty());
        assertThrows(NullPointerException.class, () -> CanonicalCommandDefinition.fromWord(null));
        assertThrows(UnsupportedOperationException.class, () -> CanonicalCommandDefinition.forSession(false).clear());
        assertThrows(UnsupportedOperationException.class, () -> CanonicalCommandDefinition.forSession(true).clear());
    }

    @Test
    public void execute_everyDefinition_hasNormalRouteAndProtectedEligibility() throws Exception {
        CanonicalPeopleSession normal = new CanonicalPeopleSession(directory.resolve("missing.json"));
        Path rejected = directory.resolve("rejected.json");
        Files.writeString(rejected, "{broken");
        byte[] before = Files.readAllBytes(rejected);
        CanonicalPeopleSession protectedSession = new CanonicalPeopleSession(rejected);
        for (var definition : CanonicalCommandDefinition.values()) {
            String word = definition.getWord();
            assertEquals(definition.getUsage(), normal.execute("help " + word.toUpperCase(Locale.ROOT))
                    .getFeedbackToUser());
            try {
                normal.execute(word);
            } catch (ParseException failure) {
                assertFalse(failure.getMessage().contains("Unknown command:"), word);
            }
            if (definition.isAllowedWhenProtected()) {
                protectedSession.execute(word);
            } else {
                CommandException failure = assertThrows(CommandException.class, () -> protectedSession.execute(word));
                assertEquals(protectedSession.getDataLoadError().orElseThrow(), failure.getMessage());
            }
        }
        assertEquals(List.of("help", "list", "exit"), protectedSession.getCommandWords());
        assertFalse(Files.exists(normal.getDataFilePath()));
        assertArrayEquals(before, Files.readAllBytes(rejected));
    }

    @Test
    public void execute_unknownCaseAndLineBreaks_preserveErrorPrecedence() throws Exception {
        CanonicalPeopleSession normal = new CanonicalPeopleSession(directory.resolve("normal.json"));
        assertEquals(String.format(CanonicalPeopleSession.MESSAGE_UNKNOWN_COMMAND_FORMAT, "HELP"),
                assertThrows(ParseException.class, () -> normal.execute("HELP")).getMessage());
        assertEquals("Commands must be on one line.",
                assertThrows(ParseException.class, () -> normal.execute("unknown\nargument")).getMessage());
        Path rejected = directory.resolve("broken.json");
        Files.writeString(rejected, "broken");
        CanonicalPeopleSession protectedSession = new CanonicalPeopleSession(rejected);
        for (String command : List.of("add\ninvalid", "unknown\nargument", "HELP")) {
            assertEquals(protectedSession.getDataLoadError().orElseThrow(),
                    assertThrows(CommandException.class, () -> protectedSession.execute(command)).getMessage());
        }
        assertEquals("Commands must be on one line.",
                assertThrows(ParseException.class, () -> protectedSession.execute("help\nlist")).getMessage());
    }
}
