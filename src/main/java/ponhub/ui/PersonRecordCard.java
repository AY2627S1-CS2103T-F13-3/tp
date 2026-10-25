package ponhub.ui;

import static java.util.Objects.requireNonNull;

import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ponhub.model.person.PersonRecord;
import ponhub.model.search.PersonSearchMatch;

/**
 * A read-only card for the immutable PonHub person records in the current people view.
 * Every text line wraps to the card's available width; the enclosing people view supplies vertical scrolling.
 */
public class PersonRecordCard extends UiPart<Region> {

    private static final String FXML = "PersonRecordCard.fxml";

    @FXML
    private Label heading;
    @FXML
    private Label identity;
    @FXML
    private Label position;
    @FXML
    private VBox details;
    @FXML
    private VBox context;

    /**
     * Creates a card showing a record and its positive current-view position separately from its stable ID.
     * This component renders the supplied record without reading or mutating a registry.
     */
    public PersonRecordCard(PersonRecord person, int displayedIndex) {
        this(person, displayedIndex, List.of());
    }

    /**
     * Adds only matching relationship context below the person's own details.
     */
    public PersonRecordCard(PersonRecord person, int displayedIndex, List<PersonSearchMatch> matches) {
        this(new PersonRecordCardData(person, displayedIndex), requireNonNull(matches));
    }

    private PersonRecordCard(PersonRecordCardData data, List<PersonSearchMatch> matches) {
        super(FXML);
        heading.setText(data.getHeading());
        identity.setText(data.getIdentity());
        position.setText(data.getPosition());
        for (List<PersonRecordCardData.Detail> group : data.getDetailGroups()) {
            VBox groupBox = new VBox();
            groupBox.getStyleClass().add("person-detail-group");
            for (PersonRecordCardData.Detail detail : group) {
                VBox field = new VBox(2, createLabel(detail.caption(), "person-caption"),
                        createLabel(detail.value(), "person-value"));
                field.setMinWidth(0);
                groupBox.getChildren().add(field);
            }
            details.getChildren().add(groupBox);
        }
        context.setVisible(!matches.isEmpty());
        context.setManaged(!matches.isEmpty());
        if (!matches.isEmpty()) {
            context.getChildren().add(createLabel("Matching relationships", "person-caption"));
            for (PersonSearchMatch match : matches) {
                context.getChildren().add(createLabel(PersonSearchContextFormatter.format(match), "person-value"));
            }
        }
    }

    private static Label createLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }
}
