package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import seedu.address.logic.parser.PersonAdditionInput;
import seedu.address.logic.parser.RoleAwareAddParser;
import seedu.address.model.PonHubData;
import seedu.address.model.PonHubDataState;
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
import seedu.address.model.person.PeopleRegistry;
import seedu.address.model.person.PeopleRegistryState;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Student;
import seedu.address.model.person.exceptions.DuplicatePersonException;
import seedu.address.model.person.exceptions.PersonIdExhaustedException;

public class PersonAdditionCandidateTest {

    private final RoleAwareAddParser parser = new RoleAwareAddParser();

    @Test
    public void prepare_allRolesAndOptionalContacts_returnsAppendedCandidateWithoutChangingSource() throws Exception {
        PonHubData liveData = new PonHubData(sourceWithHistory());
        PonHubDataState source = liveData.exportState();
        for (PersonRole role : PersonRole.values()) {
            for (String optionalContacts : new String[]{"", " e/New@example.com a/Block 2"}) {
                String arguments = role == PersonRole.STUDENT
                        ? "r/student n/New Person l/S2 pp/00012345" : "r/" + role.getValue()
                                + " n/New Person p/00012345";
                if (role == PersonRole.STUDENT && !optionalContacts.isEmpty()) {
                    arguments += " p/00345678";
                }
                PersonAdditionInput input = parser.parse(arguments + optionalContacts);
                PersonAdditionCandidate candidate = PersonAdditionCandidate.prepare(source, input);
                PersonRecord added = candidate.getAddedPerson();
                assertEquals(role, added.getRole());
                assertEquals(input.contactDetails(), added.getContactDetails());
                assertEquals(PersonId.of(role, source.people().getLastAllocatedSequences().get(role) + 1),
                        added.getId());
                List<PersonRecord> candidatePeople = candidate.getCandidateState().people().getPeople();
                assertEquals(source.people().getPeople(), candidatePeople.subList(0, candidatePeople.size() - 1));
                assertEquals(added, candidatePeople.getLast());
                if (added instanceof Student student) {
                    assertEquals(input.level().orElseThrow(), student.getLevel());
                    assertEquals(input.parentPhone().orElseThrow(), student.getParentPhone());
                }
                assertEquals(source, liveData.exportState());
            }
        }
    }

    @Test
    public void prepare_studentsWithSharedParentPhone_doesNotRequireParentRecord() throws Exception {
        PonHubDataState source = sourceWithHistory();
        assertTrue(source.people().getPeople().stream().noneMatch(person -> person.getRole() == PersonRole.PARENT));
        PersonAdditionCandidate candidate = PersonAdditionCandidate.prepare(source,
                parser.parse("r/student n/Bob Tan l/P2 pp/00123456"));
        Student sibling = (Student) candidate.getAddedPerson();
        Student original = (Student) source.people().getPeople().get(1);
        assertEquals(original.getParentPhone(), sibling.getParentPhone());
        assertNotEquals(original.getId(), sibling.getId());
        assertEquals(Optional.empty(), sibling.getContactDetails().getPhone());
        assertEquals(3, candidate.getCandidateState().people().getPeople().size());
        assertEquals(2, source.people().getPeople().size());
    }

    @Test
    public void prepare_nearDuplicateNamesAndDifferentIdentityPhones_remainDistinct() throws Exception {
        PonHubDataState source = sourceWithHistory();
        for (String arguments : new String[]{"r/student n/Alice Tang l/P1 pp/00123456",
            "r/student n/Alice Tan l/P1 pp/00123457", "r/tutor n/Mei Lim p/00987655"}) {
            PersonAdditionCandidate candidate = PersonAdditionCandidate.prepare(source, parser.parse(arguments));
            assertEquals(3, candidate.getCandidateState().people().getPeople().size());
            assertNotEquals(source, candidate.getCandidateState());
        }
    }

    @Test
    public void prepare_normalizedDuplicatesAndOptionalChanges_preservesSourceAndCounters() throws Exception {
        PeopleRegistry people = new PeopleRegistry(sourceWithHistory().people());
        people.addParent(contacts("Pat Tan", "00098765"));
        PonHubDataState source = new PonHubDataState(people.exportState(), List.of(), List.of(), 7);
        PonHubData liveData = new PonHubData(source);
        for (String arguments : new String[]{"r/student n/ALICE  tan l/S2 pp/00123456 p/00345678",
            "r/tutor n/MEI  lim p/00987654 e/Changed@example.com",
            "r/parent n/PAT  tan p/00098765 a/Changed address"}) {
            PersonAdditionInput input = parser.parse(arguments);
            assertThrows(DuplicatePersonException.class, () -> PersonAdditionCandidate.prepare(source, input));
            assertEquals(source, liveData.exportState());
        }
        assertEquals(new PersonId("S2"), PersonAdditionCandidate.prepare(source,
                parser.parse("r/student n/Bob Tan l/P1 pp/00123456")).getAddedPerson().getId());
    }

    @Test
    public void prepare_deletedIdentityHistoryAndEmptyRoles_usesStoredCounters() throws Exception {
        EnumMap<PersonRole, Long> counters = new EnumMap<>(PersonRole.class);
        counters.put(PersonRole.STUDENT, 20L);
        counters.put(PersonRole.TUTOR, 10L);
        counters.put(PersonRole.PARENT, 5L);
        PonHubDataState source = new PonHubDataState(new PeopleRegistryState(List.of(), counters),
                List.of(), List.of(), 30);
        for (PersonRole role : PersonRole.values()) {
            PersonAdditionCandidate candidate = PersonAdditionCandidate.prepare(source, inputFor(role));
            assertEquals(PersonId.of(role, counters.get(role) + 1), candidate.getAddedPerson().getId());
            for (PersonRole counterRole : PersonRole.values()) {
                assertEquals(counters.get(counterRole) + (counterRole == role ? 1 : 0),
                        candidate.getCandidateState().people().getLastAllocatedSequences().get(counterRole));
            }
        }
        assertEquals(counters, source.people().getLastAllocatedSequences());
    }

    @Test
    public void prepare_lastAvailableIdentityAndExhaustion_preservesOtherRolesAndSource() throws Exception {
        for (PersonRole role : PersonRole.values()) {
            EnumMap<PersonRole, Long> counters = new EnumMap<>(PersonRole.class);
            for (PersonRole counterRole : PersonRole.values()) {
                counters.put(counterRole, counterRole == role ? Long.MAX_VALUE - 1 : 0L);
            }
            PonHubDataState source = new PonHubDataState(new PeopleRegistryState(List.of(), counters),
                    List.of(), List.of(), Long.MAX_VALUE);
            PersonAdditionInput input = inputFor(role);
            PersonAdditionCandidate last = PersonAdditionCandidate.prepare(source, input);
            assertEquals(PersonId.of(role, Long.MAX_VALUE), last.getAddedPerson().getId());
            assertThrows(PersonIdExhaustedException.class, () ->
                    PersonAdditionCandidate.prepare(last.getCandidateState(), input));
            PersonRole otherRole = role == PersonRole.STUDENT ? PersonRole.TUTOR : PersonRole.STUDENT;
            assertEquals(PersonId.of(otherRole, 1), PersonAdditionCandidate.prepare(last.getCandidateState(),
                    inputFor(otherRole)).getAddedPerson().getId());
            assertEquals(counters, source.people().getLastAllocatedSequences());
        }
    }

    @Test
    public void prepare_retainedRelationshipsAndSnapshots_remainImmutableAndIndependent() throws Exception {
        PonHubData liveData = new PonHubData(sourceWithHistory());
        PonHubDataState source = liveData.exportState();
        PersonAdditionInput input = parser.parse("r/student n/Bob Tan l/P2 pp/00123456");
        PersonAdditionCandidate candidate = PersonAdditionCandidate.prepare(source, input);
        PonHubDataState state = candidate.getCandidateState();
        assertEquals(source.lessons(), state.lessons());
        assertEquals(source.attendance(), state.attendance());
        assertEquals(source.lastAllocatedLessonSequence(), state.lastAllocatedLessonSequence());
        assertTrue(state.lessons().getFirst().getEnrolledStudentIds().isEmpty());
        assertEquals(Set.of(new PersonId("S1")), state.lessons().getLast().getEnrolledStudentIds());
        assertThrows(UnsupportedOperationException.class, () -> state.people().getPeople().clear());
        assertThrows(UnsupportedOperationException.class, () -> state.people().getLastAllocatedSequences().clear());
        assertThrows(UnsupportedOperationException.class, () -> state.lessons().clear());
        assertThrows(UnsupportedOperationException.class, () -> state.attendance().clear());
        assertThrows(UnsupportedOperationException.class, () ->
                state.lessons().getLast().getEnrolledStudentIds().clear());
        assertEquals(state, PersonAdditionCandidate.prepare(source, input).getCandidateState());
        liveData.resetData(new PonHubData());
        assertEquals(3, state.people().getPeople().size());
        assertEquals(source.lessons(), state.lessons());
        assertEquals(source.attendance(), state.attendance());
        assertEquals(2, source.people().getPeople().size());
    }

    @Test
    public void prepare_nullSourceOrInput_rejectsWithoutChangingLiveData() throws Exception {
        PonHubData liveData = new PonHubData(sourceWithHistory());
        PonHubDataState source = liveData.exportState();
        PersonAdditionInput input = inputFor(PersonRole.STUDENT);
        assertThrows(NullPointerException.class, () -> PersonAdditionCandidate.prepare(null, input));
        assertThrows(NullPointerException.class, () -> PersonAdditionCandidate.prepare(source, null));
        assertEquals(source, liveData.exportState());
    }

    private PersonAdditionInput inputFor(PersonRole role) throws Exception {
        return parser.parse(role == PersonRole.STUDENT ? "r/student n/New Person l/P1 pp/00012345"
                : "r/" + role.getValue() + " n/New Person p/00012345");
    }

    private static PonHubDataState sourceWithHistory() {
        PeopleRegistry people = new PeopleRegistry();
        var tutor = people.addTutor(contacts("Mei Lim", "00987654"));
        var student = people.addStudent(new ContactDetails(new Name("Alice Tan")),
                new EducationLevel("P1"), new Phone("00123456"));
        Lesson lesson = new Lesson(new LessonId("L3"), tutor.getId(), new LessonTimeSlot(LessonDay.MONDAY,
                new LessonTime("0900"), new LessonTime("1000")), new Subject("Mathematics"), new Room("R1"),
                Set.of());
        Lesson enrolledLesson = new Lesson(new LessonId("L5"), tutor.getId(), new LessonTimeSlot(LessonDay.TUESDAY,
                new LessonTime("0900"), new LessonTime("1000")), new Subject("Science"), new Room("R2"),
                Set.of(student.getId()));
        Attendance retained = new Attendance(student.getId(), lesson.getId(), LocalDate.of(2026, 10, 5),
                AttendanceStatus.PRESENT);
        return new PonHubDataState(people.exportState(), List.of(lesson, enrolledLesson), List.of(retained), 7);
    }

    private static ContactDetails contacts(String name, String phone) {
        return new ContactDetails(new Name(name), Optional.of(new Phone(phone)), Optional.empty(), Optional.empty());
    }
}
