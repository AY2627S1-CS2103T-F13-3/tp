package ponhub.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRole;
import ponhub.storage.JsonPonHubDataStorage;

/**
 * Exercises the shared Unicode name contract through commands, relationships and real JSON persistence.
 */
public class CanonicalUnicodeNameIntegrationTest {
    @TempDir
    Path directory;

    @Test
    public void execute_representativeNames_normalizesAllRolesAndReloads() throws Exception {
        Path path = directory.resolve("names.json");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("add r/student n/\u00a0Jose\u0301   Santos\u2003 l/P1 pp/91234567");
        session.execute("add r/tutor n/李明 p/92345678");
        session.execute("add r/parent n/தமிழ் p/91234567");
        session.execute("add r/student n/Anne-Marie O’Neill l/P2 pp/91234567");

        assertEquals(List.of("José Santos", "李明", "தமிழ்", "Anne-Marie O’Neill"),
                session.getPeople().stream().map(person -> person.getContactDetails().getName().fullName).toList());
        assertEquals(List.of("S1", "T1", "P1", "S2"),
                session.getPeople().stream().map(person -> person.getId().toString()).toList());
        assertTrue(Files.readString(path).contains("José Santos"));
        assertEquals(session.getDataSnapshot(), new JsonPonHubDataStorage(path).readData().orElseThrow());
        CanonicalPeopleSession restarted = new CanonicalPeopleSession(path);
        assertEquals(session.getDataSnapshot(), restarted.getDataSnapshot());
        restarted.execute("add r/student n/Next Student l/P1 pp/91234567");
        assertEquals(new PersonId("S3"), restarted.getPeople().getLast().getId());
    }

    @Test
    public void execute_canonicalDuplicatesAndNoOpEdit_preserveBytesAndAllocationForEveryRole() throws Exception {
        for (PersonRole role : PersonRole.values()) {
            Path path = directory.resolve(role.getValue() + ".json");
            CanonicalPeopleSession session = new CanonicalPeopleSession(path);
            String fields = role == PersonRole.STUDENT ? " l/P1 pp/91234567" : " p/91234567";
            String add = "add r/" + role.getValue() + " n/";
            session.execute(add + "José" + fields);
            var before = session.getDataSnapshot();
            byte[] bytes = Files.readAllBytes(path);

            assertThrows(CommandException.class, () -> session.execute(add + "JOSE\u0301" + fields));
            assertTrue(session.execute("edit 1 n/\u00a0Jose\u0301\u2003").getFeedbackToUser()
                    .startsWith("No changes"));
            assertSame(before, session.getDataSnapshot());
            assertArrayEquals(bytes, Files.readAllBytes(path));

            // Accents remain meaningful; a distinct identifying phone also permits the same name.
            session.execute(add + "Jose" + fields);
            before = session.getDataSnapshot();
            bytes = Files.readAllBytes(path);
            assertThrows(CommandException.class, () -> session.execute("edit 2 n/JOSE\u0301"));
            assertSame(before, session.getDataSnapshot());
            assertArrayEquals(bytes, Files.readAllBytes(path));
            session.execute(add + "José" + fields.replace("91234567", "91234568"));
            assertEquals(new PersonId(role.getIdPrefix() + "3"), session.getPeople().getLast().getId());
            assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
        }
    }

    @Test
    public void execute_tutorLookup_normalizesBothLessonCommandsAndRetainsExactPhoneRules() throws Exception {
        Path path = directory.resolve("tutors.json");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("add r/tutor n/José Santos p/00123456");
        String lesson = "addlesson d/Mon st/0900 et/1000 s/Math rm/R1 tu/";
        session.execute(lesson + "JOSE\u0301   SANTOS");
        assertEquals(new PersonId("T1"), session.getDataSnapshot().lessons().getFirst().getTutorId());
        session.execute("add r/tutor n/José Santos p/00987654");
        var before = session.getDataSnapshot();
        byte[] bytes = Files.readAllBytes(path);
        String edit = "editlesson lid/L1 tu/JOSE\u0301 SANTOS";
        assertThrows(CommandException.class, () -> session.execute(edit));
        assertThrows(CommandException.class, () -> session.execute(edit + " tp/99999999"));
        assertThrows(CommandException.class, () -> session.execute(lesson + "Jose Santos"));
        assertSame(before, session.getDataSnapshot());
        assertArrayEquals(bytes, Files.readAllBytes(path));
        session.execute(edit + " tp/00987654");
        assertEquals(new PersonId("T2"), session.getDataSnapshot().lessons().getFirst().getTutorId());
        session.execute("add r/tutor n/李明 p/93456789");
        session.execute("editlesson lid/L1 tu/李明");
        assertEquals(new PersonId("T3"), session.getDataSnapshot().lessons().getFirst().getTutorId());
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
    }

    @Test
    public void execute_renameLoadedAsciiPeople_preservesIdsCountersAndRelationships() throws Exception {
        Path path = directory.resolve("existing.json");
        CanonicalPeopleSession initial = new CanonicalPeopleSession(path);
        initial.execute("add r/tutor n/Mei Lim p/92345678");
        initial.execute("add r/student n/Alex Tan l/P1 pp/91234567");
        initial.execute("add r/parent n/Pat Tan p/91234567");
        initial.execute("addlesson d/Mon st/0900 et/1000 s/Math tu/Mei Lim rm/R1");
        initial.execute("list r/student");
        initial.execute("enrol 1 lid/L1");
        initial.execute("mark 1 lid/L1 d/2026-10-05 s/present");

        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        var before = session.getDataSnapshot();
        session.execute("list r/tutor");
        session.execute("edit 1 n/Jose\u0301 Santos");
        session.execute("list r/student");
        session.execute("edit 1 n/李明");
        session.execute("list r/parent");
        session.execute("edit 1 n/தமிழ்");
        var after = session.getDataSnapshot();
        assertEquals(before.lessons(), after.lessons());
        assertEquals(before.attendance(), after.attendance());
        assertEquals(before.lastAllocatedLessonSequence(), after.lastAllocatedLessonSequence());
        assertEquals(before.people().getLastAllocatedSequences(), after.people().getLastAllocatedSequences());
        assertEquals(before.people().getPeople().stream().map(person -> person.getId()).toList(),
                after.people().getPeople().stream().map(person -> person.getId()).toList());
        session.execute("showlesson lid/L1");
        assertEquals("José Santos", session.getDisplayedLessons().getFirst().getTutorName());
        assertEquals(after, new CanonicalPeopleSession(path).getDataSnapshot());
    }

    @Test
    public void execute_invalidNamesAndUnchangedPrefixGrammar_preserveSnapshotAndFile() throws Exception {
        Path path = directory.resolve("rejections.json");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("add r/tutor n/José Santos p/92345678");
        var before = session.getDataSnapshot();
        byte[] bytes = Files.readAllBytes(path);
        for (String command : List.of("edit 1 n/\u0301José", "edit 1 n/José\u200b", "edit 1 n/José\u2028",
                "edit 1 n/José\u00a0Santos", "edit 1 n/José😀", "edit 1 n/José\ud800", "edit 1 n/José\t",
                "edit 1 n/José x/value", "edit 1 n/José n/Santos", "edit 1 N/José",
                "add r/student n/Ravi s/other Kumar l/P1 pp/91234567", "edit 1 a/Blk 10, #01//02")) {
            assertThrows(ParseException.class, () -> session.execute(command), command);
            assertSame(before, session.getDataSnapshot());
            assertArrayEquals(bytes, Files.readAllBytes(path));
        }
    }

    @Test
    public void execute_excessiveCombiningRuns_preservesEveryNameCommandAndNormalReload() throws Exception {
        Path path = directory.resolve("normalization-budget.json");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("add r/tutor n/Mei Lim p/92345678");
        session.execute("add r/student n/Alex Tan l/P1 pp/91234567");
        session.execute("addlesson d/Mon st/0900 et/1000 s/Math tu/Mei Lim rm/R1");
        var before = session.getDataSnapshot();
        byte[] bytes = Files.readAllBytes(path);
        String oversizedName = "A" + "\u0315\u0327".repeat(20_000);
        for (String command : List.of("add r/student n/" + oversizedName + " l/P1 pp/91234568",
                "add r/tutor n/" + oversizedName + " p/93456789",
                "add r/parent n/" + oversizedName + " p/94567890", "edit 2 n/" + oversizedName,
                "addlesson d/Tue st/0900 et/1000 s/Math rm/R2 tu/" + oversizedName,
                "editlesson lid/L1 tu/" + oversizedName)) {
            assertThrows(ParseException.class, () -> session.execute(command));
            assertSame(before, session.getDataSnapshot());
            assertArrayEquals(bytes, Files.readAllBytes(path));
        }

        String composedName = "\u1f82".repeat(100);
        session.execute("edit 2 n/" + Normalizer.normalize(composedName, Normalizer.Form.NFD));
        assertEquals(composedName, session.getPeople().get(1).getContactDetails().getName().fullName);
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
    }
}
