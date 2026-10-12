package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.FxTestUtil.callOnFxThread;
import static seedu.address.testutil.FxTestUtil.runOnFxThread;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import seedu.address.MainApp;
import seedu.address.model.person.Person;
import seedu.address.testutil.FxTestUtil;
import seedu.address.testutil.PersonBuilder;
import seedu.address.testutil.TypicalPersons;

/**
 * Preserves inherited contact and tag wrapping regressions in the legacy panel after canonical UI activation.
 */
public class LegacyPersonListPanelTest {

    private static final Person LONG_PERSON = new PersonBuilder()
            .withName("Alexandria ".repeat(9).strip())
            .withPhone("000123456789012")
            .withEmail("longcontact".repeat(20) + "@example.com")
            .withAddress("12 " + "A long residential road ".repeat(8).strip())
            .withTags("LongTag".repeat(50), "shortTag")
            .build();

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxTestUtil.initializeToolkit();
    }

    @Test
    public void layout_longContactAndTags_wrapAndRemainReachableAfterResizing() throws Exception {
        Region root = callOnFxThread(LegacyPersonListPanelTest::createPanel);
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
    public void refresh_longContactTags_wrapAfterCardsRefresh() throws Exception {
        Region root = callOnFxThread(LegacyPersonListPanelTest::createPanel);
        runOnFxThread(() -> layoutScene(root, 450, 330));
        runOnFxThread(() -> {
            ListView<?> people = (ListView<?>) root.lookup("#personListView");
            people.refresh();
            root.applyCss();
            root.layout();
        });
        runOnFxThread(() -> {
            root.layout();
            ListView<?> people = (ListView<?>) root.lookup("#personListView");
            Region card = findCard(people, LONG_PERSON.getName().fullName);
            for (Node node : card.lookup("#tags").lookupAll(".label")) {
                Label label = (Label) node;
                assertTrue(label.getHeight() + 1 >= label.prefHeight(label.getWidth()),
                        "Refreshed tags must retain the complete wrapped text height: " + label.getText()
                                + "; actual=" + label.getHeight()
                                + "; preferred=" + label.prefHeight(label.getWidth()));
                Text renderedText = (Text) label.lookup(".text");
                assertEquals(label.getText(), renderedText.getText(), "Refreshed tags must not use an ellipsis.");
            }
            assertLongFieldEndsReachable(people);
        });
    }

    /**
     * Builds the retained legacy panel with actual cards and the production stylesheet.
     */
    private static Region createPanel() {
        ObservableList<Person> people = FXCollections.observableArrayList();
        people.add(LONG_PERSON);
        people.addAll(TypicalPersons.getTypicalPersons());
        Region root = new PersonListPanel(people).getRoot();
        Scene scene = new Scene(root, 450, 330);
        scene.getStylesheets().add(MainApp.class.getResource("/view/DarkTheme.css").toExternalForm());
        return root;
    }

    /**
     * Detaches the populated FXML root from its previous scene so the fixture has the requested scene dimensions.
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
                .filter(LegacyPersonListPanelTest::isVisibleInHierarchy)
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
