package ponhub.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.FxTestUtil.callOnFxThread;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

public class CreditsDialogTest {
    @Test
    public void browser_deliversProfileUriAndPreservesLaunchFailure() {
        String profile = "https://github.com/gugrusaurus";
        Set<URI> opened = new HashSet<>();
        CreditsDialog.browseProfile(profile, opened::add);
        assertEquals(Set.of(URI.create(profile)), opened);
        IOException unavailable = new IOException("Browser unavailable");
        UncheckedIOException failure = assertThrows(UncheckedIOException.class, () ->
                CreditsDialog.browseProfile(profile, uri -> {
                    throw unavailable;
                }));
        assertSame(unavailable, failure.getCause());
    }

    @Test
    public void chat_browserUnavailable_showsUsableProfileUrlWithoutClosingCredits() throws Exception {
        callOnFxThread(() -> {
            Stage owner = new Stage();
            owner.setScene(new Scene(new StackPane()));
            CreditsDialog dialog = new CreditsDialog(owner, url -> CreditsDialog.browseProfile(url, uri -> {
                throw new IOException("Browser unavailable");
            }));
            try {
                dialog.show();
                var pane = dialog.getDialogPane();
                pane.lookupAll(".credits-chat").forEach(node -> ((Button) node).fire());
                Set<String> guidance = new HashSet<>();
                pane.lookupAll(".label").stream().map(Label.class::cast)
                        .filter(label -> label.getText().startsWith("Open this profile in your browser: "))
                        .forEach(label -> {
                            assertTrue(label.isVisible());
                            assertTrue(label.isManaged());
                            guidance.add(label.getText());
                        });
                assertEquals(5, guidance.size());
                assertTrue(guidance.contains("Open this profile in your browser: https://github.com/gugrusaurus"));
                assertTrue(dialog.isShowing());
            } finally {
                dialog.close();
            }
            return null;
        });
    }

    @Test
    public void confetti_finishesNaturally_clearsParticlesWhileCreditsRemainUsable() throws Exception {
        CompletableFuture<Void> finished = new CompletableFuture<>();
        CreditsDialog dialog = callOnFxThread(() -> {
            Stage owner = new Stage();
            owner.setScene(new Scene(new StackPane()));
            return new CreditsDialog(owner, url -> {
                throw new AssertionError("Confetti must not open profiles");
            });
        });
        try {
            callOnFxThread(() -> {
                dialog.show();
                Pane confetti = (Pane) dialog.getDialogPane().lookup(".credits-confetti");
                assertEquals(69, confetti.getChildren().size());
                PauseTransition wait = new PauseTransition(Duration.millis(3400));
                wait.setOnFinished(event -> {
                    try {
                        assertTrue(confetti.getChildren().isEmpty());
                        assertTrue(dialog.isShowing());
                        assertEquals(5, dialog.getDialogPane().lookupAll(".credits-chat").size());
                        finished.complete(null);
                    } catch (Throwable failure) {
                        finished.completeExceptionally(failure);
                    }
                });
                wait.play();
                return null;
            });
            finished.get(6, TimeUnit.SECONDS);
        } finally {
            callOnFxThread(() -> {
                dialog.close();
                return null;
            });
        }
    }

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
