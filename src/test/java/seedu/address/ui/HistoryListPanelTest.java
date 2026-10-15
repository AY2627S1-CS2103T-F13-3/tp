package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.FxTestUtil.runOnFxThread;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import seedu.address.model.query.AttendanceHistoryEntry;
import seedu.address.model.query.AttendanceHistoryEntryData;
import seedu.address.testutil.LessonQueryTestData;

public class HistoryListPanelTest {
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
