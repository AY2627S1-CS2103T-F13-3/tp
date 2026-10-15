package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.stream.Stream;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.search.PersonSearchMatch;

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
    private VBox details;

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
        for (String line : Stream.concat(data.getDetailLines().stream(),
                matches.stream().map(PersonSearchContextFormatter::format)).toList()) {
            Label label = new Label(line);
            label.getStyleClass().add("cell_small_label");
            label.setWrapText(true);
            label.setMinWidth(0);
            label.setMaxWidth(Double.MAX_VALUE);
            details.getChildren().add(label);
        }
    }
}
