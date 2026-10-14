package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.RelationshipSearchTestData.createData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.PeopleView;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.person.PersonId;
import seedu.address.model.query.LessonQueries;
import seedu.address.model.search.SearchCategory;
import seedu.address.model.search.SearchCriteria;
import seedu.address.model.search.SearchField;
import seedu.address.storage.JsonPonHubDataStorage;
import seedu.address.storage.ProtectedPonHubDataStorage;

public class TutorRelationshipSearchIntegrationTest {
    @TempDir
    Path directory;

    @Test
    public void tutorSearch_readsAssignedLessonContextsWithoutSavingAndRejectsTuPrefix() throws Exception {
        Path path = directory.resolve("data.json");
        new JsonPonHubDataStorage(path).saveData(createData().exportState());
        byte[] bytes = Files.readAllBytes(path);
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            throw new AssertionError("Tutor retrieval must not save");
        });
        session.execute("search c/tutor n/MEI sn/Alex d/Mon s/Math rm/r1");
        assertEquals(List.of(new PersonId("T1")), session.getPeople().stream().map(person -> person.getId()).toList());
        var before = session.personSearchContextProperty().get();
        assertEquals(new PersonId("S1"), before.get(new PersonId("T1")).getFirst().student().orElseThrow().getId());
        for (String command : List.of("search c/tutor tu/Mei", "search c/tutor sn/", "search c/tutor l/P3",
                "search c/tutor d/Mon d/Tue", "search c/tutor st/1100 et/1000")) {
            assertThrows(ParseException.class, () -> session.execute(command));
            assertEquals(before, session.personSearchContextProperty().get());
        }
        session.execute("search c/tutor d/Mon s/Science");
        assertEquals(List.of(new PersonId("T2")), session.getPeople().stream().map(person -> person.getId()).toList());
        assertTrue(session.personSearchContextProperty().get().get(new PersonId("T2")).getFirst().student().isEmpty());
        session.execute("search c/tutor d/Mon s/Science sn/Alex");
        assertTrue(session.getPeople().isEmpty());
        session.execute("search c/tutor n/Unassigned");
        assertEquals(1, session.getPeople().size());
        assertTrue(session.personSearchContextProperty().get().isEmpty());
        assertArrayEquals(bytes, Files.readAllBytes(path));
    }

    @Test
    public void failedRosterPublication_preservesTutorProjectionThenCommitAndReloadRefreshIt() throws Exception {
        var data = createData();
        Path path = directory.resolve("data.json");
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        storage.saveData(data.exportState());
        var queries = new LessonQueries(data);
        var view = new PeopleView(data, queries);
        view.setSearchCriteria(new SearchCriteria(SearchCategory.TUTOR,
                Map.of(SearchField.STUDENT_NAME, "Alex", SearchField.SUBJECT, "Math")));
        var before = view.searchContextProperty().get();
        CanonicalCommandExecutor.StagedCommand remove = (staged, stagedView) -> {
            staged.unenrolStudent(new PersonId("S1"), new LessonId("L10"));
            return new CommandResult("Unenrolled");
        };
        var executor = new CanonicalCommandExecutor(view, state -> {
            throw new IOException("Injected Tutor context save failure");
        }, Optional::empty);
        assertThrows(CommandException.class, () -> executor.execute(remove));
        assertEquals(before, view.searchContextProperty().get());
        new CanonicalCommandExecutor(view, storage::saveData, Optional::empty).execute(remove);
        assertTrue(view.getPeople().isEmpty());
        assertTrue(view.searchContextProperty().get().isEmpty());
        CanonicalPeopleSession reload = new CanonicalPeopleSession(path);
        reload.execute("search c/tutor sn/Alex s/Math");
        assertTrue(reload.getPeople().isEmpty());
        reload.execute("list r/student");
        reload.execute("enrol 1 lid/L4");
        reload.execute("search c/tutor sn/Alex s/Math");
        assertEquals(1, reload.getPeople().size());
        assertEquals("L4", reload.personSearchContextProperty().get().get(new PersonId("T1"))
                .getFirst().lesson().orElseThrow().getLessonId().toString());
    }
}
