package ponhub.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.FxTestUtil.runOnFxThread;

import java.time.LocalDate;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.Region;
import ponhub.MainApp;
import ponhub.model.lesson.LessonId;
import ponhub.model.query.AttendanceHistoryEntry;
import ponhub.model.query.AttendanceHistoryEntryData;
import ponhub.testutil.LessonQueryTestData;

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
            scene.getStylesheets().add(MainApp.class.getResource("/view/DarkTheme.css").toExternalForm());
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
                assertWrappedCard(list, history.getFirst());
                list.scrollTo(59);
                layout(panel);
                assertWrappedCard(list, history.getLast());
                list.scrollTo(0);
                layout(panel);
            }
            // Refresh and clear the same live list after VirtualFlow has reused its cells.
            history.setAll(new AttendanceHistoryEntryData(LessonQueryTestData.STUDENT_ONE,
                    LessonQueryTestData.SHARED_LESSON, LocalDate.of(2026, 10, 5),
                    AttendanceHistoryEntry.Status.ABSENT, false));
            list.scrollTo(0);
            layout(panel);
            assertWrappedCard(list, history.getFirst());
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

    private static void assertWrappedCard(ListView<?> list, AttendanceHistoryEntry entry) {
        ListCell<?> cell = list.lookupAll(".list-cell").stream().map(ListCell.class::cast)
                .filter(candidate -> entry.equals(candidate.getItem())).findFirst().orElseThrow();
        assertNull(cell.getText());
        Region card = (Region) cell.getGraphic();
        assertEquals(entry.getDate().toString(), label(card, "attendanceDate").getText());
        assertEquals("Student " + entry.getStudentId() + " · Lesson " + entry.getLessonId(),
                label(card, "attendanceIdentity").getText());
        assertEquals(entry.getStatus().name().toLowerCase(java.util.Locale.ROOT),
                label(card, "attendanceStatus").getText());
        assertEquals(entry.isCurrentlyEnrolled() ? "currently enrolled" : "former enrolment",
                label(card, "attendanceMembership").getText());
        for (var node : card.lookupAll(".label")) {
            Label cardLabel = (Label) node;
            assertTrue(cardLabel.isWrapText(), cardLabel.getText());
            assertTrue(cardLabel.getWidth() > 0 && cardLabel.getWidth() <= list.getWidth(), cardLabel.getText());
            assertTrue(cardLabel.getHeight() + 1 >= cardLabel.prefHeight(cardLabel.getWidth()), cardLabel.getText());
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
            cell.updateItem(entry, false);
            Region previous = (Region) cell.getGraphic();
            assertTrue(previous.prefWidthProperty().isBound());
            assertEquals("2026-10-05", label(previous, "attendanceDate").getText());
            assertEquals("Student S1 · Lesson L10", label(previous, "attendanceIdentity").getText());
            assertEquals("present", label(previous, "attendanceStatus").getText());
            assertEquals("currently enrolled", label(previous, "attendanceMembership").getText());
            var former = new AttendanceHistoryEntryData(LessonQueryTestData.STUDENT_ONE,
                    LessonQueryTestData.SHARED_LESSON, LocalDate.of(2026, 10, 5),
                    AttendanceHistoryEntry.Status.ABSENT, false);
            cell.updateItem(former, false);
            assertFalse(previous.prefWidthProperty().isBound());
            Region replacement = (Region) cell.getGraphic();
            assertEquals("absent", label(replacement, "attendanceStatus").getText());
            assertEquals("former enrolment", label(replacement, "attendanceMembership").getText());
            cell.updateItem(null, false);
            assertNull(cell.getText());
            assertNull(cell.getGraphic());
            cell.updateItem(null, true);
            assertNull(cell.getText());
            assertNull(cell.getGraphic());
            assertThrows(NullPointerException.class, () -> new AttendanceHistoryCard(null));
            assertThrows(NullPointerException.class, () -> new HistoryListPanel(null, context));
            assertThrows(NullPointerException.class, () -> new HistoryListPanel(history, null));
        });
    }

    private static Label label(Region root, String id) {
        return (Label) root.lookup("#" + id);
    }
}
