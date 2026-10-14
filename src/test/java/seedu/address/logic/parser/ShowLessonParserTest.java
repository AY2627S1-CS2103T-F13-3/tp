package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.lesson.LessonId;

public class ShowLessonParserTest {
    private final ShowLessonParser parser = new ShowLessonParser();

    @Test
    public void validIdentityAndOptionalRealDate() throws Exception {
        assertEquals(new ShowLessonInput(new LessonId("L10"), Optional.empty()), parser.parse(" lid/l10 "));
        assertEquals(new ShowLessonInput(new LessonId("L1"), Optional.of(LocalDate.of(2028, 2, 29))),
                parser.parse("lid/L1   d/2028-02-29"));
        assertEquals(new ShowLessonInput(new LessonId("L1"), Optional.of(LocalDate.of(2028, 2, 29))),
                parser.parse("d/2028-02-29 lid/L1"));
    }

    @Test
    public void invalidFieldsAndDates_rejectWithoutProducingInput() {
        for (String arguments : List.of("", "lid/", "d/2026-10-05", "lid/L1 d/", "lid/L1 d/2026-10-05 d/2026-10-12",
                "lid/L1 s/present", "lid/L1 lid/L2", "lid/L0", "lid/L01", "lid/L1 d/2026-02-30",
                "lid/L1 d/2026-10-5", "lid/L1\n", "lid/L1\td/2026-10-05")) {
            assertThrows(ParseException.class, () -> parser.parse(arguments), arguments);
        }
        assertThrows(NullPointerException.class, () -> parser.parse(null));
        assertThrows(NullPointerException.class, () -> new ShowLessonInput(null, Optional.empty()));
        assertThrows(NullPointerException.class, () -> new ShowLessonInput(new LessonId("L1"), null));
    }
}
