package ponhub.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import ponhub.logic.parser.exceptions.ParseException;

/**
 * Checks that bare, legacy and filtered retrieval selectors share the common index rules.
 */
public class UnicodeRetrievalIndexTest {
    @Test
    public void parse_unicodeDecimalIndices_matchAsciiAcrossRetrievalForms() throws Exception {
        LessonsArgumentsParser lessons = new LessonsArgumentsParser();
        HistoryArgumentsParser history = new HistoryArgumentsParser();
        for (String digits : List.of("２", "٢", "०२", "0２", "２١４٧４８３٦４٧")) {
            String ascii = Integer.toString(ParserUtil.parseIndex(digits).getOneBased());
            for (String selector : List.of("", "si/")) {
                for (String order : List.of("", " sort/schedule", " sort/creation")) {
                    assertEquals(lessons.parseInput(selector + ascii + order),
                            lessons.parseInput(selector + digits + order));
                }
            }
            for (String filters : List.of("", " lid/L1", " s/present d/2026-10-05")) {
                assertEquals(history.parse(ascii + filters), history.parse(digits + filters));
            }
        }
    }

    @Test
    public void parse_invalidUnicodeIndices_retainRangeAndSyntaxRejections() {
        LessonsArgumentsParser lessons = new LessonsArgumentsParser();
        HistoryArgumentsParser history = new HistoryArgumentsParser();
        for (String digits : List.of("０", "٠٠", "２１４７４８３６４８", "²", "-２", "+２", "𝟚")) {
            assertThrows(ParseException.class, () -> lessons.parseInput(digits));
            assertThrows(ParseException.class, () -> lessons.parseInput("si/" + digits + " sort/schedule"));
            assertThrows(ParseException.class, () -> history.parse(digits));
            assertThrows(ParseException.class, () -> history.parse(digits + " lid/L1"));
        }
        assertThrows(ParseException.class, () -> history.parse("２ lid/L１"));
        assertThrows(ParseException.class, () -> lessons.parseInput("sort/schedule ２"));
    }
}
