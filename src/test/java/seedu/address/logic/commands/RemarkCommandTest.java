package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.showPersonAtIndex;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static seedu.address.testutil.TypicalIndexes.INDEX_SECOND_PERSON;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.Messages;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.model.person.Remark;
import seedu.address.testutil.PersonBuilder;

public class RemarkCommandTest {
    private Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

    @Test
    public void execute_addReplaceRemove_preservesOtherFields() throws Exception {
        Person original = model.getFilteredPersonList().get(0);
        for (String value : new String[]{"Likes baseball", "Prefers swimming", ""}) {
            RemarkCommand command = new RemarkCommand(INDEX_FIRST_PERSON, new Remark(value));
            String message = value.isEmpty() ? RemarkCommand.MESSAGE_DELETE_REMARK_SUCCESS
                    : RemarkCommand.MESSAGE_ADD_REMARK_SUCCESS;
            Person expected = new PersonBuilder(original).withRemark(value).build();
            assertEquals(String.format(message, Messages.format(expected)), command.execute(model).getFeedbackToUser());
            assertEquals(expected, model.getFilteredPersonList().get(0));
        }
    }

    @Test
    public void execute_filteredList_editsDisplayedPersonAndShowsAll() throws Exception {
        Person target = model.getFilteredPersonList().get(1);
        showPersonAtIndex(model, INDEX_SECOND_PERSON);
        new RemarkCommand(INDEX_FIRST_PERSON, new Remark("A note")).execute(model);
        assertEquals(new PersonBuilder(target).withRemark("A note").build(), model.getFilteredPersonList().get(1));
        assertEquals(getTypicalAddressBook().getPersonList().size(), model.getFilteredPersonList().size());
    }

    @Test
    public void execute_invalidIndex_failsWithoutChanges() {
        Index invalid = Index.fromZeroBased(model.getFilteredPersonList().size());
        assertCommandFailure(new RemarkCommand(invalid, new Remark("A note")), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        assertCommandFailure(new RemarkCommand(INDEX_SECOND_PERSON, new Remark("A note")), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void execute_editAfterRemark_preservesRemark() throws Exception {
        new RemarkCommand(INDEX_FIRST_PERSON, new Remark("Keep me")).execute(model);
        EditCommand.EditPersonDescriptor descriptor = new EditCommand.EditPersonDescriptor();
        descriptor.setPhone(new seedu.address.model.person.Phone("12345678"));
        new EditCommand(INDEX_FIRST_PERSON, descriptor).execute(model);
        assertEquals(new Remark("Keep me"), model.getFilteredPersonList().get(0).getRemark());
    }

    @Test
    public void equals() {
        RemarkCommand command = new RemarkCommand(INDEX_FIRST_PERSON, new Remark("note"));
        assertTrue(command.equals(command));
        assertTrue(command.equals(new RemarkCommand(INDEX_FIRST_PERSON, new Remark("note"))));
        assertFalse(command.equals(new RemarkCommand(INDEX_SECOND_PERSON, new Remark("note"))));
        assertFalse(command.equals(new RemarkCommand(INDEX_FIRST_PERSON, new Remark(""))));
        assertFalse(command.equals(null));
        assertFalse(command.equals("note"));
    }

    @Test
    public void toStringMethod() {
        Index index = Index.fromOneBased(1);
        Remark remark = new Remark("note");
        RemarkCommand command = new RemarkCommand(index, remark);
        String expected = RemarkCommand.class.getCanonicalName() + "{index=" + index + ", remark=" + remark + "}";
        assertEquals(expected, command.toString());
    }
}
