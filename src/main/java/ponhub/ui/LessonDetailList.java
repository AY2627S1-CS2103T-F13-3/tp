package ponhub.ui;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Locale;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ponhub.model.attendance.AttendanceStatus;
import ponhub.model.query.LessonDetailView;
import ponhub.model.query.LessonView;

/**
 * Scrolls a complete lesson detail while creating controls only for the rows needed by the viewport.
 * The lesson summary and both expanded sections share one variable-height virtualized list.
 */
public class LessonDetailList extends ListView<LessonDetailList.Row> {
    private static final KeyCodeCombination END_SCROLL = new KeyCodeCombination(KeyCode.END,
            KeyCombination.SHIFT_ANY, KeyCombination.SHORTCUT_ANY);

    private boolean shouldScrollToEnd;

    /**
     * Creates a width-fitting detail view with ordinary keyboard scrolling, independent of people selectors.
     */
    public LessonDetailList() {
        setCellFactory(list -> new DetailCell());
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            shouldScrollToEnd = END_SCROLL.match(event);
            if (shouldScrollToEnd) {
                requestLayout();
            }
        });
        addEventFilter(ScrollEvent.SCROLL, event -> shouldScrollToEnd = false);
        addEventFilter(MouseEvent.MOUSE_PRESSED, event -> shouldScrollToEnd = false);
    }

    /**
     * Replaces the displayed immutable snapshot, retaining every record in its supplied order.
     */
    public void showDetail(LessonDetailView detail) {
        requireNonNull(detail);
        shouldScrollToEnd = false;
        var rows = new ArrayList<Row>();
        rows.add(new Summary(detail));
        rows.add(new Heading("Current roster", false));
        if (detail.currentRoster().isEmpty()) {
            rows.add(new EmptyMessage("No students currently enrolled."));
        }
        for (var entry : detail.currentRoster()) {
            String status = detail.date().isEmpty() ? ""
                    : "Attendance: " + entry.status().map(LessonDetailList::displayStatus).orElse("unrecorded");
            rows.add(new Entry(entry.student().getId() + " · " + entry.student().getName().fullName, status));
        }
        rows.add(new Heading("Former members' retained attendance", true));
        if (detail.formerAttendance().isEmpty()) {
            rows.add(new EmptyMessage("No retained attendance for former members."));
        }
        for (var record : detail.formerAttendance()) {
            rows.add(new Entry(record.student().getId() + " · " + record.student().getName().fullName,
                    "Date: " + record.date() + " · Status: " + displayStatus(record.status())));
        }
        getItems().setAll(rows);
        getSelectionModel().clearSelection();
        getFocusModel().focus(0);
        scrollTo(0);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (shouldScrollToEnd) {
            shouldScrollToEnd = false;
            if (getItems().isEmpty()) {
                return;
            }
            // End can use estimated heights before the final wrapping rows have been measured.
            for (var child : getChildrenUnmodifiable()) {
                if (child instanceof Parent parent) {
                    parent.layout();
                }
            }
            scrollTo(getItems().size() - 1);
        }
    }

    private static String displayStatus(AttendanceStatus status) {
        return status.name().toLowerCase(Locale.ROOT);
    }

    private static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().addAll("cell_small_label", styleClass);
        return label;
    }

    /**
     * Data-only rows keep the complete result available without allocating its scene graph eagerly.
     */
    sealed interface Row permits Summary, Heading, EmptyMessage, Entry {
    }

    /** Summary data is rendered only while its row is needed. */
    private record Summary(LessonView lesson) implements Row {
    }

    /** An expanded section heading, with a boundary before retained attendance. */
    private record Heading(String text, boolean isSecondary) implements Row {
    }

    /** Explicit feedback for a section with no entries. */
    private record EmptyMessage(String text) implements Row {
    }

    /** Complete entry text, with an optional empty secondary line for an undated roster. */
    private record Entry(String primary, String secondary) implements Row {
    }

    /**
     * Releases recycled width bindings and fits wrapping content to the viewport breadth.
     */
    private static class DetailCell extends ListCell<Row> {
        DetailCell() {
            setPrefWidth(0);
        }

        @Override
        protected void updateItem(Row row, boolean empty) {
            super.updateItem(row, empty);
            if (getGraphic() instanceof Region oldRow) {
                oldRow.prefWidthProperty().unbind();
            }
            setText(null);
            if (empty || row == null) {
                setGraphic(null);
                return;
            }
            Region content = switch (row) {
                case Summary summary -> new LessonCard(summary.lesson()).getRoot();
                case Heading heading -> createHeading(heading);
                case EmptyMessage message -> wrap(label(message.text(), "card_empty_label"), 2, 5);
                case Entry entry -> createEntry(entry);
            };
            content.prefWidthProperty().bind(Bindings.createDoubleBinding(() ->
                    Math.max(0, getWidth() - getInsets().getLeft() - getInsets().getRight()),
                    widthProperty(), insetsProperty()));
            setGraphic(content);
        }

        private static VBox createHeading(Heading heading) {
            VBox content = wrap(label(heading.text(), "card_section_heading"), 10, 5);
            if (heading.isSecondary()) {
                content.getStyleClass().add("card_secondary_section");
            }
            return content;
        }

        private static VBox createEntry(Entry entry) {
            VBox content = wrap(label(entry.primary(), "card_entry_primary"), 2, 2);
            content.getStyleClass().add("card_entry");
            if (!entry.secondary().isEmpty()) {
                content.getChildren().add(label(entry.secondary(), "card_entry_secondary"));
            }
            return content;
        }

        private static VBox wrap(Label label, double top, double bottom) {
            VBox content = new VBox(2, label);
            content.setPadding(new Insets(top, 15, bottom, 15));
            content.setMinWidth(0);
            content.setMaxWidth(Double.MAX_VALUE);
            return content;
        }
    }
}
