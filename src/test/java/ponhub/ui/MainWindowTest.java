package ponhub.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.FxTestUtil.callOnFxThread;
import static ponhub.testutil.FxTestUtil.runOnFxThread;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.animation.PauseTransition;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import ponhub.logic.CanonicalCommandCatalog;
import ponhub.logic.CanonicalLogicManager;
import ponhub.logic.CanonicalPeopleSession;
import ponhub.model.PonHubDataState;
import ponhub.model.UserPrefs;
import ponhub.model.person.Address;
import ponhub.model.person.ContactDetails;
import ponhub.model.person.EducationLevel;
import ponhub.model.person.Email;
import ponhub.model.person.Name;
import ponhub.model.person.PeopleRegistry;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.Phone;
import ponhub.model.person.Student;
import ponhub.storage.JsonPonHubDataStorage;
import ponhub.testutil.FxTestUtil;

public class MainWindowTest {

    private static final Student LONG_PERSON = new Student(new PersonId("S1"),
            new ContactDetails(new Name("Alexandria ".repeat(9).strip()),
                    Optional.of(new Phone("000123456789012")),
                    Optional.of(new Email("longcontact".repeat(20) + "@example.com")),
                    Optional.of(new Address("12 " + "A long residential road ".repeat(8).strip()))),
            new EducationLevel("P1"), new Phone("009876543210123"));
    private static final String LONG_PERSON_IDENTITY = "Student · ID: S1";
    private static final List<ContactDetails> TUTOR_CONTACTS = List.of(
            contacts("Alice Pauline", "94351253", "alice@example.com", "123, Jurong West Ave 6, #08-111"),
            contacts("Benson Meier", "98765432", "johnd@example.com", "311, Clementi Ave 2, #02-25"),
            contacts("Carl Kurz", "95352563", "heinz@example.com", "wall street"),
            contacts("Daniel Meier", "87652533", "cornelia@example.com", "10th street"),
            contacts("Elle Meyer", "9482224", "werner@example.com", "michegan ave"),
            contacts("Fiona Kunz", "9482427", "lydia@example.com", "little tokyo"),
            contacts("George Best", "9482442", "anna@example.com", "4th street"));

    @TempDir
    public Path tempDirectory;

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxTestUtil.initializeToolkit();
    }

    @Test
    public void logoClick_sixtyNinePrimaryClicks_opensCreditsWithoutChangingDraftOrSavedData() throws Exception {
        callOnFxThread(() -> {
            MainWindow window = createWindow();
            try {
                Region root = layoutWindow(window, 1000, 700);
                Node logo = root.lookup("#ponhubWordmark");
                Label badge = (Label) root.lookup(".wordmark-hub");
                assertEquals("hub", badge.getText());
                double badgeWidth = badge.getWidth();
                TextField command = (TextField) root.lookup("#commandTextField");
                command.setText("unfinished draft");
                command.requestFocus();
                command.selectRange(9, 2);
                byte[] before = Files.readAllBytes(tempDirectory.resolve("contacts.json"));
                for (int click = 0; click < 68; click++) {
                    logo.getOnMouseClicked().handle(logoClick(MouseButton.PRIMARY));
                    assertEquals(Integer.toString(click + 1), badge.getText());
                    root.layout();
                    assertEquals(badgeWidth, badge.getWidth());
                }
                logo.getOnMouseClicked().handle(logoClick(MouseButton.SECONDARY));
                assertEquals("68", badge.getText());
                assertTrue(Window.getWindows().stream().noneMatch(candidate -> candidate.getScene() != null
                        && candidate.getScene().getRoot() instanceof DialogPane));
                // Dialog creation can outlast the badge's 850 ms hold/fade and process its reset pulse.
                List<String> thresholdChanges = new ArrayList<>();
                ChangeListener<String> counterListener = (observable, previous, current) ->
                        thresholdChanges.add(current);
                badge.textProperty().addListener(counterListener);
                try {
                    logo.getOnMouseClicked().handle(logoClick(MouseButton.PRIMARY));
                } finally {
                    badge.textProperty().removeListener(counterListener);
                }
                assertFalse(thresholdChanges.isEmpty(), "The threshold click must update the counter");
                assertEquals("69", thresholdChanges.getFirst(), "The threshold must display before credits open");
                DialogPane credits = (DialogPane) Window.getWindows().stream()
                        .filter(candidate -> candidate.getScene() != null
                                && candidate.getScene().getRoot() instanceof DialogPane)
                        .findFirst().orElseThrow().getScene().getRoot();
                assertTrue(credits.getStyleClass().contains("credits-dialog"));
                Label heading = (Label) credits.lookup(".credits-heading");
                assertEquals("HOT SINGLES IN YOUR AREA!!!!", heading.getText());
                assertEquals(5, credits.lookupAll(".credits-card").size());
                Pane confetti = (Pane) credits.lookup(".credits-confetti");
                assertTrue(confetti.isMouseTransparent());
                assertEquals(69, confetti.getChildren().size());
                assertEquals("unfinished draft", command.getText());
                assertEquals(9, command.getAnchor());
                assertEquals(2, command.getCaretPosition());
                assertArrayEquals(before, Files.readAllBytes(tempDirectory.resolve("contacts.json")));
                Window creditsWindow = credits.getScene().getWindow();
                Button close = (Button) credits.lookupButton(credits.getButtonTypes().getFirst());
                close.fire();
                assertFalse(creditsWindow.isShowing());
                assertTrue(confetti.getChildren().isEmpty());
                assertEquals("hub", badge.getText());
                for (int click = 0; click < 69; click++) {
                    logo.getOnMouseClicked().handle(logoClick(MouseButton.PRIMARY));
                    if (click == 0) {
                        assertEquals("1", badge.getText());
                    }
                }
                assertEquals(1, Window.getWindows().stream().filter(candidate -> candidate.getScene() != null
                        && candidate.getScene().getRoot() instanceof DialogPane).count());
            } finally {
                Event.fireEvent(window.getPrimaryStage(),
                        new WindowEvent(window.getPrimaryStage(), WindowEvent.WINDOW_HIDDEN));
            }
            return null;
        });
    }

    @Test
    public void logoClick_repeatedActivation_keepsOneDialogAndRestoresNavigationFocus() throws Exception {
        MainWindow window = callOnFxThread(this::createWindow);
        try {
            callOnFxThread(() -> {
                window.getPrimaryStage().show();
                Region root = layoutWindow(window, 1000, 700);
                Node navigation = root.lookup("#lessonsButton");
                navigation.requestFocus();
                Node logo = root.lookup("#ponhubWordmark");
                for (int click = 0; click < 69; click++) {
                    logo.getOnMouseClicked().handle(logoClick(MouseButton.PRIMARY));
                }
                Window creditsWindow = Window.getWindows().stream()
                        .filter(candidate -> candidate.getScene() != null
                                && candidate.getScene().getRoot() instanceof DialogPane)
                        .findFirst().orElseThrow();
                for (int click = 0; click < 69; click++) {
                    logo.getOnMouseClicked().handle(logoClick(MouseButton.PRIMARY));
                }
                assertEquals(1, Window.getWindows().stream().filter(candidate -> candidate.getScene() != null
                        && candidate.getScene().getRoot() instanceof DialogPane).count());
                assertTrue(creditsWindow.isShowing());
                DialogPane pane = (DialogPane) creditsWindow.getScene().getRoot();
                Button close = (Button) pane.lookupButton(pane.getButtonTypes().getFirst());
                close.fire();
                return null;
            });
            runOnFxThread(() -> assertSame(window.getPrimaryStage().getScene().lookup("#lessonsButton"),
                    window.getPrimaryStage().getScene().getFocusOwner()));
        } finally {
            runOnFxThread(() -> window.getPrimaryStage().hide());
        }
    }

    @Test
    public void logoClick_dismissCredits_fadesNumberBackToHub() throws Exception {
        MainWindow window = callOnFxThread(this::createWindow);
        CompletableFuture<Void> finished = new CompletableFuture<>();
        try {
            callOnFxThread(() -> {
                window.getPrimaryStage().show();
                Region root = layoutWindow(window, 1000, 700);
                Node logo = root.lookup("#ponhubWordmark");
                Label badge = (Label) root.lookup(".wordmark-hub");
                for (int click = 0; click < 69; click++) {
                    logo.getOnMouseClicked().handle(logoClick(MouseButton.PRIMARY));
                }
                DialogPane credits = (DialogPane) Window.getWindows().stream()
                        .filter(candidate -> candidate.getScene() != null
                                && candidate.getScene().getRoot() instanceof DialogPane)
                        .findFirst().orElseThrow().getScene().getRoot();
                Button close = (Button) credits.lookupButton(credits.getButtonTypes().getFirst());
                close.fire();
                assertEquals("69", badge.getText(), "The number must fade out before the label changes.");
                PauseTransition wait = new PauseTransition(Duration.millis(1900));
                wait.setOnFinished(event -> {
                    try {
                        assertEquals("hub", badge.getText());
                        assertEquals(1, badge.lookup(".text").getOpacity(), 0.001);
                        finished.complete(null);
                    } catch (Throwable failure) {
                        finished.completeExceptionally(failure);
                    }
                });
                wait.play();
                return null;
            });
            finished.get(5, TimeUnit.SECONDS);
        } finally {
            runOnFxThread(() -> window.getPrimaryStage().hide());
        }
    }

    @Test
    public void logoClick_eachNumberFades_retainsCountAndCancelsEarlierReset() throws Exception {
        MainWindow window = callOnFxThread(this::createWindow);
        CompletableFuture<Void> finished = new CompletableFuture<>();
        try {
            callOnFxThread(() -> {
                window.getPrimaryStage().show();
                Region root = layoutWindow(window, 1000, 700);
                Node logo = root.lookup("#ponhubWordmark");
                Label badge = (Label) root.lookup(".wordmark-hub");
                logo.getOnMouseClicked().handle(logoClick(MouseButton.PRIMARY));
                assertEquals("1", badge.getText());
                PauseTransition firstReset = new PauseTransition(Duration.millis(1900));
                firstReset.setOnFinished(event -> {
                    try {
                        assertEquals("hub", badge.getText());
                        logo.getOnMouseClicked().handle(logoClick(MouseButton.PRIMARY));
                        assertEquals("2", badge.getText());
                        logo.getOnMouseClicked().handle(logoClick(MouseButton.PRIMARY));
                        assertEquals("3", badge.getText());
                        PauseTransition nextReset = new PauseTransition(Duration.millis(1900));
                        nextReset.setOnFinished(resetEvent -> {
                            try {
                                assertEquals("hub", badge.getText());
                                logo.getOnMouseClicked().handle(logoClick(MouseButton.PRIMARY));
                                assertEquals("4", badge.getText());
                                finished.complete(null);
                            } catch (Throwable failure) {
                                finished.completeExceptionally(failure);
                            }
                        });
                        nextReset.play();
                    } catch (Throwable failure) {
                        finished.completeExceptionally(failure);
                    }
                });
                firstReset.play();
                return null;
            });
            finished.get(8, TimeUnit.SECONDS);
        } finally {
            runOnFxThread(() -> window.getPrimaryStage().hide());
        }
    }

    @Test
    public void logoClick_routesOnlyPrimaryClick_preservesCommandAndClosesAudioOnHide() throws Exception {
        callOnFxThread(() -> {
            LogoSoundPlayerTest.TestWorker worker = new LogoSoundPlayerTest.TestWorker();
            LogoSoundPlayerTest.TestClip clip = new LogoSoundPlayerTest.TestClip();
            LogoSoundPlayer sound = new LogoSoundPlayer(worker,
                    LogoSoundPlayer.class.getResource("/audio/logo-drum.wav"), () -> clip.line);
            MainWindow window = createWindow(sound);
            try {
                Region root = layoutWindow(window, 1000, 700);
                Node logo = root.lookup("#ponhubWordmark");
                TextField command = (TextField) root.lookup("#commandTextField");
                command.setText("unfinished draft");
                command.requestFocus();
                command.selectRange(2, 7);
                Node focus = root.getScene().getFocusOwner();
                assertSame(command, focus);
                assertFalse(logo.isMouseTransparent());
                assertEquals(Cursor.HAND, logo.getCursor());
                logo.getOnMouseClicked().handle(logoClick(MouseButton.SECONDARY));
                assertEquals(0, worker.taskCount());
                MouseEvent primary = logoClick(MouseButton.PRIMARY);
                logo.getOnMouseClicked().handle(primary);
                assertTrue(primary.isConsumed());
                assertEquals(1, worker.taskCount());
                worker.runAll();
                assertEquals("unfinished draft", command.getText());
                assertEquals(2, command.getSelection().getStart());
                assertEquals(7, command.getSelection().getEnd());
                assertSame(focus, root.getScene().getFocusOwner());
                Event.fireEvent(window.getPrimaryStage(),
                        new WindowEvent(window.getPrimaryStage(), WindowEvent.WINDOW_HIDDEN));
                assertTrue(worker.isShutdown());
                worker.runAll();
                assertTrue(worker.isTerminated());
            } finally {
                sound.close();
                worker.runAll();
            }
            return null;
        });
    }

    private static MouseEvent logoClick(MouseButton button) {
        return new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, button, 1,
                false, false, false, false, false, false, false, false, false, true, null);
    }

    @Test
    public void layout_bottomDock_spansBothColumnsAndStaysOutsideResizableContent() throws Exception {
        Region root = callOnFxThread(() -> (Region) createWindow().getPrimaryStage().getScene().getRoot());
        for (int[] size : new int[][] {{450, 330}, {853, 420}, {1536, 800}, {1920, 1020}}) {
            runOnFxThread(() -> {
                layoutScene(root, size[0], size[1]);
                SplitPane results = (SplitPane) root.lookup("#resultSplitPane");
                SplitPane records = (SplitPane) root.lookup("#recordsSplitPane");
                TextField command = (TextField) root.lookup("#commandTextField");
                command.setText("unfinished draft");
                command.selectRange(2, 7);
                for (double position : new double[] {0.2, 0.8}) {
                    results.setDividerPositions(position);
                    records.setDividerPositions(position);
                    root.layout();
                    Bounds dock = root.lookup("#commandBoxPlaceholder").localToScene(
                            root.lookup("#commandBoxPlaceholder").getBoundsInLocal());
                    Bounds workspace = results.localToScene(results.getBoundsInLocal());
                    assertTrue(dock.getMinY() >= workspace.getMaxY() - 1,
                            "Command input must stay below the result and both record columns.");
                    assertEquals(0, dock.getMinX(), 1);
                    assertEquals(root.getWidth(), dock.getWidth(), 1);
                    assertAccessible(command, root);
                    assertAccessible(root.lookup("#ponhubWordmark"), root);
                    assertEquals("unfinished draft", command.getText());
                    assertEquals(2, command.getSelection().getStart());
                    assertEquals(7, command.getSelection().getEnd());
                }
            });
        }
    }

    @Test
    public void layout_compactAndLargeScenes_keepControlsAndListAccessible() throws Exception {
        Region root = callOnFxThread(() -> (Region) createWindow().getPrimaryStage().getScene().getRoot());
        // Exceed even the enlarged result viewport at the widest scene, including compact Mac font metrics.
        String feedback = "Long feedback that must remain readable. ".repeat(400) + "Final feedback marker.";
        for (int[] size : new int[][] {{450, 330}, {853, 420}, {1536, 800}, {1920, 1020}}) {
            runOnFxThread(() -> {
                layoutScene(root, size[0], size[1]);
                TextArea result = (TextArea) root.lookup("#resultDisplay");
                result.setText(feedback);
                result.setScrollTop(0);
                root.layout();
            });
            runOnFxThread(() -> {
                root.applyCss();
                root.layout();
                TextArea result = (TextArea) root.lookup("#resultDisplay");
                // Wrapped-content layout can defer the ScrollPane fit update to the next JavaFX turn.
                result.layout();
            });
            runOnFxThread(() -> {
                root.layout();
                for (String selector : List.of("#menuBar", "#commandTextField", "#resultDisplay",
                        "#personListView", "#statusbarPlaceholder", "#lessonsButton", "#studentLessonsButton",
                        "#attendanceButton")) {
                    assertAccessible(root.lookup(selector), root);
                }

                ListView<?> people = (ListView<?>) root.lookup("#personListView");
                assertTrue(people.getHeight() >= 40, "The people view must retain a usable viewport.");
                assertTrue(verticalScrollBar(people).isVisible(), "The populated list must be scrollable.");

                TextArea result = (TextArea) root.lookup("#resultDisplay");
                result.layout();
                assertTrue(result.isWrapText());
                assertTrue(verticalScrollBar(result).isVisible(),
                        "Long feedback must have a scrollable result area at " + size[0] + "x" + size[1]
                                + "; height=" + result.getHeight());
                ScrollPane scrollPane = (ScrollPane) result.lookup(".scroll-pane");
                assertTrue(scrollPane.getContent().getBoundsInLocal().getHeight()
                        > scrollPane.getViewportBounds().getHeight(), "Long feedback must exceed its viewport.");
                assertEquals(0, result.getScrollTop(), 1, "Each size must begin at the top of the feedback.");
                ScrollBar bar = verticalScrollBar(result);
                bar.setValue(bar.getMax());
            });
            runOnFxThread(() -> {
                root.layout();
                TextArea result = (TextArea) root.lookup("#resultDisplay");
                ScrollPane scrollPane = (ScrollPane) result.lookup(".scroll-pane");
                Node viewport = scrollPane.lookup(".viewport");
                Bounds viewportBounds = viewport.localToScene(viewport.getBoundsInLocal());
                Node content = scrollPane.getContent();
                Bounds contentBounds = content.localToScene(content.getBoundsInLocal());
                assertTrue(result.getScrollTop() > 0, "Scrolling must move the long feedback.");
                assertTrue(contentBounds.getMaxY() >= viewportBounds.getMinY()
                                && contentBounds.getMaxY() <= viewportBounds.getMaxY() + 1,
                        "Scrolling must expose the final feedback at " + size[0] + "x" + size[1]);
                assertEquals(feedback, result.getText(), "Scrolling must retain the complete feedback.");
            });
        }
    }

    @Test
    public void layout_longCanonicalContact_wrapAndRemainReachableAfterResizing() throws Exception {
        Region root = callOnFxThread(() -> (Region) createWindow().getPrimaryStage().getScene().getRoot());
        double[] narrowCardHeights = {0};
        for (int width : new int[] {450, 1536, 450}) {
            runOnFxThread(() -> {
                layoutScene(root, width, 420);
                ListView<?> people = (ListView<?>) root.lookup("#personListView");
                people.scrollTo(0);
                people.getSelectionModel().select(0);
                root.layout();
            });
            runOnFxThread(() -> {
                root.applyCss();
                for (int pass = 0; pass < 3; pass++) {
                    root.layout();
                }
                ListView<?> people = (ListView<?>) root.lookup("#personListView");
                Region card = findCard(people, LONG_PERSON_IDENTITY);

                for (Node node : card.lookupAll(".label")) {
                    Label label = (Label) node;
                    assertTrue(label.isWrapText(), label.getText());
                    assertTrue(label.getWidth() > 0 && label.getWidth() <= card.getWidth(),
                            label.getText() + "; label width=" + label.getWidth()
                                    + "; card width=" + card.getWidth() + "; scene width=" + width);
                    assertTrue(label.getHeight() + 1 >= label.prefHeight(label.getWidth()),
                            label.getText() + "; actual=" + label.getHeight()
                                    + "; preferred=" + label.prefHeight(label.getWidth())
                                    + "; width=" + label.getWidth());
                    assertEquals(label.getText(), ((Text) label.lookup(".text")).getText(),
                            "Complete canonical values must not use an ellipsis.");
                    if (width == 450 && label.getText().length() > 100) {
                        assertTrue(label.getHeight() > label.getFont().getSize() * 1.5,
                                "Long values must wrap onto multiple lines: " + label.getText());
                    }
                }
                if (width == 1536) {
                    assertTrue(card.getHeight() < narrowCardHeights[0],
                            "A wider cell must recompute the wrapped card's height.");
                } else {
                    narrowCardHeights[0] = card.getHeight();
                }

                assertLongFieldEndsReachable(people);
                people.scrollTo(people.getItems().size() - 1);
                root.layout();
                ScrollBar bar = verticalScrollBar(people);
                bar.setValue(bar.getMax());
                root.applyCss();
                for (int pass = 0; pass < 3; pass++) {
                    root.layout();
                }

                Region lastCard = findCard(people, "Tutor · ID: T7");
                Node lastField = lastCard.lookupAll(".label").stream()
                        .filter(node -> ((Label) node).getText().equals(
                                TUTOR_CONTACTS.getLast().getAddress().orElseThrow().value))
                        .findFirst().orElseThrow();
                Bounds lastFieldBounds = lastField.localToScene(lastField.getBoundsInLocal());
                Bounds viewport = people.localToScene(people.getBoundsInLocal());
                assertTrue(lastFieldBounds.getWidth() > 0 && lastFieldBounds.getHeight() > 0
                                && lastFieldBounds.getMinY() >= viewport.getMinY()
                                && lastFieldBounds.getMaxY() <= viewport.getMaxY() + 1,
                        "Scrolling must expose the final field of the last contact: " + lastFieldBounds);
            });
        }
    }

    @Test
    public void list_longCanonicalContact_wrapAfterRoleFilterAndCardsRefresh() throws Exception {
        MainWindow window = callOnFxThread(this::createWindow);
        Region root = callOnFxThread(() -> (Region) window.getPrimaryStage().getScene().getRoot());
        runOnFxThread(() -> layoutScene(root, 450, 330));
        runOnFxThread(() -> {
            TextField command = (TextField) root.lookup("#commandTextField");
            command.setText("list r/tutor");
            Event.fireEvent(command, new ActionEvent());
            ListView<?> people = (ListView<?>) root.lookup("#personListView");
            assertEquals(List.of("T1", "T2", "T3", "T4", "T5", "T6", "T7"),
                    people.getItems().stream().map(PersonRecord.class::cast)
                            .map(person -> person.getId().toString()).toList());
            command.setText("list");
            Event.fireEvent(command, new ActionEvent());
            assertEquals(List.of("S1", "T1", "T2", "T3", "T4", "T5", "T6", "T7"),
                    people.getItems().stream().map(PersonRecord.class::cast)
                            .map(person -> person.getId().toString()).toList());
            command.setText("help add");
            Event.fireEvent(command, new ActionEvent());
            root.applyCss();
            root.layout();
        });
        runOnFxThread(() -> {
            root.layout();
            ListView<?> people = (ListView<?>) root.lookup("#personListView");
            Region card = findCard(people, LONG_PERSON_IDENTITY);
            for (Node node : card.lookupAll(".label")) {
                Label label = (Label) node;
                assertTrue(label.isWrapText(), label.getText());
                assertTrue(label.getHeight() + 1 >= label.prefHeight(label.getWidth()),
                        "Refreshed fields must retain the complete wrapped text height: " + label.getText()
                                + "; actual=" + label.getHeight()
                                + "; preferred=" + label.prefHeight(label.getWidth()));
                Text renderedText = (Text) label.lookup(".text");
                assertEquals(label.getText(), renderedText.getText(), "Refreshed fields must not use an ellipsis.");
            }
            assertLongFieldEndsReachable(people);
        });
    }

    @Test
    public void help_compactScene_menuAndF1PreserveCommandDraftAndSelection() throws Exception {
        callOnFxThread(() -> {
            MainWindow window = createWindow();
            byte[] originalData = Files.readAllBytes(tempDirectory.resolve("contacts.json"));
            Region root = layoutWindow(window, 450, 330);
            TextField command = (TextField) root.lookup("#commandTextField");
            TextArea result = (TextArea) root.lookup("#resultDisplay");
            ListView<?> people = (ListView<?>) root.lookup("#personListView");
            MenuBar menuBar = (MenuBar) root.lookup("#menuBar");
            command.setText("add r/student n/Unfinished");
            people.getSelectionModel().select(1);

            MenuItem exit = menuBar.getMenus().get(0).getItems().get(0);
            assertEquals("Exit", exit.getText());
            assertNotNull(exit.getOnAction());
            MenuItem help = menuBar.getMenus().get(1).getItems().get(0);
            help.fire();
            assertEquals(CanonicalCommandCatalog.getOverview(Optional.empty()), result.getText());
            assertEquals("add r/student n/Unfinished", command.getText());
            assertEquals(1, people.getSelectionModel().getSelectedIndex());

            result.setText("Previous result");
            Event.fireEvent(command, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F1,
                    false, false, false, false));
            assertEquals(CanonicalCommandCatalog.getOverview(Optional.empty()), result.getText());
            assertEquals("add r/student n/Unfinished", command.getText());
            assertEquals(1, people.getSelectionModel().getSelectedIndex());
            assertArrayEquals(originalData, Files.readAllBytes(tempDirectory.resolve("contacts.json")));
            return null;
        });
    }

    /**
     * Builds the active window with canonical logic and immutable records stored only in the test directory.
     */
    private MainWindow createWindow() throws IOException {
        return createWindow(new LogoSoundPlayer());
    }

    private MainWindow createWindow(LogoSoundPlayer sound) throws IOException {
        PeopleRegistry people = new PeopleRegistry();
        people.addStudent(LONG_PERSON.getContactDetails(), LONG_PERSON.getLevel(), LONG_PERSON.getParentPhone());
        for (ContactDetails contacts : TUTOR_CONTACTS) {
            people.addTutor(contacts);
        }
        Path dataPath = tempDirectory.resolve("contacts.json");
        new JsonPonHubDataStorage(dataPath).saveData(
                new PonHubDataState(people.exportState(), List.of(), List.of(), 0));
        CanonicalLogicManager logic = new CanonicalLogicManager(new CanonicalPeopleSession(dataPath), new UserPrefs());
        MainWindow window = new MainWindow(new Stage(), logic, dataPath, sound);
        window.fillInnerParts();
        return window;
    }

    private static ContactDetails contacts(String name, String phone, String email, String address) {
        return new ContactDetails(new Name(name), Optional.of(new Phone(phone)),
                Optional.of(new Email(email)), Optional.of(new Address(address)));
    }

    /**
     * Lays out the actual themed FXML at a logical content size without displaying a native window.
     */
    private static Region layoutWindow(MainWindow window, double width, double height) {
        Region root = (Region) window.getPrimaryStage().getScene().getRoot();
        root.applyCss();
        root.resize(width, height);
        for (int pass = 0; pass < 3; pass++) {
            root.layout();
        }
        return root;
    }

    /**
     * Detaches the populated FXML root from its hidden stage so the fixture has the requested scene dimensions.
     */
    private static void layoutScene(Region root, double width, double height) {
        Scene previousScene = root.getScene();
        List<String> stylesheets = List.copyOf(previousScene.getStylesheets());
        previousScene.setRoot(new StackPane());
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().setAll(stylesheets);
        root.applyCss();
        root.resize(width, height);
        for (int pass = 0; pass < 3; pass++) {
            root.layout();
        }
        assertEquals(width, scene.getWidth());
        assertEquals(height, scene.getHeight());
    }

    private static void assertAccessible(Node node, Region root) {
        assertNotNull(node);
        Bounds bounds = node.localToScene(node.getBoundsInLocal());
        assertTrue(node.isVisible() && bounds.getWidth() > 0 && bounds.getHeight() > 0, node.getId());
        assertTrue(bounds.getMinX() >= -1 && bounds.getMinY() >= -1
                        && bounds.getMaxX() <= root.getWidth() + 1 && bounds.getMaxY() <= root.getHeight() + 1,
                node.getId() + " must fit in the scene: " + bounds);
    }

    private static ScrollBar verticalScrollBar(Node control) {
        return control.lookupAll(".scroll-bar").stream()
                .filter(ScrollBar.class::isInstance)
                .map(ScrollBar.class::cast)
                .filter(bar -> bar.getOrientation() == Orientation.VERTICAL)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing vertical scrollbar: " + control.getId()));
    }

    private static Region findCard(ListView<?> people, String identity) {
        return people.lookupAll("#cardPane").stream()
                .filter(MainWindowTest::isVisibleInHierarchy)
                .filter(node -> identity.equals(((Label) node.lookup("#identity")).getText()))
                .map(Region.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Contact is not reachable: " + identity));
    }

    private static boolean isVisibleInHierarchy(Node node) {
        for (Node ancestor = node; ancestor != null; ancestor = ancestor.getParent()) {
            if (!ancestor.isVisible()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks that scrolling can expose the complete heading, stable identity and every canonical contact field.
     */
    private static void assertLongFieldEndsReachable(ListView<?> people) {
        ContactDetails contacts = LONG_PERSON.getContactDetails();
        Set<String> unseenValues = new HashSet<>(List.of(LONG_PERSON.getName().fullName,
                LONG_PERSON_IDENTITY, LONG_PERSON.getLevel().toString(),
                LONG_PERSON.getParentPhone().value,
                contacts.getPhone().orElseThrow().value,
                contacts.getEmail().orElseThrow().value,
                contacts.getAddress().orElseThrow().value));
        ScrollBar bar = verticalScrollBar(people);
        Bounds viewport = people.localToScene(people.getBoundsInLocal());
        for (int step = 0; step <= 100 && !unseenValues.isEmpty(); step++) {
            bar.setValue(bar.getMin() + (bar.getMax() - bar.getMin()) * step / 100);
            people.applyCss();
            people.layout();
            for (Node card : people.lookupAll("#cardPane")) {
                if (!isVisibleInHierarchy(card)
                        || !LONG_PERSON_IDENTITY.equals(((Label) card.lookup("#identity")).getText())) {
                    continue;
                }
                for (Node node : card.lookupAll(".label")) {
                    Label label = (Label) node;
                    Bounds bounds = label.localToScene(label.getBoundsInLocal());
                    if (bounds.getWidth() > 0 && bounds.getHeight() > 0
                            && bounds.getMaxY() >= viewport.getMinY() && bounds.getMaxY() <= viewport.getMaxY() + 1) {
                        unseenValues.remove(label.getText());
                    }
                }
            }
        }
        assertTrue(unseenValues.isEmpty(), "Scrolling must expose every complete value: " + unseenValues);
    }
}
