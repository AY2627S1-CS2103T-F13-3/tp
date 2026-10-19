package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.FxTestUtil.callOnFxThread;
import static seedu.address.testutil.FxTestUtil.runOnFxThread;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import seedu.address.logic.CanonicalCommandCatalog;
import seedu.address.logic.CanonicalLogicManager;
import seedu.address.logic.CanonicalPeopleSession;
import seedu.address.model.PonHubDataState;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Address;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.PeopleRegistry;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Student;
import seedu.address.storage.JsonPonHubDataStorage;
import seedu.address.testutil.FxTestUtil;
import seedu.address.testutil.TypicalPersons;

public class MainWindowTest {

    private static final Student LONG_PERSON = new Student(new PersonId("S1"),
            new ContactDetails(new Name("Alexandria ".repeat(9).strip()),
                    Optional.of(new Phone("000123456789012")),
                    Optional.of(new Email("longcontact".repeat(20) + "@example.com")),
                    Optional.of(new Address("12 " + "A long residential road ".repeat(8).strip()))),
            new EducationLevel("P1"), new Phone("009876543210123"));
    private static final String LONG_PERSON_IDENTITY = "Student · S1";

    @TempDir
    public Path tempDirectory;

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxTestUtil.initializeToolkit();
    }

    @Test
    public void layout_bottomDock_spansBothColumnsAndStaysOutsideResizableContent() throws Exception {
        Region root = callOnFxThread(() -> (Region) createWindow().getPrimaryStage().getScene().getRoot());
        for (int[] size : new int[][] {{450, 330}, {853, 420}, {1536, 800}, {1920, 1020}}) {
            runOnFxThread(() -> {
                layoutScene(root, size[0], size[1]);
                SplitPane results = (SplitPane) root.lookup("#resultSplitPane");
                SplitPane records = (SplitPane) root.lookup("#recordsSplitPane");
                TextField command = (TextField) root.lookup("#commandTextField");
                command.setText("unfinished draft");
                command.selectRange(2, 7);
                for (double position : new double[] {0.2, 0.8}) {
                    results.setDividerPositions(position);
                    records.setDividerPositions(position);
                    root.layout();
                    Bounds dock = root.lookup("#commandBoxPlaceholder").localToScene(
                            root.lookup("#commandBoxPlaceholder").getBoundsInLocal());
                    Bounds workspace = results.localToScene(results.getBoundsInLocal());
                    assertTrue(dock.getMinY() >= workspace.getMaxY() - 1,
                            "Command input must stay below the result and both record columns.");
                    assertEquals(0, dock.getMinX(), 1);
                    assertEquals(root.getWidth(), dock.getWidth(), 1);
                    assertAccessible(command, root);
                    assertAccessible(root.lookup("#productTitle"), root);
                    assertEquals("unfinished draft", command.getText());
                    assertEquals(2, command.getSelection().getStart());
                    assertEquals(7, command.getSelection().getEnd());
                }
            });
        }
    }

    @Test
    public void layout_compactAndLargeScenes_keepControlsAndListAccessible() throws Exception {
        Region root = callOnFxThread(() -> (Region) createWindow().getPrimaryStage().getScene().getRoot());
        // Exceed even the enlarged result viewport at the widest scene, including compact Mac font metrics.
        String feedback = "Long feedback that must remain readable. ".repeat(400) + "Final feedback marker.";
        for (int[] size : new int[][] {{450, 330}, {853, 420}, {1536, 800}, {1920, 1020}}) {
            runOnFxThread(() -> {
                layoutScene(root, size[0], size[1]);
                TextArea result = (TextArea) root.lookup("#resultDisplay");
                result.setText(feedback);
                result.setScrollTop(0);
                root.layout();
            });
            runOnFxThread(() -> {
                root.applyCss();
                root.layout();
                TextArea result = (TextArea) root.lookup("#resultDisplay");
                // Wrapped-content layout can defer the ScrollPane fit update to the next JavaFX turn.
                result.layout();
            });
            runOnFxThread(() -> {
                root.layout();
                for (String selector : List.of("#menuBar", "#commandTextField", "#resultDisplay",
                        "#personListView", "#statusbarPlaceholder")) {
                    assertAccessible(root.lookup(selector), root);
                }

                ListView<?> people = (ListView<?>) root.lookup("#personListView");
                assertTrue(people.getHeight() >= 40, "The people view must retain a usable viewport.");
                assertTrue(verticalScrollBar(people).isVisible(), "The populated list must be scrollable.");

                TextArea result = (TextArea) root.lookup("#resultDisplay");
                result.layout();
                assertTrue(result.isWrapText());
                assertTrue(verticalScrollBar(result).isVisible(),
                        "Long feedback must have a scrollable result area at " + size[0] + "x" + size[1]
                                + "; height=" + result.getHeight());
                ScrollPane scrollPane = (ScrollPane) result.lookup(".scroll-pane");
                assertTrue(scrollPane.getContent().getBoundsInLocal().getHeight()
                        > scrollPane.getViewportBounds().getHeight(), "Long feedback must exceed its viewport.");
                assertEquals(0, result.getScrollTop(), 1, "Each size must begin at the top of the feedback.");
                ScrollBar bar = verticalScrollBar(result);
                bar.setValue(bar.getMax());
            });
            runOnFxThread(() -> {
                root.layout();
                TextArea result = (TextArea) root.lookup("#resultDisplay");
                ScrollPane scrollPane = (ScrollPane) result.lookup(".scroll-pane");
                Node viewport = scrollPane.lookup(".viewport");
                Bounds viewportBounds = viewport.localToScene(viewport.getBoundsInLocal());
                Node content = scrollPane.getContent();
                Bounds contentBounds = content.localToScene(content.getBoundsInLocal());
                assertTrue(result.getScrollTop() > 0, "Scrolling must move the long feedback.");
                assertTrue(contentBounds.getMaxY() >= viewportBounds.getMinY()
                                && contentBounds.getMaxY() <= viewportBounds.getMaxY() + 1,
                        "Scrolling must expose the final feedback at " + size[0] + "x" + size[1]);
                assertEquals(feedback, result.getText(), "Scrolling must retain the complete feedback.");
            });
        }
    }

    @Test
    public void layout_longCanonicalContact_wrapAndRemainReachableAfterResizing() throws Exception {
        Region root = callOnFxThread(() -> (Region) createWindow().getPrimaryStage().getScene().getRoot());
        double[] narrowCardHeights = {0};
        for (int width : new int[] {450, 1536, 450}) {
            runOnFxThread(() -> {
                layoutScene(root, width, 420);
                ListView<?> people = (ListView<?>) root.lookup("#personListView");
                people.scrollTo(0);
                people.getSelectionModel().select(0);
                root.layout();
            });
            runOnFxThread(() -> {
                root.applyCss();
                for (int pass = 0; pass < 3; pass++) {
                    root.layout();
                }
                ListView<?> people = (ListView<?>) root.lookup("#personListView");
                Region card = findCard(people, LONG_PERSON_IDENTITY);

                for (Node node : card.lookupAll(".label")) {
                    Label label = (Label) node;
                    assertTrue(label.isWrapText(), label.getText());
                    assertTrue(label.getWidth() > 0 && label.getWidth() <= card.getWidth(),
                            label.getText() + "; label width=" + label.getWidth()
                                    + "; card width=" + card.getWidth() + "; scene width=" + width);
                    assertTrue(label.getHeight() + 1 >= label.prefHeight(label.getWidth()),
                            label.getText() + "; actual=" + label.getHeight()
                                    + "; preferred=" + label.prefHeight(label.getWidth())
                                    + "; width=" + label.getWidth());
                    assertEquals(label.getText(), ((Text) label.lookup(".text")).getText(),
                            "Complete canonical values must not use an ellipsis.");
                    if (width == 450 && label.getText().length() > 100) {
                        assertTrue(label.getHeight() > label.getFont().getSize() * 1.5,
                                "Long values must wrap onto multiple lines: " + label.getText());
                    }
                }
                if (width == 1536) {
                    assertTrue(card.getHeight() < narrowCardHeights[0],
                            "A wider cell must recompute the wrapped card's height.");
                } else {
                    narrowCardHeights[0] = card.getHeight();
                }

                assertLongFieldEndsReachable(people);
                people.scrollTo(people.getItems().size() - 1);
                root.layout();
                ScrollBar bar = verticalScrollBar(people);
                bar.setValue(bar.getMax());
                root.applyCss();
                for (int pass = 0; pass < 3; pass++) {
                    root.layout();
                }

                Region lastCard = findCard(people, "Tutor · T7");
                Node lastField = lastCard.lookupAll(".label").stream()
                        .filter(node -> ((Label) node).getText().equals("Address: "
                                + TypicalPersons.GEORGE.getAddress().value))
                        .findFirst().orElseThrow();
                Bounds lastFieldBounds = lastField.localToScene(lastField.getBoundsInLocal());
                Bounds viewport = people.localToScene(people.getBoundsInLocal());
                assertTrue(lastFieldBounds.getWidth() > 0 && lastFieldBounds.getHeight() > 0
                                && lastFieldBounds.getMinY() >= viewport.getMinY()
                                && lastFieldBounds.getMaxY() <= viewport.getMaxY() + 1,
                        "Scrolling must expose the final field of the last contact: " + lastFieldBounds);
            });
        }
    }

    @Test
    public void list_longCanonicalContact_wrapAfterRoleFilterAndCardsRefresh() throws Exception {
        MainWindow window = callOnFxThread(this::createWindow);
        Region root = callOnFxThread(() -> (Region) window.getPrimaryStage().getScene().getRoot());
        runOnFxThread(() -> layoutScene(root, 450, 330));
        runOnFxThread(() -> {
            TextField command = (TextField) root.lookup("#commandTextField");
            command.setText("list r/tutor");
            Event.fireEvent(command, new ActionEvent());
            ListView<?> people = (ListView<?>) root.lookup("#personListView");
            assertEquals(7, people.getItems().size());
            command.setText("list");
            Event.fireEvent(command, new ActionEvent());
            assertEquals(8, people.getItems().size());
            command.setText("help add");
            Event.fireEvent(command, new ActionEvent());
            root.applyCss();
            root.layout();
        });
        runOnFxThread(() -> {
            root.layout();
            ListView<?> people = (ListView<?>) root.lookup("#personListView");
            Region card = findCard(people, LONG_PERSON_IDENTITY);
            for (Node node : card.lookupAll(".label")) {
                Label label = (Label) node;
                assertTrue(label.isWrapText(), label.getText());
                assertTrue(label.getHeight() + 1 >= label.prefHeight(label.getWidth()),
                        "Refreshed fields must retain the complete wrapped text height: " + label.getText()
                                + "; actual=" + label.getHeight()
                                + "; preferred=" + label.prefHeight(label.getWidth()));
                Text renderedText = (Text) label.lookup(".text");
                assertEquals(label.getText(), renderedText.getText(), "Refreshed fields must not use an ellipsis.");
            }
            assertLongFieldEndsReachable(people);
        });
    }

    @Test
    public void help_compactScene_menuAndF1PreserveCommandDraftAndSelection() throws Exception {
        callOnFxThread(() -> {
            MainWindow window = createWindow();
            byte[] originalData = Files.readAllBytes(tempDirectory.resolve("contacts.json"));
            Region root = layoutWindow(window, 450, 330);
            TextField command = (TextField) root.lookup("#commandTextField");
            TextArea result = (TextArea) root.lookup("#resultDisplay");
            ListView<?> people = (ListView<?>) root.lookup("#personListView");
            MenuBar menuBar = (MenuBar) root.lookup("#menuBar");
            command.setText("add r/student n/Unfinished");
            people.getSelectionModel().select(1);

            MenuItem exit = menuBar.getMenus().get(0).getItems().get(0);
            assertEquals("Exit", exit.getText());
            assertNotNull(exit.getOnAction());
            MenuItem help = menuBar.getMenus().get(1).getItems().get(0);
            help.fire();
            assertEquals(CanonicalCommandCatalog.getOverview(Optional.empty()), result.getText());
            assertEquals("add r/student n/Unfinished", command.getText());
            assertEquals(1, people.getSelectionModel().getSelectedIndex());

            result.setText("Previous result");
            Event.fireEvent(command, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F1,
                    false, false, false, false));
            assertEquals(CanonicalCommandCatalog.getOverview(Optional.empty()), result.getText());
            assertEquals("add r/student n/Unfinished", command.getText());
            assertEquals(1, people.getSelectionModel().getSelectedIndex());
            assertArrayEquals(originalData, Files.readAllBytes(tempDirectory.resolve("contacts.json")));
            return null;
        });
    }

    /**
     * Builds the active window with canonical logic and immutable records stored only in the test directory.
     */
    private MainWindow createWindow() throws IOException {
        PeopleRegistry people = new PeopleRegistry();
        people.addStudent(LONG_PERSON.getContactDetails(), LONG_PERSON.getLevel(), LONG_PERSON.getParentPhone());
        for (Person person : TypicalPersons.getTypicalPersons()) {
            people.addTutor(new ContactDetails(person.getName(), Optional.of(person.getPhone()),
                    Optional.of(person.getEmail()), Optional.of(person.getAddress())));
        }
        Path dataPath = tempDirectory.resolve("contacts.json");
        new JsonPonHubDataStorage(dataPath).saveData(
                new PonHubDataState(people.exportState(), List.of(), List.of(), 0));
        CanonicalLogicManager logic = new CanonicalLogicManager(new CanonicalPeopleSession(dataPath), new UserPrefs());
        MainWindow window = new MainWindow(new Stage(), logic, dataPath);
        window.fillInnerParts();
        return window;
    }

    /**
     * Lays out the actual themed FXML at a logical content size without displaying a native window.
     */
    private static Region layoutWindow(MainWindow window, double width, double height) {
        Region root = (Region) window.getPrimaryStage().getScene().getRoot();
        root.applyCss();
        root.resize(width, height);
        for (int pass = 0; pass < 3; pass++) {
            root.layout();
        }
        return root;
    }

    /**
     * Detaches the populated FXML root from its hidden stage so the fixture has the requested scene dimensions.
     */
    private static void layoutScene(Region root, double width, double height) {
        Scene previousScene = root.getScene();
        List<String> stylesheets = List.copyOf(previousScene.getStylesheets());
        previousScene.setRoot(new StackPane());
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().setAll(stylesheets);
        root.applyCss();
        root.resize(width, height);
        for (int pass = 0; pass < 3; pass++) {
            root.layout();
        }
        assertEquals(width, scene.getWidth());
        assertEquals(height, scene.getHeight());
    }

    private static void assertAccessible(Node node, Region root) {
        assertNotNull(node);
        Bounds bounds = node.localToScene(node.getBoundsInLocal());
        assertTrue(node.isVisible() && bounds.getWidth() > 0 && bounds.getHeight() > 0, node.getId());
        assertTrue(bounds.getMinX() >= -1 && bounds.getMinY() >= -1
                        && bounds.getMaxX() <= root.getWidth() + 1 && bounds.getMaxY() <= root.getHeight() + 1,
                node.getId() + " must fit in the scene: " + bounds);
    }

    private static ScrollBar verticalScrollBar(Node control) {
        return control.lookupAll(".scroll-bar").stream()
                .filter(ScrollBar.class::isInstance)
                .map(ScrollBar.class::cast)
                .filter(bar -> bar.getOrientation() == Orientation.VERTICAL)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing vertical scrollbar: " + control.getId()));
    }

    private static Region findCard(ListView<?> people, String identity) {
        return people.lookupAll("#cardPane").stream()
                .filter(MainWindowTest::isVisibleInHierarchy)
                .filter(node -> identity.equals(((Label) node.lookup("#identity")).getText()))
                .map(Region.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Contact is not reachable: " + identity));
    }

    private static boolean isVisibleInHierarchy(Node node) {
        for (Node ancestor = node; ancestor != null; ancestor = ancestor.getParent()) {
            if (!ancestor.isVisible()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks that scrolling can expose the complete heading, stable identity and every canonical contact field.
     */
    private static void assertLongFieldEndsReachable(ListView<?> people) {
        ContactDetails contacts = LONG_PERSON.getContactDetails();
        Set<String> unseenValues = new HashSet<>(List.of("1. " + LONG_PERSON.getName().fullName,
                LONG_PERSON_IDENTITY, "Level: " + LONG_PERSON.getLevel(),
                "Parent phone: " + LONG_PERSON.getParentPhone().value,
                "Own phone: " + contacts.getPhone().orElseThrow().value,
                "Email: " + contacts.getEmail().orElseThrow().value,
                "Address: " + contacts.getAddress().orElseThrow().value));
        ScrollBar bar = verticalScrollBar(people);
        Bounds viewport = people.localToScene(people.getBoundsInLocal());
        for (int step = 0; step <= 100 && !unseenValues.isEmpty(); step++) {
            bar.setValue(bar.getMin() + (bar.getMax() - bar.getMin()) * step / 100);
            people.applyCss();
            people.layout();
            for (Node card : people.lookupAll("#cardPane")) {
                if (!isVisibleInHierarchy(card)
                        || !LONG_PERSON_IDENTITY.equals(((Label) card.lookup("#identity")).getText())) {
                    continue;
                }
                for (Node node : card.lookupAll(".label")) {
                    Label label = (Label) node;
                    Bounds bounds = label.localToScene(label.getBoundsInLocal());
                    if (bounds.getWidth() > 0 && bounds.getHeight() > 0
                            && bounds.getMaxY() >= viewport.getMinY() && bounds.getMaxY() <= viewport.getMaxY() + 1) {
                        unseenValues.remove(label.getText());
                    }
                }
            }
        }
        assertTrue(unseenValues.isEmpty(), "Scrolling must expose every complete value: " + unseenValues);
    }
}
