package ponhub.ui;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import ponhub.model.PeopleView;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.search.PersonSearchMatch;

/**
 * A scrolling host for the canonical people's read-only observable projection.
 * The model view supplies both the cards and command-index source; this panel owns no operational records.
 */
public class PersonRecordListPanel extends UiPart<Region> {

    private static final String FXML = "PersonRecordListPanel.fxml";

    @FXML
    private Label summary;

    @FXML
    private Label peopleContext;

    @FXML
    private ScrollPane peopleContextPane;

    @FXML
    private Label filterResetHint;

    @FXML
    private ListView<PersonRecord> personListView;

    /**
     * Creates a panel bound to the supplied canonical view.
     * Later filter changes and explicit model refreshes update the same list and renumber visible cards.
     */
    public PersonRecordListPanel(PeopleView peopleView) {
        this(requireNonNull(peopleView).getPeople(), peopleView.searchContextProperty(),
                peopleView.resultContextProperty());
    }

    /**
     * Creates a panel over the exact read-only list supplied by application Logic.
     * Committed changes and filtering update that shared list without creating another data source.
     */
    public PersonRecordListPanel(ObservableList<PersonRecord> people) {
        this(people, new ReadOnlyObjectWrapper<Map<PersonId, List<PersonSearchMatch>>>(Map.of()).getReadOnlyProperty());
    }

    /**
     * Refreshes contextual cards from Logic's immutable projection without changing operational relationships.
     */
    public PersonRecordListPanel(ObservableList<PersonRecord> people,
            ObservableValue<Map<PersonId, List<PersonSearchMatch>>> contexts) {
        this(people, contexts, new ReadOnlyStringWrapper("All people").getReadOnlyProperty());
    }

    /**
     * Keeps the current query visible when unrelated commands replace transient feedback.
     */
    public PersonRecordListPanel(ObservableList<PersonRecord> people,
            ObservableValue<Map<PersonId, List<PersonSearchMatch>>> contexts, ObservableValue<String> scope) {
        super(FXML);
        requireNonNull(contexts);
        peopleContext.textProperty().bind(requireNonNull(scope));
        scope.addListener((observable, previous, current) -> peopleContextPane.setVvalue(0));
        ResultHeadingTooltip.install(peopleContext);
        filterResetHint.visibleProperty().bind(peopleContext.textProperty().isNotEqualTo("All people"));
        filterResetHint.managedProperty().bind(filterResetHint.visibleProperty());
        personListView.setItems(requireNonNull(people));
        summary.textProperty().bind(Bindings.size(people).asString("Showing %d person(s)."));
        personListView.setCellFactory(list -> new PersonRecordListCell(contexts));
        contexts.addListener((observable, previous, current) -> personListView.refresh());
        personListView.setPlaceholder(new Label("No persons to display."));
    }

    /**
     * Returns the selected stable identity before a command can replace immutable records.
     */
    public Optional<PersonId> getSelectedPersonId() {
        return Optional.ofNullable(personListView.getSelectionModel().getSelectedItem()).map(PersonRecord::getId);
    }

    /**
     * Retains selection by identity only while visible, clearing it if a filter removed that person.
     */
    public void restoreSelection(PersonId id) {
        requireNonNull(id);
        var replacement = personListView.getItems().stream().filter(person -> person.getId().equals(id)).findFirst();
        if (replacement.isPresent()) {
            personListView.getSelectionModel().select(replacement.orElseThrow());
        } else {
            personListView.getSelectionModel().clearSelection();
        }
    }

    /**
     * Renders the cell's current position separately from stable identity and fits wrapping to its content width.
     */
    private static class PersonRecordListCell extends ListCell<PersonRecord> {
        private final ObservableValue<Map<PersonId, List<PersonSearchMatch>>> contexts;
        private final DoubleProperty cardWidth = new SimpleDoubleProperty();

        private PersonRecordListCell(ObservableValue<Map<PersonId, List<PersonSearchMatch>>> contexts) {
            this.contexts = contexts;
            widthProperty().addListener(observable -> updateCardWidth());
            insetsProperty().addListener(observable -> updateCardWidth());
            indexProperty().addListener((observable, oldIndex, newIndex) -> updateCard());
        }

        @Override
        protected void layoutChildren() {
            // Refresh snapping when a window changes display scale without changing logical cell width.
            updateCardWidth();
            super.layoutChildren();
        }

        /**
         * Reserves the same pixel-snapped insets as the cell skin when fitting its wrapping graphic.
         */
        private void updateCardWidth() {
            cardWidth.set(Math.max(0, getWidth() - snappedLeftInset() - snappedRightInset()));
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

            Region card = new PersonRecordCard(getItem(), getIndex() + 1,
                    contexts.getValue().getOrDefault(getItem().getId(), List.of())).getRoot();
            card.prefWidthProperty().bind(cardWidth);
            setGraphic(card);
        }
    }
}
