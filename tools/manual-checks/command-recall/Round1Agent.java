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
            Thread.sleep(300);
            key(KeyCode.UP);
            require(input().isEmpty(),"History unexpectedly persisted");
            if(out.getFileName().toString().equals("restart")) {
                screen("restart-empty-history");
                record("Same-folder restart command history=EMPTY");
                pass=true;return;
            }
            command("help list");
            command("list r/student");
            String previousFeedback=feedback();
            typeDraft("unfinished draft");
            key(KeyCode.HOME);
            for(int i=0;i<3;i++)key(KeyCode.RIGHT);
            chord(KeyCode.SHIFT,KeyCode.RIGHT);
            chord(KeyCode.SHIFT,KeyCode.RIGHT);
            int anchor=fx(()->field().getAnchor()),caret=fx(()->field().getCaretPosition());
            key(KeyCode.UP);require(input().equals("list r/student"),"Newest recall mismatch");
            key(KeyCode.UP);require(input().equals("help list"),"Older recall mismatch");
            key(KeyCode.UP);require(input().equals("help list"),"Oldest boundary mismatch");
            key(KeyCode.DOWN);key(KeyCode.DOWN);
            require(input().equals("unfinished draft"),"Draft lost");
            require(fx(()->field().getAnchor())==anchor&&fx(()->field().getCaretPosition())==caret,"Draft selection lost");
            require(feedback().equals(previousFeedback),"Recall executed a command");
            require(!Files.exists(Path.of("data/ponhub.json")),"Recall saved data");
            screen("01-restored-draft-selection");
            typeDraft("sho");key(KeyCode.TAB);require(input().equals("showlesson"),"Tab no longer completes");
            key(KeyCode.UP);require(input().equals("list r/student"),"Recall after Tab failed");
            key(KeyCode.DOWN);require(input().equals("showlesson"),"Completed draft lost");
            typeDraft("unknown");key(KeyCode.ENTER);Thread.sleep(150);
            require(feedback().contains("Unknown command"),"Failed control did not fail");
            typeDraft("help lis");key(KeyCode.UP);require(input().equals("unknown"),"Failed command not retained");
            key(KeyCode.DOWN);require(input().equals("help lis"),"Failed-history draft lost");
            key(KeyCode.T);require(input().equals("help list"),"Recalled draft cannot be edited");
            key(KeyCode.ENTER);Thread.sleep(150);
            require(input().isEmpty()&&feedback().contains("Use list or list r/"),"Enter did not execute edited text");
            key(KeyCode.UP);require(input().equals("help list"),"Edited submission missing from recall");
            require(fx(()->field().isFocused()),"Recall lost command focus");
            screen("02-recalled-edited-submission");
            record("Native Up/Down order, boundaries, draft/selection preservation, no execution/write, Tab, failure recall and Enter=PASS");

            record("Only disposable fictional data. Native Robot mouse/keyboard; no display settings changed.");
            record("Coverage: Windows/Java 25/default window/observed scale only; no independent human or other-platform acceptance.");
            pass=true;
        }catch(Throwable t){record("HARNESS FAILURE="+t);for(var line:t.getStackTrace())record("  "+line);try{screen("failure");}catch(Exception ignored){}}
        finally{
            if(oldClipboard!=null)try{fx(()->{Clipboard.getSystemClipboard().setContent(oldClipboard);return null;});}catch(Exception ignored){}
            record("Diagnostic completed="+pass);
            try{Files.write(out.resolve("result.txt"),log);}catch(Exception ignored){}
            try{fx(()->{stage.hide();Platform.exit();return null;});}catch(Exception ignored){}
        }
    }
    static TextField field(){return (TextField)stage.getScene().lookup("#commandTextField");}
    static String input()throws Exception{return fx(()->field().getText());}
    static String feedback()throws Exception{return fx(()->((TextArea)stage.getScene().lookup("#resultDisplay")).getText());}
    static void typeDraft(String text)throws Exception{
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
    }
    static void command(String text)throws Exception{
        typeDraft(text);key(KeyCode.ENTER);Thread.sleep(180);
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
