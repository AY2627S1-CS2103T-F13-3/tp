package ponhub.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.RelationshipSearchTestData.createData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ponhub.logic.commands.CommandResult;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.PeopleView;
import ponhub.model.lesson.LessonId;
import ponhub.model.person.PersonId;
import ponhub.model.query.LessonQueries;
import ponhub.model.search.SearchCategory;
import ponhub.model.search.SearchCriteria;
import ponhub.model.search.SearchField;
import ponhub.storage.JsonPonHubDataStorage;
import ponhub.storage.ProtectedPonHubDataStorage;

public class ParentRelationshipSearchIntegrationTest {
    @TempDir
    Path directory;

    @Test
    public void failedPublication_keepsAnActiveParentProjectionThenCommitRefreshesContext() throws Exception {
        var data = createData();
        var queries = new LessonQueries(data);
        var view = new PeopleView(data, queries);
        view.setSearchCriteria(new SearchCriteria(SearchCategory.PARENT,
                Map.of(SearchField.STUDENT_NAME, "Jamie", SearchField.SUBJECT, "Math")));
        var people = view.getPeople();
        var context = view.searchContextProperty();
        var before = context.get();
        var executor = new CanonicalCommandExecutor(view, state -> {
            throw new IOException("Injected parent projection save failure");
        }, Optional::empty);
        CanonicalCommandExecutor.StagedCommand enrol = (staged, stagedView) -> {
            staged.enrolStudent(new PersonId("S2"), new LessonId("L4"));
            return new CommandResult("Enrolled");
        };
        assertThrows(CommandException.class, () -> executor.execute(enrol));
        assertEquals(before, context.get());
        assertEquals(1, context.get().get(new PersonId("P1")).size());
        new CanonicalCommandExecutor(view, state -> { }, Optional::empty).execute(enrol);
        assertEquals(2, people.size());
        assertEquals(2, context.get().get(new PersonId("P1")).size());
    }

    @Test
    public void parentQueries_validatePreserveBytesAndDisplayOnlySameChildLessonContext() throws Exception {
        Path path = directory.resolve("data.json");
        new JsonPonHubDataStorage(path).saveData(createData().exportState());
        byte[] bytes = Files.readAllBytes(path);
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            throw new AssertionError("Parent retrieval must not save operational data");
        });
        session.execute("search c/parent n/Pat p/00987654 e/@EXAMPLE sn/Alex d/Mon s/Math");
        assertEquals(List.of(new PersonId("P1")), session.getPeople().stream().map(person -> person.getId()).toList());
        var matches = session.personSearchContextProperty().get().get(new PersonId("P1"));
        assertEquals(new PersonId("S1"), matches.getFirst().student().orElseThrow().getId());
        assertEquals("L10", matches.getFirst().lesson().orElseThrow().getLessonId().toString());
        var context = session.personSearchContextProperty().get();
        for (String command : List.of("search c/parent sn/", "search c/parent sn/Alex sn/Jamie",
                "search c/parent l/P3", "search c/parent pp/00987654", "search c/parent st/2400")) {
            assertThrows(ParseException.class, () -> session.execute(command));
            assertEquals(context, session.personSearchContextProperty().get());
        }
        assertThrows(CommandException.class, () -> session.execute("enrol 1 lid/L4")); // Position 1 is a Parent.
        session.execute("search c/parent sn/Alex d/Wed s/中文");
        assertTrue(session.getPeople().isEmpty());
        session.execute("search c/parent sn/Alex");
        assertEquals(3, session.getPeople().size()); // Includes S3's parent despite S3 having no lessons.
        assertTrue(session.personSearchContextProperty().get().get(new PersonId("P3"))
                .getFirst().lesson().isEmpty());
        assertArrayEquals(bytes, Files.readAllBytes(path));
    }

    @Test
    public void membershipChangesAndReload_updateSubsequentParentQueriesUsingCurrentRosters() throws Exception {
        Path path = directory.resolve("data.json");
        new JsonPonHubDataStorage(path).saveData(createData().exportState());
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("search c/parent sn/Alex d/Mon s/Math");
        assertEquals(2, session.getPeople().size());
        session.execute("list r/student");
        session.execute("unenrol 1 lid/L10");
        session.execute("search c/parent sn/Alex d/Mon s/Math");
        assertTrue(session.getPeople().isEmpty());
        session.execute("list r/student");
        session.execute("enrol 1 lid/L4");
        session.execute("search c/parent sn/Alex d/Mon s/Math");
        assertEquals(2, session.getPeople().size());
        assertEquals("L4", session.personSearchContextProperty().get().get(new PersonId("P1"))
                .getFirst().lesson().orElseThrow().getLessonId().toString());
        CanonicalPeopleSession reload = new CanonicalPeopleSession(path);
        reload.execute("search c/parent sn/Alex d/Mon s/Math");
        assertEquals(session.getPeople(), reload.getPeople());
        assertEquals(session.getDataSnapshot(), reload.getDataSnapshot());
    }
}
