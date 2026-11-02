package ponhub.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import ponhub.logic.commands.exceptions.CommandException;
import ponhub.model.PonHubData;
import ponhub.model.person.Name;

/**
 * Checks the guidance given when no tutor matches a supplied full name.
 */
public class TutorResolverTest {
    @Test
    public void resolve_unknownNameWithSubjectMarker_suggestsSubjectPrefixOrder() {
        // editlesson keeps a lowercase s/o in tu/ as part of the name, so a missing tutor may be a misread subject.
        CommandException error = assertThrows(CommandException.class, () ->
                TutorResolver.resolve(new PonHubData(), new Name("Bob Ong s/o level math"), Optional.empty()));
        assertEquals("No tutor found with the full name 'Bob Ong s/o level math'."
                + TutorResolver.MESSAGE_SUBJECT_MARKER_HINT, error.getMessage());
    }

    @Test
    public void resolve_unknownNameWithoutSubjectMarker_reportsNameOnly() {
        // d/o cannot start a subject, and ordinary names need no prefix-order hint.
        for (String name : new String[] {"Missing Tutor", "Anita d/o Kumar"}) {
            CommandException error = assertThrows(CommandException.class, () ->
                    TutorResolver.resolve(new PonHubData(), new Name(name), Optional.empty()));
            assertEquals(String.format(TutorResolver.MESSAGE_TUTOR_NOT_FOUND, name), error.getMessage());
        }
    }
}
