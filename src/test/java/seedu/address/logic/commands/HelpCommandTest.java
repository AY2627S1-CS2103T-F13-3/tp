package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import org.junit.jupiter.api.Test;

import seedu.address.logic.CommandCatalog;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;

public class HelpCommandTest {
    @Test
    public void execute_overview_returnsInlineGuidance() {
        CommandResult result = new HelpCommand().execute(new ModelManager());
        assertEquals(CommandCatalog.getOverview(), result.getFeedbackToUser());
        assertFalse(result.isShowHelp());
        assertFalse(result.isExit());
    }

    @Test
    public void execute_everyTopic_preservesRecordsFilterAndPreferences() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        model.updateFilteredPersonList(person -> person.equals(ALICE));
        var filteredList = model.getFilteredPersonList();
        var addressBook = model.getAddressBook();
        Model expectedModel = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        expectedModel.updateFilteredPersonList(person -> person.equals(ALICE));

        for (String topic : CommandCatalog.getCommandWords()) {
            CommandResult result = new HelpCommand(topic).execute(model);
            assertEquals(CommandCatalog.getUsage(topic).orElseThrow(), result.getFeedbackToUser());
            assertFalse(result.isShowHelp());
            assertFalse(result.isExit());
            assertEquals(expectedModel, model);
            assertSame(addressBook, model.getAddressBook());
            assertSame(filteredList, model.getFilteredPersonList());
            assertEquals(java.util.List.of(ALICE), filteredList);
        }
        new HelpCommand().execute(model);
        assertEquals(expectedModel, model);
        assertEquals(java.util.List.of(ALICE), filteredList);
    }

    @Test
    public void constructor_invalidTopic_throwsException() {
        assertThrows(NullPointerException.class, () -> new HelpCommand(null));
        assertThrows(IllegalArgumentException.class, () -> new HelpCommand("unknown"));
    }

    @Test
    public void equals_comparesTopic() {
        HelpCommand addHelp = new HelpCommand("add");
        assertEquals(addHelp, addHelp);
        assertEquals(addHelp, new HelpCommand("add"));
        assertEquals(addHelp.hashCode(), new HelpCommand("add").hashCode());
        assertEquals(new HelpCommand(), new HelpCommand());
        assertNotEquals(addHelp, new HelpCommand("delete"));
        assertNotEquals(addHelp, new HelpCommand());
        assertNotEquals(addHelp, null);
        assertNotEquals(addHelp, "add");
    }
}
