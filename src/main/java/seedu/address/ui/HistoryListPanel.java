package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import seedu.address.model.query.AttendanceHistoryEntry;

/** Scrolling retained attendance results, separate from the numbered people list. */
public class HistoryListPanel extends UiPart<Region> {
    private static final String FXML = "HistoryListPanel.fxml";

    @FXML
    private Label historyContext;
    @FXML
    private Label historySummary;
    @FXML
    private ListView<AttendanceHistoryEntry> historyListView;

    /** Binds a stable observable result list and selector context to the history pane. */
    public HistoryListPanel(ObservableList<AttendanceHistoryEntry> history, ObservableValue<String> context) {
        super(FXML);
        historyContext.textProperty().bind(requireNonNull(context));
        historyListView.setItems(requireNonNull(history));
        historySummary.textProperty().bind(Bindings.size(history).asString("Showing %d attendance record(s)."));
        historyListView.setPlaceholder(new Label("No recorded attendance. Missing dates are unrecorded."));
        historyListView.setCellFactory(list -> new HistoryCell());
    }

    static class HistoryCell extends ListCell<AttendanceHistoryEntry> {
        HistoryCell() {
            setWrapText(true);
        }

        @Override
        protected void updateItem(AttendanceHistoryEntry entry, boolean empty) {
            super.updateItem(entry, empty);
            setGraphic(null);
            setText(empty || entry == null ? null : entry.getDate() + " · " + entry.getLessonId()
                    + " · " + entry.getStatus().name().toLowerCase(java.util.Locale.ROOT)
                    + " · " + (entry.isCurrentlyEnrolled() ? "currently enrolled" : "former enrolment"));
        }
    }
}
