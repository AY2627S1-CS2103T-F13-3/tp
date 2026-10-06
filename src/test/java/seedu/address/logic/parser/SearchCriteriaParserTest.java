package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.search.SearchCategory;
import seedu.address.model.search.SearchCriteria;
import seedu.address.model.search.SearchField;

public class SearchCriteriaParserTest {
    private final SearchCriteriaParser parser = new SearchCriteriaParser();

    @Test
    public void parse_categoryOnly_acceptsEveryCategoryIgnoringCase() throws Exception {
        for (SearchCategory category : SearchCategory.values()) {
            SearchCriteria criteria = parser.parse("c/" + category.name());
            assertEquals(category, criteria.getCategory());
            assertTrue(criteria.getFilters().isEmpty());
        }
        assertEquals(SearchCategory.STUDENT, parser.parse(" c/StUdEnT ").getCategory());
    }

    @Test
    public void parse_filterMatrix_acceptsOnlyDocumentedCategoryCombinations() throws Exception {
        // This fixture is taken from the UG rather than generated from SearchField's category sets.
        Map<SearchCategory, Set<String>> allowed = Map.of(
                SearchCategory.STUDENT, Set.of("n/", "l/", "p/", "pp/", "e/", "a/", "d/", "st/", "et/", "s/",
                        "tu/", "rm/"),
                SearchCategory.PARENT, Set.of("n/", "p/", "e/", "a/", "sn/", "d/", "st/", "et/", "s/", "tu/", "rm/"),
                SearchCategory.TUTOR, Set.of("n/", "p/", "e/", "a/", "sn/", "d/", "st/", "et/", "s/", "rm/"),
                SearchCategory.LESSON, Set.of("sn/", "d/", "st/", "et/", "s/", "tu/", "rm/"));
        Map<String, String> values = Map.ofEntries(
                Map.entry("n/", "Alex"), Map.entry("l/", "S2"), Map.entry("p/", "91234567"),
                Map.entry("pp/", "0012345"), Map.entry("e/", "@example.com"), Map.entry("a/", "Jurong"),
                Map.entry("sn/", "Alex"), Map.entry("d/", "Mon"), Map.entry("st/", "0900"),
                Map.entry("et/", "1000"), Map.entry("s/", "Math"), Map.entry("tu/", "Mei"), Map.entry("rm/", "R1"));
        for (SearchCategory category : SearchCategory.values()) {
            for (Map.Entry<String, String> filter : values.entrySet()) {
                String input = "c/" + category.name() + " " + filter.getKey() + filter.getValue();
                if (allowed.get(category).contains(filter.getKey())) {
                    SearchCriteria criteria = parser.parse(input);
                    assertEquals(category, criteria.getCategory(), input);
                    assertEquals(1, criteria.getFilters().size(), input);
                    assertTrue(criteria.getValue(SearchField.fromPrefix(filter.getKey())).isPresent(), input);
                } else {
                    assertThrows(ParseException.class, () -> parser.parse(input));
                }
            }
        }
    }

    @Test
    public void parse_documentedExamples_acceptsRelationshipCriteria() throws Exception {
        String[] examples = {
            "c/student n/Alex l/S2", "c/student pp/91234567", "c/parent n/Tan",
            "c/parent s/Math d/Mon", "c/tutor sn/Alex s/Science", "c/lesson sn/Alex tu/Mei",
            "c/lesson d/Mon st/1600 rm/R1"
        };
        for (String input : examples) {
            assertTrue(parser.parse(input).getFilters().size() > 0, input);
        }
    }

    @Test
    public void parse_completeStudentFilters_normalizesValuesAndPreservesPhoneZeros() throws Exception {
        SearchCriteria criteria = parser.parse("c/STUDENT n/ALex l/jc2 p/0012345 pp/000 e/@Example.COM a/Jurong "
                + "d/MON st/0000 et/2359 s/Add Math tu/Mei Lim rm/R1");
        assertEquals(Map.ofEntries(
                Map.entry(SearchField.NAME, "alex"), Map.entry(SearchField.LEVEL, "JC2"),
                Map.entry(SearchField.PHONE, "0012345"), Map.entry(SearchField.PARENT_PHONE, "000"),
                Map.entry(SearchField.EMAIL, "@example.com"), Map.entry(SearchField.ADDRESS, "jurong"),
                Map.entry(SearchField.DAY, "mon"), Map.entry(SearchField.START_TIME, "0000"),
                Map.entry(SearchField.END_TIME, "2359"), Map.entry(SearchField.SUBJECT, "add math"),
                Map.entry(SearchField.TUTOR_NAME, "mei lim"), Map.entry(SearchField.ROOM, "r1")),
                criteria.getFilters());
    }

    @Test
    public void parse_prefixOrderAndHorizontalWhitespace_doesNotChangeCriteria() throws Exception {
        assertEquals(parser.parse("c/student n/Alex Tan p/0012345"),
                parser.parse("  n/Alex Tan\tp/0012345\t c/StUdEnT  "));
    }

    @Test
    public void parse_missingCategoryOrUnexpectedPreamble_throwsParseException() {
        String[] inputs = {"", "   ", "\t", "n/Alex", "student", "search c/student", "Alex c/student"};
        for (String input : inputs) {
            assertThrows(ParseException.class, () -> parser.parse(input));
        }
    }

    @Test
    public void parse_invalidCategory_throwsParseException() {
        for (String category : new String[]{"people", "students", "all", "student extra", "1"}) {
            assertThrows(ParseException.class, () -> parser.parse("c/" + category));
        }
    }

    @Test
    public void parse_blankValues_throwsParseException() {
        for (SearchField field : SearchField.values()) {
            assertThrows(ParseException.class, () -> parser.parse("c/student " + field.getPrefix() + "  "));
            assertThrows(ParseException.class, () -> parser.parse(field.getPrefix() + "  c/student"));
        }
        assertThrows(ParseException.class, () -> parser.parse("c/"));
        assertThrows(ParseException.class, () -> parser.parse("c/  n/Alex"));
    }

    @Test
    public void parse_repeatedPrefixes_throwsParseExceptionEvenWhenValuesAgree() {
        assertThrows(ParseException.class, "Repeated search prefix 'c/'.", () ->
                parser.parse("c/student c/student"));
        for (SearchField field : SearchField.values()) {
            assertThrows(ParseException.class, "Repeated search prefix '" + field.getPrefix() + "'.", () ->
                    parser.parse("c/student " + field.getPrefix() + "value " + field.getPrefix() + "value"));
        }
    }

    @Test
    public void parse_unknownPrefixAfterText_throwsInsteadOfAbsorbingItIntoValue() {
        assertThrows(ParseException.class, "Unsupported search prefix 'xyz/'.", () ->
                parser.parse("c/student n/Alex xyz/no p/91234567"));
        String[] prefixes = {"C/", "N/", "t/", "tp/", "lid/", "r/", "foo_bar/", "1/"};
        for (String prefix : prefixes) {
            assertThrows(ParseException.class, () -> parser.parse("c/student n/Alex " + prefix + "value"));
        }
    }

    @Test
    public void parse_textFragments_acceptsPartialEmailAndTutorName() throws Exception {
        assertEquals("@example", parser.parse("c/parent e/@Example").getValue(SearchField.EMAIL).orElseThrow());
        assertEquals("mei", parser.parse("c/lesson tu/Mei").getValue(SearchField.TUTOR_NAME).orElseThrow());
        assertEquals("o'", parser.parse("c/student n/O'").getValue(SearchField.NAME).orElseThrow());
        assertThrows(ParseException.class, () -> parser.parse("c/tutor tu/Mei"));
        assertTrue(parser.parse("c/tutor n/Mei").getValue(SearchField.NAME).isPresent());
    }

    @Test
    public void parse_phoneBoundaries_acceptsCompleteNumbers() throws Exception {
        for (String phone : new String[]{"000", "012345678901234"}) {
            SearchCriteria criteria = parser.parse("c/student p/" + phone + " pp/" + phone);
            assertEquals(phone, criteria.getValue(SearchField.PHONE).orElseThrow());
            assertEquals(phone, criteria.getValue(SearchField.PARENT_PHONE).orElseThrow());
        }
    }

    @Test
    public void parse_invalidPhone_throwsParseException() {
        String[] invalid = {"12", "1234567890123456", "+123", "123 456", "12a", "１２３"};
        for (String phone : invalid) {
            assertThrows(ParseException.class, () -> parser.parse("c/student p/" + phone));
            assertThrows(ParseException.class, () -> parser.parse("c/student pp/" + phone));
        }
    }

    @Test
    public void parse_levels_acceptsAllDocumentedValues() throws Exception {
        String[] levels = {"p1", "P2", "P3", "P4", "P5", "P6", "s1", "S2", "S3", "S4", "S5", "jc1", "JC2"};
        for (String level : levels) {
            assertEquals(level.toUpperCase(Locale.ROOT),
                    parser.parse("c/student l/" + level).getValue(SearchField.LEVEL).orElseThrow());
        }
    }

    @Test
    public void parse_invalidLevel_throwsParseException() {
        for (String level : new String[]{"P0", "P7", "S0", "S6", "JC0", "JC3", "P01", "JC01", "Primary 1"}) {
            assertThrows(ParseException.class, () -> parser.parse("c/student l/" + level));
        }
    }

    @Test
    public void parse_days_acceptsOnlyDocumentedAbbreviations() throws Exception {
        for (String day : new String[]{"MON", "Tue", "wed", "Thu", "FRI", "Sat", "sun"}) {
            assertEquals(day.toLowerCase(Locale.ROOT),
                    parser.parse("c/lesson d/" + day).getValue(SearchField.DAY).orElseThrow());
        }
        for (String day : new String[]{"Monday", "Mo", "M0n", "1", "mon tue"}) {
            assertThrows(ParseException.class, () -> parser.parse("c/lesson d/" + day));
        }
    }

    @Test
    public void parse_timeBoundariesAndSingleBounds_acceptsValidTimes() throws Exception {
        assertEquals("0000", parser.parse("c/lesson et/0000").getValue(SearchField.END_TIME).orElseThrow());
        assertEquals("2359", parser.parse("c/lesson st/2359").getValue(SearchField.START_TIME).orElseThrow());
        assertEquals(2, parser.parse("c/lesson st/0000 et/2359").getFilters().size());
        assertEquals(2, parser.parse("c/lesson et/1200 st/1159").getFilters().size());
    }

    @Test
    public void parse_invalidTimes_throwsParseException() {
        String[] invalid = {"900", "2400", "1260", "12:30", "0099", "2360", "abcd", "+0900", "-100"};
        for (String time : invalid) {
            assertThrows(ParseException.class, () -> parser.parse("c/lesson st/" + time));
            assertThrows(ParseException.class, () -> parser.parse("c/lesson et/" + time));
        }
    }

    @Test
    public void parse_nonIncreasingOrOvernightTimeRange_throwsParseException() {
        for (String input : new String[]{"st/0900 et/0900", "st/1000 et/0900", "et/0100 st/2300"}) {
            assertThrows(ParseException.class, "Search end time must be later than start time on the same day.", () ->
                    parser.parse("c/lesson " + input));
        }
    }

    @Test
    public void parse_slashesOrLineBreaks_throwsParseException() {
        String[] inputs = {"c/student n/Alex/Tan", "c/student e/user/example", "c/lesson s/Math/Science",
            "c/student n/Alex\nTan", "c/student\n n/Alex", "c/student n/Alex\rTan", "c/student\r\n"};
        for (String input : inputs) {
            assertThrows(ParseException.class, () -> parser.parse(input));
        }
    }

    @Test
    public void parse_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }

    @Test
    public void parseCommand_searchRoute_remainsInactive() {
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () ->
                new AddressBookParser().parseCommand("search c/student n/Alex"));
    }
}
