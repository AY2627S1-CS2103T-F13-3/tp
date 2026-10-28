package ponhub.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ponhub.testutil.LessonQueryTestData.createData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ponhub.logic.parser.exceptions.ParseException;
import ponhub.storage.JsonPonHubDataStorage;

/**
 * Exercises optional schedule ordering without rewriting canonical order or people positions.
 */
public class LessonOrderIntegrationTest {
    @TempDir
    Path directory;

    @Test
    public void lessonsAndSearch_orderByWeekdayTimeAndNumericIdAndRestoreCreationOrder() throws Exception {
        CanonicalPeopleSession session = fixture();
        var displayed = session.getDisplayedLessons();
        var state = session.getDataSnapshot();
        session.execute("list r/student");
        var people = List.copyOf(session.getPeople());
        byte[] bytes = Files.readAllBytes(directory.resolve("data.json"));
        session.execute("lessons sort/schedule");
        assertIds(session, "L3", "L10", "L4", "L2", "L11");
        session.execute("search c/lesson s/Science sort/schedule");
        assertIds(session, "L3", "L2");
        session.execute("lessons sort/creation");
        assertIds(session, "L2", "L10", "L3", "L4", "L11");
        session.execute("lessons 1 sort/schedule");
        assertIds(session, "L10", "L2");
        session.execute("lessons 1");
        assertIds(session, "L2", "L10");
        assertSame(displayed, session.getDisplayedLessons());
        assertEquals(people, session.getPeople());
        assertSame(state, session.getDataSnapshot());
        assertArrayEquals(bytes, Files.readAllBytes(directory.resolve("data.json")));
    }

    @Test
    public void search_scheduleOrderComposesWithTutorPhoneAndOverlap() throws Exception {
        CanonicalPeopleSession session = fixture();
        byte[] bytes = Files.readAllBytes(directory.resolve("data.json"));
        session.execute("search c/lesson tp/00112233 from/0900 to/1000 sort/schedule");
        assertIds(session, "L10", "L2");
        session.execute("search c/lesson tp/00112233 from/0900 to/1000 sort/creation");
        assertIds(session, "L2", "L10");
        session.execute("search c/lesson tp/00112233 from/1000 to/1100 sort/schedule");
        assertIds(session, "L4");
        assertArrayEquals(bytes, Files.readAllBytes(directory.resolve("data.json")));
    }

    @Test
    public void scheduleOrder_usesMondayFirstWeekdaysRatherThanAlphabeticalNames() throws Exception {
        CanonicalPeopleSession session = fixture();
        session.execute("addlesson d/Fri st/0900 et/1000 s/English tu/Mei Lim tp/00112233 rm/R1");
        session.execute("addlesson d/Sun st/0900 et/1000 s/English tu/Mei Lim tp/00112233 rm/R1");
        session.execute("lessons sort/schedule");
        assertIds(session, "L3", "L10", "L4", "L2", "L11", "L12", "L13");
    }

    @Test
    public void activeOrder_refreshesAfterSavedEditsAndNewRequestsResetToCreation() throws Exception {
        CanonicalPeopleSession session = fixture();
        session.execute("lessons sort/schedule");
        session.execute("editlesson lid/L2 d/Mon st/0800 et/0900");
        assertIds(session, "L2", "L3", "L10", "L4", "L11");
        session.execute("search c/lesson d/Mon sort/schedule");
        assertIds(session, "L2", "L3", "L10", "L4");
        session.execute("editlesson lid/L2 d/Tue");
        assertIds(session, "L3", "L10", "L4");
        session.execute("search c/lesson d/Mon");
        assertIds(session, "L10", "L3", "L4");
        CanonicalPeopleSession reloaded = new CanonicalPeopleSession(directory.resolve("data.json"));
        assertIds(reloaded, "L2", "L10", "L3", "L4", "L11");
    }

    @Test
    public void invalidOrder_preservesExistingResultAndContext() throws Exception {
        CanonicalPeopleSession session = fixture();
        session.execute("lessons sort/schedule");
        String context = session.lessonResultContextProperty().get();
        for (String command : List.of("lessons sort/name", "lessons sort/schedule sort/creation",
                "search c/lesson sort/name", "search c/student sort/schedule")) {
            assertThrows(ParseException.class, () -> session.execute(command), command);
            assertIds(session, "L3", "L10", "L4", "L2", "L11");
            assertEquals(context, session.lessonResultContextProperty().get());
        }
    }

    private CanonicalPeopleSession fixture() throws Exception {
        Path path = directory.resolve("data.json");
        new JsonPonHubDataStorage(path).saveData(createData().exportState());
        return new CanonicalPeopleSession(path);
    }

    private void assertIds(CanonicalPeopleSession session, String... ids) {
        assertEquals(List.of(ids), session.getDisplayedLessons().stream()
                .map(lesson -> lesson.getLessonId().toString()).toList());
    }
}
