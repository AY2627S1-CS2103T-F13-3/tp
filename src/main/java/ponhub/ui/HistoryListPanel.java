package ponhub.ui;

import static java.util.Objects.requireNonNull;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import ponhub.model.query.AttendanceHistoryEntry;

/**
 * Scrolling retained attendance results, separate from the numbered people list.
 */
public class HistoryListPanel extends UiPart<Region> {
    private static final String FXML = "HistoryListPanel.fxml";

    @FXML
    private Label historyContext;
    @FXML
    private Label historySummary;
    @FXML
    private ListView<AttendanceHistoryEntry> historyListView;

    /**
     * Binds a stable observable result list and selector context to the history pane.
     */
    public HistoryListPanel(ObservableList<AttendanceHistoryEntry> history, ObservableValue<String> context) {
        super(FXML);
        historyContext.textProperty().bind(requireNonNull(context));
        historyListView.setItems(requireNonNull(history));
        historySummary.textProperty().bind(Bindings.size(history).asString("Showing %d attendance record(s)."));
        ResultHeadingTooltip.install(historyContext, historySummary);
        historyListView.setPlaceholder(new Label("No recorded attendance. Missing dates are unrecorded."));
        historyListView.setCellFactory(list -> new HistoryCell());
    }

    /**
     * Reuses a history row while fitting its current card to the available viewport width.
     */
    static class HistoryCell extends ListCell<AttendanceHistoryEntry> {
        HistoryCell() {
            // Let VirtualFlow allocate the viewport breadth, including space for its vertical scrollbar.
            setPrefWidth(0);
        }

        @Override
        protected void updateItem(AttendanceHistoryEntry entry, boolean empty) {
            super.updateItem(entry, empty);
            if (getGraphic() instanceof Region oldCard) {
                oldCard.prefWidthProperty().unbind();
            }
            setText(null);
            if (empty || entry == null) {
                setGraphic(null);
                return;
            }
            Region card = new AttendanceHistoryCard(entry).getRoot();
            card.prefWidthProperty().bind(Bindings.createDoubleBinding(() ->
                    Math.max(0, getWidth() - getInsets().getLeft() - getInsets().getRight()),
                    widthProperty(), insetsProperty()));
            setGraphic(card);
        }
    }
}
