package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import seedu.address.testutil.EditPersonDescriptorBuilder;
import seedu.address.testutil.PersonBuilder;

public class RemarkCommandTest {
    private final Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

    @Test
    public void execute_addReplaceAndRemoveRemark_success() throws Exception {
        Person original = model.getFilteredPersonList().get(0);
        for (String text : new String[]{"Likes swimming", "Prefers mornings", ""}) {
            CommandResult result = new RemarkCommand(INDEX_FIRST_PERSON, new Remark(text)).execute(model);
            Person expected = new PersonBuilder(original).withRemark(text).build();
            assertEquals(expected, model.getFilteredPersonList().get(0));
            String message = text.isEmpty() ? RemarkCommand.MESSAGE_DELETE_REMARK_SUCCESS
                    : RemarkCommand.MESSAGE_ADD_REMARK_SUCCESS;
            assertEquals(String.format(message, Messages.format(expected)), result.getFeedbackToUser());
        }
    }

    @Test
    public void execute_filteredList_changesDisplayedPerson() throws Exception {
        Person target = model.getFilteredPersonList().get(1);
        showPersonAtIndex(model, INDEX_SECOND_PERSON);
        new RemarkCommand(INDEX_FIRST_PERSON, new Remark("Selected")).execute(model);
        assertEquals(new PersonBuilder(target).withRemark("Selected").build(),
                model.getFilteredPersonList().get(1));
    }

    @Test
    public void execute_invalidIndex_failure() {
        Index outside = Index.fromZeroBased(model.getFilteredPersonList().size());
        assertCommandFailure(new RemarkCommand(outside, new Remark("Note")), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        assertCommandFailure(new RemarkCommand(INDEX_SECOND_PERSON, new Remark("Note")), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void execute_editOtherDetails_preservesRemark() throws Exception {
        new RemarkCommand(INDEX_FIRST_PERSON, new Remark("Keep me")).execute(model);
        new EditCommand(INDEX_FIRST_PERSON, new EditPersonDescriptorBuilder().withPhone("91234567").build())
                .execute(model);
        assertEquals(new Remark("Keep me"), model.getFilteredPersonList().get(0).getRemark());
    }
}
