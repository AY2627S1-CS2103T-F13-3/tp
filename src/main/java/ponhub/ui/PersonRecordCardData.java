package ponhub.ui;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.AppUtil.checkArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ponhub.model.person.ContactDetails;
import ponhub.model.person.Parent;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.Student;
import ponhub.model.person.Tutor;

/**
 * An immutable display projection of one role-specific person record and its current-view position.
 * It preserves contact display values and owns no operational data or relationship state.
 */
public final class PersonRecordCardData {

    private final String heading;
    private final String position;
    private final String identity;
    private final List<List<Detail>> detailGroups;

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
        heading = person.getName().fullName;
        position = "Position: " + displayedIndex;
        String role = switch (person.getRole()) {
            case STUDENT -> "Student";
            case TUTOR -> "Tutor";
            case PARENT -> "Parent";
        };
        identity = role + " · ID: " + person.getId();
        detailGroups = createDetailGroups(person);
    }

    public String getHeading() {
        return heading;
    }

    public String getIdentity() {
        return identity;
    }

    public String getPosition() {
        return position;
    }

    /**
     * Returns immutable groups in reading order: education, own contacts, parent contact, then address.
     * Tutors and parents omit the student-specific groups. Absent contacts remain explicit.
     */
    public List<List<Detail>> getDetailGroups() {
        return detailGroups;
    }

    /**
     * Formats required role fields and optional contacts without changing their supplied display values.
     */
    private static List<List<Detail>> createDetailGroups(PersonRecord person) {
        List<List<Detail>> groups = new ArrayList<>();
        if (person instanceof Student student) {
            groups.add(List.of(new Detail("Level", student.getLevel().toString(), false)));
        }
        ContactDetails contacts = person.getContactDetails();
        String phoneLabel = person instanceof Student ? "Own phone" : "Phone";
        groups.add(List.of(
                createContact(phoneLabel, contacts.getPhone().map(phone -> phone.value)),
                createContact("Email", contacts.getEmail().map(email -> email.value))));
        if (person instanceof Student student) {
            groups.add(List.of(createContact("Parent phone", Optional.of(student.getParentPhone().value))));
        }
        groups.add(List.of(createContact("Address", contacts.getAddress().map(address -> address.value))));
        return List.copyOf(groups);
    }

    private static Detail createContact(String caption, Optional<String> value) {
        return new Detail(caption, value.orElse("Not provided"), value.isPresent());
    }

    /**
     * A caption and unchanged display value, with copying enabled only for present contact values.
     */
    public record Detail(String caption, String value, boolean isCopyable) { }
}
