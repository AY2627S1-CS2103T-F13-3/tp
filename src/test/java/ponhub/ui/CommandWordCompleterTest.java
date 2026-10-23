package ponhub.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ponhub.logic.CanonicalCommandCatalog;

public class CommandWordCompleterTest {
    @Test
    public void complete_uniqueMatch_replacesOnlyWordAndPreservesArguments() {
        CommandWordCompleter completer = normalCompleter();
        assertEquals(new CommandWordCompleter.Completion("  showlesson  lid/L1  ", 12),
                completer.complete("  sho  lid/L1  ", 5).orElseThrow());
        completer.reset();
        // The prefix before the caret replaces the rest of the word, including a mistyped suffix.
        assertEquals(new CommandWordCompleter.Completion("\tshowlesson lid/L1", 11),
                completer.complete("\tshoxxx lid/L1", 4).orElseThrow());
    }

    @Test
    public void complete_ambiguousPrefix_cyclesOriginalMatchesInCatalogueOrder() {
        CommandWordCompleter completer = normalCompleter();
        assertEquals(new CommandWordCompleter.Completion("add r/student", 3),
                completer.complete("ad r/student", 2).orElseThrow());
        assertEquals(new CommandWordCompleter.Completion("addlesson r/student", 9),
                completer.complete("add r/student", 3).orElseThrow());
        assertEquals(new CommandWordCompleter.Completion("add r/student", 3),
                completer.complete("addlesson r/student", 9).orElseThrow());
        completer.reset();
        assertEquals(new CommandWordCompleter.Completion("edit", 4),
                completer.complete("e", 1).orElseThrow());
        assertEquals(new CommandWordCompleter.Completion("editlesson", 10),
                completer.complete("edit", 4).orElseThrow());
        assertEquals(new CommandWordCompleter.Completion("enrol", 5),
                completer.complete("editlesson", 10).orElseThrow());
        assertEquals(new CommandWordCompleter.Completion("exit", 4),
                completer.complete("enrol", 5).orElseThrow());
    }

    @Test
    public void complete_emptyUnmatchedUppercaseAndArgumentPositions_hasNoCompletion() {
        for (String draft : new String[] {"", "  ", "unknown", "SHO", "sho ", "help sho"}) {
            assertTrue(normalCompleter().complete(draft, draft.length()).isEmpty(), draft);
        }
        assertTrue(normalCompleter().complete("sho", 0).isEmpty());
        assertTrue(normalCompleter().complete("  sho", 2).isEmpty());
    }

    @Test
    public void complete_protectedSession_onlyOffersRecoveryRoutes() {
        CommandWordCompleter completer = new CommandWordCompleter(CanonicalCommandCatalog.getCommandWords(true));
        assertTrue(completer.complete("ad", 2).isEmpty());
        assertTrue(completer.complete("sho", 3).isEmpty());
        assertEquals(new CommandWordCompleter.Completion("exit", 4), completer.complete("e", 1).orElseThrow());
        completer.reset();
        assertEquals(new CommandWordCompleter.Completion("help", 4), completer.complete("h", 1).orElseThrow());
        completer.reset();
        assertEquals(new CommandWordCompleter.Completion("list r/student", 4),
                completer.complete("li r/student", 2).orElseThrow());
    }

    private static CommandWordCompleter normalCompleter() {
        return new CommandWordCompleter(CanonicalCommandCatalog.getCommandWords(false));
    }
}
