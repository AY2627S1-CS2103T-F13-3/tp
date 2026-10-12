package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.FxTestUtil.callOnFxThread;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import seedu.address.MainApp;
import seedu.address.logic.Logic;
import seedu.address.model.PonHubDataState;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Name;
import seedu.address.model.person.Parent;
import seedu.address.model.person.PeopleRegistryState;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Student;
import seedu.address.model.person.Tutor;
import seedu.address.model.query.LessonView;
import seedu.address.storage.JsonPonHubDataStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.testutil.LessonQueryTestData;

/**
 * Exercises real application startup, command-box routing and canonical cards in the production window.
 * Offscreen FXML checks complement rather than establish native display and scaling acceptance.
 */
public class CanonicalMainWindowTest {

    @TempDir
    Path directory;

    @Test
    public void lessonCommands_renderSeparateCardsAndPreservePeopleSelectionAndIndices() throws Exception {
        Path path = directory.resolve("canonical.json");
        new JsonPonHubDataStorage(path).saveData(LessonQueryTestData.createData().exportState());
        byte[] bytes = Files.readAllBytes(path);
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                var results = fixture.lessons().getItems();
                assertEquals(5, results.size());
                fixture.enter("search c/student n/Jamie");
                fixture.people().getSelectionModel().select(0);
                var selected = fixture.people().getSelectionModel().getSelectedItem();
                fixture.enter("lessons si/1");
                assertEquals(List.of("L10", "L11"), lessonIds(fixture));
                fixture.enter("search c/lesson d/Mon s/Math sn/Alex");
                assertEquals("Found 1 lesson(s).", fixture.feedback());
                assertEquals(List.of("L10"), lessonIds(fixture));
                assertEquals("Students enrolled: 2", ((Label) fixture.root()
                        .lookup("#lessonRosterSize")).getText());
                assertSame(selected, fixture.people().getSelectionModel().getSelectedItem());
                assertCard(fixture, "Student · S2", "1. Jamie Tan");
                fixture.enter("lessons si/2");
                assertEquals(List.of("L10"), lessonIds(fixture));
                fixture.enter("search c/lesson s/Math s/Science");
                assertEquals(List.of("L10"), lessonIds(fixture));
                fixture.enter("search c/lesson sn/Nobody");
                assertEquals("Found 0 lesson(s).", fixture.feedback());
                assertTrue(fixture.lessons().getItems().isEmpty());
                assertEquals("No lessons to display.", ((Label) fixture.lessons().getPlaceholder()).getText());
                fixture.enter("lessons");
                assertSame(results, fixture.lessons().getItems());
                assertEquals(5, results.size());
                assertSame(selected, fixture.people().getSelectionModel().getSelectedItem());
                assertArrayEquals(bytes, Files.readAllBytes(path));
                captureLessonWindow(fixture);
            }
            return null;
        });
    }

    @Test
    public void lessonCards_addRefreshesSelectedSearchAndBothPanelsFitMinimumWindow() throws Exception {
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.enter("add r/tutor n/Mei Lim p/92345678");
                fixture.enter("search c/lesson s/Math");
                fixture.enter("addlesson d/Mon st/0900 et/1000 s/Math tu/Mei Lim rm/R1");
                assertEquals(List.of("L1"), lessonIds(fixture));
                assertEquals("Students enrolled: 0", ((Label) fixture.root()
                        .lookup("#lessonRosterSize")).getText());
                fixture.enter("addlesson d/Tue st/0900 et/1000 s/Science tu/Mei Lim rm/R1");
                assertEquals(List.of("L1"), lessonIds(fixture));
                fixture.enter("lessons");
                assertEquals(List.of("L1", "L2"), lessonIds(fixture));
                fixture.root().resize(450, 360);
                fixture.root().layout();
                assertTrue(fixture.people().getWidth() > 100);
                assertTrue(fixture.lessons().getWidth() > 100);
                assertTrue(fixture.people().getHeight() > 50);
                assertTrue(fixture.lessons().getHeight() > 50);
            }
            return null;
        });
    }

    private static List<String> lessonIds(WindowFixture fixture) {
        return fixture.lessons().getItems().stream().map(LessonView::getLessonId).map(Object::toString).toList();
    }

    /**
     * Optionally captures the real FXML window for guide updates, using only the test's temporary saved fixture.
     */
    private static void captureLessonWindow(WindowFixture fixture) throws IOException {
        String destination = System.getenv("PONHUB_CAPTURE_UI");
        if (destination == null) {
            return;
        }
        fixture.input().setText("lessons");
        fixture.root().setPrefSize(1000, 720);
        fixture.root().resize(1000, 720);
        fixture.root().layout();
        WritableImage snapshot = fixture.root().snapshot(null, null);
        int width = (int) snapshot.getWidth();
        int height = (int) snapshot.getHeight();
        BufferedImage png = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                png.setRGB(x, y, snapshot.getPixelReader().getArgb(x, y));
            }
        }
        ImageIO.write(png, "png", Path.of(destination).toFile());
    }

    @Test
    public void commandBox_roleAdditions_publishCardsAndPersistOptionalContacts() throws Exception {
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                assertTrue(fixture.people().getItems().isEmpty());
                assertFalse(Files.exists(fixture.dataPath));
                fixture.enter("add r/STUDENT n/Alex Tan l/p1 pp/91234567");
                assertEquals("New student added: Alex Tan (S1).", fixture.feedback());
                assertCard(fixture, "Student · S1", "1. Alex Tan");
                fixture.enter("add r/student n/Amelia Tan l/P2 pp/91234567 p/00012345"
                        + " e/amelia@example.com a/12 Main Road");
                fixture.enter("add r/tutor n/Mei Lee p/81234567");
                fixture.enter("list r/TUTOR");
                assertEquals("Showing 1 tutor(s).", fixture.feedback());
                assertCard(fixture, "Tutor · T1", "1. Mei Lee");
                fixture.enter("add r/parent n/Sam Tan p/91234567");
                assertEquals("New parent added: Sam Tan (P1).", fixture.feedback());
                assertEquals(4, fixture.people().getItems().size());
                fixture.people().scrollTo(3);
                fixture.layout();
                // Variable-height cells can leave the last card below the viewport after scrollTo on Linux.
                // Drive the actual scrollbar to the bottom, as in the dedicated card-scrolling regressions.
                for (var node : fixture.people().lookupAll(".scroll-bar")) {
                    if (node instanceof ScrollBar bar && bar.isVisible()
                            && bar.getOrientation() == Orientation.VERTICAL) {
                        bar.setValue(bar.getMax());
                    }
                }
                fixture.people().layout();
                assertCard(fixture, "Parent · P1", "4. Sam Tan");
                assertSame(fixture.app.getLogic().getPersonRecords(), fixture.people().getItems());
                PonHubDataState saved = new JsonPonHubDataStorage(fixture.dataPath).readData().orElseThrow();
                assertEquals(fixture.app.snapshot(), saved);
                assertTrue(saved.people().getPeople().getFirst().getContactDetails().getPhone().isEmpty());
                assertEquals("00012345", saved.people().getPeople().get(1).getContactDetails()
                        .getPhone().orElseThrow().value);
                assertEquals("amelia@example.com", saved.people().getPeople().get(1).getContactDetails()
                        .getEmail().orElseThrow().value);
                assertEquals("12 Main Road", saved.people().getPeople().get(1).getContactDetails()
                        .getAddress().orElseThrow().value);
            }
            return null;
        });
    }

    @Test
    public void commandBox_personSearch_updatesCardsIndicesAndRetainsSelectionOnFailure() throws Exception {
        seedMixedPeople();
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                byte[] original = Files.readAllBytes(fixture.dataPath);
                fixture.enter("search c/student n/Selected");
                assertEquals("Found 1 student(s).", fixture.feedback());
                assertCard(fixture, "Student · S3", "1. Selected Student");
                assertSame(fixture.app.getLogic().getPersonRecords(), fixture.people().getItems());
                fixture.people().getSelectionModel().select(0);
                fixture.enter("help SEARCH");
                assertEquals(new PersonId("S3"), fixture.people().getSelectionModel().getSelectedItem().getId());
                fixture.enter("search c/student d/Mon");
                assertTrue(fixture.feedback().contains("not available"));
                assertEquals(new PersonId("S3"), fixture.people().getSelectionModel().getSelectedItem().getId());
                assertCard(fixture, "Student · S3", "1. Selected Student");
                fixture.enter("search c/student n/No Match");
                assertEquals("Found 0 student(s).", fixture.feedback());
                assertTrue(fixture.people().getItems().isEmpty());
                fixture.enter("list r/student");
                assertEquals(List.of(new PersonId("S9"), new PersonId("S3")),
                        fixture.people().getItems().stream().map(PersonRecord::getId).toList());
                assertArrayEquals(original, Files.readAllBytes(fixture.dataPath));
            }
            return null;
        });
    }

    @Test
    public void commandBox_filteredIndicesAndRejectedCommands_keepOneViewAndFile() throws Exception {
        seedMixedPeople();
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                byte[] original = Files.readAllBytes(fixture.dataPath);
                fixture.people().scrollTo(1);
                fixture.layout();
                assertCard(fixture, "Student · S9", "2. Older Student");
                fixture.enter("list r/student");
                assertEquals(List.of(new PersonId("S9"), new PersonId("S3")),
                        fixture.people().getItems().stream().map(PersonRecord::getId).toList());
                assertCard(fixture, "Student · S3", "2. Selected Student");
                fixture.people().getSelectionModel().select(1);
                PonHubDataState originalState = fixture.app.snapshot();
                for (String command : List.of("list r/all", "list r/student r/tutor", "delete 2", "edit 2",
                        "find Selected", "clear", "search n/Selected", "lessons sid/S3", "help delete")) {
                    fixture.enter(command);
                    assertEquals(command, fixture.input().getText());
                    assertTrue(fixture.input().getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
                    assertEquals(new PersonId("S3"), fixture.people().getSelectionModel().getSelectedItem().getId());
                    assertEquals(originalState, fixture.app.snapshot());
                    assertArrayEquals(original, Files.readAllBytes(fixture.dataPath));
                }
            }
            return null;
        });
    }

    @Test
    public void commandBox_failedSave_preservesCardsSelectionCountersAndSuccessfulRetry() throws Exception {
        seedMixedPeople();
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.enter("list r/student");
                fixture.people().getSelectionModel().select(1);
                PonHubDataState original = fixture.app.snapshot();
                Path backup = directory.resolve("original.json");
                Files.move(fixture.dataPath, backup);
                Files.createDirectory(fixture.dataPath);
                String addition = "add r/student n/New Student l/P1 pp/91234567";
                fixture.enter(addition);
                assertEquals(addition, fixture.input().getText());
                assertEquals(original, fixture.app.snapshot());
                assertEquals(new PersonId("S3"), fixture.people().getSelectionModel().getSelectedItem().getId());
                assertEquals(1, fixture.people().getSelectionModel().getSelectedIndex());
                assertEquals(2, fixture.people().getItems().size());
                assertEquals(Optional.of(PersonRole.STUDENT), fixture.app.roleFilter());
                assertCard(fixture, "Student · S3", "2. Selected Student");
                assertTrue(Files.isDirectory(fixture.dataPath));
                Files.delete(fixture.dataPath);
                Files.move(backup, fixture.dataPath);
                fixture.enter(addition);
                assertEquals("New student added: New Student (S10).", fixture.feedback());
                assertEquals(Optional.empty(), fixture.app.roleFilter());
                assertEquals(5, fixture.people().getItems().size());
                fixture.people().scrollTo(4);
                fixture.layout();
                assertCard(fixture, "Student · S10", "5. New Student");
                assertEquals(fixture.app.snapshot(), new JsonPonHubDataStorage(fixture.dataPath)
                        .readData().orElseThrow());
            }
            return null;
        });
    }

    @Test
    public void help_menuAndKeyboard_preserveDraftSelectionAndOperationalBytes() throws Exception {
        seedMixedPeople();
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                byte[] original = Files.readAllBytes(fixture.dataPath);
                fixture.people().getSelectionModel().select(1);
                fixture.input().setText("add r/student n/Unfinished");
                fixture.window.handleHelp();
                assertTrue(fixture.feedback().contains("add r/student"));
                assertFalse(fixture.feedback().contains("delete -"));
                assertEquals("add r/student n/Unfinished", fixture.input().getText());
                assertEquals(new PersonId("S9"), fixture.people().getSelectionModel().getSelectedItem().getId());
                Event.fireEvent(fixture.input(), new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F1,
                        false, false, false, false));
                assertTrue(fixture.feedback().contains("help [COMMAND]"));
                assertEquals("add r/student n/Unfinished", fixture.input().getText());
                assertArrayEquals(original, Files.readAllBytes(fixture.dataPath));
                fixture.enter("exit");
                fixture.app.stop();
                assertArrayEquals(original, Files.readAllBytes(fixture.dataPath));
                assertEquals(fixture.app.getLogic().getGuiSettings(), new JsonUserPrefsStorage(fixture.prefsPath)
                        .readUserPrefs().orElseThrow().getGuiSettings());
            }
            return null;
        });
    }

    @Test
    public void protectedWindow_helpListAndMutationFeedback_preserveRejectedBytes() throws Exception {
        Path dataPath = directory.resolve("canonical.json");
        Files.writeString(dataPath, "{broken json");
        byte[] original = Files.readAllBytes(dataPath);
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.window.handleHelp();
                assertTrue(fixture.feedback().contains("Operational writes are blocked"));
                assertFalse(fixture.feedback().contains("add -"));
                fixture.enter("list");
                assertTrue(fixture.feedback().contains("Showing 0 person(s)."));
                assertTrue(fixture.feedback().contains("An empty view is not your saved data"));
                assertTrue(fixture.people().getItems().isEmpty());
                fixture.enter("add r/student n/Rejected Student l/P1 pp/91234567");
                assertTrue(fixture.feedback().contains("Operational writes are blocked"));
                fixture.enter("exit");
            }
            assertArrayEquals(original, Files.readAllBytes(dataPath));
            return null;
        });
    }

    private void seedMixedPeople() throws Exception {
        ContactDetails tutorContacts = new ContactDetails(new Name("First Tutor"), Optional.of(new Phone("81234567")),
                Optional.empty(), Optional.empty());
        ContactDetails parentContacts = new ContactDetails(new Name("First Parent"), Optional.of(new Phone("91234567")),
                Optional.empty(), Optional.empty());
        List<PersonRecord> people = List.of(new Tutor(new PersonId("T7"), tutorContacts),
                new Student(new PersonId("S9"), new ContactDetails(new Name("Older Student")),
                        new EducationLevel("P2"), new Phone("91234567")),
                new Parent(new PersonId("P2"), parentContacts),
                new Student(new PersonId("S3"), new ContactDetails(new Name("Selected Student")),
                        new EducationLevel("P1"), new Phone("91234567")));
        PonHubDataState state = new PonHubDataState(new PeopleRegistryState(people,
                Map.of(PersonRole.STUDENT, 9L, PersonRole.TUTOR, 7L, PersonRole.PARENT, 2L)), List.of(), List.of(), 0);
        new JsonPonHubDataStorage(directory.resolve("canonical.json")).saveData(state);
    }

    private WindowFixture createWindow() throws Exception {
        Path dataPath = directory.resolve("canonical.json");
        Path prefsPath = directory.resolve("preferences.json");
        AppHarness app = new AppHarness();
        app.initializePaths(dataPath, prefsPath);
        MainWindow window = new MainWindow(new Stage(), app.getLogic(), dataPath);
        window.fillInnerParts();
        WindowFixture fixture = new WindowFixture(app, window, dataPath, prefsPath);
        fixture.layout();
        return fixture;
    }

    private static void assertCard(WindowFixture fixture, String identity, String heading) {
        fixture.layout();
        Region card = fixture.root().lookupAll("#cardPane").stream()
                .filter(node -> identity.equals(((Label) node.lookup("#identity")).getText()))
                .map(Region.class::cast).findFirst().orElseThrow(() -> new AssertionError(
                        "Missing visible card " + identity + "; rendered=" + fixture.root().lookupAll("#identity")
                                .stream().map(node -> ((Label) node).getText()).toList()));
        assertEquals(heading, ((Label) card.lookup("#heading")).getText());
        assertNotNull(card.lookup("#details"));
    }

    private record WindowFixture(AppHarness app, MainWindow window, Path dataPath, Path prefsPath)
            implements AutoCloseable {
        private Region root() {
            return (Region) window.getPrimaryStage().getScene().getRoot();
        }

        private void layout() {
            root().applyCss();
            root().resize(853, 600);
            root().layout();
            // Offscreen tests have no native pulse to finish a virtualized list's pending scroll/layout.
            people().layout();
            lessons().layout();
        }

        private TextField input() {
            return (TextField) root().lookup("#commandTextField");
        }

        private String feedback() {
            return ((TextArea) root().lookup("#resultDisplay")).getText();
        }

        @SuppressWarnings("unchecked")
        private ListView<PersonRecord> people() {
            return (ListView<PersonRecord>) root().lookup("#personListView");
        }

        @SuppressWarnings("unchecked")
        private ListView<LessonView> lessons() {
            return (ListView<LessonView>) root().lookup("#lessonListView");
        }

        private void enter(String command) {
            input().setText(command);
            Event.fireEvent(input(), new ActionEvent());
            layout();
        }

        @Override
        public void close() {
            window.getPrimaryStage().hide();
            app.stop();
        }
    }

    /**
     * Exposes the production initialization boundary without launching another JavaFX application.
     */
    private static class AppHarness extends MainApp {
        private void initializePaths(Path dataPath, Path prefsPath) throws IOException {
            initialize(dataPath, prefsPath);
        }

        private Logic getLogic() {
            return logic;
        }

        private PonHubDataState snapshot() {
            return canonicalSession.getDataSnapshot();
        }

        private Optional<PersonRole> roleFilter() {
            return canonicalSession.getRoleFilter();
        }
    }
}
