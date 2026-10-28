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
import ponhub.storage.ProtectedPonHubDataStorage;

/**
 * Exercises sigma-equivalent substring searches without changing saved names or duplicate identities.
 */
public class CanonicalUnicodeSearchIntegrationTest {
    @TempDir
    Path directory;

    @Test
    public void execute_sigmaSearches_matchOwnAndRelatedNamesWithoutSaving() throws Exception {
        Path path = saveFixture();
        byte[] bytes = Files.readAllBytes(path);
        CanonicalPeopleSession session = readOnlySession(path);
        var before = session.getDataSnapshot();
        var people = session.getPeople();
        for (String query : List.of("Σ", "σ", "ς")) {
            session.execute("search c/student n/" + query);
            assertPeople(session, "S1", "S2");
            session.execute("search c/parent n/" + query + " sn/" + query);
            assertPeople(session, "P1");
            session.execute("search c/tutor n/" + query + " sn/" + query);
            assertPeople(session, "T1");
            session.execute("search c/student tu/" + query);
            assertPeople(session, "S1", "S2");
            session.execute("search c/parent tu/" + query + " sn/" + query);
            assertPeople(session, "P1");
            session.execute("search c/lesson sn/" + query + " tu/" + query);
            assertEquals(List.of("L1"), session.getDisplayedLessons().stream()
                    .map(lesson -> lesson.getLessonId().toString()).toList());
            assertPeople(session, "P1");
        }
        for (String query : List.of("ΝΊΚΟΣ", "Νίκος", "ΝΊΚΟσ", "ΝίκοΣ")) {
            session.execute("search c/student n/" + query);
            assertPeople(session, "S1", "S2");
        }
        assertSame(people, session.getPeople());
        assertSame(before, session.getDataSnapshot());
        assertEquals(List.of("Στέλιος", "ΝΊΚΟΣ", "Νίκοσ", "Σίγμα"), before.people().getPeople().stream()
                .map(person -> person.getContactDetails().getName().fullName).toList());
        assertArrayEquals(bytes, Files.readAllBytes(path));
        assertEquals(before, new CanonicalPeopleSession(path).getDataSnapshot());
    }

    @Test
    public void execute_invalidUnicodeSearches_preserveBothResultScopesAndBytes() throws Exception {
        Path path = saveFixture();
        byte[] bytes = Files.readAllBytes(path);
        CanonicalPeopleSession session = readOnlySession(path);
        session.execute("search c/student n/Σ l/P1 pp/91234569");
        session.execute("search c/lesson sn/ς tu/Σ");
        var people = List.copyOf(session.getPeople());
        var lessons = List.copyOf(session.getDisplayedLessons());
        String peopleContext = session.peopleResultContextProperty().get();
        String lessonContext = session.lessonResultContextProperty().get();
        var before = session.getDataSnapshot();
        for (String command : List.of("search c/student n/Σ n/ς", "search c/student n/Σ x/σ",
                "search c/student n/Σ/ς", "search c/student n/Σ\n", "search c/lesson sn/ς tp/12")) {
            assertThrows(ParseException.class, () -> session.execute(command), command);
            assertEquals(people, session.getPeople());
            assertEquals(lessons, session.getDisplayedLessons());
            assertEquals(peopleContext, session.peopleResultContextProperty().get());
            assertEquals(lessonContext, session.lessonResultContextProperty().get());
            assertSame(before, session.getDataSnapshot());
            assertArrayEquals(bytes, Files.readAllBytes(path));
        }
    }

    private Path saveFixture() throws Exception {
        Path path = directory.resolve("unicode-search.json");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("add r/tutor n/Στέλιος p/92345678");
        session.execute("add r/student n/ΝΊΚΟΣ l/P1 pp/91234569");
        // The existing duplicate policy permits this spelling; search must not migrate it or reject loading it.
        session.execute("add r/student n/Νίκοσ l/P1 pp/91234569");
        session.execute("add r/parent n/Σίγμα p/91234569");
        session.execute("addlesson d/Mon st/0900 et/1000 s/Math tu/Στέλιος rm/R1");
        session.execute("list r/student");
        session.execute("enrol 1 lid/L1");
        session.execute("enrol 2 lid/L1");
        return path;
    }

    private CanonicalPeopleSession readOnlySession(Path path) {
        return new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            throw new AssertionError("Search must not save");
        });
    }

    private static void assertPeople(CanonicalPeopleSession session, String... ids) {
        assertEquals(List.of(ids), session.getPeople().stream().map(person -> person.getId().toString()).toList());
    }
}
