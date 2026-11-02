package ponhub.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ponhub.logic.commands.exceptions.CommandException;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.person.PersonRole;
import ponhub.model.search.SearchCategory;
import ponhub.model.search.SearchCriteria;

public class CanonicalSearchCommandTest {
    @Test
    public void execute_lessonCategory_rejectsWithoutChangingPeopleViewOrData() {
        PonHubData data = new PonHubData();
        PeopleView view = new PeopleView(data);
        view.setRoleFilter(Optional.of(PersonRole.PARENT));
        var before = data.exportState();
        // Direct construction must retain the people-only boundary even though the session routes lessons separately.
        var command = new CanonicalSearchCommand(new SearchCriteria(SearchCategory.LESSON, Map.of()));

        CommandException failure = assertThrows(CommandException.class, () -> command.execute(data, view));

        assertEquals("People search requires c/student, c/parent, or c/tutor.", failure.getMessage());
        assertEquals(Optional.of(PersonRole.PARENT), view.getRoleFilter());
        assertEquals(Optional.empty(), view.getSearchCriteria());
        assertEquals(before, data.exportState());
    }
}
