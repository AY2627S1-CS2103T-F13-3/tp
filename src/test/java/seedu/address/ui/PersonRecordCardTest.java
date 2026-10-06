package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import seedu.address.MainApp;
import seedu.address.model.person.Address;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Parent;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Student;
import seedu.address.model.person.Tutor;

public class PersonRecordCardTest {

    private static final int FX_TIMEOUT_SECONDS = 15;

    private static boolean hasStartedToolkit;

    @BeforeAll
    public static void startToolkit() throws Exception {
        FutureTask<Void> ready = new FutureTask<>(() -> Platform.setImplicitExit(false), null);
        try {
            Platform.startup(ready);
            hasStartedToolkit = true;
        } catch (IllegalStateException e) {
            // Reuse a toolkit already initialized by another test without attempting to restart it.
            Platform.runLater(ready);
        }
        ready.get(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @AfterAll
    public static void stopToolkit() {
        if (hasStartedToolkit) {
            Platform.exit();
        }
    }

    @Test
    public void constructor_studentWithoutOptionalContacts_rendersPositionIdentityAndRequiredFields() throws Exception {
        Student student = new Student(new PersonId("S" + Long.MAX_VALUE), new ContactDetails(new Name("Casey Tan")),
                new EducationLevel("JC2"), new Phone("00987654"));

        onFxThread(() -> {
            Region root = new PersonRecordCard(student, 2).getRoot();
            layoutScene(root, 600, 480);

            assertCardText(root, "2. Casey Tan", "Student · S9223372036854775807",
                    List.of("Level: JC2", "Parent phone: 00987654", "Own phone: Not provided",
                            "Email: Not provided", "Address: Not provided"));
        });
    }

    @Test
    public void constructor_populatedStudent_rendersOwnAndParentContactsSeparately() throws Exception {
        ContactDetails contacts = populatedContacts();
        Student student = new Student(new PersonId("S12"), contacts, new EducationLevel("S2"),
                new Phone("00987654"));

        onFxThread(() -> {
            Region root = new PersonRecordCard(student, 4).getRoot();
            layoutScene(root, 600, 480);

            assertCardText(root, "4. " + contacts.getName().fullName, "Student · S12",
                    List.of("Level: S2", "Parent phone: 00987654", "Own phone: 00012345",
                            "Email: Alex.Tan@Example.com",
                            "Address: " + contacts.getAddress().orElseThrow().value));
        });
    }

    @Test
    public void constructor_populatedTutor_rendersOwnContactsWithoutStudentFields() throws Exception {
        ContactDetails contacts = populatedContacts();
        Tutor tutor = new Tutor(new PersonId("T41"), contacts);

        onFxThread(() -> {
            Region root = new PersonRecordCard(tutor, 7).getRoot();
            layoutScene(root, 600, 480);

            assertCardText(root, "7. " + contacts.getName().fullName, "Tutor · T41",
                    List.of("Phone: 00012345", "Email: Alex.Tan@Example.com",
                            "Address: " + contacts.getAddress().orElseThrow().value));
        });
    }

    @Test
    public void constructor_parentWithoutOptionalContacts_rendersOwnPhoneAndMissingFields() throws Exception {
        Parent parent = new Parent(new PersonId("P11"), new ContactDetails(new Name("Beatrice Tan"),
                Optional.of(new Phone("00987654")), Optional.empty(), Optional.empty()));

        onFxThread(() -> {
            Region root = new PersonRecordCard(parent, 3).getRoot();
            layoutScene(root, 600, 480);

            assertCardText(root, "3. Beatrice Tan", "Parent · P11",
                    List.of("Phone: 00987654", "Email: Not provided", "Address: Not provided"));
        });
    }

    @Test
    public void layout_narrowAndRegularPeopleViews_wrapCompleteValuesAndReachLastCard() throws Exception {
        onFxThread(() -> {
            for (int width : new int[] {320, 853}) {
                ListView<PersonRecord> view = PersonRecordCardPreview.createPeopleView();
                layoutScene(view, width, 480);
                view.scrollTo(1);
                view.layout();

                Region tutorCard = findCard(view, "Tutor · T1");
                long longLines = 0;
                for (var node : tutorCard.lookupAll(".label")) {
                    Label label = (Label) node;
                    assertTrue(label.isWrapText(), label.getText());
                    assertTrue(label.getWidth() > 0 && label.getWidth() <= width, label.getText());
                    assertTrue(label.getHeight() + 1 >= label.prefHeight(label.getWidth()), label.getText());
                    if (label.getText().length() > 100) {
                        longLines++;
                        if (width == 320) {
                            assertTrue(label.getHeight() > label.getFont().getSize() * 1.5,
                                    "Long text must occupy multiple lines: " + label.getText());
                        }
                    }
                }
                assertTrue(longLines >= 2, "The long email and address must both be rendered.");

                view.scrollTo(2);
                view.layout();
                for (Node node : view.lookupAll(".scroll-bar")) {
                    if (node instanceof ScrollBar bar && bar.isVisible()
                            && bar.getOrientation() == Orientation.VERTICAL) {
                        bar.setValue(bar.getMax());
                    }
                }
                view.layout();
                Region parentCard = findCard(view, "Parent · P1");
                Bounds parentBounds = parentCard.localToScene(parentCard.getBoundsInLocal());
                assertTrue(parentBounds.getMinY() >= 0 && parentBounds.getMaxY() <= view.getHeight() + 1,
                        "Scrolling must expose the complete last card: " + parentBounds
                                + "; viewport height=" + view.getHeight());
                assertCardText(parentCard, "3. Beatrice Tan", "Parent · P1",
                        List.of("Phone: 00987654", "Email: Not provided", "Address: Not provided"));
            }
        });
    }

    @Test
    public void constructor_invalidInput_rejectsBeforeLoadingControls() {
        Tutor tutor = new Tutor(new PersonId("T1"), populatedContacts());

        assertThrows(NullPointerException.class, () -> new PersonRecordCard(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new PersonRecordCard(tutor, 0));
        assertThrows(IllegalArgumentException.class, () -> new PersonRecordCard(tutor, -1));
    }

    /**
     * Executes toolkit-dependent assertions on the JavaFX thread with a bounded wait.
     */
    private static void onFxThread(Runnable assertions) throws Exception {
        FutureTask<Void> task = new FutureTask<>(assertions, null);
        Platform.runLater(task);
        try {
            task.get(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            task.cancel(false);
            throw e;
        }
    }

    /**
     * Applies the real theme and performs offscreen layout without opening a stage.
     */
    private static void layoutScene(Region root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(MainApp.class.getResource("/view/DarkTheme.css").toExternalForm());
        root.applyCss();
        root.resize(width, height);
        root.layout();
    }

    /**
     * Verifies the actual FXML heading, identity, and complete ordered detail labels.
     */
    private static void assertCardText(Region root, String heading, String identity, List<String> details) {
        Label headingLabel = (Label) root.lookup("#heading");
        Label identityLabel = (Label) root.lookup("#identity");
        VBox detailBox = (VBox) root.lookup("#details");
        assertNotNull(headingLabel);
        assertNotNull(identityLabel);
        assertNotNull(detailBox);
        assertEquals(heading, headingLabel.getText());
        assertEquals(identity, identityLabel.getText());
        assertEquals(details, detailBox.getChildren().stream().map(node -> ((Label) node).getText()).toList());
    }

    /**
     * Finds a rendered card by stable identity after the virtualized list has scrolled.
     */
    private static Region findCard(ListView<PersonRecord> view, String identity) {
        return view.lookupAll("#cardPane").stream()
                .filter(node -> identity.equals(((Label) node.lookup("#identity")).getText()))
                .map(Region.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Card is not reachable: " + identity));
    }

    private static ContactDetails populatedContacts() {
        return new ContactDetails(new Name("Alex  TAN "), Optional.of(new Phone("00012345")),
                Optional.of(new Email("Alex.Tan@Example.com")), Optional.of(new Address("12  Main Street ")));
    }
}
