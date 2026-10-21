package ponhub.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.FxTestUtil.callOnFxThread;
import static ponhub.testutil.FxTestUtil.runOnFxThread;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ponhub.logic.CanonicalLogicManager;
import ponhub.logic.CanonicalPeopleSession;
import ponhub.model.PonHubDataState;
import ponhub.model.UserPrefs;
import ponhub.model.person.ContactDetails;
import ponhub.model.person.EducationLevel;
import ponhub.model.person.Name;
import ponhub.model.person.PeopleRegistry;
import ponhub.model.person.PersonId;
import ponhub.model.person.Phone;
import ponhub.storage.JsonPonHubDataStorage;
import ponhub.testutil.LessonQueryTestData;

/** Attached production layouts; native drag/scaling acceptance remains a separate check. */
public class MainWindowResultResizeTest {
    @TempDir
    public Path directory;

    @Test
    public void rosterAndHistoryCommands_preserveWorkspaceFooterAndChosenSplit() throws Exception {
        Fixture fixture = callOnFxThread(() -> createFixture(createLargeRosterData()));
        for (int[] size : new int[][] {{1200, 772}, {853, 600}, {450, 360}}) {
            runOnFxThread(() -> {
                fixture.resize(size[0], size[1]);
                fixture.enter("help");
                fixture.split().setDividerPositions(0.5);
                fixture.layout();
                double splitHeight = fixture.split().getHeight();
                double resultHeight = fixture.resultPane().getHeight();
                Region footer = (Region) fixture.root().lookup("#statusbarPlaceholder");
                double footerHeight = footer.getHeight();
                double inputY = fixture.input().localToScene(0, 0).getY();
                double position = fixture.split().getDividerPositions()[0];
                for (int cycle = 0; cycle < 3; cycle++) {
                    for (String command : List.of("help", "showlesson lid/L1 d/2026-10-19", "history 1", "help")) {
                        fixture.enter(command);
                        String context = command + " at " + size[0] + "x" + size[1];
                        if (command.startsWith("showlesson")) {
                            ScrollPane detail = (ScrollPane) fixture.root().lookup("#lessonDetailScroll");
                            assertTrue(detail.isManaged(), context);
                            assertTrue(detail.getContent().prefHeight(-1) > detail.getHeight(),
                                    "The roster must exceed its scrolling viewport.");
                        } else if (command.startsWith("history")) {
                            assertTrue(fixture.root().lookup("#historyListView").getParent().isManaged(), context);
                        }
                        assertEquals(splitHeight, fixture.split().getHeight(), 0.01, context);
                        assertEquals(resultHeight, fixture.resultPane().getHeight(), 0.01, context);
                        assertEquals(footerHeight, footer.getHeight(), 0.01, context);
                        assertEquals(inputY, fixture.input().localToScene(0, 0).getY(), 0.01, context);
                        assertEquals(position, fixture.split().getDividerPositions()[0], 0.000001, context);
                    }
                }
            });
        }
        assertEquals(fixture.originalData(), Files.readString(fixture.dataPath()));
    }

    private static PonHubDataState createLargeRosterData() {
        PeopleRegistry people = new PeopleRegistry();
        Set<PersonId> students = new HashSet<>();
        for (int index = 0; index < 30; index++) {
            String name = "Student " + (char) ('A' + index / 26) + (char) ('A' + index % 26);
            students.add(people.addStudent(new ContactDetails(new Name(name),
                    Optional.empty(), Optional.empty(), Optional.empty()),
                    new EducationLevel("P3"), new Phone("12345678")).getId());
        }
        PersonId tutor = people.addTutor(new ContactDetails(new Name("Roster Tutor"),
                Optional.of(new Phone("12345678")), Optional.empty(), Optional.empty())).getId();
        return new PonHubDataState(people.exportState(), List.of(LessonQueryTestData.lesson(
                "L1", tutor, "Mon", "0800", "0900", "Math", "R1", students)), List.of(), 1);
    }

    @Test
    public void divider_expandsAndShrinksFeedbackAndKeepsBothAreasReachable() throws Exception {
        Fixture fixture = callOnFxThread(this::createFixture);
        for (int[] size : new int[][] {{853, 600}, {450, 330}, {1536, 800}}) {
            runOnFxThread(() -> {
                fixture.resize(size[0], size[1]);
                fixture.records().setDividerPositions(0.55);
                fixture.split().setDividerPositions(0.25);
                fixture.layout();
                double initialHeight = fixture.resultPane().getHeight();
                fixture.split().setDividerPositions(0.75);
                fixture.layout();
                assertTrue(fixture.resultPane().getHeight() > initialHeight + 1,
                        "Moving the divider down must enlarge the actual result area.");
                assertEquals(0.55, fixture.records().getDividerPositions()[0], 0.01);
                for (double position : new double[] {0, 1, 0.4}) {
                    fixture.split().setDividerPositions(position);
                    fixture.layout();
                    assertTrue(fixture.resultPane().getHeight() >= 80);
                    assertTrue(fixture.root().lookup("#resultDisplayPlaceholder").getBoundsInLocal().getHeight() >= 50);
                    assertTrue(fixture.records().getHeight() >= 120);
                    assertTrue(fixture.root().lookup("#personListView").getBoundsInLocal().getHeight() >= 50);
                    assertTrue(fixture.root().lookup("#lessonListView").getBoundsInLocal().getHeight() >= 50);
                    for (String selector : List.of("#commandTextField", "#resultDisplay", "#personListView",
                            "#lessonListView", "#statusbarPlaceholder")) {
                        Bounds bounds = fixture.root().lookup(selector).localToScene(
                                fixture.root().lookup(selector).getBoundsInLocal());
                        assertTrue(bounds.getHeight() > 0 && bounds.getMinY() >= 0
                                && bounds.getMaxY() <= fixture.root().getHeight() + 1, selector + ": " + bounds);
                    }
                }
                Node divider = fixture.split().lookupAll(".split-pane-divider").stream()
                        .filter(node -> node.getParent() == fixture.split()).findFirst().orElseThrow();
                assertEquals(Cursor.V_RESIZE, divider.getCursor());
                assertTrue(divider.getBoundsInParent().getHeight() >= 6);
            });
        }
    }

    @Test
    public void commandsAndHelp_preserveSplitDraftSelectionAndOperationalFile() throws Exception {
        Fixture fixture = callOnFxThread(this::createFixture);
        runOnFxThread(() -> {
            fixture.resize(853, 600);
            fixture.enter("search c/student n/Jamie");
            ListView<?> people = (ListView<?>) fixture.root().lookup("#personListView");
            people.getSelectionModel().select(0);
            Object selected = people.getSelectionModel().getSelectedItem();
            fixture.split().setDividerPositions(0.6);
            fixture.layout();
            double height = fixture.resultPane().getHeight();
            for (String command : List.of("help", "help search", "add", "list r/student")) {
                fixture.enter(command);
                assertEquals(height, fixture.resultPane().getHeight(), 1);
            }
            // Select after the explicit list command; help must preserve this exact selection and draft.
            people.getSelectionModel().select(people.getItems().indexOf(selected));
            fixture.input().setText("add r/student n/Unfinished");
            Event.fireEvent(fixture.input(), new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F1,
                    false, false, false, false));
            fixture.layout();
            assertEquals("add r/student n/Unfinished", fixture.input().getText());
            assertSame(selected, people.getSelectionModel().getSelectedItem());
            assertEquals(height, fixture.resultPane().getHeight(), 1);
            MenuBar menu = (MenuBar) fixture.root().lookup("#menuBar");
            menu.getMenus().get(1).getItems().getFirst().fire();
            fixture.layout();
            assertEquals("add r/student n/Unfinished", fixture.input().getText());
            assertSame(selected, people.getSelectionModel().getSelectedItem());
            assertEquals(height, fixture.resultPane().getHeight(), 1);
            assertFalse(fixture.result().isEditable());
            assertTrue(fixture.result().isWrapText());
            fixture.result().selectRange(0, 4);
            assertEquals(fixture.result().getText().substring(0, 4), fixture.result().getSelectedText());
        });
        assertEquals(fixture.originalData(), Files.readString(fixture.dataPath()));
    }

    @Test
    public void longFeedback_scrollsToItsEndAfterResizingAndRepeatedUpdates() throws Exception {
        Fixture fixture = callOnFxThread(this::createFixture);
        String feedback = "Long guidance and validation details. ".repeat(120) + "Final feedback marker.";
        runOnFxThread(() -> {
            fixture.resize(450, 330);
            fixture.split().setDividerPositions(0.7);
            fixture.result().setText(feedback);
            fixture.layout();
        });
        runOnFxThread(fixture::layout);
        runOnFxThread(() -> {
            ScrollBar vertical = fixture.result().lookupAll(".scroll-bar").stream().map(ScrollBar.class::cast)
                    .filter(bar -> bar.getOrientation() == Orientation.VERTICAL).findFirst().orElseThrow();
            assertTrue(vertical.isVisible());
            vertical.setValue(vertical.getMax());
        });
        runOnFxThread(() -> {
            fixture.layout();
            ScrollPane scroll = (ScrollPane) fixture.result().lookup(".scroll-pane");
            Bounds viewport = scroll.lookup(".viewport").localToScene(
                    scroll.lookup(".viewport").getBoundsInLocal());
            Bounds content = scroll.getContent().localToScene(scroll.getContent().getBoundsInLocal());
            assertTrue(fixture.result().getScrollTop() > 0);
            assertTrue(content.getMaxY() <= viewport.getMaxY() + 2,
                    "The end of the complete message must be reachable.");
            fixture.resize(853, 600);
            fixture.enter("help search");
            double height = fixture.resultPane().getHeight();
            fixture.enter("add");
            fixture.enter("help");
            assertEquals(height, fixture.resultPane().getHeight(), 1);
        });
    }

    private Fixture createFixture() throws Exception {
        return createFixture(LessonQueryTestData.createData().exportState());
    }

    private Fixture createFixture(PonHubDataState data) throws Exception {
        Path dataPath = directory.resolve("canonical.json");
        new JsonPonHubDataStorage(dataPath).saveData(data);
        var logic = new CanonicalLogicManager(new CanonicalPeopleSession(dataPath), new UserPrefs());
        MainWindow window = new MainWindow(new Stage(), logic, dataPath);
        window.fillInnerParts();
        Scene previous = window.getPrimaryStage().getScene();
        Region root = (Region) previous.getRoot();
        List<String> stylesheets = List.copyOf(previous.getStylesheets());
        previous.setRoot(new StackPane());
        Scene scene = new Scene(root, 853, 600);
        scene.getStylesheets().setAll(stylesheets);
        Fixture fixture = new Fixture(root, dataPath, Files.readString(dataPath));
        fixture.resize(853, 600);
        return fixture;
    }

    private record Fixture(Region root, Path dataPath, String originalData) {
        private SplitPane split() {
            return (SplitPane) root.lookup("#resultSplitPane");
        }

        private SplitPane records() {
            return (SplitPane) root.lookup("#recordsSplitPane");
        }

        private Region resultPane() {
            return (Region) root.lookup("#feedbackSection");
        }

        private TextArea result() {
            return (TextArea) root.lookup("#resultDisplay");
        }

        private TextField input() {
            return (TextField) root.lookup("#commandTextField");
        }

        private void resize(double width, double height) {
            root.resize(width, height);
            layout();
        }

        private void layout() {
            root.applyCss();
            for (int pass = 0; pass < 4; pass++) {
                root.layout();
            }
        }

        private void enter(String command) {
            input().setText(command);
            Event.fireEvent(input(), new ActionEvent());
            layout();
        }
    }
}
