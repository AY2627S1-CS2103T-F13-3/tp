package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

public class CanonicalCommandCatalogTest {
    @Test
    public void guidance_preservesHelpOrderAndProtectedAllowList() {
        List<String> normal = List.of("add", "addlesson", "enrol", "unenrol", "mark", "unmark", "history",
                "delete", "deletelesson", "list", "lessons", "showlesson", "search", "help", "exit");
        List<String> protectedWords = List.of("help", "list", "exit");
        assertEquals(normal, CanonicalCommandCatalog.getCommandWords(false));
        assertEquals(protectedWords, CanonicalCommandCatalog.getCommandWords(true));
        for (boolean isProtected : List.of(false, true)) {
            var words = CanonicalCommandCatalog.getCommandWords(isProtected);
            assertThrows(UnsupportedOperationException.class, () -> words.add("clear"));
            Optional<String> error = isProtected
                    ? Optional.of("Restore the rejected file and restart") : Optional.empty();
            String overview = CanonicalCommandCatalog.getOverview(error);
            int previous = -1;
            for (String word : words) {
                int position = overview.indexOf("\n" + word + " - ");
                assertTrue(position > previous);
                previous = position;
                assertTrue(overview.contains(CanonicalCommandCatalog.getUsage(word, isProtected).orElseThrow()));
            }
            error.ifPresent(guidance -> assertTrue(overview.contains(guidance)));
            assertTrue(CanonicalCommandCatalog.getUsage("clear", isProtected).isEmpty());
            assertTrue(CanonicalCommandCatalog.getUsage("EXIT", isProtected).isEmpty());
        }
        assertTrue(CanonicalCommandCatalog.getUsage("add", true).isEmpty());
        assertThrows(NullPointerException.class, () -> CanonicalCommandCatalog.getUsage(null, false));
        assertThrows(NullPointerException.class, () -> CanonicalCommandCatalog.getOverview(null));
    }
}
