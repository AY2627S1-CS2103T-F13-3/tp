package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.PonHubData;
import seedu.address.model.PonHubDataState;
import seedu.address.model.lesson.Lesson;
import seedu.address.model.lesson.LessonDay;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.lesson.LessonTime;
import seedu.address.model.lesson.LessonTimeSlot;
import seedu.address.model.lesson.Room;
import seedu.address.model.lesson.Subject;
import seedu.address.model.person.Address;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.PeopleRegistry;
import seedu.address.model.person.PeopleRegistryState;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.Phone;
import seedu.address.model.person.exceptions.PersonIdExhaustedException;

public class JsonPonHubDataStorageTest {
    @TempDir
    public Path folder;
    private PonHubDataState original;
    private JsonPonHubDataStorage storage;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        PeopleRegistry people = new PeopleRegistry();
        people.addTutor(new ContactDetails(new Name("Tutor"), Optional.of(new Phone("00123456")),
                Optional.of(new Email("tutor@example.com")), Optional.of(new Address("Somewhere"))));
        people.addStudent(new ContactDetails(new Name("Student")), new EducationLevel("P1"), new Phone("00987654"));
        people.addParent(new ContactDetails(new Name("Parent"), Optional.of(new Phone("00987654")),
                Optional.empty(), Optional.empty()));
        var deleted = people.addStudent(new ContactDetails(new Name("Deleted")), new EducationLevel("S2"),
                new Phone("00123456"));
        people.remove(deleted.getId(), person -> false);
        original = new PonHubDataState(people.exportState(), List.of(), List.of(), 7);
        storage = new JsonPonHubDataStorage(folder.resolve("data/ponhub.json"));
    }

    @Test
    public void roundTrip_preservesAllRolesOrderOptionalValuesAndAllocationHistory() throws Exception {
        storage.saveData(original);
        assertEquals(original, storage.readData().orElseThrow());
        String json = Files.readString(storage.getDataFilePath());
        ObjectNode root = (ObjectNode) mapper.readTree(json);
        assertEquals("00123456", root.get("people").get(0).get("phone").textValue());
        assertEquals("00987654", root.get("people").get(1).get("parentPhone").textValue());
        assertFalse(root.get("people").get(1).has("phone"));
        assertFalse(root.get("people").get(1).has("email"));
        assertFalse(root.get("people").get(1).has("address"));
        PeopleRegistry restored = new PeopleRegistry(storage.readData().orElseThrow().people());
        assertEquals(new PersonId("S3"), restored.addStudent(new ContactDetails(new Name("Next")),
                new EducationLevel("P1"), new Phone("00123456")).getId());
        assertEquals(7, storage.readData().orElseThrow().lastAllocatedLessonSequence());
    }

    @Test
    public void roundTrip_exhaustedCountersAndEmptyPeople_preservesHistory() throws Exception {
        var counters = new HashMap<>(original.people().getLastAllocatedSequences());
        counters.replaceAll((role, value) -> Long.MAX_VALUE);
        PonHubDataState state = new PonHubDataState(new PeopleRegistryState(List.of(), counters),
                List.of(), List.of(), Long.MAX_VALUE);
        storage.saveData(state);
        PonHubDataState loaded = storage.readData().orElseThrow();
        assertEquals(state, loaded);
        assertThrows(PersonIdExhaustedException.class, () -> new PeopleRegistry(loaded.people())
                .addStudent(new ContactDetails(new Name("Next")), new EducationLevel("P1"), new Phone("00123456")));
    }

    @Test
    public void read_missingAndUnreadablePaths_areDistinct() throws Exception {
        assertTrue(storage.readData().isEmpty());
        assertFalse(Files.exists(storage.getDataFilePath()));
        assertThrows(DataLoadingException.class, () -> new JsonPonHubDataStorage(folder).readData());
    }

    @Test
    public void decode_invalidShapesAndUnknownFields_rejectsWithoutChangingFileOrLiveModel() throws Exception {
        assertRejected(root -> root.remove("personCounters"));
        assertRejected(root -> root.with("personCounters").remove("STUDENT"));
        assertRejected(root -> root.with("personCounters").put("OTHER", 0));
        assertRejected(root -> root.put("people", "wrong shape"));
        assertRejected(root -> root.withArray("people").addNull());
        assertRejected(root -> root.put("foreign", true));
        assertRejected(root -> root.remove("lessons"));
        assertRejected(root -> root.putNull("attendance"));
        assertRejected(root -> root.withArray("lessons").addObject());
        assertRejected(root -> root.withArray("attendance").addObject());
        assertRejected(root -> person(root, 0).put("phone", 123456));
        assertRejected(root -> person(root, 1).putNull("email"));
        assertRejected(root -> person(root, 0).put("level", "P1"));
    }

    @Test
    public void decode_invalidPeopleAndCounters_reusesDomainValidation() throws Exception {
        assertRejected(root -> person(root, 1).remove("parentPhone"));
        assertRejected(root -> person(root, 0).remove("phone"));
        assertRejected(root -> person(root, 0).put("id", "S1"));
        assertRejected(root -> person(root, 1).put("level", "P9"));
        assertRejected(root -> person(root, 1).put("name", ""));
        assertRejected(root -> person(root, 1).put("role", "UNKNOWN"));
        assertRejected(root -> root.withArray("people").add(person(root, 1).deepCopy()));
        assertRejected(root -> root.withArray("people").add(person(root, 1).deepCopy().put("id", "S2")));
        assertRejected(root -> root.with("personCounters").put("STUDENT", 0));
        assertRejected(root -> root.with("personCounters").put("PARENT", -1));
        assertRejected(root -> root.with("personCounters").put("TUTOR", "1"));
        assertRejected(root -> root.put("lastAllocatedLessonSequence", 1.5));
        assertRejected(root -> root.remove("lastAllocatedLessonSequence"));
        assertRejected(root -> root.set("lastAllocatedLessonSequence",
                mapper.getNodeFactory().numberNode(new java.math.BigInteger("9223372036854775808"))));
    }

    @Test
    public void decode_wrongVersionMalformedOrLegacy_rejects() throws Exception {
        for (String json : new String[]{"{broken", "null", "{}", "{\"persons\":[]}",
            "{\"schemaVersion\":99,\"persons\":[]}", "{\"schemaVersion\":\"1\"}",
            "{\"schemaVersion\":1,\"schemaVersion\":1}", JsonPonHubDataCodec.encode(original) + " {}"}) {
            assertThrows(IOException.class, () -> JsonPonHubDataCodec.decode(json), json);
        }
    }

    @Test
    public void save_unsupportedCollections_leavesExistingFileUnchanged() throws Exception {
        storage.saveData(original);
        byte[] before = Files.readAllBytes(storage.getDataFilePath());
        Lesson lesson = new Lesson(new LessonId("L1"), new PersonId("T1"), new LessonTimeSlot(LessonDay.MONDAY,
                new LessonTime("0900"), new LessonTime("1000")), new Subject("Math"), new Room("R1"), Set.of());
        PonHubDataState unsupported = new PonHubDataState(original.people(), List.of(lesson), List.of(), 7);
        assertThrows(IOException.class, () -> storage.saveData(unsupported));
        assertArrayEquals(before, Files.readAllBytes(storage.getDataFilePath()));
    }

    @Test
    public void decode_humanEditedOptionalContacts_acceptsValidValues() throws Exception {
        ObjectNode root = (ObjectNode) mapper.readTree(JsonPonHubDataCodec.encode(original));
        person(root, 1).put("phone", "00012345").put("email", "student@example.com").put("address", "New home");
        PonHubDataState edited = JsonPonHubDataCodec.decode(root.toString());
        assertEquals("00012345", edited.people().getPeople().get(1).getContactDetails().getPhone().orElseThrow().value);
        assertEquals(edited, JsonPonHubDataCodec.decode(JsonPonHubDataCodec.encode(edited)));
    }

    private void assertRejected(Consumer<ObjectNode> edit) throws Exception {
        ObjectNode root = (ObjectNode) mapper.readTree(JsonPonHubDataCodec.encode(original));
        edit.accept(root);
        Files.createDirectories(storage.getDataFilePath().getParent());
        Files.writeString(storage.getDataFilePath(), root.toString());
        byte[] before = Files.readAllBytes(storage.getDataFilePath());
        PonHubData live = new PonHubData(original);
        DataLoadingException error = assertThrows(DataLoadingException.class, () ->
                live.resetData(storage.readData().orElseThrow()));
        assertTrue(error.getMessage().length() > 0);
        assertEquals(original, live.exportState());
        assertArrayEquals(before, Files.readAllBytes(storage.getDataFilePath()));
    }

    private static ObjectNode person(ObjectNode root, int index) {
        return (ObjectNode) root.get("people").get(index);
    }
}
