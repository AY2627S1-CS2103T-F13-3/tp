package ponhub.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ponhub.commons.core.index.Index;
import ponhub.logic.parser.exceptions.ParseException;

public class LessonsArgumentsParserTest {
    private final LessonsArgumentsParser parser = new LessonsArgumentsParser();

    @Test
    public void parse_validCatalogueAndStudentSelector() throws ParseException {
        assertEquals(Optional.empty(), parser.parse(""));
        assertEquals(Optional.empty(), parser.parse("  \t "));
        assertEquals(Optional.of(Index.fromOneBased(1)), parser.parse("1"));
        assertEquals(Optional.of(Index.fromOneBased(12)), parser.parse("  12  "));
        assertEquals(Optional.of(Index.fromOneBased(1)), parser.parse("01"));
        assertEquals(Optional.of(Index.fromOneBased(1)), parser.parse("si/1"));
        assertEquals(Optional.of(Index.fromOneBased(12)), parser.parse("  si/12  "));
        assertEquals(Optional.of(Index.fromOneBased(1)), parser.parse("si/01"));
    }

    @Test
    public void parse_invalidSelectorsGiveFormatOrIndexFeedback() {
        for (String argument : List.of("si/", "si/S1", "si/L1", "si/-1", "si/ 1", "si/1 si/2",
                "sid/S1", "r/student", "+1", "1 2", "si/1 extra", "SI/1", "si/1\n", "si/1\r", "si/1\u2028",
                "si/1\u0000", "\u0000si/1", "\u0000")) {
            assertThrows(ParseException.class, () -> parser.parse(argument), argument);
        }
        for (String argument : List.of("0", "2147483648", "si/0", "si/2147483648")) {
            assertThrows(ParseException.class, () -> parser.parse(argument), argument);
        }
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
