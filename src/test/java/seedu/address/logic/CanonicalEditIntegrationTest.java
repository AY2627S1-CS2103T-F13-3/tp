package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.collections.ListChangeListener;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Student;
import seedu.address.storage.JsonPonHubDataStorage;
import seedu.address.storage.ProtectedPonHubDataStorage;

/** Exercises editing through real startup, displayed selectors and the saved canonical session. */
public class CanonicalEditIntegrationTest {
    @TempDir
    Path directory;

    @Test
    public void execute_partialStudentEdit_clearsOptionalFieldsAndPersistsStableIdentity() throws Exception {
        Path path = fixture();
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("list r/student");
        var before = session.getDataSnapshot();
        var people = session.getPeople();

        assertEquals("Edited student: Alex Tan (S1).",
                session.execute("edit 1 l/P2 p/ e/ a/").getFeedbackToUser());
        Student edited = (Student) people.getFirst();
        assertEquals(new PersonId("S1"), edited.getId());
        assertEquals(new EducationLevel("P2"), edited.getLevel());
        assertTrue(edited.getContactDetails().getPhone().isEmpty());
        assertTrue(edited.getContactDetails().getEmail().isEmpty());
        assertTrue(edited.getContactDetails().getAddress().isEmpty());
        assertSame(people, session.getPeople());
        assertEquals(Optional.of(PersonRole.STUDENT), session.getRoleFilter());
        assertEquals(before.people().getPeople().stream().map(PersonRecord::getId).toList(),
                session.getDataSnapshot().people().getPeople().stream().map(PersonRecord::getId).toList());
        assertEquals(before.people().getLastAllocatedSequences(),
                session.getDataSnapshot().people().getLastAllocatedSequences());
        assertEquals(before.lessons(), session.getDataSnapshot().lessons());
        assertEquals(before.attendance(), session.getDataSnapshot().attendance());
        assertEquals(before.lastAllocatedLessonSequence(), session.getDataSnapshot().lastAllocatedLessonSequence());
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
    }

    @Test
    public void execute_normalizedNoOp_doesNotSavePublishOrTreatCapitalizationAsUnchanged() throws Exception {
        Path path = fixture();
        AtomicInteger saves = new AtomicInteger();
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            saves.incrementAndGet();
            new JsonPonHubDataStorage(path).saveData(state);
        });
        session.execute("list r/student");
        var before = session.getDataSnapshot();
        AtomicInteger notifications = new AtomicInteger();
        session.getPeople().addListener((ListChangeListener<PersonRecord>) change -> notifications.incrementAndGet());
        byte[] bytes = Files.readAllBytes(path);

        assertEquals("No changes to student: Alex Tan (S1).",
                session.execute("edit 1 n/ Alex   Tan l/p1").getFeedbackToUser());
        assertSame(before, session.getDataSnapshot());
        assertArrayEquals(bytes, Files.readAllBytes(path));
        assertEquals(0, saves.get());
        assertEquals(0, notifications.get());
        assertEquals("Edited student: ALEX TAN (S1).", session.execute("edit 1 n/ALEX TAN").getFeedbackToUser());
        assertEquals(1, saves.get());
        assertTrue(notifications.get() > 0);
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
    }

    @Test
    public void execute_rejections_preserveVisibleMappingsAndSavedBytes() throws Exception {
        Path path = fixture();
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("list r/student");
        var before = session.getDataSnapshot();
        var visible = List.copyOf(session.getPeople());
        byte[] bytes = Files.readAllBytes(path);
        for (String command : List.of("edit 1 n/", "edit 1 l/", "edit 1 pp/", "edit 3 n/Alex",
                "edit 1 n/Jamie Tan pp/91234567")) {
            assertThrows(CommandException.class, () -> session.execute(command), command);
            assertSame(before, session.getDataSnapshot());
            assertEquals(visible, session.getPeople());
            assertArrayEquals(bytes, Files.readAllBytes(path));
        }
        for (String command : List.of("edit 1", "edit 0 n/Alex", "edit S1 n/Alex", "edit 1 r/tutor",
                "edit 1 a/Block x/value", "edit 1 n/Alex n/Alex", "edit 1 p/abc")) {
            assertThrows(ParseException.class, () -> session.execute(command), command);
            assertSame(before, session.getDataSnapshot());
            assertEquals(visible, session.getPeople());
            assertArrayEquals(bytes, Files.readAllBytes(path));
        }
        for (String role : List.of("tutor", "parent")) {
            session.execute("list r/" + role);
            for (String field : List.of("p/", "l/P2", "pp/91234567")) {
                assertThrows(CommandException.class, () -> session.execute("edit 1 " + field));
                assertSame(before, session.getDataSnapshot());
                assertArrayEquals(bytes, Files.readAllBytes(path));
            }
        }
    }

    @Test
    public void execute_protectedSession_rejectsBeforeParsingAndHidesEditHelp() throws Exception {
        Path path = directory.resolve("protected.json");
        Files.writeString(path, "{");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        String error = session.getDataLoadError().orElseThrow();
        for (String command : List.of("edit", "edit 1 n/Alex", "edit 1 n/")) {
            assertEquals(error, assertThrows(CommandException.class, () -> session.execute(command)).getMessage());
        }
        assertFalse(session.getCommandWords().contains("edit"));
        assertThrows(ParseException.class, () -> session.execute("help edit"));
        assertEquals("{", Files.readString(path));
    }

    private Path fixture() throws Exception {
        Path path = directory.resolve("ponhub.json");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("add r/tutor n/Mei Lim p/92345678");
        session.execute("add r/student n/Alex Tan l/P1 pp/91234567 p/81111111 e/alex@example.com a/Block One");
        session.execute("add r/parent n/Pat Tan p/91234567");
        session.execute("add r/student n/Jamie Tan l/P2 pp/91234567");
        return path;
    }
}
