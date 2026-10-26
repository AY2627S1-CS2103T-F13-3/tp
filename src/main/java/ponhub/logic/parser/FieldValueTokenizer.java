package ponhub.logic.parser;

import java.util.regex.Matcher;

/**
 * Retains bounded slash literals while finding every other prefix-shaped token in a field value.
 * Domain validation still checks complete names and addresses after extraction.
 */
final class FieldValueTokenizer {
    private FieldValueTokenizer() {
    }

    /**
     * Advances the supplied prefix matcher, skipping legal-name marker tokens only in name fields and
     * digit-surrounded slashes only in address fields. Unknown and repeated prefixes remain visible.
     */
    static boolean findNextPrefix(Matcher matcher, String arguments, String fieldPrefix) {
        while (matcher.find()) {
            if ((fieldPrefix.equals("n/") || fieldPrefix.equals("tu/")) && isNameMarker(matcher, arguments)) {
                continue;
            }
            int slash = matcher.end() - 1;
            if (fieldPrefix.equals("a/") && slash + 1 < arguments.length()
                    && isAsciiDigit(arguments.charAt(slash - 1)) && isAsciiDigit(arguments.charAt(slash + 1))) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static boolean isNameMarker(Matcher matcher, String arguments) {
        int end = matcher.end();
        return (matcher.group(1).equals("s/") || matcher.group(1).equals("d/"))
                && end < arguments.length() && arguments.charAt(end) == 'o'
                && (end + 1 == arguments.length() || Character.isWhitespace(arguments.codePointAt(end + 1))
                || Character.isSpaceChar(arguments.codePointAt(end + 1)));
    }

    private static boolean isAsciiDigit(char character) {
        return character >= '0' && character <= '9';
    }
}
