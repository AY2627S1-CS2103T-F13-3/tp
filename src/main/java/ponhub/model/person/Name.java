package ponhub.model.person;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.AppUtil.checkArgument;

import java.text.Normalizer;

import ponhub.commons.util.StringUtil;

/**
 * Represents a person's name in PonHub contact details.
 * Guarantees: immutable; is valid as declared in {@link #isValidName(String)}
 */
public final class Name {

    public static final int MAX_LENGTH = 100;
    public static final String MESSAGE_CONSTRAINTS =
            "Names must contain 1 to 100 Unicode code points after normalization, using letters with attached "
            + "combining marks, spaces, apostrophes (' or ’), hyphens or periods, with at least one letter. "
            + "Standalone lowercase s/o or d/o is allowed between letter-bearing name text. "
            + "Controls, format characters and internal non-ASCII whitespace are not allowed.";

    // Every Unicode scalar has at most four code points in its full canonical decomposition in Java 25.
    // Canonically equivalent input cannot exceed the NFD length of a valid 100-code-point NFC name.
    private static final int MAX_NORMALIZATION_CODE_POINTS = MAX_LENGTH * 4;

    public final String fullName;

    /**
     * Creates an NFC-normalized {@code Name}, trimming Unicode boundary spaces and collapsing ASCII spaces.
     * Rejects controls, format characters, line/paragraph separators and lone surrogates before trimming.
     *
     * @param name A valid name.
     */
    public Name(String name) {
        requireNonNull(name);
        checkArgument(!hasForbiddenCodePoints(name), MESSAGE_CONSTRAINTS);
        String spacedName = normalizeSpaces(name);
        checkArgument(isWithinNormalizationBudget(spacedName), MESSAGE_CONSTRAINTS);
        String normalizedName = Normalizer.normalize(spacedName, Normalizer.Form.NFC);
        checkArgument(isValidNormalizedName(normalizedName), MESSAGE_CONSTRAINTS);
        fullName = normalizedName;
    }

    /**
     * Returns whether a string satisfies the same raw-input and normalization rules as construction.
     */
    public static boolean isValidName(String test) {
        requireNonNull(test);
        if (hasForbiddenCodePoints(test)) {
            return false;
        }
        String spacedName = normalizeSpaces(test);
        return isWithinNormalizationBudget(spacedName)
                && isValidNormalizedName(Normalizer.normalize(spacedName, Normalizer.Form.NFC));
    }

    private static String normalizeSpaces(String name) {
        return StringUtil.normalizeSpaces(StringUtil.stripUnicodeBoundaryWhitespace(name));
    }

    private static boolean isWithinNormalizationBudget(String name) {
        return name.codePointCount(0, name.length()) <= MAX_NORMALIZATION_CODE_POINTS;
    }

    private static boolean hasForbiddenCodePoints(String name) {
        return name.codePoints().anyMatch(codePoint -> Character.isISOControl(codePoint)
                || Character.getType(codePoint) == Character.FORMAT
                || Character.getType(codePoint) == Character.SURROGATE
                || Character.getType(codePoint) == Character.LINE_SEPARATOR
                || Character.getType(codePoint) == Character.PARAGRAPH_SEPARATOR);
    }

    private static boolean isValidNormalizedName(String name) {
        if (name.codePointCount(0, name.length()) > MAX_LENGTH) {
            return false;
        }
        boolean hasLetterSinceMarker = false;
        boolean canAttachMark = false;
        for (int offset = 0; offset < name.length();) {
            if (isNameMarkerAt(name, offset)) {
                if (!hasLetterSinceMarker) {
                    return false;
                }
                hasLetterSinceMarker = false;
                canAttachMark = false;
                offset += 3;
                continue;
            }
            int codePoint = name.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.isLetter(codePoint)) {
                hasLetterSinceMarker = true;
                canAttachMark = true;
            } else if (isCombiningMark(codePoint)) {
                if (!canAttachMark) {
                    return false;
                }
            } else if (codePoint == ' ' || codePoint == '\'' || codePoint == '’'
                    || codePoint == '-' || codePoint == '.') {
                canAttachMark = false;
            } else {
                return false;
            }
        }
        return hasLetterSinceMarker;
    }

    private static boolean isNameMarkerAt(String name, int offset) {
        return offset > 0 && name.charAt(offset - 1) == ' ' && offset + 3 < name.length()
                && name.charAt(offset + 3) == ' '
                && (name.startsWith("s/o", offset) || name.startsWith("d/o", offset));
    }

    private static boolean isCombiningMark(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK;
    }

    @Override
    public String toString() {
        return fullName;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Name otherName)) {
            return false;
        }

        return fullName.equals(otherName.fullName);
    }

    @Override
    public int hashCode() {
        return fullName.hashCode();
    }

}
