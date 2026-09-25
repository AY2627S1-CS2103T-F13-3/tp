package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class RemarkTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Remark(null));
    }

    @Test
    public void constructor_anyString_success() {
        assertDoesNotThrow(() -> new Remark(""));
        assertDoesNotThrow(() -> new Remark("Likes swimming"));
        assertDoesNotThrow(() -> new Remark("!@#$%^&*()"));
    }

    @Test
    public void equals() {
        Remark remark = new Remark("Likes swimming");

        assertEquals(remark, remark);
        assertEquals(remark, new Remark("Likes swimming"));
        assertNotEquals(remark, new Remark("Prefers email"));
        assertNotEquals(remark, null);
        assertNotEquals(remark, "Likes swimming");
    }
}
