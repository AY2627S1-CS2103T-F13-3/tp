package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.LessonQueryTestData.createData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.PonHubDataState;
import seedu.address.model.attendance.Attendance;
import seedu.address.model.attendance.AttendanceStatus;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.person.PersonId;
import seedu.address.model.query.LessonDetailView;
import seedu.address.storage.JsonPonHubDataStorage;
import seedu.address.storage.ProtectedPonHubDataStorage;

public class ShowLessonIntegrationTest {
    @TempDir
    Path directory;

    @Test
    public void show_preservesPeopleIndicesAndOperationalBytesAcrossMarksUnenrolmentAndReload() throws Exception {
        Path path = directory.resolve("data.json");
        new JsonPonHubDataStorage(path).saveData(createData().exportState());
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("list r/student");
        var people = session.getPeople();
        assertEquals(List.of(new PersonId("S1"), new PersonId("S2"), new PersonId("S3")),
                people.stream().map(person -> person.getId()).toList());
        byte[] before = Files.readAllBytes(path);
        assertEquals("Showing lesson L10 for 2026-10-05.",
                session.execute("showlesson d/2026-10-05 lid/L10").getFeedbackToUser());
        var displayed = session.getDisplayedLessons();
        LessonDetailView initial = (LessonDetailView) displayed.getFirst();
        assertEquals(List.of("S1", "S2"), initial.currentRoster().stream()
                .map(entry -> entry.student().getId().toString()).toList());
        assertTrue(initial.currentRoster().stream().allMatch(entry -> entry.status().isEmpty()));
        assertArrayEquals(before, Files.readAllBytes(path));
        assertSame(people, session.getPeople());
        assertEquals("Showing 2 lesson(s).", session.execute("lessons si/1").getFeedbackToUser());

        session.execute("showlesson lid/L10 d/2026-10-05");
        session.execute("mark 1 lid/L10 d/2026-10-05 s/present");
        session.execute("mark 2 lid/L10 d/2026-10-05 s/absent");
        LessonDetailView marked = (LessonDetailView) displayed.getFirst();
        assertEquals(List.of(AttendanceStatus.PRESENT, AttendanceStatus.ABSENT), marked.currentRoster().stream()
                .map(entry -> entry.status().orElseThrow()).toList());
        session.execute("unenrol 2 lid/L10");
        LessonDetailView afterUnenrol = (LessonDetailView) displayed.getFirst();
        assertEquals(List.of(new PersonId("S1")), afterUnenrol.currentRoster().stream()
                .map(entry -> entry.student().getId()).toList());
        assertEquals(new PersonId("S2"), afterUnenrol.formerAttendance().getFirst().student().getId());
        assertEquals(AttendanceStatus.ABSENT, afterUnenrol.formerAttendance().getFirst().status());
        assertSame(people, session.getPeople());
        assertEquals(new PersonId("S2"), people.get(1).getId());

        session.execute("unmark 2 d/2026-10-05 lid/L10");
        LessonDetailView afterUnmark = (LessonDetailView) displayed.getFirst();
        assertTrue(afterUnmark.formerAttendance().isEmpty());
        assertEquals(AttendanceStatus.PRESENT, afterUnmark.currentRoster().getFirst().status().orElseThrow());
        assertSame(people, session.getPeople());
        assertEquals(new PersonId("S2"), people.get(1).getId());
        byte[] afterRemoval = Files.readAllBytes(path);
        session.execute("unmark 2 lid/L10 d/2026-10-05");
        assertSame(afterUnmark, displayed.getFirst());
        assertArrayEquals(afterRemoval, Files.readAllBytes(path));

        CanonicalPeopleSession reload = new CanonicalPeopleSession(path);
        reload.execute("showlesson lid/L10 d/2026-10-05");
        LessonDetailView restored = (LessonDetailView) reload.getDisplayedLessons().getFirst();
        assertEquals(AttendanceStatus.PRESENT, restored.currentRoster().getFirst().status().orElseThrow());
        assertTrue(restored.formerAttendance().isEmpty());
    }

    @Test
    public void invalidShowAndFailedSave_leavePreviousViewAndBytesIntact() throws Exception {
        Path path = directory.resolve("data.json");
        new JsonPonHubDataStorage(path).saveData(createData().exportState());
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            throw new IOException("Injected save failure");
        });
        session.execute("list r/student");
        var people = session.getPeople();
        byte[] before = Files.readAllBytes(path);
        session.execute("showlesson lid/L10 d/2026-10-05");
        var displayed = session.getDisplayedLessons();
        LessonDetailView initial = (LessonDetailView) displayed.getFirst();
        assertThrows(ParseException.class, () -> session.execute("showlesson lid/L10 d/2026-02-30"));
        assertThrows(CommandException.class, () -> session.execute("showlesson lid/L10 d/2026-10-06"));
        assertThrows(CommandException.class, () -> session.execute("showlesson lid/L99"));
        assertThrows(CommandException.class, () -> session.execute("mark 1 lid/L10 d/2026-10-05 s/present"));
        assertSame(initial, displayed.getFirst());
        assertSame(people, session.getPeople());
        assertArrayEquals(before, Files.readAllBytes(path));
        assertTrue(session.execute("help showlesson").getFeedbackToUser().contains("unrecorded"));
    }

    @Test
    public void protectedStartup_rejectsShowBeforeParsing() throws Exception {
        Path path = directory.resolve("rejected.json");
        Files.writeString(path, "not valid JSON");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        assertThrows(CommandException.class, () -> session.execute("showlesson lid/"));
        assertTrue(session.getCommandWords().stream().noneMatch("showlesson"::equals));
        assertArrayEquals("not valid JSON".getBytes(), Files.readAllBytes(path));
    }

    @Test
    public void unmarkFailedSave_preservesSelectedDetailPeopleAndBytes() throws Exception {
        Path path = directory.resolve("data.json");
        PonHubDataState source = createData().exportState();
        Attendance retained = Attendance.fromStrings(new PersonId("S2"), new LessonId("L10"),
                "2026-10-05", "absent");
        var lessons = source.lessons().stream().map(lesson -> lesson.getId().equals(new LessonId("L10"))
                ? lesson.withoutEnrolledStudent(new PersonId("S2")) : lesson).toList();
        new JsonPonHubDataStorage(path).saveData(new PonHubDataState(source.people(), lessons,
                List.of(retained), source.lastAllocatedLessonSequence()));
        byte[] before = Files.readAllBytes(path);
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            throw new IOException("Injected unmark save failure");
        });
        session.execute("list r/student");
        var people = session.getPeople();
        session.execute("showlesson d/2026-10-05 lid/L10");
        var displayed = session.getDisplayedLessons();
        LessonDetailView initial = (LessonDetailView) displayed.getFirst();
        assertEquals(AttendanceStatus.ABSENT, initial.formerAttendance().getFirst().status());

        assertThrows(CommandException.class, () -> session.execute("unmark 2 lid/L10 d/2026-10-05"));
        assertSame(initial, displayed.getFirst());
        assertSame(people, session.getPeople());
        assertEquals(new PersonId("S2"), people.get(1).getId());
        assertArrayEquals(before, Files.readAllBytes(path));
    }

    @Test
    public void deleteSelectedEmptyLesson_clearsDetailAndKeepsPeopleView() throws Exception {
        Path path = directory.resolve("data.json");
        new JsonPonHubDataStorage(path).saveData(createData().exportState());
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("list r/student");
        var people = session.getPeople();
        session.execute("showlesson lid/L3");
        var displayed = session.getDisplayedLessons();
        assertEquals(new LessonId("L3"), displayed.getFirst().getLessonId());

        session.execute("deletelesson lid/L3");
        assertTrue(displayed.isEmpty());
        assertTrue(session.lessonResultContextProperty().get().contains("L3 is no longer available"));
        assertSame(people, session.getPeople());
    }
}
