package ponhub.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import ponhub.model.person.ContactDetails;
import ponhub.model.person.EducationLevel;
import ponhub.model.person.Name;
import ponhub.model.person.PeopleRegistry;
import ponhub.model.person.PersonId;
import ponhub.model.person.Phone;

public class JsonAttendanceStorageTest {
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
        original = new PonHubDataState(people.exportState(), List.of(first, second), List.of(
                record("S1", "L2", "2026-10-05"), record("S2", "L2", "2026-10-05"),
                record("S1", "L2", "2026-10-12"), record("S1", "L5", "2026-10-05")), 9);
    }

    @Test
    public void saveLoad_unenrolCorrectAndUnmark_preservesOtherKeysAndCompleteSnapshot() throws Exception {
        Path path = directory.resolve("folder with spaces/data.json");
        ProtectedPonHubDataStorage storage = new ProtectedPonHubDataStorage(path);
        storage.load();
        storage.saveData(original);
        assertEquals(original, new ProtectedPonHubDataStorage(path).load());
        Lesson unenrolled = original.lessons().get(0).withoutEnrolledStudent(new PersonId("S1"));
        PonHubDataState retained = new PonHubDataState(original.people(),
                List.of(unenrolled, original.lessons().get(1)), original.attendance(), 9);
        storage.saveData(retained);
        PonHubDataState loaded = new ProtectedPonHubDataStorage(path).load();
        assertEquals(retained, loaded);
        var correctedRecords = new java.util.ArrayList<>(loaded.attendance());
        correctedRecords.set(0, correctedRecords.get(0).withStatus(AttendanceStatus.ABSENT));
        PonHubDataState corrected = new PonHubDataState(loaded.people(), loaded.lessons(), correctedRecords, 9);
        storage.saveData(corrected);
        assertEquals(corrected, new ProtectedPonHubDataStorage(path).load());
        assertNotEquals(retained, corrected);
        var removedKey = correctedRecords.get(0).getKey();
        PonHubDataState unmarked = new PonHubDataState(corrected.people(), corrected.lessons(),
                corrected.attendance().stream().filter(record -> !record.getKey().equals(removedKey)).toList(), 9);
        storage.saveData(unmarked);
        assertEquals(unmarked, new ProtectedPonHubDataStorage(path).load());
        assertEquals(original.attendance().subList(1, 4), unmarked.attendance());
        assertNotEquals(corrected, unmarked);
        PonHubData live = new PonHubData(original);
        live.resetData(unmarked);
        assertEquals(unmarked, live.exportState());
        assertEquals(4, original.attendance().size());
    }

    @Test
    public void decode_invalidFields_rejectsWithoutChangingFileOrLiveState() throws Exception {
        for (String field : List.of("studentId", "lessonId", "date", "status")) {
            reject(root -> getAttendanceNode(root).remove(field));
            reject(root -> getAttendanceNode(root).putNull(field));
            reject(root -> getAttendanceNode(root).put(field, 123));
        }
        reject(root -> getAttendanceNode(root).put("extra", "unknown"));
        reject(root -> root.remove("attendance"));
        reject(root -> root.put("attendance", "wrong type"));
        reject(root -> root.withArray("attendance").addNull());
        for (String date : List.of("2026-02-30", "2026-02-29", "2026-10-06", "2026-1-5",
                "0000-10-05", "2026-13-01", "2026-10-05T00:00:00", " 2026-10-05")) {
            reject(root -> getAttendanceNode(root).put("date", date));
        }
        reject(root -> getAttendanceNode(root).put("status", "late"));
        reject(root -> getAttendanceNode(root).put("status", ""));
    }

    @Test
    public void decode_duplicateKeysAndInvalidReferences_rejectsWholeCandidate() throws Exception {
        reject(root -> root.withArray("attendance").add(getAttendanceNode(root).deepCopy()));
        reject(root -> root.withArray("attendance").add(getAttendanceNode(root).deepCopy().put("status", "absent")));
        for (String id : List.of("S99", "T1", "P1", "S0")) {
            reject(root -> getAttendanceNode(root).put("studentId", id));
        }
        for (String id : List.of("L99", "S1", "L0")) {
            reject(root -> getAttendanceNode(root).put("lessonId", id));
        }
    }

    @Test
    public void decode_validLeapDateAndNormalizedStatus_roundTrips() throws Exception {
        ObjectNode root = createJson();
        getAttendanceNode(root).put("date", "2044-02-29").put("status", " AbSeNt ");
        PonHubDataState loaded = JsonPonHubDataCodec.decode(root.toString());
        assertEquals(LocalDate.of(2044, 2, 29), loaded.attendance().get(0).getDate());
        assertEquals(AttendanceStatus.ABSENT, loaded.attendance().get(0).getStatus());
        assertEquals(loaded, JsonPonHubDataCodec.decode(JsonPonHubDataCodec.encode(loaded)));
    }

    @Test
    public void snapshot_invalidDate_rejectsBeforeSave() {
        for (LocalDate date : List.of(LocalDate.of(2026, 10, 6), LocalDate.of(0, 1, 1),
                LocalDate.of(10000, 1, 1))) {
            Attendance record = new Attendance(new PersonId("S1"), new LessonId("L2"), date,
                    AttendanceStatus.PRESENT);
            assertThrows(IllegalArgumentException.class, () -> new PonHubDataState(original.people(),
                    original.lessons(), List.of(record), 9));
        }
    }

    private static Attendance record(String student, String lesson, String date) {
        return Attendance.fromStrings(new PersonId(student), new LessonId(lesson), date, "present");
    }

    private ObjectNode createJson() throws IOException {
        return (ObjectNode) mapper.readTree(JsonPonHubDataCodec.encode(original));
    }

    private ObjectNode getAttendanceNode(ObjectNode root) {
        return (ObjectNode) root.withArray("attendance").get(0);
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
