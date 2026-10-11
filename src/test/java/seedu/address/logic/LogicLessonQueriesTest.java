package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.LessonQueryTestData.SHARED_LESSON;
import static seedu.address.testutil.LessonQueryTestData.STUDENT_ONE;
import static seedu.address.testutil.LessonQueryTestData.TUTOR_ONE;
import static seedu.address.testutil.LessonQueryTestData.createData;
import static seedu.address.testutil.TypicalPersons.AMY;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.parser.SearchCriteriaParser;
import seedu.address.model.ModelManager;
import seedu.address.model.PonHubData;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.query.LessonQueries;
import seedu.address.model.query.LessonQuerySource;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

public class LogicLessonQueriesTest {
    @TempDir
    public Path temporaryFolder;

    @Test
    public void canonicalAdapter_delegatesReadOnlyResultsWithoutSavingOrChangingPeople() throws Exception {
        PonHubData data = createData();
        LessonQueries queries = new LessonQueries(data);
        // Prepared adapter contract only: production ModelManager still has no canonical root.
        ModelManager adapter = new ModelManager() {
            @Override
            public LessonQuerySource getLessonQueries() {
                return queries;
            }
        };
        adapter.addPerson(AMY);
        adapter.updateFilteredPersonList(person -> false);
        var people = adapter.getFilteredPersonList();
        Path dataFile = temporaryFolder.resolve("people.json");
        Logic logic = new LogicManager(adapter, new StorageManager(new JsonAddressBookStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
        var before = data.exportState();
        assertSame(queries.getLessonList(), logic.getLessonList());
        assertSame(queries.getFilteredLessonList(), logic.getFilteredLessonList());
        assertSame(queries.getStudentLessons(STUDENT_ONE), logic.getStudentLessons(STUDENT_ONE));
        assertSame(queries.getLessonRoster(SHARED_LESSON), logic.getLessonRoster(SHARED_LESSON));
        assertSame(queries.getTutorSchedule(TUTOR_ONE), logic.getTutorSchedule(TUTOR_ONE));
        assertSame(queries.findLessonById(SHARED_LESSON).orElseThrow(),
                logic.findLessonById(SHARED_LESSON).orElseThrow());
        queries.setLessonFilter(new SearchCriteriaParser().parse("c/lesson s/math"));
        assertEquals(2, logic.getFilteredLessonList().size());
        assertTrue(logic.findLessonById(new LessonId("L999")).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> logic.getLessonRoster(new LessonId("L999")));
        assertSame(people, logic.getFilteredPersonList());
        assertTrue(people.isEmpty());
        assertEquals(List.of(AMY), adapter.getAddressBook().getPersonList());
        assertSame(before, data.exportState());
        assertFalse(Files.exists(dataFile));
        var held = logic.getLessonList();
        data.resetData(new PonHubData());
        assertSame(held, logic.getLessonList());
        assertTrue(held.isEmpty());
        assertTrue(people.isEmpty());
    }
}
