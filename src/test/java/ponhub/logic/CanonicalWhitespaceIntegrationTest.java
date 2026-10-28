package ponhub.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ponhub.logic.parser.exceptions.ParseException;

/**
 * Exercises copied horizontal spaces through production routing, persistence and current-view selectors.
 */
public class CanonicalWhitespaceIntegrationTest {
    @TempDir
    Path directory;

    @Test
    public void execute_copiedSpaces_addEditAndSearchAgreeWithoutRewritingData() throws Exception {
        for (String space : new String[]{" ", "\u2003", "\u00a0", "\u202f"}) {
            Path path = directory.resolve("people-" + (int) space.charAt(0) + ".json");
            CanonicalPeopleSession session = new CanonicalPeopleSession(path);
            session.execute(space + "add" + space + "r/student" + space + "n/Alex Tan" + space
                    + "l/P1" + space + "pp/91234567" + space);
            byte[] saved = Files.readAllBytes(path);
            var snapshot = session.getDataSnapshot();
            session.execute("edit" + space + "1" + space + "n/" + space + "Alex Tan" + space);
            session.execute(space + "search" + space + "c/" + space + "student" + space + "n/" + space
                    + "Alex" + space + "l/" + space + "P1" + space + "pp/" + space + "91234567" + space);
            assertEquals(1, session.getPeople().size());
            assertEquals("Alex Tan", session.getPeople().getFirst().getContactDetails().getName().fullName);
            assertEquals("S1", session.getPeople().getFirst().getId().toString());
            assertEquals(snapshot, session.getDataSnapshot());
            assertArrayEquals(saved, Files.readAllBytes(path));
            assertEquals(snapshot, new CanonicalPeopleSession(path).getDataSnapshot());
        }
    }

    @Test
    public void execute_mixedLeadingWhitespace_preservesCommandRouting() throws Exception {
        CanonicalPeopleSession session = new CanonicalPeopleSession(directory.resolve("mixed.json"));
        for (String leading : new String[]{" \t", "\u00a0\t", "\t\u00a0", "\u202f\t\u2003"}) {
            assertEquals("Showing 0 person(s).", session.execute(leading + "list").getFeedbackToUser());
        }
        assertThrows(ParseException.class, () -> session.execute("add r/student n/Alex l/P1 pp/91234567\t"));
    }

    @Test
    public void execute_auditedRoutes_acceptUnicodeHorizontalSeparators() throws Exception {
        for (String space : new String[]{"\u2003", "\u00a0", "\u202f"}) {
            CanonicalPeopleSession session = new CanonicalPeopleSession(
                    directory.resolve("routes-" + (int) space.charAt(0) + ".json"));
            for (String command : List.of("add r/student n/Alex l/P1 pp/91234567",
                    "add r/tutor n/Mei p/92345678", "addlesson d/Mon st/0900 et/1000 s/Math tu/Mei rm/R1",
                    "editlesson lid/L1 rm/R2", "enrol 1 lid/L1", "list r/student", "lessons 1 sort/schedule",
                    "mark 1 lid/L1 d/2026-10-05 s/absent", "history 1", "history 1 lid/L1",
                    "showlesson lid/L1 d/2026-10-05", "unmark 1 lid/L1 d/2026-10-05", "unenrol 1 lid/L1",
                    "deletelesson lid/L1", "help search", "delete 1", "delete cancel", "exit")) {
                session.execute(space + command.replace(" ", space) + space);
            }
            assertEquals(1, session.getPeople().size());
            assertEquals("S1", session.getPeople().getFirst().getId().toString());
        }
    }

    @Test
    public void execute_invalidCopiedInput_preservesResultContextStateAndBytes() throws Exception {
        Path path = directory.resolve("invalid.json");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("add r/student n/Alex Tan l/P1 pp/91234567");
        session.execute("search c/student n/Alex");
        byte[] bytes = Files.readAllBytes(path);
        var state = session.getDataSnapshot();
        String context = session.peopleResultContextProperty().get();
        for (String space : new String[]{" ", "\u2003", "\u00a0", "\u202f"}) {
            for (String command : List.of("search c/student n/Alex" + space + "xyz/no",
                    "search c/student n/Alex" + space + "n/Alex", "search c/student pp/9123" + space + "4567",
                    "search c/student l/P" + space + "1", "search c/student n/Alex/Tan",
                    "edit 1 n/Alex" + space + "Tan", "edit 1 a/Block" + space + "1")) {
                // Ordinary spaces remain valid inside names and addresses; Unicode interiors do not.
                if (space.equals(" ") && command.startsWith("edit")) {
                    continue;
                }
                assertThrows(ParseException.class, () -> session.execute(command), command);
                assertEquals(state, session.getDataSnapshot());
                assertEquals(context, session.peopleResultContextProperty().get());
                assertArrayEquals(bytes, Files.readAllBytes(path));
            }
        }
        for (String invalid : new String[]{"\n", "\r", "\u2028", "\u2029", "\u0000", "\u001b", "\t"}) {
            assertThrows(ParseException.class, () -> session.execute("edit 1 n/" + invalid + "Alex Tan"));
            assertEquals(state, session.getDataSnapshot());
            assertEquals(context, session.peopleResultContextProperty().get());
            assertArrayEquals(bytes, Files.readAllBytes(path));
        }
        for (String lineBreak : new String[]{"\n", "\r", "\u2028", "\u2029", "\u0085"}) {
            assertThrows(ParseException.class, () -> session.execute("search c/student n/Alex" + lineBreak));
            assertEquals(context, session.peopleResultContextProperty().get());
            assertArrayEquals(bytes, Files.readAllBytes(path));
        }
    }
}
