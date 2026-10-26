package ponhub.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
            var roster = List.of(rosterEntry(1, Optional.of(AttendanceStatus.PRESENT)),
                    rosterEntry(2, Optional.of(AttendanceStatus.ABSENT)), rosterEntry(3, Optional.empty()));
            var former = List.of(formerEntry(4, 0), formerEntry(5, 1));
            for (var date : List.of(Optional.of(SELECTED_DATE), Optional.<LocalDate>empty())) {
                var detail = detail(date, roster, former);
                Region panel = panel(detail, 420, 1600);
                ListView<?> view = detailList(panel);
                assertNotNull(view.getSelectionModel());
                assertTrue(view.isFocusTraversable());
                assertEquals(List.of("Current roster", "Former members' retained attendance"),
                        labelsInOrder(view, ".card_section_heading"));
                assertEquals(List.of(primary(1), primary(2), primary(3), primary(4), primary(5)),
                        labelsInOrder(view, ".card_entry_primary"));
                List<String> expectedSecondary = new ArrayList<>();
                if (date.isPresent()) {
                    expectedSecondary.addAll(List.of("Attendance: present", "Attendance: absent",
                            "Attendance: unrecorded"));
                }
                expectedSecondary.addAll(List.of(formerSecondary(former.get(0)), formerSecondary(former.get(1))));
                assertEquals(expectedSecondary, labelsInOrder(view, ".card_entry_secondary"));
                assertTrue(view.lookupAll(".card_empty_label").isEmpty());
                Region summary = new LessonCard(detail).getRoot();
                assertTrue(summary.lookupAll(".card_entry_primary").isEmpty());
                assertTrue(summary.lookupAll(".card_section_heading").isEmpty());
            }
        });
    }

    @Test
    public void detail_emptySections_preservesExplicitEmptyFeedback() throws Exception {
        runOnFxThread(() -> {
            for (var date : List.of(Optional.of(SELECTED_DATE), Optional.<LocalDate>empty())) {
                Region panel = panel(detail(date, List.of(), List.of()), 420, 900);
                ListView<?> view = detailList(panel);
                assertEquals(List.of("No students currently enrolled.",
                        "No retained attendance for former members."), labelsInOrder(view, ".card_empty_label"));
                assertTrue(view.lookupAll(".card_entry_primary").isEmpty());
                assertTrue(view.lookupAll(".card_entry_secondary").isEmpty());
            }
        });
    }

    @Test
    public void detail_tenThousandFormerRecords_rendersBoundedRowsAndScrollsToCompleteLastRecord() throws Exception {
        runOnFxThread(() -> {
            var records = new ArrayList<LessonDetailView.FormerAttendance>();
            for (int index = 0; index < LARGE_ROW_COUNT; index++) {
                records.add(formerEntry(1, index));
            }
            var detail = detail(Optional.empty(), List.of(), records);
            for (int width : new int[] {210, 420}) {
                Region panel = panel(detail, width, 320);
                ListView<?> view = detailList(panel);
                assertEquals(LARGE_ROW_COUNT + 4, view.getItems().size());
                assertTrue(view.lookupAll(".card_entry").size() < 50,
                        "Opening detail must instantiate visible rows instead of the complete history");
                assertEquals(1, catalogue(panel).getItems().size());
                assertTrue(catalogue(panel).lookupAll(".card_entry").isEmpty());
                assertFalse(catalogue(panel).isVisible());
                for (int index : new int[] {0, LARGE_ROW_COUNT / 2}) {
                    scrollTo(panel, view, index + 4);
                    assertEntry(view, primary(1), formerSecondary(records.get(index)));
                    assertWrapping(view, width);
                }
                scrollToEnd(panel, view);
                Node finalEntry = assertEntry(view, primary(1), formerSecondary(records.getLast()));
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
                roster.add(rosterEntry(index, Optional.of(AttendanceStatus.PRESENT)));
            }
            Region panel = panel(detail(Optional.of(SELECTED_DATE), roster, List.of()), 210, 320);
            ListView<?> view = detailList(panel);
            assertEquals(LARGE_ROW_COUNT + 4, view.getItems().size());
            assertTrue(view.lookupAll(".card_entry").size() < 50,
                    "Opening detail must instantiate visible rows instead of the complete current roster");
            for (int index : new int[] {1, LARGE_ROW_COUNT / 2, LARGE_ROW_COUNT}) {
                scrollTo(panel, view, index + 1);
                assertEntry(view, primary(index), "Attendance: present");
                assertWrapping(view, 210);
            }
            scrollToEnd(panel, view);
            assertTrue(labelsInOrder(view, ".card_empty_label")
                    .contains("No retained attendance for former members."));
            assertNoHorizontalScroll(view);
        });
    }

    @Test
    public void detail_keyboardNavigation_scrollsWithoutChangingTheResultProjection() throws Exception {
        runOnFxThread(() -> {
            var records = new ArrayList<LessonDetailView.FormerAttendance>();
            for (int index = 0; index < 1000; index++) {
                records.add(formerEntry(1, index));
            }
            var detail = detail(Optional.empty(), List.of(), records);
            var results = FXCollections.observableArrayList((LessonView) detail);
            Region panel = new LessonListPanel(results).getRoot();
            layout(panel, 210, 320);
            ListView<?> view = detailList(panel);
            assertTrue(view.isFocusTraversable());
            assertNotNull(view.getSelectionModel());

            pressKey(panel, view, KeyCode.END);
            assertEquals(view.getItems().size() - 1, view.getSelectionModel().getSelectedIndex());
            Node finalEntry = assertEntry(view, primary(1), formerSecondary(records.getLast()));
            assertEndVisible(view, finalEntry);

            pressKey(panel, view, KeyCode.HOME);
            assertEquals(0, view.getSelectionModel().getSelectedIndex());
            assertNotNull(view.lookup("#lessonCardPane"));
            double topPosition = verticalScrollBar(view).getValue();
            pressKey(panel, view, KeyCode.PAGE_DOWN);
            pressKey(panel, view, KeyCode.PAGE_DOWN);
            assertTrue(view.getSelectionModel().getSelectedIndex() > 0,
                    "Two Page Down presses must advance the active row");
            assertTrue(verticalScrollBar(view).getValue() > topPosition,
                    "Page Down must move the viewport beyond its initial position");

            int selectedBeforeDown = view.getSelectionModel().getSelectedIndex();
            double pagePosition = verticalScrollBar(view).getValue();
            for (int index = 0; index < 12; index++) {
                pressKey(panel, view, KeyCode.DOWN);
            }
            assertEquals(selectedBeforeDown + 12, view.getSelectionModel().getSelectedIndex());
            assertTrue(verticalScrollBar(view).getValue() > pagePosition,
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
            assertSame(results, catalogue(panel).getItems());
            assertEquals(List.of(detail), results);
            assertSame(detail, results.getFirst());
        });
    }

    @Test
    public void panel_refreshAndCatalogueTransitions_replacesRowsAndPreservesListIdentity() throws Exception {
        runOnFxThread(() -> {
            var originalRoster = List.of(rosterEntry(1, Optional.of(AttendanceStatus.ABSENT)));
            var originalRecords = List.of(formerEntry(2, 0));
            var original = detail(Optional.of(SELECTED_DATE), originalRoster, originalRecords);
            var results = FXCollections.observableArrayList((LessonView) original);
            Region panel = new LessonListPanel(results).getRoot();
            layout(panel, 420, 1200);
            ListView<?> view = detailList(panel);
            assertEntry(view, primary(1), "Attendance: absent");
            assertEntry(view, primary(2), formerSecondary(originalRecords.getFirst()));

            var refreshed = detail(Optional.of(SELECTED_DATE),
                    List.of(rosterEntry(3, Optional.empty())), List.of());
            results.setAll(refreshed);
            settleLayout(panel, view);
            assertEntry(view, primary(3), "Attendance: unrecorded");
            assertFalse(labelsInOrder(view, ".card_entry_primary").contains(primary(1)));
            assertFalse(labelsInOrder(view, ".card_entry_primary").contains(primary(2)));
            assertEquals(originalRoster, original.currentRoster());
            assertEquals(originalRecords, original.formerAttendance());

            results.setAll(new LessonQueries(createData()).getLessonList());
            settleLayout(panel, view);
            assertFalse(view.isVisible());
            assertTrue(view.getItems().isEmpty());
            assertTrue(catalogue(panel).isVisible());
            assertSame(results, catalogue(panel).getItems());
            assertEquals(5, catalogue(panel).getItems().size());
            var catalogueResults = List.copyOf(results);
            pressKey(panel, view, KeyCode.END);
            assertTrue(view.getItems().isEmpty());
            assertSame(results, catalogue(panel).getItems());
            assertEquals(catalogueResults, results);
            results.clear();
            settleLayout(panel, catalogue(panel));
            assertSame(results, catalogue(panel).getItems());
            assertEquals("No lessons to display.", ((Label) catalogue(panel).getPlaceholder()).getText());
            pressKey(panel, view, KeyCode.END);
            assertTrue(view.getItems().isEmpty());
            assertTrue(results.isEmpty());
            assertSame(results, catalogue(panel).getItems());

            results.setAll(original);
            settleLayout(panel, view);
            assertTrue(view.isVisible());
            assertSame(results, catalogue(panel).getItems());
            assertEquals(1, catalogue(panel).getItems().size());
            assertEntry(view, primary(1), "Attendance: absent");
            assertEntry(view, primary(2), formerSecondary(originalRecords.getFirst()));
            assertFalse(labelsInOrder(view, ".card_entry_primary").contains(primary(3)));
        });
    }

    @Test
    public void detail_endThenWheelBeforeLayout_preservesNewerViewportPosition() throws Exception {
        runOnFxThread(() -> {
            Region panel = largeHistoryPanel();
            ListView<?> view = detailList(panel);
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
            Region panel = largeHistoryPanel();
            ListView<?> view = detailList(panel);
            fireKey(view, KeyCode.END);
            ScrollBar bar = verticalScrollBar(view);
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

    private static Region largeHistoryPanel() {
        var records = new ArrayList<LessonDetailView.FormerAttendance>();
        for (int index = 0; index < 1000; index++) {
            records.add(formerEntry(1, index));
        }
        return panel(detail(Optional.empty(), List.of(), records), 210, 320);
    }

    private static void assertInterruptedViewport(Region panel, ListView<?> view) {
        double interruptedPosition = verticalScrollBar(view).getValue();
        assertTrue(interruptedPosition > 0 && interruptedPosition < 1,
                "New input must move the viewport away from End before the correction layout: "
                        + interruptedPosition);
        settleLayout(panel, view);
        assertTrue(verticalScrollBar(view).getValue() < 1,
                "The deferred End correction must not override the newer viewport position");
        assertEquals(view.getItems().size() - 1, view.getSelectionModel().getSelectedIndex(),
                "Scrolling the viewport must not replace the keyboard-selected row");
        assertEquals(1004, view.getItems().size(), "Interruption must retain every history row");
        assertFalse(labelsInOrder(view, ".card_entry_secondary").contains(formerSecondary(formerEntry(1, 999))),
                "The viewport must stay away from the final retained record");
    }

    private static LessonDetailView detail(Optional<LocalDate> date,
            List<LessonDetailView.RosterStatus> roster, List<LessonDetailView.FormerAttendance> records) {
        var lesson = createData().getLesson(new LessonId("L10")).orElseThrow();
        return new LessonDetailView(lesson, "Mei Lim", date, roster, records);
    }

    private static LessonDetailView.RosterStatus rosterEntry(int studentId, Optional<AttendanceStatus> status) {
        return new LessonDetailView.RosterStatus(student(studentId), status);
    }

    private static LessonDetailView.FormerAttendance formerEntry(int studentId, int week) {
        return new LessonDetailView.FormerAttendance(student(studentId), SELECTED_DATE.minusWeeks(week),
                week % 2 == 0 ? AttendanceStatus.ABSENT : AttendanceStatus.PRESENT);
    }

    private static Student student(int studentId) {
        return new Student(new PersonId("S" + studentId),
                new ContactDetails(new Name("Alexandra Catherine Montgomery Wellington")),
                new EducationLevel("S2"), new Phone("91234567"));
    }

    private static String primary(int studentId) {
        return "S" + studentId + " · Alexandra Catherine Montgomery Wellington";
    }

    private static String formerSecondary(LessonDetailView.FormerAttendance record) {
        String status = record.status() == AttendanceStatus.ABSENT ? "absent" : "present";
        return "Date: " + record.date() + " · Status: " + status;
    }

    private static Region panel(LessonDetailView detail, int width, int height) {
        Region panel = new LessonListPanel(FXCollections.observableArrayList((LessonView) detail)).getRoot();
        layout(panel, width, height);
        return panel;
    }

    private static void layout(Region panel, int width, int height) {
        Scene scene = new Scene(panel, width, height);
        scene.getStylesheets().add(MainApp.class.getResource("/view/DarkTheme.css").toExternalForm());
        panel.resize(width, height);
        settleLayout(panel, detailList(panel));
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

    private static ScrollBar verticalScrollBar(ListView<?> view) {
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

    private static List<String> labelsInOrder(ListView<?> view, String selector) {
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

    private static ListView<?> detailList(Region panel) {
        return (ListView<?>) panel.lookup("#lessonDetailList");
    }

    private static ListView<?> catalogue(Region panel) {
        return (ListView<?>) panel.lookup("#lessonListView");
    }
}
