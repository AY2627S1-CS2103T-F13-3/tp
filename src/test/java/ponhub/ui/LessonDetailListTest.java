package ponhub.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.FxTestUtil.runOnFxThread;
import static ponhub.testutil.LessonQueryTestData.createData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import ponhub.MainApp;
import ponhub.model.attendance.AttendanceStatus;
import ponhub.model.lesson.LessonId;
import ponhub.model.person.ContactDetails;
import ponhub.model.person.EducationLevel;
import ponhub.model.person.Name;
import ponhub.model.person.PersonId;
import ponhub.model.person.Phone;
import ponhub.model.person.Student;
import ponhub.model.query.LessonDetailView;
import ponhub.model.query.LessonQueries;
import ponhub.model.query.LessonView;

/**
 * Checks that lesson details render bounded visible rows while retaining complete, wrapping roster/history data.
 */
public class LessonDetailListTest {
    private static final LocalDate SELECTED_DATE = LocalDate.of(2026, 10, 5);
    private static final int LARGE_ROW_COUNT = 10_000;

    @Test
    public void detail_datedAndUndated_preservesLabelsStatusesAndOrder() throws Exception {
        runOnFxThread(() -> {
            var roster = List.of(createRosterEntry(1, Optional.of(AttendanceStatus.PRESENT)),
                    createRosterEntry(2, Optional.of(AttendanceStatus.ABSENT)), createRosterEntry(3, Optional.empty()));
            var former = List.of(createFormerEntry(4, 0), createFormerEntry(5, 1));
            for (var date : List.of(Optional.of(SELECTED_DATE), Optional.<LocalDate>empty())) {
                var detail = createDetail(date, roster, former);
                Region panel = createPanel(detail, 420, 1600);
                ListView<?> view = getDetailList(panel);
                assertNotNull(view.getSelectionModel());
                assertTrue(view.isFocusTraversable());
                assertEquals(List.of("Current roster", "Former members' retained attendance"),
                        getLabelsInOrder(view, ".card_section_heading"));
                assertEquals(List.of(getPrimaryText(1), getPrimaryText(2), getPrimaryText(3), getPrimaryText(4),
                        getPrimaryText(5)),
                        getLabelsInOrder(view, ".card_entry_primary"));
                List<String> expectedSecondary = new ArrayList<>();
                if (date.isPresent()) {
                    expectedSecondary.addAll(List.of("Attendance: present", "Attendance: absent",
                            "Attendance: unrecorded"));
                }
                expectedSecondary.addAll(List.of(getFormerSecondaryText(former.get(0)),
                        getFormerSecondaryText(former.get(1))));
                assertEquals(expectedSecondary, getLabelsInOrder(view, ".card_entry_secondary"));
                assertTrue(view.lookupAll(".card_empty_label").isEmpty());
                Region summary = new LessonCard(detail).getRoot();
                assertTrue(summary.lookupAll(".card_entry_primary").isEmpty());
                assertTrue(summary.lookupAll(".card_section_heading").isEmpty());
            }
        });
    }

    @Test
    public void detail_historicalContext_labelsCurrentInformationAndClearsItForPlainRoster() throws Exception {
        runOnFxThread(() -> {
            Region panel = createPanel(createDetail(Optional.of(SELECTED_DATE), List.of(), List.of()), 210, 900);
            LessonDetailList view = (LessonDetailList) getDetailList(panel);
            String note = "Current Lesson information";
            assertEquals(List.of(note), getLabelsInOrder(view, ".card_context_label"));
            assertWrapping(view, 210);
            assertNoHorizontalScroll(view);

            view.showDetail(createDetail(Optional.empty(), List.of(), List.of(createFormerEntry(1, 0))));
            settleLayout(panel, view);
            assertEquals(List.of(note), getLabelsInOrder(view, ".card_context_label"));
            assertWrapping(view, 210);
            assertNoHorizontalScroll(view);

            view.showDetail(createDetail(Optional.empty(), List.of(createRosterEntry(1, Optional.empty())), List.of()));
            settleLayout(panel, view);
            assertTrue(getLabelsInOrder(view, ".card_context_label").isEmpty());
            assertEquals(List.of(getPrimaryText(1)), getLabelsInOrder(view, ".card_entry_primary"));
        });
    }

    @Test
    public void detail_emptySections_preservesExplicitEmptyFeedback() throws Exception {
        runOnFxThread(() -> {
            for (var date : List.of(Optional.of(SELECTED_DATE), Optional.<LocalDate>empty())) {
                Region panel = createPanel(createDetail(date, List.of(), List.of()), 420, 900);
                ListView<?> view = getDetailList(panel);
                assertEquals(List.of("No students currently enrolled.",
                        "No retained attendance for former members."), getLabelsInOrder(view, ".card_empty_label"));
                assertTrue(view.lookupAll(".card_entry_primary").isEmpty());
                assertTrue(view.lookupAll(".card_entry_secondary").isEmpty());
            }
        });
    }

    @Test
    public void detail_nullListItem_clearsRecycledCellAndRestoresNextSnapshot() throws Exception {
        runOnFxThread(() -> {
            var original = createDetail(Optional.of(SELECTED_DATE),
                    List.of(createRosterEntry(1, Optional.of(AttendanceStatus.PRESENT))), List.of());
            Region panel = createPanel(original, 420, 900);
            LessonDetailList view = (LessonDetailList) getDetailList(panel);
            ListCell<?> entryCell = findCellAt(view, 2);
            Region oldRow = (Region) entryCell.getGraphic();
            assertNotNull(oldRow);
            assertTrue(oldRow.prefWidthProperty().isBound());

            // The inherited ListView API permits null items, which are distinct from empty cells.
            view.getItems().set(2, null);
            settleLayout(panel, view);
            ListCell<?> nullCell = findCellAt(view, 2);
            assertFalse(nullCell.isEmpty());
            assertNull(nullCell.getItem());
            assertNull(nullCell.getText());
            assertNull(nullCell.getGraphic(), "A null item must not retain the previous student's graphic");
            assertFalse(oldRow.prefWidthProperty().isBound(), "Recycled content must release its width binding");

            var refreshed = createDetail(Optional.of(SELECTED_DATE),
                    List.of(createRosterEntry(2, Optional.of(AttendanceStatus.ABSENT))), List.of());
            view.showDetail(refreshed);
            settleLayout(panel, view);
            assertEntry(view, getPrimaryText(2), "Attendance: absent");
            assertFalse(getLabelsInOrder(view, ".card_entry_primary").contains(getPrimaryText(1)));
            assertWrapping(view, 420);
        });
    }

    @Test
    public void detail_tenThousandFormerRecords_rendersBoundedRowsAndScrollsToCompleteLastRecord() throws Exception {
        runOnFxThread(() -> {
            var records = new ArrayList<LessonDetailView.FormerAttendance>();
            for (int index = 0; index < LARGE_ROW_COUNT; index++) {
                records.add(createFormerEntry(1, index));
            }
            var detail = createDetail(Optional.empty(), List.of(), records);
            for (int width : new int[] {210, 420}) {
                Region panel = createPanel(detail, width, 320);
                ListView<?> view = getDetailList(panel);
                assertEquals(LARGE_ROW_COUNT + 4, view.getItems().size());
                assertTrue(view.lookupAll(".card_entry").size() < 50,
                        "Opening detail must instantiate visible rows instead of the complete history");
                assertEquals(1, getCatalogue(panel).getItems().size());
                assertTrue(getCatalogue(panel).lookupAll(".card_entry").isEmpty());
                assertFalse(getCatalogue(panel).isVisible());
                for (int index : new int[] {0, LARGE_ROW_COUNT / 2}) {
                    scrollTo(panel, view, index + 4);
                    assertEntry(view, getPrimaryText(1), getFormerSecondaryText(records.get(index)));
                    assertWrapping(view, width);
                }
                scrollToEnd(panel, view);
                Node finalEntry = assertEntry(view, getPrimaryText(1), getFormerSecondaryText(records.getLast()));
                assertEndVisible(view, finalEntry);
                assertNoHorizontalScroll(view);
                assertTrue(view.lookupAll(".card_entry").size() < 50);
            }
        });
    }

    @Test
    public void detail_tenThousandCurrentStudents_rendersBoundedRowsAndKeepsLastStudentReachable() throws Exception {
        runOnFxThread(() -> {
            var roster = new ArrayList<LessonDetailView.RosterStatus>();
            for (int index = 1; index <= LARGE_ROW_COUNT; index++) {
                roster.add(createRosterEntry(index, Optional.of(AttendanceStatus.PRESENT)));
            }
            Region panel = createPanel(createDetail(Optional.of(SELECTED_DATE), roster, List.of()), 210, 320);
            ListView<?> view = getDetailList(panel);
            assertEquals(LARGE_ROW_COUNT + 4, view.getItems().size());
            assertTrue(view.lookupAll(".card_entry").size() < 50,
                    "Opening detail must instantiate visible rows instead of the complete current roster");
            for (int index : new int[] {1, LARGE_ROW_COUNT / 2, LARGE_ROW_COUNT}) {
                scrollTo(panel, view, index + 1);
                assertEntry(view, getPrimaryText(index), "Attendance: present");
                assertWrapping(view, 210);
            }
            scrollToEnd(panel, view);
            assertTrue(getLabelsInOrder(view, ".card_empty_label")
                    .contains("No retained attendance for former members."));
            assertNoHorizontalScroll(view);
        });
    }

    @Test
    public void detail_keyboardNavigation_scrollsWithoutChangingTheResultProjection() throws Exception {
        runOnFxThread(() -> {
            var records = new ArrayList<LessonDetailView.FormerAttendance>();
            for (int index = 0; index < 1000; index++) {
                records.add(createFormerEntry(1, index));
            }
            var detail = createDetail(Optional.empty(), List.of(), records);
            var results = FXCollections.observableArrayList((LessonView) detail);
            Region panel = new LessonListPanel(results).getRoot();
            layout(panel, 210, 320);
            ListView<?> view = getDetailList(panel);
            assertTrue(view.isFocusTraversable());
            assertNotNull(view.getSelectionModel());

            pressKey(panel, view, KeyCode.END);
            assertEquals(view.getItems().size() - 1, view.getSelectionModel().getSelectedIndex());
            Node finalEntry = assertEntry(view, getPrimaryText(1), getFormerSecondaryText(records.getLast()));
            assertEndVisible(view, finalEntry);

            pressKey(panel, view, KeyCode.HOME);
            assertEquals(0, view.getSelectionModel().getSelectedIndex());
            assertNotNull(view.lookup("#lessonCardPane"));
            double topPosition = findVerticalScrollBar(view).getValue();
            pressKey(panel, view, KeyCode.PAGE_DOWN);
            pressKey(panel, view, KeyCode.PAGE_DOWN);
            assertTrue(view.getSelectionModel().getSelectedIndex() > 0,
                    "Two Page Down presses must advance the active row");
            assertTrue(findVerticalScrollBar(view).getValue() > topPosition,
                    "Page Down must move the viewport beyond its initial position");

            int selectedBeforeDown = view.getSelectionModel().getSelectedIndex();
            double pagePosition = findVerticalScrollBar(view).getValue();
            for (int index = 0; index < 12; index++) {
                pressKey(panel, view, KeyCode.DOWN);
            }
            assertEquals(selectedBeforeDown + 12, view.getSelectionModel().getSelectedIndex());
            assertTrue(findVerticalScrollBar(view).getValue() > pagePosition,
                    "Arrow navigation must keep advancing the viewport after moving beyond the visible rows");
            fireKey(view, KeyCode.END);
            fireKey(view, KeyCode.HOME);
            settleLayout(panel, view);
            assertEquals(0, view.getSelectionModel().getSelectedIndex());
            Node summary = view.lookup("#lessonCardPane");
            assertNotNull(summary, "Home after End must leave the lesson summary reachable at the top");
            var summaryBounds = summary.localToScene(summary.getBoundsInLocal());
            var viewportBounds = view.localToScene(view.getBoundsInLocal());
            assertTrue(summaryBounds.getMinY() >= viewportBounds.getMinY()
                            && summaryBounds.getMinY() <= viewportBounds.getMinY() + 2,
                    "Home before the next layout must keep the viewport at the summary");
            assertSame(results, getCatalogue(panel).getItems());
            assertEquals(List.of(detail), results);
            assertSame(detail, results.getFirst());
        });
    }

    @Test
    public void panel_refreshAndCatalogueTransitions_replacesRowsAndPreservesListIdentity() throws Exception {
        runOnFxThread(() -> {
            var originalRoster = List.of(createRosterEntry(1, Optional.of(AttendanceStatus.ABSENT)));
            var originalRecords = List.of(createFormerEntry(2, 0));
            var original = createDetail(Optional.of(SELECTED_DATE), originalRoster, originalRecords);
            var results = FXCollections.observableArrayList((LessonView) original);
            Region panel = new LessonListPanel(results).getRoot();
            layout(panel, 420, 1200);
            ListView<?> view = getDetailList(panel);
            assertEntry(view, getPrimaryText(1), "Attendance: absent");
            assertEntry(view, getPrimaryText(2), getFormerSecondaryText(originalRecords.getFirst()));

            var refreshed = createDetail(Optional.of(SELECTED_DATE),
                    List.of(createRosterEntry(3, Optional.empty())), List.of());
            results.setAll(refreshed);
            settleLayout(panel, view);
            assertEntry(view, getPrimaryText(3), "Attendance: unrecorded");
            assertFalse(getLabelsInOrder(view, ".card_entry_primary").contains(getPrimaryText(1)));
            assertFalse(getLabelsInOrder(view, ".card_entry_primary").contains(getPrimaryText(2)));
            assertEquals(originalRoster, original.currentRoster());
            assertEquals(originalRecords, original.formerAttendance());

            results.setAll(new LessonQueries(createData()).getLessonList());
            settleLayout(panel, view);
            assertFalse(view.isVisible());
            assertTrue(view.getItems().isEmpty());
            assertTrue(getCatalogue(panel).isVisible());
            assertSame(results, getCatalogue(panel).getItems());
            assertEquals(5, getCatalogue(panel).getItems().size());
            var catalogueResults = List.copyOf(results);
            pressKey(panel, view, KeyCode.END);
            assertTrue(view.getItems().isEmpty());
            assertSame(results, getCatalogue(panel).getItems());
            assertEquals(catalogueResults, results);
            results.clear();
            settleLayout(panel, getCatalogue(panel));
            assertSame(results, getCatalogue(panel).getItems());
            assertEquals("No lessons to display.", ((Label) getCatalogue(panel).getPlaceholder()).getText());
            pressKey(panel, view, KeyCode.END);
            assertTrue(view.getItems().isEmpty());
            assertTrue(results.isEmpty());
            assertSame(results, getCatalogue(panel).getItems());

            results.setAll(original);
            settleLayout(panel, view);
            assertTrue(view.isVisible());
            assertSame(results, getCatalogue(panel).getItems());
            assertEquals(1, getCatalogue(panel).getItems().size());
            assertEntry(view, getPrimaryText(1), "Attendance: absent");
            assertEntry(view, getPrimaryText(2), getFormerSecondaryText(originalRecords.getFirst()));
            assertFalse(getLabelsInOrder(view, ".card_entry_primary").contains(getPrimaryText(3)));
        });
    }

    @Test
    public void detail_endThenWheelBeforeLayout_preservesNewerViewportPosition() throws Exception {
        runOnFxThread(() -> {
            Region panel = createLargeHistoryPanel();
            ListView<?> view = getDetailList(panel);
            fireKey(view, KeyCode.END);
            Event.fireEvent(view.lookup(".virtual-flow"), new ScrollEvent(ScrollEvent.SCROLL,
                    20, 20, 20, 20, false, false, false, false, false, false,
                    0, 600, 0, 600, ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                    ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0, null));
            assertInterruptedViewport(panel, view);
        });
    }

    @Test
    public void detail_endThenScrollbarBeforeLayout_preservesNewerViewportPosition() throws Exception {
        runOnFxThread(() -> {
            Region panel = createLargeHistoryPanel();
            ListView<?> view = getDetailList(panel);
            fireKey(view, KeyCode.END);
            ScrollBar bar = findVerticalScrollBar(view);
            Node thumb = bar.lookup(".thumb");
            Event.fireEvent(thumb, new MouseEvent(MouseEvent.MOUSE_PRESSED,
                    2, 2, 2, 2, MouseButton.PRIMARY, 1, false, false, false, false,
                    true, false, false, false, false, false, null));
            bar.setValue((bar.getMin() + bar.getMax()) / 2);
            Event.fireEvent(thumb, new MouseEvent(MouseEvent.MOUSE_RELEASED,
                    2, 2, 2, 2, MouseButton.PRIMARY, 1, false, false, false, false,
                    false, false, false, false, false, false, null));
            assertInterruptedViewport(panel, view);
        });
    }

    private static Region createLargeHistoryPanel() {
        var records = new ArrayList<LessonDetailView.FormerAttendance>();
        for (int index = 0; index < 1000; index++) {
            records.add(createFormerEntry(1, index));
        }
        return createPanel(createDetail(Optional.empty(), List.of(), records), 210, 320);
    }

    private static void assertInterruptedViewport(Region panel, ListView<?> view) {
        double interruptedPosition = findVerticalScrollBar(view).getValue();
        assertTrue(interruptedPosition > 0 && interruptedPosition < 1,
                "New input must move the viewport away from End before the correction layout: "
                        + interruptedPosition);
        settleLayout(panel, view);
        assertTrue(findVerticalScrollBar(view).getValue() < 1,
                "The deferred End correction must not override the newer viewport position");
        assertEquals(view.getItems().size() - 1, view.getSelectionModel().getSelectedIndex(),
                "Scrolling the viewport must not replace the keyboard-selected row");
        assertEquals(1004, view.getItems().size(), "Interruption must retain every history row");
        assertFalse(getLabelsInOrder(view, ".card_entry_secondary")
                .contains(getFormerSecondaryText(createFormerEntry(1, 999))),
                "The viewport must stay away from the final retained record");
    }

    private static LessonDetailView createDetail(Optional<LocalDate> date,
            List<LessonDetailView.RosterStatus> roster, List<LessonDetailView.FormerAttendance> records) {
        var lesson = createData().getLesson(new LessonId("L10")).orElseThrow();
        return new LessonDetailView(lesson, "Mei Lim", date, roster, records);
    }

    private static LessonDetailView.RosterStatus createRosterEntry(int studentId, Optional<AttendanceStatus> status) {
        return new LessonDetailView.RosterStatus(createStudent(studentId), status);
    }

    private static LessonDetailView.FormerAttendance createFormerEntry(int studentId, int week) {
        return new LessonDetailView.FormerAttendance(createStudent(studentId), SELECTED_DATE.minusWeeks(week),
                week % 2 == 0 ? AttendanceStatus.ABSENT : AttendanceStatus.PRESENT);
    }

    private static Student createStudent(int studentId) {
        return new Student(new PersonId("S" + studentId),
                new ContactDetails(new Name("Alexandra Catherine Montgomery Wellington")),
                new EducationLevel("S2"), new Phone("91234567"));
    }

    private static String getPrimaryText(int studentId) {
        return "S" + studentId + " · Alexandra Catherine Montgomery Wellington";
    }

    private static String getFormerSecondaryText(LessonDetailView.FormerAttendance record) {
        String status = record.status() == AttendanceStatus.ABSENT ? "absent" : "present";
        return "Date: " + record.date() + " · Status: " + status;
    }

    private static Region createPanel(LessonDetailView detail, int width, int height) {
        Region panel = new LessonListPanel(FXCollections.observableArrayList((LessonView) detail)).getRoot();
        layout(panel, width, height);
        return panel;
    }

    private static void layout(Region panel, int width, int height) {
        Scene scene = new Scene(panel, width, height);
        scene.getStylesheets().add(MainApp.class.getResource("/view/DarkTheme.css").toExternalForm());
        panel.resize(width, height);
        settleLayout(panel, getDetailList(panel));
    }

    private static void settleLayout(Region panel, ListView<?> view) {
        for (int pass = 0; pass < 4; pass++) {
            panel.applyCss();
            panel.layout();
            view.layout();
        }
    }

    private static void pressKey(Region panel, ListView<?> view, KeyCode code) {
        fireKey(view, code);
        settleLayout(panel, view);
    }

    private static void fireKey(ListView<?> view, KeyCode code) {
        Event.fireEvent(view, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code,
                false, false, false, false));
    }

    private static ScrollBar findVerticalScrollBar(ListView<?> view) {
        return view.lookupAll(".scroll-bar").stream().map(ScrollBar.class::cast)
                .filter(bar -> bar.getOrientation() == Orientation.VERTICAL && bar.isVisible())
                .findFirst().orElseThrow();
    }

    private static void scrollTo(Region panel, ListView<?> view, int row) {
        view.scrollTo(row);
        settleLayout(panel, view);
    }

    private static void scrollToEnd(Region panel, ListView<?> view) {
        scrollTo(panel, view, view.getItems().size() - 1);
        for (int pass = 0; pass < 12; pass++) {
            view.lookupAll(".scroll-bar").stream().map(ScrollBar.class::cast)
                    .filter(bar -> bar.getOrientation() == Orientation.VERTICAL && bar.isVisible())
                    .forEach(bar -> bar.setValue(bar.getMax()));
            settleLayout(panel, view);
        }
    }

    private static Node assertEntry(ListView<?> view, String primary, String secondary) {
        return view.lookupAll(".card_entry").stream().filter(entry -> {
            Label primaryLabel = (Label) entry.lookup(".card_entry_primary");
            Label secondaryLabel = (Label) entry.lookup(".card_entry_secondary");
            return primaryLabel != null && secondaryLabel != null
                    && primary.equals(primaryLabel.getText()) && secondary.equals(secondaryLabel.getText());
        }).findFirst().orElseThrow(() -> new AssertionError("Expected visible record: " + primary + "; " + secondary));
    }

    private static List<String> getLabelsInOrder(ListView<?> view, String selector) {
        return view.lookupAll(selector).stream().map(Label.class::cast)
                .sorted((left, right) -> Double.compare(left.localToScene(left.getBoundsInLocal()).getMinY(),
                        right.localToScene(right.getBoundsInLocal()).getMinY()))
                .map(Label::getText).collect(Collectors.toList());
    }

    private static void assertWrapping(ListView<?> view, int width) {
        for (Node entry : view.lookupAll(".card_entry")) {
            for (Node labelNode : entry.lookupAll(".label")) {
                Label label = (Label) labelNode;
                assertTrue(label.isWrapText(), label.getText());
                assertTrue(label.getWidth() > 0 && label.getWidth() <= width,
                        label.getText() + " width=" + label.getWidth());
                assertTrue(label.getHeight() + 1 >= label.prefHeight(label.getWidth()), label.getText());
            }
        }
    }

    private static void assertEndVisible(ListView<?> view, Node entry) {
        var viewportBounds = view.localToScene(view.getBoundsInLocal());
        var entryBounds = entry.localToScene(entry.getBoundsInLocal());
        var scrollBars = view.lookupAll(".scroll-bar").stream().map(ScrollBar.class::cast)
                .map(bar -> bar.getOrientation() + "=" + bar.getValue() + "/" + bar.getMax())
                .collect(Collectors.toList());
        assertTrue(entryBounds.getMaxY() >= viewportBounds.getMinY()
                        && entryBounds.getMaxY() <= viewportBounds.getMaxY() + 1,
                "Scrolling to the last record must expose its end: " + entryBounds + "; viewport=" + viewportBounds
                        + "; scrollbars=" + scrollBars);
    }

    private static void assertNoHorizontalScroll(ListView<?> view) {
        assertFalse(view.lookupAll(".scroll-bar").stream().map(ScrollBar.class::cast)
                .anyMatch(bar -> bar.getOrientation() == Orientation.HORIZONTAL && bar.isVisible()));
    }

    private static ListView<?> getDetailList(Region panel) {
        return (ListView<?>) panel.lookup("#lessonDetailList");
    }

    private static ListCell<?> findCellAt(ListView<?> view, int index) {
        return view.lookupAll(".list-cell").stream().map(node -> (ListCell<?>) node)
                .filter(cell -> cell.getIndex() == index).findFirst().orElseThrow();
    }

    private static ListView<?> getCatalogue(Region panel) {
        return (ListView<?>) panel.lookup("#lessonListView");
    }
}
