package seedu.address.commons.core.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class IndexTest {

    @Test
    public void createOneBasedIndex() {
        // invalid index
        assertThrows(IndexOutOfBoundsException.class, () -> Index.fromOneBased(0));

        // check equality using the same base
        assertEquals(1, Index.fromOneBased(1).getOneBased());
        assertEquals(5, Index.fromOneBased(5).getOneBased());

        // convert from one-based index to zero-based index
        assertEquals(0, Index.fromOneBased(1).getZeroBased());
        assertEquals(4, Index.fromOneBased(5).getZeroBased());
    }

    @Test
    public void createZeroBasedIndex() {
        // invalid index
        assertThrows(IndexOutOfBoundsException.class, () -> Index.fromZeroBased(-1));

        // check equality using the same base
        assertEquals(0, Index.fromZeroBased(0).getZeroBased());
        assertEquals(5, Index.fromZeroBased(5).getZeroBased());

        // convert from zero-based index to one-based index
        assertEquals(1, Index.fromZeroBased(0).getOneBased());
        assertEquals(6, Index.fromZeroBased(5).getOneBased());
    }

    @Test
    public void fromOneBased_nonPositiveInput_throwsIndexOutOfBoundsException() {
        for (int invalidIndex : new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -1, 0}) {
            assertThrows(IndexOutOfBoundsException.class, () -> Index.fromOneBased(invalidIndex));
        }
    }

    @Test
    public void fromZeroBased_unrepresentableInput_throwsIndexOutOfBoundsException() {
        for (int invalidIndex : new int[]{Integer.MIN_VALUE, -1, Integer.MAX_VALUE}) {
            assertThrows(IndexOutOfBoundsException.class, () -> Index.fromZeroBased(invalidIndex));
        }
    }

    @Test
    public void fromOneBased_validBoundaries_roundTripWithoutOverflow() {
        for (int oneBasedIndex : new int[]{1, 2, Integer.MAX_VALUE - 1, Integer.MAX_VALUE}) {
            Index index = Index.fromOneBased(oneBasedIndex);
            assertEquals(oneBasedIndex, index.getOneBased());
            assertEquals(oneBasedIndex - 1, index.getZeroBased());
            assertEquals(index, Index.fromZeroBased(index.getZeroBased()));
            assertEquals(index, Index.fromOneBased(index.getOneBased()));
        }
    }

    @Test
    public void fromZeroBased_validBoundaries_roundTripWithoutOverflow() {
        for (int zeroBasedIndex : new int[]{0, 1, Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1}) {
            Index index = Index.fromZeroBased(zeroBasedIndex);
            assertEquals(zeroBasedIndex, index.getZeroBased());
            assertEquals(zeroBasedIndex + 1, index.getOneBased());
            assertEquals(index, Index.fromOneBased(index.getOneBased()));
            assertEquals(index, Index.fromZeroBased(index.getZeroBased()));
        }
    }

    @Test
    public void hashCode_equalValues_supportHashCollections() {
        for (int oneBasedIndex : new int[]{1, 5, Integer.MAX_VALUE}) {
            Index oneBased = Index.fromOneBased(oneBasedIndex);
            Index sameOneBased = Index.fromOneBased(oneBasedIndex);
            Index zeroBased = Index.fromZeroBased(oneBasedIndex - 1);
            assertEquals(oneBased, zeroBased);
            assertEquals(zeroBased, oneBased);
            assertEquals(oneBased.hashCode(), sameOneBased.hashCode());
            assertEquals(oneBased.hashCode(), zeroBased.hashCode());

            Set<Index> indices = new HashSet<>();
            assertTrue(indices.add(oneBased));
            assertFalse(indices.add(sameOneBased));
            assertFalse(indices.add(zeroBased));
            assertEquals(1, indices.size());

            Map<Index, String> valuesByIndex = new HashMap<>();
            valuesByIndex.put(oneBased, "original");
            assertEquals("original", valuesByIndex.get(zeroBased));
            assertEquals("original", valuesByIndex.put(zeroBased, "updated"));
            assertEquals(1, valuesByIndex.size());
            assertEquals("updated", valuesByIndex.get(sameOneBased));

            Index differentIndex = Index.fromOneBased(oneBasedIndex == 1 ? 2 : 1);
            assertTrue(indices.add(differentIndex));
            assertEquals(2, indices.size());
            assertEquals("updated", valuesByIndex.remove(sameOneBased));
            assertTrue(valuesByIndex.isEmpty());
        }
    }

    @Test
    public void equals() {
        final Index fifthPersonIndex = Index.fromOneBased(5);

        // same values -> returns true
        assertTrue(fifthPersonIndex.equals(Index.fromOneBased(5)));
        assertTrue(fifthPersonIndex.equals(Index.fromZeroBased(4)));

        // same object -> returns true
        assertTrue(fifthPersonIndex.equals(fifthPersonIndex));

        // null -> returns false
        assertFalse(fifthPersonIndex.equals(null));

        // different types -> returns false
        assertFalse(fifthPersonIndex.equals(5.0f));

        // different index -> returns false
        assertFalse(fifthPersonIndex.equals(Index.fromOneBased(1)));
    }

    @Test
    public void toStringMethod() {
        Index index = Index.fromZeroBased(0);
        String expected = Index.class.getCanonicalName() + "{zeroBasedIndex=" + index.getZeroBased() + "}";
        assertEquals(expected, index.toString());
    }
}
