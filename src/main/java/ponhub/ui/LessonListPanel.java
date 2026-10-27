package ponhub.ui;

import static java.util.Objects.requireNonNull;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import ponhub.model.query.LessonDetailView;
import ponhub.model.query.LessonView;

/**
 * A separate scrolling lesson view bound to the selected read-only results supplied by Logic.
 */
public class LessonListPanel extends UiPart<Region> {
    private static final String FXML = "LessonListPanel.fxml";

    @FXML
    private Label lessonSummary;
    @FXML
    private Label lessonContext;
    @FXML
    private ListView<LessonView> lessonListView;
    @FXML
    private LessonDetailList lessonDetailList;

    /**
     * Keeps the same observable result list through commands and committed refreshes.
     */
    public LessonListPanel(ObservableList<LessonView> lessons) {
        this(lessons, new ReadOnlyStringWrapper("All lessons").getReadOnlyProperty());
    }

    /**
     * Displays the selected student or search filters even after another command replaces command feedback.
     */
    public LessonListPanel(ObservableList<LessonView> lessons, ObservableValue<String> context) {
        super(FXML);
        lessonContext.textProperty().bind(requireNonNull(context));
        ResultHeadingTooltip.install(lessonContext, lessonSummary);
        lessonListView.setItems(requireNonNull(lessons));
        lessonListView.setCellFactory(list -> new LessonListCell());
        lessonListView.setPlaceholder(new Label("No lessons to display."));
        lessons.addListener((ListChangeListener<LessonView>) change -> refreshDisplay(lessons));
        refreshDisplay(lessons);
    }

    private void refreshDisplay(ObservableList<LessonView> lessons) {
        boolean showingDetail = lessons.size() == 1 && lessons.getFirst() instanceof LessonDetailView;
        lessonListView.setVisible(!showingDetail);
        lessonListView.setManaged(!showingDetail);
        lessonDetailList.setVisible(showingDetail);
        lessonDetailList.setManaged(showingDetail);
        if (showingDetail) {
            lessonSummary.setText("Showing lesson details.");
            lessonDetailList.showDetail((LessonDetailView) lessons.getFirst());
        } else {
            lessonSummary.setText(String.format("Showing %d lesson(s).", lessons.size()));
            lessonDetailList.getItems().clear();
        }
    }

    /**
     * Fits each wrapping card to the available cell width and clears recycled graphics.
     */
    static class LessonListCell extends ListCell<LessonView> {
        @Override
        protected void updateItem(LessonView lesson, boolean empty) {
            super.updateItem(lesson, empty);
            if (getGraphic() instanceof Region oldCard) {
                oldCard.prefWidthProperty().unbind();
            }
            setText(null);
            if (empty || lesson == null || lesson instanceof LessonDetailView) {
                setGraphic(null);
                return;
            }
            Region card = new LessonCard(lesson).getRoot();
            card.prefWidthProperty().bind(Bindings.createDoubleBinding(() ->
                    Math.max(0, getWidth() - getInsets().getLeft() - getInsets().getRight()),
                    widthProperty(), insetsProperty()));
            setGraphic(card);
        }
    }
}
