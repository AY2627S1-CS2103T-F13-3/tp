package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.core.GuiSettings;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.PersonId;
import seedu.address.model.query.LessonQueries;
import seedu.address.model.query.LessonQuerySource;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonPonHubDataStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.LessonQueryTestData;

/**
 * Verifies the application's Logic boundary shares the canonical session and independent preferences.
 */
public class CanonicalLogicManagerTest {
    @TempDir
    Path directory;

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
        assertThrows(UnsupportedOperationException.class, logic::getFilteredPersonList);
        assertThrows(ParseException.class, () -> logic.execute("delete 1"));
        assertThrows(ParseException.class, () -> logic.execute("find Alice"));
        assertSame(people, logic.getPersonRecords());
        assertEquals(1, people.size());
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
        assertThrows(UnsupportedOperationException.class, () ->
                logic.getAttendanceHistory(LessonQueryTestData.STUDENT_ONE));
        assertThrows(UnsupportedOperationException.class, () ->
                logic.getAttendanceHistory(LessonQueryTestData.STUDENT_ONE, LessonQueryTestData.SHARED_LESSON));
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
    public void inheritedLogic_exposesOnlyItsRetainedViewAndRejectsCanonicalPeopleAccess() {
        Logic legacy = new LogicManager(new ModelManager(), new StorageManager(
                new JsonAddressBookStorage(directory.resolve("legacy.json")),
                new JsonUserPrefsStorage(directory.resolve("preferences.json"))));

        assertTrue(legacy.getFilteredPersonList().isEmpty());
        assertThrows(UnsupportedOperationException.class, legacy::getPersonRecords);
        assertThrows(UnsupportedOperationException.class, legacy::getDisplayedLessons);
        assertThrows(UnsupportedOperationException.class, legacy::lessonResultContextProperty);
        assertFalse(Files.exists(directory.resolve("legacy.json")));
    }

    @Test
    public void inheritedLogic_queryCapableTestAdapterReturnsItsFilteredView() {
        LessonQueries queries = new LessonQueries(LessonQueryTestData.createData());
        ModelManager model = new ModelManager() {
            @Override
            public LessonQuerySource getLessonQueries() {
                return queries;
            }
        };
        Logic legacy = new LogicManager(model, new StorageManager(
                new JsonAddressBookStorage(directory.resolve("legacy.json")),
                new JsonUserPrefsStorage(directory.resolve("preferences.json"))));
        assertSame(queries.getFilteredLessonList(), legacy.getDisplayedLessons());
        assertFalse(Files.exists(directory.resolve("legacy.json")));
    }
}
