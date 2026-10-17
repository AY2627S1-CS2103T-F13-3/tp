package seedu.address.commons.util;

import static java.util.Objects.requireNonNull;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Helper functions for handling strings.
 */
public class StringUtil {

    /**
     * Returns {@code value} with surrounding ASCII spaces removed, preserving other characters.
     */
    public static String trimSpaces(String value) {
        requireNonNull(value);
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == ' ') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == ' ') {
            end--;
        }
        return value.substring(start, end);
    }

    /**
     * Returns {@code value} with surrounding ASCII spaces removed and interior runs collapsed.
     * Preserves all other characters, including controls, for subsequent field validation.
     */
    public static String normalizeSpaces(String value) {
        requireNonNull(value);
        StringBuilder normalizedValue = new StringBuilder();
        boolean hasPendingSpace = false;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == ' ') {
                hasPendingSpace = true;
            } else {
                if (hasPendingSpace && !normalizedValue.isEmpty()) {
                    normalizedValue.append(' ');
                }
                normalizedValue.append(character);
                hasPendingSpace = false;
            }
        }
        return normalizedValue.toString();
    }

    /**
     * Returns a detailed message of {@code t}, including the stack trace.
     */
    public static String getDetails(Throwable t) {
        requireNonNull(t);
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return t.getMessage() + "\n" + sw.toString();
    }

    /**
     * Returns true if {@code s} represents a non-zero unsigned integer
     * e.g. 1, 2, 3, ..., {@code Integer.MAX_VALUE} <br>
     * Will return false for any other non-null string input
     * e.g. empty string, "-1", "0", "+1", and " 2 " (untrimmed), "3 0" (contains whitespace), "1 a" (contains letters)
     * @throws NullPointerException if {@code s} is null.
     */
    public static boolean isNonZeroUnsignedInteger(String s) {
        requireNonNull(s);

        try {
            int value = Integer.parseInt(s);
            return value > 0 && !s.startsWith("+"); // "+1" is successfully parsed by Integer#parseInt(String)
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}
