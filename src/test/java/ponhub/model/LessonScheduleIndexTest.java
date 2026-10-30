package ponhub.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ponhub.testutil.LessonQueryTestData.lesson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ponhub.model.lesson.Lesson;
import ponhub.model.person.PeopleRegistryState;
import ponhub.model.person.PersonId;
import ponhub.testutil.PeopleSampleDataUtil;

public class LessonScheduleIndexTest {
    private static final PersonId FIRST_TUTOR = new PersonId("T1");
    private static final PersonId SECOND_TUTOR = new PersonId("T2");
    private final PeopleRegistryState people = PeopleSampleDataUtil.getSamplePeopleRegistry().exportState();

    @Test
    public void restore_spanningSlot_reportsFirstCreatedConflictRatherThanFirstChronological() {
        List<Lesson> lessons = List.of(
                lesson("L1", FIRST_TUTOR, "Mon", "1100", "1200", "Math", "R1", Set.of()),
                lesson("L2", FIRST_TUTOR, "Mon", "0900", "1000", "Math", "R1", Set.of()),
                lesson("L3", FIRST_TUTOR, "Mon", "1000", "1100", "Math", "R1", Set.of()),
                lesson("L4", FIRST_TUTOR, "Mon", "0830", "1230", "Math", "R2", Set.of()));
        assertEquals("Tutor schedule overlap between L1 and L4.", rejection(lessons));
    }

    @Test
    public void restore_multipleResourceConflicts_preservesPairBeforeTypePrecedence() {
        List<Lesson> lessons = List.of(
                lesson("L1", FIRST_TUTOR, "Mon", "0900", "1000", "Math", "R1", Set.of()),
                lesson("L2", SECOND_TUTOR, "Mon", "0900", "1000", "Math", "R2", Set.of()),
                lesson("L3", SECOND_TUTOR, "Mon", "0900", "1000", "Math", "r1", Set.of()));
        assertEquals("Room schedule overlap between L1 and L3.", rejection(lessons));
    }

    @Test
    public void restore_disjointResourcesAdjacentSlotsAndWeekdays_preservesCatalogueOrder() {
        List<Lesson> lessons = List.of(
                lesson("L4", FIRST_TUTOR, "Tue", "0900", "1000", "Math", "R1", Set.of(new PersonId("S1"))),
                lesson("L2", FIRST_TUTOR, "Mon", "1000", "1100", "Math", "R1", Set.of(new PersonId("S1"))),
                lesson("L1", FIRST_TUTOR, "Mon", "0900", "1000", "Math", "R1", Set.of(new PersonId("S1"))),
                lesson("L3", SECOND_TUTOR, "Mon", "0900", "1100", "Math", "R2", Set.of(new PersonId("S2"))));
        assertEquals(lessons, new PonHubDataState(people, lessons, List.of(), 4).lessons());
    }

    @Test
    public void restore_seededCatalogues_matchPairwiseOracleIncludingExactDiagnostic() {
        Random random = new Random(586);
        Set<String> outcomes = new HashSet<>();
        for (int trial = 0; trial < 4_000; trial++) {
            List<Lesson> lessons = new ArrayList<>();
            int count = 1 + random.nextInt(10);
            for (int i = 0; i < count; i++) {
                int start = 480 + 30 * random.nextInt(10);
                int end = start + 30 * (1 + random.nextInt(4));
                List<PersonId> students = new ArrayList<>(List.of(
                        new PersonId("S1"), new PersonId("S2"), new PersonId("S3")));
                Collections.shuffle(students, random);
                Set<PersonId> roster = new LinkedHashSet<>();
                students.stream().filter(unused -> random.nextBoolean()).forEach(roster::add);
                PersonId tutor = random.nextBoolean() ? FIRST_TUTOR : SECOND_TUTOR;
                String day = random.nextBoolean() ? "Mon" : "Tue";
                String room = (random.nextBoolean() ? "R" : "r") + random.nextInt(4);
                lessons.add(lesson("L" + (i + 1), tutor, day, time(start), time(end), "Math", room, roster));
            }
            Collections.shuffle(lessons, random);
            List<Lesson> original = List.copyOf(lessons);
            String expected = pairwiseDiagnostic(lessons);
            if (expected == null) {
                outcomes.add("valid");
                assertEquals(original, new PonHubDataState(people, lessons, List.of(), count).lessons());
            } else {
                outcomes.add(expected.split(" ")[0]);
                assertEquals(expected, rejection(lessons), "trial=" + trial);
            }
            assertEquals(original, lessons);
        }
        assertEquals(Set.of("valid", "Tutor", "Room", "Student"), outcomes);
    }

    private String rejection(List<Lesson> lessons) {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () ->
                new PonHubDataState(people, lessons, List.of(), lessons.size()));
        assertTrue(failure.getMessage().contains("schedule overlap"));
        return failure.getMessage();
    }

    private static String time(int minutes) {
        return String.format(java.util.Locale.ROOT, "%02d%02d", minutes / 60, minutes % 60);
    }

    /**
     * Independent brute-force reference preserves the previous pair/type/roster diagnostic ordering.
     */
    private static String pairwiseDiagnostic(List<Lesson> lessons) {
        for (int i = 0; i < lessons.size(); i++) {
            Lesson current = lessons.get(i);
            for (int j = 0; j < i; j++) {
                Lesson earlier = lessons.get(j);
                if (!current.getTimeSlot().overlaps(earlier.getTimeSlot())) {
                    continue;
                }
                String pair = " schedule overlap between " + earlier.getId() + " and " + current.getId() + ".";
                if (current.getTutorId().equals(earlier.getTutorId())) {
                    return "Tutor" + pair;
                }
                if (current.getRoom().equals(earlier.getRoom())) {
                    return "Room" + pair;
                }
                for (PersonId student : current.getEnrolledStudentIds()) {
                    if (earlier.hasEnrolledStudent(student)) {
                        return "Student " + student + pair;
                    }
                }
            }
        }
        return null;
    }
}
