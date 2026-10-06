package seedu.address.model.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.model.search.SearchCategory.LESSON;
import static seedu.address.model.search.SearchCategory.PARENT;
import static seedu.address.model.search.SearchCategory.STUDENT;
import static seedu.address.model.search.SearchCategory.TUTOR;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.Test;

public class SearchFieldTest {
    @Test
    public void getSupportedCategories_tutorNameRules_cannotBeChangedByCaller() {
        Set<SearchCategory> categories = SearchField.TUTOR_NAME.getSupportedCategories();
        assertEquals(Set.of(STUDENT, PARENT, LESSON), categories);
        assertThrows(UnsupportedOperationException.class, () -> categories.add(TUTOR));
        assertThrows(UnsupportedOperationException.class, () -> categories.remove(STUDENT));
        assertFalse(SearchField.TUTOR_NAME.isSupportedBy(TUTOR));
        assertTrue(SearchField.TUTOR_NAME.isSupportedBy(STUDENT));
    }
}
