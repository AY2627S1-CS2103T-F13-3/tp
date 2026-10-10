package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
import seedu.address.model.person.exceptions.DuplicatePersonException;

/**
 * Version-1 people codec. Non-empty lessons/attendance are rejected until their codecs are implemented.
 */
public final class JsonPonHubDataCodec {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Set<String> ROOT_FIELDS = Set.of("schemaVersion", "people", "personCounters",
            "lastAllocatedLessonSequence", "lessons", "attendance");
    private static final Set<String> PERSON_FIELDS = Set.of("id", "role", "name", "phone", "email", "address");
    private static final Set<String> STUDENT_FIELDS = Set.of("id", "role", "name", "phone", "email", "address",
            "level", "parentPhone");

    private JsonPonHubDataCodec() {
    }

    /**
     * Serializes a complete people-only snapshot; optional fields are omitted rather than written as null.
     */
    public static String encode(PonHubDataState state) throws IOException {
        requireNonNull(state);
        if (!state.lessons().isEmpty() || !state.attendance().isEmpty()) {
            throw new IOException("Non-empty lessons/attendance cannot be saved by this people-only codec.");
        }
        ObjectNode root = mapper.createObjectNode();
        root.put("schemaVersion", JsonDataVersionDetector.CURRENT_VERSION);
        var people = root.putArray("people");
        for (PersonRecord person : state.people().getPeople()) {
            ObjectNode node = people.addObject();
            node.put("id", person.getId().toString());
            node.put("role", person.getRole().name());
            ContactDetails contact = person.getContactDetails();
            node.put("name", contact.getName().fullName);
            contact.getPhone().ifPresent(value -> node.put("phone", value.value));
            contact.getEmail().ifPresent(value -> node.put("email", value.value));
            contact.getAddress().ifPresent(value -> node.put("address", value.value));
            if (person instanceof Student student) {
                node.put("level", student.getLevel().getValue());
                node.put("parentPhone", student.getParentPhone().value);
            }
        }
        ObjectNode counters = root.putObject("personCounters");
        for (PersonRole role : PersonRole.values()) {
            counters.put(role.name(), state.people().getLastAllocatedSequences().get(role));
        }
        root.put("lastAllocatedLessonSequence", state.lastAllocatedLessonSequence());
        root.putArray("lessons");
        root.putArray("attendance");
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    /**
     * Builds and validates a complete candidate without modifying live state or writing any file.
     */
    public static PonHubDataState decode(String json) throws IOException {
        requireNonNull(json);
        JsonDataVersionDetector.Format format = JsonDataVersionDetector.detect(json);
        if (format != JsonDataVersionDetector.Format.PONHUB_V1_CANDIDATE) {
            throw new IOException("Expected schemaVersion 1; detected " + format + ".");
        }
        JsonNode root = mapper.readTree(json);
        checkObject(root, ROOT_FIELDS, "root");
        requireEmptyArray(root, "lessons");
        requireEmptyArray(root, "attendance");
        JsonNode counters = root.get("personCounters");
        checkObject(counters, Set.of("STUDENT", "TUTOR", "PARENT"), "personCounters");
        var allocation = new EnumMap<PersonRole, Long>(PersonRole.class);
        for (PersonRole role : PersonRole.values()) {
            allocation.put(role, counter(counters, role.name(), "personCounters." + role.name()));
        }
        long lessonCounter = counter(root, "lastAllocatedLessonSequence", "lastAllocatedLessonSequence");
        JsonNode array = root.get("people");
        if (array == null || !array.isArray()) {
            throw new IOException("people must be an array.");
        }
        List<PersonRecord> people = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            try {
                people.add(decodePerson(array.get(i)));
            } catch (IOException | IllegalArgumentException failure) {
                throw new IOException("people[" + i + "]: " + failure.getMessage(), failure);
            }
        }
        try {
            return new PonHubDataState(new PeopleRegistryState(people, allocation),
                    List.of(), List.of(), lessonCounter);
        } catch (IllegalArgumentException | DuplicatePersonException failure) {
            throw new IOException("Invalid people/identity state: " + failure.getMessage(), failure);
        }
    }

    private static PersonRecord decodePerson(JsonNode node) throws IOException {
        checkObject(node, STUDENT_FIELDS, "person");
        PersonRole role = PersonRole.valueOf(text(node, "role"));
        checkObject(node, role == PersonRole.STUDENT ? STUDENT_FIELDS : PERSON_FIELDS, "person");
        PersonId id = new PersonId(text(node, "id"));
        ContactDetails contact = new ContactDetails(new Name(text(node, "name")),
                optionalText(node, "phone").map(Phone::new), optionalText(node, "email").map(Email::new),
                optionalText(node, "address").map(Address::new));
        return switch (role) {
            case STUDENT -> new Student(id, contact, new EducationLevel(text(node, "level")),
                    new Phone(text(node, "parentPhone")));
            case TUTOR -> new Tutor(id, contact);
            case PARENT -> new Parent(id, contact);
        };
    }

    private static String text(JsonNode node, String field) throws IOException {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) {
            throw new IOException(field + " must be a JSON string.");
        }
        return value.textValue();
    }

    private static Optional<String> optionalText(JsonNode node, String field) throws IOException {
        return node.has(field) ? Optional.of(text(node, field)) : Optional.empty();
    }

    private static long counter(JsonNode node, String field, String label) throws IOException {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0) {
            throw new IOException(label + " must be an integer between 0 and " + Long.MAX_VALUE + ".");
        }
        return value.longValue();
    }

    private static void requireEmptyArray(JsonNode root, String field) throws IOException {
        JsonNode value = root.get(field);
        if (value == null || !value.isArray() || value.size() != 0) {
            throw new IOException(field + " must be an empty array for this people-only codec.");
        }
    }

    private static void checkObject(JsonNode node, Set<String> allowed, String label) throws IOException {
        if (node == null || !node.isObject()) {
            throw new IOException(label + " must be an object.");
        }
        var fields = node.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            if (!allowed.contains(field)) {
                throw new IOException("Unknown field " + label + "." + field + ".");
            }
        }
    }
}
