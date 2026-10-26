import java.awt.image.BufferedImage;
import java.lang.instrument.Instrumentation;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.robot.Robot;
import javafx.stage.*;

/** Diagnostic only. Ernest Chua's alpha session, with OpenAI Codex assistance. */
public class Round1Agent {
    static Path out;
    static Stage stage;
    static Robot robot;
    static final List<String> log = new ArrayList<>();
    static Map<DataFormat,Object> oldClipboard;
    public static void premain(String arg, Instrumentation ignored) {
        out=Path.of(arg);
        Thread worker=new Thread(Round1Agent::run,"round1-native");
        worker.setDaemon(true); worker.start();
    }
    static void run() {
        boolean pass=false;
        try {
            Files.createDirectories(out);
            record("Time="+Instant.now()+"; Java="+System.getProperty("java.runtime.version")
                +"; OS="+System.getProperty("os.name")+"; arch="+System.getProperty("os.arch"));
            for(int i=0;i<100&&stage==null;i++) {
                try { stage=fx(()->Window.getWindows().stream().filter(w->w instanceof Stage&&w.isShowing()
                    &&w.getScene()!=null&&w.getScene().lookup("#commandTextField")!=null)
                    .map(Stage.class::cast).findFirst().orElse(null)); } catch(IllegalStateException starting){}
                if(stage==null)Thread.sleep(200);
            }
            require(stage!=null,"App did not launch");
            fx(()->{robot=new Robot();stage.toFront();stage.requestFocus();
                for(Screen s:Screen.getScreens())record("Screen="+s.getBounds()+"; scale="+s.getOutputScaleX());
                record("Window="+stage.getWidth()+"x"+stage.getHeight()+"; scene="
                    +stage.getScene().getWidth()+"x"+stage.getScene().getHeight());return null;});
            for(int i=0;i<30&&!fx(()->stage.isFocused());i++){fx(()->{stage.toFront();stage.requestFocus();return null;});Thread.sleep(100);}
            require(!Files.exists(Path.of("data/ponhub.json")),"Fresh store required");
            command("add r/tutor n/mei lim p/92345678");
            command("add r/student n/alex tan l/p1 pp/91234567");
            command("add r/student n/jamie tan l/s2 pp/91234568");
            command("list r/student");
            screen("01-scrollbar-short-cards");
            record("People horizontal bars="+fx(()->stage.getScene().lookup("#personListView")
                .lookupAll(".scroll-bar").stream().map(n->(ScrollBar)n)
                .filter(b->b.getOrientation()==javafx.geometry.Orientation.HORIZONTAL)
                .map(b->"visible="+b.isVisible()+";max="+b.getMax()+";width="+b.getWidth()).toList()));

            record("#416 horizontal scrollbar visible="+fx(()->stage.getScene().lookup("#personListView").lookupAll(".scroll-bar").stream()
                .map(n->(ScrollBar)n).filter(b->b.getOrientation()==javafx.geometry.Orientation.HORIZONTAL)
                .anyMatch(Node::isVisible)));
            record("#416 observed default-window scrollbar; see actual screenshot");
            byte[] before=Files.readAllBytes(Path.of("data/ponhub.json"));
            fx(()->{stage.setWidth(640);stage.setHeight(480);return null;});
            Thread.sleep(350);
            screen("02-scrollbar-compact");
            record("Compact window="+fx(()->stage.getWidth()+"x"+stage.getHeight())
                +"; scene="+fx(()->stage.getScene().getWidth()+"x"+stage.getScene().getHeight()));
            record("#416 horizontal scrollbar visible="+fx(()->stage.getScene().lookup("#personListView").lookupAll(".scroll-bar").stream()
                .map(n->(ScrollBar)n).filter(b->b.getOrientation()==javafx.geometry.Orientation.HORIZONTAL)
                .anyMatch(Node::isVisible)));
            require(Arrays.equals(before,Files.readAllBytes(Path.of("data/ponhub.json"))),"Layout changed data");
            record("#416 compact-window observation complete; saved bytes preserved");
            Files.copy(Path.of("data/ponhub.json"),out.resolve("final-data.json"));
            record("Only disposable fictional data. Native Robot mouse/keyboard; no display settings changed.");
            record("Coverage: Windows/Java 25/default and compact windows/observed scale only; no independent human or other-platform acceptance.");
            pass=true;
        }catch(Throwable t){record("HARNESS FAILURE="+t);for(var line:t.getStackTrace())record("  "+line);try{screen("failure");}catch(Exception ignored){}}
        finally{
            if(oldClipboard!=null)try{fx(()->{Clipboard.getSystemClipboard().setContent(oldClipboard);return null;});}catch(Exception ignored){}
            record("Diagnostic completed="+pass);
            try{Files.write(out.resolve("result.txt"),log);}catch(Exception ignored){}
            try{fx(()->{stage.hide();Platform.exit();return null;});}catch(Exception ignored){}
        }
    }
    static String feedback()throws Exception{return fx(()->((TextArea)stage.getScene().lookup("#resultDisplay")).getText());}
    static void command(String text)throws Exception{
        TextField input=fx(()->(TextField)stage.getScene().lookup("#commandTextField"));
        Bounds b=fx(()->input.localToScreen(input.getBoundsInLocal()));
        nativeInput(()->{robot.mouseMove(b.getCenterX(),b.getCenterY());robot.mouseClick(MouseButton.PRIMARY);});
        chord(KeyCode.CONTROL,KeyCode.A);key(KeyCode.BACK_SPACE);
        nativeInput(()->{for(char c:text.toCharArray()){
            KeyCode k=c==' '?KeyCode.SPACE:c=='/'?KeyCode.SLASH:c=='-'?KeyCode.MINUS:
                KeyCode.getKeyCode(String.valueOf(Character.toUpperCase(c)));
            if(k==null)throw new IllegalArgumentException("Unsupported fixture char="+c);
            robot.keyPress(k);robot.keyRelease(k);
        }});
        require(fx(()->input.getText()).equals(text),"Native input mismatch; expected="+text+"; actual="+fx(()->input.getText())+"; inputFocused="+fx(()->input.isFocused()));
        key(KeyCode.ENTER);Thread.sleep(180);
        record("COMMAND="+text+"; FEEDBACK="+feedback().replace('\n',' '));
    }
    static void key(KeyCode k)throws Exception{nativeInput(()->{robot.keyPress(k);robot.keyRelease(k);});}
    static void chord(KeyCode m,KeyCode k)throws Exception{nativeInput(()->{robot.keyPress(m);robot.keyPress(k);robot.keyRelease(k);robot.keyRelease(m);});}
    static void nativeInput(Runnable r)throws Exception{require(fx(()->stage.isFocused()),"App lost foreground");fx(()->{r.run();return null;});Thread.sleep(110);}
    static void screen(String name)throws Exception{
        WritableImage shot=fx(()->robot.getScreenCapture(null,stage.getX(),stage.getY(),stage.getWidth(),stage.getHeight(),false));
        BufferedImage png=new BufferedImage((int)shot.getWidth(),(int)shot.getHeight(),BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<png.getHeight();y++)for(int x=0;x<png.getWidth();x++)png.setRGB(x,y,shot.getPixelReader().getArgb(x,y));
        ImageIO.write(png,"png",out.resolve(name+".png").toFile());
    }
    static <T>T fx(Callable<T> action)throws Exception{FutureTask<T> t=new FutureTask<>(action);Platform.runLater(t);return t.get(10,TimeUnit.SECONDS);}
    static void record(String s){log.add(s);System.out.println(s);}
    static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
}
