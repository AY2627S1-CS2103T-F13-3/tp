package ponhub.ui;

import java.util.List;
import java.util.Optional;

/**
 * Cycles command-word matches in the supplied session catalogue's order.
 * The input control must reset the cycle after external text, caret or selection changes.
 */
final class CommandWordCompleter {
    private final List<String> commandWords;
    private List<String> matches = List.of();
    private String leadingText;
    private String arguments;
    private int nextMatch;

    CommandWordCompleter(List<String> commandWords) {
        this.commandWords = List.copyOf(commandWords);
    }

    /**
     * Completes the prefix before the caret, replacing the whole command word and preserving its surroundings.
     * Empty prefixes and carets outside the command word have no completion.
     */
    Optional<Completion> complete(String draft, int caret) {
        if (matches.isEmpty()) {
            int start = 0;
            while (start < draft.length() && Character.isWhitespace(draft.charAt(start))) {
                start++;
            }
            int end = start;
            while (end < draft.length() && !Character.isWhitespace(draft.charAt(end))) {
                end++;
            }
            if (caret <= start || caret > end) {
                return Optional.empty();
            }
            String prefix = draft.substring(start, caret);
            matches = commandWords.stream().filter(word -> word.startsWith(prefix)).toList();
            if (matches.isEmpty()) {
                return Optional.empty();
            }
            leadingText = draft.substring(0, start);
            arguments = draft.substring(end);
            nextMatch = 0;
        }

        String word = matches.get(nextMatch);
        nextMatch = (nextMatch + 1) % matches.size();
        return Optional.of(new Completion(leadingText + word + arguments, leadingText.length() + word.length()));
    }

    void reset() {
        matches = List.of();
    }

    /**
     * A completed draft and the caret immediately after its command word.
     */
    record Completion(String text, int caret) {
    }
}
