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
            command("add r/tutor n/mei lim p/92345678");
            command("add r/student n/alex tan l/p1 pp/91234567");
            command("add r/student n/jamie tan l/s2 pp/91234568");
            for(int hour=9;hour<15;hour++)command("addlesson d/mon st/"+String.format("%02d00",hour)
                +" et/"+String.format("%02d00",hour+1)+" s/math tu/mei lim rm/r1");
            command("list r/student");command("test");
            byte[] bytes=Files.readAllBytes(Path.of("data/ponhub.json"));
            String feedback=feedback();
            ListView<?> people=fx(()->(ListView<?>)stage.getScene().lookup("#personListView"));
            Node cell=fx(()->people.lookupAll(".list-cell").stream().filter(n->n instanceof ListCell<?> c&&!c.isEmpty()).findFirst().orElseThrow());
            Bounds cellBounds=fx(()->cell.localToScreen(cell.getBoundsInLocal()));
            nativeInput(()->{robot.mouseMove(cellBounds.getMinX()+30,cellBounds.getMinY()+20);robot.mouseClick(MouseButton.PRIMARY);});
            Object selected=fx(()->people.getSelectionModel().getSelectedItem());require(selected!=null,"Native selection failed");
            typeDraft("unfinished draft");screen("01-default");
            dragDivider("#resultSplitPane",false,75);
            dragDivider("#recordsSplitPane",true,55);
            require(input().equals("unfinished draft"),"Divider lost draft");
            require(feedback().equals(feedback),"Divider changed feedback");
            screen("02-dividers-dragged");
            ScrollBar peopleBar=verticalBar("#personListView"),lessonBar=verticalBar("#lessonListView");
            double lessonBefore=fx(()->lessonBar.getValue());
            dragScrollBar(peopleBar);
            require(fx(()->lessonBar.getValue())==lessonBefore,"People scrolling moved lesson list");
            double peopleBefore=fx(()->peopleBar.getValue());
            dragScrollBar(lessonBar);
            require(fx(()->peopleBar.getValue())==peopleBefore,"Lesson scrolling moved people list");
            require(fx(()->people.getSelectionModel().getSelectedItem())==selected,"Layout/scroll lost selection");
            require(Arrays.equals(bytes,Files.readAllBytes(Path.of("data/ponhub.json"))),"Layout changed stored bytes");
            typeDraft("unfinished draft");require(fx(()->field().isFocused()),"Input focus failed");
            screen("03-scrolled-focus");
            fx(()->{stage.setWidth(640);stage.setHeight(480);return null;});Thread.sleep(350);
            screen("04-compact");
            record("Native divider drags and independent scrollbars preserve draft, feedback, selection and saved bytes=PASS");
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
    static void dragDivider(String id,boolean horizontal,double distance)throws Exception {
        SplitPane pane=fx(()->(SplitPane)stage.getScene().lookup(id));
        Node divider=fx(()->pane.lookupAll(".split-pane-divider").stream().filter(n->n.getParent()==pane).findFirst().orElseThrow());
        Bounds b=fx(()->divider.localToScreen(divider.getBoundsInLocal()));
        double before=fx(()->pane.getDividerPositions()[0]);
        record(id+" hit area="+b.getWidth()+"x"+b.getHeight()+"; before="+before);
        nativeInput(()->{robot.mouseMove(b.getCenterX(),b.getCenterY());robot.mousePress(MouseButton.PRIMARY);});
        for(int i=1;i<=10;i++){final double step=distance*i/10;nativeInput(()->robot.mouseMove(b.getCenterX()+(horizontal?step:0),b.getCenterY()+(horizontal?0:step)));}
        nativeInput(()->robot.mouseRelease(MouseButton.PRIMARY));
        double after=fx(()->pane.getDividerPositions()[0]);
        require(Math.abs(after-before)>0.03,"Native divider did not move: "+id);
        record(id+" after="+after);
    }
    static ScrollBar verticalBar(String id)throws Exception {
        return fx(()->stage.getScene().lookup(id).lookupAll(".scroll-bar").stream().map(n->(ScrollBar)n)
            .filter(b->b.getOrientation()==javafx.geometry.Orientation.VERTICAL&&b.isVisible()).findFirst().orElseThrow());
    }
    static void dragScrollBar(ScrollBar bar)throws Exception {
        Bounds thumb=fx(()->{Node n=bar.lookup(".thumb");return n.localToScreen(n.getBoundsInLocal());});
        Bounds track=fx(()->bar.localToScreen(bar.getBoundsInLocal()));
        double before=fx(()->bar.getValue());
        nativeInput(()->{robot.mouseMove(thumb.getCenterX(),thumb.getCenterY());robot.mousePress(MouseButton.PRIMARY);});
        for(int i=1;i<=10;i++){final double y=thumb.getCenterY()+(track.getMaxY()-20-thumb.getCenterY())*i/10;nativeInput(()->robot.mouseMove(thumb.getCenterX(),y));}
        nativeInput(()->robot.mouseRelease(MouseButton.PRIMARY));
        require(Math.abs(fx(()->bar.getValue())-before)>0.001,"Native scrollbar did not move");
        record("Native scrollbar "+bar.getParent().getClass().getSimpleName()+" moved from="+before+" to="+fx(()->bar.getValue()));
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
