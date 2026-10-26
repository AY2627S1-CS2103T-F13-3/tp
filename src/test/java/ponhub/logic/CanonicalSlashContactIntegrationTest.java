package ponhub.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRole;

/**
 * Exercises bounded slash contact values through commands, duplicate keys, relationships and real JSON reload.
 */
public class CanonicalSlashContactIntegrationTest {
    @TempDir
    Path directory;

    @Test
    public void execute_addEditDuplicatesAndNoOp_preservesIdentityForEveryRole() throws Exception {
        for (PersonRole role : PersonRole.values()) {
            Path path = directory.resolve(role.getValue() + ".json");
            CanonicalPeopleSession session = new CanonicalPeopleSession(path);
            String fields = role == PersonRole.STUDENT ? " l/P1 pp/91234567" : " p/91234567";
            String add = "add r/" + role.getValue() + " n/";
            session.execute(add + "José s/o Kumar" + fields + " a/Blk 10, #01/02");
            var before = session.getDataSnapshot();
            byte[] bytes = Files.readAllBytes(path);
            assertThrows(CommandException.class, () -> session.execute(add + "JOSE\u0301 s/o KUMAR" + fields));
            assertTrue(session.execute("edit 1 n/Jose\u0301  s/o  Kumar a/Blk 10,  #01/02")
                    .getFeedbackToUser().startsWith("No changes"));
            assertSame(before, session.getDataSnapshot());
            assertArrayEquals(bytes, Files.readAllBytes(path));

            session.execute(add + "Anita d/o Kumar" + fields);
            before = session.getDataSnapshot();
            bytes = Files.readAllBytes(path);
            assertThrows(CommandException.class, () -> session.execute("edit 2 n/JOSÉ s/o KUMAR"));
            assertSame(before, session.getDataSnapshot());
            assertArrayEquals(bytes, Files.readAllBytes(path));
            session.execute("edit 1 n/Ravi s/o Kumar a/Block A12/34B");
            assertEquals(new PersonId(role.getIdPrefix() + "1"), session.getPeople().getFirst().getId());
            assertEquals("Block A12/34B", session.getPeople().getFirst().getContactDetails()
                    .getAddress().orElseThrow().value);
            CanonicalPeopleSession restarted = new CanonicalPeopleSession(path);
            assertEquals(session.getDataSnapshot(), restarted.getDataSnapshot());
            restarted.execute(add + "Next Person" + fields);
            assertEquals(new PersonId(role.getIdPrefix() + "3"), restarted.getPeople().getLast().getId());
        }
    }

    @Test
    public void execute_existingAsciiStoreAndTutorLookup_retainsLinksAndExactPhoneRules() throws Exception {
        Path path = directory.resolve("existing.json");
        CanonicalPeopleSession original = new CanonicalPeopleSession(path);
        original.execute("add r/tutor n/Mei Lim p/00123456");
        original.execute("add r/student n/Alex Tan l/P1 pp/91234567");
        original.execute("add r/parent n/Pat Tan p/91234567");
        original.execute("addlesson d/Mon st/0900 et/1000 s/Math tu/Mei Lim rm/R1");
        original.execute("list r/student");
        original.execute("enrol 1 lid/L1");
        original.execute("mark 1 lid/L1 d/2026-10-05 s/present");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        var before = session.getDataSnapshot();
        session.execute("list r/tutor");
        session.execute("edit 1 n/José s/o Kumar a/Blk 12/34");
        session.execute("list r/student");
        session.execute("edit 1 n/Anita d/o Kumar a/Blk 10, #01/02");
        session.execute("list r/parent");
        session.execute("edit 1 a/Block A12/34B");
        var after = session.getDataSnapshot();
        assertEquals(before.lessons(), after.lessons());
        assertEquals(before.attendance(), after.attendance());
        assertEquals(before.people().getLastAllocatedSequences(), after.people().getLastAllocatedSequences());
        assertEquals(before.people().getPeople().stream().map(person -> person.getId()).toList(),
                after.people().getPeople().stream().map(person -> person.getId()).toList());
        assertEquals(after, new CanonicalPeopleSession(path).getDataSnapshot());

        session.execute("addlesson tu/JOSE\u0301 s/o KUMAR d/Tue st/0900 et/1000 s/Math rm/R2");
        assertEquals(new PersonId("T1"), session.getDataSnapshot().lessons().getLast().getTutorId());
        session.execute("add r/tutor n/José s/o Kumar p/00987654");
        before = session.getDataSnapshot();
        byte[] bytes = Files.readAllBytes(path);
        assertThrows(CommandException.class, () -> session.execute("editlesson lid/L2 tu/José s/o Kumar"));
        assertThrows(CommandException.class, () -> session.execute("editlesson lid/L2 tu/José s/o Kumar tp/999"));
        assertSame(before, session.getDataSnapshot());
        assertArrayEquals(bytes, Files.readAllBytes(path));
        session.execute("editlesson lid/L2 tu/JOSE\u0301 s/o KUMAR tp/00987654");
        assertEquals(new PersonId("T2"), session.getDataSnapshot().lessons().getLast().getTutorId());
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
    }

    @Test
    public void execute_invalidLiteralsAndPrefixTypos_preservesSnapshotAndFile() throws Exception {
        Path path = directory.resolve("invalid.json");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("add r/tutor n/Ravi s/o Kumar p/92345678 a/Blk #01/02");
        var before = session.getDataSnapshot();
        byte[] bytes = Files.readAllBytes(path);
        for (String command : List.of("edit 1 n/s/o", "edit 1 n/Ravi d/o", "edit 1 n/Ravi/Kumar",
                "edit 1 n/Ravi s/other Kumar", "edit 1 n/Ravi S/o Kumar", "edit 1 n/Ravi s/o Kumar x/value",
                "edit 1 n/Ravi s/o Kumar n/Other", "edit 1 a/A/B", "edit 1 a/Blk #01//02",
                "edit 1 a/Blk #01/02 a/Other", "edit 1 a/Blk #01/02 x/value", "edit 1 a/Block\u2028",
                "edit 1 a/Block\u200b", "edit 1 a/Block\ud800", "edit 1 n/Ravi s/o Kumar\t",
                "add r/student n/Ravi s/o Kumar l/P1 pp/91234567 N/Other",
                "addlesson d/Mon st/0900 et/1000 s/Math tu/Ravi s/o Kumar rm/R1 x/value")) {
            assertThrows(ParseException.class, () -> session.execute(command), command);
            assertSame(before, session.getDataSnapshot());
            assertArrayEquals(bytes, Files.readAllBytes(path));
        }
    }

    @Test
    public void execute_fragmentSearch_retainsExistingSlashQueryLimit() throws Exception {
        Path path = directory.resolve("search.json");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("add r/student n/Ravi s/o Kumar l/P1 pp/91234567 a/Blk 10, #01/02");
        session.execute("search c/student n/Ravi a/#01");
        assertEquals(List.of(new PersonId("S1")), session.getPeople().stream().map(person -> person.getId()).toList());
        var before = session.getDataSnapshot();
        byte[] bytes = Files.readAllBytes(path);
        // Search still treats s/ as its subject filter, rather than as a name literal.
        session.execute("search c/student n/Ravi s/o Kumar");
        assertTrue(session.getPeople().isEmpty());
        assertThrows(ParseException.class, () -> session.execute("search c/student a/#01/02"));
        assertSame(before, session.getDataSnapshot());
        assertArrayEquals(bytes, Files.readAllBytes(path));
    }
}
