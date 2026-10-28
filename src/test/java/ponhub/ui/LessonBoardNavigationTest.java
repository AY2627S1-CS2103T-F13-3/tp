package ponhub.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.FxTestUtil.callOnFxThread;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import ponhub.MainApp;
import ponhub.logic.Logic;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.model.PonHubData;
import ponhub.model.PonHubDataState;
import ponhub.model.attendance.Attendance;
import ponhub.model.attendance.AttendanceStatus;
import ponhub.model.lesson.LessonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.query.AttendanceHistoryEntry;
import ponhub.model.query.LessonDetailView;
import ponhub.model.query.LessonView;
import ponhub.storage.JsonPonHubDataStorage;
import ponhub.testutil.LessonQueryTestData;

/**
 * Exercises lesson-board routes against the real command pipeline and canonical saved data.
 */
public class LessonBoardNavigationTest {

    @TempDir
    Path directory;

    @Test
    public void initialCatalogue_explainsSelectionAndNamesAllThreeViewingShortcuts() throws Exception {
        seedRecordedData();
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.assertLessonPane("lessonsButton");
                assertEquals("All lessons", ((Button) fixture.root().lookup("#lessonsButton")).getText());
                assertEquals("Student lessons", ((Button) fixture.root().lookup("#studentLessonsButton")).getText());
                assertEquals("Attendance history", ((Button) fixture.root().lookup("#attendanceButton")).getText());
                Label guidance = (Label) fixture.root().lookup("#studentSelectionGuidance");
                assertEquals("Select a Student card for Student lessons or Attendance history.", guidance.getText());
                assertTrue(guidance.isWrapText());
                assertWithinScene(guidance, fixture.root().getWidth(), fixture.root().getHeight());
            }
            return null;
        });
    }

    @Test
    public void shownWindow_minimumStageSize_keepsNavigationCommandStatusAndBothViewportsAccessible() throws Exception {
        seedRecordedData();
        WindowFixture fixture = callOnFxThread(this::createWindow);
        try {
            callOnFxThread(() -> {
                Stage stage = fixture.window().getPrimaryStage();
                stage.show();
                stage.setWidth(stage.getMinWidth());
                stage.setHeight(stage.getMinHeight());
                return null;
            });
            // Native resize acknowledgements can arrive after several JavaFX event-queue turns.
            long resizeDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < resizeDeadline && !callOnFxThread(() -> {
                Stage stage = fixture.window().getPrimaryStage();
                return Math.abs(stage.getWidth() - 450) <= 1 && Math.abs(stage.getHeight() - 360) <= 1
                        && stage.getScene().getWidth() <= stage.getWidth()
                        && stage.getScene().getHeight() <= stage.getHeight();
            })) {
                TimeUnit.MILLISECONDS.sleep(20);
            }
            callOnFxThread(() -> {
                fixture.layoutShownWindow();
                Stage stage = fixture.window().getPrimaryStage();
                assertTrue(stage.isShowing());
                assertEquals(450, stage.getWidth(), 1);
                assertEquals(360, stage.getHeight(), 1);
                double contentWidth = stage.getScene().getWidth();
                double contentHeight = stage.getScene().getHeight();
                assertTrue(contentWidth <= stage.getWidth());
                assertTrue(contentHeight <= stage.getHeight());
                for (String id : List.of("lessonsButton", "studentLessonsButton", "attendanceButton",
                        "studentSelectionGuidance", "commandTextField", "statusbarPlaceholder",
                        "personListView", "lessonListView")) {
                    assertWithinScene(fixture.root().lookup("#" + id), contentWidth, contentHeight);
                }
                for (ListView<?> list : List.of(fixture.people(), fixture.lessons())) {
                    Node viewport = list.lookup(".clipped-container");
                    assertWithinScene(viewport, contentWidth, contentHeight);
                    Bounds bounds = viewport.localToScene(viewport.getBoundsInLocal());
                    assertTrue(bounds.getWidth() >= 100, list.getId() + " must retain a usable viewport width");
                    assertTrue(bounds.getHeight() >= 40, list.getId() + " must retain a usable viewport height");
                }
                return null;
            });
        } finally {
            callOnFxThread(() -> {
                fixture.close();
                return null;
            });
        }
    }

    @Test
    public void typedLessonRoutes_fromHistory_useCommandSelectorsAndPreserveConflictingSelection() throws Exception {
        seedRecordedData();
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.people().getSelectionModel().select(3);
                PersonRecord selected = fixture.people().getSelectionModel().getSelectedItem();
                byte[] original = Files.readAllBytes(fixture.dataPath());
                PonHubDataState originalState = fixture.app().snapshot();
                List<PersonRecord> people = List.copyOf(fixture.people().getItems());

                fixture.enter("history 4");
                fixture.assertHistoryPane();
                fixture.enter("lessons si/2");
                fixture.assertLessonPane("studentLessonsButton");
                assertEquals(List.of("L2", "L10"), fixture.lessonIds());
                assertEquals("Student S1 · current lessons", fixture.label("lessonContext"));

                fixture.enter("history 4");
                fixture.enter("search c/lesson s/Science");
                fixture.assertLessonPane("");
                assertEquals(List.of("L2", "L3"), fixture.lessonIds());
                assertEquals("Lesson search: s/science", fixture.label("lessonContext"));

                fixture.enter("history 4");
                fixture.enter("showlesson lid/L10 d/2026-10-12");
                fixture.assertLessonPane("");
                assertTrue(fixture.detail().isVisible());
                assertEquals(List.of("L10"), fixture.lessonIds());
                assertEquals("Math", ((Label) fixture.detail().lookup("#lessonHeading")).getText());
                assertEquals("Lesson L10", ((Label) fixture.detail().lookup("#lessonIdentity")).getText());
                Label schedule = (Label) fixture.detail().lookup("#lessonSchedule");
                assertEquals("Mon 0900-1000", schedule.getText());
                assertEquals(people, fixture.people().getItems());
                assertSame(selected, fixture.people().getSelectionModel().getSelectedItem());
                assertEquals(originalState, fixture.app().snapshot());
                assertArrayEquals(original, Files.readAllBytes(fixture.dataPath()));
            }
            return null;
        });
    }

    @Test
    public void typedHistory_fromDetail_usesMixedAndFilteredPeoplePositionsWithoutChangingSelection() throws Exception {
        seedRecordedData();
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.people().getSelectionModel().select(1);
                PersonRecord selected = fixture.people().getSelectionModel().getSelectedItem();
                byte[] original = Files.readAllBytes(fixture.dataPath());

                fixture.enter("showlesson lid/L2");
                fixture.enter("history 4");
                fixture.assertHistoryPane();
                assertEquals("Student S2 · attendance history", fixture.label("historyContext"));
                assertEquals(List.of("S2/L11/2026-10-07/PRESENT", "S2/L10/2026-10-05/ABSENT"),
                        fixture.historyKeys());
                fixture.enter("history 4 lid/L10");
                assertEquals(List.of("S2/L10/2026-10-05/ABSENT"), fixture.historyKeys());
                assertSame(selected, fixture.people().getSelectionModel().getSelectedItem());

                fixture.enter("search c/student n/Alex");
                fixture.people().getSelectionModel().select(0);
                assertEquals(List.of("S1", "S3"), fixture.peopleIds());
                fixture.enter("showlesson lid/L10");
                fixture.enter("history 2");
                fixture.assertHistoryPane();
                assertEquals("Student S3 · attendance history", fixture.label("historyContext"));
                assertTrue(fixture.history().getItems().isEmpty());
                assertSame(selected, fixture.people().getSelectionModel().getSelectedItem());
                assertEquals(List.of("S1", "S3"), fixture.peopleIds());
                assertArrayEquals(original, Files.readAllBytes(fixture.dataPath()));
            }
            return null;
        });
    }

    @Test
    public void emptyLessonAndHistoryRoutes_fromOpposingPane_keepExplicitEmptyContexts() throws Exception {
        seedRecordedData();
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                byte[] original = Files.readAllBytes(fixture.dataPath());
                fixture.enter("list r/student");
                fixture.people().getSelectionModel().select(0);
                PersonRecord selected = fixture.people().getSelectionModel().getSelectedItem();
                fixture.enter("history 2");
                fixture.enter("lessons si/3");
                fixture.assertLessonPane("studentLessonsButton");
                assertTrue(fixture.lessons().getItems().isEmpty());
                assertEquals("Student S3 · current lessons", fixture.label("lessonContext"));
                assertEquals("No lessons to display.", ((Label) fixture.lessons().getPlaceholder()).getText());

                fixture.enter("history 2 lid/L3");
                fixture.assertHistoryPane();
                assertTrue(fixture.history().getItems().isEmpty());
                assertEquals("Student S2 · attendance history · L3", fixture.label("historyContext"));
                Label placeholder = (Label) fixture.history().getPlaceholder();
                assertEquals("No recorded attendance. Missing dates are unrecorded.",
                        placeholder.getText());
                fixture.enter("search c/lesson s/Nonexistent");
                fixture.assertLessonPane("");
                assertTrue(fixture.lessons().getItems().isEmpty());
                assertEquals("Lesson search: s/nonexistent", fixture.label("lessonContext"));
                assertSame(selected, fixture.people().getSelectionModel().getSelectedItem());
                assertArrayEquals(original, Files.readAllBytes(fixture.dataPath()));
            }
            return null;
        });
    }

    @Test
    public void datedDetail_distinguishesUnrecordedCurrentMemberFromRetainedFormerRecords() throws Exception {
        PonHubDataState source = LessonQueryTestData.createData().exportState();
        var lessons = source.lessons().stream().map(lesson ->
                lesson.getId().equals(LessonQueryTestData.SHARED_LESSON)
                        ? lesson.withoutEnrolledStudent(LessonQueryTestData.STUDENT_TWO) : lesson).toList();
        List<Attendance> attendance = List.of(
                Attendance.fromStrings(LessonQueryTestData.STUDENT_TWO, LessonQueryTestData.SHARED_LESSON,
                        "2026-10-05", "absent"),
                Attendance.fromStrings(LessonQueryTestData.STUDENT_TWO, LessonQueryTestData.SHARED_LESSON,
                        "2026-09-28", "present"));
        new JsonPonHubDataStorage(dataPath()).saveData(
                new PonHubDataState(source.people(), lessons, attendance, source.lastAllocatedLessonSequence()));
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                byte[] original = Files.readAllBytes(fixture.dataPath());
                fixture.enter("history 4 lid/L10");
                fixture.enter("showlesson lid/L10 d/2026-10-05");
                fixture.assertLessonPane("");
                LessonDetailView dated = (LessonDetailView) fixture.lessons().getItems().getFirst();
                assertEquals(1, dated.currentRoster().size());
                assertEquals(LessonQueryTestData.STUDENT_ONE, dated.currentRoster().getFirst().student().getId());
                assertTrue(dated.currentRoster().getFirst().status().isEmpty());
                assertEquals(1, dated.formerAttendance().size());
                assertEquals(AttendanceStatus.ABSENT, dated.formerAttendance().getFirst().status());
                assertTrue(fixture.detailLabels().stream()
                        .anyMatch(text -> text.contains("S1 · Alex Tan")));
                assertTrue(fixture.detailLabels().stream()
                        .anyMatch(text -> text.contains("Attendance: unrecorded")));
                assertTrue(fixture.detailLabels().stream()
                        .anyMatch(text -> text.contains("S2 · Jamie Tan")));
                assertTrue(fixture.detailLabels().stream()
                        .anyMatch(text -> text.contains("Status: absent")));

                fixture.enter("showlesson lid/L10");
                LessonDetailView undated = (LessonDetailView) fixture.lessons().getItems().getFirst();
                assertEquals(List.of(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 9, 28)),
                        undated.formerAttendance().stream().map(LessonDetailView.FormerAttendance::date).toList());
                assertArrayEquals(original, Files.readAllBytes(fixture.dataPath()));
            }
            return null;
        });
    }

    @Test
    public void committedEdits_refreshSelectedSearchAndDetailWithoutChangingTheirStableIdentity() throws Exception {
        seedRecordedData();
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.enter("search c/lesson s/Math");
                fixture.enter("editlesson lid/L4 s/Geometry");
                fixture.assertLessonPane("");
                assertEquals(List.of("L10"), fixture.lessonIds());
                assertEquals("Lesson search: s/math", fixture.label("lessonContext"));

                fixture.enter("search c/tutor p/00112233");
                fixture.people().getSelectionModel().select(0);
                fixture.enter("edit 1 n/Mei Updated");
                assertEquals("Mei Updated", fixture.lessons().getItems().getFirst().getTutorName());
                fixture.enter("showlesson lid/L10 d/2026-10-05");
                fixture.enter("list r/student");
                fixture.people().getSelectionModel().select(0);
                fixture.enter("edit 1 n/Alex Updated");
                fixture.assertLessonPane("");
                LessonDetailView detail = (LessonDetailView) fixture.lessons().getItems().getFirst();
                assertEquals(LessonQueryTestData.SHARED_LESSON, detail.getLessonId());
                assertEquals("Mei Updated", detail.getTutorName());
                assertTrue(detail.currentRoster().stream().anyMatch(member ->
                        member.student().getId().equals(LessonQueryTestData.STUDENT_ONE)
                                && member.student().getName().fullName.equals("Alex Updated")));
                assertEquals("S1", fixture.people().getSelectionModel().getSelectedItem().getId().toString());
                byte[] afterEdits = Files.readAllBytes(fixture.dataPath());
                PonHubDataState editedState = fixture.app().snapshot();
                fixture.enter("history 1 lid/L10");
                fixture.enter("showlesson lid/L10 d/2026-10-05");
                assertEquals(editedState, fixture.app().snapshot());
                assertArrayEquals(afterEdits, Files.readAllBytes(fixture.dataPath()));
            }
            return null;
        });
    }

    @Test
    public void navigationButtons_matchTypedQueriesAndPreserveDraftCaretSelectionAndSavedData() throws Exception {
        seedRecordedData();
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.people().getSelectionModel().select(3);
                PersonRecord selected = fixture.people().getSelectionModel().getSelectedItem();
                byte[] original = Files.readAllBytes(fixture.dataPath());
                fixture.enter("lessons si/4");
                List<String> expectedLessons = fixture.lessonIds();
                String studentContext = fixture.label("lessonContext");
                String studentFeedback = fixture.feedback();
                fixture.enter("history 4");
                fixture.draft();
                fixture.click("studentLessonsButton");
                fixture.assertLessonPane("studentLessonsButton");
                assertEquals(expectedLessons, fixture.lessonIds());
                assertEquals(studentContext, fixture.label("lessonContext"));
                assertEquals(studentFeedback, fixture.feedback());
                fixture.assertDraft();

                fixture.enter("history 4");
                List<String> expectedHistory = fixture.historyKeys();
                String historyContext = fixture.label("historyContext");
                String historyFeedback = fixture.feedback();
                fixture.enter("showlesson lid/L2");
                fixture.draft();
                fixture.click("attendanceButton");
                fixture.assertHistoryPane();
                assertEquals(expectedHistory, fixture.historyKeys());
                assertEquals(historyContext, fixture.label("historyContext"));
                assertEquals(historyFeedback, fixture.feedback());
                fixture.assertDraft();

                fixture.enter("lessons");
                List<String> allLessons = fixture.lessonIds();
                String allFeedback = fixture.feedback();
                fixture.enter("history 4");
                fixture.draft();
                fixture.click("lessonsButton");
                fixture.assertLessonPane("lessonsButton");
                assertEquals(allLessons, fixture.lessonIds());
                assertEquals("All lessons", fixture.label("lessonContext"));
                assertEquals(allFeedback, fixture.feedback());
                fixture.assertDraft();
                assertSame(selected, fixture.people().getSelectionModel().getSelectedItem());

                fixture.enter("search c/student n/Jamie");
                fixture.people().getSelectionModel().select(0);
                fixture.click("studentLessonsButton");
                fixture.assertLessonPane("studentLessonsButton");
                assertEquals(expectedLessons, fixture.lessonIds());
                fixture.click("attendanceButton");
                fixture.assertHistoryPane();
                assertEquals(expectedHistory, fixture.historyKeys());
                assertArrayEquals(original, Files.readAllBytes(fixture.dataPath()));
            }
            return null;
        });
    }

    @Test
    public void studentNavigation_invalidSelection_preservesPreviousResultsAndDraft() throws Exception {
        seedRecordedData();
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.enter("history 4 lid/L10");
                List<String> expectedHistory = fixture.historyKeys();
                byte[] original = Files.readAllBytes(fixture.dataPath());
                for (int index : List.of(-1, 0, 2)) {
                    if (index < 0) {
                        fixture.people().getSelectionModel().clearSelection();
                    } else {
                        fixture.people().getSelectionModel().select(index);
                    }
                    for (String button : List.of("studentLessonsButton", "attendanceButton")) {
                        fixture.draft();
                        fixture.click(button);
                        fixture.assertHistoryPane();
                        assertEquals(expectedHistory, fixture.historyKeys());
                        assertEquals("Student S2 · attendance history · L10", fixture.label("historyContext"));
                        assertTrue(fixture.feedback().toLowerCase(Locale.ROOT).contains("student"));
                        fixture.assertDraft();
                    }
                }

                PersonRecord stale = fixture.people().getItems().get(3);
                fixture.enter("list r/tutor");
                fixture.people().getSelectionModel().select(stale);
                assertEquals(-1, fixture.people().getSelectionModel().getSelectedIndex());
                for (String button : List.of("studentLessonsButton", "attendanceButton")) {
                    fixture.draft();
                    fixture.click(button);
                    fixture.assertHistoryPane();
                    assertEquals(expectedHistory, fixture.historyKeys());
                    assertTrue(fixture.feedback().toLowerCase(Locale.ROOT).contains("student"));
                    fixture.assertDraft();
                }
                assertArrayEquals(original, Files.readAllBytes(fixture.dataPath()));
            }
            return null;
        });
    }

    @Test
    public void navigationButtons_protectedSession_preservesRejectedBytesAndDraft() throws Exception {
        Files.writeString(dataPath(), "{broken json");
        byte[] original = Files.readAllBytes(dataPath());
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.assertLessonPane("lessonsButton");
                fixture.enter("lessons");
                fixture.assertLessonPane("lessonsButton");
                String protectedFeedback = fixture.feedback();
                assertTrue(protectedFeedback.contains("Operational writes are blocked"));
                fixture.draft();
                fixture.click("lessonsButton");
                assertEquals(protectedFeedback, fixture.feedback());
                fixture.assertLessonPane("lessonsButton");
                fixture.assertDraft();
                for (String button : List.of("studentLessonsButton", "attendanceButton")) {
                    fixture.click(button);
                    assertFalse(fixture.feedback().isBlank());
                    fixture.assertLessonPane("lessonsButton");
                    fixture.assertDraft();
                }
                assertTrue(fixture.people().getItems().isEmpty());
                assertTrue(fixture.lessons().getItems().isEmpty());
                assertTrue(fixture.history().getItems().isEmpty());
                assertArrayEquals(original, Files.readAllBytes(fixture.dataPath()));
            }
            return null;
        });
    }

    @Test
    public void selectionFilteringAndHelp_keepDisplayedStudentIdentityUntilExplicitShortcut() throws Exception {
        seedRecordedData();
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                byte[] original = Files.readAllBytes(fixture.dataPath());
                fixture.people().getSelectionModel().select(3);
                fixture.assertLessonPane("lessonsButton");
                fixture.enter("lessons si/2");
                List<String> lessons = fixture.lessonIds();
                fixture.assertLessonPane("studentLessonsButton");
                assertEquals("Student S1 · current lessons", fixture.label("lessonContext"));

                fixture.people().getSelectionModel().select(5);
                fixture.people().getSelectionModel().clearSelection();
                fixture.enter("list r/tutor");
                fixture.people().getSelectionModel().select(0);
                fixture.enter("help");
                fixture.assertLessonPane("studentLessonsButton");
                assertEquals(lessons, fixture.lessonIds());
                assertEquals("Student S1 · current lessons", fixture.label("lessonContext"));

                fixture.enter("search c/student n/Jamie");
                fixture.people().getSelectionModel().select(0);
                fixture.assertLessonPane("studentLessonsButton");
                assertEquals(lessons, fixture.lessonIds());
                fixture.draft();
                fixture.click("studentLessonsButton");
                fixture.assertLessonPane("studentLessonsButton");
                assertEquals(List.of("L10", "L11"), fixture.lessonIds());
                assertEquals("Student S2 · current lessons", fixture.label("lessonContext"));
                assertEquals(List.of("S2"), fixture.peopleIds());
                fixture.assertDraft();
                assertArrayEquals(original, Files.readAllBytes(fixture.dataPath()));
            }
            return null;
        });
    }

    @Test
    public void rejectedTypedRoutes_preserveEveryResultKindContextAndIndicator() throws Exception {
        seedRecordedData();
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                byte[] original = Files.readAllBytes(fixture.dataPath());
                fixture.people().getSelectionModel().select(3);
                PersonRecord selected = fixture.people().getSelectionModel().getSelectedItem();
                List<String> routes = List.of("lessons", "lessons si/4", "search c/lesson",
                        "search c/lesson s/Math", "showlesson lid/L3", "history 4 lid/L10");
                List<String> activeButtons = List.of("lessonsButton", "studentLessonsButton", "", "", "",
                        "attendanceButton");
                for (int route = 0; route < routes.size(); route++) {
                    fixture.enter(routes.get(route));
                    String activeButton = activeButtons.get(route);
                    fixture.assertActiveOnly(activeButton);
                    List<String> lessons = fixture.lessonIds();
                    List<String> history = fixture.historyKeys();
                    String lessonContext = fixture.label("lessonContext");
                    String historyContext = fixture.label("historyContext");
                    for (String command : List.of("lessons si/1", "lessons si/3", "lessons si/99", "lessons si/x",
                            "search c/lesson bad", "showlesson lid/L99", "showlesson lid/L10 d/2026-10-06",
                            "history 1", "history 3", "history 99", "history 4 lid/L99")) {
                        fixture.enter(command);
                        fixture.assertActiveOnly(activeButton);
                        assertEquals(lessons, fixture.lessonIds());
                        assertEquals(history, fixture.historyKeys());
                        assertEquals(lessonContext, fixture.label("lessonContext"));
                        assertEquals(historyContext, fixture.label("historyContext"));
                        assertSame(selected, fixture.people().getSelectionModel().getSelectedItem());
                    }
                    fixture.enter("help");
                    fixture.assertActiveOnly(activeButton);
                }
                assertArrayEquals(original, Files.readAllBytes(fixture.dataPath()));
            }
            return null;
        });
    }

    @Test
    public void committedStudentRefresh_retainsStudentIndicatorWhenResultBecomesNonempty() throws Exception {
        new JsonPonHubDataStorage(dataPath()).saveData(LessonQueryTestData.createData().exportState());
        callOnFxThread(() -> {
            try (WindowFixture fixture = createWindow()) {
                fixture.enter("list r/student");
                fixture.enter("lessons si/3");
                fixture.assertLessonPane("studentLessonsButton");
                assertTrue(fixture.lessonIds().isEmpty());
                fixture.enter("enrol 3 lid/L3");
                fixture.assertLessonPane("studentLessonsButton");
                assertEquals(List.of("L3"), fixture.lessonIds());
                assertEquals("Student S3 · current lessons", fixture.label("lessonContext"));
                fixture.enter("unenrol 3 lid/L3");
                fixture.assertLessonPane("studentLessonsButton");
                assertTrue(fixture.lessonIds().isEmpty());
                fixture.enter("delete 3");
                fixture.enter("delete confirm");
                fixture.assertLessonPane("studentLessonsButton");
                assertTrue(fixture.lessonIds().isEmpty());
                assertEquals("Student S3 · current lessons", fixture.label("lessonContext"));
            }
            return null;
        });
    }

    @Test
    public void historyInvalidation_restoresRetainedLessonScopeIncludingUnavailableDetail() throws Exception {
        callOnFxThread(() -> {
            List<String> routes = List.of("lessons", "lessons si/6", "search c/lesson", "showlesson lid/L3");
            List<String> activeButtons = List.of("lessonsButton", "studentLessonsButton", "", "");
            for (int route = 0; route < routes.size(); route++) {
                new JsonPonHubDataStorage(dataPath()).saveData(LessonQueryTestData.createData().exportState());
                try (WindowFixture fixture = createWindow()) {
                    fixture.enter(routes.get(route));
                    String lessonContext = fixture.label("lessonContext");
                    fixture.enter("history 6");
                    fixture.assertHistoryPane();
                    fixture.enter("delete 6");
                    fixture.enter("delete confirm");
                    fixture.assertLessonPane(activeButtons.get(route));
                    assertEquals(lessonContext, fixture.label("lessonContext"));
                    assertEquals("Attendance history", fixture.label("historyContext"));
                    assertTrue(fixture.historyKeys().isEmpty());
                }
            }
            new JsonPonHubDataStorage(dataPath()).saveData(LessonQueryTestData.createData().exportState());
            try (WindowFixture fixture = createWindow()) {
                fixture.enter("showlesson lid/L3");
                fixture.enter("history 6 lid/L3");
                fixture.assertHistoryPane();
                fixture.enter("deletelesson lid/L3");
                fixture.assertLessonPane("");
                assertTrue(fixture.lessonIds().isEmpty());
                assertEquals("Lesson L3 is no longer available", fixture.label("lessonContext"));
            }
            return null;
        });
    }

    @Test
    public void helpFailureAndUnrelatedKey_preserveStudentResultsAndUnfinishedDraft() throws Exception {
        seedRecordedData();
        callOnFxThread(() -> {
            AppHarness app = new AppHarness();
            app.initializePaths(dataPath(), directory.resolve("help-failure-preferences.json"));
            Logic actual = app.getLogic();
            Logic rejectingHelp = (Logic) Proxy.newProxyInstance(Logic.class.getClassLoader(),
                    new Class<?>[] {Logic.class}, (proxy, method, arguments) -> {
                        if (method.getName().equals("execute") && arguments[0].equals("help")) {
                            throw new CommandException("Help is temporarily unavailable");
                        }
                        return method.invoke(actual, arguments);
                    });
            MainWindow window = new MainWindow(new Stage(), rejectingHelp, dataPath());
            window.fillInnerParts();
            try (WindowFixture fixture = new WindowFixture(app, window, dataPath())) {
                fixture.layout();
                fixture.enter("lessons si/2");
                List<String> lessons = fixture.lessonIds();
                String context = fixture.label("lessonContext");
                fixture.draft();
                window.handleHelp();
                assertEquals("Help is temporarily unavailable", fixture.feedback());
                fixture.assertLessonPane("studentLessonsButton");
                fixture.assertDraft();
                fixture.input().setText("Unrelated key draft");
                for (Node target : List.of(fixture.input(), fixture.root())) {
                    Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F2,
                            false, false, false, false));
                }
                assertEquals("Unrelated key draft", fixture.input().getText());
                assertEquals("Help is temporarily unavailable", fixture.feedback());
                fixture.assertLessonPane("studentLessonsButton");
                assertEquals(lessons, fixture.lessonIds());
                assertEquals(context, fixture.label("lessonContext"));
            }
            return null;
        });
    }

    private Path dataPath() {
        return directory.resolve("navigation.json");
    }

    private static void assertWithinScene(Node node, double width, double height) {
        assertTrue(node != null, "Required control must be present");
        Bounds bounds = node.localToScene(node.getBoundsInLocal());
        assertTrue(node.isVisible() && bounds.getWidth() > 0 && bounds.getHeight() > 0, node.getId());
        assertTrue(bounds.getMinX() >= -1 && bounds.getMinY() >= -1
                        && bounds.getMaxX() <= width + 1 && bounds.getMaxY() <= height + 1,
                node.getId() + " must fit inside the actual scene content: " + bounds);
    }

    private void seedRecordedData() throws IOException {
        PonHubData data = LessonQueryTestData.createData();
        data.markAttendance(LessonQueryTestData.STUDENT_ONE, LessonQueryTestData.SHARED_LESSON,
                LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT);
        data.markAttendance(LessonQueryTestData.STUDENT_TWO, LessonQueryTestData.SHARED_LESSON,
                LocalDate.of(2026, 10, 5), AttendanceStatus.ABSENT);
        data.markAttendance(LessonQueryTestData.STUDENT_TWO, new LessonId("L11"),
                LocalDate.of(2026, 10, 7), AttendanceStatus.PRESENT);
        new JsonPonHubDataStorage(dataPath()).saveData(data.exportState());
    }

    private WindowFixture createWindow() throws Exception {
        AppHarness app = new AppHarness();
        app.initializePaths(dataPath(), directory.resolve("navigation-preferences.json"));
        MainWindow window = new MainWindow(new Stage(), app.getLogic(), dataPath());
        window.fillInnerParts();
        WindowFixture fixture = new WindowFixture(app, window, dataPath());
        fixture.layout();
        return fixture;
    }

    private record WindowFixture(AppHarness app, MainWindow window, Path dataPath) implements AutoCloseable {
        private static final String DRAFT = "add r/student n/Unfinished";

        private Region root() {
            return (Region) window.getPrimaryStage().getScene().getRoot();
        }

        private void layout() {
            root().applyCss();
            root().resize(853, 600);
            root().layout();
            people().layout();
            lessons().layout();
            history().layout();
        }

        private void layoutShownWindow() {
            root().applyCss();
            root().requestLayout();
            root().layout();
            people().layout();
            lessons().layout();
            history().layout();
        }

        private TextField input() {
            return (TextField) root().lookup("#commandTextField");
        }

        private String feedback() {
            return ((TextArea) root().lookup("#resultDisplay")).getText();
        }

        private String label(String id) {
            return ((Label) root().lookup("#" + id)).getText();
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

        private ListView<?> detail() {
            return (ListView<?>) root().lookup("#lessonDetailList");
        }

        private List<String> detailLabels() {
            var labels = new ArrayList<String>();
            for (int row = 0; row < detail().getItems().size(); row++) {
                detail().scrollTo(row);
                detail().layout();
                labels.addAll(detail().lookupAll(".label").stream().map(Label.class::cast)
                        .map(Label::getText).toList());
            }
            detail().scrollTo(0);
            detail().layout();
            return labels;
        }

        private List<String> peopleIds() {
            return people().getItems().stream().map(PersonRecord::getId).map(Object::toString).toList();
        }

        private List<String> lessonIds() {
            return lessons().getItems().stream().map(LessonView::getLessonId).map(Object::toString).toList();
        }

        private List<String> historyKeys() {
            return history().getItems().stream().map(entry -> entry.getStudentId() + "/" + entry.getLessonId()
                    + "/" + entry.getDate() + "/" + entry.getStatus()).toList();
        }

        private void enter(String command) {
            input().setText(command);
            Event.fireEvent(input(), new ActionEvent());
            layout();
        }

        private void click(String id) {
            Button button = (Button) root().lookup("#" + id);
            button.fire();
            layout();
        }

        private void draft() {
            input().setText(DRAFT);
            input().selectRange(3, 8);
        }

        private void assertDraft() {
            assertEquals(DRAFT, input().getText());
            assertEquals(3, input().getAnchor());
            assertEquals(8, input().getCaretPosition());
        }

        private void assertLessonPane(String activeButton) {
            assertTrue(lessons().getParent().isVisible());
            assertFalse(history().getParent().isVisible());
            assertActiveOnly(activeButton);
        }

        private void assertHistoryPane() {
            assertTrue(history().getParent().isVisible());
            assertFalse(lessons().getParent().isVisible());
            assertActiveOnly("attendanceButton");
        }

        private void assertActiveOnly(String activeButton) {
            for (String id : List.of("lessonsButton", "studentLessonsButton", "attendanceButton")) {
                assertEquals(id.equals(activeButton), root().lookup("#" + id).getPseudoClassStates()
                        .contains(PseudoClass.getPseudoClass("active")), id);
            }
        }

        @Override
        public void close() {
            window.getPrimaryStage().hide();
            app.stop();
        }
    }

    /**
     * Exposes the application's canonical initialization and snapshot boundary.
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
    }
}
