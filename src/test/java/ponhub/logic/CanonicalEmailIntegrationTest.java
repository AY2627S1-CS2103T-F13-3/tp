package ponhub.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.person.Email;
import ponhub.model.person.PersonRole;

/** Exercises email validation and preservation through commands and canonical storage. */
public class CanonicalEmailIntegrationTest {
    @TempDir
    Path directory;

    @Test
    public void execute_apostropheEmail_addsEditsAndReloadsEachRole() throws Exception {
        for (PersonRole role : PersonRole.values()) {
            Path path = directory.resolve(role.getValue() + ".json");
            CanonicalPeopleSession session = new CanonicalPeopleSession(path);
            String requiredFields = role == PersonRole.STUDENT ? " l/P1 pp/91234569" : " p/91234569";
            String addedEmail = "Sean.O'Connor+School@Example.COM";
            session.execute("add r/" + role.getValue() + " n/Sean O'Connor"
                    + requiredFields + " e/" + addedEmail);
            assertEquals(addedEmail, session.getPeople().getFirst().getContactDetails()
                    .getEmail().orElseThrow().value);

            CanonicalPeopleSession reloaded = new CanonicalPeopleSession(path);
            assertEquals(session.getDataSnapshot(), reloaded.getDataSnapshot());
            reloaded.execute("list r/" + role.getValue());
            String editedEmail = "Sean.O'Connor.Work@Example.COM";
            assertEquals("Edited " + role.getValue() + ": Sean O'Connor ("
                    + reloaded.getPeople().getFirst().getId() + ").",
                    reloaded.execute("edit 1 e/" + editedEmail).getFeedbackToUser());

            CanonicalPeopleSession editedReload = new CanonicalPeopleSession(path);
            assertEquals(reloaded.getDataSnapshot(), editedReload.getDataSnapshot());
            assertEquals(editedEmail, editedReload.getPeople().getFirst().getContactDetails()
                    .getEmail().orElseThrow().value);
        }
    }

    @Test
    public void execute_invalidEmail_preservesDataAfterRejectedAddAndEdit() throws Exception {
        Path path = directory.resolve("rejections.json");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("add r/parent n/Sean O'Connor p/91234569 e/sean.o'connor@example.com");
        var before = session.getDataSnapshot();
        byte[] saved = Files.readAllBytes(path);
        for (String email : List.of("sean.o\u2019connor@example.com", "sean@o'connor.com",
                "sean.o'connor@localhost", "sean..o'connor@example.com", "'".repeat(243) + "@example.com")) {
            for (String command : List.of("add r/parent n/Pat Tan p/91234567 e/" + email,
                    "edit 1 e/" + email)) {
                assertEquals(Email.MESSAGE_CONSTRAINTS,
                        assertThrows(ParseException.class, () -> session.execute(command)).getMessage());
                assertEquals(before, session.getDataSnapshot());
                assertArrayEquals(saved, Files.readAllBytes(path));
            }
        }
        assertEquals(before, new CanonicalPeopleSession(path).getDataSnapshot());
    }
}
