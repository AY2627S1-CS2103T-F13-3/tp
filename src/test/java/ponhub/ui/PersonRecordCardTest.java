package ponhub.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.Assert.assertThrows;
import static ponhub.testutil.FxTestUtil.runOnFxThread;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import ponhub.MainApp;
import ponhub.commons.core.index.Index;
import ponhub.logic.CanonicalCommandExecutor;
import ponhub.logic.PersonIndexResolver;
import ponhub.logic.commands.CommandResult;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.PeopleListParser;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.PonHubDataState;
import ponhub.model.person.Address;
import ponhub.model.person.ContactDetails;
import ponhub.model.person.EducationLevel;
import ponhub.model.person.Email;
import ponhub.model.person.Name;
import ponhub.model.person.Parent;
import ponhub.model.person.PeopleRegistry;
import ponhub.model.person.PeopleRegistryState;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.PersonRole;
import ponhub.model.person.Phone;
import ponhub.model.person.Student;
import ponhub.model.person.Tutor;
import ponhub.model.query.LessonQueries;
import ponhub.model.search.PersonSearchMatch;
import ponhub.testutil.FxTestUtil;
import ponhub.testutil.LessonQueryTestData;
import ponhub.testutil.PeopleSampleDataUtil;

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

            assertCardText(root, "Casey Tan", "Student · ID: S9223372036854775807", 2,
                    List.of("Level: JC2", "Own phone: Not provided", "Email: Not provided",
                            "Parent phone: 00987654", "Address: Not provided"));
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

            assertCardText(root, contacts.getName().fullName, "Student · ID: S12", 4,
                    List.of("Level: S2", "Own phone: 00012345", "Email: Alex.Tan@Example.com",
                            "Parent phone: 00987654",
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

            assertCardText(root, contacts.getName().fullName, "Tutor · ID: T41", 7,
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

            assertCardText(root, "Beatrice Tan", "Parent · ID: P11", 3,
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

                Region tutorCard = findCard(view, "Tutor · ID: T1");
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
                Region parentCard = findCard(view, "Parent · ID: P1");
                Bounds parentBounds = parentCard.localToScene(parentCard.getBoundsInLocal());
                assertTrue(parentBounds.getMinY() >= 0 && parentBounds.getMaxY() <= panel.getHeight() + 1,
                        "Scrolling must expose the complete last card: " + parentBounds
                                + "; viewport height=" + view.getHeight());
                assertCardText(parentCard, "Beatrice Tan", "Parent · ID: P1", 3,
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
            Region studentCard = findCard(list, "Student · ID: S1");
            assertEquals("Alex Tan", ((Label) studentCard.lookup("#heading")).getText());
            assertEquals("Position: 1", ((Label) studentCard.lookup("#position")).getText());

            peopleView.setRoleFilter(Optional.empty());
            panel.layout();
            list.scrollTo(1);
            list.layout();
            studentCard = findCard(list, "Student · ID: S1");
            assertEquals("Alex Tan", ((Label) studentCard.lookup("#heading")).getText());
            assertEquals("Position: 2", ((Label) studentCard.lookup("#position")).getText());
            assertEquals(6, list.getItems().size());
            assertEquals("Showing 6 person(s).", ((Label) panel.lookup("#summary")).getText());
            assertTrue(list.getItems() == peopleView.getPeople());
            assertEquals(originalState, data.exportState());
        });
    }

    @Test
    public void panel_failedCanonicalSave_preservesActualSelectionAndFilteredIndex() throws Exception {
        Student student = new Student(new PersonId("S7"), new ContactDetails(new Name("Selected Student")),
                new EducationLevel("P1"), new Phone("91234567"));
        PonHubData data = peopleData(new PeopleRegistryState(List.of(student),
                Map.of(PersonRole.STUDENT, 7L, PersonRole.TUTOR, 0L, PersonRole.PARENT, 0L)));
        PeopleView view = new PeopleView(data);
        view.setRoleFilter(Optional.of(PersonRole.STUDENT));
        runOnFxThread(() -> {
            Region panel = new PersonRecordListPanel(view).getRoot();
            layoutScene(panel, 853, 480);
            ListView<PersonRecord> list = getRecordList(panel);
            list.getSelectionModel().select(0);
            CanonicalCommandExecutor executor = new CanonicalCommandExecutor(view, state -> {
                throw new IOException("Injected save failure");
            }, Optional::empty);
            assertThrows(CommandException.class, () -> executor.execute((staged, stagedView) -> {
                assertEquals(student.getId(), PersonIndexResolver.resolve(Index.fromOneBased(1),
                        stagedView.getPeople()).getId());
                staged.resetData(new PonHubData());
                stagedView.setRoleFilter(Optional.empty());
                return new CommandResult("Rejected deletion");
            }));
            assertEquals(student, list.getSelectionModel().getSelectedItem());
            assertEquals(0, list.getSelectionModel().getSelectedIndex());
            assertEquals(Optional.of(PersonRole.STUDENT), view.getRoleFilter());
            assertEquals(List.of(student), list.getItems());
            assertNotNull(findCard(list, "Student · ID: S7"));
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
            assertNotNull(findCard(list, "Parent · ID: P1"));

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
                Region tutorCard = findCard(list, "Tutor · ID: T1");
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
                Region parentCard = findCard(list, "Parent · ID: P1");
                Bounds bounds = parentCard.localToScene(parentCard.getBoundsInLocal());
                assertTrue(bounds.getMinY() >= 0 && bounds.getMaxY() <= panel.getHeight() + 1,
                        "The production host must expose the complete last card: " + bounds);
            }
        });
    }

    @Test
    public void panel_selectedAndUnselectedRoles_preservesHierarchyWithoutHorizontalOverflow() throws Exception {
        runOnFxThread(() -> {
            for (int width : new int[] {320, 853}) {
                Region panel = PersonRecordCardPreview.createPeopleView();
                layoutScene(panel, width, 480);
                ListView<PersonRecord> list = getRecordList(panel);
                for (int index = 0; index < list.getItems().size(); index++) {
                    for (boolean selected : new boolean[] {false, true}) {
                        list.getSelectionModel().clearSelection();
                        if (selected) {
                            list.getSelectionModel().select(index);
                        }
                        list.scrollTo(index);
                        list.applyCss();
                        list.layout();
                        list.applyCss();
                        list.layout();
                        var person = list.getItems().get(index);
                        Region card = findCard(list, new PersonRecordCardData(person, index + 1).getIdentity());
                        ListCell<?> cell = list.lookupAll(".list-cell").stream().map(ListCell.class::cast)
                                .filter(candidate -> candidate.getGraphic() == card).findFirst().orElseThrow();
                        assertEquals(selected, cell.isSelected());
                        assertEquals(selected,
                                cell.getPseudoClassStates().contains(PseudoClass.getPseudoClass("selected")));
                        assertEquals(selected ? Color.web("#FF9900") : Color.TRANSPARENT,
                                cell.getBorder().getStrokes().get(0).getLeftStroke());
                        Label heading = (Label) card.lookup("#heading");
                        Label value = (Label) card.lookup(".person-value");
                        Label caption = (Label) card.lookup(".person-caption");
                        assertTrue(heading.getFont().getSize() > value.getFont().getSize());
                        assertTrue(heading.getFont().getStyle().contains("Bold"));
                        assertTrue(caption.getFont().getStyle().contains("Bold"));
                        assertEquals(Color.web("#B8B8B8"), caption.getTextFill());
                        assertEquals(Color.web("#F2F2F2"), value.getTextFill());
                        assertEquals("Position: " + (index + 1), ((Label) card.lookup("#position")).getText());
                        assertCompleteLabels(card);
                        assertTrue(list.lookupAll(".scroll-bar").stream().map(ScrollBar.class::cast)
                                .noneMatch(bar -> bar.getOrientation() == Orientation.HORIZONTAL && bar.isVisible()));
                    }
                }
            }
        });
    }

    @Test
    public void constructor_relationshipContext_wrapsCompleteMatchesBelowOwnDetails() throws Exception {
        var data = LessonQueryTestData.createData();
        var student = (Student) data.getPerson(new PersonId("S1")).orElseThrow();
        var lesson = new LessonQueries(data).getLessonList().get(1);
        var matches = List.of(new PersonSearchMatch(Optional.of(student), Optional.of(lesson)),
                new PersonSearchMatch(Optional.empty(), Optional.of(lesson)));
        runOnFxThread(() -> {
            Region card = new PersonRecordCard(student, 3, matches).getRoot();
            layoutScene(card, 260, 900);
            VBox context = (VBox) card.lookup("#context");
            VBox details = (VBox) card.lookup("#details");
            assertTrue(context.isVisible() && context.isManaged());
            assertTrue(context.getLayoutY() >= details.getLayoutY() + details.getHeight());
            assertEquals(matches.stream().map(PersonSearchContextFormatter::format).toList(),
                    context.getChildren().stream().skip(1).map(Label.class::cast).map(Label::getText).toList());
            assertCompleteLabels(card);

            Region plainCard = new PersonRecordCard(student, 3).getRoot();
            VBox plainContext = (VBox) plainCard.lookup("#context");
            assertTrue(!plainContext.isVisible() && !plainContext.isManaged());
        });
    }

    private static void assertCompleteLabels(Region card) {
        for (Node node : card.lookupAll(".label")) {
            Label label = (Label) node;
            Bounds bounds = label.localToScene(label.getBoundsInLocal());
            Bounds cardBounds = card.localToScene(card.getBoundsInLocal());
            assertTrue(label.isWrapText() && label.getWidth() > 0, label.getText());
            assertTrue(bounds.getMinX() >= cardBounds.getMinX()
                    && bounds.getMaxX() <= cardBounds.getMaxX() + 1, label.getText());
            assertTrue(label.getHeight() + 1 >= label.prefHeight(label.getWidth()), label.getText());
            assertEquals(label.getText(), ((Text) label.lookup(".text")).getText(),
                    "Complete text must wrap without an ellipsis.");
        }
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
    private static void assertCardText(Region root, String heading, String identity, int position,
            List<String> details) {
        Label headingLabel = (Label) root.lookup("#heading");
        Label identityLabel = (Label) root.lookup("#identity");
        VBox detailBox = (VBox) root.lookup("#details");
        assertNotNull(headingLabel);
        assertNotNull(identityLabel);
        assertNotNull(detailBox);
        assertEquals(heading, headingLabel.getText());
        assertEquals(identity, identityLabel.getText());
        assertEquals("Position: " + position, ((Label) root.lookup("#position")).getText());
        assertEquals(details, detailBox.getChildren().stream().map(VBox.class::cast)
                .flatMap(group -> group.getChildren().stream()).map(VBox.class::cast)
                .map(field -> ((Label) field.getChildren().get(0)).getText() + ": "
                        + ((Label) field.getChildren().get(1)).getText()).toList());
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
