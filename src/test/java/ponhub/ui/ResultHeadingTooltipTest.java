package ponhub.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.FxTestUtil.runOnFxThread;
import static ponhub.testutil.LessonQueryTestData.createData;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import ponhub.model.lesson.LessonId;
import ponhub.model.query.AttendanceHistoryEntry;
import ponhub.model.query.LessonDetailView;
import ponhub.model.query.LessonQueries;
import ponhub.model.query.LessonView;

/**
 * Checks live result text and the actual tooltip skin at compact and normal panel widths.
 * These JavaFX rendering checks do not establish native hover or display-scaling acceptance.
 */
public class ResultHeadingTooltipTest {
    @Test
    public void lessonHeadings_followQueryCountAndDetailChanges() throws Exception {
        runOnFxThread(() -> {
            var context = new SimpleStringProperty("All lessons");
            var results = FXCollections.<LessonView>observableArrayList();
            Region panel = new LessonListPanel(results, context).getRoot();
            assertTooltip(panel, "lessonContext", "All lessons");
            assertTooltip(panel, "lessonSummary", "Showing 0 lesson(s).");

            var queries = new LessonQueries(createData());
            results.setAll(queries.getLessonList());
            context.set("Lesson search: sn/Alex tu/Mei s/Math d/Mon st/0900 et/1000 rm/R1");
            assertTooltip(panel, "lessonContext", context.get());
            assertTooltip(panel, "lessonSummary", "Showing 5 lesson(s).");

            var lesson = createData().getLesson(new LessonId("L10")).orElseThrow();
            results.setAll(new LessonDetailView(lesson, "Mei Lim", Optional.empty(), List.of(), List.of()));
            context.set("Lesson L10 · 2026-10-05");
            assertTooltip(panel, "lessonSummary", "Showing lesson details.");
            assertTooltip(panel, "lessonContext", context.get());
            context.set("Student S2 · current lessons");
            results.clear();
            assertTooltip(panel, "lessonContext", context.get());
            assertTooltip(panel, "lessonSummary", "Showing 0 lesson(s).");
        });
    }

    @Test
    public void tooltips_renderCompleteTextWithoutGrowingHeadings() throws Exception {
        runOnFxThread(() -> {
            String longContext = "Lesson search: sn/" + "Alex ".repeat(20).strip()
                    + " tu/" + "Mei ".repeat(25).strip() + " s/" + "Math ".repeat(10).strip()
                    + " d/Mon st/0900 et/1000 rm/ROOM123456";
            var historyContext = new SimpleStringProperty("Student S9223372036854775807 · attendance history"
                    + " · L9223372036854775807");
            Region lessons = new LessonListPanel(new LessonQueries(createData()).getLessonList(),
                    new SimpleStringProperty(longContext)).getRoot();
            Region history = new HistoryListPanel(FXCollections.<AttendanceHistoryEntry>observableArrayList(),
                    historyContext).getRoot();
            for (Region panel : List.of(lessons, history)) {
                Stage stage = new Stage();
                try {
                    Scene scene = new Scene(panel);
                    scene.getStylesheets().add(MainWindow.class.getResource("/view/DarkTheme.css").toExternalForm());
                    stage.setScene(scene);
                    stage.show();
                    for (int width : new int[] {198, 399}) {
                        panel.resize(width, 220);
                        panel.applyCss();
                        panel.layout();
                        for (String id : panel == lessons ? List.of("lessonContext", "lessonSummary")
                                : List.of("historyContext", "historySummary")) {
                            Label heading = (Label) panel.lookup("#" + id);
                            double height = heading.getHeight();
                            Tooltip tooltip = heading.getTooltip();
                            tooltip.show(heading, stage.getX(), stage.getY());
                            try {
                                Region popup = (Region) tooltip.getScene().getRoot();
                                popup.applyCss();
                                popup.layout();
                                Text rendered = (Text) popup.lookup(".text");
                                assertNotNull(rendered);
                                assertEquals(heading.getText(), rendered.getText(), id);
                                assertTrue(popup.getWidth() <= 321, "Tooltip must stay contained");
                                Bounds textBounds = rendered.localToScene(rendered.getBoundsInLocal());
                                assertTrue(textBounds.getMinY() >= 0
                                        && textBounds.getMaxY() <= tooltip.getScene().getHeight() + 1,
                                        "All wrapped lines must fit the popup window: " + textBounds);
                                assertTrue(textBounds.getMinX() >= 0
                                        && textBounds.getMaxX() <= tooltip.getScene().getWidth() + 1,
                                        "Complete text must fit the popup width: " + textBounds);
                                assertEquals(height, heading.getHeight(), "Inspecting must not grow the heading");
                            } finally {
                                tooltip.hide();
                            }
                        }
                    }
                    if (panel == history) {
                        historyContext.set("Student S2 · attendance history");
                        assertTooltip(panel, "historyContext", historyContext.get());
                    }
                } finally {
                    stage.hide();
                }
            }
        });
    }

    private static void assertTooltip(Region panel, String id, String expected) {
        Label heading = (Label) panel.lookup("#" + id);
        assertEquals(expected, heading.getText());
        assertNotNull(heading.getTooltip());
        assertEquals(expected, heading.getTooltip().getText());
        assertTrue(heading.getTooltip().isWrapText());
        assertEquals(Duration.INDEFINITE, heading.getTooltip().getShowDuration());
    }
}
