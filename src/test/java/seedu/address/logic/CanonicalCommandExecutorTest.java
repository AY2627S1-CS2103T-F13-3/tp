package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.collections.ListChangeListener;
import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.PersonAdditionInput;
import seedu.address.model.PeopleView;
import seedu.address.model.PonHubData;
import seedu.address.model.PonHubDataState;
import seedu.address.model.attendance.Attendance;
import seedu.address.model.attendance.AttendanceStatus;
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
import seedu.address.model.person.PeopleRegistryState;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Student;
import seedu.address.model.person.Tutor;
import seedu.address.storage.JsonPonHubDataStorage;

public class CanonicalCommandExecutorTest {
    @TempDir
    public Path directory;

    private PonHubDataState fixture(boolean withLesson) {
        Student student = new Student(new PersonId("S7"), new ContactDetails(new Name("Student")),
                new EducationLevel("P1"), new Phone("91234567"));
        Tutor tutor = new Tutor(new PersonId("T2"), new ContactDetails(new Name("Tutor"),
                Optional.of(new Phone("92345678")), Optional.empty(), Optional.empty()));
        PeopleRegistryState people = new PeopleRegistryState(List.of(tutor, student),
                Map.of(PersonRole.TUTOR, 2L, PersonRole.STUDENT, 7L, PersonRole.PARENT, 0L));
        Lesson lesson = new Lesson(new LessonId("L3"), tutor.getId(), new LessonTimeSlot(LessonDay.MONDAY,
                new LessonTime("0900"), new LessonTime("1000")), new Subject("Math"), new Room("R1"),
                Set.of(student.getId()));
        Attendance attendance = new Attendance(student.getId(), lesson.getId(), LocalDate.of(2026, 10, 5),
                AttendanceStatus.PRESENT);
        return new PonHubDataState(people, withLesson ? List.of(lesson) : List.of(),
                withLesson ? List.of(attendance) : List.of(), 3);
    }

    @Test
    public void execute_failedAggregateSave_preservesFilterIdentityCountersAndRelationships() throws Exception {
        PonHubDataState before = fixture(true);
        PonHubData live = new PonHubData(before);
        PeopleView view = new PeopleView(live);
        view.setRoleFilter(Optional.of(PersonRole.STUDENT));
        var visible = view.getPeople();
        int[] events = {0};
        visible.addListener((ListChangeListener<PersonRecord>) change -> events[0]++);
        CanonicalCommandExecutor executor = new CanonicalCommandExecutor(view, state -> {
            assertEquals(before, live.exportState());
            assertTrue(state.lessons().get(0).getEnrolledStudentIds().isEmpty());
            assertEquals(before.attendance(), state.attendance());
            throw new IOException("Disk unavailable");
        }, Optional::empty);
        assertThrows(CommandException.class, () -> executor.execute((staged, stagedView) -> {
            assertEquals(new PersonId("S7"), PersonIndexResolver.resolve(Index.fromOneBased(1),
                    stagedView.getPeople()).getId());
            var counters = new HashMap<>(before.people().getLastAllocatedSequences());
            counters.put(PersonRole.STUDENT, Long.MAX_VALUE);
            staged.resetData(new PonHubDataState(new PeopleRegistryState(before.people().getPeople(), counters),
                    List.of(before.lessons().get(0).withEnrolledStudentIds(Set.of())), before.attendance(),
                    Long.MAX_VALUE));
            stagedView.setRoleFilter(Optional.empty());
            return new CommandResult("Unsaved");
        }));
        assertEquals(before, live.exportState());
        assertSame(visible, view.getPeople());
        assertEquals(Optional.of(PersonRole.STUDENT), view.getRoleFilter());
        assertEquals(new PersonId("S7"), PersonIndexResolver.resolve(Index.fromOneBased(1), visible).getId());
        assertEquals(0, events[0]);
    }

    @Test
    public void execute_failedAdditionThenRetry_reusesUncommittedIdAndReloads() throws Exception {
        PonHubData live = new PonHubData(fixture(false));
        PeopleView view = new PeopleView(live);
        view.setRoleFilter(Optional.of(PersonRole.TUTOR));
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(directory.resolve("canonical.json"));
        storage.saveData(live.exportState());
        PonHubDataState before = live.exportState();
        int[] writes = {0};
        int[] events = {0};
        view.getPeople().addListener((ListChangeListener<PersonRecord>) change -> events[0]++);
        CanonicalCommandExecutor executor = new CanonicalCommandExecutor(view, state -> {
            assertEquals(before, live.exportState());
            if (writes[0]++ == 0) {
                throw new IOException("Injected failure");
            }
            storage.saveData(state);
        }, Optional::empty);
        PersonAdditionInput input = new PersonAdditionInput(PersonRole.STUDENT,
                new ContactDetails(new Name("New Student")), Optional.of(new EducationLevel("P2")),
                Optional.of(new Phone("93456789")));
        CanonicalCommandExecutor.StagedCommand add = (staged, stagedView) -> {
            var candidate = PersonAdditionCandidate.prepare(staged.exportState(), input);
            assertEquals(new PersonId("S8"), candidate.getAddedPerson().getId());
            staged.resetData(candidate.getCandidateState());
            stagedView.setRoleFilter(Optional.empty());
            return new CommandResult("Added S8");
        };
        assertThrows(CommandException.class, () -> executor.execute(add));
        assertEquals(before, storage.readData().orElseThrow());
        assertEquals(0, events[0]);
        assertEquals(Optional.of(PersonRole.TUTOR), view.getRoleFilter());
        assertEquals("Added S8", executor.execute(add).getFeedbackToUser());
        assertEquals(live.exportState(), storage.readData().orElseThrow());
        assertEquals(3, view.getPeople().size());
        assertTrue(events[0] > 0);
    }

    @Test
    public void execute_failureProtectionAndNoOps_neverWriteOrPublishRejectedChanges() throws Exception {
        PonHubData live = new PonHubData(fixture(false));
        PeopleView view = new PeopleView(live);
        PonHubDataState before = live.exportState();
        CanonicalCommandExecutor executor = new CanonicalCommandExecutor(view, state -> {
            throw new AssertionError("Must not write");
        }, () -> Optional.of("Restore the rejected file and restart"));
        assertThrows(CommandException.class, () -> executor.execute((staged, stagedView) -> {
            staged.resetData(new PonHubDataState(before.people(), List.of(), List.of(), Long.MAX_VALUE));
            throw new CommandException("Command failed after staging");
        }));
        CommandException failure = assertThrows(CommandException.class, () -> executor.execute((staged, stagedView) -> {
            staged.resetData(new PonHubDataState(before.people(), List.of(), List.of(), Long.MAX_VALUE));
            return new CommandResult("Rejected");
        }));
        assertTrue(failure.getMessage().contains("restart"));
        executor.execute((staged, stagedView) -> {
            staged.resetData(before);
            stagedView.setRoleFilter(Optional.of(PersonRole.STUDENT));
            return new CommandResult("Listed");
        });
        assertEquals(before, live.exportState());
        assertEquals(1, view.getPeople().size());
        assertEquals(Optional.of(PersonRole.STUDENT), view.getRoleFilter());
    }

    @Test
    public void execute_aggregateCommitAndNoOp_publishesCompleteSnapshotOnce() throws Exception {
        PonHubDataState before = fixture(true);
        PonHubData live = new PonHubData(before);
        PeopleView view = new PeopleView(live);
        PonHubDataState[] saved = {before};
        int[] writes = {0};
        CanonicalCommandExecutor executor = new CanonicalCommandExecutor(view, state -> {
            assertEquals(saved[0], live.exportState());
            saved[0] = state;
            writes[0]++;
        }, Optional::empty);
        var counters = new HashMap<>(before.people().getLastAllocatedSequences());
        counters.put(PersonRole.STUDENT, Long.MAX_VALUE);
        PonHubDataState candidate = new PonHubDataState(
                new PeopleRegistryState(before.people().getPeople(), counters),
                List.of(before.lessons().get(0).withEnrolledStudentIds(Set.of())),
                List.of(before.attendance().get(0).withStatus(AttendanceStatus.ABSENT)), Long.MAX_VALUE);
        CanonicalCommandExecutor.StagedCommand command = (staged, stagedView) -> {
            staged.resetData(candidate);
            return new CommandResult("Committed");
        };
        executor.execute(command);
        assertEquals(candidate, live.exportState());
        assertEquals(candidate, saved[0]);
        assertEquals(before.people().getPeople(), view.getPeople());
        executor.execute(command);
        assertEquals(1, writes[0]);
        // Allocation history alone is operational data even when records and relationships are unchanged.
        executor.execute((staged, stagedView) -> {
            staged.resetData(new PonHubDataState(before.people(), candidate.lessons(), candidate.attendance(),
                    candidate.lastAllocatedLessonSequence()));
            return new CommandResult("Restored counters");
        });
        assertEquals(2, writes[0]);
        assertEquals(before.people(), live.exportState().people());
    }

    @Test
    public void execute_unsupportedCollections_doesNotTruncateExistingFile() throws Exception {
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(directory.resolve("canonical.json"));
        PonHubData live = new PonHubData(fixture(false));
        storage.saveData(live.exportState());
        CanonicalCommandExecutor executor = new CanonicalCommandExecutor(new PeopleView(live),
                storage::saveData, Optional::empty);
        assertThrows(CommandException.class, () -> executor.execute((staged, view) -> {
            staged.resetData(fixture(true));
            return new CommandResult("Not supported yet");
        }));
        assertEquals(fixture(false), live.exportState());
        assertEquals(fixture(false), storage.readData().orElseThrow());
    }
}
