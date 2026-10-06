package seedu.address.model.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class SearchCriteriaTest {
    @Test
    public void matches_textFilters_useCaseInsensitivePartialMatching() {
        // category, query, matching value, non-matching value
        Map<SearchField, String[]> cases = Map.of(
                SearchField.NAME, new String[]{"student", "AL", "Sally Tan", "Bob"},
                SearchField.EMAIL, new String[]{"parent", "@Example", "MEI@example.com", "mei@school.com"},
                SearchField.ADDRESS, new String[]{"tutor", "Jurong", "1 JURONG West", "1 Clementi Road"},
                SearchField.STUDENT_NAME, new String[]{"lesson", "alex", "Alexander Tan", "Bob"},
                SearchField.SUBJECT, new String[]{"lesson", "Math", "Add MATH", "Science"},
                SearchField.TUTOR_NAME, new String[]{"lesson", "mei", "MEI Lim", "Bob Lim"},
                SearchField.ROOM, new String[]{"lesson", "R1", "r10", "R2"});
        cases.forEach((field, values) -> {
            SearchCriteria criteria = new SearchCriteria(SearchCategory.fromValue(values[0]), Map.of(field, values[1]));
            assertTrue(criteria.matches(field, values[2]), field.name());
            assertFalse(criteria.matches(field, values[3]), field.name());
            assertFalse(criteria.matches(field, null), field.name());
        });
    }

    @Test
    public void matches_phoneFilters_preserveZerosAndRequireWholeNumbers() {
        for (SearchField field : new SearchField[]{SearchField.PHONE, SearchField.PARENT_PHONE}) {
            SearchCriteria criteria = new SearchCriteria(SearchCategory.STUDENT, Map.of(field, "0012345"));
            assertTrue(criteria.matches(field, "0012345"));
            assertFalse(criteria.matches(field, "12345"));
            assertFalse(criteria.matches(field, "00012345"));
            assertFalse(criteria.matches(field, null));
        }
        SearchCriteria criteria = new SearchCriteria(SearchCategory.STUDENT, Map.of(SearchField.PHONE, "123"));
        assertFalse(criteria.matches(SearchField.PHONE, "1234"));
    }

    @Test
    public void matches_levelAndDay_normalizeCaseButMatchExactly() {
        SearchCriteria criteria = new SearchCriteria(SearchCategory.STUDENT,
                Map.of(SearchField.LEVEL, "jc1", SearchField.DAY, "MON"));
        assertTrue(criteria.matches(SearchField.LEVEL, "JC1"));
        assertTrue(criteria.matches(SearchField.LEVEL, "jc1"));
        assertFalse(criteria.matches(SearchField.LEVEL, "JC2"));
        assertFalse(criteria.matches(SearchField.LEVEL, "JC10"));
        assertTrue(criteria.matches(SearchField.DAY, "Mon"));
        assertFalse(criteria.matches(SearchField.DAY, "Monday"));
        assertFalse(criteria.matches(SearchField.DAY, "Tue"));
    }

    @Test
    public void matches_times_requireExactValidHhmmValues() {
        SearchCriteria criteria = new SearchCriteria(SearchCategory.LESSON,
                Map.of(SearchField.START_TIME, "0900", SearchField.END_TIME, "1000"));
        assertTrue(criteria.matches(SearchField.START_TIME, "0900"));
        assertFalse(criteria.matches(SearchField.START_TIME, "900"));
        assertFalse(criteria.matches(SearchField.START_TIME, "0901"));
        assertTrue(criteria.matches(SearchField.END_TIME, "1000"));
        assertFalse(criteria.matches(SearchField.END_TIME, "1100"));
    }

    @Test
    public void matches_omittedFilter_imposesNoRestriction() {
        SearchCriteria criteria = new SearchCriteria(SearchCategory.STUDENT, Map.of());
        assertTrue(criteria.matches(SearchField.NAME, "Alex"));
        assertTrue(criteria.matches(SearchField.PHONE, null));
        assertTrue(criteria.getValue(SearchField.NAME).isEmpty());
    }

    @Test
    public void constructor_defensiveCopyAndUnmodifiableFilters_preserveCriteria() {
        Map<SearchField, String> input = new EnumMap<>(SearchField.class);
        input.put(SearchField.NAME, "Alex");
        SearchCriteria criteria = new SearchCriteria(SearchCategory.STUDENT, input);
        input.put(SearchField.NAME, "Bob");
        input.put(SearchField.PHONE, "12345");
        assertEquals(Map.of(SearchField.NAME, "alex"), criteria.getFilters());
        assertThrows(UnsupportedOperationException.class, () -> criteria.getFilters().put(SearchField.NAME, "Bob"));
        assertThrows(UnsupportedOperationException.class, () -> criteria.getFilters().clear());
    }

    @Test
    public void equals_normalizedValuesAndCategory_defineIdentity() {
        SearchCriteria criteria = new SearchCriteria(SearchCategory.STUDENT,
                Map.of(SearchField.NAME, "Alex", SearchField.LEVEL, "s2"));
        SearchCriteria equivalent = new SearchCriteria(SearchCategory.STUDENT,
                Map.of(SearchField.LEVEL, "S2", SearchField.NAME, "alex"));
        assertEquals(criteria, equivalent);
        assertEquals(criteria.hashCode(), equivalent.hashCode());
        assertEquals(criteria, criteria);
        assertNotEquals(criteria, new SearchCriteria(SearchCategory.STUDENT, Map.of(SearchField.NAME, "Bob")));
        assertNotEquals(new SearchCriteria(SearchCategory.STUDENT, Map.of(SearchField.NAME, "Alex")),
                new SearchCriteria(SearchCategory.PARENT, Map.of(SearchField.NAME, "Alex")));
        assertNotEquals(criteria, null);
        assertNotEquals(criteria, "alex");
    }

    @Test
    public void constructor_invalidCriteriaCannotBypassParserValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                new SearchCriteria(SearchCategory.TUTOR, Map.of(SearchField.TUTOR_NAME, "Mei")));
        assertThrows(IllegalArgumentException.class, () ->
                new SearchCriteria(SearchCategory.STUDENT, Map.of(SearchField.NAME, "  ")));
        assertThrows(IllegalArgumentException.class, () ->
                new SearchCriteria(SearchCategory.STUDENT, Map.of(SearchField.NAME, "Alex/Tan")));
        assertThrows(IllegalArgumentException.class, () ->
                new SearchCriteria(SearchCategory.STUDENT, Map.of(SearchField.NAME, "Alex\nTan")));
        assertThrows(IllegalArgumentException.class, () ->
                new SearchCriteria(SearchCategory.STUDENT, Map.of(SearchField.NAME, "Alex\rTan")));
        assertThrows(IllegalArgumentException.class, () ->
                new SearchCriteria(SearchCategory.STUDENT, Map.of(SearchField.PHONE, "12")));
        assertThrows(IllegalArgumentException.class, () ->
                new SearchCriteria(SearchCategory.LESSON,
                        Map.of(SearchField.START_TIME, "1000", SearchField.END_TIME, "0900")));
    }

    @Test
    public void nullArguments_areRejected() {
        assertThrows(NullPointerException.class, () -> new SearchCriteria(null, Map.of()));
        assertThrows(NullPointerException.class, () -> new SearchCriteria(SearchCategory.STUDENT, null));
        Map<SearchField, String> nullValue = new EnumMap<>(SearchField.class);
        nullValue.put(SearchField.NAME, null);
        assertThrows(NullPointerException.class, () -> new SearchCriteria(SearchCategory.STUDENT, nullValue));
        SearchCriteria criteria = new SearchCriteria(SearchCategory.STUDENT, Map.of());
        assertThrows(NullPointerException.class, () -> criteria.getValue(null));
        assertThrows(NullPointerException.class, () -> criteria.matches(null, "Alex"));
        assertThrows(NullPointerException.class, () -> SearchCategory.fromValue(null));
        assertThrows(NullPointerException.class, () -> SearchField.fromPrefix(null));
    }

    @Test
    public void matches_textCaseNormalization_isIndependentOfDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            SearchCriteria criteria = new SearchCriteria(SearchCategory.STUDENT, Map.of(SearchField.NAME, "INDIGO"));
            assertEquals("indigo", criteria.getValue(SearchField.NAME).orElseThrow());
            assertTrue(criteria.matches(SearchField.NAME, "Indigo Tan"));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    public void toString_normalizedCriteria_includeCategoryAndFilters() {
        SearchCriteria criteria = new SearchCriteria(SearchCategory.STUDENT,
                Map.of(SearchField.NAME, " Alex ", SearchField.LEVEL, "s2"));
        String expected = SearchCriteria.class.getCanonicalName()
                + "{category=STUDENT, filters={NAME=alex, LEVEL=S2}}";
        assertEquals(expected, criteria.toString());

        SearchCriteria categoryOnly = new SearchCriteria(SearchCategory.LESSON, Map.of());
        assertEquals(SearchCriteria.class.getCanonicalName() + "{category=LESSON, filters={}}",
                categoryOnly.toString());
    }
}
