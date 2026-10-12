package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.FxTestUtil.runOnFxThread;
import static seedu.address.testutil.LessonQueryTestData.createData;
import static seedu.address.testutil.LessonQueryTestData.lesson;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.Region;
import seedu.address.MainApp;
import seedu.address.model.PonHubData;
import seedu.address.model.PonHubDataState;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.Name;
import seedu.address.model.person.PeopleRegistryState;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Tutor;
import seedu.address.model.query.LessonQueries;
import seedu.address.model.query.LessonView;

/**
 * Verifies actual FXML cards, recycled-cell bindings, long-value wrapping and scrolling to the last result.
 */
public class LessonListPanelTest {
    @Test
    public void cards_showStableIdentityCompleteDetailsAndEmptyRoster() throws Exception {
        runOnFxThread(() -> {
            var lessons = new LessonQueries(createData()).getLessonList();
            Region card = new LessonCard(lessons.get(1)).getRoot();
            assertLabel(card, "lessonHeading", "L10 · Math");
            assertLabel(card, "lessonSchedule", "Schedule: Mon 0900-1000");
            assertLabel(card, "lessonTutor", "Tutor: Mei Lim (T1)");
            assertLabel(card, "lessonRoom", "Room: R1");
            assertLabel(card, "lessonRosterSize", "Students enrolled: 2");
            assertLabel(new LessonCard(lessons.get(2)).getRoot(), "lessonRosterSize", "Students enrolled: 0");
            assertThrows(NullPointerException.class, () -> new LessonCard(null));
            assertThrows(NullPointerException.class, () -> new LessonListPanel(null));
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
            assertLabel((Region) cell.getGraphic(), "lessonHeading", "L10 · Math");
            cell.updateItem(null, false);
            assertNull(cell.getGraphic());
            cell.updateItem(lessons.get(0), false);
            cell.updateItem(null, true);
            assertNull(cell.getGraphic());
            assertNull(cell.getText());
        });
    }

    @Test
    public void narrowPanel_wrapsLongValuesAndScrollsToCompleteLastCard() throws Exception {
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
                for (var node : view.lookupAll("#lessonCardPane")) {
                    for (var labelNode : node.lookupAll(".label")) {
                        Label label = (Label) labelNode;
                        assertTrue(label.isWrapText());
                        assertTrue(label.getWidth() > 0 && label.getWidth() <= width);
                        assertTrue(label.getHeight() + 1 >= label.prefHeight(label.getWidth()), label.getText());
                    }
                }
                view.scrollTo(1);
                view.layout();
                for (var node : view.lookupAll(".scroll-bar")) {
                    if (node instanceof ScrollBar bar && bar.isVisible()
                            && bar.getOrientation() == Orientation.VERTICAL) {
                        bar.setValue(bar.getMax());
                    }
                }
                view.layout();
                var last = view.lookupAll("#lessonCardPane").stream()
                        .filter(node -> ((Label) node.lookup("#lessonHeading")).getText().startsWith("L2 ·"))
                        .findFirst().orElseThrow();
                var bounds = last.localToScene(last.getBoundsInLocal());
                assertTrue(bounds.getMinY() >= 0 && bounds.getMaxY() <= panel.getHeight() + 1, bounds.toString());
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

    @SuppressWarnings("unchecked")
    private static ListView<LessonView> list(Region panel) {
        return (ListView<LessonView>) panel.lookup("#lessonListView");
    }
}
