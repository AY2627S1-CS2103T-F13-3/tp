package ponhub.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.FxTestUtil.runOnFxThread;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import ponhub.MainApp;
import ponhub.model.person.ContactDetails;
import ponhub.model.person.EducationLevel;
import ponhub.model.person.Name;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.Phone;
import ponhub.model.person.Student;
import ponhub.testutil.FxTestUtil;

public class PersonRecordListScaleTest {

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxTestUtil.initializeToolkit();
    }

    @Test
    public void panel_shortCardsAcrossDisplayScaleChanges_keepsHorizontalBarHidden() throws Exception {
        runOnFxThread(() -> {
            for (double initialScale : new double[] {1.25, 1.5, 1, 2}) {
                var people = FXCollections.<PersonRecord>observableArrayList(
                        new Student(new PersonId("S1"), new ContactDetails(new Name("alex tan")),
                                new EducationLevel("P1"), new Phone("91234567")),
                        new Student(new PersonId("S2"), new ContactDetails(new Name("jamie tan")),
                                new EducationLevel("S2"), new Phone("91234568")));
                PersonRecordListPanel panel = new PersonRecordListPanel(people);
                Region root = panel.getRoot();
                Stage stage = createLayoutStage(root);
                stage.setRenderScaleX(initialScale);
                stage.setRenderScaleY(initialScale);
                try {
                    layout(root, 290.4, 342.4);
                    assertNoHorizontalBar(root);
                    for (double scale : new double[] {1, 1.25, 1.5, 2, 1.25}) {
                        stage.setRenderScaleX(scale);
                        stage.setRenderScaleY(scale);
                        // At 125%, this gives the reported 275.2px people viewport after its vertical bar.
                        layout(root, 290.4, 342.4);
                        assertNoHorizontalBar(root);
                        assertEquals("Position: 1", ((Label) findCard(root, "Student · ID: S1")
                                .lookup("#position")).getText());
                    }

                    getList(root).getSelectionModel().select(1);
                    people.removeFirst();
                    panel.restoreSelection(new PersonId("S2"));
                    layout(root, 290.4, 342.4);
                    assertEquals(new PersonId("S2"), panel.getSelectedPersonId().orElseThrow());
                    assertEquals("Position: 1", ((Label) findCard(root, "Student · ID: S2")
                            .lookup("#position")).getText());
                    assertNoHorizontalBar(root);
                } finally {
                    stage.close();
                }
            }
        });
    }

    @Test
    public void panel_fractionalScaleResizing_preservesCompleteSelectedRoleCards() throws Exception {
        runOnFxThread(() -> {
            Region root = PersonRecordCardPreview.createPeopleView();
            Stage stage = createLayoutStage(root);
            ListView<PersonRecord> list = getList(root);
            try {
                for (double scale : new double[] {1.25, 1.5, 2, 1.25}) {
                    stage.setRenderScaleX(scale);
                    stage.setRenderScaleY(scale);
                    for (double width : new double[] {320, 853, 290.4}) {
                        layout(root, width, 480);
                        for (int index = 0; index < list.getItems().size(); index++) {
                            for (boolean selected : new boolean[] {false, true}) {
                                list.getSelectionModel().clearSelection();
                                if (selected) {
                                    list.getSelectionModel().select(index);
                                }
                                list.scrollTo(index);
                                layout(root, width, 480);
                                PersonRecordCardData data = new PersonRecordCardData(list.getItems().get(index),
                                        index + 1);
                                Region card = findCard(root, data.getIdentity());
                                for (var node : card.lookupAll(".label")) {
                                    Label label = (Label) node;
                                    assertTrue(label.isWrapText(), label.getText());
                                    assertTrue(label.getWidth() > 0 && label.getWidth() <= width, label.getText());
                                    assertTrue(label.getHeight() + 1 >= label.prefHeight(label.getWidth()),
                                            label.getText());
                                }
                                Label position = (Label) card.lookup("#position");
                                assertEquals("Position: " + (index + 1), position.getText());
                                assertNoHorizontalBar(root);
                            }
                        }
                        for (var node : list.lookupAll(".scroll-bar")) {
                            ScrollBar bar = (ScrollBar) node;
                            if (bar.getOrientation() == Orientation.VERTICAL && bar.isVisible()) {
                                bar.setValue(bar.getMax());
                            }
                        }
                        layout(root, width, 480);
                        Region lastCard = findCard(root, "Parent · ID: P1");
                        var bounds = lastCard.localToScene(lastCard.getBoundsInLocal());
                        assertTrue(bounds.getMinY() >= 0 && bounds.getMaxY() <= root.getHeight() + 1,
                                "Vertical scrolling must expose the complete final card: " + bounds);
                    }
                }
            } finally {
                stage.close();
            }
        });
    }

    private static Stage createLayoutStage(Region root) {
        Scene scene = new Scene(root);
        scene.getStylesheets().add(MainApp.class.getResource("/view/DarkTheme.css").toExternalForm());
        // A Scene without a Window always snaps at 100%, even on a scaled host display.
        Stage stage = new Stage();
        stage.setScene(scene);
        return stage;
    }

    private static void layout(Region root, double width, double height) {
        for (int pass = 0; pass < 4; pass++) {
            root.applyCss();
            root.resize(width, height);
            root.layout();
        }
    }

    private static void assertNoHorizontalBar(Region root) {
        assertTrue(getList(root).lookupAll(".scroll-bar").stream().map(ScrollBar.class::cast)
                .noneMatch(bar -> bar.getOrientation() == Orientation.HORIZONTAL && bar.isVisible()),
                "Wrapping people cards must fit without a horizontal scrollbar");
    }

    @SuppressWarnings("unchecked")
    private static ListView<PersonRecord> getList(Region root) {
        return (ListView<PersonRecord>) root.lookup("#personListView");
    }

    private static Region findCard(Region root, String identity) {
        return getList(root).lookupAll("#cardPane").stream()
                .filter(node -> identity.equals(((Label) node.lookup("#identity")).getText()))
                .map(Region.class::cast).findFirst().orElseThrow();
    }
}
