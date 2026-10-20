package seedu.address.model.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.PersonRole;

/**
 * Tests category parsing and its explicit mapping to people roles.
 */
public class SearchCategoryTest {
    @Test
    public void getPersonRole_allCategories_returnExplicitRoleOrNoRoleForLessons() {
        Map<SearchCategory, Optional<PersonRole>> roles = Map.of(
                SearchCategory.STUDENT, Optional.of(PersonRole.STUDENT),
                SearchCategory.PARENT, Optional.of(PersonRole.PARENT),
                SearchCategory.TUTOR, Optional.of(PersonRole.TUTOR),
                SearchCategory.LESSON, Optional.empty());

        assertEquals(EnumSet.allOf(SearchCategory.class), roles.keySet());
        roles.forEach((category, role) -> assertEquals(role, category.getPersonRole(), category.name()));
    }

    @Test
    public void fromValue_supportedKeywords_ignoreSurroundingWhitespaceAndCase() {
        Map<String, SearchCategory> categories = Map.of(
                "student", SearchCategory.STUDENT,
                "parent", SearchCategory.PARENT,
                "tutor", SearchCategory.TUTOR,
                "lesson", SearchCategory.LESSON);
        categories.forEach((value, category) -> {
            assertEquals(category, SearchCategory.fromValue(value));
            assertEquals(category, SearchCategory.fromValue("\u2003" + value.toUpperCase(Locale.ROOT) + "\t"));
        });
    }

    @Test
    public void fromValue_unsupportedKeyword_preservesOriginalValueInError() {
        for (String value : new String[]{"", " ", "students", " teacher ", "c/student"}) {
            String error = "Unsupported search category '" + value + "'. Use student, parent, tutor or lesson.";
            assertThrows(IllegalArgumentException.class, error, () -> SearchCategory.fromValue(value));
        }
    }

    @Test
    public void fromValue_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> SearchCategory.fromValue(null));
    }
}
