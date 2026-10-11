package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Name;
import seedu.address.model.person.PeopleRegistry;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.Phone;

public class JsonLessonStorageTest {
    @TempDir
    Path directory;
    private final ObjectMapper mapper = new ObjectMapper();
    private PonHubDataState original;

    @BeforeEach
    public void setUp() {
        PeopleRegistry people = new PeopleRegistry();
        for (String name : List.of("Tutor One", "Tutor Two")) {
            people.addTutor(new ContactDetails(new Name(name), Optional.of(new Phone("91234567")),
                    Optional.empty(), Optional.empty()));
        }
        for (String name : List.of("Student One", "Student Two")) {
            people.addStudent(new ContactDetails(new Name(name)), new EducationLevel("P1"), new Phone("92345678"));
        }
        Lesson first = new Lesson(new LessonId("L2"), new PersonId("T1"), new LessonTimeSlot(LessonDay.MONDAY,
                new LessonTime("0900"), new LessonTime("1000")), new Subject("Math"), new Room("R1"),
                Set.of(new PersonId("S1"), new PersonId("S2")));
        Lesson second = new Lesson(new LessonId("L5"), new PersonId("T2"), new LessonTimeSlot(LessonDay.MONDAY,
                new LessonTime("1000"), new LessonTime("1100")), new Subject("English"), new Room("R2"), Set.of());
        original = new PonHubDataState(people.exportState(), List.of(first, second), List.of(), 9);
    }

    @Test
    public void saveLoad_sharedAndEmptyLessons_preservesCompleteStateAndDeletedIdHistory() throws Exception {
        Path path = directory.resolve("folder with spaces/data.json");
        ProtectedPonHubDataStorage storage = new ProtectedPonHubDataStorage(path);
        storage.load();
        storage.saveData(original);
        assertEquals(original, new ProtectedPonHubDataStorage(path).load());
        PonHubDataState deleted = new PonHubDataState(original.people(), List.of(), List.of(), Long.MAX_VALUE);
        storage.saveData(deleted);
        assertEquals(deleted, new ProtectedPonHubDataStorage(path).load());
    }

    @Test
    public void decode_invalidLessonFields_rejectsWithoutChangingFileOrLiveState() throws Exception {
        for (String field : List.of("id", "tutorId", "day", "startTime", "endTime", "subject", "room",
                "enrolledStudentIds")) {
            reject(root -> getLessonNode(root, 0).remove(field));
            reject(root -> getLessonNode(root, 0).putNull(field));
            reject(root -> getLessonNode(root, 0).put(field, 123));
        }
        reject(root -> getLessonNode(root, 0).put("extra", "unknown"));
        reject(root -> root.put("lessons", "wrong type"));
        reject(root -> root.withArray("lessons").addNull());
        reject(root -> getLessonNode(root, 0).put("id", "L0"));
        reject(root -> getLessonNode(root, 0).put("day", "Someday"));
        reject(root -> getLessonNode(root, 0).put("startTime", "2400"));
        reject(root -> getLessonNode(root, 0).put("endTime", "0900"));
        reject(root -> getLessonNode(root, 0).put("endTime", "0800"));
        reject(root -> getLessonNode(root, 0).put("subject", ""));
        reject(root -> getLessonNode(root, 0).put("room", "bad room"));
        reject(root -> getLessonNode(root, 0).withArray("enrolledStudentIds").add("S1"));
        reject(root -> getLessonNode(root, 0).withArray("enrolledStudentIds").addNull());
        reject(root -> getLessonNode(root, 0).withArray("enrolledStudentIds").add(1));
    }

    @Test
    public void decode_invalidReferencesCountersAndClashes_rejectsWholeCandidate() throws Exception {
        reject(root -> root.withArray("lessons").add(getLessonNode(root, 0).deepCopy()));
        reject(root -> getLessonNode(root, 0).put("tutorId", "T99"));
        reject(root -> getLessonNode(root, 0).put("tutorId", "S1"));
        reject(root -> getLessonNode(root, 0).withArray("enrolledStudentIds").add("S99"));
        reject(root -> getLessonNode(root, 0).withArray("enrolledStudentIds").add("T1"));
        reject(root -> root.put("lastAllocatedLessonSequence", 4));
        reject(root -> root.put("lastAllocatedLessonSequence", -1));
        reject(root -> root.put("lastAllocatedLessonSequence", 1.5));
        reject(root -> root.put("lastAllocatedLessonSequence", "9"));
        reject(root -> {
            getLessonNode(root, 1).put("startTime", "0930").put("tutorId", "T1");
        });
        reject(root -> {
            getLessonNode(root, 1).put("startTime", "0930").put("room", "r1");
        });
        reject(root -> {
            getLessonNode(root, 1).put("startTime", "0930").withArray("enrolledStudentIds").add("S1");
        });
    }

    @Test
    public void decode_reorderedReferencesAndAdjacentLessons_acceptsValidCandidate() throws Exception {
        ObjectNode root = createJson();
        var people = root.withArray("people");
        var records = new java.util.ArrayList<com.fasterxml.jackson.databind.JsonNode>();
        people.forEach(records::add);
        people.removeAll();
        records.reversed().forEach(people::add);
        getLessonNode(root, 1).put("tutorId", "T1").put("room", "r1");
        getLessonNode(root, 1).withArray("enrolledStudentIds").add("S1");
        var lessons = root.withArray("lessons");
        var first = lessons.get(0);
        var second = lessons.get(1);
        lessons.removeAll().add(second).add(first);
        PonHubDataState loaded = JsonPonHubDataCodec.decode(root.toString());
        assertEquals(new LessonId("L5"), loaded.lessons().get(0).getId());
        assertEquals(loaded, JsonPonHubDataCodec.decode(JsonPonHubDataCodec.encode(loaded)));
    }

    private ObjectNode createJson() throws IOException {
        return (ObjectNode) mapper.readTree(JsonPonHubDataCodec.encode(original));
    }

    private ObjectNode getLessonNode(ObjectNode root, int index) {
        return (ObjectNode) root.withArray("lessons").get(index);
    }

    private void reject(Consumer<ObjectNode> edit) throws Exception {
        ObjectNode root = createJson();
        edit.accept(root);
        Path path = directory.resolve("rejected.json");
        Files.writeString(path, root.toString());
        byte[] bytes = Files.readAllBytes(path);
        PonHubData live = new PonHubData(original);
        ProtectedPonHubDataStorage storage = new ProtectedPonHubDataStorage(path);
        DataLoadingException failure = assertThrows(DataLoadingException.class, () -> live.resetData(storage.load()));
        assertTrue(failure.getMessage().contains("Cause:"));
        assertEquals(original, live.exportState());
        assertThrows(IOException.class, () -> storage.saveData(original));
        assertArrayEquals(bytes, Files.readAllBytes(path));
    }
}
