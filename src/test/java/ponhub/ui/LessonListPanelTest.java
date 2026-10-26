package ponhub.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.FxTestUtil.runOnFxThread;
import static ponhub.testutil.LessonQueryTestData.createData;
import static ponhub.testutil.LessonQueryTestData.lesson;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.Region;
import ponhub.MainApp;
import ponhub.model.PonHubData;
import ponhub.model.PonHubDataState;
import ponhub.model.lesson.LessonId;
import ponhub.model.person.ContactDetails;
import ponhub.model.person.Name;
import ponhub.model.person.PeopleRegistryState;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRole;
import ponhub.model.person.Phone;
import ponhub.model.person.Tutor;
import ponhub.model.query.LessonDetailView;
import ponhub.model.query.LessonQueries;
import ponhub.model.query.LessonView;

/**
 * Verifies actual FXML cards, recycled-cell bindings, long-value wrapping and scrolling to the last result.
 */
public class LessonListPanelTest {
    @Test
    public void cards_showStableIdentityCompleteDetailsAndEmptyRoster() throws Exception {
        runOnFxThread(() -> {
            var lessons = new LessonQueries(createData()).getLessonList();
            Region card = new LessonCard(lessons.get(1)).getRoot();
            assertLabel(card, "lessonHeading", "Math");
            assertLabel(card, "lessonIdentity", "Lesson L10");
            assertLabel(card, "lessonSchedule", "Mon 0900-1000");
            assertLabel(card, "lessonTutor", "Mei Lim · T1");
            assertLabel(card, "lessonRoom", "R1");
            assertLabel(card, "lessonRosterSize", "2");
            assertLabel(new LessonCard(lessons.get(2)).getRoot(), "lessonRosterSize", "0");
            assertEquals(Set.of("Schedule", "Tutor", "Room", "Students enrolled"),
                    card.lookupAll(".card_field_label").stream().map(Label.class::cast)
                            .map(Label::getText).collect(Collectors.toSet()));
            assertThrows(NullPointerException.class, () -> new LessonCard(null));
            assertThrows(NullPointerException.class, () -> new LessonListPanel(null));
            assertThrows(NullPointerException.class, () -> new LessonListPanel(lessons, null));
            SimpleStringProperty context = new SimpleStringProperty("Student S2 · current lessons");
            Region panel = new LessonListPanel(lessons, context).getRoot();
            assertLabel(panel, "lessonContext", context.get());
            context.set("Lesson search: s/math");
            assertLabel(panel, "lessonContext", "Lesson search: s/math");
        });
    }

    @Test
    public void panel_retainsObservableListAndShowsEmptyFeedbackAfterRefresh() throws Exception {
        runOnFxThread(() -> {
            PonHubData data = createData();
            var results = new LessonQueries(data).getLessonList();
            Region panel = new LessonListPanel(results).getRoot();
            layout(panel, 400, 500);
            ListView<LessonView> list = list(panel);
            assertSame(results, list.getItems());
            assertLabel(panel, "lessonSummary", "Showing 5 lesson(s).");
            var before = data.exportState();
            data.resetData(new PonHubDataState(before.people(), List.of(), List.of(), 11));
            panel.layout();
            assertLabel(panel, "lessonSummary", "Showing 0 lesson(s).");
            assertEquals("No lessons to display.", ((Label) list.getPlaceholder()).getText());
            data.resetData(before);
            panel.layout();
            assertSame(results, list.getItems());
            assertEquals(5, list.getItems().size());
        });
    }

    @Test
    public void recycledCell_releasesOldBindingsAndClearsEmptyOrNullItems() throws Exception {
        runOnFxThread(() -> {
            var lessons = new LessonQueries(createData()).getLessonList();
            var cell = new LessonListPanel.LessonListCell();
            cell.updateItem(lessons.get(0), false);
            Region previous = (Region) cell.getGraphic();
            assertTrue(previous.prefWidthProperty().isBound());
            cell.resize(320, 200);
            assertTrue(previous.getPrefWidth() <= 320);
            cell.updateItem(lessons.get(1), false);
            assertFalse(previous.prefWidthProperty().isBound());
            assertLabel((Region) cell.getGraphic(), "lessonHeading", "Math");
            var detail = new LessonDetailView(createData().getLesson(new LessonId("L10")).orElseThrow(),
                    "Mei Lim", Optional.empty(), List.of(), List.of());
            cell.updateItem(detail, false);
            assertNull(cell.getGraphic());
            cell.updateItem(null, false);
            assertNull(cell.getGraphic());
            cell.updateItem(lessons.get(0), false);
            cell.updateItem(null, true);
            assertNull(cell.getGraphic());
            assertNull(cell.getText());
        });
    }

    @Test
    public void narrowPanel_wrapsLongValuesAndScrollsToLastCardEnd() throws Exception {
        runOnFxThread(() -> {
            PersonId tutor = new PersonId("T1");
            var people = new PeopleRegistryState(List.of(new Tutor(tutor,
                    new ContactDetails(new Name("Long Tutor Name ".repeat(6).strip()),
                            Optional.of(new Phone("91234567")), Optional.empty(), Optional.empty()))),
                    Map.of(PersonRole.TUTOR, 1L, PersonRole.STUDENT, 0L, PersonRole.PARENT, 0L));
            var data = new PonHubData(new PonHubDataState(people, List.of(
                    lesson("L1", tutor, "Mon", "0000", "0100", "Subject ".repeat(6).strip(),
                            "ROOM123456", Set.of()),
                    lesson("L2", tutor, "Tue", "2300", "2359", "Last subject", "LASTROOM", Set.of())),
                    List.of(), 2));
            for (int width : new int[] {210, 420, 853}) {
                Region panel = new LessonListPanel(new LessonQueries(data).getLessonList()).getRoot();
                layout(panel, width, 480);
                ListView<LessonView> view = list(panel);
                assertFalse(view.lookupAll(".scroll-bar").stream().map(ScrollBar.class::cast)
                        .anyMatch(bar -> bar.getOrientation() == Orientation.HORIZONTAL && bar.isVisible()));
                for (var node : view.lookupAll("#lessonCardPane")) {
                    for (var labelNode : node.lookupAll(".label")) {
                        Label label = (Label) labelNode;
                        assertTrue(label.isWrapText());
                        assertTrue(label.getWidth() > 0 && label.getWidth() <= width,
                                label.getText() + " width=" + label.getWidth() + ", panel=" + width);
                        assertTrue(label.getHeight() + 1 >= label.prefHeight(label.getWidth()), label.getText());
                    }
                }
                view.scrollTo(1);
                view.layout();
                Optional<ScrollBar> vertical = visibleScrollBar(view, Orientation.VERTICAL);
                if (vertical.isPresent()) {
                    ScrollBar bar = vertical.orElseThrow();
                    double previousMaximum;
                    int layoutPasses = 0;
                    do {
                        previousMaximum = bar.getMax();
                        bar.setValue(previousMaximum);
                        panel.applyCss();
                        panel.layout();
                        view.layout();
                        layoutPasses++;
                    } while (layoutPasses < 12 && (Math.abs(bar.getMax() - previousMaximum) > 0.01
                            || Math.abs(bar.getValue() - bar.getMax()) > 0.01));
                    bar.setValue(bar.getMax());
                    panel.layout();
                    view.layout();
                    assertEquals(bar.getMax(), bar.getValue(), 0.01);
                }
                var viewportBounds = view.localToScene(view.getBoundsInLocal());
                var last = view.lookupAll("#lessonCardPane").stream()
                        .filter(node -> ((Label) node.lookup("#lessonIdentity")).getText().equals("Lesson L2"))
                        .findFirst().orElseThrow();
                var bounds = last.localToScene(last.getBoundsInLocal());
                assertTrue(bounds.getMaxY() >= viewportBounds.getMinY()
                                && bounds.getMaxY() <= viewportBounds.getMaxY() + 1,
                        "Scrolling to the final card must expose its end: " + bounds
                                + "; viewport=" + viewportBounds);
            }
        });
    }

    private static void assertLabel(Region root, String id, String expected) {
        assertEquals(expected, ((Label) root.lookup("#" + id)).getText());
    }

    private static void layout(Region panel, int width, int height) {
        Scene scene = new Scene(panel, width, height);
        scene.getStylesheets().add(MainApp.class.getResource("/view/DarkTheme.css").toExternalForm());
        panel.applyCss();
        panel.resize(width, height);
        panel.layout();
    }

    private static Optional<ScrollBar> visibleScrollBar(ListView<?> view, Orientation orientation) {
        return view.lookupAll(".scroll-bar").stream().map(ScrollBar.class::cast)
                .filter(bar -> bar.getOrientation() == orientation && bar.isVisible()).findFirst();
    }

    @SuppressWarnings("unchecked")
    private static ListView<LessonView> list(Region panel) {
        return (ListView<LessonView>) panel.lookup("#lessonListView");
    }
}
