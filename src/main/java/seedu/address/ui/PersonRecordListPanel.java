package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import seedu.address.model.PeopleView;
import seedu.address.model.person.PersonRecord;

/**
 * A dormant scrolling host for the canonical people's read-only observable projection.
 * The model view supplies both the cards and command-index source; this panel owns no operational records.
 */
public class PersonRecordListPanel extends UiPart<Region> {

    private static final String FXML = "PersonRecordListPanel.fxml";

    @FXML
    private Label summary;

    @FXML
    private ListView<PersonRecord> personListView;

    /**
     * Creates a panel bound to the supplied canonical view without activating it in MainWindow.
     * Later filter changes and explicit model refreshes update the same list and renumber visible cards.
     */
    public PersonRecordListPanel(PeopleView peopleView) {
        super(FXML);
        personListView.setItems(requireNonNull(peopleView).getPeople());
        summary.textProperty().bind(Bindings.size(peopleView.getPeople()).asString("Showing %d person(s)."));
        personListView.setCellFactory(list -> new PersonRecordListCell());
        personListView.setPlaceholder(new Label("No persons to display."));
    }

    /**
     * Renders the cell's current position separately from stable identity and fits wrapping to its content width.
     */
    private static class PersonRecordListCell extends ListCell<PersonRecord> {

        private PersonRecordListCell() {
            indexProperty().addListener((observable, oldIndex, newIndex) -> updateCard());
        }

        @Override
        protected void updateItem(PersonRecord person, boolean empty) {
            super.updateItem(person, empty);
            updateCard();
        }

        /**
         * Clears recycled graphics before displaying the current record at its current position.
         */
        private void updateCard() {
            if (getGraphic() instanceof Region oldCard) {
                oldCard.prefWidthProperty().unbind();
            }
            setText(null);
            if (isEmpty() || getItem() == null || getIndex() < 0) {
                setGraphic(null);
                return;
            }

            Region card = new PersonRecordCard(getItem(), getIndex() + 1).getRoot();
            card.prefWidthProperty().bind(Bindings.createDoubleBinding(() ->
                    Math.max(0, getWidth() - getInsets().getLeft() - getInsets().getRight()),
                    widthProperty(), insetsProperty()));
            setGraphic(card);
        }
    }
}
