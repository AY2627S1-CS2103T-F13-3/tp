package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.parser.exceptions.ParseException;

public class LessonsArgumentsParserTest {
    private final LessonsArgumentsParser parser = new LessonsArgumentsParser();

    @Test
    public void parse_validCatalogueAndStudentSelector() throws ParseException {
        assertEquals(Optional.empty(), parser.parse(""));
        assertEquals(Optional.empty(), parser.parse("  \t "));
        assertEquals(Optional.of(Index.fromOneBased(1)), parser.parse("si/1"));
        assertEquals(Optional.of(Index.fromOneBased(12)), parser.parse("  si/12  "));
        assertEquals(Optional.of(Index.fromOneBased(1)), parser.parse("si/01"));
    }

    @Test
    public void parse_invalidSelectorsGiveFormatOrIndexFeedback() {
        for (String argument : List.of("si/", "si/S1", "si/L1", "si/-1", "si/ 1", "si/1 si/2",
                "sid/S1", "r/student", "1", "si/1 extra", "SI/1", "si/1\n", "si/1\r")) {
            assertThrows(ParseException.class, () -> parser.parse(argument), argument);
        }
        for (String argument : List.of("si/0", "si/2147483648")) {
            assertThrows(ParseException.class, () -> parser.parse(argument), argument);
        }
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
