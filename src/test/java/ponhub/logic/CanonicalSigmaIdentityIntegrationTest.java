package ponhub.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ponhub.logic.commands.exceptions.CommandException;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRole;
import ponhub.storage.ProtectedPonHubDataStorage.LoadStatus;

/**
 * Exercises sigma-equivalent identity checks and Tutor selection while preserving older saved identities.
 */
public class CanonicalSigmaIdentityIntegrationTest {
    @TempDir
    Path directory;

    @Test
    public void execute_sigmaDuplicates_rejectsAddAndEditForEveryRoleWithoutSavingOrAllocating() throws Exception {
        for (PersonRole role : PersonRole.values()) {
            for (List<String> names : List.of(List.of("Σ", "σ", "ς"), List.of("ΝΊΚΟΣ", "Νίκος", "Νίκοσ"))) {
                Path path = directory.resolve(role.getValue() + names.getFirst() + ".json");
                CanonicalPeopleSession session = new CanonicalPeopleSession(path);
                String fields = role == PersonRole.STUDENT ? " l/P1 pp/91234567" : " p/91234567";
                String add = "add r/" + role.getValue() + " n/";
                session.execute(add + names.getFirst() + fields);
                for (String name : names) {
                    assertRejectedWithoutSaving(session, path, add + name + fields);
                }
                session.execute(add + "Other Name" + fields);
                assertEquals(new PersonId(role.getIdPrefix() + "2"), session.getPeople().getLast().getId());
                for (String name : names) {
                    assertRejectedWithoutSaving(session, path, "edit 2 n/" + name);
                }
                session.execute(add + names.getLast() + fields.replace("91234567", "91234568"));
                assertEquals(new PersonId(role.getIdPrefix() + "3"), session.getPeople().getLast().getId());
                String identifyingPhone = role == PersonRole.STUDENT ? " pp/" : " p/";
                assertRejectedWithoutSaving(session, path, "edit 3" + identifyingPhone + "91234567");
                assertEquals(names.getFirst(), session.getPeople().getFirst().getContactDetails().getName().fullName);
                session.execute("edit 1 n/" + names.getLast());
                assertEquals(names.getLast(), session.getPeople().getFirst().getContactDetails().getName().fullName);
                assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
            }
        }
    }

    @Test
    public void execute_sigmaTutorNames_resolvesBothLessonCommandsAndExactPhones() throws Exception {
        for (List<String> names : List.of(List.of("Σ", "σ", "ς"), List.of("ΝΊΚΟΣ", "Νίκος", "Νίκοσ"))) {
            for (String storedName : names) {
                Path path = Files.createTempDirectory(directory, "lookup-").resolve("people.json");
                CanonicalPeopleSession session = new CanonicalPeopleSession(path);
                session.execute("add r/tutor n/" + storedName + " p/00123456");
                String addLesson = "addlesson d/Mon st/0900 et/1000 s/Math rm/R1 tu/";
                session.execute(addLesson + names.getLast());
                for (String query : names) {
                    session.execute("editlesson lid/L1 tu/" + query);
                    assertEquals(new PersonId("T1"), session.getDataSnapshot().lessons().getFirst().getTutorId());
                }
                assertRejectedWithoutSaving(session, path, "editlesson lid/L1 tu/" + names.getLast()
                        + " tp/123456");
                session.execute("add r/tutor n/" + names.getLast() + " p/00987654");
                assertRejectedWithoutSaving(session, path, "editlesson lid/L1 tu/" + names.getFirst());
                session.execute("editlesson lid/L1 tu/" + names.getFirst() + " tp/00987654");
                assertEquals(new PersonId("T2"), session.getDataSnapshot().lessons().getFirst().getTutorId());
                assertEquals(storedName, session.getDataSnapshot().people().getPeople().getFirst()
                        .getContactDetails().getName().fullName);
                assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
            }
        }
    }

    @Test
    public void execute_legacySigmaIdentities_loadsAndEditsWithoutChangingRelationships() throws Exception {
        Path path = saveLegacyFixture();
        byte[] bytes = Files.readAllBytes(path);
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        assertEquals(LoadStatus.LOADED, session.getLoadStatus());
        assertFalse(session.getDataLoadError().isPresent());
        var before = session.getDataSnapshot();
        session.execute("search c/student n/σ");
        assertEquals(List.of("S1", "S2"), session.getPeople().stream()
                .map(person -> person.getId().toString()).toList());
        assertArrayEquals(bytes, Files.readAllBytes(path));

        for (PersonRole role : PersonRole.values()) {
            session.execute("list r/" + role.getValue());
            session.execute("edit 1 e/first@example.com");
            session.execute("edit 2 a/10 Main Road");
            String fields = role == PersonRole.STUDENT ? " l/P1 pp/91234567" : " p/91234567";
            assertRejectedWithoutSaving(session, path, "add r/" + role.getValue() + " n/σ" + fields);
        }
        var after = session.getDataSnapshot();
        assertEquals(before.lessons(), after.lessons());
        assertEquals(before.attendance(), after.attendance());
        assertEquals(before.people().getLastAllocatedSequences(), after.people().getLastAllocatedSequences());
        assertEquals(before.people().getPeople().stream().map(person -> person.getId()).toList(),
                after.people().getPeople().stream().map(person -> person.getId()).toList());
        assertEquals(List.of("Σ", "ς", "Σ", "ς", "Σ", "ς"), after.people().getPeople().stream()
                .map(person -> person.getContactDetails().getName().fullName).toList());
        assertEquals(after, new CanonicalPeopleSession(path).getDataSnapshot());
    }

    @Test
    public void execute_legacySameNameAndPhoneTutors_rejectsAmbiguityUntilIdentityIsCorrected() throws Exception {
        Path path = saveLegacyFixture();
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        for (String selection : List.of("Σ", "σ", "ς", "Σ tp/91234567", "ς tp/91234567")) {
            assertRejectedWithoutSaving(session, path, "addlesson d/Tue st/0900 et/1000 s/Math rm/R3 tu/"
                    + selection);
            assertRejectedWithoutSaving(session, path, "editlesson lid/L1 tu/" + selection);
        }
        session.execute("list r/tutor");
        session.execute("edit 2 p/92345678");
        assertRejectedWithoutSaving(session, path, "editlesson lid/L1 tu/σ");
        session.execute("editlesson lid/L1 tu/σ tp/92345678");
        assertEquals(new PersonId("T2"), session.getDataSnapshot().lessons().getFirst().getTutorId());
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
        for (String topic : List.of("addlesson", "editlesson")) {
            String help = session.execute("help " + topic).getFeedbackToUser();
            assertTrue(help.contains("full name"));
            assertTrue(help.contains("Σ/σ/ς"));
            assertTrue(help.contains("edit a tutor's name or phone first"));
        }
    }

    private Path saveLegacyFixture() throws Exception {
        Path path = directory.resolve("legacy-" + System.nanoTime() + ".json");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("add r/tutor n/Σ p/91234567");
        session.execute("add r/tutor n/Legacy Sigma p/91234567");
        session.execute("add r/student n/Σ l/P1 pp/91234567");
        session.execute("add r/student n/Legacy Sigma l/P1 pp/91234567");
        session.execute("add r/parent n/Σ p/91234567");
        session.execute("add r/parent n/Legacy Sigma p/91234567");
        session.execute("addlesson d/Mon st/0900 et/1000 s/Math rm/R1 tu/Σ");
        session.execute("addlesson d/Mon st/1100 et/1200 s/Math rm/R2 tu/Legacy Sigma");
        session.execute("list r/student");
        session.execute("enrol 1 lid/L1");
        session.execute("enrol 2 lid/L2");
        session.execute("mark 1 lid/L1 d/2026-10-05 s/present");
        // Recreate spellings accepted by older versions, independently of the corrected add policy.
        Files.writeString(path, Files.readString(path).replace("Legacy Sigma", "ς"));
        return path;
    }

    private static void assertRejectedWithoutSaving(CanonicalPeopleSession session, Path path, String command)
            throws Exception {
        var before = session.getDataSnapshot();
        var people = List.copyOf(session.getPeople());
        var lessons = List.copyOf(session.getDisplayedLessons());
        byte[] bytes = Files.readAllBytes(path);
        assertThrows(CommandException.class, () -> session.execute(command), command);
        assertSame(before, session.getDataSnapshot());
        assertEquals(people, session.getPeople());
        assertEquals(lessons, session.getDisplayedLessons());
        assertArrayEquals(bytes, Files.readAllBytes(path));
    }
}
