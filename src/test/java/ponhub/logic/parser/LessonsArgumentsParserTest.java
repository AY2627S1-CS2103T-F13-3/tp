package ponhub.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ponhub.commons.core.index.Index;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.query.LessonOrder;

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
    public void parseInput_acceptsOptionalOrderAndRejectsMalformedOrRepeatedOptions() throws Exception {
        assertEquals(new LessonsInput(Optional.empty(), LessonOrder.CREATION), parser.parseInput(""));
        assertEquals(new LessonsInput(Optional.empty(), LessonOrder.SCHEDULE), parser.parseInput("sort/SCHEDULE"));
        assertEquals(new LessonsInput(Optional.of(Index.fromOneBased(2)), LessonOrder.SCHEDULE),
                parser.parseInput("si/2 sort/schedule"));
        assertEquals(new LessonsInput(Optional.of(Index.fromOneBased(2)), LessonOrder.CREATION),
                parser.parseInput("2 sort/creation"));
        for (String input : List.of("sort/", "sort/name", "sort/schedule sort/creation", "1sort/schedule",
                "sort/schedule 1", "1 SORT/schedule", "1 sort/schedule\n", "1 sort/schedule extra")) {
            assertThrows(ParseException.class, () -> parser.parseInput(input), input);
        }
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
