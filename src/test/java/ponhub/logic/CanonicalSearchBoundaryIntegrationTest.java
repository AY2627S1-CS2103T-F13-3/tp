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
 * Exercises pasted search spaces and rejected slash values through both live result panels.
 */
public class CanonicalSearchBoundaryIntegrationTest {
    @TempDir
    Path directory;

    @Test
    public void execute_slashNameSearches_preserveBothResultScopesAndStoredBytes() throws Exception {
        Path path = saveFixture();
        byte[] bytes = Files.readAllBytes(path);
        CanonicalPeopleSession session = createReadOnlySession(path);
        session.execute("search c/student n/Alex");
        session.execute("search c/lesson s/Math");
        var before = session.getDataSnapshot();
        var people = List.copyOf(session.getPeople());
        var lessons = List.copyOf(session.getDisplayedLessons());
        String peopleContext = session.peopleResultContextProperty().get();
        String lessonContext = session.lessonResultContextProperty().get();

        for (String command : List.of("search c/student n/Ravi s/o Kumar",
                "search n/Anita d/o Kumar c/student", "search c/parent sn/Ravi s/o Kumar",
                "search c/lesson sn/Anita d/o Kumar s/Primary Math",
                "search c/lesson tu/Ravi s/o Kumar d/Mon", "search tu/Ravi d/o Kumar c/lesson")) {
            assertThrows(ParseException.class, () -> session.execute(command), command);
            assertEquals(people, session.getPeople(), command);
            assertEquals(lessons, session.getDisplayedLessons(), command);
            assertEquals(peopleContext, session.peopleResultContextProperty().get(), command);
            assertEquals(lessonContext, session.lessonResultContextProperty().get(), command);
            assertSame(before, session.getDataSnapshot(), command);
            assertArrayEquals(bytes, Files.readAllBytes(path), command);
        }

        session.execute("search c/student n/Ravi");
        assertPeople(session, "S1");
        session.execute("search c/lesson sn/Anita d/Mon s/Primary Math");
        assertLessons(session, "L1");
        assertArrayEquals(bytes, Files.readAllBytes(path));
    }

    @Test
    public void execute_pastedSpaces_matchOwnAndRelatedFieldsWithoutSaving() throws Exception {
        Path path = saveFixture();
        byte[] bytes = Files.readAllBytes(path);
        CanonicalPeopleSession session = createReadOnlySession(path);
        var before = session.getDataSnapshot();

        for (String space : List.of("\u00a0", "\u2003", "\u202f")) {
            session.execute("search c/student n/Alex" + space + "Tan");
            assertPeople(session, "S3");
            session.execute("search c/parent n/Pat" + space + "Ong sn/Alex" + space + "Tan");
            assertPeople(session, "P1");
            session.execute("search c/tutor n/Mei" + space + "Lim sn/Alex" + space + "Tan");
            assertPeople(session, "T1");
            session.execute("search c/student n/Alex" + space + "Tan tu/Mei" + space + "Lim");
            assertPeople(session, "S3");
            session.execute("search c/lesson sn/Alex" + space + "Tan tu/Mei" + space + "Lim s/Primary"
                    + space + space + "Math");
            assertLessons(session, "L1");
            assertPeople(session, "S3");
            assertSame(before, session.getDataSnapshot());
            assertArrayEquals(bytes, Files.readAllBytes(path));
        }
        assertEquals(before, new CanonicalPeopleSession(path).getDataSnapshot());
    }

    private Path saveFixture() throws Exception {
        Path path = directory.resolve("search-boundaries.json");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("add r/tutor n/Mei Lim p/92345678");
        session.execute("add r/tutor n/Ravi s/o Kumar p/92345679");
        session.execute("add r/student n/Ravi s/o Kumar l/P1 pp/91234567");
        session.execute("add r/student n/Anita d/o Kumar l/P1 pp/91234567");
        session.execute("add r/student n/Alex Tan l/P1 pp/91234567");
        session.execute("add r/parent n/Pat Ong p/91234567");
        session.execute("addlesson d/Mon st/0900 et/1000 s/Primary Math tu/Mei Lim rm/R1");
        session.execute("list r/student");
        session.execute("enrol 1 lid/L1");
        session.execute("enrol 2 lid/L1");
        session.execute("enrol 3 lid/L1");
        return path;
    }

    private static CanonicalPeopleSession createReadOnlySession(Path path) {
        return new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            throw new AssertionError("Search must not save");
        });
    }

    private static void assertPeople(CanonicalPeopleSession session, String... ids) {
        assertEquals(List.of(ids), session.getPeople().stream().map(person -> person.getId().toString()).toList());
    }

    private static void assertLessons(CanonicalPeopleSession session, String... ids) {
        assertEquals(List.of(ids), session.getDisplayedLessons().stream()
                .map(lesson -> lesson.getLessonId().toString()).toList());
    }
}
