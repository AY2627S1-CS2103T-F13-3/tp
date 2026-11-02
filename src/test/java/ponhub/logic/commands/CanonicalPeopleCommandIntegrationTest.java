package ponhub.logic.commands;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.collections.ListChangeListener;
import ponhub.commons.core.index.Index;
import ponhub.logic.CanonicalCommandExecutor;
import ponhub.logic.PersonIndexResolver;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.PeopleListParser;
import ponhub.logic.parser.RoleAwareAddParser;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.PeopleView;
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
import ponhub.model.person.Parent;
import ponhub.model.person.PeopleRegistryState;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.PersonRole;
import ponhub.model.person.Phone;
import ponhub.model.person.Student;
import ponhub.model.person.Tutor;
import ponhub.storage.JsonPonHubDataStorage;

/**
 * Exercises prepared people commands through the real transaction, parser and people-storage boundaries.
 */
public class CanonicalPeopleCommandIntegrationTest {
    @TempDir
    public Path directory;

    private final RoleAwareAddParser addParser = new RoleAwareAddParser();
    private final PeopleListParser listParser = new PeopleListParser();

    @Test
    public void execute_addEachRoleAndOptionalContacts_savesBeforePublishingAndReloads() throws Exception {
        for (PersonRole role : PersonRole.values()) {
            for (boolean hasOptionalContacts : new boolean[]{false, true}) {
                PonHubData live = new PonHubData(createPeopleState());
                PeopleView view = new PeopleView(live);
                view.setRoleFilter(Optional.of(PersonRole.STUDENT));
                var visible = view.getPeople();
                PonHubDataState before = live.exportState();
                JsonPonHubDataStorage storage = new JsonPonHubDataStorage(directory.resolve(
                        role.getValue() + hasOptionalContacts + ".json"));
                storage.saveData(before);
                int[] writes = {0};
                CanonicalCommandExecutor executor = new CanonicalCommandExecutor(view, state -> {
                    assertEquals(before, live.exportState());
                    assertEquals(Optional.of(PersonRole.STUDENT), view.getRoleFilter());
                    writes[0]++;
                    storage.saveData(state);
                }, Optional::empty);
                var input = addParser.parse(addArguments(role, hasOptionalContacts));
                CommandResult result = executor.execute(new CanonicalAddCommand(input));
                PersonRecord added = live.getPeople().getLast();
                PersonId expectedId = PersonId.of(role, before.people().getLastAllocatedSequences().get(role) + 1);
                assertEquals(expectedId, added.getId());
                assertEquals(input.contactDetails(), added.getContactDetails());
                if (added instanceof Student student) {
                    assertEquals(input.level().orElseThrow(), student.getLevel());
                    assertEquals(input.parentPhone().orElseThrow(), student.getParentPhone());
                }
                assertEquals("New " + role.getValue() + " added: New Person (" + expectedId + ").",
                        result.getFeedbackToUser());
                assertEquals(before.people().getPeople(), live.getPeople().subList(0, 4));
                assertEquals(Optional.empty(), view.getRoleFilter());
                assertSame(visible, view.getPeople());
                assertEquals(added, visible.getLast());
                assertEquals(1, writes[0]);
                assertEquals(live.exportState(), storage.readData().orElseThrow());
            }
        }
    }

    @Test
    public void execute_failedSaveThenRetry_preservesBytesFilterIndicesAndUncommittedId() throws Exception {
        PonHubData live = new PonHubData(createPeopleState());
        PeopleView view = new PeopleView(live);
        view.setRoleFilter(Optional.of(PersonRole.STUDENT));
        var visible = view.getPeople();
        int[] events = {0};
        visible.addListener((ListChangeListener<PersonRecord>) change -> events[0]++);
        PonHubDataState before = live.exportState();
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(directory.resolve("retry.json"));
        storage.saveData(before);
        byte[] original = Files.readAllBytes(storage.getDataFilePath());
        int[] writes = {0};
        CanonicalCommandExecutor executor = new CanonicalCommandExecutor(view, state -> {
            assertEquals(before, live.exportState());
            assertEquals(Optional.of(PersonRole.STUDENT), view.getRoleFilter());
            if (writes[0]++ == 0) {
                throw new IOException("Injected disk failure");
            }
            storage.saveData(state);
        }, Optional::empty);
        CanonicalAddCommand command = new CanonicalAddCommand(addParser.parse(addArguments(PersonRole.STUDENT, false)));
        CommandException failure = assertThrows(CommandException.class, () -> executor.execute(command));
        assertTrue(failure.getMessage().contains("Injected disk failure"));
        assertArrayEquals(original, Files.readAllBytes(storage.getDataFilePath()));
        assertEquals(before, live.exportState());
        assertSame(visible, view.getPeople());
        assertEquals(Optional.of(PersonRole.STUDENT), view.getRoleFilter());
        assertEquals(new PersonId("S3"), PersonIndexResolver.resolve(Index.fromOneBased(2), visible).getId());
        assertEquals(0, events[0]);

        assertEquals("New student added: New Person (S10).", executor.execute(command).getFeedbackToUser());
        assertEquals(new PersonId("S10"), live.getPeople().getLast().getId());
        assertEquals(Optional.empty(), view.getRoleFilter());
        assertEquals(live.exportState(), storage.readData().orElseThrow());
        assertEquals(2, writes[0]);
        assertTrue(events[0] > 0);
    }

    @Test
    public void execute_normalizedDuplicates_returnsRoleSpecificErrorsWithoutWritesOrViewChanges() throws Exception {
        PonHubData live = new PonHubData(createPeopleState());
        PeopleView view = new PeopleView(live);
        view.setRoleFilter(Optional.of(PersonRole.TUTOR));
        PonHubDataState before = live.exportState();
        CanonicalCommandExecutor executor = createNoWriteExecutor(view);
        Map<PersonRole, String> duplicates = Map.of(
                PersonRole.STUDENT, "r/student n/ALICE  tan l/S2 pp/00123456 p/00456789",
                PersonRole.TUTOR, "r/tutor n/TUTOR  tan p/00234567 e/Changed@example.com",
                PersonRole.PARENT, "r/parent n/PARENT  tan p/00345678 a/Changed address");
        for (PersonRole role : PersonRole.values()) {
            CanonicalAddCommand command = new CanonicalAddCommand(addParser.parse(duplicates.get(role)));
            CommandException failure = assertThrows(CommandException.class, () -> executor.execute(command));
            String phoneKind = role == PersonRole.STUDENT ? "parent phone" : "phone";
            assertEquals("A " + role.getValue() + " with the same name and " + phoneKind + " already exists.",
                    failure.getMessage());
            assertEquals(before, live.exportState());
            assertEquals(Optional.of(PersonRole.TUTOR), view.getRoleFilter());
            assertEquals(List.of(new PersonId("T7")), getVisibleIds(view));
        }
    }

    @Test
    public void execute_exhaustedRole_returnsCheckedErrorWithoutConsumingOtherIds() throws Exception {
        for (PersonRole role : PersonRole.values()) {
            EnumMap<PersonRole, Long> counters =
                    new EnumMap<>(createPeopleState().people().getLastAllocatedSequences());
            counters.put(role, Long.MAX_VALUE);
            PonHubDataState before = new PonHubDataState(
                    new PeopleRegistryState(createPeopleState().people().getPeople(), counters),
                    List.of(), List.of(), 12);
            PonHubData live = new PonHubData(before);
            PeopleView view = new PeopleView(live);
            view.setRoleFilter(Optional.of(PersonRole.PARENT));
            CanonicalCommandExecutor executor = createNoWriteExecutor(view);
            CanonicalAddCommand command = new CanonicalAddCommand(addParser.parse(addArguments(role, false)));
            CommandException failure = assertThrows(CommandException.class, () -> executor.execute(command));
            assertEquals("No more person IDs are available for role " + role.getValue() + ".", failure.getMessage());
            assertEquals(before, live.exportState());
            assertEquals(Optional.of(PersonRole.PARENT), view.getRoleFilter());
            assertEquals(List.of(new PersonId("P2")), getVisibleIds(view));
        }
    }

    @Test
    public void execute_listRolesAndAll_preservesGlobalOrderAndDataWithoutSaving() throws Exception {
        PonHubData live = new PonHubData(createPeopleState());
        PeopleView view = new PeopleView(live);
        var visible = view.getPeople();
        PonHubDataState before = live.exportState();
        CanonicalCommandExecutor executor = createNoWriteExecutor(view);
        assertEquals("Showing 4 person(s).", executor.execute(new CanonicalListCommand(listParser.parse("")))
                .getFeedbackToUser());
        assertEquals(List.of(new PersonId("T7"), new PersonId("S9"), new PersonId("P2"), new PersonId("S3")),
                getVisibleIds(view));
        assertEquals(new PersonId("S9"), PersonIndexResolver.resolve(Index.fromOneBased(2), visible).getId());
        assertEquals("Showing 2 student(s).", executor.execute(new CanonicalListCommand(listParser.parse("r/student")))
                .getFeedbackToUser());
        assertEquals(List.of(new PersonId("S9"), new PersonId("S3")), getVisibleIds(view));
        assertEquals(new PersonId("S3"), PersonIndexResolver.resolve(Index.fromOneBased(2), visible).getId());
        assertEquals("Showing 1 tutor(s).", executor.execute(new CanonicalListCommand(listParser.parse("r/TUTOR")))
                .getFeedbackToUser());
        assertEquals(List.of(new PersonId("T7")), getVisibleIds(view));
        assertEquals("Showing 1 parent(s).", executor.execute(new CanonicalListCommand(listParser.parse("r/parent")))
                .getFeedbackToUser());
        assertEquals(List.of(new PersonId("P2")), getVisibleIds(view));
        assertSame(visible, view.getPeople());
        assertEquals(before, live.exportState());
    }

    @Test
    public void execute_emptyRoleAndInvalidArguments_preservesDataAndLastSuccessfulView() throws Exception {
        PonHubDataState before = new PonHubDataState(new PeopleRegistryState(List.of(createPeopleState().people()
                .getPeople().get(1)), createPeopleState().people().getLastAllocatedSequences()),
                List.of(), List.of(), 12);
        PonHubData live = new PonHubData(before);
        PeopleView view = new PeopleView(live);
        CanonicalCommandExecutor executor = createNoWriteExecutor(view);
        assertEquals("Showing 0 tutor(s).", executor.execute(new CanonicalListCommand(listParser.parse("r/tutor")))
                .getFeedbackToUser());
        assertTrue(view.getPeople().isEmpty());
        for (String arguments : new String[]{"r/", "r/all", "r/student r/parent", "r/student extra"}) {
            assertThrows(ParseException.class, () -> executor.execute(
                    new CanonicalListCommand(listParser.parse(arguments))));
            assertEquals(Optional.of(PersonRole.TUTOR), view.getRoleFilter());
            assertTrue(view.getPeople().isEmpty());
        }
        for (String arguments : new String[]{"r/student n/New l/P1", "r/tutor n/New p/00123456 l/P1",
            "r/parent n/New p/00123456 n/Repeated", "r/parent n/New p/00123456 x/Unknown"}) {
            assertThrows(ParseException.class, () -> executor.execute(
                    new CanonicalAddCommand(addParser.parse(arguments))));
            assertEquals(before, live.exportState());
            assertEquals(Optional.of(PersonRole.TUTOR), view.getRoleFilter());
        }
    }

    @Test
    public void execute_protectedAddition_rejectsWithoutPublishingWhileListStillWorks() throws Exception {
        PonHubData live = new PonHubData(createPeopleState());
        PeopleView view = new PeopleView(live);
        view.setRoleFilter(Optional.of(PersonRole.PARENT));
        PonHubDataState before = live.exportState();
        CanonicalCommandExecutor executor = new CanonicalCommandExecutor(view, state -> {
            throw new AssertionError("Protected commands must never save");
        }, () -> Optional.of("Restore the rejected file and restart"));
        CanonicalAddCommand command = new CanonicalAddCommand(addParser.parse(addArguments(PersonRole.TUTOR, false)));
        CommandException failure = assertThrows(CommandException.class, () -> executor.execute(command));
        assertEquals("Restore the rejected file and restart", failure.getMessage());
        assertEquals(before, live.exportState());
        assertEquals(Optional.of(PersonRole.PARENT), view.getRoleFilter());
        assertEquals("Showing 2 student(s).", executor.execute(new CanonicalListCommand(listParser.parse("r/student")))
                .getFeedbackToUser());
        assertEquals(before, live.exportState());
    }

    @Test
    public void execute_addWithAttendance_preservesRelationshipsInSavedSnapshot() throws Exception {
        PonHubDataState peopleOnly = createPeopleState();
        Lesson lesson = new Lesson(new LessonId("L12"), new PersonId("T7"), new LessonTimeSlot(LessonDay.MONDAY,
                new LessonTime("0900"), new LessonTime("1000")), new Subject("Math"), new Room("R1"),
                Set.of(new PersonId("S9")));
        Attendance attendance = new Attendance(new PersonId("S3"), lesson.getId(), LocalDate.of(2026, 10, 5),
                AttendanceStatus.PRESENT);
        PonHubDataState before = new PonHubDataState(peopleOnly.people(), List.of(lesson), List.of(attendance), 12);
        PonHubData live = new PonHubData(before);
        PeopleView view = new PeopleView(live);
        view.setRoleFilter(Optional.of(PersonRole.STUDENT));
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(directory.resolve("attendance.json"));
        storage.saveData(before);
        CanonicalCommandExecutor executor = new CanonicalCommandExecutor(view, state -> {
            assertEquals(before.lessons(), state.lessons());
            assertEquals(before.attendance(), state.attendance());
            assertEquals(12, state.lastAllocatedLessonSequence());
            storage.saveData(state);
        }, Optional::empty);
        CanonicalAddCommand command = new CanonicalAddCommand(addParser.parse(addArguments(PersonRole.STUDENT, false)));
        executor.execute(command);
        assertEquals(before.lessons(), live.exportState().lessons());
        assertEquals(before.attendance(), live.exportState().attendance());
        assertEquals(before.people().getPeople().size() + 1, live.exportState().people().getPeople().size());
        assertEquals(live.exportState(), storage.readData().orElseThrow());
    }

    private static CanonicalCommandExecutor createNoWriteExecutor(PeopleView view) {
        return new CanonicalCommandExecutor(view, state -> {
            throw new AssertionError("Read-only or rejected commands must never save");
        }, Optional::empty);
    }

    private static List<PersonId> getVisibleIds(PeopleView view) {
        return view.getPeople().stream().map(PersonRecord::getId).toList();
    }

    private static String addArguments(PersonRole role, boolean hasOptionalContacts) {
        String required = role == PersonRole.STUDENT ? "r/StUdEnT n/New Person l/p2 pp/00123456"
                : "r/" + role.getValue() + " n/New Person p/00012345";
        String optional = hasOptionalContacts ? " e/New@example.com a/Block 2" : "";
        if (role == PersonRole.STUDENT && hasOptionalContacts) {
            optional += " p/00456789";
        }
        return required + optional;
    }

    private static PonHubDataState createPeopleState() {
        Tutor tutor = new Tutor(new PersonId("T7"), createContacts("Tutor Tan", "00234567"));
        Student first = new Student(new PersonId("S9"), new ContactDetails(new Name("Alice Tan")),
                new EducationLevel("P1"), new Phone("00123456"));
        Parent parent = new Parent(new PersonId("P2"), createContacts("Parent Tan", "00345678"));
        Student second = new Student(new PersonId("S3"), new ContactDetails(new Name("Bob Tan")),
                new EducationLevel("P2"), new Phone("00123456"));
        return new PonHubDataState(new PeopleRegistryState(List.of(tutor, first, parent, second),
                Map.of(PersonRole.STUDENT, 9L, PersonRole.TUTOR, 7L, PersonRole.PARENT, 2L)),
                List.of(), List.of(), 12);
    }

    private static ContactDetails createContacts(String name, String phone) {
        return new ContactDetails(new Name(name), Optional.of(new Phone(phone)), Optional.empty(), Optional.empty());
    }
}
