package ponhub.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ponhub.logic.commands.exceptions.CommandException;
import ponhub.model.person.PersonId;
import ponhub.model.person.Student;
import ponhub.storage.JsonPonHubDataStorage;
import ponhub.storage.ProtectedPonHubDataStorage;

/**
 * Checks lost exact-phone relationship feedback through the saved canonical editing boundary.
 */
public class ParentStudentEditWarningTest {
    @TempDir
    Path directory;

    @Test
    public void edit_parentPhone_reportsAllLinkedStudentsEvenOutsideSearchAndPersists() throws Exception {
        CanonicalPeopleSession session = fixture();
        session.execute("search c/parent sn/Alex");
        var before = session.getDataSnapshot();
        String feedback = session.execute("edit 1 p/90000000").getFeedbackToUser();
        assertEquals("Edited parent: Pat Tan (P1).\nWarning: Parent-Student links removed: P1 / S1, P1 / S2. "
                + "Update the corresponding phone values to restore a link.", feedback);
        assertTrue(session.getPeople().isEmpty());
        assertEquals(before.lessons(), session.getDataSnapshot().lessons());
        assertEquals(before.attendance(), session.getDataSnapshot().attendance());
        assertEquals(before.people().getLastAllocatedSequences(),
                session.getDataSnapshot().people().getLastAllocatedSequences());
        assertEquals("00123456", ((Student) session.getDataSnapshot().people().getPeople().get(1))
                .getParentPhone().value);
        assertEquals(session.getDataSnapshot(),
                new CanonicalPeopleSession(session.getDataFilePath()).getDataSnapshot());
    }

    @Test
    public void edit_studentParentPhone_reportsEachOldParentEvenWhenNewParentMatches() throws Exception {
        CanonicalPeopleSession session = fixture();
        session.execute("add r/parent n/Other Parent p/00123456");
        session.execute("add r/parent n/New Parent p/90000000");
        session.execute("search c/student n/Alex");
        assertEquals("Edited student: Alex Tan (S1).\nWarning: Parent-Student links removed: P1 / S1, P2 / S1. "
                + "Update the corresponding phone values to restore a link.",
                session.execute("edit 1 pp/90000000").getFeedbackToUser());
        session.execute("search c/parent sn/Alex");
        assertEquals(List.of(new PersonId("P3")), session.getPeople().stream().map(person -> person.getId()).toList());
        assertEquals(session.getDataSnapshot(),
                new CanonicalPeopleSession(session.getDataFilePath()).getDataSnapshot());
    }

    @Test
    public void edit_unchangedPhonesAndUnrelatedFields_doNotWarnOrWriteForNoOp() throws Exception {
        CanonicalPeopleSession setup = fixture();
        Path path = setup.getDataFilePath();
        AtomicInteger saves = new AtomicInteger();
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            saves.incrementAndGet();
            new JsonPonHubDataStorage(path).saveData(state);
        });
        session.execute("list r/parent");
        var before = session.getDataSnapshot();
        assertEquals("No changes to parent: Pat Tan (P1).",
                session.execute("edit 1 p/00123456").getFeedbackToUser());
        assertEquals(0, saves.get());
        assertSame(before, session.getDataSnapshot());
        assertEquals("Edited parent: Pat New (P1).", session.execute("edit 1 n/Pat New").getFeedbackToUser());
        session.execute("list r/student");
        assertEquals("Edited student: Alex Tan (S1).",
                session.execute("edit 1 p/81111111 e/alex@example.com").getFeedbackToUser());
        assertEquals("No changes to student: Alex Tan (S1).",
                session.execute("edit 1 pp/00123456").getFeedbackToUser());
        session.execute("list r/tutor");
        assertEquals("Edited tutor: Mei Lim (T1).", session.execute("edit 1 p/90000000").getFeedbackToUser());
    }

    @Test
    public void edit_previouslyUnlinkedPhones_doesNotWarnForNewLinkOrDifferentUnlinkedValue() throws Exception {
        CanonicalPeopleSession session = fixture();
        session.execute("add r/parent n/Unlinked Parent p/99999999");
        session.execute("search c/parent n/Unlinked");
        assertEquals("Edited parent: Unlinked Parent (P2).",
                session.execute("edit 1 p/88888888").getFeedbackToUser());
        session.execute("add r/student n/Unlinked Student l/P1 pp/77777777");
        session.execute("search c/student n/Unlinked");
        assertEquals("Edited student: Unlinked Student (S3).",
                session.execute("edit 1 pp/00123456").getFeedbackToUser());
        session.execute("search c/parent sn/Unlinked");
        assertEquals(new PersonId("P1"), session.getPeople().getFirst().getId());
    }

    @Test
    public void edit_failedSaveAndValidation_doNotReturnWarningOrPublishUntilSuccessfulRetry() throws Exception {
        CanonicalPeopleSession setup = fixture();
        Path path = setup.getDataFilePath();
        AtomicBoolean failSave = new AtomicBoolean(true);
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            if (failSave.get()) {
                throw new IOException("Injected relationship edit save failure");
            }
            new JsonPonHubDataStorage(path).saveData(state);
        });
        session.execute("search c/parent sn/Alex");
        var before = session.getDataSnapshot();
        var people = List.copyOf(session.getPeople());
        byte[] bytes = Files.readAllBytes(path);
        CommandException failure = assertThrows(CommandException.class, () -> session.execute("edit 1 p/90000000"));
        assertTrue(failure.getMessage().contains("Injected relationship edit save failure"));
        assertFalse(failure.getMessage().contains("links removed"));
        assertSame(before, session.getDataSnapshot());
        assertEquals(people, session.getPeople());
        assertArrayEquals(bytes, Files.readAllBytes(path));
        assertThrows(CommandException.class, () -> session.execute("edit 1 p/"));
        assertSame(before, session.getDataSnapshot());
        failSave.set(false);
        assertTrue(session.execute("edit 1 p/90000000").getFeedbackToUser().contains("P1 / S1, P1 / S2"));
        assertTrue(session.getPeople().isEmpty());
    }

    private CanonicalPeopleSession fixture() throws Exception {
        CanonicalPeopleSession session = new CanonicalPeopleSession(directory.resolve("ponhub.json"));
        session.execute("add r/tutor n/Mei Lim p/92345678");
        session.execute("add r/student n/Alex Tan l/P1 pp/00123456");
        session.execute("add r/student n/Jamie Tan l/P2 pp/00123456");
        session.execute("add r/parent n/Pat Tan p/00123456");
        session.execute("addlesson d/Mon st/1600 et/1700 s/Math tu/Mei Lim rm/R1");
        session.execute("list r/student");
        session.execute("enrol 1 lid/L1");
        session.execute("mark 1 lid/L1 d/2026-10-05 s/present");
        return session;
    }
}
