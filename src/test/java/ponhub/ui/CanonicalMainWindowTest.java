package ponhub.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.FxTestUtil.callOnFxThread;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
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
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import ponhub.MainApp;
import ponhub.logic.CanonicalCommandCatalog;
import ponhub.logic.Logic;
import ponhub.model.PonHubData;
import ponhub.model.PonHubDataState;
import ponhub.model.attendance.Attendance;
import ponhub.model.attendance.AttendanceStatus;
import ponhub.model.lesson.LessonId;
import ponhub.model.person.ContactDetails;
import ponhub.model.person.EducationLevel;
import ponhub.model.person.Name;
import ponhub.model.person.Parent;
import ponhub.model.person.PeopleRegistryState;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.PersonRole;
import ponhub.model.person.Phone;
import ponhub.model.person.Student;
import ponhub.model.person.Tutor;
import ponhub.model.query.AttendanceHistoryEntry;
import ponhub.model.query.LessonView;
import ponhub.storage.JsonPonHubDataStorage;
import ponhub.storage.JsonUserPrefsStorage;
import ponhub.testutil.LessonQueryTestData;

/**
 * Exercises real application startup, command-box routing and canonical cards in the production window.
 * Offscreen FXML checks complement rather than establish native display and scaling acceptance.
 */
public class CanonicalMainWindowTest {

    @TempDir
    Path directory;

    @Test
    public void initializeMainWindow_shownStage_focusesCommandInput() throws Exception {
        AppHarness app = new AppHarness();
        Stage stage = callOnFxThread(() -> {
            Path dataPath = directory.resolve("canonical.json");
            app.initializePaths(dataPath, directory.resolve("preferences.json"));
            Stage primaryStage = new Stage();
            new UiManager(app.getLogic(), dataPath).initializeMainWindow(primaryStage);
            return primaryStage;
        });
        try {
            callOnFxThread(() -> {
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();
                TextField input = (TextField) stage.getScene().lookup("#commandTextField");
                assertTrue(stage.isShowing());
                assertSame(input, stage.getScene().getFocusOwner());
                TextArea feedback = (TextArea) stage.getScene().lookup("#resultDisplay");
                feedback.requestFocus();
                assertSame(feedback, stage.getScene().getFocusOwner());
                return null;
            });
        } finally {
            callOnFxThread(() -> {
                stage.hide();
                app.stop();
                return null;
            });
        }
    }

    @Test
    public void history_showsRetainedRowsWithoutChangingPeopleSelection() throws Exception {
        Path path = directory.resolve("canonical.json");
        PonHubData data = LessonQueryTestData.createData();
        data.markAttendance(LessonQueryTestData.STUDENT_TWO, LessonQueryTestData.SHARED_LESSON,
                LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT);
        new JsonPonHubDataStorage(path).saveData(data.exportState());
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.enter("search c/student n/Jamie s/Math d/Mon");
                fixture.people().getSelectionModel().select(0);
                var selected = fixture.people().getSelectionModel().getSelectedItem();
                byte[] before = Files.readAllBytes(path);
                fixture.enter("history 1 lid/L10");
                assertTrue(fixture.history().getParent().isVisible());
                assertFalse(fixture.lessons().getParent().isVisible());
                Label context = (Label) fixture.root().lookup("#historyContext");
                Label summary = (Label) fixture.root().lookup("#historySummary");
                assertEquals("Student S2 · attendance history · L10", context.getText());
                assertEquals("Showing 1 attendance record(s).", summary.getText());
                assertEquals(AttendanceHistoryEntry.Status.PRESENT, fixture.history().getItems().get(0).getStatus());
                assertSame(selected, fixture.people().getSelectionModel().getSelectedItem());
                assertArrayEquals(before, Files.readAllBytes(path));
                assertTrue(fixture.root().lookupAll(".label").stream().map(Label.class::cast)
                        .anyMatch(label -> label.getText().contains("Lesson: L10")));
                fixture.enter("search c/student n/Jamie d/Invalid");
                assertTrue(fixture.history().getParent().isVisible());
                assertSame(selected, fixture.people().getSelectionModel().getSelectedItem());
                fixture.enter("search c/parent");
                fixture.enter("history 1");
                assertTrue(fixture.history().getParent().isVisible());
                assertEquals("Student S2 · attendance history · L10", context.getText());
                fixture.enter("search c/tutor s/Math d/Mon");
                assertTrue(fixture.history().getParent().isVisible());
                assertEquals(AttendanceHistoryEntry.Status.PRESENT, fixture.history().getItems().get(0).getStatus());
                fixture.enter("search c/student n/Jamie");
                fixture.people().getSelectionModel().select(0);
                fixture.enter("unenrol 1 lid/L10");
                assertFalse(fixture.history().getItems().get(0).isCurrentlyEnrolled());
                fixture.enter("mark 1 lid/L10 d/2026-10-05 s/absent");
                assertEquals(AttendanceHistoryEntry.Status.ABSENT, fixture.history().getItems().get(0).getStatus());
                assertSame(selected, fixture.people().getSelectionModel().getSelectedItem());
                fixture.enter("showlesson lid/L99");
                assertTrue(fixture.history().getParent().isVisible());
                fixture.enter("showlesson lid/L10 d/2026-10-05");
                ScrollPane detail = (ScrollPane) fixture.root().lookup("#lessonDetailScroll");
                assertFalse(fixture.history().getParent().isVisible());
                assertTrue(fixture.lessons().getParent().isVisible());
                assertTrue(detail.isVisible());
                fixture.enter("history 0");
                assertFalse(fixture.history().getParent().isVisible());
                assertTrue(detail.isVisible());
                fixture.enter("history 1 lid/L10");
                assertTrue(fixture.history().getParent().isVisible());
                fixture.enter("unmark 1 lid/L10 d/2026-10-05");
                assertTrue(fixture.history().getItems().isEmpty());
                assertEquals("Showing 0 attendance record(s).", summary.getText());
                assertSame(selected, fixture.people().getSelectionModel().getSelectedItem());
                fixture.enter("lessons");
                assertFalse(fixture.history().getParent().isVisible());
                assertTrue(fixture.lessons().getParent().isVisible());
            }
            return null;
        });
    }

    @Test
    public void history_deletedSelectionRestoresLessonPane() throws Exception {
        new JsonPonHubDataStorage(directory.resolve("canonical.json"))
                .saveData(LessonQueryTestData.createData().exportState());
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.enter("search c/student");
                fixture.people().getSelectionModel().select(1);
                var selected = fixture.people().getSelectionModel().getSelectedItem();
                fixture.enter("history 2 lid/L3");
                assertTrue(fixture.history().getParent().isVisible());
                fixture.enter("deletelesson lid/L3");
                assertEquals("Deleted lesson: L3.", fixture.feedback());
                assertFalse(fixture.history().getParent().isVisible());
                assertTrue(fixture.lessons().getParent().isVisible());
                assertSame(selected, fixture.people().getSelectionModel().getSelectedItem());
                assertTrue(fixture.lessons().getItems().stream()
                        .noneMatch(lesson -> lesson.getLessonId().equals(LessonQueryTestData.EMPTY_LESSON)));
                fixture.enter("history 3");
                assertTrue(fixture.history().getParent().isVisible());
                fixture.enter("delete 3");
                assertFalse(fixture.history().getParent().isVisible());
                assertTrue(fixture.lessons().getParent().isVisible());
                assertEquals("Attendance history", ((Label) fixture.root().lookup("#historyContext")).getText());
                fixture.enter("history 2 lid/L10");
                fixture.enter("delete 2");
                assertTrue(fixture.history().getParent().isVisible());
                Label historyContext = (Label) fixture.root().lookup("#historyContext");
                assertEquals("Student S2 · attendance history · L10", historyContext.getText());
            }
            return null;
        });
    }

    @Test
    public void tutorRelationshipSearch_displaysTheMatchingRosterAndEmptyLessonContexts() throws Exception {
        new JsonPonHubDataStorage(directory.resolve("canonical.json"))
                .saveData(ponhub.testutil.RelationshipSearchTestData.createData().exportState());
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.enter("search c/tutor sn/Alex d/Mon s/Math");
                assertCard(fixture, "Tutor · ID: T1", "Mei Lim", 1);
                assertTrue(fixture.root().lookupAll(".label").stream().map(Label.class::cast)
                        .anyMatch(label -> label.getText().contains("Student: Alex Tan (S1) · Lesson: L10")));
                fixture.enter("search c/tutor d/Mon s/Science");
                assertCard(fixture, "Tutor · ID: T2", "Mei Lim", 1);
                assertTrue(fixture.root().lookupAll(".label").stream().map(Label.class::cast)
                        .anyMatch(label -> label.getText().startsWith("Lesson: L3")));
            }
            return null;
        });
    }

    @Test
    public void parentRelationshipSearch_rendersAssociatedChildAndOnlyTheirMatchingLesson() throws Exception {
        new JsonPonHubDataStorage(directory.resolve("canonical.json"))
                .saveData(ponhub.testutil.RelationshipSearchTestData.createData().exportState());
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.enter("search c/parent n/Pat sn/Alex d/Mon s/Math");
                assertCard(fixture, "Parent · ID: P1", "Pat Tan", 1);
                assertTrue(fixture.root().lookupAll(".label").stream().map(Label.class::cast)
                        .anyMatch(label -> label.getText().contains("Student: Alex Tan (S1) · Lesson: L10")));
                fixture.enter("search c/parent sn/Alex d/Wed s/中文");
                assertEquals("Found 0 parent(s).", fixture.feedback());
                fixture.enter("search c/parent n/Unassigned sn/Alex");
                assertTrue(fixture.root().lookupAll(".label").stream().map(Label.class::cast)
                        .anyMatch(label -> label.getText().equals("Student: Alex Tan (S3)")));
            }
            return null;
        });
    }

    @Test
    public void studentRelationshipSearch_rendersOnlyMatchingContextAndRefreshesItForTheSamePerson() throws Exception {
        new JsonPonHubDataStorage(directory.resolve("canonical.json"))
                .saveData(LessonQueryTestData.createData().exportState());
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.enter("search c/student p/00012345 s/Math d/Mon");
                assertEquals(1, fixture.people().getItems().size());
                assertCard(fixture, "Student · ID: S2", "Jamie Tan", 1);
                assertTrue(fixture.root().lookupAll(".label").stream().map(Label.class::cast)
                        .anyMatch(label -> label.getText().contains("Lesson: L10")));
                fixture.enter("enrol 1 lid/L4");
                assertTrue(fixture.root().lookupAll(".label").stream().map(Label.class::cast)
                        .anyMatch(label -> label.getText().contains("Lesson: L4")));
                fixture.enter("unenrol 1 lid/L4");
                assertFalse(fixture.root().lookupAll(".label").stream().map(Label.class::cast)
                        .anyMatch(label -> label.getText().contains("Lesson: L4")));
                fixture.enter("search c/student n/Jamie");
                assertFalse(fixture.root().lookupAll(".label").stream().map(Label.class::cast)
                        .anyMatch(label -> label.getText().startsWith("Lesson:")));
            }
            return null;
        });
    }

    @Test
    public void realMembership_refreshesLessonCardsAndPreservesSelectedPeopleAndContext() throws Exception {
        new JsonPonHubDataStorage(directory.resolve("canonical.json"))
                .saveData(LessonQueryTestData.createData().exportState());
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.enter("search c/student n/Jamie");
                fixture.people().getSelectionModel().select(0);
                var selected = fixture.people().getSelectionModel().getSelectedItem();
                fixture.enter("lessons si/1");
                var displayed = fixture.lessons().getItems();
                fixture.enter("list r/tutor");
                assertEquals("Student S2 · current lessons", ((Label) fixture.root()
                        .lookup("#lessonContext")).getText());
                fixture.enter("search c/student n/Jamie");
                fixture.people().getSelectionModel().select(0);
                fixture.enter("enrol 1 lid/L4");
                assertEquals(List.of("L10", "L4", "L11"), lessonIds(fixture));
                assertSame(selected, fixture.people().getSelectionModel().getSelectedItem());
                fixture.enter("search c/lesson sn/Jamie s/Math");
                assertEquals(List.of("L10", "L4"), lessonIds(fixture));
                fixture.enter("unenrol 1 lid/L4");
                assertEquals(List.of("L10"), lessonIds(fixture));
                assertSame(displayed, fixture.lessons().getItems());
                assertSame(selected, fixture.people().getSelectionModel().getSelectedItem());
                assertEquals("Lesson search: sn/jamie, s/math", ((Label) fixture.root()
                        .lookup("#lessonContext")).getText());
                assertCard(fixture, "Student · ID: S2", "Jamie Tan", 1);
            }
            return null;
        });
    }

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
                assertCard(fixture, "Student · ID: S2", "Jamie Tan", 1);
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
    public void showLesson_rendersCurrentAndFormerAttendanceInScrollingDetailWithoutChangingPeople() throws Exception {
        Path path = directory.resolve("canonical.json");
        var source = LessonQueryTestData.createData().exportState();
        LessonId lessonId = new LessonId("L10");
        var lessons = source.lessons().stream().map(lesson -> lesson.getId().equals(lessonId)
                ? lesson.withoutEnrolledStudent(new PersonId("S2")) : lesson).toList();
        var attendance = List.of(
                Attendance.fromStrings(new PersonId("S1"), lessonId, "2026-10-05", "present"),
                Attendance.fromStrings(new PersonId("S2"), lessonId, "2026-10-05", "absent"));
        new JsonPonHubDataStorage(path).saveData(new PonHubDataState(source.people(), lessons, attendance, 11));
        byte[] bytes = Files.readAllBytes(path);
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.enter("search c/student n/Jamie");
                fixture.people().getSelectionModel().select(0);
                var selected = fixture.people().getSelectionModel().getSelectedItem();
                fixture.enter("showlesson lid/L10 d/2026-10-05");
                ScrollPane detail = (ScrollPane) fixture.root().lookup("#lessonDetailScroll");
                assertTrue(detail.isVisible());
                assertFalse(fixture.lessons().isVisible());
                assertEquals("Math", ((Label) detail.lookup("#lessonHeading")).getText());
                assertTrue(detail.lookupAll(".label").stream().map(Label.class::cast)
                        .anyMatch(label -> label.getText().contains("S1 · Alex Tan — present")));
                assertTrue(detail.lookupAll(".label").stream().map(Label.class::cast)
                        .anyMatch(label -> label.getText().contains("S2 · Jamie Tan — absent")));
                assertSame(selected, fixture.people().getSelectionModel().getSelectedItem());
                assertCard(fixture, "Student · ID: S2", "Jamie Tan", 1);
                fixture.enter("lessons si/1");
                assertTrue(fixture.lessons().isVisible());
                assertFalse(detail.isVisible());
                assertSame(selected, fixture.people().getSelectionModel().getSelectedItem());
                assertArrayEquals(bytes, Files.readAllBytes(path));
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
                assertCard(fixture, "Student · ID: S1", "Alex Tan", 1);
                fixture.enter("add r/student n/Amelia Tan l/P2 pp/91234567 p/00012345"
                        + " e/amelia@example.com a/12 Main Road");
                fixture.enter("add r/tutor n/Mei Lee p/81234567");
                fixture.enter("list r/TUTOR");
                assertEquals("Showing 1 tutor(s).", fixture.feedback());
                assertCard(fixture, "Tutor · ID: T1", "Mei Lee", 1);
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
                assertCard(fixture, "Parent · ID: P1", "Sam Tan", 4);
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
                assertCard(fixture, "Student · ID: S3", "Selected Student", 1);
                assertSame(fixture.app.getLogic().getPersonRecords(), fixture.people().getItems());
                fixture.people().getSelectionModel().select(0);
                fixture.enter("help SEARCH");
                assertEquals(new PersonId("S3"), fixture.people().getSelectionModel().getSelectedItem().getId());
                fixture.enter("search c/tutor tu/First");
                assertTrue(fixture.feedback().contains("not supported"));
                assertEquals(new PersonId("S3"), fixture.people().getSelectionModel().getSelectedItem().getId());
                assertCard(fixture, "Student · ID: S3", "Selected Student", 1);
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
                assertCard(fixture, "Student · ID: S9", "Older Student", 2);
                fixture.enter("list r/student");
                assertEquals(List.of(new PersonId("S9"), new PersonId("S3")),
                        fixture.people().getItems().stream().map(PersonRecord::getId).toList());
                assertCard(fixture, "Student · ID: S3", "Selected Student", 2);
                fixture.people().getSelectionModel().select(1);
                PonHubDataState originalState = fixture.app.snapshot();
                for (String command : List.of("list r/all", "list r/student r/tutor", "delete S3", "edit 2",
                        "find Selected", "clear", "search n/Selected", "lessons sid/S3", "help clear")) {
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
    public void commandBox_personEdit_retainsVisibleIdentityAndClearsDisappearingSelection() throws Exception {
        seedMixedPeople();
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.enter("list r/student");
                fixture.people().getSelectionModel().select(1);
                fixture.enter("edit 2 n/Corrected Student");
                assertEquals("Edited student: Corrected Student (S3).", fixture.feedback());
                assertEquals(new PersonId("S3"), fixture.people().getSelectionModel().getSelectedItem().getId());
                assertCard(fixture, "Student · ID: S3", "Corrected Student", 2);
                fixture.enter("search c/student n/Student");
                fixture.people().getSelectionModel().select(0);
                fixture.enter("edit 1 n/Changed Person");
                assertEquals(List.of(new PersonId("S3")),
                        fixture.people().getItems().stream().map(PersonRecord::getId).toList());
                assertTrue(fixture.people().getSelectionModel().isEmpty());
                assertCard(fixture, "Student · ID: S3", "Corrected Student", 1);
                fixture.enter("help edit");
                assertTrue(fixture.feedback().contains("Usage: edit INDEX"));
                assertEquals(fixture.app.snapshot(),
                        new JsonPonHubDataStorage(fixture.dataPath).readData().orElseThrow());
            }
            return null;
        });
    }

    @Test
    public void commandBox_personEditFailedSave_preservesSelectionAndDetailUntilRetry() throws Exception {
        seedMixedPeople();
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.enter("addlesson d/Mon st/0900 et/1000 s/Math tu/First Tutor rm/R1");
                fixture.enter("list r/student");
                fixture.enter("enrol 1 lid/L1");
                fixture.enter("showlesson lid/L1");
                fixture.people().getSelectionModel().select(0);
                var selected = fixture.people().getSelectionModel().getSelectedItem();
                var detail = fixture.lessons().getItems().getFirst();
                var original = fixture.app.snapshot();
                Path backup = directory.resolve("edit-original.json");
                Files.move(fixture.dataPath, backup);
                Files.createDirectory(fixture.dataPath);
                fixture.enter("edit 1 n/Corrected Student");
                assertEquals("edit 1 n/Corrected Student", fixture.input().getText());
                assertTrue(fixture.feedback().contains("Could not save"));
                assertEquals(original, fixture.app.snapshot());
                assertSame(selected, fixture.people().getSelectionModel().getSelectedItem());
                assertSame(detail, fixture.lessons().getItems().getFirst());
                Files.delete(fixture.dataPath);
                Files.move(backup, fixture.dataPath);
                fixture.enter("edit 1 n/Corrected Student");
                assertEquals("Edited student: Corrected Student (S9).", fixture.feedback());
                assertEquals(new PersonId("S9"), fixture.people().getSelectionModel().getSelectedItem().getId());
                assertEquals(List.of("L1"), lessonIds(fixture));
                assertEquals(fixture.app.snapshot(),
                        new JsonPonHubDataStorage(fixture.dataPath).readData().orElseThrow());
            }
            return null;
        });
    }

    @Test
    public void commandBox_guardedDeletion_preservesFilteredCardsAndRenumbers() throws Exception {
        seedMixedPeople();
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.enter("list r/student");
                var visible = fixture.people().getItems();
                fixture.people().getSelectionModel().select(0);
                fixture.enter("delete 1");
                assertEquals("Deleted student: Older Student (S9).", fixture.feedback());
                assertTrue(fixture.input().getText().isEmpty());
                assertSame(visible, fixture.people().getItems());
                assertEquals(List.of(new PersonId("S3")),
                        visible.stream().map(PersonRecord::getId).toList());
                assertEquals(Optional.of(PersonRole.STUDENT), fixture.app.roleFilter());
                fixture.layout();
                assertCard(fixture, "Student · ID: S3", "Selected Student", 1);
                assertEquals(fixture.app.snapshot(),
                        new JsonPonHubDataStorage(fixture.dataPath).readData().orElseThrow());
            }
            return null;
        });
    }

    @Test
    public void commandBox_deletedLessonViewStudent_preservesBothPanelsOnFailedSaveAndRetry() throws Exception {
        seedMixedPeople();
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.enter("addlesson d/Mon st/0900 et/1000 s/Math tu/First Tutor rm/R1");
                fixture.enter("list r/student");
                fixture.enter("enrol 1 lid/L1");
                fixture.enter("lessons si/1");
                assertEquals(List.of("L1"), lessonIds(fixture));
                fixture.people().getSelectionModel().select(0);
                var selected = fixture.people().getSelectionModel().getSelectedItem();
                var displayed = fixture.lessons().getItems();
                fixture.enter("delete 1");
                assertTrue(fixture.feedback().contains("enrolled in lesson L1"));
                assertSame(selected, fixture.people().getSelectionModel().getSelectedItem());
                assertEquals(List.of("L1"), lessonIds(fixture));

                fixture.enter("unenrol 1 lid/L1");
                PonHubDataState original = fixture.app.snapshot();
                Path backup = directory.resolve("original.json");
                Files.move(fixture.dataPath, backup);
                Files.createDirectory(fixture.dataPath);
                fixture.enter("delete 1");
                assertEquals("delete 1", fixture.input().getText());
                assertTrue(fixture.input().getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
                assertEquals(original, fixture.app.snapshot());
                assertSame(selected, fixture.people().getSelectionModel().getSelectedItem());
                assertSame(displayed, fixture.lessons().getItems());
                assertTrue(displayed.isEmpty());
                assertEquals("Student S9 · current lessons", ((Label) fixture.root()
                        .lookup("#lessonContext")).getText());

                Files.delete(fixture.dataPath);
                Files.move(backup, fixture.dataPath);
                fixture.enter("delete 1");
                assertEquals("Deleted student: Older Student (S9).", fixture.feedback());
                assertCard(fixture, "Student · ID: S3", "Selected Student", 1);
                assertEquals(Optional.of(PersonRole.STUDENT), fixture.app.roleFilter());
                assertSame(displayed, fixture.lessons().getItems());
                assertTrue(displayed.isEmpty());
                assertEquals("Student S9 · current lessons", ((Label) fixture.root()
                        .lookup("#lessonContext")).getText());
                fixture.enter("lessons");
                assertSame(displayed, fixture.lessons().getItems());
                assertEquals(List.of("L1"), lessonIds(fixture));
                assertEquals("Students enrolled: 0", ((Label) fixture.root()
                        .lookup("#lessonRosterSize")).getText());
                assertEquals(fixture.app.snapshot(),
                        new JsonPonHubDataStorage(fixture.dataPath).readData().orElseThrow());
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
                assertCard(fixture, "Student · ID: S3", "Selected Student", 2);
                assertTrue(Files.isDirectory(fixture.dataPath));
                Files.delete(fixture.dataPath);
                Files.move(backup, fixture.dataPath);
                fixture.enter(addition);
                assertEquals("New student added: New Student (S10).", fixture.feedback());
                assertEquals(Optional.empty(), fixture.app.roleFilter());
                assertEquals(5, fixture.people().getItems().size());
                fixture.people().scrollTo(4);
                fixture.layout();
                assertCard(fixture, "Student · ID: S10", "New Student", 5);
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
                fixture.enter("search c/student");
                var snapshot = fixture.app.snapshot();
                var visible = List.copyOf(fixture.people().getItems());
                var settings = fixture.app.getLogic().getGuiSettings();
                fixture.people().getSelectionModel().select(1);
                String draft = "editlesson lid/L1 s/Unfinished";
                fixture.input().setText(draft);
                MenuBar menu = (MenuBar) fixture.root().lookup("#menuBar");
                menu.getMenus().get(1).getItems().get(0).fire();
                String overview = CanonicalCommandCatalog.getOverview(Optional.empty());
                assertEquals(overview, fixture.feedback());
                for (var target : List.of(fixture.input(), fixture.root().lookup("#resultDisplay"))) {
                    Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F1,
                            false, false, false, false));
                    assertEquals(overview, fixture.feedback());
                    assertEquals(draft, fixture.input().getText());
                    assertEquals(new PersonId("S3"), fixture.people().getSelectionModel().getSelectedItem().getId());
                }
                for (String topic : List.of("edit", "EDIT", "editlesson", "EDITLESSON")) {
                    fixture.enter("help " + topic);
                    assertEquals(CanonicalCommandCatalog.getUsage(topic.toLowerCase(Locale.ROOT), false)
                            .orElseThrow(), fixture.feedback());
                    assertTrue(fixture.input().getText().isEmpty());
                    assertEquals(new PersonId("S3"), fixture.people().getSelectionModel().getSelectedItem().getId());
                }
                assertEquals(visible, fixture.people().getItems());
                assertEquals(Optional.of(PersonRole.STUDENT), fixture.app.roleFilter());
                assertEquals(snapshot, fixture.app.snapshot());
                assertEquals(settings, fixture.app.getLogic().getGuiSettings());
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
                String draft = "add r/student n/Unfinished";
                fixture.input().setText(draft);
                MenuBar menu = (MenuBar) fixture.root().lookup("#menuBar");
                menu.getMenus().get(1).getItems().get(0).fire();
                String guidance = fixture.feedback();
                assertTrue(fixture.feedback().contains("Operational writes are blocked"));
                assertFalse(fixture.feedback().contains("add -"));
                for (var target : List.of(fixture.input(), fixture.root().lookup("#resultDisplay"))) {
                    Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F1,
                            false, false, false, false));
                    assertEquals(guidance, fixture.feedback());
                    assertEquals(draft, fixture.input().getText());
                    assertArrayEquals(original, Files.readAllBytes(dataPath));
                }
                fixture.enter("list");
                assertTrue(fixture.feedback().contains("Showing 0 person(s)."));
                assertTrue(fixture.feedback().contains("An empty view is not your saved data"));
                assertTrue(fixture.people().getItems().isEmpty());
                fixture.enter("add r/student n/Rejected Student l/P1 pp/91234567");
                assertTrue(fixture.feedback().contains("Operational writes are blocked"));
                for (String command : List.of("help EDIT", "help EDITLESSON", "edit invalid", "editlesson invalid")) {
                    fixture.enter(command);
                    assertEquals(command, fixture.input().getText());
                    assertTrue(fixture.input().getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
                    assertFalse(fixture.feedback().isBlank());
                    if (!command.startsWith("help")) {
                        assertTrue(fixture.feedback().contains("Operational writes are blocked"));
                    }
                    assertTrue(fixture.people().getItems().isEmpty());
                    assertArrayEquals(original, Files.readAllBytes(dataPath));
                }
                fixture.enter("exit");
            }
            assertArrayEquals(original, Files.readAllBytes(dataPath));
            return null;
        });
    }

    @Test
    public void tab_normalAndProtectedWindows_useSessionRoutesWithoutExecuting() throws Exception {
        Path path = directory.resolve("canonical.json");
        for (boolean isProtected : List.of(false, true)) {
            if (isProtected) {
                Files.writeString(path, "{broken json");
            }
            callOnFxThread(() -> {
                try (WindowFixture fixture = createWindow()) {
                    String originalFeedback = fixture.feedback();
                    for (String prefix : List.of("h", "li", "e", "sho")) {
                        fixture.input().setText(prefix);
                        fixture.input().end();
                        Event.fireEvent(fixture.input(), new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB,
                                false, false, false, false));
                        String expected = switch (prefix) {
                            case "h" -> isProtected ? "help" : "history";
                            case "li" -> "list";
                            case "e" -> isProtected ? "exit" : "edit";
                            default -> isProtected ? "sho" : "showlesson";
                        };
                        assertEquals(expected, fixture.input().getText());
                        assertEquals(originalFeedback, fixture.feedback());
                        assertTrue(fixture.people().getItems().isEmpty());
                    }
                    fixture.input().setText("he");
                    fixture.input().end();
                    Event.fireEvent(fixture.input(), new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB,
                            false, false, false, false));
                    Event.fireEvent(fixture.input(), new ActionEvent());
                    assertTrue(fixture.input().getText().isEmpty());
                    assertTrue(fixture.feedback().contains("PonHub commands"));
                }
                return null;
            });
            if (isProtected) {
                assertEquals("{broken json", Files.readString(path));
            } else {
                assertFalse(Files.exists(path));
            }
        }
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

    private static void assertCard(WindowFixture fixture, String identity, String heading, int position) {
        fixture.layout();
        Region card = fixture.root().lookupAll("#cardPane").stream()
                .filter(node -> identity.equals(((Label) node.lookup("#identity")).getText()))
                .map(Region.class::cast).findFirst().orElseThrow(() -> new AssertionError(
                        "Missing visible card " + identity + "; rendered=" + fixture.root().lookupAll("#identity")
                                .stream().map(node -> ((Label) node).getText()).toList()));
        assertEquals(heading, ((Label) card.lookup("#heading")).getText());
        assertEquals("Position: " + position, ((Label) card.lookup("#position")).getText());
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
            history().layout();
            for (String id : List.of("lessonContext", "lessonSummary", "historyContext", "historySummary")) {
                Label heading = (Label) root().lookup("#" + id);
                assertNotNull(heading.getTooltip(), id);
                assertEquals(heading.getText(), heading.getTooltip().getText(), id);
            }
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

        @SuppressWarnings("unchecked")
        private ListView<AttendanceHistoryEntry> history() {
            return (ListView<AttendanceHistoryEntry>) root().lookup("#historyListView");
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
