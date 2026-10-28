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
 * Tests same-named Tutor selection through the active saved lesson-search route.
 */
public class LessonTutorSearchIntegrationTest {
    @TempDir
    Path directory;

    @Test
    public void search_exactTutorPhoneCombinesWithPartialNameAndOtherLessonFilters() throws Exception {
        CanonicalPeopleSession session = fixture();
        session.execute("search c/lesson tu/mEI");
        assertIds(session, "L1", "L2", "L3");
        session.execute("search c/lesson tu/mEI tp/0012345 d/Mon");
        assertIds(session, "L1", "L2");
        session.execute("search c/lesson tp/93456789");
        assertIds(session, "L3");
        session.execute("search c/lesson tu/Other tp/0012345");
        assertIds(session);
        session.execute("search c/lesson tp/12345");
        assertIds(session);
        session.execute("search c/lesson tp/0012345 s/Science");
        assertIds(session, "L2");
    }

    @Test
    public void search_invalidPhoneOrCategoryPreservesCurrentResultsPeopleAndSavedBytes() throws Exception {
        CanonicalPeopleSession session = fixture();
        session.execute("search c/tutor n/Mei");
        var people = List.copyOf(session.getPeople());
        byte[] bytes = Files.readAllBytes(directory.resolve("people.json"));
        session.execute("search c/lesson tp/0012345");
        String context = session.lessonResultContextProperty().get();
        for (String args : List.of("c/lesson tp/12", "c/lesson tp/+123", "c/lesson tp/",
                "c/lesson tp/0012345 tp/0012345", "c/student tp/0012345", "c/tutor tp/0012345")) {
            assertThrows(ParseException.class, () -> session.execute("search " + args), args);
            assertIds(session, "L1", "L2");
            assertEquals(context, session.lessonResultContextProperty().get());
        }
        assertEquals(people, session.getPeople());
        assertArrayEquals(bytes, Files.readAllBytes(directory.resolve("people.json")));
    }

    @Test
    public void activeSearch_refreshesAfterTutorPhoneEditAndReloadUsesCurrentPhone() throws Exception {
        CanonicalPeopleSession session = fixture();
        session.execute("search c/lesson tp/0012345");
        session.execute("list r/tutor");
        session.execute("edit 1 p/0098765");
        assertIds(session);
        session.execute("search c/lesson tu/mei tp/0098765");
        assertIds(session, "L1", "L2");
        CanonicalPeopleSession reloaded = new CanonicalPeopleSession(directory.resolve("people.json"));
        reloaded.execute("search c/lesson tu/mei tp/0098765");
        assertIds(reloaded, "L1", "L2");
    }

    private CanonicalPeopleSession fixture() throws Exception {
        CanonicalPeopleSession session = new CanonicalPeopleSession(directory.resolve("people.json"));
        session.execute("add r/tutor n/Mei Lim p/0012345");
        session.execute("add r/tutor n/Mei Lim p/93456789");
        session.execute("addlesson d/Mon st/1600 et/1700 s/Math tu/Mei Lim tp/0012345 rm/R1");
        session.execute("addlesson d/Mon st/1700 et/1800 s/Science tu/Mei Lim tp/0012345 rm/R1");
        session.execute("addlesson d/Tue st/1600 et/1700 s/History tu/Mei Lim tp/93456789 rm/R2");
        return session;
    }

    private void assertIds(CanonicalPeopleSession session, String... ids) {
        assertEquals(List.of(ids), session.getDisplayedLessons().stream()
                .map(lesson -> lesson.getLessonId().toString()).toList());
    }
}
