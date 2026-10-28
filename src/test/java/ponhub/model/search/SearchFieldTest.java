package ponhub.model.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.model.search.SearchCategory.LESSON;
import static ponhub.model.search.SearchCategory.PARENT;
import static ponhub.model.search.SearchCategory.STUDENT;
import static ponhub.model.search.SearchCategory.TUTOR;
import static ponhub.testutil.Assert.assertThrows;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ponhub.model.lesson.LessonDay;
import ponhub.model.lesson.LessonTime;
import ponhub.model.person.EducationLevel;
import ponhub.model.person.Phone;

/**
 * Tests search-field compatibility and the query values shared with domain records.
 */
public class SearchFieldTest {
    @Test
    public void fromPrefix_supportedPrefixes_returnCorrespondingFields() {
        Map<String, SearchField> fields = Map.ofEntries(
                Map.entry("n/", SearchField.NAME),
                Map.entry("l/", SearchField.LEVEL),
                Map.entry("p/", SearchField.PHONE),
                Map.entry("pp/", SearchField.PARENT_PHONE),
                Map.entry("e/", SearchField.EMAIL),
                Map.entry("a/", SearchField.ADDRESS),
                Map.entry("sn/", SearchField.STUDENT_NAME),
                Map.entry("d/", SearchField.DAY),
                Map.entry("st/", SearchField.START_TIME),
                Map.entry("et/", SearchField.END_TIME),
                Map.entry("from/", SearchField.FROM_TIME),
                Map.entry("to/", SearchField.TO_TIME),
                Map.entry("s/", SearchField.SUBJECT),
                Map.entry("tu/", SearchField.TUTOR_NAME),
                Map.entry("tp/", SearchField.TUTOR_PHONE),
                Map.entry("rm/", SearchField.ROOM));

        assertEquals(EnumSet.allOf(SearchField.class), Set.copyOf(fields.values()));
        fields.forEach((prefix, field) -> {
            assertEquals(field, SearchField.fromPrefix(prefix));
            assertEquals(prefix, field.getPrefix());
        });
    }

    @Test
    public void fromPrefix_unsupportedPrefix_preservesError() {
        for (String prefix : List.of("", "n", "N/", " n/", "x/")) {
            String error = "Unsupported search prefix '" + prefix + "'.";
            assertThrows(IllegalArgumentException.class, error, () -> SearchField.fromPrefix(prefix));
        }
        assertThrows(NullPointerException.class, () -> SearchField.fromPrefix(null));
    }

    @Test
    public void isSupportedBy_allFields_preserveCategoryMatrix() {
        Set<SearchCategory> people = Set.of(STUDENT, PARENT, TUTOR);
        Set<SearchCategory> all = Set.of(STUDENT, PARENT, TUTOR, LESSON);
        Map<SearchField, Set<SearchCategory>> categories = Map.ofEntries(
                Map.entry(SearchField.NAME, people),
                Map.entry(SearchField.LEVEL, Set.of(STUDENT)),
                Map.entry(SearchField.PHONE, people),
                Map.entry(SearchField.PARENT_PHONE, Set.of(STUDENT)),
                Map.entry(SearchField.EMAIL, people),
                Map.entry(SearchField.ADDRESS, people),
                Map.entry(SearchField.STUDENT_NAME, Set.of(PARENT, TUTOR, LESSON)),
                Map.entry(SearchField.DAY, all),
                Map.entry(SearchField.START_TIME, all),
                Map.entry(SearchField.END_TIME, all),
                Map.entry(SearchField.FROM_TIME, Set.of(LESSON)),
                Map.entry(SearchField.TO_TIME, Set.of(LESSON)),
                Map.entry(SearchField.SUBJECT, all),
                Map.entry(SearchField.TUTOR_NAME, Set.of(STUDENT, PARENT, LESSON)),
                Map.entry(SearchField.TUTOR_PHONE, Set.of(LESSON)),
                Map.entry(SearchField.ROOM, all));

        assertEquals(EnumSet.allOf(SearchField.class), categories.keySet());
        categories.forEach((field, supported) -> {
            assertEquals(supported, field.getSupportedCategories(), field.name());
            for (SearchCategory category : SearchCategory.values()) {
                assertEquals(supported.contains(category), field.isSupportedBy(category),
                        field + " in " + category);
            }
            assertThrows(NullPointerException.class, () -> field.isSupportedBy(null));
        });
    }

    @Test
    public void getSupportedCategories_tutorNameRules_cannotBeChangedByCaller() {
        Set<SearchCategory> categories = SearchField.TUTOR_NAME.getSupportedCategories();
        assertEquals(Set.of(STUDENT, PARENT, LESSON), categories);
        assertThrows(UnsupportedOperationException.class, () -> categories.add(TUTOR));
        assertThrows(UnsupportedOperationException.class, () -> categories.remove(STUDENT));
        assertFalse(SearchField.TUTOR_NAME.isSupportedBy(TUTOR));
        assertTrue(SearchField.TUTOR_NAME.isSupportedBy(STUDENT));
    }

    @Test
    public void normalizeValue_validPhones_preserveDomainDigitsAndLeadingZeros() {
        for (SearchField field : List.of(SearchField.PHONE, SearchField.PARENT_PHONE, SearchField.TUTOR_PHONE)) {
            for (String phone : List.of("000", "123", "0012345", "123456789012345", "000000000000000")) {
                assertEquals(phone, field.normalizeValue("\u2003" + phone + "\t"));
                assertEquals(new Phone(phone).toString(), field.normalizeValue(phone));
            }
        }
    }

    @Test
    public void normalizeValue_invalidPhones_preserveBothPrefixErrors() {
        for (SearchField field : List.of(SearchField.PHONE, SearchField.PARENT_PHONE, SearchField.TUTOR_PHONE)) {
            String error = "Search filter '" + field.getPrefix() + "' requires 3-15 digits.";
            for (String phone : List.of("12", "1234567890123456", "+123", "12 3", "123-4", "12a", "１２３")) {
                assertFalse(Phone.isValidPhone(phone), phone);
                assertThrows(IllegalArgumentException.class, error, () -> field.normalizeValue(phone));
            }
        }
    }

    @Test
    public void normalizeValue_allEducationLevels_matchDomainNormalization() {
        List<String> levels = List.of("P1", "P2", "P3", "P4", "P5", "P6",
                "S1", "S2", "S3", "S4", "S5", "JC1", "JC2");
        for (String level : levels) {
            String input = level.toLowerCase(Locale.ROOT);
            assertEquals(level, SearchField.LEVEL.normalizeValue("\u2003" + input + "\t"));
            assertEquals(new EducationLevel(input).toString(), SearchField.LEVEL.normalizeValue(input));
            assertEquals(level, SearchField.LEVEL.normalizeValue(level));
        }
        assertEquals("S1", SearchField.LEVEL.normalizeValue("ſ1"));
    }

    @Test
    public void normalizeValue_invalidEducationLevels_preservesError() {
        String error = "Search filter 'l/' requires P1-P6, S1-S5, JC1 or JC2.";
        for (String level : List.of("P0", "P7", "S0", "S6", "JC0", "JC3", "P01", "P", "Primary 1")) {
            assertFalse(EducationLevel.isValid(level), level);
            assertThrows(IllegalArgumentException.class, error, () -> SearchField.LEVEL.normalizeValue(level));
        }
    }

    @Test
    public void normalizeValue_levelWithBoundaryControls_preservesSearchRejectionDespiteDomainTrimming() {
        String error = "Search filter 'l/' requires P1-P6, S1-S5, JC1 or JC2.";
        for (String level : List.of("\u0000s2\u0000", "\u001bp1", "jc2\b")) {
            assertTrue(EducationLevel.isValid(level));
            assertThrows(IllegalArgumentException.class, error, () -> SearchField.LEVEL.normalizeValue(level));
        }
    }

    @Test
    public void normalizeValue_exactFieldBoundaries_preservesWhitespaceAndControlRules() {
        Map<SearchField, String[]> cases = Map.of(
                SearchField.PHONE, new String[]{"00123", "Search filter 'p/' requires 3-15 digits."},
                SearchField.PARENT_PHONE, new String[]{"00123", "Search filter 'pp/' requires 3-15 digits."},
                SearchField.LEVEL, new String[]{"S2", "Search filter 'l/' requires P1-P6, S1-S5, JC1 or JC2."},
                SearchField.DAY, new String[]{"mon",
                    "Search filter 'd/' requires Mon, Tue, Wed, Thu, Fri, Sat or Sun."},
                SearchField.START_TIME, new String[]{"0000", "Search filter 'st/' requires a valid four-digit HHMM."},
                SearchField.END_TIME, new String[]{"2359", "Search filter 'et/' requires a valid four-digit HHMM."});
        cases.forEach((field, values) -> {
            for (String boundary : List.of("\t", "\u001f", "\u2003", "\u3000")) {
                assertEquals(values[0], field.normalizeValue(boundary + values[0] + boundary), field.name());
            }
            for (String boundary : List.of("\u0000", "\b", "\u001b", "\u00a0", "\u2007", "\u202f")) {
                assertThrows(IllegalArgumentException.class, values[1], () ->
                        field.normalizeValue(boundary + values[0]));
                assertThrows(IllegalArgumentException.class, values[1], () ->
                        field.normalizeValue(values[0] + boundary));
            }
        });
    }

    @Test
    public void normalizeValue_allWeekdays_matchCanonicalDomainSpellings() {
        Map<String, String> days = Map.of("Mon", "mon", "Tue", "tue", "Wed", "wed", "Thu", "thu",
                "Fri", "fri", "Sat", "sat", "Sun", "sun");
        assertEquals(LessonDay.values().length, days.size());
        days.forEach((day, normalized) -> {
            assertEquals(normalized, SearchField.DAY.normalizeValue(day));
            assertEquals(normalized, SearchField.DAY.normalizeValue(day.toUpperCase(Locale.ROOT)));
            assertEquals(normalized, SearchField.DAY.normalizeValue("\u2003" + normalized + "\t"));
            assertEquals(LessonDay.fromString(day).toString().toLowerCase(Locale.ROOT),
                    SearchField.DAY.normalizeValue(day));
        });
    }

    @Test
    public void normalizeValue_invalidWeekdays_preservesAcceptedSpellingsAndError() {
        String error = "Search filter 'd/' requires Mon, Tue, Wed, Thu, Fri, Sat or Sun.";
        for (String day : List.of("M", "Monday", "Tues", "Thur", "Funday", "ſun", "frı", "frİ")) {
            assertThrows(IllegalArgumentException.class, error, () -> SearchField.DAY.normalizeValue(day));
        }
    }

    @Test
    public void normalizeValue_everyValidTime_matchesDomainAndRetainsFourDigits() {
        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute++) {
                String time = String.format(Locale.ROOT, "%02d%02d", hour, minute);
                String domainTime = new LessonTime(time).toString();
                for (SearchField field : List.of(SearchField.START_TIME, SearchField.END_TIME)) {
                    assertEquals(time, field.normalizeValue(time), field + " " + time);
                    assertEquals(domainTime, field.normalizeValue("\u2003" + time + "\t"));
                }
            }
        }
    }

    @Test
    public void normalizeValue_invalidTimes_preserveBothPrefixErrors() {
        for (SearchField field : List.of(SearchField.START_TIME, SearchField.END_TIME)) {
            String error = "Search filter '" + field.getPrefix() + "' requires a valid four-digit HHMM.";
            for (String time : List.of("000", "00000", "0060", "1260", "2360", "2400", "9959",
                    "-100", "090a", "09 0", "０９００")) {
                assertFalse(LessonTime.isValidLessonTime(time), time);
                assertThrows(IllegalArgumentException.class, error, () -> field.normalizeValue(time));
            }
        }
    }

    @Test
    public void normalizeValue_textFields_remainPermissiveFragments() {
        Map<SearchField, String[]> fragments = Map.of(
                SearchField.NAME, new String[]{"  AL  ", "al", "Sally Tan"},
                SearchField.EMAIL, new String[]{"  @EXAMPLE  ", "@example", "mei@example.com"},
                SearchField.ADDRESS, new String[]{"  Jurong   WEST  ", "jurong west", "1 Jurong West Road"},
                SearchField.STUDENT_NAME, new String[]{"  lex  ", "lex", "Alexander Tan"},
                SearchField.SUBJECT, new String[]{"  Ma  ", "ma", "Add Math"},
                SearchField.TUTOR_NAME, new String[]{"  ei  ", "ei", "Mei Lim"},
                SearchField.ROOM, new String[]{"  R  ", "r", "R1"});
        fragments.forEach((field, values) -> {
            assertEquals(values[1], field.normalizeValue(values[0]), field.name());
            assertTrue(field.matchesValue(values[1], values[2]), field.name());
            assertFalse(field.matchesValue(values[1], "ZZZ"), field.name());
            assertFalse(field.matchesValue(values[1], null), field.name());
            assertEquals("@", field.normalizeValue("@"), field.name());
            assertEquals("中", field.normalizeValue("中"), field.name());
        });
    }

    @Test
    public void normalizeValue_blankOrDelimitedValues_preserveAllPrefixErrors() {
        for (SearchField field : SearchField.values()) {
            String error = "Search filter '" + field.getPrefix()
                    + "' requires a non-blank value without slashes or line breaks.";
            for (String value : List.of("", " \t ", "a/b", "a\nb", "a\rb")) {
                assertThrows(IllegalArgumentException.class, error, () -> field.normalizeValue(value));
            }
            assertThrows(NullPointerException.class, () -> field.normalizeValue(null));
        }
    }

    @Test
    public void matchesValue_exactFields_normalizeCandidatesAndRejectInvalidValues() {
        Map<SearchField, String[]> cases = Map.of(
                SearchField.PHONE, new String[]{"00123", " 00123 ", "123", "12"},
                SearchField.PARENT_PHONE, new String[]{"00123", " 00123 ", "000123", "+00123"},
                SearchField.LEVEL, new String[]{"JC1", " jc1 ", "JC2", "JC10"},
                SearchField.DAY, new String[]{"mon", " MON ", "Tue", "Monday"},
                SearchField.START_TIME, new String[]{"0000", " 0000 ", "0001", "000"},
                SearchField.END_TIME, new String[]{"2359", " 2359 ", "2358", "2400"});
        cases.forEach((field, values) -> {
            assertTrue(field.matchesValue(values[0], values[1]), field.name());
            assertFalse(field.matchesValue(values[0], values[2]), field.name());
            assertFalse(field.matchesValue(values[0], values[3]), field.name());
            assertFalse(field.matchesValue(values[0], null), field.name());
        });
    }
}
