package seedu.address.ui;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import seedu.address.MainApp;
import seedu.address.model.PeopleView;
import seedu.address.model.PonHubData;
import seedu.address.model.PonHubDataState;
import seedu.address.model.person.Address;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Parent;
import seedu.address.model.person.PeopleRegistryState;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Student;
import seedu.address.model.person.Tutor;

/**
 * A developer-only preview of dormant person cards, with no application model, commands, or storage.
 * Runs manually from the test source set; optional arguments specify logical scene width and height.
 */
public class PersonRecordCardPreview extends Application {

    @Override
    public void start(Stage stage) {
        List<String> arguments = getParameters().getRaw();
        double width = arguments.isEmpty() ? 853 : Double.parseDouble(arguments.get(0));
        double height = arguments.size() < 2 ? 480 : Double.parseDouble(arguments.get(1));
        Scene scene = new Scene(createPeopleView(), width, height);
        scene.getStylesheets().add(MainApp.class.getResource("/view/DarkTheme.css").toExternalForm());
        stage.setTitle("PonHub person card preview — developer fixture");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Launches the isolated card preview without loading or saving operational files.
     */
    public static void main(String[] arguments) {
        launch(arguments);
    }

    /**
     * Creates the intended list-cell host, fitting each card to its cell's available width.
     */
    static Region createPeopleView() {
        PeopleRegistryState people = new PeopleRegistryState(createRecords(),
                Map.of(PersonRole.STUDENT, Long.MAX_VALUE, PersonRole.TUTOR, 1L, PersonRole.PARENT, 1L));
        PonHubData data = new PonHubData(new PonHubDataState(people, List.of(), List.of(), 0));
        return new PersonRecordListPanel(new PeopleView(data)).getRoot();
    }

    /**
     * Creates records covering required fields, absent contacts, long text, and the largest stable sequence.
     */
    static List<PersonRecord> createRecords() {
        ContactDetails longContacts = new ContactDetails(new Name("Alexandria ".repeat(9).strip()),
                Optional.of(new Phone("000123456789012")),
                Optional.of(new Email("longcontact".repeat(10) + "@example.com")),
                Optional.of(new Address("12 " + "A long residential road ".repeat(8).strip())));
        ContactDetails parentContacts = new ContactDetails(new Name("Beatrice Tan"),
                Optional.of(new Phone("00987654")), Optional.empty(), Optional.empty());
        return List.of(
                new Student(new PersonId("S" + Long.MAX_VALUE), new ContactDetails(new Name("Casey Tan")),
                        new EducationLevel("JC2"), new Phone("00987654")),
                new Tutor(new PersonId("T1"), longContacts),
                new Parent(new PersonId("P1"), parentContacts));
    }
}
