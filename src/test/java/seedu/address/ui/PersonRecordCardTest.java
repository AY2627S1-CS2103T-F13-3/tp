package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.FxTestUtil.runOnFxThread;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
import seedu.address.commons.core.index.Index;
import seedu.address.logic.PersonIndexResolver;
import seedu.address.logic.parser.PeopleListParser;
import seedu.address.model.PeopleView;
import seedu.address.model.PonHubData;
import seedu.address.model.PonHubDataState;
import seedu.address.model.person.Address;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Parent;
import seedu.address.model.person.PeopleRegistry;
import seedu.address.model.person.PeopleRegistryState;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Student;
import seedu.address.model.person.Tutor;
import seedu.address.model.util.PeopleSampleDataUtil;
import seedu.address.testutil.FxTestUtil;

public class PersonRecordCardTest {

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxTestUtil.initializeToolkit();
    }

    @Test
    public void constructor_studentWithoutOptionalContacts_rendersPositionIdentityAndRequiredFields() throws Exception {
        Student student = new Student(new PersonId("S" + Long.MAX_VALUE), new ContactDetails(new Name("Casey Tan")),
                new EducationLevel("JC2"), new Phone("00987654"));

        runOnFxThread(() -> {
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

        runOnFxThread(() -> {
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

        runOnFxThread(() -> {
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

        runOnFxThread(() -> {
            Region root = new PersonRecordCard(parent, 3).getRoot();
            layoutScene(root, 600, 480);

            assertCardText(root, "3. Beatrice Tan", "Parent · P11",
                    List.of("Phone: 00987654", "Email: Not provided", "Address: Not provided"));
        });
    }

    @Test
    public void layout_narrowAndRegularPeopleViews_wrapCompleteValuesAndReachLastCard() throws Exception {
        runOnFxThread(() -> {
            for (int width : new int[] {320, 853}) {
                Region panel = PersonRecordCardPreview.createPeopleView();
                layoutScene(panel, width, 480);
                ListView<PersonRecord> view = getRecordList(panel);
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
                assertTrue(parentBounds.getMinY() >= 0 && parentBounds.getMaxY() <= panel.getHeight() + 1,
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

    @Test
    public void panel_roleFiltering_rendersTheSamePeopleUsedByCommandIndices() throws Exception {
        PonHubData data = peopleData(PeopleSampleDataUtil.getSamplePeopleRegistry().exportState());
        PonHubDataState originalState = data.exportState();
        PeopleView peopleView = new PeopleView(data);
        peopleView.setRoleFilter(new PeopleListParser().parse("r/student"));
        assertEquals(new PersonId("S1"), PersonIndexResolver.resolve(Index.fromOneBased(1),
                peopleView.getPeople()).getId());

        runOnFxThread(() -> {
            Region panel = new PersonRecordListPanel(peopleView).getRoot();
            layoutScene(panel, 853, 480);
            ListView<PersonRecord> list = getRecordList(panel);
            assertTrue(list.getItems() == peopleView.getPeople());
            assertEquals("Showing 3 person(s).", ((Label) panel.lookup("#summary")).getText());
            Region studentCard = findCard(list, "Student · S1");
            assertEquals("1. Alex Tan", ((Label) studentCard.lookup("#heading")).getText());

            peopleView.setRoleFilter(Optional.empty());
            panel.layout();
            list.scrollTo(1);
            list.layout();
            studentCard = findCard(list, "Student · S1");
            assertEquals("2. Alex Tan", ((Label) studentCard.lookup("#heading")).getText());
            assertEquals(6, list.getItems().size());
            assertEquals("Showing 6 person(s).", ((Label) panel.lookup("#summary")).getText());
            assertTrue(list.getItems() == peopleView.getPeople());
            assertEquals(originalState, data.exportState());
        });
    }

    @Test
    public void panel_rootRefresh_keepsBindingAndClearsCardsForEmptyRole() throws Exception {
        PeopleRegistry registry = PeopleSampleDataUtil.getSamplePeopleRegistry();
        PonHubData data = peopleData(registry.exportState());
        PeopleView peopleView = new PeopleView(data);
        peopleView.setRoleFilter(Optional.of(PersonRole.PARENT));

        runOnFxThread(() -> {
            Region panel = new PersonRecordListPanel(peopleView).getRoot();
            layoutScene(panel, 853, 480);
            ListView<PersonRecord> list = getRecordList(panel);
            assertNotNull(findCard(list, "Parent · P1"));

            registry.remove(new PersonId("P1"), person -> false);
            data.resetData(peopleData(registry.exportState()).exportState());
            peopleView.refresh();
            panel.layout();
            assertTrue(list.getItems() == peopleView.getPeople());
            assertTrue(list.getItems().isEmpty());
            assertEquals("Showing 0 person(s).", ((Label) panel.lookup("#summary")).getText());
            assertEquals("No persons to display.", ((Label) list.getPlaceholder()).getText());
            assertTrue(list.lookupAll("#identity").isEmpty(), "Recycled cards must not show removed people.");
        });
    }

    @Test
    public void panel_compactAndRegularLayouts_wrapLongValuesAndReachLastCard() throws Exception {
        PeopleRegistryState state = new PeopleRegistryState(PersonRecordCardPreview.createRecords(),
                Map.of(PersonRole.STUDENT, Long.MAX_VALUE, PersonRole.TUTOR, 1L, PersonRole.PARENT, 1L));

        runOnFxThread(() -> {
            for (int width : new int[] {320, 853, 1536, 1920}) {
                Region panel = new PersonRecordListPanel(new PeopleView(peopleData(state))).getRoot();
                layoutScene(panel, width, 420);
                ListView<PersonRecord> list = getRecordList(panel);
                list.scrollTo(1);
                list.layout();
                Region tutorCard = findCard(list, "Tutor · T1");
                for (Node node : tutorCard.lookupAll(".label")) {
                    Label label = (Label) node;
                    assertTrue(label.isWrapText(), label.getText());
                    assertTrue(label.getWidth() > 0 && label.getWidth() <= width, label.getText());
                    assertTrue(label.getHeight() + 1 >= label.prefHeight(label.getWidth()), label.getText());
                }

                list.scrollTo(2);
                list.layout();
                for (Node node : list.lookupAll(".scroll-bar")) {
                    if (node instanceof ScrollBar bar && bar.isVisible()
                            && bar.getOrientation() == Orientation.VERTICAL) {
                        bar.setValue(bar.getMax());
                    }
                }
                list.layout();
                Region parentCard = findCard(list, "Parent · P1");
                Bounds bounds = parentCard.localToScene(parentCard.getBoundsInLocal());
                assertTrue(bounds.getMinY() >= 0 && bounds.getMaxY() <= panel.getHeight() + 1,
                        "The production host must expose the complete last card: " + bounds);
            }
        });
    }

    private static PonHubData peopleData(PeopleRegistryState people) {
        return new PonHubData(new PonHubDataState(people, List.of(), List.of(), 0));
    }

    @SuppressWarnings("unchecked")
    private static ListView<PersonRecord> getRecordList(Region panel) {
        return (ListView<PersonRecord>) panel.lookup("#personListView");
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
