package seedu.address.model.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class AttendanceStatusTest {

    @Test
    public void parse_mixedCaseAndWhitespace_returnsCanonicalStatus() {
        assertEquals(AttendanceStatus.PRESENT, AttendanceStatus.parse("present"));
        assertEquals(AttendanceStatus.PRESENT, AttendanceStatus.parse(" PrEsEnT "));
        assertEquals(AttendanceStatus.ABSENT, AttendanceStatus.parse("ABSENT"));
        assertEquals("present", AttendanceStatus.PRESENT.toString());
        assertEquals("absent", AttendanceStatus.ABSENT.toString());
    }

    @Test
    public void parse_unknownOrMissingStatus_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> AttendanceStatus.parse(""));
        assertThrows(IllegalArgumentException.class, () -> AttendanceStatus.parse("unrecorded"));
        assertThrows(IllegalArgumentException.class, () -> AttendanceStatus.parse("late"));
        assertThrows(NullPointerException.class, () -> AttendanceStatus.parse(null));
    }
}
