package seedu.address.model.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import seedu.address.model.query.AttendanceHistoryEntry.Status;

public class AttendanceHistoryEntryTest {
    @Test
    public void recordedStatuses_excludeUnrecordedOccurrences() {
        // The guides require missing attendance to remain an absent entry, never a third recorded status.
        assertEquals(Set.of(Status.PRESENT, Status.ABSENT), EnumSet.allOf(Status.class));
        assertThrows(IllegalArgumentException.class, () -> Status.valueOf("UNRECORDED"));
    }
}
