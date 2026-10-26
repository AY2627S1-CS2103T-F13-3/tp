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
            fx(()->{stage.setWidth(1000);stage.setHeight(700);return null;});
            Thread.sleep(400);
            Node divider=fx(()->stage.getScene().lookup("#resultSplitPane").lookup(".split-pane-divider"));
            Bounds dividerBounds=fx(()->divider.localToScreen(divider.getBoundsInLocal()));
            nativeInput(()->{robot.mouseMove(dividerBounds.getCenterX(),dividerBounds.getCenterY());robot.mousePress(MouseButton.PRIMARY);});
            for(int i=1;i<=10;i++){final int step=i;nativeInput(()->robot.mouseMove(dividerBounds.getCenterX(),dividerBounds.getCenterY()+step*10));}
            nativeInput(()->robot.mouseRelease(MouseButton.PRIMARY));
            record("Window after resize="+fx(()->stage.getWidth()+"x"+stage.getHeight())+"; feedback divider dragged with native mouse");
            command("add r/student n/alex tan l/p1 pp/91234567 p/81111111");
            command("add r/student n/jamie tan l/s2 pp/91234567 p/82222222");
            command("list r/student");
            checkDuplicate("student", "edit 2 n/alex tan", "parent phone (pp/)", "pp/", "01-student");
            command("edit 2 pp/90000000");
            require(feedback().startsWith("Edited student"),"Correction failed");
            command("edit 2 n/jamie tan");
            require(feedback().startsWith("No changes"),"Self identity became duplicate");
            for(String role:List.of("tutor","parent")) {
                command("add r/"+role+" n/pat tan p/91234567");
                command("add r/"+role+" n/kim tan p/91234567");
                command("list r/"+role);
                checkDuplicate(role,"edit 2 n/pat tan","phone (p/)","p/","02-"+role);
            }
            Files.copy(Path.of("data/ponhub.json"),out.resolve("final-data.json"));
            record("Only disposable fictional data. Native Robot mouse/keyboard; no display settings changed.");
            record("Coverage: Windows/Java 25/1000x700 window/observed scale only; no independent human or other-platform acceptance.");
            pass=true;
        }catch(Throwable t){record("HARNESS FAILURE="+t);for(var line:t.getStackTrace())record("  "+line);try{screen("failure");}catch(Exception ignored){}}
        finally{
            if(oldClipboard!=null)try{fx(()->{Clipboard.getSystemClipboard().setContent(oldClipboard);return null;});}catch(Exception ignored){}
            record("Diagnostic completed="+pass);
            try{Files.write(out.resolve("result.txt"),log);}catch(Exception ignored){}
            try{fx(()->{stage.hide();Platform.exit();return null;});}catch(Exception ignored){}
        }
    }
    static void checkDuplicate(String role,String cmd,String phone,String prefix,String picture)throws Exception {
        byte[] bytes=Files.readAllBytes(Path.of("data/ponhub.json"));
        command(cmd);
        require(feedback().equals("A "+role+" with the same normalized name and "+phone
            +" already exists. Change n/ or "+prefix+" to a unique combination."),"Wrong guidance: "+feedback());
        require(fx(()->((TextField)stage.getScene().lookup("#commandTextField")).getText()).equals(cmd),"Rejected edit lost");
        require(Arrays.equals(bytes,Files.readAllBytes(Path.of("data/ponhub.json"))),"Duplicate changed bytes");
        screen(picture);
        record("Duplicate "+role+": role identity guidance, failed-input retention and data preservation=PASS");
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
