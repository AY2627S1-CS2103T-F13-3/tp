package ponhub.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ponhub.logic.parser.exceptions.ParseException;

/**
 * Exercises valid subject orders and actionable rejection of ambiguous name-marker tokens.
 */
public class CanonicalSubjectOrderIntegrationTest {
    @TempDir
    Path directory;

    @Test
    public void execute_subjectOrders_matchWhileAmbiguousNamesPreserveBothPanels() throws Exception {
        Path path = directory.resolve("subject-order.json");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("add r/tutor n/Mei Lim p/92345678");
        session.execute("add r/student n/Alex Tan l/S2 pp/91234567");
        session.execute("addlesson d/Mon st/0900 et/1000 s/o level math tu/Mei Lim rm/R1");
        session.execute("addlesson d/Tue st/0900 et/1000 s/Math tu/Mei Lim rm/R1");
        session.execute("list r/student");
        session.execute("enrol 1 lid/L1");
        session.execute("enrol 1 lid/L2");
        byte[] bytes = Files.readAllBytes(path);
        var before = session.getDataSnapshot();
        for (String name : List.of("n/Alex", "sn/Alex", "tu/Mei Lim")) {
            String category = name.startsWith("n/") ? "student" : "lesson";
            for (String arguments : List.of("c/" + category + " s/o level math " + name,
                    "s/o level math " + name + " c/" + category,
                    name + " c/" + category + " s/o level math",
                    "c/" + category + " " + name + " s/Math",
                    "c/" + category + " s/Math " + name)) {
                String expected = arguments.contains("s/o level") || category.equals("student") ? "1" : "2";
                assertEquals("Found " + expected + " " + category + "(s).",
                        session.execute("search " + arguments).getFeedbackToUser(), arguments);
            }
            var people = List.copyOf(session.getPeople());
            var lessons = List.copyOf(session.getDisplayedLessons());
            String peopleContext = session.peopleResultContextProperty().get();
            String lessonContext = session.lessonResultContextProperty().get();
            for (String value : List.of("o level math", "o Kumar")) {
                ParseException failure = assertThrows(ParseException.class, () ->
                        session.execute("search c/" + category + " " + name + " s/" + value));
                assertTrue(failure.getMessage().contains("put it before name filters or immediately after c/CATEGORY"));
                assertEquals(people, session.getPeople());
                assertEquals(lessons, session.getDisplayedLessons());
                assertEquals(peopleContext, session.peopleResultContextProperty().get());
                assertEquals(lessonContext, session.lessonResultContextProperty().get());
                assertSame(before, session.getDataSnapshot());
                assertArrayEquals(bytes, Files.readAllBytes(path));
            }
        }
    }
}
