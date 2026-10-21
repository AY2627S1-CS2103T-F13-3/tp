package ponhub.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.LessonQueryTestData.createData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.person.PersonId;
import ponhub.storage.JsonPonHubDataStorage;
import ponhub.storage.ProtectedPonHubDataStorage;

public class StudentRelationshipSearchIntegrationTest {
    @TempDir
    Path directory;

    @Test
    public void studentSearch_readsCompleteCatalogueAndKeepsSeparateLessonSelectionAndSavedBytes() throws Exception {
        Path path = saveFixture();
        byte[] bytes = Files.readAllBytes(path);
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            throw new AssertionError("Search must not call the writer");
        });
        session.execute("list r/parent");
        session.execute("search c/lesson s/Science");
        var lessonSelection = List.copyOf(session.getDisplayedLessons());
        var people = session.getPeople();
        var context = session.personSearchContextProperty();
        assertEquals("Found 2 student(s).", session.execute("search c/student d/Mon s/Math tu/MEI rm/r1")
                .getFeedbackToUser());
        assertIds(session, "S1", "S2");
        assertEquals(2, context.get().size());
        assertEquals("L10", context.get().get(new PersonId("S1")).getFirst().lesson().orElseThrow()
                .getLessonId().toString());
        assertEquals(lessonSelection, session.getDisplayedLessons());
        for (String command : List.of("search c/student d/Mon d/Tue", "search c/student s/", "search c/student sn/Alex",
                "search c/student st/1000 et/0900", "search c/student tu/Mei tu/Other")) {
            assertThrows(ParseException.class, () -> session.execute(command));
            assertIds(session, "S1", "S2");
        }
        session.execute("search c/student d/Tue s/Math");
        assertTrue(people.isEmpty());
        assertTrue(context.get().isEmpty());
        session.execute("search c/student n/Alex");
        assertIds(session, "S1", "S3");
        assertTrue(context.get().isEmpty());
        session.execute("list");
        assertEquals(7, people.size());
        assertSame(people, session.getPeople());
        assertSame(context, session.personSearchContextProperty());
        assertArrayEquals(bytes, Files.readAllBytes(path));
    }

    @Test
    public void membership_refreshesResultsAndContextWithFailedSaveRetryAndReload() throws Exception {
        Path path = saveFixture();
        AtomicBoolean fail = new AtomicBoolean(true);
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            if (fail.get()) {
                throw new IOException("Injected enrolment write failure");
            }
            new JsonPonHubDataStorage(path).saveData(state);
        });
        session.execute("search c/student d/Mon s/Math");
        var people = session.getPeople();
        var contexts = session.personSearchContextProperty();
        var before = contexts.get();
        byte[] bytes = Files.readAllBytes(path);
        assertThrows(CommandException.class, () -> session.execute("unenrol 1 lid/L10"));
        assertIds(session, "S1", "S2");
        assertSame(before, contexts.get());
        assertArrayEquals(bytes, Files.readAllBytes(path));
        fail.set(false);
        session.execute("unenrol 1 lid/L10");
        assertIds(session, "S2");
        assertEquals(List.of(new PersonId("S2")), contexts.get().keySet().stream().toList());
        session.execute("list r/student");
        session.execute("enrol 3 lid/L4");
        session.execute("search c/student d/Mon s/Math");
        assertIds(session, "S2", "S3");
        assertEquals("L4", contexts.get().get(new PersonId("S3")).getFirst().lesson().orElseThrow()
                .getLessonId().toString());
        assertSame(people, session.getPeople());
        assertSame(contexts, session.personSearchContextProperty());
        CanonicalPeopleSession reload = new CanonicalPeopleSession(path);
        reload.execute("search c/student d/Mon s/Math");
        assertIds(reload, "S2", "S3");
        assertEquals(session.getDataSnapshot(), reload.getDataSnapshot());
    }

    private Path saveFixture() throws Exception {
        Path path = directory.resolve("data.json");
        new JsonPonHubDataStorage(path).saveData(createData().exportState());
        return path;
    }

    private static void assertIds(CanonicalPeopleSession session, String... ids) {
        assertEquals(List.of(ids), session.getPeople().stream().map(person -> person.getId().toString()).toList());
    }
}
