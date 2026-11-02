package ponhub.model.person;

import ponhub.commons.util.StringUtil;

/**
 * Shared raw-character and spacing policy for names and addresses.
 */
final class ContactText {
    private ContactText() {
    }

    /**
     * Trims Unicode boundary whitespace and collapses interior ASCII spaces.
     * Callers must reject forbidden raw code points before normalizing.
     */
    static String normalizeSpaces(String value) {
        return StringUtil.normalizeSpaces(StringUtil.stripUnicodeBoundaryWhitespace(value));
    }

    /**
     * Detects controls, format characters, lone surrogates, and line or paragraph separators before trimming.
     */
    static boolean hasForbiddenCodePoints(String value) {
        return value.codePoints().anyMatch(codePoint -> Character.isISOControl(codePoint)
                || Character.getType(codePoint) == Character.FORMAT
                || Character.getType(codePoint) == Character.SURROGATE
                || Character.getType(codePoint) == Character.LINE_SEPARATOR
                || Character.getType(codePoint) == Character.PARAGRAPH_SEPARATOR);
    }
}
