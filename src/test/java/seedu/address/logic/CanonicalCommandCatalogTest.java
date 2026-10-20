package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

public class CanonicalCommandCatalogTest {
    @Test
    public void editingGuidance_describesSelectorsAndDifferentBlankValueRules() {
        String person = CanonicalCommandCatalog.getUsage("edit", false).orElseThrow();
        assertTrue(person.contains("current displayed people position and at least one field"));
        assertTrue(person.contains("omitted fields stay unchanged"));
        assertTrue(person.contains("Blank e/ or a/ clears that value"));
        assertTrue(person.contains("blank p/ clears only a Student's phone"));
        assertTrue(person.contains("Names, Student levels/parent phones and Tutor/Parent phones cannot be cleared"));

        String lesson = CanonicalCommandCatalog.getUsage("editlesson", false).orElseThrow();
        assertTrue(lesson.contains("stable Lesson ID and at least one field"));
        assertTrue(lesson.contains("omitted fields stay unchanged"));
        assertTrue(lesson.contains("Supplied values must be nonblank; Lesson fields cannot be cleared"));
        assertTrue(lesson.contains("Use tp/ only with tu/"));
        assertTrue(lesson.contains("must not clash for its tutor, room or Students"));
        assertTrue(lesson.contains("weekday change cannot invalidate retained attendance"));
        for (String word : List.of("edit", "editlesson")) {
            assertTrue(CanonicalCommandCatalog.getUsage(word, true).isEmpty());
        }
    }

    @Test
    public void guidance_preservesHelpOrderAndProtectedAllowList() {
        List<String> normal = List.of("add", "edit", "addlesson", "editlesson", "enrol", "unenrol", "mark",
                "unmark", "history", "delete", "deletelesson", "list", "lessons", "showlesson", "search",
                "help", "exit");
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
        assertTrue(CanonicalCommandCatalog.getUsage("edit", true).isEmpty());
        assertTrue(CanonicalCommandCatalog.getUsage("editlesson", true).isEmpty());
        assertThrows(NullPointerException.class, () -> CanonicalCommandCatalog.getUsage(null, false));
        assertThrows(NullPointerException.class, () -> CanonicalCommandCatalog.getOverview(null));
    }
}
