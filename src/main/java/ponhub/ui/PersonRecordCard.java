package ponhub.ui;

import static java.util.Objects.requireNonNull;

import java.util.List;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
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
                Label caption = createLabel(detail.caption(), "person-caption");
                HBox captionRow = new HBox(6, caption);
                captionRow.setAlignment(Pos.CENTER_LEFT);
                if (detail.isCopyable()) {
                    captionRow.getChildren().add(createCopyButton(detail, data.getHeading()));
                }
                VBox field = new VBox(2, captionRow, createLabel(detail.value(), "person-value"));
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

    /**
     * Creates an action that copies the exact contact without executing a command or changing card selection.
     * Standard button activation and the platform's copy shortcut share the same action.
     */
    private static Button createCopyButton(PersonRecordCardData.Detail detail, String name) {
        Button copy = new Button("Copy");
        copy.getStyleClass().add("person-contact-copy");
        copy.setMinWidth(Region.USE_PREF_SIZE);
        copy.setAccessibleText("Copy " + detail.caption() + " for " + name);
        copy.setAccessibleHelp("Press Space or the platform copy shortcut to copy this contact value.");
        copy.setTooltip(new Tooltip("Copy " + detail.caption()));
        copy.setOnAction(event -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(detail.value());
            Clipboard.getSystemClipboard().setContent(content);
        });
        KeyCombination shortcut = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);
        copy.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (shortcut.match(event)) {
                copy.fire();
                event.consume();
            }
        });
        // Keep mouse copying from moving focus and clearing a TextField's draft selection.
        // Keyboard traversal still focuses the standard button normally.
        copy.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                copy.arm();
            }
            event.consume();
        });
        copy.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && copy.isArmed()) {
                copy.disarm();
                if (copy.contains(event.getX(), event.getY())) {
                    copy.fire();
                }
            }
            event.consume();
        });
        return copy;
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
