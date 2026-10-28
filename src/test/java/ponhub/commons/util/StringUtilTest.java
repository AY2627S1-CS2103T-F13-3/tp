package ponhub.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.Assert.assertThrows;

import java.io.FileNotFoundException;

import org.junit.jupiter.api.Test;

public class StringUtilTest {

    @Test
    public void stripUnicodeBoundarySpaces_horizontalSeparators_preservesInteriorAndControls() {
        assertEquals("", StringUtil.stripUnicodeBoundarySpaces(""));
        assertEquals("", StringUtil.stripUnicodeBoundarySpaces("\u2003\u00a0\u202f"));
        assertEquals("Alex\u00a0Tan", StringUtil.stripUnicodeBoundarySpaces("\u2003Alex\u00a0Tan\u202f"));
        for (String control : new String[]{"\t", "\n", "\u2028", "\u2029", "\u0000", "\u001b"}) {
            assertEquals(control + "Alex" + control,
                    StringUtil.stripUnicodeBoundarySpaces("\u00a0" + control + "Alex" + control + "\u202f"));
        }
        assertThrows(NullPointerException.class, () -> StringUtil.stripUnicodeBoundarySpaces(null));
    }

    @Test
    public void trimSpaces_onlyAsciiSpaces_preservesOtherCharacters() {
        assertEquals("", StringUtil.trimSpaces(""));
        assertEquals("", StringUtil.trimSpaces(" ".repeat(100_000)));
        assertEquals("A  B", StringUtil.trimSpaces("  A  B  "));
        assertEquals("\tA\n\u00a0", StringUtil.trimSpaces("  \tA\n\u00a0  "));
    }

    @Test
    public void normalizeSpaces_onlyAsciiSpaces_collapsesSpaces() {
        assertEquals("", StringUtil.normalizeSpaces(""));
        assertEquals("", StringUtil.normalizeSpaces(" ".repeat(100_000)));
        assertEquals("Anne-Marie O'Neil", StringUtil.normalizeSpaces("  Anne-Marie   O'Neil  "));
        assertEquals("\tA \n\u00a0", StringUtil.normalizeSpaces("  \tA   \n\u00a0  "));
    }

    //---------------- Tests for isNonZeroUnsignedInteger --------------------------------------

    @Test
    public void isNonZeroUnsignedInteger() {

        // EP: empty strings
        assertFalse(StringUtil.isNonZeroUnsignedInteger("")); // Boundary value
        assertFalse(StringUtil.isNonZeroUnsignedInteger("  "));

        // EP: not a number
        assertFalse(StringUtil.isNonZeroUnsignedInteger("a"));
        assertFalse(StringUtil.isNonZeroUnsignedInteger("aaa"));

        // EP: zero
        assertFalse(StringUtil.isNonZeroUnsignedInteger("0"));

        // EP: zero as prefix
        assertTrue(StringUtil.isNonZeroUnsignedInteger("01"));

        // EP: signed numbers
        assertFalse(StringUtil.isNonZeroUnsignedInteger("-1"));
        assertFalse(StringUtil.isNonZeroUnsignedInteger("+1"));

        // EP: numbers with white space
        assertFalse(StringUtil.isNonZeroUnsignedInteger(" 10 ")); // Leading/trailing spaces
        assertFalse(StringUtil.isNonZeroUnsignedInteger("1 0")); // Spaces in the middle

        // EP: number larger than Integer.MAX_VALUE
        assertFalse(StringUtil.isNonZeroUnsignedInteger(Long.toString(Integer.MAX_VALUE + 1L)));

        // EP: valid numbers, should return true
        assertTrue(StringUtil.isNonZeroUnsignedInteger("1")); // Boundary value
        assertTrue(StringUtil.isNonZeroUnsignedInteger("10"));
        assertTrue(StringUtil.isNonZeroUnsignedInteger(Integer.toString(Integer.MAX_VALUE)));
    }

    //---------------- Tests for getDetails --------------------------------------

    /*
     * Equivalence Partitions: null, valid throwable object
     */

    @Test
    public void getDetails_exceptionGiven() {
        assertTrue(StringUtil.getDetails(new FileNotFoundException("file not found"))
            .contains("java.io.FileNotFoundException: file not found"));
    }

    @Test
    public void getDetails_nullGiven_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringUtil.getDetails(null));
    }

}
