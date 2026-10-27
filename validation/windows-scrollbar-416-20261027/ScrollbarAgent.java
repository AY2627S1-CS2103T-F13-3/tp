import java.awt.image.BufferedImage;
import java.lang.instrument.Instrumentation;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.Region;
import javafx.scene.robot.Robot;
import javafx.stage.*;

/** Separate diagnostic, adapted from the team's Round1Agent. Ernest Chua with OpenAI Codex assistance. */
public class ScrollbarAgent {
    static Path out;
    static Stage stage;
    static Robot robot;
    static final List<String> log = new ArrayList<>();
    static final String LONG_NAME = "a deliberately long fictional person name for wrapping validation";
    static final String LONG_ADDRESS = "123 fictional avenue apartment 456 near the imaginary community learning centre singapore";
    static final String LONG_EMAIL = "fictional.very.long.contact.for.wrapping@example.com";

    public static void premain(String arg, Instrumentation ignored) {
        out = Path.of(arg);
        Thread worker = new Thread(ScrollbarAgent::run, "windows-scrollbar-recheck");
        worker.setDaemon(true);
        worker.start();
    }

    static void run() {
        boolean pass = false;
        try {
            Files.createDirectories(out);
            record("Time=" + Instant.now() + "; Java=" + System.getProperty("java.runtime.version")
                + "; vendor=" + System.getProperty("java.vendor") + "; OS=" + System.getProperty("os.name")
                + "; arch=" + System.getProperty("os.arch"));
            var mode = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
            record("Physical display mode=" + mode.getWidth() + "x" + mode.getHeight());
            for (int i = 0; i < 100 && stage == null; i++) {
                try {
                    stage = fx(() -> Window.getWindows().stream().filter(w -> w instanceof Stage && w.isShowing()
                        && w.getScene() != null && w.getScene().lookup("#commandTextField") != null)
                        .map(Stage.class::cast).findFirst().orElse(null));
                } catch (IllegalStateException starting) { }
                if (stage == null) Thread.sleep(200);
            }
            require(stage != null, "App did not launch");
            Thread.sleep(2200);
            fx(() -> { robot = new Robot(); stage.setAlwaysOnTop(true); stage.toFront(); stage.requestFocus();
                record("JavaFX=" + System.getProperty("javafx.runtime.version"));
                for (Screen s : Screen.getScreens()) record("Screen=" + s.getBounds()
                    + "; visual=" + s.getVisualBounds() + "; outputScale=" + s.getOutputScaleX()
                    + "x" + s.getOutputScaleY());
                return null; });
            for (int i = 0; i < 30 && !fx(() -> stage.isFocused()); i++) {
                fx(() -> { stage.setAlwaysOnTop(true); stage.toFront(); stage.requestFocus(); return null; }); Thread.sleep(100);
            }
            require(!Files.exists(Path.of("data/ponhub.json")), "Fresh writable store required");
            command("add r/tutor n/mei lim p/92345678");
            command("add r/student n/alex tan l/p1 pp/91234567");
            command("add r/student n/jamie tan l/s2 pp/91234568");
            command("list r/student");
            require(fx(() -> people().getItems().size()) == 2, "Original short fixture missing");
            checkLayout("original-short-default");
            screen("01-original-short-default");
            dragDivider(3.2);
            checkLayout("original-fractional-native-narrower-viewport");
            screen("01b-original-fractional-viewport");
            resize(640, 480);
            checkLayout("original-short-compact");
            screen("02-original-short-compact");
            resize(740, 600);

            command("add r/parent n/pat tan p/91234567");
            command("add r/tutor n/" + LONG_NAME + " p/92345679 e/" + LONG_EMAIL + " a/" + LONG_ADDRESS);
            command("add r/parent n/" + LONG_NAME + " p/91234569 e/" + LONG_EMAIL + " a/" + LONG_ADDRESS);
            command("add r/student n/" + LONG_NAME + " l/p1 pp/91234569 e/" + LONG_EMAIL + " a/" + LONG_ADDRESS);
            String[] roles = {"tutor", "parent", "student"};
            for (String role : roles) {
                command("list r/" + role);
                checkLayout(role + "-default");
                ScrollBar bar = verticalBar();
                dragScrollBar(bar, true);
                assertLongCard(role);
                checkLayout(role + "-long-bottom-default");
                screen("03-" + role + "-long-default");
                resize(640, 480);
                dragScrollBar(verticalBar(), false);
                screen("04-" + role + "-long-compact-top");
                dragScrollBar(verticalBar(), true);
                checkLayout(role + "-long-compact");
                assertLongCard(role);
                screen("04-" + role + "-long-compact");
                resize(740, 600);
            }

            command("search c/student n/jamie");
            require(fx(() -> people().getItems().size()) == 1, "Filter not applied");
            require(fx(() -> labels().stream().anyMatch(l -> l.getText().equals("Position: 1"))), "Filtered position did not reindex");
            require(fx(() -> labels().stream().anyMatch(l -> l.getText().contains("S2"))), "Stable student ID missing");
            checkLayout("filtered-jamie-position-one-id-s2");
            screen("05-filtered-reindexed");
            command("list r/student");
            nativeSelectFirst();
            Object selected = fx(() -> people().getSelectionModel().getSelectedItem());
            List<?> visible = fx(() -> List.copyOf(people().getItems()));
            byte[] saved = Files.readAllBytes(Path.of("data/ponhub.json"));
            String feedback = feedback();
            typeDraft("unfinished draft");
            dragDivider(45);
            checkLayout("native-divider-narrower");
            require(input().equals("unfinished draft"), "Divider lost draft");
            require(feedback().equals(feedback), "Divider changed feedback");
            dragScrollBar(verticalBar(), true);
            require(fx(() -> people().getSelectionModel().getSelectedItem()) == selected, "Native scroll lost selection");
            checkLayout("native-scroll-bottom");
            assertLongCard("student");
            screen("06-native-divider-scroll-bottom");
            dragScrollBar(verticalBar(), false);
            Node copy = fx(() -> people().lookupAll(".person-contact-copy").stream()
                .filter(n -> n.isVisible() && n.localToScreen(n.getBoundsInLocal()) != null).findFirst().orElseThrow());
            Bounds copyBounds = fx(() -> copy.localToScreen(copy.getBoundsInLocal()));
            nativeInput(() -> robot.mouseMove(copyBounds.getCenterX(), copyBounds.getCenterY()));
            Thread.sleep(1400);
            require(fx(() -> Window.getWindows().stream().anyMatch(w -> w instanceof PopupWindow && w.isShowing())), "Native contact tooltip did not show");
            screen("07-native-card-tooltip");
            nativeInput(() -> robot.mouseMove(copyBounds.getMaxX() + 100, copyBounds.getMinY() - 30));
            key(KeyCode.ESCAPE);
            typeDraft("unfinished draft");
            key(KeyCode.F1);
            require(input().equals("unfinished draft"), "Native F1 lost draft");
            require(feedback().startsWith("PonHub commands available"), "Native F1 did not open help feedback");
            require(fx(() -> people().getSelectionModel().getSelectedItem()) == selected, "F1 lost selection");
            require(fx(() -> visible.equals(people().getItems())), "Layout/shortcut changed visible order");
            require(Arrays.equals(saved, Files.readAllBytes(Path.of("data/ponhub.json"))), "Read-only interactions changed stored bytes");
            checkLayout("final-state-preserved");
            screen("08-final-preserved-state");
            record("PASS: original short-card reproduction absent at default and compact windows; long values in all roles; native vertical thumb drags reach last card; native divider drag; filtered position/stable ID; pointer tooltip; F1; draft/selection/order/data preserved");
            record("Limits: actual Windows display scale only; no system display changes, injected scale, other-platform or independent human acceptance. Round 1 build, not final versioned Round 2 candidate.");
            pass = true;
        } catch (Throwable t) {
            record("FAIL=" + t);
            for (var line : t.getStackTrace()) record("  " + line);
            try { screen("failure"); } catch (Exception ignored) { }
        } finally {
            record("Windows scrollbar recheck=" + (pass ? "PASS" : "FAIL"));
            try { Files.write(out.resolve("result.txt"), log); } catch (Exception ignored) { }
            try { fx(() -> { if (robot != null) robot.mouseRelease(MouseButton.PRIMARY);
                if (stage != null) stage.hide(); Platform.exit(); return null; }); } catch (Exception ignored) { }
        }
    }

    static ListView<?> people() { return (ListView<?>) stage.getScene().lookup("#personListView"); }
    static TextField field() { return (TextField) stage.getScene().lookup("#commandTextField"); }
    static List<Label> labels() { return people().lookupAll(".label").stream().filter(Label.class::isInstance).map(Label.class::cast).toList(); }
    static String input() throws Exception { return fx(() -> field().getText()); }
    static String feedback() throws Exception { return fx(() -> ((TextArea) stage.getScene().lookup("#resultDisplay")).getText()); }
    static void resize(double w, double h) throws Exception {
        fx(() -> { stage.setWidth(w); stage.setHeight(h); return null; }); Thread.sleep(400);
    }
    static void checkLayout(String name) throws Exception {
        Thread.sleep(220);
        fx(() -> {
            record("LAYOUT=" + name + "; window=" + stage.getWidth() + "x" + stage.getHeight()
                + "; scene=" + stage.getScene().getWidth() + "x" + stage.getScene().getHeight()
                + "; renderScale=" + stage.getRenderScaleX() + "; peopleWidth=" + people().getWidth());
            for (Node n : people().lookupAll(".scroll-bar")) {
                ScrollBar b = (ScrollBar) n;
                record("  " + b.getOrientation() + "; visible=" + b.isVisible() + "; min=" + b.getMin()
                    + "; max=" + b.getMax() + "; value=" + b.getValue() + "; width=" + b.getWidth());
                if (b.getOrientation() == Orientation.HORIZONTAL) require(!b.isVisible(), "Phantom horizontal bar: " + name);
            }
            for (Node n : people().lookupAll(".list-cell")) {
                if (n instanceof ListCell<?> c && !c.isEmpty() && c.getGraphic() instanceof Region card) {
                    record("  cellIndex=" + c.getIndex() + "; width=" + c.getWidth() + "; pref=" + c.prefWidth(-1)
                        + "; cardWidth=" + card.getWidth() + "; height=" + card.getHeight());
                    require(c.prefWidth(-1) <= c.getWidth() + 0.01, "Cell exceeds available width: " + name);
                }
            }
            return null;
        });
    }
    static void assertLongCard(String role) throws Exception {
        fx(() -> {
            ListCell<?> last = people().lookupAll(".list-cell").stream()
                .filter(n -> n instanceof ListCell<?> c && !c.isEmpty()
                    && c.getIndex() == people().getItems().size() - 1
                    && c.getItem() == people().getItems().get(c.getIndex()))
                .map(n -> (ListCell<?>) n).findFirst().orElseThrow();
            List<Label> current = last.getGraphic().lookupAll(".label").stream()
                .filter(Label.class::isInstance).map(Label.class::cast).toList();
            for (String expected : List.of(LONG_NAME, LONG_EMAIL, LONG_ADDRESS)) {
                Label value = current.stream().filter(l -> l.getText().equals(expected)).findFirst().orElseThrow();
                require(value.isWrapText(), "Long field no longer wraps: " + role);
                require(value.getHeight() + 0.1 >= value.prefHeight(value.getWidth()), "Long field vertically clipped: " + role);
            }
            require(current.stream().anyMatch(l -> l.getText().equals("Position: " + people().getItems().size())), "Native scroll did not reach last card: " + role);
            record("Long complete values/last position=" + role + " PASS");
            return null;
        });
    }
    static ScrollBar verticalBar() throws Exception {
        return fx(() -> people().lookupAll(".scroll-bar").stream().map(ScrollBar.class::cast)
            .filter(b -> b.getOrientation() == Orientation.VERTICAL && b.isVisible()).findFirst().orElseThrow());
    }
    static void dragScrollBar(ScrollBar bar, boolean bottom) throws Exception {
        Bounds thumb = fx(() -> { Node n = bar.lookup(".thumb"); return n.localToScreen(n.getBoundsInLocal()); });
        Bounds track = fx(() -> bar.localToScreen(bar.getBoundsInLocal()));
        double before = fx(() -> bar.getValue());
        nativeInput(() -> { robot.mouseMove(thumb.getCenterX(), thumb.getCenterY()); robot.mousePress(MouseButton.PRIMARY); });
        double target = bottom ? track.getMaxY() - 12 : track.getMinY() + 12;
        for (int i = 1; i <= 10; i++) { final double y = thumb.getCenterY() + (target - thumb.getCenterY()) * i / 10;
            nativeInput(() -> robot.mouseMove(thumb.getCenterX(), y)); }
        nativeInput(() -> robot.mouseRelease(MouseButton.PRIMARY));
        double after = fx(() -> bar.getValue());
        record("Native vertical thumb drag=" + before + " -> " + after);
        require(bottom ? after > 0.99 : after < 0.01, "Native scrollbar did not reach requested end");
    }
    static void nativeSelectFirst() throws Exception {
        ListCell<?> cell = fx(() -> people().lookupAll(".list-cell").stream().filter(n -> n instanceof ListCell<?> c && !c.isEmpty() && c.getIndex() == 0).map(n -> (ListCell<?>) n).findFirst().orElseThrow());
        Bounds b = fx(() -> cell.localToScreen(cell.getBoundsInLocal()));
        nativeInput(() -> { robot.mouseMove(b.getMinX() + 35, b.getMinY() + 25); robot.mouseClick(MouseButton.PRIMARY); });
        require(fx(() -> people().getSelectionModel().getSelectedIndex()) == 0, "Native card selection failed");
    }
    static void dragDivider(double distance) throws Exception {
        SplitPane pane = fx(() -> (SplitPane) stage.getScene().lookup("#recordsSplitPane"));
        Node divider = fx(() -> pane.lookupAll(".split-pane-divider").stream().filter(n -> n.getParent() == pane).findFirst().orElseThrow());
        Bounds b = fx(() -> divider.localToScreen(divider.getBoundsInLocal()));
        double before = fx(() -> pane.getDividerPositions()[0]);
        nativeInput(() -> { robot.mouseMove(b.getCenterX(), b.getCenterY()); robot.mousePress(MouseButton.PRIMARY); });
        for (int i = 1; i <= 10; i++) { final double step = distance * i / 10;
            nativeInput(() -> robot.mouseMove(b.getCenterX() - step, b.getCenterY())); }
        nativeInput(() -> robot.mouseRelease(MouseButton.PRIMARY));
        double after = fx(() -> pane.getDividerPositions()[0]);
        require(Math.abs(after - before) > Math.min(0.03, distance / 1500), "Native divider did not move");
        record("Native records divider=" + before + " -> " + after);
    }
    static void typeDraft(String text) throws Exception {
        Bounds b = fx(() -> field().localToScreen(field().getBoundsInLocal()));
        nativeInput(() -> { robot.mouseMove(b.getCenterX(), b.getCenterY()); robot.mouseClick(MouseButton.PRIMARY); });
        chord(KeyCode.CONTROL, KeyCode.A); key(KeyCode.BACK_SPACE);
        nativeInput(() -> {
            for (char c : text.toCharArray()) {
                KeyCode k = switch (c) { case ' ' -> KeyCode.SPACE; case '/' -> KeyCode.SLASH;
                    case '-' -> KeyCode.MINUS; case '.' -> KeyCode.PERIOD; case '@' -> KeyCode.DIGIT2;
                    default -> KeyCode.getKeyCode(String.valueOf(Character.toUpperCase(c))); };
                if (k == null) throw new IllegalArgumentException("Unsupported fixture character=" + c);
                if (c == '@') robot.keyPress(KeyCode.SHIFT);
                robot.keyPress(k); robot.keyRelease(k);
                if (c == '@') robot.keyRelease(KeyCode.SHIFT);
            }
        });
        for (int i = 0; i < 30 && !input().equals(text); i++) Thread.sleep(100);
        require(input().equals(text), "Native text mismatch: " + input());
    }
    static void command(String text) throws Exception {
        typeDraft(text); key(KeyCode.ENTER); Thread.sleep(180);
        String reply = feedback(); record("COMMAND=" + text + "; FEEDBACK=" + reply.replace('\n', ' '));
        require(!reply.startsWith("Invalid") && !reply.startsWith("Error"), "Fixture command rejected");
    }
    static void key(KeyCode k) throws Exception { nativeInput(() -> { robot.keyPress(k); robot.keyRelease(k); }); }
    static void chord(KeyCode m, KeyCode k) throws Exception { nativeInput(() -> { robot.keyPress(m); robot.keyPress(k); robot.keyRelease(k); robot.keyRelease(m); }); }
    static void nativeInput(Runnable r) throws Exception {
        for (int i = 0; i < 8 && !fx(() -> stage.isFocused()); i++) { fx(() -> { stage.toFront(); stage.requestFocus(); return null; }); Thread.sleep(150); }
        require(fx(() -> stage.isFocused()), "Test app lost foreground");
        fx(() -> { r.run(); return null; }); Thread.sleep(90);
    }
    static void screen(String name) throws Exception {
        // Screen pixels from the native window, including any live popup; never a Node snapshot.
        WritableImage shot = fx(() -> {
            // Bound the real screen capture to the app's client area to exclude unrelated desktop content.
            Bounds b = stage.getScene().getRoot().localToScreen(stage.getScene().getRoot().getLayoutBounds());
            return robot.getScreenCapture(null, b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight(), false);
        });
        BufferedImage png = new BufferedImage((int) shot.getWidth(), (int) shot.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < png.getHeight(); y++) for (int x = 0; x < png.getWidth(); x++) png.setRGB(x, y, shot.getPixelReader().getArgb(x, y));
        ImageIO.write(png, "png", out.resolve(name + ".png").toFile());
    }
    static <T> T fx(Callable<T> action) throws Exception { FutureTask<T> task = new FutureTask<>(action); Platform.runLater(task); return task.get(10, TimeUnit.SECONDS); }
    static void record(String s) { log.add(s); System.out.println(s); }
    static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
