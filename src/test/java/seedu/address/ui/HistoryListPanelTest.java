package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.FxTestUtil.runOnFxThread;

import java.time.LocalDate;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.query.AttendanceHistoryEntry;
import seedu.address.model.query.AttendanceHistoryEntryData;
import seedu.address.testutil.LessonQueryTestData;

public class HistoryListPanelTest {
    @Test
    public void layout_wrapsCompleteRowsWithinCompactAndNormalViewports() throws Exception {
        runOnFxThread(() -> {
            var history = FXCollections.<AttendanceHistoryEntry>observableArrayList();
            IntStream.range(0, 60).forEach(index -> history.add(new AttendanceHistoryEntryData(
                    LessonQueryTestData.STUDENT_ONE, LessonId.fromSequenceNumber(Long.MAX_VALUE),
                    LocalDate.of(2026, 10, 5).minusDays(index),
                    index % 2 == 0 ? AttendanceHistoryEntry.Status.PRESENT : AttendanceHistoryEntry.Status.ABSENT,
                    index % 2 == 0)));
            Region panel = new HistoryListPanel(history, new SimpleStringProperty("Attendance history")).getRoot();
            Scene scene = new Scene(panel, 198, 220);
            scene.getStylesheets().add(MainWindow.class.getResource("/view/DarkTheme.css").toExternalForm());
            @SuppressWarnings("unchecked")
            ListView<AttendanceHistoryEntry> list = (ListView<AttendanceHistoryEntry>) panel.lookup("#historyListView");
            list.getSelectionModel().select(0);
            for (int width : new int[] {198, 399, 160, 399}) {
                panel.resize(width, 220);
                layout(panel);
                assertSame(history, list.getItems());
                assertEquals(history.getFirst(), list.getSelectionModel().getSelectedItem());
                assertTrue(scrollBar(list, Orientation.VERTICAL).isVisible());
                assertFalse(scrollBar(list, Orientation.HORIZONTAL).isVisible(),
                        "History must wrap without horizontal scrolling at width " + width);
                assertWrappedRow(list, history.getFirst());
                list.scrollTo(59);
                layout(panel);
                assertWrappedRow(list, history.getLast());
                list.scrollTo(0);
                layout(panel);
            }
            // Refresh and clear the same live list after VirtualFlow has reused its cells.
            history.setAll(new AttendanceHistoryEntryData(LessonQueryTestData.STUDENT_ONE,
                    LessonQueryTestData.SHARED_LESSON, LocalDate.of(2026, 10, 5),
                    AttendanceHistoryEntry.Status.ABSENT, false));
            list.scrollTo(0);
            layout(panel);
            assertWrappedRow(list, history.getFirst());
            history.clear();
            layout(panel);
            assertTrue(list.lookupAll(".list-cell").stream().map(ListCell.class::cast)
                    .allMatch(cell -> cell.getText() == null && cell.getGraphic() == null));
            assertTrue(list.getPlaceholder().isVisible());
        });
    }

    private static void layout(Region panel) {
        panel.applyCss();
        for (int pass = 0; pass < 4; pass++) {
            panel.layout();
        }
    }

    private static ScrollBar scrollBar(ListView<?> list, Orientation orientation) {
        return list.lookupAll(".scroll-bar").stream().map(ScrollBar.class::cast)
                .filter(bar -> bar.getOrientation() == orientation).findFirst().orElseThrow();
    }

    private static void assertWrappedRow(ListView<?> list, AttendanceHistoryEntry entry) {
        ListCell<?> cell = list.lookupAll(".list-cell").stream().map(ListCell.class::cast)
                .filter(candidate -> entry.equals(candidate.getItem())).findFirst().orElseThrow();
        String expected = entry.getDate() + " · " + entry.getLessonId() + " · "
                + entry.getStatus().name().toLowerCase(java.util.Locale.ROOT) + " · "
                + (entry.isCurrentlyEnrolled() ? "currently enrolled" : "former enrolment");
        assertEquals(expected, cell.getText());
        Text text = (Text) cell.lookup(".text");
        assertEquals(expected, text.getText(), "The skin must render every field without ellipsis.");
        Bounds textBounds = text.localToScene(text.getBoundsInLocal());
        Bounds cellBounds = cell.localToScene(cell.getBoundsInLocal());
        Bounds viewport = list.lookup(".clipped-container").localToScene(
                list.lookup(".clipped-container").getBoundsInLocal());
        assertTrue(textBounds.getMinX() >= viewport.getMinX() - 1
                && textBounds.getMaxX() <= viewport.getMaxX() + 1, textBounds.toString());
        assertTrue(textBounds.getHeight() <= cellBounds.getHeight() + 1,
                "The row must grow to fit all wrapped lines.");
        Text singleLine = new Text(expected);
        singleLine.setFont(text.getFont());
        if (singleLine.getLayoutBounds().getWidth() > cell.getWidth()) {
            assertTrue(textBounds.getHeight() > singleLine.getLayoutBounds().getHeight() * 1.5,
                    "Long rows must occupy multiple lines.");
        }
    }

    @Test
    public void panel_keepsLiveListAndRendersRecordedStatusAndMembership() throws Exception {
        runOnFxThread(() -> {
            var history = FXCollections.<AttendanceHistoryEntry>observableArrayList();
            var context = new SimpleStringProperty("Student S1 · attendance history");
            Region panel = new HistoryListPanel(history, context).getRoot();
            @SuppressWarnings("unchecked")
            ListView<AttendanceHistoryEntry> list = (ListView<AttendanceHistoryEntry>) panel.lookup("#historyListView");
            assertSame(history, list.getItems());
            assertEquals("Showing 0 attendance record(s).", ((Label) panel.lookup("#historySummary")).getText());
            Label placeholder = (Label) list.getPlaceholder();
            assertEquals("No recorded attendance. Missing dates are unrecorded.", placeholder.getText());
            var entry = new AttendanceHistoryEntryData(LessonQueryTestData.STUDENT_ONE,
                    LessonQueryTestData.SHARED_LESSON, LocalDate.of(2026, 10, 5),
                    AttendanceHistoryEntry.Status.PRESENT, true);
            history.add(entry);
            context.set("Student S1 · attendance history · L10");
            assertEquals("Showing 1 attendance record(s).", ((Label) panel.lookup("#historySummary")).getText());
            assertEquals(context.get(), ((Label) panel.lookup("#historyContext")).getText());
            var cell = new HistoryListPanel.HistoryCell();
            assertTrue(cell.isWrapText());
            cell.updateItem(entry, false);
            assertEquals("2026-10-05 · L10 · present · currently enrolled", cell.getText());
            cell.updateItem(new AttendanceHistoryEntryData(LessonQueryTestData.STUDENT_ONE,
                    LessonQueryTestData.SHARED_LESSON, LocalDate.of(2026, 10, 5),
                    AttendanceHistoryEntry.Status.ABSENT, false), false);
            assertEquals("2026-10-05 · L10 · absent · former enrolment", cell.getText());
            cell.updateItem(null, false);
            assertNull(cell.getText());
            cell.updateItem(null, true);
            assertNull(cell.getText());
            assertThrows(NullPointerException.class, () -> new HistoryListPanel(null, context));
            assertThrows(NullPointerException.class, () -> new HistoryListPanel(history, null));
        });
    }
}
