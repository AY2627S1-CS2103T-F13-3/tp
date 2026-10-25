package ponhub.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ponhub.commons.core.GuiSettings;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.UserPrefs;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRole;
import ponhub.storage.JsonPonHubDataStorage;
import ponhub.storage.JsonUserPrefsStorage;
import ponhub.storage.ProtectedPonHubDataStorage;
import ponhub.testutil.LessonQueryTestData;
import ponhub.testutil.LogCapture;

/**
 * Verifies the application's Logic boundary shares the canonical session and independent preferences.
 */
public class CanonicalLogicManagerTest {
    @TempDir
    Path directory;

    @Test
    public void execute_cyclicSaveCause_boundsDiagnosticsWithoutRetainingExceptionMessages() throws Exception {
        IOException first = new IOException("private378@example.com");
        IOException second = new IOException("378 Canary Avenue");
        first.initCause(second);
        second.initCause(first);
        CanonicalPeopleSession session = new CanonicalPeopleSession(
                new ProtectedPonHubDataStorage(directory.resolve("data.json")), state -> {
                    throw first;
                });
        Logic logic = new CanonicalLogicManager(session, new UserPrefs());
        try (LogCapture capture = new LogCapture()) {
            assertThrows(CommandException.class, () -> logic.execute("add r/tutor n/Privacy Canary p/99887766"));
            assertEquals(1, capture.getRecords().size());
            assertEquals("Command type=add outcome=command_rejected cause=CommandException"
                    + ">IOException".repeat(7), capture.getRecords().getFirst().getMessage());
            assertEquals(Level.WARNING, capture.getRecords().getFirst().getLevel());
            assertFalse(capture.getFormattedText().contains("private378@example.com"));
            assertFalse(capture.getFormattedText().contains("378 Canary Avenue"));
        }
    }

    @Test
    public void execute_logsSafeCategoriesWithoutRetainingSuccessfulDeletedOrRejectedPayloads() throws Exception {
        Logic logic = new CanonicalLogicManager(new CanonicalPeopleSession(directory.resolve("data.json")),
                new UserPrefs());
        String addition = "add r/tutor n/Privacy Canary p/99887766 e/privacy378@example.com a/378 Canary Avenue";
        try (LogCapture capture = new LogCapture()) {
            assertTrue(logic.execute(addition).getFeedbackToUser().contains("Privacy Canary"));
            assertThrows(CommandException.class, () -> logic.execute(addition));
            logic.execute("delete 1");
            assertTrue(logic.getPersonRecords().isEmpty());
            assertThrows(ParseException.class, () -> logic.execute(addition + "\nInjected378"));
            assertThrows(ParseException.class, () -> logic.execute("privacy378@example.com a/378 Canary Avenue"));
            assertThrows(ParseException.class, () -> logic.execute("add r/tutor n/Privacy Canary p/invalid378"));
            assertThrows(ParseException.class, () -> logic.execute("   "));

            assertEquals(List.of("Command type=add outcome=success",
                    "Command type=add outcome=command_rejected cause=CommandException>DuplicatePersonException",
                    "Command type=delete outcome=success",
                    "Command type=add outcome=parse_rejected cause=ParseException",
                    "Command type=unknown outcome=parse_rejected cause=ParseException",
                    "Command type=add outcome=parse_rejected cause=ParseException",
                    "Command type=unknown outcome=parse_rejected cause=ParseException"),
                    capture.getRecords().stream().map(record -> record.getMessage()).toList());
            for (String privateValue : List.of("Privacy Canary", "99887766", "privacy378@example.com",
                    "378 Canary Avenue", "Injected378", "invalid378", "New tutor added")) {
                assertFalse(capture.getFormattedText().contains(privateValue), privateValue);
            }
            assertTrue(capture.getRecords().stream().allMatch(record -> record.getThrown() == null
                    && record.getParameters() == null));
        }
    }

    @Test
    public void execute_protectedSessionAndStorageCause_doNotLogExceptionPayloads() throws Exception {
        Path path = directory.resolve("private378@example.com.json");
        Files.writeString(path, "Rejected Privacy Canary data");
        Logic protectedLogic = new CanonicalLogicManager(new CanonicalPeopleSession(path), new UserPrefs());
        CanonicalPeopleSession failedSave = new CanonicalPeopleSession(
                new ProtectedPonHubDataStorage(directory.resolve("save.json")), state -> {
                    throw new IOException("privacy378@example.com", new AccessDeniedException("378 Canary Avenue"));
                });
        Logic savingLogic = new CanonicalLogicManager(failedSave, new UserPrefs());
        try (LogCapture capture = new LogCapture()) {
            assertThrows(CommandException.class, () -> protectedLogic.execute("add n/Privacy Canary"));
            assertThrows(CommandException.class, () -> savingLogic.execute(
                    "add r/tutor n/Privacy Canary p/99887766 e/privacy378@example.com a/378 Canary Avenue"));
            assertEquals(2, capture.getRecords().size());
            assertEquals("Command type=add outcome=command_rejected cause=CommandException",
                    capture.getRecords().get(0).getMessage());
            assertEquals("Command type=add outcome=command_rejected "
                    + "cause=CommandException>IOException>AccessDeniedException",
                    capture.getRecords().get(1).getMessage());
            assertEquals(Level.WARNING, capture.getRecords().get(1).getLevel());
            for (String privateValue : List.of("Privacy Canary", "privacy378@example.com", "378 Canary Avenue",
                    "99887766", "Rejected Privacy Canary data")) {
                assertFalse(capture.getFormattedText().contains(privateValue), privateValue);
            }
            assertTrue(capture.getRecords().stream().allMatch(record -> record.getThrown() == null
                    && record.getParameters() == null));
        }
        assertEquals("Rejected Privacy Canary data", Files.readString(path));
        assertTrue(savingLogic.getPersonRecords().isEmpty());
    }

    @Test
    public void getCommandWords_usesCanonicalDefinitionsAndStartupProtectionWithoutWriting() throws Exception {
        Path path = directory.resolve("canonical.json");
        Logic normal = new CanonicalLogicManager(new CanonicalPeopleSession(path), new UserPrefs());
        assertEquals(CanonicalCommandDefinition.forSession(false).stream()
                .map(CanonicalCommandDefinition::getWord).toList(), normal.getCommandWords());
        assertThrows(UnsupportedOperationException.class, () -> normal.getCommandWords().clear());
        assertFalse(Files.exists(path));

        Files.writeString(path, "rejected data");
        Logic protectedLogic = new CanonicalLogicManager(new CanonicalPeopleSession(path), new UserPrefs());
        assertEquals(List.of("help", "list", "exit"), protectedLogic.getCommandWords());
        assertEquals("rejected data", Files.readString(path));
    }

    @Test
    public void execute_usesOneCanonicalViewAndPersistsThroughItsSession() throws Exception {
        Path path = directory.resolve("canonical.json");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        UserPrefs prefs = new UserPrefs();
        GuiSettings settings = new GuiSettings(850, 650, 10, 20);
        prefs.setGuiSettings(settings);
        Logic logic = new CanonicalLogicManager(session, prefs);
        var people = logic.getPersonRecords();

        assertSame(session.getPeople(), people);
        assertSame(session.personSearchContextProperty(), logic.personSearchContextProperty());
        assertEquals(CanonicalCommandCatalog.getOverview(Optional.empty()), logic.execute("help").getFeedbackToUser());
        assertEquals("Showing 0 parent(s).", logic.execute("list r/parent").getFeedbackToUser());
        assertFalse(Files.exists(path));
        assertEquals("New student added: Alice Tan (S1).",
                logic.execute("add r/student n/Alice Tan l/P1 pp/91234567").getFeedbackToUser());
        assertSame(people, logic.getPersonRecords());
        assertEquals(List.of(new PersonId("S1")), people.stream().map(person -> person.getId()).toList());
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
        assertEquals(settings, prefs.getGuiSettings());
        assertThrows(UnsupportedOperationException.class, people::clear);
        assertThrows(ParseException.class, () -> logic.execute("delete S1"));
        assertThrows(ParseException.class, () -> logic.execute("find Alice"));
        assertSame(people, logic.getPersonRecords());
        assertEquals(1, people.size());
    }

    @Test
    public void execute_retiredCommands_preservesCanonicalDataViewAndPreferences() throws Exception {
        Path path = directory.resolve("canonical.json");
        new JsonPonHubDataStorage(path).saveData(LessonQueryTestData.createData().exportState());
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        UserPrefs prefs = new UserPrefs();
        GuiSettings settings = new GuiSettings(900, 700, 30, 40);
        prefs.setGuiSettings(settings);
        Logic logic = new CanonicalLogicManager(session, prefs);
        logic.execute("search c/student d/Mon s/Math");
        var people = logic.getPersonRecords();
        List<PersonId> visibleIds = people.stream().map(person -> person.getId()).toList();
        var context = logic.personSearchContextProperty();
        var matchingLessons = context.get();
        assertFalse(matchingLessons.isEmpty());
        var before = session.getDataSnapshot();
        byte[] original = Files.readAllBytes(path);

        for (String command : List.of("find Alex", "clear", "clear 1", "unknown")) {
            ParseException failure = assertThrows(ParseException.class, () -> logic.execute(command));
            assertEquals(String.format(CanonicalPeopleSession.MESSAGE_UNKNOWN_COMMAND_FORMAT,
                    command.split(" ")[0]), failure.getMessage());
            assertSame(before, session.getDataSnapshot());
            assertArrayEquals(original, Files.readAllBytes(path));
            assertSame(people, logic.getPersonRecords());
            assertSame(context, logic.personSearchContextProperty());
            assertSame(matchingLessons, context.get());
            assertEquals(visibleIds, people.stream().map(person -> person.getId()).toList());
            assertEquals(settings, prefs.getGuiSettings());
        }

        assertTrue(logic.execute("help").getFeedbackToUser().contains("help COMMAND"));
        assertTrue(logic.execute("exit").isExit());
        assertArrayEquals(original, Files.readAllBytes(path));
    }

    @Test
    public void lessonRetrieval_delegatesToSharedSessionQueriesWithoutWritingOrChangingPeople() throws Exception {
        Path path = directory.resolve("canonical.json");
        new JsonPonHubDataStorage(path).saveData(LessonQueryTestData.createData().exportState());
        byte[] before = Files.readAllBytes(path);
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        Logic logic = new CanonicalLogicManager(session, new UserPrefs());
        var queries = session.getLessonQueries();
        var people = logic.getPersonRecords();
        logic.execute("list r/parent");
        List<PersonId> visibleIds = people.stream().map(person -> person.getId()).toList();

        assertSame(session.getDisplayedLessons(), logic.getDisplayedLessons());
        assertSame(session.lessonResultContextProperty(), logic.lessonResultContextProperty());
        assertEquals(queries.getLessonList(), logic.getDisplayedLessons());
        assertSame(queries.getLessonList(), logic.getLessonList());
        assertSame(queries.getFilteredLessonList(), logic.getFilteredLessonList());
        assertSame(queries.findLessonById(LessonQueryTestData.SHARED_LESSON).orElseThrow(),
                logic.findLessonById(LessonQueryTestData.SHARED_LESSON).orElseThrow());
        assertSame(queries.getStudentLessons(LessonQueryTestData.STUDENT_ONE),
                logic.getStudentLessons(LessonQueryTestData.STUDENT_ONE));
        assertSame(queries.getLessonRoster(LessonQueryTestData.SHARED_LESSON),
                logic.getLessonRoster(LessonQueryTestData.SHARED_LESSON));
        assertSame(queries.getTutorSchedule(LessonQueryTestData.TUTOR_ONE),
                logic.getTutorSchedule(LessonQueryTestData.TUTOR_ONE));
        assertSame(queries.getAttendanceHistory(LessonQueryTestData.STUDENT_ONE),
                logic.getAttendanceHistory(LessonQueryTestData.STUDENT_ONE));
        assertSame(queries.getAttendanceHistory(LessonQueryTestData.STUDENT_ONE, LessonQueryTestData.SHARED_LESSON),
                logic.getAttendanceHistory(LessonQueryTestData.STUDENT_ONE, LessonQueryTestData.SHARED_LESSON));
        assertSame(session.getDisplayedHistory(), logic.getDisplayedHistory());
        assertSame(session.historyResultContextProperty(), logic.historyResultContextProperty());
        assertSame(session.historyViewActiveProperty(), logic.historyViewActiveProperty());
        assertSame(people, logic.getPersonRecords());
        assertEquals(visibleIds, people.stream().map(person -> person.getId()).toList());
        assertArrayEquals(before, Files.readAllBytes(path));
    }

    @Test
    public void guiSettings_sharePreferencesAndPersistIndependentlyOfRejectedData() throws Exception {
        Path dataPath = directory.resolve("canonical.json");
        Files.writeString(dataPath, "Rejected original bytes");
        byte[] original = Files.readAllBytes(dataPath);
        CanonicalPeopleSession session = new CanonicalPeopleSession(dataPath);
        UserPrefs prefs = new UserPrefs();
        Logic logic = new CanonicalLogicManager(session, prefs);
        GuiSettings first = new GuiSettings(900, 700, 30, 40);
        GuiSettings second = new GuiSettings(1000, 750, 50, 60);

        logic.setGuiSettings(first);
        assertEquals(first, prefs.getGuiSettings());
        prefs.setGuiSettings(second);
        assertEquals(second, logic.getGuiSettings());
        assertEquals(CanonicalCommandCatalog.getOverview(session.getDataLoadError()),
                logic.execute("help").getFeedbackToUser());
        assertThrows(CommandException.class, () -> logic.execute("add"));
        JsonUserPrefsStorage prefsStorage = new JsonUserPrefsStorage(directory.resolve("preferences.json"));
        prefsStorage.saveUserPrefs(prefs);

        assertEquals(prefs, prefsStorage.readUserPrefs().orElseThrow());
        assertArrayEquals(original, Files.readAllBytes(dataPath));
        assertTrue(logic.getPersonRecords().isEmpty());
        assertThrows(NullPointerException.class, () -> logic.setGuiSettings(null));
        assertEquals(second, logic.getGuiSettings());
    }

    @Test
    public void helpAndExit_everyStartupStatus_preserveDataPreferencesAndPeopleWithoutSaving() throws Exception {
        for (String status : List.of("missing", "loaded", "protected")) {
            Path path = directory.resolve(status + ".json");
            if (status.equals("loaded")) {
                new JsonPonHubDataStorage(path).saveData(LessonQueryTestData.createData().exportState());
                Files.writeString(path, Files.readString(path) + "\n  ");
            } else if (status.equals("protected")) {
                Files.writeString(path, "Rejected original bytes");
            }
            byte[] original = Files.exists(path) ? Files.readAllBytes(path) : null;
            CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
                throw new AssertionError("Help/list/exit must not save operational data");
            });
            UserPrefs prefs = new UserPrefs();
            GuiSettings settings = new GuiSettings(900, 700, 30, 40);
            prefs.setGuiSettings(settings);
            Logic logic = new CanonicalLogicManager(session, prefs);
            logic.execute("list r/student");
            var before = session.getDataSnapshot();
            var people = logic.getPersonRecords();
            var visible = List.copyOf(people);
            Locale previous = Locale.getDefault();
            try {
                Locale.setDefault(Locale.forLanguageTag("tr-TR"));
                for (String topic : CanonicalCommandCatalog.getCommandWords(session.getDataLoadError().isPresent())) {
                    var result = logic.execute("help " + topic.toUpperCase(Locale.ROOT));
                    assertEquals(CanonicalCommandCatalog.getUsage(topic, session.getDataLoadError().isPresent())
                            .orElseThrow(), result.getFeedbackToUser());
                    assertFalse(result.isExit());
                }
            } finally {
                Locale.setDefault(previous);
            }
            assertEquals(CanonicalCommandCatalog.getOverview(session.getDataLoadError()),
                    logic.execute("help").getFeedbackToUser());
            for (String invalid : List.of("help unknown", "help list exit", "exit extra")) {
                assertThrows(ParseException.class, () -> logic.execute(invalid));
            }
            var exit = logic.execute("exit");
            assertEquals("Exiting PonHub as requested ...", exit.getFeedbackToUser());
            assertTrue(exit.isExit());
            assertSame(before, session.getDataSnapshot());
            assertSame(people, logic.getPersonRecords());
            assertEquals(visible, people);
            assertEquals(Optional.of(PersonRole.STUDENT), session.getRoleFilter());
            assertEquals(settings, logic.getGuiSettings());
            if (original == null) {
                assertFalse(Files.exists(path));
            } else {
                assertArrayEquals(original, Files.readAllBytes(path));
            }
        }
    }

}
