package ponhub.ui;

import java.awt.Desktop;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Random;
import java.util.function.Consumer;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Shows the hidden team credits without invoking commands or changing operational data.
 */
final class CreditsDialog extends Dialog<Void> {
    private final Pane confetti = new Pane();
    private final ParallelTransition burst = new ParallelTransition();
    private final Consumer<String> openProfile;

    CreditsDialog(Window owner) {
        this(owner, CreditsDialog::browseProfile);
    }

    CreditsDialog(Window owner, Consumer<String> openProfile) {
        this.openProfile = openProfile;
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        setTitle("HOT SINGLES IN YOUR AREA!!!!");
        setResizable(true);
        getDialogPane().getStyleClass().add("credits-dialog");
        getDialogPane().getStylesheets().add(CreditsDialog.class.getResource("/view/Credits.css").toExternalForm());
        ButtonType dismiss = new ButtonType("Back to debugging", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().add(dismiss);
        Button close = (Button) getDialogPane().lookupButton(dismiss);
        close.setMinWidth(Region.USE_PREF_SIZE);
        close.setWrapText(false);
        ButtonBar.setButtonUniformSize(close, false);

        Label ribbon = new Label("!!! 100% REAL DEVELOPERS !!!");
        ribbon.getStyleClass().add("credits-ribbon");
        ribbon.setAlignment(Pos.CENTER);
        ribbon.setMaxWidth(Double.MAX_VALUE);
        ribbon.setWrapText(true);
        Label heading = new Label("HOT SINGLES IN YOUR AREA!!!!");
        heading.getStyleClass().add("credits-heading");
        heading.setWrapText(true);
        Label subtitle = new Label("Single-threaded. Ready to commit. Waiting for YOUR pull request.");
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("credits-subtitle");
        VBox team = new VBox(10,
                member("Zhu Zhi Yu", "People & testing", "Finds bugs before they find you.", "ultramanarm"),
                member("Vincent Lin", "Storage & delivery", "Looking for a committed relationship. With Git.",
                        "gugrusaurus"),
                member("Benjamin Ng", "Retrieval & views", "Your perfect match, now indexed.", "ben58882"),
                member("Ernest Chua", "Help & code quality", "Type help. He is already on it.", "ErnestChuaa"),
                member("Yang Shuo", "Lessons & attendance", "Always present. Occasionally compiling.", "ys000009"));
        ScrollPane roster = new ScrollPane(team);
        roster.setFitToWidth(true);
        roster.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        roster.setPrefViewportHeight(310);
        roster.getStyleClass().add("credits-roster");
        Label finePrint = new Label("NO CREDIT CARD. NO SUBSCRIPTION. JUST GITHUB.");
        finePrint.getStyleClass().add("credits-fine-print");
        finePrint.setWrapText(true);
        VBox content = new VBox(12, ribbon, heading, subtitle, roster, finePrint);
        content.setPrefWidth(560);
        confetti.setMouseTransparent(true);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(confetti.widthProperty());
        clip.heightProperty().bind(confetti.heightProperty());
        confetti.setClip(clip);
        confetti.getStyleClass().add("credits-confetti");
        getDialogPane().setContent(new StackPane(content, confetti));
        setOnShown(event -> launchConfetti());
        setOnHiding(event -> {
            burst.stop();
            confetti.getChildren().clear();
        });
    }

    private void launchConfetti() {
        burst.stop();
        burst.getChildren().clear();
        confetti.getChildren().clear();
        Random random = new Random();
        Color[] colors = {Color.web("#ff66a0"), Color.web("#ffb347"), Color.web("#fff36d"),
            Color.web("#78f79a"), Color.web("#66d9ff"), Color.web("#c49bff")};
        for (int index = 0; index < 69; index++) {
            Rectangle piece = new Rectangle(5 + random.nextInt(4), 8 + random.nextInt(7),
                    colors[index % colors.length]);
            double startX = random.nextDouble() * confetti.getWidth();
            Duration duration = Duration.millis(1800 + random.nextInt(1200));
            TranslateTransition fall = new TranslateTransition(duration, piece);
            fall.setFromX(startX);
            fall.setToX(startX + random.nextDouble() * 160 - 80);
            fall.setFromY(-20 - random.nextInt(90));
            fall.setToY(confetti.getHeight() + 30);
            RotateTransition spin = new RotateTransition(duration, piece);
            spin.setByAngle(360 + random.nextInt(540));
            FadeTransition fade = new FadeTransition(Duration.millis(650), piece);
            fade.setDelay(duration.subtract(Duration.millis(650)));
            fade.setFromValue(1);
            fade.setToValue(0);
            confetti.getChildren().add(piece);
            burst.getChildren().addAll(fall, spin, fade);
        }
        burst.setOnFinished(event -> confetti.getChildren().clear());
        burst.playFromStart();
    }

    private HBox member(String name, String role, String joke, String github) {
        ImageView photo = new ImageView(new Image(CreditsDialog.class.getResource(
                "/images/team/" + github.toLowerCase(java.util.Locale.ROOT) + ".png").toExternalForm(),
                84, 84, true, true));
        photo.setFitWidth(84);
        photo.setFitHeight(84);
        photo.setPreserveRatio(true);
        photo.setAccessibleText(name + "'s team photo");
        photo.getStyleClass().add("credits-photo");
        StackPane portrait = new StackPane(photo);
        portrait.setMinSize(88, 88);
        portrait.setMaxSize(88, 88);
        portrait.getStyleClass().add("credits-portrait");
        Label nameLabel = new Label(name);
        nameLabel.setWrapText(true);
        nameLabel.getStyleClass().add("credits-name");
        Label roleLabel = new Label(role);
        roleLabel.setWrapText(true);
        roleLabel.getStyleClass().add("credits-role");
        Label jokeLabel = new Label(joke);
        jokeLabel.setWrapText(true);
        String url = "https://github.com/" + github;
        Button chat = new Button("CHAT NOW");
        chat.getStyleClass().add("credits-chat");
        chat.setMinWidth(Region.USE_PREF_SIZE);
        chat.setAccessibleText("CHAT NOW — Open " + name + "'s GitHub profile");
        Label linkFailure = new Label();
        linkFailure.setWrapText(true);
        linkFailure.setVisible(false);
        linkFailure.setManaged(false);
        chat.setOnAction(event -> {
            try {
                openProfile.accept(url);
            } catch (RuntimeException failure) {
                linkFailure.setText("Open this profile in your browser: " + url);
                linkFailure.setVisible(true);
                linkFailure.setManaged(true);
            }
        });
        VBox details = new VBox(6, nameLabel, roleLabel, jokeLabel, chat, linkFailure);
        HBox.setHgrow(details, Priority.ALWAYS);
        details.setMinWidth(0);
        HBox card = new HBox(14, portrait, details);
        card.setAlignment(Pos.TOP_LEFT);
        card.getStyleClass().add("credits-card");
        return card;
    }

    private static void browseProfile(String url) {
        browseProfile(url, target -> Desktop.getDesktop().browse(target));
    }

    static void browseProfile(String url, ProfileBrowser browser) {
        try {
            browser.browse(URI.create(url));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    /** Browser boundary, allowing URI delivery and launch failures to be checked without opening real tabs. */
    @FunctionalInterface
    interface ProfileBrowser {
        /** Opens the requested URI, or reports that the browser could not be launched. */
        void browse(URI uri) throws IOException;
    }
}
