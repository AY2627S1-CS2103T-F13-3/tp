package ponhub.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.FxTestUtil.callOnFxThread;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class CreditsDialogTest {
    @Test
    public void cards_packagedPhotosAndChatButtons_openMatchingProfiles() throws Exception {
        callOnFxThread(() -> {
            Stage owner = new Stage();
            owner.setScene(new Scene(new StackPane()));
            Set<String> opened = new HashSet<>();
            CreditsDialog dialog = new CreditsDialog(owner, opened::add);
            try {
                dialog.show();
                assertEquals("HOT SINGLES IN YOUR AREA!!!!", dialog.getTitle());
                var pane = dialog.getDialogPane();
                pane.applyCss();
                pane.layout();
                assertEquals(5, pane.lookupAll(".credits-photo").size());
                for (var node : pane.lookupAll(".credits-photo")) {
                    ImageView photo = (ImageView) node;
                    assertFalse(photo.getImage().isError());
                    assertTrue(photo.getImage().getWidth() > 0);
                }
                assertEquals(5, pane.lookupAll(".credits-chat").size());
                pane.lookupAll(".credits-chat").forEach(node -> ((Button) node).fire());
                assertEquals(Set.of("https://github.com/ultramanarm", "https://github.com/gugrusaurus",
                        "https://github.com/ben58882", "https://github.com/ErnestChuaa",
                        "https://github.com/ys000009"), opened);
                Button close = (Button) pane.lookupButton(pane.getButtonTypes().getFirst());
                close.requestFocus();
                pane.applyCss();
                pane.layout();
                assertEquals("Back to debugging", ((Text) close.lookup(".text")).getText());
                assertTrue(close.getWidth() >= close.prefWidth(-1) - 0.5);
                assertFalse(close.isWrapText());
            } finally {
                dialog.close();
                owner.hide();
            }
            return null;
        });
    }
}
