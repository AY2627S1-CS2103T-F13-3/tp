package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.FxTestUtil.callOnFxThread;
import static seedu.address.testutil.FxTestUtil.runOnFxThread;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import seedu.address.logic.CommandCatalog;
import seedu.address.logic.LogicManager;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.FxTestUtil;
import seedu.address.testutil.PersonBuilder;
import seedu.address.testutil.TypicalPersons;

public class MainWindowTest {

    private static final Person LONG_PERSON = new PersonBuilder()
            .withName("Alexandria ".repeat(9).strip())
            .withPhone("000123456789012")
            .withEmail("longcontact".repeat(20) + "@example.com")
            .withAddress("12 " + "A long residential road ".repeat(8).strip())
            .withTags("LongTag".repeat(50), "shortTag")
            .build();

    @TempDir
    public Path tempDirectory;

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxTestUtil.initializeToolkit();
    }

    @Test
    public void layout_compactAndLargeScenes_keepControlsAndListAccessible() throws Exception {
        Region root = callOnFxThread(() -> (Region) createWindow().getPrimaryStage().getScene().getRoot());
        for (int[] size : new int[][] {{450, 330}, {853, 420}, {1536, 800}, {1920, 1020}}) {
            runOnFxThread(() -> {
                layoutScene(root, size[0], size[1]);
                TextArea result = (TextArea) root.lookup("#resultDisplay");
                result.setText("Long feedback that must remain readable. ".repeat(100));
                root.layout();
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
            });
        }
    }

    @Test
    public void layout_longContactAndTags_wrapAndRemainReachableAfterResizing() throws Exception {
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
                Region card = findCard(people, LONG_PERSON.getName().fullName);

                for (Node node : card.lookupAll(".label")) {
                    Label label = (Label) node;
                    if ("id".equals(label.getId())) {
                        continue;
                    }
                    assertTrue(label.isWrapText(), label.getText());
                    assertTrue(label.getWidth() > 0 && label.getWidth() <= card.getWidth(),
                            label.getText() + "; label width=" + label.getWidth()
                                    + "; card width=" + card.getWidth() + "; scene width=" + width);
                    assertTrue(label.getHeight() + 1 >= label.prefHeight(label.getWidth()),
                            label.getText() + "; actual=" + label.getHeight()
                                    + "; preferred=" + label.prefHeight(label.getWidth())
                                    + "; width=" + label.getWidth());
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

                Region lastCard = findCard(people, TypicalPersons.GEORGE.getName().fullName);
                Bounds lastEmail = lastCard.lookup("#email").localToScene(
                        lastCard.lookup("#email").getBoundsInLocal());
                Bounds viewport = people.localToScene(people.getBoundsInLocal());
                assertTrue(lastEmail.getWidth() > 0 && lastEmail.getHeight() > 0
                                && lastEmail.getMinY() >= viewport.getMinY()
                                && lastEmail.getMaxY() <= viewport.getMaxY() + 1,
                        "Scrolling must expose the final field of the last contact: " + lastEmail);
            });
        }
    }

    @Test
    public void help_compactScene_menuAndF1PreserveCommandDraftAndSelection() throws Exception {
        runOnFxThread(() -> {
            MainWindow window = createWindow();
            Region root = layoutWindow(window, 450, 330);
            TextField command = (TextField) root.lookup("#commandTextField");
            TextArea result = (TextArea) root.lookup("#resultDisplay");
            ListView<?> people = (ListView<?>) root.lookup("#personListView");
            MenuBar menuBar = (MenuBar) root.lookup("#menuBar");
            command.setText("add n/Unfinished");
            people.getSelectionModel().select(1);

            MenuItem exit = menuBar.getMenus().get(0).getItems().get(0);
            assertEquals("Exit", exit.getText());
            assertNotNull(exit.getOnAction());
            MenuItem help = menuBar.getMenus().get(1).getItems().get(0);
            help.fire();
            assertEquals(CommandCatalog.getOverview(), result.getText());
            assertEquals("add n/Unfinished", command.getText());
            assertEquals(1, people.getSelectionModel().getSelectedIndex());

            result.setText("Previous result");
            Event.fireEvent(command, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F1,
                    false, false, false, false));
            assertEquals(CommandCatalog.getOverview(), result.getText());
            assertEquals("add n/Unfinished", command.getText());
            assertEquals(1, people.getSelectionModel().getSelectedIndex());
            assertFalse(Files.exists(tempDirectory.resolve("contacts.json")));
        });
    }

    /**
     * Builds the active window with real logic and fixture records, without loading or saving user files.
     */
    private MainWindow createWindow() {
        AddressBook addressBook = new AddressBook();
        addressBook.addPerson(LONG_PERSON);
        TypicalPersons.getTypicalPersons().forEach(addressBook::addPerson);
        ModelManager model = new ModelManager(addressBook, new UserPrefs());
        Path dataPath = tempDirectory.resolve("contacts.json");
        StorageManager storage = new StorageManager(new JsonAddressBookStorage(dataPath),
                new JsonUserPrefsStorage(tempDirectory.resolve("preferences.json")));
        MainWindow window = new MainWindow(new Stage(), new LogicManager(model, storage), dataPath);
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

    private static Region findCard(ListView<?> people, String name) {
        return people.lookupAll("#cardPane").stream()
                .filter(MainWindowTest::isVisibleInHierarchy)
                .filter(node -> name.equals(((Label) node.lookup("#name")).getText()))
                .map(Region.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Contact is not reachable: " + name));
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
     * Checks that scrolling can expose the end of every long value, including an individual long tag.
     */
    private static void assertLongFieldEndsReachable(ListView<?> people) {
        Set<String> unseenValues = new HashSet<>(List.of(LONG_PERSON.getName().fullName, LONG_PERSON.getPhone().value,
                LONG_PERSON.getAddress().value, LONG_PERSON.getEmail().value));
        LONG_PERSON.getTags().forEach(tag -> unseenValues.add(tag.tagName));
        ScrollBar bar = verticalScrollBar(people);
        Bounds viewport = people.localToScene(people.getBoundsInLocal());
        for (int step = 0; step <= 100 && !unseenValues.isEmpty(); step++) {
            bar.setValue(bar.getMin() + (bar.getMax() - bar.getMin()) * step / 100);
            people.applyCss();
            people.layout();
            for (Node card : people.lookupAll("#cardPane")) {
                if (!isVisibleInHierarchy(card)
                        || !LONG_PERSON.getName().fullName.equals(((Label) card.lookup("#name")).getText())) {
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
