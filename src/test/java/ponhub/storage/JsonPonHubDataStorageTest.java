package ponhub.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ponhub.commons.exceptions.DataLoadingException;
import ponhub.model.PonHubData;
import ponhub.model.PonHubDataState;
import ponhub.model.attendance.Attendance;
import ponhub.model.attendance.AttendanceStatus;
import ponhub.model.lesson.Lesson;
import ponhub.model.lesson.LessonDay;
import ponhub.model.lesson.LessonId;
import ponhub.model.lesson.LessonTime;
import ponhub.model.lesson.LessonTimeSlot;
import ponhub.model.lesson.Room;
import ponhub.model.lesson.Subject;
import ponhub.model.person.Address;
import ponhub.model.person.ContactDetails;
import ponhub.model.person.EducationLevel;
import ponhub.model.person.Email;
import ponhub.model.person.Name;
import ponhub.model.person.PeopleRegistry;
import ponhub.model.person.PeopleRegistryState;
import ponhub.model.person.PersonId;
import ponhub.model.person.Phone;
import ponhub.model.person.exceptions.PersonIdExhaustedException;

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
    public void save_retainedAttendance_roundTrips() throws Exception {
        storage.saveData(original);
        Lesson lesson = new Lesson(new LessonId("L1"), new PersonId("T1"), new LessonTimeSlot(LessonDay.MONDAY,
                new LessonTime("0900"), new LessonTime("1000")), new Subject("Math"), new Room("R1"), Set.of());
        PonHubDataState retained = new PonHubDataState(original.people(), List.of(lesson),
                List.of(new Attendance(new PersonId("S1"), lesson.getId(), LocalDate.of(2026, 10, 5),
                        AttendanceStatus.PRESENT)), 7);
        storage.saveData(retained);
        assertEquals(retained, storage.readData().orElseThrow());
    }

    @Test
    public void decode_humanEditedOptionalContacts_acceptsValidValues() throws Exception {
        ObjectNode root = (ObjectNode) mapper.readTree(JsonPonHubDataCodec.encode(original));
        person(root, 1).put("phone", "00012345").put("email", "student@example.com").put("address", "New home");
        PonHubDataState edited = JsonPonHubDataCodec.decode(root.toString());
        assertEquals("00012345", edited.people().getPeople().get(1).getContactDetails().getPhone().orElseThrow().value);
        assertEquals(edited, JsonPonHubDataCodec.decode(JsonPonHubDataCodec.encode(edited)));
    }

    @Test
    public void decode_humanEditedUnicodeNames_normalizesWithoutChangingIdentityOrRelationships() throws Exception {
        ObjectNode root = (ObjectNode) mapper.readTree(JsonPonHubDataCodec.encode(original));
        person(root, 0).put("name", "\u00a0Jose\u0301   Santos\u2003");
        person(root, 1).put("name", "李明");
        person(root, 2).put("name", "தமிழ்");
        Files.createDirectories(storage.getDataFilePath().getParent());
        Files.writeString(storage.getDataFilePath(), root.toString());
        byte[] before = Files.readAllBytes(storage.getDataFilePath());

        PonHubDataState edited = storage.readData().orElseThrow();
        assertEquals(List.of("José Santos", "李明", "தமிழ்"), edited.people().getPeople().stream()
                .map(person -> person.getContactDetails().getName().fullName).toList());
        assertEquals(original.people().getLastAllocatedSequences(), edited.people().getLastAllocatedSequences());
        assertEquals(original.people().getPeople().stream().map(person -> person.getId()).toList(),
                edited.people().getPeople().stream().map(person -> person.getId()).toList());
        assertEquals(original.lessons(), edited.lessons());
        assertEquals(original.attendance(), edited.attendance());
        assertArrayEquals(before, Files.readAllBytes(storage.getDataFilePath()));
        storage.saveData(edited);
        assertEquals(edited, storage.readData().orElseThrow());
        assertTrue(Files.readString(storage.getDataFilePath()).contains("José Santos"));
    }

    @Test
    public void decode_invalidUnicodeNamesAndCanonicalDuplicates_protectsOriginalFile() throws Exception {
        for (String name : List.of("\u0301José", "José\u0000", "José\t", "\u2028José", "José\u2029",
                "\u202eJosé", "José\u200b", "José\ud800", "José\udc00", "José\u00a0Santos", "José😀")) {
            assertProtectedNameRejection(root -> person(root, 0).put("name", name));
        }
        assertProtectedNameRejection(root -> {
            person(root, 0).put("name", "José Santos");
            ObjectNode duplicate = person(root, 0).deepCopy();
            duplicate.put("id", "T2").put("name", "\u00a0JOSE\u0301   SANTOS\u2003");
            ArrayNode people = (ArrayNode) root.get("people");
            people.add(duplicate);
            ObjectNode counters = (ObjectNode) root.get("personCounters");
            counters.put("TUTOR", 2);
        });
    }

    @Test
    public void decode_humanEditedSlashContacts_normalizesWithoutSchemaOrIdentityChanges() throws Exception {
        ObjectNode root = (ObjectNode) mapper.readTree(JsonPonHubDataCodec.encode(original));
        person(root, 0).put("name", "\u00a0Jose\u0301   s/o   Santos\u2003")
                .put("address", "\u00a0Blk 10,   #01/02\u2003");
        person(root, 1).put("name", "Anita d/o Kumar");
        PonHubDataState edited = JsonPonHubDataCodec.decode(root.toString());
        var contacts = edited.people().getPeople().getFirst().getContactDetails();
        assertEquals("José s/o Santos", contacts.getName().fullName);
        assertEquals("Blk 10, #01/02", contacts.getAddress().orElseThrow().value);
        assertEquals(original.people().getLastAllocatedSequences(), edited.people().getLastAllocatedSequences());
        assertEquals(original.lastAllocatedLessonSequence(), edited.lastAllocatedLessonSequence());
        assertEquals(original.people().getPeople().stream().map(person -> person.getId()).toList(),
                edited.people().getPeople().stream().map(person -> person.getId()).toList());
        storage.saveData(edited);
        assertEquals(edited, storage.readData().orElseThrow());
    }

    @Test
    public void decode_invalidSlashContactsAndNormalizedDuplicates_protectsOriginalFile() throws Exception {
        for (String name : List.of("s/o", "Ravi s/o", "Ravi/Kumar", "Ravi s/other Kumar")) {
            assertProtectedNameRejection(root -> person(root, 0).put("name", name));
        }
        for (String address : List.of("A/B", "#01//02", "1/", "/2", "Block\t", "\u2028Block",
                "Block\u2029", "Block\u200b", "Block\ud800", "Block\u00a0A", "A".repeat(201))) {
            assertProtectedNameRejection(root -> person(root, 0).put("address", address));
        }
        assertProtectedNameRejection(root -> {
            person(root, 0).put("name", "José s/o Santos");
            ObjectNode duplicate = person(root, 0).deepCopy();
            duplicate.put("id", "T2").put("name", "\u00a0JOSE\u0301   s/o   SANTOS\u2003");
            ArrayNode people = (ArrayNode) root.get("people");
            people.add(duplicate);
            ObjectNode counters = (ObjectNode) root.get("personCounters");
            counters.put("TUTOR", 2);
        });
    }

    private void assertProtectedNameRejection(Consumer<ObjectNode> edit) throws Exception {
        ObjectNode root = (ObjectNode) mapper.readTree(JsonPonHubDataCodec.encode(original));
        edit.accept(root);
        Files.createDirectories(storage.getDataFilePath().getParent());
        // Escape lone surrogates so the UTF-8 fixture contains malformed name data rather than invalid bytes.
        Files.writeString(storage.getDataFilePath(), mapper.writer().with(JsonGenerator.Feature.ESCAPE_NON_ASCII)
                .writeValueAsString(root));
        byte[] before = Files.readAllBytes(storage.getDataFilePath());
        ProtectedPonHubDataStorage protectedStorage = new ProtectedPonHubDataStorage(storage.getDataFilePath());
        assertThrows(DataLoadingException.class, protectedStorage::load);
        assertEquals(ProtectedPonHubDataStorage.LoadStatus.CORRUPT, protectedStorage.getLoadStatus());
        assertThrows(IOException.class, () -> protectedStorage.saveData(original));
        assertArrayEquals(before, Files.readAllBytes(storage.getDataFilePath()));
    }

    @Test
    public void decode_largeSpaceRuns_normalizesCanonicalContacts() throws Exception {
        String spaces = " ".repeat(100_000);
        ObjectNode root = (ObjectNode) mapper.readTree(JsonPonHubDataCodec.encode(original));
        person(root, 0).put("name", spaces + "Anne-Marie" + spaces + "O'Neil" + spaces)
                .put("address", spaces + "Blk 10," + spaces + "#01-02" + spaces);
        Files.createDirectories(storage.getDataFilePath().getParent());
        Files.writeString(storage.getDataFilePath(), root.toString());

        var contact = storage.readData().orElseThrow().people().getPeople().get(0).getContactDetails();
        assertEquals(new Name("Anne-Marie O'Neil"), contact.getName());
        assertEquals(new Address("Blk 10, #01-02"), contact.getAddress().orElseThrow());
    }

    @Test
    public void decode_largeInvalidContacts_preservesFileAndLiveData() throws Exception {
        String spaces = " ".repeat(100_000);
        assertRejected(root -> person(root, 0).put("name", "Anne-Marie" + spaces + "O'Neil\t"));
        assertRejected(root -> person(root, 0).put("address", "Blk 10," + spaces + "#01-02\n"));
        assertRejected(root -> person(root, 0).put("name", "A".repeat(101)));
        assertRejected(root -> person(root, 0).put("phone", "1234567890123456"));
        for (String email : new String[]{"a.".repeat(5000) + "a@example.com",
            "a@" + "a-".repeat(5000) + "ab", "a@" + "a.".repeat(5000) + "ab",
            "a@" + "a".repeat(10000), "a.".repeat(5000) + "a!@example.com",
            "a@" + "a-".repeat(5000) + "a", "a@" + "a.".repeat(5000) + "a"}) {
            assertRejected(root -> person(root, 0).put("email", email));
        }
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
