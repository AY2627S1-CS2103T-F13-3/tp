package seedu.address.ui;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.util.ArrayList;
import java.util.List;

import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.Parent;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.Student;
import seedu.address.model.person.Tutor;

/**
 * An immutable display projection of one role-specific person record and its current-view position.
 * It preserves contact display values and owns no operational data or relationship state.
 */
public final class PersonRecordCardData {

    private final String heading;
    private final String identity;
    private final List<String> detailLines;

    /**
     * Creates display text for a student, tutor, or parent at a positive current-view position.
     * The position is independent of the stable person ID shown on a separate line.
     *
     * @throws NullPointerException if the record is null.
     * @throws IllegalArgumentException if the position is not positive or the record type is unsupported.
     */
    public PersonRecordCardData(PersonRecord person, int displayedIndex) {
        requireNonNull(person);
        checkArgument(displayedIndex > 0, "Displayed person indices must be positive.");
        checkArgument(person instanceof Student || person instanceof Tutor || person instanceof Parent,
                "Person cards support student, tutor, and parent records.");
        heading = displayedIndex + ". " + person.getName().fullName;
        String role = switch (person.getRole()) {
            case STUDENT -> "Student";
            case TUTOR -> "Tutor";
            case PARENT -> "Parent";
        };
        identity = role + " · " + person.getId();
        detailLines = createDetailLines(person);
    }

    public String getHeading() {
        return heading;
    }

    public String getIdentity() {
        return identity;
    }

    /**
     * Returns immutable detail lines, explicitly labeling absent optional contacts as not provided.
     */
    public List<String> getDetailLines() {
        return detailLines;
    }

    /**
     * Formats required role fields and optional contacts without changing their supplied display values.
     */
    private static List<String> createDetailLines(PersonRecord person) {
        List<String> lines = new ArrayList<>();
        if (person instanceof Student student) {
            lines.add("Level: " + student.getLevel());
            lines.add("Parent phone: " + student.getParentPhone().value);
        }
        ContactDetails contacts = person.getContactDetails();
        String phoneLabel = person instanceof Student ? "Own phone: " : "Phone: ";
        lines.add(phoneLabel + contacts.getPhone().map(phone -> phone.value).orElse("Not provided"));
        lines.add("Email: " + contacts.getEmail().map(email -> email.value).orElse("Not provided"));
        lines.add("Address: " + contacts.getAddress().map(address -> address.value).orElse("Not provided"));
        return List.copyOf(lines);
    }
}
