import java.awt.image.BufferedImage;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.robot.Robot;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Bounded acceptance of the actual packaged application; no production instrumentation changes. */
public class CompactFeedbackAgent {
    static final List<String> report = new ArrayList<>();
    static Stage stage;
    static Robot robot;
    static Path output;
    public static void premain(String arg, Instrumentation instrumentation) {
        output = Path.of(arg);
        Thread worker = new Thread(CompactFeedbackAgent::audit, "compact-feedback-363-audit");
        worker.setDaemon(true);
        worker.start();
    }
    static void audit() {
        boolean passed = false;
        try {
            Files.createDirectories(output);
            log("Time=" + Instant.now() + "; Java=" + System.getProperty("java.runtime.version")
                    + "; vendor=" + System.getProperty("java.vendor") + "; OS="
                    + System.getProperty("os.name") + "; arch=" + System.getProperty("os.arch"));
            for (int i = 0; i < 100 && stage == null; i++) {
                try {
                    stage = fx(() -> Window.getWindows().stream().filter(w -> w instanceof Stage
                            && w.isShowing() && w.getScene() != null
                            && w.getScene().lookup("#commandTextField") != null).map(w -> (Stage) w)
                            .findFirst().orElse(null));
                } catch (IllegalStateException starting) { }
                if (stage == null) Thread.sleep(200);
            }
            require(stage != null, "Application did not show its production window");
            fx(() -> {
                robot = new Robot();
                stage.toFront();
                stage.requestFocus();
                for (Screen screen : Screen.getScreens()) log("Screen=" + screen.getBounds()
                        + "; workArea=" + screen.getVisualBounds() + "; outputScale=" + screen.getOutputScaleX());
                return null;
            });
            Thread.sleep(500);
            require(!Files.exists(Path.of("data/ponhub.json")), "Startup must remain an empty unwritten store");
            compact("fresh-startup");
            capture("01-fresh-startup");
            command("add r/tutor n/Mei Lim p/92345678");
            command("add r/student n/Alex Tan l/S2 pp/91234567");
            command("addlesson d/Mon st/1600 et/1730 s/Math tu/Mei Lim rm/R1");
            command("list");
            byte[] saved = Files.readAllBytes(Path.of("data/ponhub.json"));
            compact("short-feedback");
            fx(() -> { stage.setMaximized(true); return null; });
            Thread.sleep(500);
            compact("maximized");
            capture("02-maximized");
            command("help search");
            double compactHeight = height();
            scrollToEnd();
            require(Math.abs(height() - compactHeight) <= 1, "Long help moved the divider");
            drag(0.55);
            require(height() > compactHeight + 80, "Native divider drag did not expand feedback");
            double expanded = height();
            command("list");
            command("help search");
            require(Math.abs(height() - expanded) <= 1, "Commands changed the chosen height");
            capture("03-expanded-search-help");
            drag(0.01);
            compact("drag-shrunk");
            fx(() -> { stage.setMaximized(false); stage.setWidth(450); stage.setHeight(360); return null; });
            Thread.sleep(500);
            compact("compact-window");
            command("help search");
            scrollToEnd();
            fx(() -> { input().setText("add r/student n/Unfinished"); input().requestFocus();
                robot.keyPress(KeyCode.F1); robot.keyRelease(KeyCode.F1); return null; });
            Thread.sleep(300);
            require(fx(() -> input().getText().equals("add r/student n/Unfinished")), "F1 changed the draft");
            compact("compact-F1-draft");
            capture("04-compact-F1-draft");
            require(Arrays.equals(saved, Files.readAllBytes(Path.of("data/ponhub.json"))),
                    "Viewing, help or resizing changed operational data");
            log("Commands use the production CommandBox action handler. Divider drags and F1 use JavaFX Robot.");
            log("Display settings observed only; other OS/scaling configurations remain NOT RUN.");
            passed = true;
        } catch (Throwable e) {
            log("FAIL=" + e);
            for (StackTraceElement line : e.getStackTrace()) log("  " + line);
            try { capture("failure"); } catch (Throwable ignored) { }
        } finally {
            log("Packaged affected checks=" + (passed ? "PASS" : "FAIL"));
            try { Files.write(output.resolve("result.txt"), report); } catch (Exception ignored) { }
            try { fx(() -> { robot.mouseRelease(MouseButton.PRIMARY); stage.hide(); Platform.exit(); return null; }); }
            catch (Exception ignored) { }
        }
    }
    static Node node(String id) { return stage.getScene().lookup("#" + id); }
    static TextField input() { return (TextField) node("commandTextField"); }
    static TextArea result() { return (TextArea) node("resultDisplay"); }
    static double height() throws Exception { return fx(() -> ((Region) node("feedbackSection")).getHeight()); }
    static void compact(String label) throws Exception {
        fx(() -> {
            double h = ((Region) node("feedbackSection")).getHeight();
            log(label + ": scene=" + stage.getScene().getWidth() + "x" + stage.getScene().getHeight()
                    + "; feedback=" + h + "; records=" + ((Region) node("recordsSplitPane")).getHeight());
            require(h >= 80 && h <= 100, "Feedback is not compact: " + h);
            require(((Region) node("recordsSplitPane")).getHeight() >= 120, "Records lost their minimum");
            for (String id : List.of("viewNavigation", "commandTextField", "resultDisplay", "personListView",
                    "lessonListView", "statusbarPlaceholder")) {
                Bounds b = node(id).localToScene(node(id).getBoundsInLocal());
                require(b.getWidth() > 0 && b.getHeight() > 0 && b.getMinY() >= 0
                        && b.getMaxY() <= stage.getScene().getHeight() + 1, "Unreachable " + id + ": " + b);
            }
            return null;
        });
    }
    static void command(String text) throws Exception {
        fx(() -> { input().setText(text); Event.fireEvent(input(), new ActionEvent()); return null; });
        Thread.sleep(150);
        log("Command=" + text + "; feedbackHeight=" + height());
    }
    static void scrollToEnd() throws Exception {
        fx(() -> {
            ScrollBar bar = result().lookupAll(".scroll-bar").stream().map(ScrollBar.class::cast)
                    .filter(b -> b.getOrientation() == Orientation.VERTICAL).findFirst().orElseThrow();
            require(bar.isVisible(), "Long search help has no scrollbar");
            bar.setValue(bar.getMax());
            return null;
        });
        Thread.sleep(150);
        fx(() -> {
            ScrollPane scroll = (ScrollPane) result().lookup(".scroll-pane");
            Bounds viewport = scroll.lookup(".viewport").localToScene(scroll.lookup(".viewport").getBoundsInLocal());
            Bounds content = scroll.getContent().localToScene(scroll.getContent().getBoundsInLocal());
            require(result().getScrollTop() > 0 && content.getMaxY() >= viewport.getMinY()
                    && content.getMaxY() <= viewport.getMaxY() + 2, "Search help end is unreachable");
            log("Complete search-help end reached; characters=" + result().getLength());
            return null;
        });
    }
    static void drag(double fraction) throws Exception {
        Point2D start = fx(() -> {
            SplitPane split = (SplitPane) node("resultSplitPane");
            Node divider = split.lookupAll(".split-pane-divider").stream()
                    .filter(n -> n.getParent() == split).findFirst().orElseThrow();
            Bounds b = divider.localToScreen(divider.getBoundsInLocal());
            return new Point2D(b.getCenterX(), b.getCenterY());
        });
        Point2D end = fx(() -> {
            Region split = (Region) node("resultSplitPane");
            return split.localToScreen(split.getWidth() / 2, split.getHeight() * fraction);
        });
        fx(() -> { robot.mouseMove(start); robot.mousePress(MouseButton.PRIMARY); return null; });
        for (int i = 1; i <= 20; i++) {
            double y = start.getY() + (end.getY() - start.getY()) * i / 20;
            fx(() -> { robot.mouseMove(start.getX(), y); return null; });
            Thread.sleep(25);
        }
        fx(() -> { robot.mouseRelease(MouseButton.PRIMARY); return null; });
        Thread.sleep(200);
        log("Native drag fraction=" + fraction + "; feedbackHeight=" + height());
    }
    static void capture(String name) throws Exception {
        WritableImage shot = fx(() -> robot.getScreenCapture(null, stage.getX(), stage.getY(),
                stage.getWidth(), stage.getHeight(), false));
        BufferedImage image = new BufferedImage((int) shot.getWidth(), (int) shot.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++)
            image.setRGB(x, y, shot.getPixelReader().getArgb(x, y));
        ImageIO.write(image, "png", output.resolve(name + ".png").toFile());
    }
    static void require(boolean condition, String text) { if (!condition) throw new AssertionError(text); }
    static void log(String text) { report.add(text); System.out.println(text); }
    static <T> T fx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }
}
