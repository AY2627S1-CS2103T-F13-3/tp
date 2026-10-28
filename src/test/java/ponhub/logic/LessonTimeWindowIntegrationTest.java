package ponhub.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ponhub.logic.parser.exceptions.ParseException;

/**
 * Tests same-day positive overlap while keeping exact-time filters compatible.
 */
public class LessonTimeWindowIntegrationTest {
    @TempDir
    Path directory;

    @Test
    public void search_overlapHandlesContainedContainingAndTouchingIntervals() throws Exception {
        CanonicalPeopleSession session = fixture();
        session.execute("search c/lesson d/Mon from/1630 to/1730");
        assertIds(session, "L1", "L2");
        session.execute("search c/lesson d/Mon from/1700 to/1800");
        assertIds(session, "L2");
        session.execute("search c/lesson from/1600 to/1700");
        assertIds(session, "L1", "L3");
        session.execute("search c/lesson d/Mon from/0000 to/2359");
        assertIds(session, "L1", "L2");
        session.execute("search c/lesson d/Mon from/1615 to/1645");
        assertIds(session, "L1");
        session.execute("search c/lesson d/Mon from/1800 to/1900");
        assertIds(session);
    }

    @Test
    public void search_otherFiltersRemainConjunctiveAndExactTimeBehaviorIsUnchanged() throws Exception {
        CanonicalPeopleSession session = fixture();
        session.execute("search c/lesson d/Mon from/1630 to/1730 s/Science tu/Mei rm/R1");
        assertIds(session, "L2");
        session.execute("search c/lesson d/Mon from/1630 to/1730 st/1600 et/1700");
        assertIds(session, "L1");
        session.execute("search c/lesson d/Mon st/1600 et/1800");
        assertIds(session);
        session.execute("search c/lesson d/Mon st/1600");
        assertIds(session, "L1");
        session.execute("search c/lesson d/Mon et/1800");
        assertIds(session, "L2");
    }

    @Test
    public void search_invalidWindowsRetainViewsContextAndStoredBytes() throws Exception {
        CanonicalPeopleSession session = fixture();
        session.execute("search c/tutor n/Mei");
        var people = List.copyOf(session.getPeople());
        session.execute("search c/lesson d/Mon from/1630 to/1730");
        String context = session.lessonResultContextProperty().get();
        byte[] bytes = Files.readAllBytes(directory.resolve("data.json"));
        for (String args : List.of("c/lesson from/1600", "c/lesson to/1800", "c/lesson from/1700 to/1700",
                "c/lesson from/1800 to/1600", "c/lesson from/2400 to/1800", "c/lesson from/1600 to/",
                "c/lesson from/1600 from/1600 to/1800", "c/student from/1600 to/1800",
                "c/lesson from/16:00 to/1800")) {
            assertThrows(ParseException.class, () -> session.execute("search " + args), args);
            assertIds(session, "L1", "L2");
            assertEquals(context, session.lessonResultContextProperty().get());
        }
        assertEquals(people, session.getPeople());
        assertArrayEquals(bytes, Files.readAllBytes(directory.resolve("data.json")));
    }

    @Test
    public void activeWindow_refreshesAfterEditAndReloadRetainsOperationalData() throws Exception {
        CanonicalPeopleSession session = fixture();
        session.execute("search c/lesson d/Mon from/1630 to/1730");
        session.execute("editlesson lid/L2 st/1800 et/1900");
        assertIds(session, "L1");
        CanonicalPeopleSession reloaded = new CanonicalPeopleSession(directory.resolve("data.json"));
        reloaded.execute("search c/lesson d/Mon from/1630 to/1730");
        assertIds(reloaded, "L1");
        assertEquals(session.getDataSnapshot(), reloaded.getDataSnapshot());
    }

    private CanonicalPeopleSession fixture() throws Exception {
        CanonicalPeopleSession session = new CanonicalPeopleSession(directory.resolve("data.json"));
        session.execute("add r/tutor n/Mei Lim p/92345678");
        session.execute("addlesson d/Mon st/1600 et/1700 s/Math tu/Mei Lim rm/R1");
        session.execute("addlesson d/Mon st/1700 et/1800 s/Science tu/Mei Lim rm/R1");
        session.execute("addlesson d/Tue st/1600 et/1700 s/History tu/Mei Lim rm/R1");
        return session;
    }

    private void assertIds(CanonicalPeopleSession session, String... ids) {
        assertEquals(List.of(ids), session.getDisplayedLessons().stream()
                .map(lesson -> lesson.getLessonId().toString()).toList());
    }
}
