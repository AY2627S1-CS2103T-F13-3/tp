package ponhub.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.collections.ListChangeListener;
import ponhub.commons.core.index.Index;
import ponhub.logic.commands.CommandResult;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.RoleAwareAddParser;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.PonHubData;
import ponhub.model.PonHubDataState;
import ponhub.model.lesson.LessonId;
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
import ponhub.model.query.LessonView;
import ponhub.model.query.StudentView;
import ponhub.storage.JsonPonHubDataStorage;
import ponhub.storage.ProtectedPonHubDataStorage;
import ponhub.storage.ProtectedPonHubDataStorage.LoadStatus;
import ponhub.testutil.LessonQueryTestData;

/**
 * Exercises the application session through real startup, command strings and canonical persistence.
 */
public class CanonicalPeopleSessionTest {
    @TempDir
    Path directory;

    @Test
    public void execute_unicodeFieldBoundaries_preservesCommandFeedbackAndPersistence() throws Exception {
        Path path = directory.resolve("unicode-boundaries.json");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);

        assertEquals("New tutor added: Mei Lim (T1).", session.execute(
                "add r/\u00A0tutor\u202Fn/\u3000Mei Lim\u00A0p/00123456\u3000").getFeedbackToUser());
        assertEquals("New student added: Alex Tan (S1).", session.execute(
                "add r/student n/Alex Tan l/S2 pp/00123456").getFeedbackToUser());
        assertEquals("New lesson added: L1.", session.execute(
                "addlesson d/\u00A0Mon\u202Fst/\u30000900\u00A0et/1030\u3000s/Math\u00A0tu/Mei Lim\u202Frm/R1\u3000")
                .getFeedbackToUser());
        session.execute("list r/student");
        session.execute("enrol 1 lid/L1");
        assertEquals("Attendance for Student S1 in lesson L1 on 2026-10-05 marked present.", session.execute(
                "mark \u00A01\u202Flid/\u3000L1\u00A0d/2026-10-05 s/\u202Fpresent\u3000").getFeedbackToUser());
        assertEquals("Attendance for Student S1 in lesson L1 on 2026-10-05 removed.", session.execute(
                "unmark \u30001\u00A0lid/\u202FL1\u3000d/\u00A02026-10-05\u202F").getFeedbackToUser());

        assertTrue(session.getDataSnapshot().attendance().isEmpty());
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());

        PonHubDataState before = session.getDataSnapshot();
        byte[] originalBytes = Files.readAllBytes(path);
        String[][] rejectedCommands = {
            {"mark 0 x/value\t", "Mark arguments must be on one line and use spaces rather than control characters."},
            {"addlesson x/value\n", "Commands must be on one line."}
        };
        for (String[] rejected : rejectedCommands) {
            ParseException failure = assertThrows(ParseException.class, () -> session.execute(rejected[0]));
            assertEquals(rejected[1], failure.getMessage());
            assertSame(before, session.getDataSnapshot());
            assertArrayEquals(originalBytes, Files.readAllBytes(path));
        }
    }

    @Test
    public void construct_missingFile_startsEmptyAndReadCommandsDoNotCreateFile() throws Exception {
        Path path = directory.resolve("fresh folder/canonical.json");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);

        assertEquals(LoadStatus.MISSING, session.getLoadStatus());
        assertEquals(path, session.getDataFilePath());
        assertTrue(session.getDataLoadError().isEmpty());
        assertEquals(List.of("add", "edit", "addlesson", "editlesson", "enrol", "unenrol", "mark", "unmark",
                "history", "delete", "deletelesson", "list", "lessons", "showlesson", "search", "help", "exit"),
                session.getCommandWords());
        assertEquals(new PonHubData().exportState(), session.getDataSnapshot());
        assertTrue(session.getPeople().isEmpty());
        assertTrue(session.getLessonQueries().getLessonList().isEmpty());
        assertEquals("Showing 0 student(s).", session.execute("list r/student").getFeedbackToUser());
        assertTrue(session.execute("help").getFeedbackToUser().contains("add"));
        assertTrue(session.execute("exit").isExit());
        assertFalse(Files.exists(path));
        assertFalse(Files.exists(path.getParent()));
    }

    @Test
    public void execute_readCommands_preservesSnapshotBytesAndStableReadOnlyViews() throws Exception {
        Path path = directory.resolve("canonical.json");
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        PonHubDataState before = peopleState();
        storage.saveData(before);
        byte[] original = Files.readAllBytes(path);
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            throw new AssertionError("Read commands must not save operational data");
        });
        var visible = session.getPeople();
        var queries = session.getLessonQueries();
        var lessons = queries.getLessonList();

        assertEquals(LoadStatus.LOADED, session.getLoadStatus());
        assertEquals(before, session.getDataSnapshot());
        assertEquals(List.of(new PersonId("T7"), new PersonId("S9"), new PersonId("P2"), new PersonId("S3")),
                visibleIds(session));
        assertEquals("Showing 2 student(s).", session.execute("list r/StUdEnT").getFeedbackToUser());
        assertEquals(Optional.of(PersonRole.STUDENT), session.getRoleFilter());
        assertEquals(List.of(new PersonId("S9"), new PersonId("S3")), visibleIds(session));
        assertEquals(new PersonId("S3"), PersonIndexResolver.resolve(Index.fromOneBased(2), visible).getId());
        assertEquals("Showing 1 tutor(s).", session.execute("list r/tutor").getFeedbackToUser());
        assertEquals("Showing 1 parent(s).", session.execute("list r/parent").getFeedbackToUser());
        assertEquals("Showing 4 person(s).", session.execute("list").getFeedbackToUser());
        assertEquals(Optional.empty(), session.getRoleFilter());
        for (String command : List.of("help", "help ADD", "help ADDLESSON", "help EDITLESSON", "help ENROL",
                "help UNENROL", "help MARK", "help UNMARK", "help DELETE", "help DELETELESSON", "help list",
                "help LESSONS", "help help", "help exit", "exit")) {
            session.execute(command);
        }

        assertEquals(before, session.getDataSnapshot());
        assertSame(visible, session.getPeople());
        assertSame(queries, session.getLessonQueries());
        assertSame(lessons, session.getLessonQueries().getLessonList());
        assertThrows(UnsupportedOperationException.class, visible::clear);
        assertArrayEquals(original, Files.readAllBytes(path));
    }

    @Test
    public void execute_rejectedStartup_allowsOnlyRecoveryCommandsAndPreservesOriginalBytes() throws Exception {
        Map<LoadStatus, String> rejected = Map.of(LoadStatus.CORRUPT, "{", LoadStatus.LEGACY, "{\"persons\":[]}",
                LoadStatus.UNSUPPORTED_VERSION, "{\"schemaVersion\":99}");
        for (var entry : rejected.entrySet()) {
            Path path = directory.resolve(entry.getKey() + ".json");
            Files.writeString(path, entry.getValue());
            byte[] original = Files.readAllBytes(path);
            CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path),
                    state -> {
                        throw new AssertionError("Protected sessions must never save");
                    });
            String guidance = session.getDataLoadError().orElseThrow();

            assertEquals(entry.getKey(), session.getLoadStatus());
            assertEquals(List.of("help", "list", "exit"), session.getCommandWords());
            assertEquals(new PonHubData().exportState(), session.getDataSnapshot());
            assertTrue(session.getPeople().isEmpty());
            assertTrue(guidance.contains(path.toAbsolutePath().toString()));
            assertTrue(guidance.contains("restart"));
            assertTrue(guidance.contains("An empty view is not your saved data"));
            String overview = session.execute("help").getFeedbackToUser();
            assertTrue(overview.contains(guidance));
            assertFalse(overview.contains("delete -"));
            assertFalse(overview.contains("deletelesson -"));
            assertFalse(overview.contains("add -"));
            assertFalse(overview.contains("edit -"));
            assertFalse(overview.contains("addlesson -"));
            assertFalse(overview.contains("editlesson -"));
            assertFalse(overview.contains("enrol -"));
            assertFalse(overview.contains("unenrol -"));
            assertFalse(overview.contains("mark -"));
            assertFalse(overview.contains("unmark -"));
            assertFalse(overview.contains("lessons -"));
            assertThrows(ParseException.class, () -> session.execute("help add"));
            assertThrows(ParseException.class, () -> session.execute("help EDIT"));
            assertThrows(ParseException.class, () -> session.execute("help addlesson"));
            assertThrows(ParseException.class, () -> session.execute("help editlesson"));
            assertThrows(ParseException.class, () -> session.execute("help EDITLESSON"));
            assertThrows(ParseException.class, () -> session.execute("help enrol"));
            assertThrows(ParseException.class, () -> session.execute("help unenrol"));
            assertThrows(ParseException.class, () -> session.execute("help mark"));
            assertThrows(ParseException.class, () -> session.execute("help unmark"));
            assertThrows(ParseException.class, () -> session.execute("help delete"));
            assertThrows(ParseException.class, () -> session.execute("help deletelesson"));
            assertThrows(ParseException.class, () -> session.execute("help lessons"));
            for (String command : List.of("add", "add\n", addCommand(PersonRole.STUDENT), "delete 1", "edit 1",
                    "delete \0 1", "delete 1\b", "delete 1\u001b", "delete 1\0\n",
                    "addlesson d/Mon st/0900 et/1000 s/Math tu/Tutor Tan rm/R1",
                    "editlesson lid/L1 s/Math",
                    "enrol 1 lid/L1", "unenrol 1 lid/L1", "mark 1 lid/L1 d/2026-10-05 s/present",
                    "unmark 1 lid/L1 d/2026-10-05", "deletelesson lid/L1", "lessons", "search c/lesson",
                    "find Name", "unknown", "ADD")) {
                CommandException failure = assertThrows(CommandException.class, () -> session.execute(command));
                assertEquals(guidance, failure.getMessage());
                assertArrayEquals(original, Files.readAllBytes(path));
            }
            for (String command : List.of("list", "list r/student", "list r/tutor", "list r/parent")) {
                assertTrue(session.execute(command).getFeedbackToUser().contains(guidance));
            }
            assertTrue(session.execute("exit").isExit());
            assertEquals(new PonHubData().exportState(), session.getDataSnapshot());
            assertArrayEquals(original, Files.readAllBytes(path));
        }
    }

    @Test
    public void construct_unreadablePath_createsProtectedEmptySessionWithoutReplacingPath() throws Exception {
        Path path = directory.resolve("canonical.json");
        Files.createDirectory(path);
        Path marker = path.resolve("retain.txt");
        Files.writeString(marker, "Retain this directory");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);

        assertEquals(LoadStatus.UNREADABLE, session.getLoadStatus());
        assertTrue(session.getPeople().isEmpty());
        assertThrows(CommandException.class, () -> session.execute(addCommand(PersonRole.TUTOR)));
        assertTrue(session.execute("list").getFeedbackToUser().contains("UNREADABLE"));
        assertTrue(Files.isDirectory(path));
        assertEquals("Retain this directory", Files.readString(marker));
    }

    @Test
    public void execute_eachRole_savesBeforePublishingAndReloadsWithAllocatedIdsAndContacts() throws Exception {
        RoleAwareAddParser parser = new RoleAwareAddParser();
        for (PersonRole role : PersonRole.values()) {
            Path path = directory.resolve(role.getValue() + ".json");
            JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
            PonHubDataState before = peopleState();
            storage.saveData(before);
            CanonicalPeopleSession[] current = new CanonicalPeopleSession[1];
            int[] writes = {0};
            current[0] = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
                assertEquals(before, current[0].getDataSnapshot());
                assertEquals(Optional.of(PersonRole.STUDENT), current[0].getRoleFilter());
                storage.saveData(state);
                writes[0]++;
            });
            CanonicalPeopleSession session = current[0];
            session.execute("list r/student");
            var visible = session.getPeople();
            var queries = session.getLessonQueries();
            CommandResult result = session.execute(addCommand(role));
            PersonId expectedId = PersonId.of(role, before.people().getLastAllocatedSequences().get(role) + 1);
            PersonRecord added = session.getDataSnapshot().people().getPeople().getLast();
            var input = parser.parse(addArguments(role));

            assertEquals("New " + role.getValue() + " added: New Person (" + expectedId + ").",
                    result.getFeedbackToUser());
            assertEquals(expectedId, added.getId());
            assertEquals(input.contactDetails(), added.getContactDetails());
            if (added instanceof Student student) {
                assertEquals(input.level().orElseThrow(), student.getLevel());
                assertEquals(input.parentPhone().orElseThrow(), student.getParentPhone());
                assertTrue(queries.getStudentLessons(expectedId).isEmpty());
            } else if (added instanceof Tutor) {
                assertTrue(queries.getTutorSchedule(expectedId).isEmpty());
            }
            assertEquals(before.people().getPeople(), session.getDataSnapshot().people().getPeople().subList(0, 4));
            assertEquals(before.lastAllocatedLessonSequence(), session.getDataSnapshot().lastAllocatedLessonSequence());
            assertEquals(Optional.empty(), session.getRoleFilter());
            assertSame(visible, session.getPeople());
            assertSame(queries, session.getLessonQueries());
            assertEquals(added, visible.getLast());
            assertEquals(1, writes[0]);
            CanonicalPeopleSession restarted = new CanonicalPeopleSession(path);
            assertEquals(LoadStatus.LOADED, restarted.getLoadStatus());
            assertEquals(session.getDataSnapshot(), restarted.getDataSnapshot());
        }
    }

    @Test
    public void execute_addWithPersistedLessons_preservesRostersCountersAndQueryListLifetime() throws Exception {
        Path path = directory.resolve("canonical.json");
        PonHubDataState before = LessonQueryTestData.createData().exportState();
        new JsonPonHubDataStorage(path).saveData(before);
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        var people = session.getPeople();
        var queries = session.getLessonQueries();
        var lessons = queries.getLessonList();
        var filtered = queries.getFilteredLessonList();
        var roster = queries.getLessonRoster(LessonQueryTestData.SHARED_LESSON);
        var studentLessons = queries.getStudentLessons(LessonQueryTestData.STUDENT_ONE);
        List<LessonId> lessonIds = lessons.stream().map(LessonView::getLessonId).toList();
        List<LessonId> studentLessonIds = studentLessons.stream()
                .map(LessonView::getLessonId).toList();
        List<PersonId> rosterIds = List.of(LessonQueryTestData.STUDENT_ONE, LessonQueryTestData.STUDENT_TWO);

        assertEquals(LoadStatus.LOADED, session.getLoadStatus());
        assertEquals(before, session.getDataSnapshot());
        assertEquals(rosterIds, roster.stream().map(StudentView::getStudentId).toList());
        assertEquals(2, queries.findLessonById(LessonQueryTestData.SHARED_LESSON).orElseThrow().getRosterSize());
        session.execute("list r/parent");
        assertEquals("New student added: New Person (S4).",
                session.execute(addCommand(PersonRole.STUDENT)).getFeedbackToUser());

        PonHubDataState committed = session.getDataSnapshot();
        assertEquals(before.lessons(), committed.lessons());
        assertEquals(before.attendance(), committed.attendance());
        assertEquals(before.lastAllocatedLessonSequence(), committed.lastAllocatedLessonSequence());
        assertEquals(4L, committed.people().getLastAllocatedSequences().get(PersonRole.STUDENT));
        for (PersonRole role : List.of(PersonRole.TUTOR, PersonRole.PARENT)) {
            assertEquals(before.people().getLastAllocatedSequences().get(role),
                    committed.people().getLastAllocatedSequences().get(role));
        }
        assertSame(people, session.getPeople());
        assertSame(queries, session.getLessonQueries());
        assertSame(lessons, queries.getLessonList());
        assertSame(filtered, queries.getFilteredLessonList());
        assertSame(roster, queries.getLessonRoster(LessonQueryTestData.SHARED_LESSON));
        assertSame(studentLessons, queries.getStudentLessons(LessonQueryTestData.STUDENT_ONE));
        assertEquals(lessonIds, lessons.stream().map(LessonView::getLessonId).toList());
        assertEquals(studentLessonIds, studentLessons.stream().map(LessonView::getLessonId).toList());
        assertEquals(rosterIds, roster.stream().map(StudentView::getStudentId).toList());
        assertTrue(queries.getStudentLessons(new PersonId("S4")).isEmpty());
        assertEquals(committed, new CanonicalPeopleSession(path).getDataSnapshot());
    }

    @Test
    public void execute_failedSaveThenRetry_retainsBytesCountersFilterIndicesAndViewIdentity() throws Exception {
        Path path = directory.resolve("canonical.json");
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        PonHubDataState before = peopleState();
        storage.saveData(before);
        byte[] original = Files.readAllBytes(path);
        CanonicalPeopleSession[] current = new CanonicalPeopleSession[1];
        int[] writes = {0};
        current[0] = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            assertEquals(before, current[0].getDataSnapshot());
            assertEquals(Optional.of(PersonRole.STUDENT), current[0].getRoleFilter());
            if (writes[0]++ == 0) {
                throw new IOException("Injected disk failure");
            }
            storage.saveData(state);
        });
        CanonicalPeopleSession session = current[0];
        session.execute("list r/student");
        var visible = session.getPeople();
        var queries = session.getLessonQueries();
        PersonRecord selected = PersonIndexResolver.resolve(Index.fromOneBased(2), visible);
        int[] events = {0};
        visible.addListener((ListChangeListener<PersonRecord>) change -> events[0]++);

        CommandException failure = assertThrows(CommandException.class, () ->
                session.execute(addCommand(PersonRole.STUDENT)));
        assertTrue(failure.getMessage().contains("Injected disk failure"));
        assertArrayEquals(original, Files.readAllBytes(path));
        assertEquals(before, session.getDataSnapshot());
        assertEquals(Optional.of(PersonRole.STUDENT), session.getRoleFilter());
        assertSame(visible, session.getPeople());
        assertSame(queries, session.getLessonQueries());
        assertSame(selected, PersonIndexResolver.resolve(Index.fromOneBased(2), visible));
        assertEquals(new PersonId("S3"), selected.getId());
        assertEquals(0, events[0]);

        assertEquals("New student added: New Person (S10).",
                session.execute(addCommand(PersonRole.STUDENT)).getFeedbackToUser());
        assertEquals(new PersonId("S10"), session.getPeople().getLast().getId());
        assertEquals(Optional.empty(), session.getRoleFilter());
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
        assertEquals(2, writes[0]);
        assertTrue(events[0] > 0);
    }

    @Test
    public void execute_invalidUnknownAndDuplicateCommands_doesNotSaveOrChangeCurrentView() throws Exception {
        Path path = directory.resolve("canonical.json");
        new JsonPonHubDataStorage(path).saveData(peopleState());
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            throw new AssertionError("Rejected commands must not save");
        });
        session.execute("list r/parent");
        PonHubDataState before = session.getDataSnapshot();
        var visible = session.getPeople();
        byte[] original = Files.readAllBytes(path);

        for (String command : List.of("", "ADD", "delete P1", "edit 1", "list r/teacher", "list r/student r/tutor",
                "add n/New Person", "help clear", "help add list", "exit now", "list\n", "\nhelp", "help add\n")) {
            assertThrows(ParseException.class, () -> session.execute(command));
            assertEquals(before, session.getDataSnapshot());
            assertEquals(Optional.of(PersonRole.PARENT), session.getRoleFilter());
            assertSame(visible, session.getPeople());
            assertArrayEquals(original, Files.readAllBytes(path));
        }
        assertThrows(CommandException.class, () ->
                session.execute("add r/student n/ALICE  tan l/S2 pp/00123456"));
        assertEquals(before, session.getDataSnapshot());
        assertEquals(Optional.of(PersonRole.PARENT), session.getRoleFilter());
        assertEquals(List.of(new PersonId("P2")), visibleIds(session));
        assertArrayEquals(original, Files.readAllBytes(path));
    }

    @Test
    public void execute_help_describesPreparedRoutesAndPreservesExitResultFlags() throws Exception {
        CanonicalPeopleSession session = new CanonicalPeopleSession(directory.resolve("canonical.json"));
        String overview = session.execute("help").getFeedbackToUser();

        assertTrue(overview.contains("add"));
        assertTrue(overview.contains("addlesson"));
        assertTrue(overview.contains("enrol"));
        assertTrue(overview.contains("unenrol"));
        assertTrue(overview.contains("mark"));
        assertTrue(overview.contains("unmark"));
        assertTrue(overview.contains("list"));
        assertTrue(overview.contains("help"));
        assertTrue(overview.contains("exit"));
        assertTrue(overview.contains("delete"));
        assertTrue(overview.contains("deletelesson"));
        String addUsage = session.execute("help ADD").getFeedbackToUser();
        assertTrue(addUsage.contains("r/student"));
        assertTrue(addUsage.contains("pp/"));
        String addLessonUsage = session.execute("help ADDLESSON").getFeedbackToUser();
        assertTrue(addLessonUsage.contains("d/DAY"));
        assertTrue(addLessonUsage.contains("tp/TUTOR_PHONE"));
        String editLessonUsage = session.execute("help EDITLESSON").getFeedbackToUser();
        assertTrue(editLessonUsage.contains("lid/LESSON_ID"));
        assertTrue(editLessonUsage.contains("at least one field"));
        assertTrue(session.execute("help ENROL").getFeedbackToUser().contains("STUDENT_INDEX lid/LESSON_ID"));
        assertTrue(session.execute("help UNENROL").getFeedbackToUser().contains("dated attendance"));
        assertTrue(session.execute("help MARK").getFeedbackToUser().contains("d/YYYY-MM-DD"));
        assertTrue(session.execute("help UNMARK").getFeedbackToUser().contains("Removes only"));
        assertTrue(session.execute("help DELETE").getFeedbackToUser().contains("Usage: delete INDEX"));
        assertTrue(session.execute("help DELETELESSON").getFeedbackToUser().contains("lid/LESSON_ID"));
        assertTrue(session.execute("help LESSONS").getFeedbackToUser().contains("Usage: lessons"));
        assertTrue(session.execute("help list").getFeedbackToUser().contains("r/"));
        assertFalse(session.execute("help").isExit());
        assertTrue(session.execute("exit").isExit());
    }

    @Test
    public void execute_repairedRejectedFile_requiresNewSessionBeforeWriting() throws Exception {
        Path path = directory.resolve("canonical.json");
        Files.writeString(path, "broken");
        CanonicalPeopleSession rejected = new CanonicalPeopleSession(path);
        String originalGuidance = rejected.getDataLoadError().orElseThrow();
        new JsonPonHubDataStorage(path).saveData(peopleState());
        byte[] repaired = Files.readAllBytes(path);

        assertEquals(originalGuidance, rejected.getDataLoadError().orElseThrow());
        assertEquals(LoadStatus.CORRUPT, rejected.getLoadStatus());
        assertThrows(CommandException.class, () -> rejected.execute(addCommand(PersonRole.STUDENT)));
        assertTrue(rejected.execute("list").getFeedbackToUser().contains(originalGuidance));
        assertTrue(rejected.getPeople().isEmpty());
        assertArrayEquals(repaired, Files.readAllBytes(path));

        CanonicalPeopleSession restarted = new CanonicalPeopleSession(path);
        assertEquals(LoadStatus.LOADED, restarted.getLoadStatus());
        assertTrue(restarted.getDataLoadError().isEmpty());
        assertEquals(peopleState(), restarted.getDataSnapshot());
        assertEquals("New student added: New Person (S10).",
                restarted.execute(addCommand(PersonRole.STUDENT)).getFeedbackToUser());
        assertEquals(restarted.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
    }

    @Test
    public void execute_removedRejectedFile_doesNotUnlockOrRecreateFile() throws Exception {
        Path path = directory.resolve("canonical.json");
        Files.writeString(path, "broken");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        Files.delete(path);

        assertThrows(CommandException.class, () -> session.execute(addCommand(PersonRole.PARENT)));
        assertEquals(LoadStatus.CORRUPT, session.getLoadStatus());
        assertTrue(session.execute("list r/parent").getFeedbackToUser().contains("restart"));
        assertTrue(session.getPeople().isEmpty());
        assertFalse(Files.exists(path));
    }

    private static List<PersonId> visibleIds(CanonicalPeopleSession session) {
        return session.getPeople().stream().map(PersonRecord::getId).toList();
    }

    private static String addCommand(PersonRole role) {
        return "add " + addArguments(role);
    }

    private static String addArguments(PersonRole role) {
        String required = role == PersonRole.STUDENT ? "r/student n/New Person l/p2 pp/00123456 p/00456789"
                : "r/" + role.getValue() + " n/New Person p/00012345";
        return required + " e/New@example.com a/Block 2";
    }

    private static PonHubDataState peopleState() {
        Tutor tutor = new Tutor(new PersonId("T7"), contacts("Tutor Tan", "00234567"));
        Student first = new Student(new PersonId("S9"), new ContactDetails(new Name("Alice Tan")),
                new EducationLevel("P1"), new Phone("00123456"));
        Parent parent = new Parent(new PersonId("P2"), contacts("Parent Tan", "00345678"));
        Student second = new Student(new PersonId("S3"), new ContactDetails(new Name("Bob Tan")),
                new EducationLevel("P2"), new Phone("00123456"));
        return new PonHubDataState(new PeopleRegistryState(List.of(tutor, first, parent, second),
                Map.of(PersonRole.STUDENT, 9L, PersonRole.TUTOR, 7L, PersonRole.PARENT, 2L)),
                List.of(), List.of(), 12);
    }

    private static ContactDetails contacts(String name, String phone) {
        return new ContactDetails(new Name(name), Optional.of(new Phone(phone)), Optional.empty(), Optional.empty());
    }
}
