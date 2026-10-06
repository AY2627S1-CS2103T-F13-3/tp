package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class PersonIdTest {
    @Test
    public void constructor_normalizedId_matchesFactory() {
        PersonId studentId = new PersonId(" s12 ");
        assertEquals(PersonId.of(PersonRole.STUDENT, 12), studentId);
        assertEquals(PersonRole.STUDENT, studentId.getRole());
        assertEquals(12, studentId.getSequence());
        assertEquals("S12", studentId.getValue());
        assertEquals("S12", studentId.toString());
    }

    @Test
    public void constructor_sequenceBoundaries_acceptsEveryRole() {
        for (PersonRole role : PersonRole.values()) {
            PersonId smallestId = new PersonId(role.getIdPrefix() + "1");
            PersonId largestId = new PersonId(role.getIdPrefix() + Long.MAX_VALUE);
            assertEquals(PersonId.of(role, 1), smallestId);
            assertEquals(PersonId.of(role, Long.MAX_VALUE), largestId);
            assertEquals(role, largestId.getRole());
            assertEquals(Long.MAX_VALUE, largestId.getSequence());
        }
    }

    @Test
    public void constructor_invalidIds_rejectsMalformedAndOverflowingSequences() {
        String[] invalidIds = {
            "", " ", "S", "S0", "S01", "S-1", "S+1", "S1.0", "S 1", "S1 T2", "X1", "student1",
            "1", "S9223372036854775808", "T999999999999999999999999", "P0001", "S\u0661"
        };
        for (String invalidId : invalidIds) {
            assertFalse(PersonId.isValid(invalidId), invalidId);
            IllegalArgumentException exception =
                    assertThrows(IllegalArgumentException.class, () -> new PersonId(invalidId));
            assertEquals(PersonId.MESSAGE_CONSTRAINTS, exception.getMessage());
        }

        assertFalse(PersonId.isValid(null));
        assertThrows(NullPointerException.class, () -> new PersonId(null));
    }

    @Test
    public void isValid_normalizedAndBoundaryIds_acceptsValidInputs() {
        String[] validIds = {"S1", "t17", " p83 ", "\tS9\n", "P" + Long.MAX_VALUE};
        for (String validId : validIds) {
            assertTrue(PersonId.isValid(validId), validId);
        }
    }

    @Test
    public void of_nonPositiveSequenceOrNullRole_throwsException() {
        for (PersonRole role : PersonRole.values()) {
            assertThrows(IllegalArgumentException.class, () -> PersonId.of(role, 0));
            assertThrows(IllegalArgumentException.class, () -> PersonId.of(role, -1));
            assertThrows(IllegalArgumentException.class, () -> PersonId.of(role, Long.MIN_VALUE));
        }

        assertThrows(NullPointerException.class, () -> PersonId.of(null, 1));
    }

    @Test
    public void equalsAndHashCode_normalization_deduplicatesOnlySameRoleAndSequence() {
        PersonId studentId = new PersonId("S7");
        PersonId normalizedId = new PersonId(" s7 ");
        PersonId tutorId = new PersonId("T7");
        PersonId nextId = new PersonId("S8");
        assertEquals(studentId, studentId);
        assertEquals(studentId, normalizedId);
        assertEquals(normalizedId, studentId);
        assertEquals(studentId.hashCode(), normalizedId.hashCode());
        assertNotEquals(studentId, tutorId);
        assertNotEquals(studentId, nextId);
        assertNotEquals(studentId, null);
        assertNotEquals(studentId, "S7");

        Set<PersonId> uniqueIds = new HashSet<>(List.of(studentId, normalizedId, tutorId, nextId));
        assertEquals(3, uniqueIds.size());
    }
}
