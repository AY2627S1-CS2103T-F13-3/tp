package seedu.address.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import seedu.address.model.person.PersonRecord;

/**
 * A read-only card for the immutable PonHub person records, prepared for later people-view integration.
 * Every text line wraps to the card's available width; the enclosing people view supplies vertical scrolling.
 */
public class PersonRecordCard extends UiPart<Region> {

    private static final String FXML = "PersonRecordCard.fxml";

    @FXML
    private Label heading;
    @FXML
    private Label identity;
    @FXML
    private VBox details;

    /**
     * Creates a card showing a record and its positive current-view position separately from its stable ID.
     * This component does not read or mutate a registry and is not yet wired into the active application.
     */
    public PersonRecordCard(PersonRecord person, int displayedIndex) {
        this(new PersonRecordCardData(person, displayedIndex));
    }

    private PersonRecordCard(PersonRecordCardData data) {
        super(FXML);
        heading.setText(data.getHeading());
        identity.setText(data.getIdentity());
        for (String line : data.getDetailLines()) {
            Label label = new Label(line);
            label.getStyleClass().add("cell_small_label");
            label.setWrapText(true);
            label.setMinWidth(0);
            label.setMaxWidth(Double.MAX_VALUE);
            details.getChildren().add(label);
        }
    }
}
