package seedu.address.model.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.LessonQueryTestData.EMPTY_LESSON;
import static seedu.address.testutil.LessonQueryTestData.SHARED_LESSON;
import static seedu.address.testutil.LessonQueryTestData.STUDENT_ONE;
import static seedu.address.testutil.LessonQueryTestData.STUDENT_TWO;
import static seedu.address.testutil.LessonQueryTestData.STUDENT_WITHOUT_LESSONS;
import static seedu.address.testutil.LessonQueryTestData.TUTOR_ONE;
import static seedu.address.testutil.LessonQueryTestData.TUTOR_TWO;
import static seedu.address.testutil.LessonQueryTestData.TUTOR_WITHOUT_LESSONS;
import static seedu.address.testutil.LessonQueryTestData.createData;
import static seedu.address.testutil.LessonQueryTestData.lesson;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.collections.ListChangeListener;
import seedu.address.logic.CanonicalCommandExecutor;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.SearchCriteriaParser;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.PeopleView;
import seedu.address.model.PonHubData;
import seedu.address.model.PonHubDataState;
import seedu.address.model.attendance.Attendance;
import seedu.address.model.attendance.AttendanceStatus;
import seedu.address.model.lesson.Lesson;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.Name;
import seedu.address.model.person.PeopleRegistryState;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.Student;
import seedu.address.model.person.Tutor;
import seedu.address.model.search.SearchCategory;
import seedu.address.model.search.SearchCriteria;
import seedu.address.model.search.SearchField;

public class LessonQueriesTest {
    private PonHubData data;
    private LessonQueries queries;
    private final SearchCriteriaParser parser = new SearchCriteriaParser();

    @BeforeEach
    public void setUp() {
        data = createData();
        queries = new LessonQueries(data);
    }

    @Test
    public void catalogueAndLookup_includeEmptyAndSharedLessonsInCreationOrder() {
        PonHubDataState before = data.exportState();
        assertIds(queries.getLessonList(), "L2", "L10", "L3", "L4", "L11");
        assertSame(queries.getLessonList(), queries.getLessonList());
        assertSame(queries.getFilteredLessonList(), queries.getFilteredLessonList());
        assertEquals(queries.getLessonList(), queries.getFilteredLessonList());
        LessonView shared = queries.findLessonById(new LessonId("l10")).orElseThrow();
        assertSame(shared, queries.getLessonList().get(1));
        assertEquals(SHARED_LESSON, shared.getLessonId());
        assertEquals(TUTOR_ONE, shared.getTutorId());
        assertEquals("Mei Lim", shared.getTutorName());
        assertEquals("Mon 0900-1000", shared.getTimeSlot().toString());
        assertEquals("Math", shared.getSubject().getValue());
        assertEquals("R1", shared.getRoom().getValue());
        assertEquals(2, shared.getRosterSize());
        assertEquals(0, queries.findLessonById(EMPTY_LESSON).orElseThrow().getRosterSize());
        assertTrue(queries.findLessonById(new LessonId("L999")).isEmpty());
        assertTrue(queries.findLessonById(new LessonId("L9223372036854775807")).isEmpty());
        assertSame(before, data.exportState());
    }

    @Test
    public void roster_resolvesCurrentPeopleInPeopleCreationOrderAndPreservesContacts() {
        var roster = queries.getLessonRoster(SHARED_LESSON);
        assertSame(roster, queries.getLessonRoster(SHARED_LESSON));
        assertEquals(List.of(STUDENT_ONE, STUDENT_TWO), roster.stream().map(StudentView::getStudentId).toList());
        StudentView first = roster.get(0);
        assertEquals("Alex Tan", first.getName());
        assertEquals("P3", first.getEducationLevel().toString());
        assertEquals("00987654", first.getParentPhone());
        assertTrue(first.getPhone().isEmpty());
        assertTrue(first.getEmail().isEmpty());
        assertTrue(first.getAddress().isEmpty());
        StudentView second = roster.get(1);
        assertEquals("Jamie Tan", second.getName());
        assertEquals("S2", second.getEducationLevel().toString());
        assertEquals("00987654", second.getParentPhone());
        assertEquals(Optional.of("00012345"), second.getPhone());
        assertEquals(Optional.of("Jamie.Tan@example.com"), second.getEmail());
        assertEquals(Optional.of("8 Tampines Avenue 2"), second.getAddress());
        assertTrue(queries.getLessonRoster(EMPTY_LESSON).isEmpty());
    }

    @Test
    public void relationships_useStableIdsAndReturnEmptyForExistingUnassignedPeople() {
        assertIds(queries.getStudentLessons(STUDENT_ONE), "L2", "L10");
        assertIds(queries.getStudentLessons(STUDENT_TWO), "L10", "L11");
        assertTrue(queries.getStudentLessons(STUDENT_WITHOUT_LESSONS).isEmpty());
        assertFalse(data.isPersonReferenced(data.getPerson(STUDENT_WITHOUT_LESSONS).orElseThrow()));
        assertIds(queries.getTutorSchedule(TUTOR_ONE), "L10", "L4", "L2");
        // Tutors with the same name keep independent schedules, including their empty lessons.
        assertIds(queries.getTutorSchedule(TUTOR_TWO), "L3", "L11");
        assertTrue(queries.getTutorSchedule(TUTOR_WITHOUT_LESSONS).isEmpty());
        assertSame(queries.getStudentLessons(STUDENT_ONE), queries.getStudentLessons(STUDENT_ONE));
        assertSame(queries.getTutorSchedule(TUTOR_ONE), queries.getTutorSchedule(TUTOR_ONE));
    }

    @Test
    public void invalidIdsAndNulls_rejectWithoutChangingSnapshotOrResults() {
        PonHubDataState before = data.exportState();
        assertThrows(NullPointerException.class, () -> new LessonQueries(null));
        assertThrows(NullPointerException.class, () -> queries.findLessonById(null));
        assertThrows(NullPointerException.class, () -> queries.getLessonRoster(null));
        assertThrows(NullPointerException.class, () -> queries.getStudentLessons(null));
        assertThrows(NullPointerException.class, () -> queries.getTutorSchedule(null));
        assertThrows(IllegalArgumentException.class, () -> queries.getLessonRoster(new LessonId("L999")));
        for (String id : List.of("T1", "P1", "S999")) {
            assertThrows(IllegalArgumentException.class, () -> queries.getStudentLessons(new PersonId(id)), id);
        }
        for (String id : List.of("S1", "P1", "T999")) {
            assertThrows(IllegalArgumentException.class, () -> queries.getTutorSchedule(new PersonId(id)), id);
        }
        for (String id : List.of("", " ", "L0", "L-1", "L1.0", "L01", "L1 junk", "S1",
                "L9223372036854775808", "L１", "L1\n")) {
            assertThrows(IllegalArgumentException.class, () -> new LessonId(id), id);
        }
        Lesson lesson = data.getLesson(SHARED_LESSON).orElseThrow();
        assertThrows(NullPointerException.class, () -> new LessonViewData(null, "Mei"));
        assertThrows(NullPointerException.class, () -> new LessonViewData(lesson, null));
        assertThrows(NullPointerException.class, () -> new StudentViewData(null));
        assertSame(before, data.exportState());
        assertIds(queries.getLessonList(), "L2", "L10", "L3", "L4", "L11");
    }

    @Test
    public void resultCollections_areUnmodifiableAndSearchDoesNotAlterActiveFilter() throws Exception {
        var catalogue = queries.getLessonList();
        var filtered = queries.getFilteredLessonList();
        var student = queries.getStudentLessons(STUDENT_ONE);
        var schedule = queries.getTutorSchedule(TUTOR_ONE);
        var roster = queries.getLessonRoster(SHARED_LESSON);
        for (var list : List.of(catalogue, filtered, student, schedule)) {
            assertThrows(UnsupportedOperationException.class, () -> list.remove(0));
            assertThrows(UnsupportedOperationException.class, () -> list.set(0, catalogue.get(0)));
        }
        assertThrows(UnsupportedOperationException.class, () -> roster.remove(0));
        SearchCriteria active = parser.parse("c/lesson s/math");
        queries.setLessonFilter(active);
        PonHubDataState before = data.exportState();
        var snapshot = queries.searchLessons(parser.parse("c/lesson d/tue"));
        assertIds(snapshot, "L2");
        assertThrows(UnsupportedOperationException.class, () -> snapshot.remove(0));
        assertSame(active, queries.getLessonFilter());
        assertIds(filtered, "L10", "L4");
        assertSame(before, data.exportState());
        queries.setLessonFilter(parser.parse("c/lesson"));
        assertEquals(catalogue, filtered);
    }

    @Test
    public void search_caseInsensitiveFragmentsExactTimesAndSameLessonConditions() throws Exception {
        assertSearch("c/LESSON tu/mEI d/mON s/SCI rm/r2 st/0900 et/1000", "L3");
        assertSearch("c/lesson sn/AL", "L2", "L10");
        assertSearch("c/lesson sn/Tan", "L2", "L10", "L11");
        assertSearch("c/lesson s/文 st/0000 et/2359", "L11");
        assertSearch("c/lesson rm/r1", "L2", "L10", "L4", "L11");
        assertSearch("c/lesson st/1000", "L4");
        assertSearch("c/lesson et/1100", "L4");
        assertSearch("c/lesson d/tue sn/Jam", new String[0]);
        assertSearch("c/lesson d/mon s/Science sn/Alex", new String[0]);
        assertSearch("c/lesson sn/Alex s/中文", new String[0]);
        assertSearch("c/lesson tu/T1", new String[0]);
        assertSearch("c/lesson d/sun", new String[0]);
        assertSearch("c/lesson st/0001", new String[0]);
        assertSearch("c/lesson et/2358", new String[0]);
        assertSearch("  s/MaTh c/lesson d/MON  ", "L10", "L4");
    }

    @Test
    public void search_arbitraryNonblankTextDoesNotCrashOrChangeRecords() throws Exception {
        PonHubDataState before = data.exportState();
        for (String value : List.of("' OR 1=1 --", "<script>alert(1)</script>".replace("/", ""),
                "😀", "\u0000", "é", "ß", "İ", "x".repeat(10000), "999999999999999999999")) {
            for (SearchField field : List.of(SearchField.SUBJECT, SearchField.ROOM,
                    SearchField.TUTOR_NAME, SearchField.STUDENT_NAME)) {
                assertTrue(queries.searchLessons(new SearchCriteria(SearchCategory.LESSON,
                        Map.of(field, value))).isEmpty(), field + " " + value);
            }
        }
        assertSame(before, data.exportState());
    }

    @Test
    public void search_invalidFiltersLeavePreviousFilterAndListIntact() throws Exception {
        SearchCriteria active = parser.parse("c/lesson s/math");
        queries.setLessonFilter(active);
        var visible = queries.getFilteredLessonList();
        PonHubDataState before = data.exportState();
        for (SearchCategory category : List.of(SearchCategory.STUDENT, SearchCategory.TUTOR, SearchCategory.PARENT)) {
            SearchCriteria other = new SearchCriteria(category, Map.of());
            assertThrows(IllegalArgumentException.class, () -> queries.searchLessons(other));
            assertThrows(IllegalArgumentException.class, () -> queries.setLessonFilter(other));
        }
        assertThrows(NullPointerException.class, () -> queries.searchLessons(null));
        assertThrows(NullPointerException.class, () -> queries.setLessonFilter(null));
        for (String args : List.of("", " ", "c/", "c/potato", "s/math", "search c/lesson", "C/lesson",
                "c/lesson c/lesson", "c/lesson n/Alex", "c/lesson p/123", "c/lesson pp/123", "c/lesson e/x",
                "c/lesson a/x", "c/lesson l/S2", "c/lesson sid/S1", "c/lesson x/value", "c/lesson tu/Mei X/value",
                "c/lesson d/Monday", "c/lesson d/8", "c/lesson s/a/b", "c/lesson s/math\nrm/R1",
                "c/lesson s/math\rrm/R1", "c/lesson st/900", "c/lesson st/2400", "c/lesson st/2360",
                "c/lesson st/-100", "c/lesson st/０９００", "c/lesson st/1000 et/0900",
                "c/lesson st/1000 et/1000")) {
            assertThrows(ParseException.class, () -> queries.setLessonFilter(parser.parse(args)), args);
        }
        for (SearchField field : List.of(SearchField.DAY, SearchField.START_TIME, SearchField.END_TIME,
                SearchField.SUBJECT, SearchField.TUTOR_NAME, SearchField.ROOM, SearchField.STUDENT_NAME)) {
            String valid = switch (field) {
                case DAY -> "Mon";
                case START_TIME -> "0900";
                case END_TIME -> "1000";
                default -> "a";
            };
            String prefix = field.getPrefix();
            assertThrows(ParseException.class, () -> parser.parse("c/lesson " + prefix + "   "));
            assertThrows(ParseException.class, () -> parser.parse("c/lesson " + prefix + valid
                    + " " + prefix + valid));
        }
        assertThrows(NullPointerException.class, () -> parser.parse(null));
        assertSame(active, queries.getLessonFilter());
        assertSame(visible, queries.getFilteredLessonList());
        assertIds(visible, "L10", "L4");
        assertSame(before, data.exportState());
    }

    @Test
    public void indexedSearch_matchesIndependentScanAcrossGeneratedCombinationsAndRestores() {
        assertGeneratedSearchesMatchScan();
        PonHubDataState original = data.exportState();
        replaceRoster(Set.of(STUDENT_TWO));
        assertGeneratedSearchesMatchScan();
        data.resetData(original);
        assertGeneratedSearchesMatchScan();
    }

    @Test
    public void committedAddDeleteAndRestore_refreshAlreadyHeldListsAutomatically() throws Exception {
        PonHubDataState original = data.exportState();
        var catalogue = queries.getLessonList();
        var filtered = queries.getFilteredLessonList();
        var schedule = queries.getTutorSchedule(TUTOR_WITHOUT_LESSONS);
        queries.setLessonFilter(parser.parse("c/lesson d/fri s/new"));
        AtomicInteger notifications = new AtomicInteger();
        filtered.addListener((ListChangeListener<LessonView>) change -> notifications.incrementAndGet());
        Lesson added = data.addLesson(TUTOR_WITHOUT_LESSONS,
                lesson("L12", TUTOR_WITHOUT_LESSONS, "Fri", "1200", "1300", "New", "R3", Set.of()).getTimeSlot(),
                new seedu.address.model.lesson.Subject("New"), new seedu.address.model.lesson.Room("R3"));
        var roster = queries.getLessonRoster(added.getId());
        assertIds(filtered, "L12");
        assertIds(schedule, "L12");
        assertEquals(6, catalogue.size());
        assertTrue(roster.isEmpty());
        assertEquals(1, notifications.get());
        // Removing an unreferenced lesson does not discard its allocation counter.
        data.resetData(new PonHubDataState(original.people(), original.lessons(), List.of(), 12));
        assertEquals(12, data.exportState().lastAllocatedLessonSequence());
        assertTrue(filtered.isEmpty());
        assertTrue(schedule.isEmpty());
        assertTrue(roster.isEmpty());
        assertTrue(queries.findLessonById(added.getId()).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> queries.getLessonRoster(added.getId()));
        data.resetData(new PonHubDataState(original.people(), List.of(added), List.of(), 12));
        assertSame(roster, queries.getLessonRoster(added.getId()));
        assertIds(filtered, "L12");
        data.resetData(original);
        assertEquals(original, data.exportState());
        assertTrue(filtered.isEmpty());
        assertSame(catalogue, queries.getLessonList());
        assertSame(filtered, queries.getFilteredLessonList());
    }

    @Test
    public void enrolUnenrolAndAttendance_refreshCurrentMembershipWithoutDerivingItFromHistory() throws Exception {
        var lessons = queries.getStudentLessons(STUDENT_WITHOUT_LESSONS);
        var roster = queries.getLessonRoster(SHARED_LESSON);
        queries.setLessonFilter(parser.parse("c/lesson sn/Alex"));
        replaceRoster(Set.of(STUDENT_ONE, STUDENT_TWO, STUDENT_WITHOUT_LESSONS));
        assertIds(lessons, "L10");
        assertEquals(List.of(STUDENT_ONE, STUDENT_TWO, STUDENT_WITHOUT_LESSONS),
                roster.stream().map(StudentView::getStudentId).toList());
        assertIds(queries.getFilteredLessonList(), "L2", "L10");
        assertEquals(3, queries.findLessonById(SHARED_LESSON).orElseThrow().getRosterSize());
        PonHubDataState enrolled = data.exportState();
        Attendance former = new Attendance(STUDENT_WITHOUT_LESSONS, SHARED_LESSON,
                LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT);
        data.resetData(new PonHubDataState(enrolled.people(), enrolled.lessons(), List.of(former), 11));
        replaceRoster(Set.of(STUDENT_TWO));
        assertTrue(lessons.isEmpty());
        assertEquals(List.of(STUDENT_TWO), roster.stream().map(StudentView::getStudentId).toList());
        assertIds(queries.getFilteredLessonList(), "L2");
        assertEquals(List.of(former), data.getAttendance());
        assertEquals(1, queries.findLessonById(SHARED_LESSON).orElseThrow().getRosterSize());
        PonHubDataState unenrolled = data.exportState();
        data.resetData(new PonHubDataState(unenrolled.people(), unenrolled.lessons(), List.of(), 11));
        assertIds(queries.getFilteredLessonList(), "L2");
        data.resetData(enrolled);
        assertIds(lessons, "L10");
        assertEquals(3, roster.size());
    }

    @Test
    public void peopleChanges_resolveFreshDetailsAndRemovedSubscriptionsRecoverOnRestore() throws Exception {
        PonHubDataState before = data.exportState();
        var roster = queries.getLessonRoster(SHARED_LESSON);
        LessonView oldLesson = queries.findLessonById(SHARED_LESSON).orElseThrow();
        StudentView oldStudent = roster.get(0);
        queries.setLessonFilter(parser.parse("c/lesson tu/Updated sn/Sally"));
        List<PersonRecord> people = new ArrayList<>(before.people().getPeople());
        Tutor tutor = (Tutor) data.getPerson(TUTOR_ONE).orElseThrow();
        Student student = (Student) data.getPerson(STUDENT_ONE).orElseThrow();
        people.set(people.indexOf(tutor), new Tutor(tutor.getId(), renamed(tutor, "Updated Tutor")));
        people.set(people.indexOf(student), new Student(student.getId(), renamed(student, "Sally Tan"),
                student.getLevel(), student.getParentPhone()));
        data.resetData(new PonHubDataState(new PeopleRegistryState(people,
                before.people().getLastAllocatedSequences()), before.lessons(), before.attendance(), 11));
        assertIds(queries.getFilteredLessonList(), "L2", "L10");
        assertEquals("Updated Tutor", queries.findLessonById(SHARED_LESSON).orElseThrow().getTutorName());
        assertEquals("Sally Tan", roster.get(0).getName());
        assertNotSame(oldLesson, queries.findLessonById(SHARED_LESSON).orElseThrow());
        assertEquals("Mei Lim", oldLesson.getTutorName());
        assertEquals("Alex Tan", oldStudent.getName());
        assertSearch("c/lesson sn/al", "L2", "L10");
        var noLessons = queries.getStudentLessons(STUDENT_WITHOUT_LESSONS);
        people.removeIf(person -> person.getId().equals(STUDENT_WITHOUT_LESSONS));
        data.resetData(new PonHubDataState(new PeopleRegistryState(people,
                before.people().getLastAllocatedSequences()), before.lessons(), List.of(), 11));
        assertTrue(noLessons.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> queries.getStudentLessons(STUDENT_WITHOUT_LESSONS));
        data.resetData(before);
        assertSame(noLessons, queries.getStudentLessons(STUDENT_WITHOUT_LESSONS));
        assertTrue(queries.getFilteredLessonList().isEmpty());
        assertEquals("Alex Tan", roster.get(0).getName());
    }

    @Test
    public void failedSaveAndValidation_publishNothingThenSuccessfulCommitRefreshesAllViews() throws Exception {
        PeopleView people = new PeopleView(data);
        PonHubDataState before = data.exportState();
        var catalogue = queries.getLessonList();
        var roster = queries.getLessonRoster(SHARED_LESSON);
        var studentLessons = queries.getStudentLessons(STUDENT_ONE);
        queries.setLessonFilter(parser.parse("c/lesson sn/Alex"));
        var visible = queries.getFilteredLessonList();
        List<LessonView> oldResults = List.copyOf(visible);
        AtomicInteger notifications = new AtomicInteger();
        visible.addListener((ListChangeListener<LessonView>) change -> notifications.incrementAndGet());
        CanonicalCommandExecutor failing = new CanonicalCommandExecutor(people, state -> {
            assertSame(before, data.exportState());
            assertEquals(oldResults, visible);
            throw new IOException("Rejected save");
        }, Optional::empty);
        assertThrows(CommandException.class, () -> failing.execute((staged, stagedView) -> {
            PonHubDataState state = staged.exportState();
            staged.resetData(withRoster(state, Set.of(STUDENT_TWO)));
            return new CommandResult("Unenrolled");
        }));
        assertSame(before, data.exportState());
        assertEquals(0, notifications.get());
        assertEquals(2, roster.size());
        assertIds(studentLessons, "L2", "L10");
        assertThrows(NullPointerException.class, () -> data.resetData((PonHubDataState) null));
        assertSame(before, data.stateProperty().get());
        CanonicalCommandExecutor success = new CanonicalCommandExecutor(people, state -> {
            assertSame(before, data.exportState());
            assertEquals(oldResults, visible);
        }, Optional::empty);
        success.execute((staged, stagedView) -> {
            staged.resetData(withRoster(staged.exportState(), Set.of(STUDENT_TWO)));
            return new CommandResult("Unenrolled");
        });
        assertEquals(1, notifications.get());
        assertIds(visible, "L2");
        assertIds(studentLessons, "L2");
        assertEquals(1, roster.size());
        assertSame(catalogue, queries.getLessonList());
        assertSame(roster, queries.getLessonRoster(SHARED_LESSON));
        data.resetData(new PonHubData(data));
        assertEquals(1, notifications.get());
        data.resetData(before);
        assertIds(visible, "L2", "L10");
        assertEquals(2, roster.size());
    }

    @Test
    public void emptyCatalogueAndCopy_haveIndependentStateAndValidEmptyQueries() throws Exception {
        LessonQueries empty = new LessonQueries(new PonHubData());
        assertTrue(empty.getLessonList().isEmpty());
        assertTrue(empty.searchLessons(parser.parse("c/lesson d/mon")).isEmpty());
        PonHubData copy = new PonHubData(data);
        LessonQueries independent = new LessonQueries(copy);
        var snapshot = queries.searchLessons(parser.parse("c/lesson"));
        data.resetData(new PonHubData());
        assertTrue(queries.getLessonList().isEmpty());
        assertEquals(5, snapshot.size());
        assertEquals(5, independent.getLessonList().size());
        assertSame(copy.exportState(), copy.stateProperty().get());
        assertFalse(data.stateProperty() instanceof javafx.beans.property.ObjectProperty<?>);
    }

    private void assertGeneratedSearchesMatchScan() {
        Map<SearchField, List<String>> pools = new EnumMap<>(SearchField.class);
        pools.put(SearchField.DAY, List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"));
        pools.put(SearchField.START_TIME, List.of("0000", "0900", "1000", "1200"));
        pools.put(SearchField.END_TIME, List.of("1300", "2359"));
        pools.put(SearchField.SUBJECT, List.of("math", "SCi", "文", "a", "nonexistent"));
        pools.put(SearchField.TUTOR_NAME, List.of("MEI", "Lim", "Unassigned", "no tutor"));
        pools.put(SearchField.ROOM, List.of("r", "R1", "r2", "r10", "R99"));
        pools.put(SearchField.STUDENT_NAME, List.of("alex", "TAN", "Jam", "no student"));
        Random random = new Random(80);
        for (int i = 0; i < 2500; i++) {
            Map<SearchField, String> values = new EnumMap<>(SearchField.class);
            pools.forEach((field, pool) -> {
                if (random.nextBoolean()) {
                    values.put(field, pool.get(random.nextInt(pool.size())));
                }
            });
            SearchCriteria criteria = new SearchCriteria(SearchCategory.LESSON, values);
            List<LessonId> expected = data.getLessons().stream().filter(lesson -> scanMatches(criteria, lesson))
                    .map(Lesson::getId).toList();
            assertEquals(expected, ids(queries.searchLessons(criteria)), criteria.toString());
        }
    }

    /** Independent specification: scan raw records, resolve people, compare exact values or folded substrings. */
    private boolean scanMatches(SearchCriteria criteria, Lesson lesson) {
        for (var entry : criteria.getFilters().entrySet()) {
            String query = entry.getValue();
            String candidate = switch (entry.getKey()) {
                case DAY -> lesson.getTimeSlot().getDay().toString().toLowerCase(Locale.ROOT);
                case START_TIME -> lesson.getTimeSlot().getStartTime().toString();
                case END_TIME -> lesson.getTimeSlot().getEndTime().toString();
                case SUBJECT -> lesson.getSubject().getValue().toLowerCase(Locale.ROOT);
                case ROOM -> lesson.getRoom().getValue().toLowerCase(Locale.ROOT);
                case TUTOR_NAME -> data.getPerson(lesson.getTutorId()).orElseThrow().getName()
                        .fullName.toLowerCase(Locale.ROOT);
                case STUDENT_NAME -> "";
                default -> throw new AssertionError("Unsupported fixture field");
            };
            boolean matches = switch (entry.getKey()) {
                case DAY, START_TIME, END_TIME -> candidate.equals(query);
                case STUDENT_NAME -> lesson.getEnrolledStudentIds().stream().anyMatch(id -> data.getPerson(id)
                        .orElseThrow().getName().fullName.toLowerCase(Locale.ROOT).contains(query));
                default -> candidate.contains(query);
            };
            if (!matches) {
                return false;
            }
        }
        return true;
    }

    private static ContactDetails renamed(PersonRecord person, String name) {
        ContactDetails details = person.getContactDetails();
        return new ContactDetails(new Name(name), details.getPhone(), details.getEmail(), details.getAddress());
    }

    private void replaceRoster(Set<PersonId> students) {
        data.resetData(withRoster(data.exportState(), students));
    }

    private static PonHubDataState withRoster(PonHubDataState state, Set<PersonId> students) {
        List<Lesson> lessons = state.lessons().stream()
                .map(lesson -> lesson.getId().equals(SHARED_LESSON) ? lesson.withEnrolledStudentIds(students) : lesson)
                .toList();
        return new PonHubDataState(state.people(), lessons, state.attendance(), state.lastAllocatedLessonSequence());
    }

    private void assertSearch(String arguments, String... expectedIds) throws ParseException {
        assertIds(queries.searchLessons(parser.parse(arguments)), expectedIds);
    }

    private static List<LessonId> ids(List<LessonView> lessons) {
        return lessons.stream().map(LessonView::getLessonId).toList();
    }

    private static void assertIds(List<LessonView> lessons, String... expectedIds) {
        assertEquals(List.of(expectedIds).stream().map(LessonId::new).toList(), ids(lessons));
    }
}
