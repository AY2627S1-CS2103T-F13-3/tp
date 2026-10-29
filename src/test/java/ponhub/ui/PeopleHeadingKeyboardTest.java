package ponhub.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.FxTestUtil.callOnFxThread;
import static ponhub.testutil.FxTestUtil.runOnFxThread;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.css.PseudoClass;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import ponhub.MainApp;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.search.SearchCategory;
import ponhub.model.search.SearchCriteria;
import ponhub.model.search.SearchField;
import ponhub.testutil.FxTestUtil;

/**
 * Checks the production heading's overflow-dependent focus route and stable visible focus frame.
 */
public class PeopleHeadingKeyboardTest {

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxTestUtil.initializeToolkit();
    }

    @Test
    public void heading_longCriteria_exposesScrollRouteAndResetsWithoutChangingData() throws Exception {
        PonHubData data = new PonHubData();
        var before = data.exportState();
        PeopleView view = new PeopleView(data);
        view.setSearchCriteria(new SearchCriteria(SearchCategory.STUDENT,
                Map.of(SearchField.ADDRESS, "Long criterion ".repeat(100))));
        Stage stage = callOnFxThread(() -> show(new PersonRecordListPanel(view).getRoot(), 300, 420));
        try {
            runOnFxThread(() -> {
                Region panel = (Region) stage.getScene().getRoot();
                ScrollPane pane = (ScrollPane) panel.lookup("#peopleContextPane");
                Label heading = (Label) panel.lookup("#peopleContext");
                for (double width : new double[] {300, 900}) {
                    panel.resize(width, 420);
                    panel.applyCss();
                    panel.layout();
                    assertTrue(heading.getHeight() > pane.getViewportBounds().getHeight());
                    assertTrue(pane.isFocusTraversable());
                    assertEquals(heading.getText(), pane.getAccessibleText());
                    assertTrue(((Label) panel.lookup("#filterResetHint")).getText().contains("Tab"));
                    double viewportHeight = pane.getViewportBounds().getHeight();
                    pane.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true);
                    panel.applyCss();
                    panel.layout();
                    assertEquals(viewportHeight, pane.getViewportBounds().getHeight());
                    assertEquals(Color.web("#FF9900"),
                            pane.getBorder().getStrokes().getFirst().getTopStroke());
                    pane.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), false);
                    pane.setVvalue(1);
                    assertEquals(1, pane.getVvalue());
                    ScrollBar bar = (ScrollBar) pane.lookupAll(".scroll-bar").stream()
                            .filter(node -> ((ScrollBar) node).getOrientation() == Orientation.VERTICAL)
                            .findFirst().orElseThrow();
                    Region thumb = (Region) bar.lookup(".thumb");
                    assertTrue(bar.isVisible());
                    assertTrue(thumb.isVisible(), "The skin must not hide the thumb in a short track.");
                    assertTrue(thumb.getHeight() > thumb.getBackground().getFills().getFirst().getInsets().getTop()
                            + thumb.getBackground().getFills().getFirst().getInsets().getBottom(),
                            "The thumb must retain painted height inside its insets.");
                }
                view.setRoleFilter(Optional.empty());
                panel.layout();
                assertEquals("All people", heading.getText());
                assertEquals(0, pane.getVvalue());
                assertFalse(pane.isFocusTraversable());
                assertFalse(panel.lookup("#filterResetHint").isVisible());
                assertEquals(before, data.exportState());
            });
        } finally {
            runOnFxThread(stage::hide);
        }
    }

    @Test
    public void heading_resize_recomputesOverflowForShortCriteria() throws Exception {
        PeopleView view = new PeopleView(new PonHubData());
        view.setSearchCriteria(new SearchCriteria(SearchCategory.STUDENT,
                Map.of(SearchField.NAME, "Alexandra Catherine Elizabeth Montgomery Wellington")));
        Stage stage = callOnFxThread(() -> show(new PersonRecordListPanel(view).getRoot(), 200, 420));
        try {
            runOnFxThread(() -> {
                Region panel = (Region) stage.getScene().getRoot();
                ScrollPane pane = (ScrollPane) panel.lookup("#peopleContextPane");
                assertTrue(pane.isFocusTraversable());
                panel.resize(1000, 420);
                panel.layout();
                assertFalse(pane.isFocusTraversable());
                assertEquals("Use list to show all people.", ((Label) panel.lookup("#filterResetHint")).getText());
                assertEquals(0, pane.getVvalue());
            });
        } finally {
            runOnFxThread(stage::hide);
        }
    }

    private static Stage show(Region panel, double width, double height) {
        Scene scene = new Scene(panel, width, height);
        scene.getStylesheets().add(MainApp.class.getResource("/view/DarkTheme.css").toExternalForm());
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        panel.applyCss();
        panel.layout();
        return stage;
    }
}
