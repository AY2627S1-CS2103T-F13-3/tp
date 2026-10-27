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
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.Robot;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Separate diagnostic agent, prepared by Ernest Chua with OpenAI Codex assistance. */
public class CompletionAgent {
    static final List<String> report = new ArrayList<>();
    static Path output;
    static Stage stage;
    static Robot robot;
    public static void premain(String arg, Instrumentation unused) {
        output = Path.of(arg);
        Thread worker = new Thread(CompletionAgent::audit, "completion-native-check");
        worker.setDaemon(true);
        worker.start();
    }
    static void audit() {
        boolean passed = false;
        try {
            Files.createDirectories(output);
            log("Time=" + Instant.now() + "; runtime=" + System.getProperty("java.runtime.version")
                    + "; vendor=" + System.getProperty("java.vendor") + "; OS=" + System.getProperty("os.name")
                    + "; arch=" + System.getProperty("os.arch"));
            for (int i = 0; i < 100 && stage == null; i++) {
                try { stage = fx(() -> Window.getWindows().stream().filter(w -> w instanceof Stage && w.isShowing()
                        && w.getScene() != null && w.getScene().lookup("#commandTextField") != null)
                        .map(Stage.class::cast).findFirst().orElse(null)); }
                catch (IllegalStateException starting) { }
                if (stage == null) Thread.sleep(200);
            }
            require(stage != null, "No application window");
            fx(() -> { robot = new Robot(); stage.toFront(); stage.requestFocus();
                for (Screen s : Screen.getScreens()) log("Screen=" + s.getBounds() + "; workArea="
                        + s.getVisualBounds() + "; scale=" + s.getOutputScaleX());
                log("Scene=" + stage.getScene().getWidth() + "x" + stage.getScene().getHeight()); return null; });
            if (Files.exists(Path.of("data/ponhub.json"))) {
                Thread.sleep(200);
                fx(() -> {
                    Stage dialog = Window.getWindows().stream().filter(w -> w instanceof Stage s
                            && s.isShowing() && s.isFocused() && s.getTitle().equals("Data file protected"))
                            .map(Stage.class::cast).findFirst().orElseThrow();
                    Node button = dialog.getScene().lookup(".button");
                    Bounds b = button.localToScreen(button.getBoundsInLocal());
                    robot.mouseMove(b.getCenterX(), b.getCenterY()); robot.mouseClick(MouseButton.PRIMARY);
                    return null;
                });
                Thread.sleep(200);
            }
            for (int i=0;i<30 && !fx(() -> stage.isFocused());i++) {
                fx(() -> {stage.toFront(); stage.requestFocus(); return null;}); Thread.sleep(100);
            }
            require(fx(() -> stage.isFocused()), "Application must be foreground before native input");
            boolean protectedSession = Files.exists(Path.of("data/ponhub.json"));
            if (protectedSession) {
                byte[] original = Files.readAllBytes(Path.of("data/ponhub.json"));
                for (String[] pair : new String[][] {{"h", "help"}, {"li", "list"}, {"e", "exit"}}) {
                    draft(pair[0]); key(KeyCode.TAB); expect(pair[1], pair[1].length(), true);
                }
                for (String prefix : List.of("ad", "sho")) {
                    draft(prefix); key(KeyCode.TAB); expect(prefix, prefix.length(), false);
                }
                screen("protected-session");
                require(Arrays.equals(original, Files.readAllBytes(Path.of("data/ponhub.json"))), "Rejected bytes changed");
                log("Protected completion routes and native unmatched traversal=PASS");
            } else {
                String initial = fx(() -> result().getText());
                draft("sho"); key(KeyCode.TAB); expect("showlesson", 10, true);
                require(initial.equals(fx(() -> result().getText())), "Tab changed feedback");
                draft("ad");
                key(KeyCode.TAB); expect("add", 3, true);
                key(KeyCode.TAB); expect("addlesson", 9, true);
                key(KeyCode.TAB); expect("add", 3, true);
                key(KeyCode.TAB); expect("addlesson", 9, true);
                key(KeyCode.LEFT); key(KeyCode.RIGHT); key(KeyCode.TAB); expect("addlesson", 9, true);
                draft("  sho  lid/l1  "); key(KeyCode.HOME);
                for (int i=0;i<5;i++) key(KeyCode.RIGHT);
                key(KeyCode.TAB); expect("  showlesson  lid/l1  ", 12, true);
                screen("command-word-and-arguments");
                chord(KeyCode.SHIFT, KeyCode.TAB);
                require(!fx(() -> input().isFocused()), "Shift+Tab did not traverse backwards");
                for (String value : List.of("", "unknown", "help sho")) {
                    draft(value); key(KeyCode.TAB); expect(value, value.length(), false);
                }
                draft("sho"); chord(KeyCode.CONTROL, KeyCode.A); key(KeyCode.TAB);
                require(fx(() -> input().getText()).equals("sho"), "Selection was replaced");
                draft("he"); key(KeyCode.TAB); expect("help", 4, true); key(KeyCode.ENTER);
                expect("", 0, true);
                require(fx(() -> result().getText()).contains("PonHub commands"), "Enter did not execute help");
                draft("sho lid/l1"); key(KeyCode.ENTER);
                require(fx(() -> input().getStyleClass().contains("error")), "Failed draft lost its error style");
                key(KeyCode.HOME); for (int i=0;i<3;i++) key(KeyCode.RIGHT); key(KeyCode.TAB);
                expect("showlesson lid/l1", 10, true);
                require(!fx(() -> input().getStyleClass().contains("error")), "Completion did not recover error style");
                key(KeyCode.F1); expect("showlesson lid/l1", 10, true);
                Node help = fx(() -> stage.getScene().lookup("#menuBar").lookupAll(".menu-button").stream()
                        .filter(n -> n.lookup(".label") instanceof javafx.scene.control.Label l
                                && l.getText().equals("Help")).findFirst().orElseThrow());
                click(help); screen("native-help-menu"); key(KeyCode.ENTER);
                require(fx(() -> input().getText()).equals("showlesson lid/l1"), "Menu help changed draft");
                require(!Files.exists(Path.of("data/ponhub.json")), "Tab/help created operational data");
                log("Native Tab/cycling/edit/caret/argument/selection/focus/Enter/error/F1/menu checks=PASS");
            }
            log("Only this Windows display/runtime combination was exercised; other platforms/scales NOT RUN.");
            log("Author checks assisted by Codex; independent teammate acceptance remains separate.");
            passed = true;
        } catch (Throwable failure) {
            log("FAIL=" + failure);
            for (StackTraceElement line : failure.getStackTrace()) log("  " + line);
            try { screen("failure"); } catch (Throwable ignored) { }
        } finally {
            log("Packaged completion checks=" + (passed ? "PASS" : "FAIL"));
            try { Files.write(output.resolve("result.txt"), report); } catch (Exception ignored) { }
            try { fx(() -> { stage.hide(); Platform.exit(); return null; }); } catch (Exception ignored) { }
        }
    }
    static TextField input() { return (TextField) stage.getScene().lookup("#commandTextField"); }
    static TextArea result() { return (TextArea) stage.getScene().lookup("#resultDisplay"); }
    static void draft(String text) throws Exception {
        click(fx(() -> input())); chord(KeyCode.CONTROL, KeyCode.A); key(KeyCode.BACK_SPACE);
        fx(() -> { for (char c : text.toCharArray()) {
            KeyCode code = c == ' ' ? KeyCode.SPACE : c == '/' ? KeyCode.SLASH
                    : KeyCode.getKeyCode(String.valueOf(Character.toUpperCase(c)));
            if (code == null) throw new IllegalArgumentException("Unsupported fixture character " + c);
            robot.keyPress(code); robot.keyRelease(code);
        } return null; });
        Thread.sleep(180);
        require(fx(() -> input().getText()).equals(text), "Native typing mismatch for '" + text
                + "'; actual='" + fx(() -> input().getText()) + "'");
    }
    static void expect(String text, int caret, boolean focused) throws Exception {
        fx(() -> { require(input().getText().equals(text), "Expected '" + text + "', actual '" + input().getText() + "'");
            if (focused) require(input().getCaretPosition() == caret, "Unexpected caret " + input().getCaretPosition());
            require(input().isFocused() == focused, "Unexpected focus for '" + text + "'"); return null; });
        log("Native result='" + text + "'; caret=" + fx(() -> input().getCaretPosition()) + "; focused=" + focused);
    }
    static void key(KeyCode key) throws Exception {
        require(fx(() -> stage.isFocused()), "Application lost foreground before key input");
        fx(() -> { robot.keyPress(key); robot.keyRelease(key); return null; }); Thread.sleep(150);
    }
    static void chord(KeyCode modifier, KeyCode key) throws Exception {
        require(fx(() -> stage.isFocused()), "Application lost foreground before key chord");
        fx(() -> { robot.keyPress(modifier); robot.keyPress(key); robot.keyRelease(key); robot.keyRelease(modifier);
            return null; }); Thread.sleep(150);
    }
    static void click(Node node) throws Exception {
        require(fx(() -> stage.isFocused()), "Application lost foreground before mouse input");
        fx(() -> { Bounds b = node.localToScreen(node.getBoundsInLocal()); robot.mouseMove(b.getCenterX(),b.getCenterY());
            robot.mouseClick(MouseButton.PRIMARY); return null; }); Thread.sleep(150);
    }
    static void screen(String name) throws Exception {
        WritableImage shot = fx(() -> {
            double x=stage.getX(), y=stage.getY(), maxX=x+stage.getWidth(), maxY=y+stage.getHeight();
            for (Window w : Window.getWindows()) if (w.isShowing() && w instanceof javafx.stage.PopupWindow) {
                x=Math.min(x,w.getX()); y=Math.min(y,w.getY()); maxX=Math.max(maxX,w.getX()+w.getWidth());
                maxY=Math.max(maxY,w.getY()+w.getHeight());
            }
            return robot.getScreenCapture(null,x,y,maxX-x,maxY-y,false);
        });
        BufferedImage png = new BufferedImage((int)shot.getWidth(),(int)shot.getHeight(),BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<png.getHeight();y++) for(int x=0;x<png.getWidth();x++) png.setRGB(x,y,shot.getPixelReader().getArgb(x,y));
        ImageIO.write(png,"png",output.resolve(name+".png").toFile());
    }
    static <T> T fx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action); Platform.runLater(task); return task.get(10,TimeUnit.SECONDS);
    }
    static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    static void log(String text) { report.add(text); System.out.println(text); }
}
