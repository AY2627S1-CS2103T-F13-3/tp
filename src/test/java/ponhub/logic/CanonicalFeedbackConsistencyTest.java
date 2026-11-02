package ponhub.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.person.Phone;

/**
 * Checks actionable phone feedback and retained result views at the command boundary.
 */
public class CanonicalFeedbackConsistencyTest {
    @TempDir
    Path directory;

    @Test
    public void execute_invalidSearchPhones_shareDomainRuleAndPreserveResults() throws Exception {
        Path path = directory.resolve("feedback.json");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("add r/tutor n/Mei Lim p/92345678");
        session.execute("add r/student n/Alex Tan l/S2 pp/91234567");
        session.execute("addlesson d/Mon st/0900 et/1000 s/Math tu/Mei Lim rm/R1");
        session.execute("search c/student n/Alex");
        session.execute("search c/lesson s/Math");
        var before = session.getDataSnapshot();
        var people = List.copyOf(session.getPeople());
        var lessons = List.copyOf(session.getDisplayedLessons());
        String peopleContext = session.peopleResultContextProperty().get();
        String lessonContext = session.lessonResultContextProperty().get();
        byte[] bytes = Files.readAllBytes(path);
        for (String prefix : List.of("p/", "pp/", "tp/")) {
            String category = prefix.equals("tp/") ? "lesson" : "student";
            for (String phone : List.of("12", "1234567890123456", "１２３", "123-4")) {
                ParseException failure = assertThrows(ParseException.class, () ->
                        session.execute("search c/" + category + " " + prefix + phone));
                assertEquals("Search filter '" + prefix + "': " + Phone.MESSAGE_CONSTRAINTS,
                        failure.getMessage());
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
