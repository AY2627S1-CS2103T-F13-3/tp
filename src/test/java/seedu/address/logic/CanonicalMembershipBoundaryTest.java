package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.LessonMembershipParser;
import seedu.address.logic.parser.ParserUtil;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.PonHubDataState;
import seedu.address.model.UserPrefs;
import seedu.address.model.attendance.Attendance;
import seedu.address.model.attendance.AttendanceStatus;
import seedu.address.model.lesson.Lesson;
import seedu.address.model.lesson.LessonDay;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.lesson.LessonTime;
import seedu.address.model.lesson.LessonTimeSlot;
import seedu.address.model.lesson.Room;
import seedu.address.model.lesson.Subject;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Name;
import seedu.address.model.person.Parent;
import seedu.address.model.person.PeopleRegistryState;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Student;
import seedu.address.model.person.Tutor;
import seedu.address.model.query.LessonView;
import seedu.address.model.query.StudentView;
import seedu.address.storage.JsonPonHubDataStorage;
import seedu.address.storage.ProtectedPonHubDataStorage;

/**
 * Protects membership selectors and transactions through the active application Logic boundary.
 */
public class CanonicalMembershipBoundaryTest {
    private static final PersonId SELECTED_STUDENT = new PersonId("S3");
    private static final PersonId OTHER_STUDENT = new PersonId("S9");
    private static final PersonId TUTOR = new PersonId("T7");
    private static final LessonId LESSON = new LessonId("L10");
    private static final Attendance RETAINED_ATTENDANCE = new Attendance(SELECTED_STUDENT, LESSON,
            LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT);

    @TempDir
    Path directory;

    @Test
    public void execute_narrowedSearchEnrolsStableStudentAndRetainsQuery() throws Exception {
        Fixture fixture = new Fixture("enrol.json", false, false);
        fixture.narrowToBob();

        assertEquals("Student S3 enrolled in lesson L10.",
                fixture.logic.execute("enrol 1 lid/l10").getFeedbackToUser());

        fixture.assertMembership(true);
        assertEquals(1, fixture.writes);
        fixture.assertReloaded();
        fixture.assertSearchStillApplies();
    }

    @Test
    public void execute_saveFailureInEitherDirectionPreservesStateAndRetryCommitsOnce() throws Exception {
        for (boolean isUnenrol : new boolean[]{false, true}) {
            Fixture fixture = new Fixture("retry-" + isUnenrol + ".json", isUnenrol, true);
            fixture.narrowToBob();
            String command = (isUnenrol ? "unenrol" : "enrol") + " 1 lid/L10";
            PonHubDataState before = fixture.session.getDataSnapshot();
            byte[] bytes = Files.readAllBytes(fixture.path);

            CommandException failure = assertThrows(CommandException.class, () -> fixture.logic.execute(command));

            assertEquals(String.format(CanonicalCommandExecutor.FILE_OPS_ERROR_FORMAT,
                    fixture.saveFailure.getMessage()),
                    failure.getMessage());
            assertSame(fixture.saveFailure, failure.getCause());
            fixture.assertUnchanged(before, bytes);
            assertEquals(1, fixture.writes);

            assertEquals(isUnenrol ? "Student S3 unenrolled from lesson L10." : "Student S3 enrolled in lesson L10.",
                    fixture.logic.execute(command).getFeedbackToUser());
            fixture.assertMembership(!isUnenrol);
            assertEquals(2, fixture.writes);
            assertEquals(before.attendance(), fixture.session.getDataSnapshot().attendance());
            fixture.assertReloaded();
            fixture.assertSearchStillApplies();
        }
    }

    @Test
    public void execute_invalidUnenrolSelectionsPreserveFullStateAndDisk() throws Exception {
        Fixture fixture = new Fixture("invalid-selections.json", true, false);
        String[][] cases = {
            {"search c/tutor n/Mei", "unenrol 1 lid/L10", PersonIndexResolver.MESSAGE_NOT_STUDENT},
            {"search c/parent n/Pat", "unenrol 1 lid/L10", PersonIndexResolver.MESSAGE_NOT_STUDENT},
            {"search c/student n/Bob", "unenrol 2 lid/L10", Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX},
            {"search c/student n/Missing", "unenrol 1 lid/L10", Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX},
            {"search c/student n/Bob", "unenrol 1 lid/L99", "The lesson does not exist: L99"},
            {"search c/student n/Alice", "unenrol 1 lid/L10", "Student S9 is not enrolled in lesson L10."},
            {"search c/student n/Bob", "enrol 1 lid/L10", "Student S3 is already enrolled in lesson L10."}
        };
        for (String[] testCase : cases) {
            fixture.logic.execute(testCase[0]);
            fixture.resetNotifications();
            PonHubDataState before = fixture.session.getDataSnapshot();
            byte[] bytes = Files.readAllBytes(fixture.path);
            List<PersonRecord> visible = List.copyOf(fixture.people);
            Optional<PersonRole> role = fixture.session.getRoleFilter();

            CommandException failure = assertThrows(CommandException.class, () -> fixture.logic.execute(
                    testCase[1]));

            assertEquals(testCase[2], failure.getMessage());
            fixture.assertUnchanged(before, bytes);
            assertEquals(visible, fixture.people);
            assertEquals(role, fixture.session.getRoleFilter());
            assertEquals(0, fixture.writes);
        }
        fixture.narrowToBob();
        fixture.assertSearchStillApplies();
    }

    @Test
    public void execute_malformedMembershipRoutesRejectBeforeSavingOrChangingSearch() throws Exception {
        Fixture fixture = new Fixture("parse-rejections.json", true, false);
        fixture.narrowToBob();
        PonHubDataState before = fixture.session.getDataSnapshot();
        byte[] bytes = Files.readAllBytes(fixture.path);
        String[][] arguments = {
            {"S3 lid/L10", LessonMembershipParser.MESSAGE_ARGUMENTS},
            {"0 lid/L10", ParserUtil.MESSAGE_INVALID_INDEX},
            {"1 lid/L01", LessonId.MESSAGE_CONSTRAINTS},
            {"1 lid/L10 lid/L11", LessonMembershipParser.MESSAGE_ARGUMENTS},
            {"1 lid/L10\n", "Commands must be on one line."}
        };
        for (String word : new String[]{"enrol", "unenrol"}) {
            for (String[] testCase : arguments) {
                ParseException failure = assertThrows(ParseException.class, () -> fixture.logic.execute(
                        word + " " + testCase[0]));
                assertEquals(testCase[1], failure.getMessage());
                fixture.assertUnchanged(before, bytes);
                fixture.assertNarrowedView();
                assertEquals(0, fixture.writes);
            }
        }
        fixture.assertSearchStillApplies();
    }

    private static PonHubDataState state(boolean isEnrolled) {
        Tutor tutor = new Tutor(TUTOR, contacts("Mei Lim", "00123456"));
        Student other = student(OTHER_STUDENT, "Alice Student");
        Parent parent = new Parent(new PersonId("P2"), contacts("Pat Parent", "60000002"));
        Student selected = student(SELECTED_STUDENT, "Bob Student");
        PeopleRegistryState people = new PeopleRegistryState(List.of(tutor, other, parent, selected),
                Map.of(PersonRole.STUDENT, 9L, PersonRole.TUTOR, 7L, PersonRole.PARENT, 2L));
        Lesson lesson = new Lesson(LESSON, TUTOR,
                new LessonTimeSlot(LessonDay.MONDAY, new LessonTime("0900"), new LessonTime("1000")),
                new Subject("Math"), new Room("R1"), isEnrolled ? Set.of(SELECTED_STUDENT) : Set.of());
        return new PonHubDataState(people, List.of(lesson), List.of(RETAINED_ATTENDANCE), 12);
    }

    private static Student student(PersonId id, String name) {
        return new Student(id, new ContactDetails(new Name(name)), new EducationLevel("P1"), new Phone("60000002"));
    }

    private static ContactDetails contacts(String name, String phone) {
        return new ContactDetails(new Name(name), Optional.of(new Phone(phone)), Optional.empty(), Optional.empty());
    }

    /**
     * Holds real storage, application Logic and retained observable projections for one independent scenario.
     */
    private class Fixture {
        private final Path path;
        private final PonHubDataState initial;
        private final CanonicalPeopleSession session;
        private final Logic logic;
        private final ObservableList<PersonRecord> people;
        private final ObservableList<LessonView> lessons;
        private final ObservableList<StudentView> roster;
        private final ObservableList<LessonView> studentLessons;
        private final IOException saveFailure = new IOException("Injected membership save failure");
        private int writes;
        private int notifications;

        Fixture(String filename, boolean isEnrolled, boolean shouldFailFirstSave) throws Exception {
            path = directory.resolve(filename);
            initial = state(isEnrolled);
            JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
            storage.saveData(initial);
            session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), candidate -> {
                if (writes == 0 || (writes == 1 && shouldFailFirstSave)) {
                    assertLiveInitial();
                }
                if (++writes == 1 && shouldFailFirstSave) {
                    throw saveFailure;
                }
                storage.saveData(candidate);
            });
            logic = new CanonicalLogicManager(session, new UserPrefs());
            people = logic.getPersonRecords();
            lessons = logic.getLessonList();
            roster = logic.getLessonRoster(LESSON);
            studentLessons = logic.getStudentLessons(SELECTED_STUDENT);
            people.addListener((ListChangeListener<PersonRecord>) change -> notifications++);
            lessons.addListener((ListChangeListener<LessonView>) change -> notifications++);
            roster.addListener((ListChangeListener<StudentView>) change -> notifications++);
            studentLessons.addListener((ListChangeListener<LessonView>) change -> notifications++);
        }

        void assertLiveInitial() {
            assertEquals(initial, session.getDataSnapshot());
        }

        void narrowToBob() throws Exception {
            assertEquals("Found 1 student(s).", logic.execute("search c/student n/Bob").getFeedbackToUser());
            assertNarrowedView();
            resetNotifications();
        }

        void resetNotifications() {
            notifications = 0;
        }

        void assertNarrowedView() {
            assertEquals(List.of(SELECTED_STUDENT), people.stream().map(PersonRecord::getId).toList());
            assertEquals(Optional.of(PersonRole.STUDENT), session.getRoleFilter());
            assertIdentities();
        }

        void assertIdentities() {
            assertSame(people, logic.getPersonRecords());
            assertSame(lessons, logic.getLessonList());
            assertSame(roster, logic.getLessonRoster(LESSON));
            assertSame(studentLessons, logic.getStudentLessons(SELECTED_STUDENT));
        }

        void assertUnchanged(PonHubDataState before, byte[] bytes) throws Exception {
            assertEquals(before, session.getDataSnapshot());
            assertEquals(before.people().getLastAllocatedSequences(), session.getDataSnapshot().people()
                    .getLastAllocatedSequences());
            assertEquals(before.lastAllocatedLessonSequence(), session.getDataSnapshot().lastAllocatedLessonSequence());
            assertArrayEquals(bytes, Files.readAllBytes(path));
            assertEquals(0, notifications);
            assertIdentities();
        }

        void assertMembership(boolean isEnrolled) {
            Lesson original = initial.lessons().getFirst();
            Lesson expected = new Lesson(original.getId(), original.getTutorId(), original.getTimeSlot(),
                    original.getSubject(), original.getRoom(), isEnrolled ? Set.of(SELECTED_STUDENT) : Set.of());
            assertEquals(new PonHubDataState(initial.people(), List.of(expected), initial.attendance(),
                    initial.lastAllocatedLessonSequence()), session.getDataSnapshot());
            assertEquals(isEnrolled ? List.of(SELECTED_STUDENT) : List.of(),
                    roster.stream().map(StudentView::getStudentId).toList());
            assertEquals(isEnrolled ? List.of(LESSON) : List.of(),
                    studentLessons.stream().map(LessonView::getLessonId).toList());
            assertNarrowedView();
        }

        void assertReloaded() {
            CanonicalPeopleSession reloaded = new CanonicalPeopleSession(path);
            assertEquals(session.getDataSnapshot(), reloaded.getDataSnapshot());
            assertEquals(List.of(RETAINED_ATTENDANCE), reloaded.getDataSnapshot().attendance());
        }

        void assertSearchStillApplies() throws Exception {
            // A later commit refreshes people: a lost name criterion would reveal Alice as well as Bob.
            boolean isEnrolled = session.getDataSnapshot().lessons().getFirst()
                    .getEnrolledStudentIds().contains(SELECTED_STUDENT);
            logic.execute((isEnrolled ? "unenrol" : "enrol") + " 1 lid/L10");
            assertMembership(!isEnrolled);
            assertReloaded();
        }
    }
}
