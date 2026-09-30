package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.TypicalPersons.ALICE;

import org.junit.jupiter.api.Test;

import seedu.address.testutil.PersonBuilder;

public class RemarkTest {
    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Remark(null));
    }

    @Test
    public void equals_comparesText() {
        Remark remark = new Remark("Any text! 123");
        assertEquals(remark, new Remark("Any text! 123"));
        assertEquals(remark.hashCode(), new Remark("Any text! 123").hashCode());
        assertNotEquals(remark, new Remark(""));
        assertNotEquals(remark, null);
        assertEquals("", new Remark("").toString());
    }

    @Test
    public void person_changedRemark_changesEqualityButNotIdentity() {
        Person edited = new PersonBuilder(ALICE).withRemark("New note").build();
        assertNotEquals(ALICE, edited);
        assertTrue(ALICE.isSamePerson(edited));
    }
}
