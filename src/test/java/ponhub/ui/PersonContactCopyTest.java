package ponhub.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.FxTestUtil.callOnFxThread;
import static ponhub.testutil.FxTestUtil.runOnFxThread;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Point3D;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ponhub.MainApp;
import ponhub.logic.Logic;
import ponhub.model.PonHubDataState;
import ponhub.model.person.Address;
import ponhub.model.person.ContactDetails;
import ponhub.model.person.EducationLevel;
import ponhub.model.person.Email;
import ponhub.model.person.Name;
import ponhub.model.person.Parent;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.Phone;
import ponhub.model.person.Student;
import ponhub.model.person.Tutor;

/**
 * Exercises real clipboard actions and input routing while restoring the user's original clipboard formats.
 */
public class PersonContactCopyTest {
    private static final DataFormat BINARY_FORMAT = new DataFormat("application/x-ponhub-clipboard-test");
    private static final Set<String> NATIVE_METADATA_TARGETS =
            Set.of("TARGETS", "TIMESTAMP", "MULTIPLE", "SAVE_TARGETS", "INCR");

    @TempDir
    Path directory;

    private Map<DataFormat, Object> originalClipboard;

    @BeforeEach
    public void preserveClipboard() throws Exception {
        originalClipboard = callOnFxThread(PersonContactCopyTest::snapshotClipboardContents);
    }

    @AfterEach
    public void restoreClipboard() throws Exception {
        if (originalClipboard != null) {
            runOnFxThread(() -> restoreClipboardContents(originalClipboard));
        }
    }

    @Test
    public void restoreClipboard_emptySnapshot_clearsCopiedContacts() throws Exception {
        runOnFxThread(() -> {
            setClipboard("temporary copied contact");
            restoreClipboardContents(Map.of());
            assertTrue(snapshotClipboardContents().isEmpty());
            assertNull(Clipboard.getSystemClipboard().getString());
        });
    }

    @Test
    public void restoreClipboard_nonemptySnapshot_restoresReadableTextImageFilesAndBinary() throws Exception {
        Path originalFile = Files.writeString(directory.resolve("original.txt"), "Original file");
        runOnFxThread(() -> {
            WritableImage image = new WritableImage(2, 1);
            image.getPixelWriter().setArgb(0, 0, 0xff123456);
            image.getPixelWriter().setArgb(1, 0, 0xffabcdef);
            Map<DataFormat, Object> original = Map.of(
                    DataFormat.PLAIN_TEXT, "Original clipboard text",
                    DataFormat.HTML, "<b>Original clipboard text</b>",
                    DataFormat.IMAGE, image,
                    DataFormat.FILES, List.of(originalFile.toFile()),
                    BINARY_FORMAT, ByteBuffer.wrap(new byte[] {0, 1, 2, -1}));
            Clipboard clipboard = Clipboard.getSystemClipboard();
            clipboard.setContent(original);
            Map<DataFormat, Object> snapshot = snapshotClipboardContents();
            assertTrue(snapshot.keySet().containsAll(original.keySet()));

            setClipboard("temporary copied contact");
            restoreClipboardContents(snapshot);

            assertEquals("Original clipboard text", clipboard.getString());
            assertEquals("<b>Original clipboard text</b>", clipboard.getHtml());
            assertEquals(List.of(originalFile.toFile()), clipboard.getFiles());
            assertEquals(ByteBuffer.wrap(new byte[] {0, 1, 2, -1}), clipboard.getContent(BINARY_FORMAT));
            assertEquals(0xff123456, clipboard.getImage().getPixelReader().getArgb(0, 0));
            assertEquals(0xffabcdef, clipboard.getImage().getPixelReader().getArgb(1, 0));
        });
    }

    @Test
    public void snapshotClipboard_unreadableAndEmptyFormats_retainsOnlyIndependentReadablePayloads() {
        ByteBuffer binary = ByteBuffer.wrap(new byte[] {1, 2, 3});
        Map<DataFormat, Object> advertised = new HashMap<>();
        advertised.put(DataFormat.PLAIN_TEXT, "Original text");
        advertised.put(DataFormat.HTML, null);
        advertised.put(DataFormat.FILES, List.of());
        advertised.put(BINARY_FORMAT, binary);
        DataFormat metadata = Optional.ofNullable(DataFormat.lookupMimeType("TARGETS"))
                .orElseGet(() -> new DataFormat("TARGETS"));
        advertised.put(metadata, ByteBuffer.wrap(new byte[] {0, 1}));

        Map<DataFormat, Object> snapshot = copyReadableClipboardContents(advertised);
        binary.put(0, (byte) 9);

        assertEquals(Map.of(DataFormat.PLAIN_TEXT, "Original text",
                BINARY_FORMAT, ByteBuffer.wrap(new byte[] {1, 2, 3})), snapshot);
    }

    @Test
    public void restoreClipboard_emptyFileList_clearsCopiedContacts() throws Exception {
        runOnFxThread(() -> {
            setClipboard("temporary copied contact");
            restoreClipboardContents(Map.of(DataFormat.FILES, List.of()));
            assertTrue(snapshotClipboardContents().isEmpty());
            assertNull(Clipboard.getSystemClipboard().getString());
        });
    }

    @Test
    public void copy_allRoles_preservesExactContactValuesAndProvidesAccessibleButtons() throws Exception {
        ContactDetails contacts = new ContactDetails(new Name("Alex Tan"), Optional.of(new Phone("00012345")),
                Optional.of(new Email("Alex.Tan@Example.com")), Optional.of(new Address("12 Main Street")));
        List<PersonRecord> people = List.of(
                new Student(new PersonId("S1"), contacts, new EducationLevel("P1"), new Phone("00987654")),
                new Tutor(new PersonId("T1"), contacts), new Parent(new PersonId("P1"), contacts));

        runOnFxThread(() -> {
            for (PersonRecord person : people) {
                Region card = new PersonRecordCard(person, 1).getRoot();
                layout(card);
                Map<String, String> expected = new HashMap<>(Map.of("Email", "Alex.Tan@Example.com",
                        "Address", "12 Main Street"));
                if (person instanceof Student) {
                    expected.put("Own phone", "00012345");
                    expected.put("Parent phone", "00987654");
                } else {
                    expected.put("Phone", "00012345");
                }
                assertEquals(expected.size(), card.lookupAll(".person-contact-copy").size());
                expected.forEach((caption, value) -> {
                    Button button = copyButton(card, caption, "Alex Tan");
                    assertTrue(button.isFocusTraversable());
                    assertEquals(AccessibleRole.BUTTON, button.getAccessibleRole());
                    assertTrue(button.getAccessibleHelp().contains("Space"));
                    button.fire();
                    assertEquals(value, Clipboard.getSystemClipboard().getString());
                });
                assertSame(contacts, person.getContactDetails());
            }
        });
    }

    @Test
    public void copy_missingValuesAndLiteralPlaceholder_onlyPresentContactsAreCopyable() throws Exception {
        Student student = new Student(new PersonId("S1"), new ContactDetails(new Name("Alex Tan")),
                new EducationLevel("P1"), new Phone("00987654"));
        Parent parent = new Parent(new PersonId("P1"), new ContactDetails(new Name("Sam Tan"),
                Optional.of(new Phone("00123456")), Optional.empty(), Optional.of(new Address("Not provided"))));

        runOnFxThread(() -> {
            Region studentCard = new PersonRecordCard(student, 1).getRoot();
            layout(studentCard);
            assertEquals(1, studentCard.lookupAll(".person-contact-copy").size());
            copyButton(studentCard, "Parent phone", "Alex Tan").fire();
            assertEquals("00987654", Clipboard.getSystemClipboard().getString());

            Region parentCard = new PersonRecordCard(parent, 2).getRoot();
            layout(parentCard);
            assertEquals(2, parentCard.lookupAll(".person-contact-copy").size());
            copyButton(parentCard, "Address", "Sam Tan").fire();
            assertEquals("Not provided", Clipboard.getSystemClipboard().getString());
        });
    }

    @Test
    public void keyboardCopy_usesPlatformModifierAndKeepsOrdinaryTextCopy() throws Exception {
        runOnFxThread(() -> {
            Student student = new Student(new PersonId("S1"), new ContactDetails(new Name("Alex Tan")),
                    new EducationLevel("P1"), new Phone("00987654"));
            Region card = new PersonRecordCard(student, 1).getRoot();
            layout(card);
            Button button = copyButton(card, "Parent phone", "Alex Tan");
            setClipboard("unchanged");
            Event.fireEvent(button, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.C,
                    false, false, false, false));
            assertEquals("unchanged", Clipboard.getSystemClipboard().getString());
            fireCopyShortcut(button);
            assertEquals("00987654", Clipboard.getSystemClipboard().getString());

            setClipboard("space sentinel");
            Event.fireEvent(button, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.SPACE,
                    false, false, false, false));
            Event.fireEvent(button, new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.SPACE,
                    false, false, false, false));
            assertEquals("00987654", Clipboard.getSystemClipboard().getString());

            for (var input : List.of(new TextField("command selection"), new TextArea("feedback selection"))) {
                new Scene(new StackPane(input));
                input.applyCss();
                input.selectRange(0, 7);
                fireCopyShortcut(input);
                assertEquals(input.getSelectedText(), Clipboard.getSystemClipboard().getString());
            }
        });
    }

    @Test
    public void mouseCopy_secondaryUnarmedAndOutsideRelease_leaveClipboardAndFocusUntouched() throws Exception {
        runOnFxThread(() -> {
            Student student = new Student(new PersonId("S1"), new ContactDetails(new Name("Alex Tan")),
                    new EducationLevel("P1"), new Phone("00987654"));
            Region card = new PersonRecordCard(student, 1).getRoot();
            TextField input = new TextField("Unfinished command");
            Region root = new VBox(card, input);
            layout(root);
            input.requestFocus();
            input.selectRange(3, 8);
            Button button = copyButton(card, "Parent phone", "Alex Tan");
            setClipboard("unchanged");

            fireMouse(button, MouseEvent.MOUSE_RELEASED, MouseButton.PRIMARY, 2, 2);
            fireMouse(button, MouseEvent.MOUSE_PRESSED, MouseButton.SECONDARY, 2, 2);
            fireMouse(button, MouseEvent.MOUSE_RELEASED, MouseButton.SECONDARY, 2, 2);
            fireMouse(button, MouseEvent.MOUSE_PRESSED, MouseButton.PRIMARY, 2, 2);
            fireMouse(button, MouseEvent.MOUSE_RELEASED, MouseButton.PRIMARY, -2, -2);

            assertEquals("unchanged", Clipboard.getSystemClipboard().getString());
            assertSame(input, root.getScene().getFocusOwner());
            assertEquals(3, input.getAnchor());
            assertEquals(8, input.getCaretPosition());
            assertFalse(button.isArmed());
        });
    }

    @Test
    public void mouseCopy_filteredWindow_preservesSelectionDraftFeedbackAndSavedData() throws Exception {
        callOnFxThread(() -> {
            AppHarness app = new AppHarness();
            Path dataPath = directory.resolve("people.json");
            app.initializePaths(dataPath, directory.resolve("preferences.json"));
            Logic logic = app.getLogic();
            logic.execute("add r/student n/Alex Tan l/P1 pp/00987654 e/Alex@Example.com");
            logic.execute("add r/student n/Sam Tan l/P2 pp/00123456");
            logic.execute("add r/tutor n/Mei Lim p/91234567");
            logic.execute("search c/student n/Tan");
            MainWindow window = new MainWindow(new Stage(), logic, dataPath);
            try {
                window.fillInnerParts();
                Region root = (Region) window.getPrimaryStage().getScene().getRoot();
                root.applyCss();
                root.resize(853, 900);
                root.layout();
                ListView<?> people = (ListView<?>) root.lookup("#personListView");
                people.getSelectionModel().select(1);
                Object selected = people.getSelectionModel().getSelectedItem();
                List<?> displayed = List.copyOf(people.getItems());
                PonHubDataState state = app.snapshot();
                byte[] bytes = Files.readAllBytes(dataPath);
                TextField input = (TextField) root.lookup("#commandTextField");
                input.requestFocus();
                input.setText("add r/student n/Unfinished");
                input.selectRange(3, 8);
                TextArea feedback = (TextArea) root.lookup("#resultDisplay");
                String feedbackBefore = feedback.getText();
                Button copy = copyButton(root, "Parent phone", "Alex Tan");
                click(copy);

                assertEquals("00987654", Clipboard.getSystemClipboard().getString());
                assertSame(selected, people.getSelectionModel().getSelectedItem());
                assertEquals(displayed, people.getItems());
                assertEquals("add r/student n/Unfinished", input.getText());
                assertSame(input, root.getScene().getFocusOwner());
                assertEquals(3, input.getAnchor());
                assertEquals(8, input.getCaretPosition());
                assertEquals(feedbackBefore, feedback.getText());
                assertEquals(state, app.snapshot());
                assertArrayEquals(bytes, Files.readAllBytes(dataPath));

                input.setText("edit 1 pp/00765432 e/");
                Event.fireEvent(input, new ActionEvent());
                root.layout();
                copyButton(root, "Parent phone", "Alex Tan").fire();
                assertEquals("00765432", Clipboard.getSystemClipboard().getString());
                assertFalse(root.lookupAll(".person-contact-copy").stream().map(Button.class::cast)
                        .anyMatch(button -> button.getAccessibleText().equals("Copy Email for Alex Tan")));
                assertEquals(displayed.stream().map(PersonRecord.class::cast).map(PersonRecord::getId).toList(),
                        people.getItems().stream().map(PersonRecord.class::cast).map(PersonRecord::getId).toList());
            } finally {
                window.getPrimaryStage().hide();
                app.stop();
            }
            return null;
        });
    }

    private static void layout(Region card) {
        Scene scene = new Scene(card, 320, 900);
        scene.getStylesheets().add(MainApp.class.getResource("/view/DarkTheme.css").toExternalForm());
        card.applyCss();
        card.resize(320, 900);
        card.layout();
    }

    private static Button copyButton(Region root, String caption, String name) {
        return root.lookupAll(".person-contact-copy").stream().map(Button.class::cast)
                .filter(button -> button.getAccessibleText().equals("Copy " + caption + " for " + name))
                .findFirst().orElseThrow();
    }

    private static Map<DataFormat, Object> snapshotClipboardContents() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        Map<DataFormat, Object> contents = new HashMap<>();
        for (DataFormat format : clipboard.getContentTypes()) {
            if (isClipboardPayloadFormat(format)) {
                contents.put(format, clipboard.getContent(format));
            }
        }
        return copyReadableClipboardContents(contents);
    }

    /**
     * Snapshots readable payloads, omitting advertised formats without data and empty native file lists.
     * Binary buffers must not share mutable storage with the clipboard being replaced.
     */
    private static Map<DataFormat, Object> copyReadableClipboardContents(Map<DataFormat, Object> contents) {
        Map<DataFormat, Object> snapshot = new HashMap<>();
        contents.forEach((format, value) -> {
            if (!isClipboardPayloadFormat(format) || value == null
                    || format == DataFormat.FILES && ((List<?>) value).isEmpty()
                    || value instanceof Image image && image.getPixelReader() == null) {
                return;
            }
            if (value instanceof ByteBuffer buffer) {
                value = ByteBuffer.allocate(buffer.remaining()).put(buffer.duplicate()).flip();
            } else if (value instanceof List<?> files) {
                value = List.copyOf(files);
            }
            snapshot.put(format, value);
        });
        return snapshot;
    }

    private static boolean isClipboardPayloadFormat(DataFormat format) {
        // GTK advertises selection-protocol targets even when no user payload exists.
        return format.getIdentifiers().stream().anyMatch(identifier -> !NATIVE_METADATA_TARGETS.contains(identifier));
    }

    private static void restoreClipboardContents(Map<DataFormat, Object> contents) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        Map<DataFormat, Object> readable = copyReadableClipboardContents(contents);
        if (readable.isEmpty()) {
            clipboard.clear();
            assertTrue(snapshotClipboardContents().isEmpty());
        } else {
            // OpenJFX can return false for a nonempty map containing no publishable payload.
            // Verify restored values instead of assuming map size or the return flag proves preservation.
            clipboard.setContent(readable);
            contents.forEach((format, value) -> {
                if (readable.containsKey(format)) {
                    assertClipboardValueEquals(value, clipboard.getContent(format), format);
                }
            });
        }
    }

    private static void assertClipboardValueEquals(Object expected, Object actual, DataFormat format) {
        if (expected instanceof Image expectedImage && actual instanceof Image actualImage) {
            assertEquals(expectedImage.getWidth(), actualImage.getWidth());
            assertEquals(expectedImage.getHeight(), actualImage.getHeight());
            for (int y = 0; y < expectedImage.getHeight(); y++) {
                for (int x = 0; x < expectedImage.getWidth(); x++) {
                    assertTrue(expectedImage.getPixelReader().getArgb(x, y)
                            == actualImage.getPixelReader().getArgb(x, y), "Restored image pixels differ");
                }
            }
        } else {
            assertTrue(Objects.deepEquals(expected, actual), "Restored clipboard content differs for " + format);
        }
    }

    private static void setClipboard(String value) {
        ClipboardContent contents = new ClipboardContent();
        contents.putString(value);
        Clipboard.getSystemClipboard().setContent(contents);
    }

    private static void fireCopyShortcut(Node target) {
        boolean isMac = System.getProperty("os.name").startsWith("Mac");
        Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.C,
                false, !isMac, false, isMac));
    }

    private static void click(Button button) {
        fireMouse(button, MouseEvent.MOUSE_PRESSED, MouseButton.PRIMARY, 2, 2);
        fireMouse(button, MouseEvent.MOUSE_RELEASED, MouseButton.PRIMARY, 2, 2);
    }

    private static void fireMouse(Button button, EventType<MouseEvent> type, MouseButton mouse, double x, double y) {
        boolean isPressed = type == MouseEvent.MOUSE_PRESSED;
        Event.fireEvent(button, new MouseEvent(type, x, y, x, y, mouse, 1,
                false, false, false, false, isPressed && mouse == MouseButton.PRIMARY, false,
                isPressed && mouse == MouseButton.SECONDARY, true, false, true,
                new PickResult(button, new Point3D(x, y, 0), 0)));
    }

    /**
     * Exposes the real application's initialization and immutable snapshot for state-preservation checks.
     */
    private static class AppHarness extends MainApp {
        private void initializePaths(Path dataPath, Path preferencesPath) throws IOException {
            initialize(dataPath, preferencesPath);
        }

        private Logic getLogic() {
            return logic;
        }

        private PonHubDataState snapshot() {
            return canonicalSession.getDataSnapshot();
        }
    }
}
