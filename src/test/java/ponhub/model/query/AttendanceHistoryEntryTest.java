package ponhub.model.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ponhub.testutil.Assert.assertThrows;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ponhub.model.attendance.AttendanceStatus;

public class AttendanceHistoryEntryTest {
    @Test
    public void recordedStatuses_excludeUnrecordedOccurrences() {
        // The guides require missing attendance to remain an absent entry, never a third recorded status.
        assertEquals(Set.of(AttendanceStatus.PRESENT, AttendanceStatus.ABSENT), EnumSet.allOf(AttendanceStatus.class));
        assertThrows(IllegalArgumentException.class, () -> AttendanceStatus.valueOf("UNRECORDED"));
    }
}
