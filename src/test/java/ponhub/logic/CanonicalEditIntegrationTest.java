package ponhub.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.collections.ListChangeListener;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.person.EducationLevel;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.PersonRole;
import ponhub.model.person.Student;
import ponhub.model.query.LessonDetailView;
import ponhub.storage.JsonPonHubDataStorage;
import ponhub.storage.ProtectedPonHubDataStorage;

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
    public void execute_duplicateEdit_explainsRoleIdentityAndPreservesState() throws Exception {
        for (String role : List.of("student", "tutor", "parent")) {
            Path path = directory.resolve(role + ".json");
            CanonicalPeopleSession setup = new CanonicalPeopleSession(path);
            String first = role.equals("student") ? "l/P1 pp/91234567 p/81111111" : "p/91234567";
            String second = role.equals("student") ? "l/S2 pp/91234567 p/82222222" : "p/91234567";
            setup.execute("add r/" + role + " n/Alex Tan " + first);
            setup.execute("add r/" + role + " n/Jamie Tan " + second);
            AtomicInteger saves = new AtomicInteger();
            CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
                saves.incrementAndGet();
                new JsonPonHubDataStorage(path).saveData(state);
            });
            session.execute("list r/" + role);
            var before = session.getDataSnapshot();
            var visible = List.copyOf(session.getPeople());
            byte[] bytes = Files.readAllBytes(path);
            AtomicInteger notifications = new AtomicInteger();
            session.getPeople().addListener((ListChangeListener<PersonRecord>) c -> notifications.incrementAndGet());

            String identityPhone = role.equals("student") ? "parent phone (pp/)" : "phone (p/)";
            String prefix = role.equals("student") ? "pp/" : "p/";
            CommandException failure = assertThrows(CommandException.class, () ->
                    session.execute("edit 2 n/ ALEX   TAN"));
            assertEquals("A " + role + " with the same normalized name and " + identityPhone
                    + " already exists. Change n/ or " + prefix + " to a unique combination.", failure.getMessage());
            assertSame(before, session.getDataSnapshot());
            assertEquals(visible, session.getPeople());
            assertEquals(Optional.of(PersonRole.valueOf(role.toUpperCase(java.util.Locale.ROOT))),
                    session.getRoleFilter());
            assertArrayEquals(bytes, Files.readAllBytes(path));
            assertEquals(0, saves.get());
            assertEquals(0, notifications.get());

            assertEquals("A person's name cannot be cleared.", assertThrows(CommandException.class, () ->
                    session.execute("edit 2 n/")).getMessage());
            assertArrayEquals(bytes, Files.readAllBytes(path));
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

    @Test
    public void execute_failedSaves_preserveAllLiveViewsUntilSuccessfulRetry() throws Exception {
        Path path = fixture();
        for (IOException failure : List.of(new IOException("disk failure"),
                new AccessDeniedException(path.toString()))) {
            AtomicBoolean failSave = new AtomicBoolean(true);
            AtomicInteger saves = new AtomicInteger();
            CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
                saves.incrementAndGet();
                if (failSave.get()) {
                    throw failure;
                }
                new JsonPonHubDataStorage(path).saveData(state);
            });
            session.execute("search c/student n/Alex");
            var before = session.getDataSnapshot();
            var person = session.getPeople().getFirst();
            var contexts = session.personSearchContextProperty().get();
            byte[] bytes = Files.readAllBytes(path);
            AtomicInteger notifications = new AtomicInteger();
            session.getPeople().addListener((ListChangeListener<PersonRecord>) c -> notifications.incrementAndGet());
            assertThrows(CommandException.class, () -> session.execute("edit 1 n/Changed Student"));
            assertEquals(1, saves.get());
            assertEquals(0, notifications.get());
            assertSame(before, session.getDataSnapshot());
            assertSame(person, session.getPeople().getFirst());
            assertSame(contexts, session.personSearchContextProperty().get());
            assertArrayEquals(bytes, Files.readAllBytes(path));
            failSave.set(false);
            session.execute("edit 1 n/Changed Student");
            assertTrue(session.getPeople().isEmpty());
            assertEquals(2, saves.get());
            assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
            // Restore the same fixture for the next independently injected failure.
            new JsonPonHubDataStorage(path).saveData(before);
        }
    }

    @Test
    public void execute_populatedHistoryAndTutorRename_refreshStableDetailAndSearchSubscriptions() throws Exception {
        CanonicalPeopleSession session = linkedSession();
        session.execute("history 1 lid/L1");
        var history = session.getDisplayedHistory();
        var entries = List.copyOf(history);
        session.execute("search c/student n/Alex");
        session.execute("edit 1 n/Changed Student");
        assertTrue(session.getPeople().isEmpty());
        assertSame(history, session.getDisplayedHistory());
        assertEquals(entries, history);
        assertTrue(session.historyViewActiveProperty().get());
        assertEquals("Student S1 · attendance history · L1", session.historyResultContextProperty().get());

        session.execute("list r/tutor");
        session.execute("search c/lesson tu/Mei");
        var lessons = session.getDisplayedLessons();
        assertEquals(1, lessons.size());
        session.execute("edit 1 n/New Tutor");
        assertSame(lessons, session.getDisplayedLessons());
        assertTrue(lessons.isEmpty());
        assertEquals("Lesson search: tu/mei", session.lessonResultContextProperty().get());
        session.execute("showlesson lid/L1 d/2026-10-05");
        var detail = (LessonDetailView) lessons.getFirst();
        assertEquals("New Tutor", detail.getTutorName());
        assertEquals(new PersonId("T1"), detail.getTutorId());
        assertEquals("Changed Student", detail.formerAttendance().getFirst().student().getName().toString());
        assertEquals(1, detail.currentRoster().size());
        var before = session.getDataSnapshot();
        session.execute("edit 1 n/Corrected Tutor");
        assertEquals("Corrected Tutor", lessons.getFirst().getTutorName());
        assertEquals(before.lessons(), session.getDataSnapshot().lessons());
        assertEquals(before.attendance(), session.getDataSnapshot().attendance());
        assertEquals(before.people().getLastAllocatedSequences(),
                session.getDataSnapshot().people().getLastAllocatedSequences());
        assertEquals(session.getDataSnapshot(),
                new CanonicalPeopleSession(session.getDataFilePath()).getDataSnapshot());
    }

    @Test
    public void execute_parentAndStudentPhoneEdits_recomputeExactLinksWithoutCascading() throws Exception {
        CanonicalPeopleSession session = linkedSession();
        session.execute("search c/parent sn/Alex");
        assertEquals(1, session.getPeople().size());
        session.execute("edit 1 p/90000000");
        assertTrue(session.getPeople().isEmpty());
        assertEquals("91234567", ((Student) session.getDataSnapshot().people().getPeople().get(1))
                .getParentPhone().value);
        session.execute("list r/student");
        session.execute("edit 1 pp/90000000");
        session.execute("search c/parent sn/Alex");
        assertEquals(new PersonId("P1"), session.getPeople().getFirst().getId());
        assertEquals(new PersonId("S1"), session.personSearchContextProperty().get()
                .get(new PersonId("P1")).getFirst().student().orElseThrow().getId());
        session.execute("list r/student");
        session.execute("edit 1 pp/90000001");
        session.execute("search c/parent sn/Alex");
        assertTrue(session.getPeople().isEmpty());
        assertEquals(4, session.getDataSnapshot().people().getPeople().size());
    }

    private CanonicalPeopleSession linkedSession() throws Exception {
        CanonicalPeopleSession session = new CanonicalPeopleSession(fixture());
        session.execute("addlesson d/Mon st/1600 et/1700 s/Math tu/Mei Lim rm/R1");
        session.execute("list r/student");
        session.execute("enrol 1 lid/L1");
        session.execute("enrol 2 lid/L1");
        session.execute("mark 1 lid/L1 d/2026-10-05 s/present");
        session.execute("mark 2 lid/L1 d/2026-10-05 s/absent");
        session.execute("unenrol 1 lid/L1");
        return session;
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
