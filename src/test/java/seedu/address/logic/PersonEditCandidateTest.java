package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import seedu.address.logic.parser.PersonEditInput;
import seedu.address.logic.parser.PersonEditInput.Field;
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
import seedu.address.model.person.Address;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Parent;
import seedu.address.model.person.PeopleRegistryState;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Student;
import seedu.address.model.person.Tutor;
import seedu.address.model.person.exceptions.DuplicatePersonException;
import seedu.address.model.query.LessonQueries;

public class PersonEditCandidateTest {

    @Test
    public void prepare_studentSetAndClear_preservesPositionIdentityAndCompleteRelationships() {
        PonHubData liveData = new PonHubData(sourceWithHistory());
        PonHubDataState source = liveData.exportState();
        PersonEditInput input = new PersonEditInput(Field.keep(), Field.clear(), Field.clear(), Field.clear(),
                Field.set(new EducationLevel("p2")), Field.keep());
        PersonEditCandidate candidate = PersonEditCandidate.prepare(source, new PersonId("S7"), input);
        Student expected = new Student(new PersonId("S7"), new ContactDetails(new Name("Alex Tan")),
                new EducationLevel("P2"), new Phone("00123456"));

        assertTrue(candidate.isChanged());
        assertEquals(expected, candidate.getEditedPerson());
        assertEquals(replaceFixturePerson(source, expected), candidate.getCandidateState());
        assertEquals(List.of("T4", "S7", "P2", "S9", "T6", "P5"),
                candidate.getCandidateState().people().getPeople().stream()
                        .map(person -> person.getId().toString()).toList());
        assertNotSame(source, candidate.getCandidateState());
        assertRetainedRelationships(source, candidate.getCandidateState());
        assertSame(source, liveData.exportState());
        assertEquals(Optional.of(new Phone("00345678")),
                person(source, "S7").getContactDetails().getPhone());
    }

    @Test
    public void prepare_allRolesSetContacts_buildsCanonicalRecordWithSameIdAndRole() {
        PonHubDataState source = sourceWithHistory();
        for (String id : new String[]{"S7", "T4", "P2"}) {
            PersonRecord original = person(source, id);
            ContactDetails expectedContacts = new ContactDetails(new Name("Corrected Person"),
                    Optional.of(new Phone("00011122")), Optional.of(new Email("new@example.com")),
                    Optional.of(new Address("New Road")));
            PersonEditInput input = new PersonEditInput(Field.set(new Name("  Corrected   Person  ")),
                    Field.set(new Phone("00011122")), Field.set(new Email("new@example.com")),
                    Field.set(new Address("New Road")),
                    original instanceof Student ? Field.set(new EducationLevel("S2")) : Field.keep(),
                    original instanceof Student ? Field.set(new Phone("00033344")) : Field.keep());

            PersonEditCandidate candidate = PersonEditCandidate.prepare(source, original.getId(), input);
            PersonRecord edited = candidate.getEditedPerson();
            assertTrue(candidate.isChanged());
            assertEquals(original.getClass(), edited.getClass());
            assertEquals(original.getId(), edited.getId());
            assertEquals(original.getRole(), edited.getRole());
            assertEquals(expectedContacts, edited.getContactDetails());
            if (edited instanceof Student student) {
                assertEquals(new EducationLevel("S2"), student.getLevel());
                assertEquals(new Phone("00033344"), student.getParentPhone());
            }
            assertEquals(replaceFixturePerson(source, edited), candidate.getCandidateState());
            assertRetainedRelationships(source, candidate.getCandidateState());
            assertNotEquals(expectedContacts, original.getContactDetails());
        }
    }

    @Test
    public void prepare_tutorAndParentOptionalClears_keepsRequiredOwnPhone() {
        PonHubDataState source = sourceWithHistory();
        PersonEditInput input = new PersonEditInput(Field.keep(), Field.keep(), Field.clear(), Field.clear(),
                Field.keep(), Field.keep());
        for (String id : new String[]{"T4", "P2"}) {
            PersonRecord original = person(source, id);
            PersonEditCandidate candidate = PersonEditCandidate.prepare(source, original.getId(), input);

            assertTrue(candidate.isChanged());
            assertEquals(new ContactDetails(original.getName(), original.getContactDetails().getPhone(),
                    Optional.empty(), Optional.empty()), candidate.getEditedPerson().getContactDetails());
            assertEquals(replaceFixturePerson(source, candidate.getEditedPerson()), candidate.getCandidateState());
            assertRetainedRelationships(source, candidate.getCandidateState());
        }
    }

    @Test
    public void prepare_individualContactChanges_preservesEveryOmittedFieldAndDisplayEmailCase() {
        PonHubDataState source = sourceWithHistory();
        Phone changedPhone = new Phone("00011122");
        Email changedEmail = new Email("EXISTING@example.com");
        Address changedAddress = new Address("  New   Road  ");
        List<PersonEditInput> inputs = List.of(
                new PersonEditInput(Field.keep(), Field.set(changedPhone), Field.keep(), Field.keep(),
                        Field.keep(), Field.keep()),
                new PersonEditInput(Field.keep(), Field.keep(), Field.set(changedEmail), Field.keep(),
                        Field.keep(), Field.keep()),
                new PersonEditInput(Field.keep(), Field.keep(), Field.keep(), Field.set(changedAddress),
                        Field.keep(), Field.keep()));
        for (String id : new String[]{"S7", "T4", "P2"}) {
            PersonRecord original = person(source, id);
            ContactDetails contacts = original.getContactDetails();
            List<ContactDetails> expectedContacts = List.of(
                    new ContactDetails(original.getName(), Optional.of(changedPhone), contacts.getEmail(),
                            contacts.getAddress()),
                    new ContactDetails(original.getName(), contacts.getPhone(), Optional.of(changedEmail),
                            contacts.getAddress()),
                    new ContactDetails(original.getName(), contacts.getPhone(), contacts.getEmail(),
                            Optional.of(new Address("New Road"))));
            for (int i = 0; i < inputs.size(); i++) {
                PersonEditCandidate candidate = PersonEditCandidate.prepare(source, original.getId(), inputs.get(i));

                assertTrue(candidate.isChanged());
                assertEquals(expectedContacts.get(i), candidate.getEditedPerson().getContactDetails());
                assertEquals(original.getId(), candidate.getEditedPerson().getId());
                if (candidate.getEditedPerson() instanceof Student student) {
                    assertEquals(((Student) original).getLevel(), student.getLevel());
                    assertEquals(((Student) original).getParentPhone(), student.getParentPhone());
                }
                assertRetainedRelationships(source, candidate.getCandidateState());
            }
        }
    }

    @Test
    public void prepare_omittedIdenticalAndNormalizedValues_returnsOriginalSnapshotWithoutChange() {
        PonHubDataState source = sourceWithHistory();
        for (PersonRecord original : source.people().getPeople()) {
            Name spacedName = new Name("  " + original.getName().fullName.replace(" ", "   ") + "  ");
            PersonEditInput nameOnly = new PersonEditInput(Field.set(spacedName), Field.keep(), Field.keep(),
                    Field.keep(), Field.keep(), Field.keep());
            for (PersonEditInput input : List.of(keepAll(), nameOnly, sameValues(original))) {
                PersonEditCandidate candidate = PersonEditCandidate.prepare(source, original.getId(), input);

                assertFalse(candidate.isChanged());
                assertSame(source, candidate.getCandidateState());
                assertSame(original, candidate.getEditedPerson());
            }
        }
    }

    @Test
    public void prepare_displayCaseChange_isChangeAndNeverDuplicatesSelf() {
        PonHubDataState source = sourceWithHistory();
        for (PersonRecord original : source.people().getPeople()) {
            Name changedName = new Name(original.getName().fullName.toUpperCase(Locale.ROOT));
            PersonEditInput input = new PersonEditInput(Field.set(changedName), Field.keep(), Field.keep(),
                    Field.keep(), Field.keep(), Field.keep());
            PersonEditCandidate candidate = PersonEditCandidate.prepare(source, original.getId(), input);

            assertTrue(candidate.isChanged());
            assertEquals(changedName, candidate.getEditedPerson().getName());
            assertEquals(original.getContactDetails().getNormalizedName(),
                    candidate.getEditedPerson().getContactDetails().getNormalizedName());
            assertNotSame(source, candidate.getCandidateState());
            assertRetainedRelationships(source, candidate.getCandidateState());
        }
    }

    @Test
    public void prepare_requiredFieldClears_rejectsWithoutChangingSource() {
        PonHubData liveData = new PonHubData(sourceWithHistory());
        PonHubDataState source = liveData.exportState();
        PersonEditInput clearName = new PersonEditInput(Field.clear(), Field.keep(), Field.keep(),
                Field.keep(), Field.keep(), Field.keep());
        for (PersonRecord original : source.people().getPeople()) {
            assertThrows(IllegalArgumentException.class, () ->
                    PersonEditCandidate.prepare(source, original.getId(), clearName));
        }
        for (PersonEditInput input : List.of(
                new PersonEditInput(Field.keep(), Field.keep(), Field.keep(), Field.keep(),
                        Field.clear(), Field.keep()),
                new PersonEditInput(Field.keep(), Field.keep(), Field.keep(), Field.keep(),
                        Field.keep(), Field.clear()))) {
            assertThrows(IllegalArgumentException.class, () ->
                    PersonEditCandidate.prepare(source, new PersonId("S7"), input));
        }
        PersonEditInput clearPhone = new PersonEditInput(Field.keep(), Field.clear(), Field.keep(),
                Field.keep(), Field.keep(), Field.keep());
        for (String id : new String[]{"T4", "P2"}) {
            assertThrows(IllegalArgumentException.class, () ->
                    PersonEditCandidate.prepare(source, new PersonId(id), clearPhone));
        }
        assertSame(source, liveData.exportState());
        assertEquals(sourceWithHistory(), source);
    }

    @Test
    public void prepare_studentFieldsForOtherRoles_rejectsSetAndClearWithoutChangingSource() {
        PonHubDataState source = sourceWithHistory();
        for (String id : new String[]{"T4", "P2"}) {
            for (PersonEditInput input : List.of(
                    new PersonEditInput(Field.keep(), Field.keep(), Field.keep(), Field.keep(),
                            Field.set(new EducationLevel("P2")), Field.keep()),
                    new PersonEditInput(Field.keep(), Field.keep(), Field.keep(), Field.keep(),
                            Field.clear(), Field.keep()),
                    new PersonEditInput(Field.keep(), Field.keep(), Field.keep(), Field.keep(),
                            Field.keep(), Field.set(new Phone("00123456"))),
                    new PersonEditInput(Field.keep(), Field.keep(), Field.keep(), Field.keep(),
                            Field.keep(), Field.clear()))) {
                assertThrows(IllegalArgumentException.class, () ->
                        PersonEditCandidate.prepare(source, new PersonId(id), input));
                assertEquals(sourceWithHistory(), source);
            }
        }
    }

    @Test
    public void prepare_namePhoneAndCombinedDuplicates_rejectsAcrossAllRolesAndPreservesSource() {
        PonHubDataState source = sourceWithHistory();
        for (PersonRole role : PersonRole.values()) {
            List<PersonRecord> sameRole = source.people().getPeople().stream()
                    .filter(person -> person.getRole() == role).toList();
            PersonRecord target = sameRole.getFirst();
            PersonRecord other = sameRole.getLast();
            Name duplicateName = new Name(other.getName().fullName.toUpperCase(Locale.ROOT).replace(" ", "  "));

            PonHubDataState sharedPhone = replaceFixturePerson(source,
                    withIdentity(other, other.getName(), identifyingPhone(target)));
            PersonEditInput nameOnly = identityEdit(role, Field.set(duplicateName), Field.keep());
            assertThrows(DuplicatePersonException.class, () ->
                    PersonEditCandidate.prepare(sharedPhone, target.getId(), nameOnly));

            PonHubDataState sharedName = replaceFixturePerson(source,
                    withIdentity(other, target.getName(), identifyingPhone(other)));
            PersonEditInput phoneOnly = identityEdit(role, Field.keep(), Field.set(identifyingPhone(other)));
            assertThrows(DuplicatePersonException.class, () ->
                    PersonEditCandidate.prepare(sharedName, target.getId(), phoneOnly));

            PersonEditInput identity = identityEdit(role, Field.set(duplicateName),
                    Field.set(identifyingPhone(other)));
            PersonEditInput bothWithOptionalChanges = new PersonEditInput(identity.name(),
                    role == PersonRole.STUDENT ? Field.set(new Phone("00077788")) : identity.phone(),
                    Field.set(new Email("different@example.com")), Field.set(new Address("Different Road")),
                    role == PersonRole.STUDENT ? Field.set(new EducationLevel("JC2")) : Field.keep(),
                    identity.parentPhone());
            PonHubData liveData = new PonHubData(source);
            assertThrows(DuplicatePersonException.class, () ->
                    PersonEditCandidate.prepare(source, target.getId(), bothWithOptionalChanges));
            assertSame(source, liveData.exportState());
            assertRetainedRelationships(source, sharedPhone);
            assertRetainedRelationships(source, sharedName);
            assertEquals(sourceWithHistory(), source);
        }
    }

    @Test
    public void prepare_differentNamePhoneOrRole_allowsDistinctBusinessIdentities() {
        PonHubDataState source = sourceWithHistory();
        for (PersonRole role : PersonRole.values()) {
            List<PersonRecord> sameRole = source.people().getPeople().stream()
                    .filter(person -> person.getRole() == role).toList();
            PersonRecord target = sameRole.getFirst();
            PersonRecord other = sameRole.getLast();
            PersonRecord otherRole = person(source, role == PersonRole.TUTOR ? "P2" : "T4");
            for (PersonEditInput input : List.of(
                    identityEdit(role, Field.set(other.getName()), Field.keep()),
                    identityEdit(role, Field.keep(), Field.set(identifyingPhone(other))),
                    identityEdit(role, Field.set(otherRole.getName()), Field.set(identifyingPhone(otherRole))))) {
                PersonEditCandidate candidate = PersonEditCandidate.prepare(source, target.getId(), input);

                assertTrue(candidate.isChanged());
                assertEquals(target.getId(), candidate.getEditedPerson().getId());
                assertEquals(role, candidate.getEditedPerson().getRole());
                assertEquals(6, candidate.getCandidateState().people().getPeople().size());
                assertRetainedRelationships(source, candidate.getCandidateState());
            }
        }
    }

    @Test
    public void prepare_parentPhoneChanges_recomputesExactLinksWithoutCascadingOrCreatingPeople() {
        PonHubDataState source = sourceWithHistory();
        PersonEditInput studentInput = identityEdit(PersonRole.STUDENT, Field.keep(),
                Field.set(new Phone("00765432")));
        PersonEditCandidate studentEdit = PersonEditCandidate.prepare(source, new PersonId("S7"), studentInput);
        PonHubDataState studentState = studentEdit.getCandidateState();
        LessonQueries studentQueries = new LessonQueries(new PonHubData(studentState));

        assertEquals(List.of("S7", "S9"), studentQueries.getStudentsByParentPhone(new Phone("00765432"))
                .stream().map(student -> student.getId().toString()).toList());
        assertTrue(studentQueries.getStudentsByParentPhone(new Phone("00123456")).isEmpty());
        assertEquals(person(source, "P2"), person(studentState, "P2"));
        assertEquals(person(source, "P5"), person(studentState, "P5"));
        assertEquals(6, studentState.people().getPeople().size());

        for (String unmatchedPhone : new String[]{"0123456", "00888888"}) {
            PersonEditCandidate unmatched = PersonEditCandidate.prepare(source, new PersonId("S7"),
                    identityEdit(PersonRole.STUDENT, Field.keep(), Field.set(new Phone(unmatchedPhone))));
            assertEquals(6, unmatched.getCandidateState().people().getPeople().size());
            assertEquals(person(source, "P2"), person(unmatched.getCandidateState(), "P2"));
            assertEquals(new Phone(unmatchedPhone), ((Student) unmatched.getEditedPerson()).getParentPhone());
        }

        PersonEditCandidate parentEdit = PersonEditCandidate.prepare(source, new PersonId("P2"),
                identityEdit(PersonRole.PARENT, Field.keep(), Field.set(new Phone("00765432"))));
        assertEquals(person(source, "S7"), person(parentEdit.getCandidateState(), "S7"));
        assertEquals(person(source, "S9"), person(parentEdit.getCandidateState(), "S9"));
        LessonQueries parentQueries = new LessonQueries(new PonHubData(parentEdit.getCandidateState()));
        Parent editedParent = (Parent) parentEdit.getEditedPerson();
        assertEquals(List.of(person(source, "S9")), parentQueries.getStudentsByParentPhone(editedParent.getPhone()));
        assertRetainedRelationships(source, studentState);
        assertRetainedRelationships(source, parentEdit.getCandidateState());
    }

    @Test
    public void prepare_candidateSnapshots_remainImmutableAndIndependentOfLaterLiveEdits() {
        PonHubData liveData = new PonHubData(sourceWithHistory());
        PonHubDataState source = liveData.exportState();
        PersonEditInput input = new PersonEditInput(Field.set(new Name("Jamie Lim")), Field.keep(), Field.keep(),
                Field.keep(), Field.keep(), Field.keep());
        PersonEditCandidate candidate = PersonEditCandidate.prepare(source, new PersonId("T4"), input);
        PonHubDataState state = candidate.getCandidateState();

        assertThrows(UnsupportedOperationException.class, () -> state.people().getPeople().clear());
        assertThrows(UnsupportedOperationException.class, () -> state.people().getLastAllocatedSequences().clear());
        assertThrows(UnsupportedOperationException.class, () -> state.lessons().clear());
        assertThrows(UnsupportedOperationException.class, () -> state.attendance().clear());
        assertThrows(UnsupportedOperationException.class, () ->
                state.lessons().getFirst().getEnrolledStudentIds().clear());
        assertEquals(state, PersonEditCandidate.prepare(source, new PersonId("T4"), input).getCandidateState());
        assertEquals(new PersonId("T4"), state.lessons().getFirst().getTutorId());
        assertEquals(Set.of(new PersonId("S7"), new PersonId("S9")),
                state.lessons().getFirst().getEnrolledStudentIds());
        assertTrue(state.lessons().get(1).getEnrolledStudentIds().isEmpty());
        assertEquals(new PersonId("S7"), state.attendance().get(1).getStudentId());
        assertEquals(state.lessons().get(1).getId(), state.attendance().get(1).getLessonId());

        liveData.replacePerson(withIdentity(person(source, "P2"), new Name("Later Parent"), new Phone("00066677")));
        liveData.resetData(new PonHubData());
        assertEquals(6, state.people().getPeople().size());
        assertEquals(new Name("Jamie Lim"), person(state, "T4").getName());
        assertEquals(person(source, "P2"), person(state, "P2"));
        assertRetainedRelationships(source, state);
        assertEquals(sourceWithHistory(), source);
    }

    @Test
    public void prepare_exhaustedAllocationHistories_editsEveryRoleWithoutAllocating() {
        PonHubDataState baseline = sourceWithHistory();
        PonHubDataState source = new PonHubDataState(new PeopleRegistryState(baseline.people().getPeople(),
                Map.of(PersonRole.STUDENT, Long.MAX_VALUE, PersonRole.TUTOR, Long.MAX_VALUE,
                        PersonRole.PARENT, Long.MAX_VALUE)), baseline.lessons(), baseline.attendance(), Long.MAX_VALUE);
        PersonEditInput input = new PersonEditInput(Field.set(new Name("Corrected Person")), Field.keep(),
                Field.keep(), Field.keep(), Field.keep(), Field.keep());
        for (String id : new String[]{"S7", "T4", "P2"}) {
            PersonEditCandidate candidate = PersonEditCandidate.prepare(source, new PersonId(id), input);

            assertTrue(candidate.isChanged());
            assertEquals(new PersonId(id), candidate.getEditedPerson().getId());
            assertEquals(source.people().getPeople().stream().map(PersonRecord::getId).toList(),
                    candidate.getCandidateState().people().getPeople().stream().map(PersonRecord::getId).toList());
            assertRetainedRelationships(source, candidate.getCandidateState());
            assertSame(source, PersonEditCandidate.prepare(source, new PersonId(id), keepAll()).getCandidateState());
        }
    }

    @Test
    public void prepare_nullArgumentsAndMissingTargets_rejectsWithoutChangingLiveState() {
        PonHubData liveData = new PonHubData(sourceWithHistory());
        PonHubDataState source = liveData.exportState();
        PersonEditInput input = keepAll();

        assertThrows(NullPointerException.class, () ->
                PersonEditCandidate.prepare(null, new PersonId("S7"), input));
        assertThrows(NullPointerException.class, () -> PersonEditCandidate.prepare(source, null, input));
        assertThrows(NullPointerException.class, () ->
                PersonEditCandidate.prepare(source, new PersonId("S7"), null));
        for (String id : new String[]{"S8", "T5", "P3"}) {
            assertThrows(IllegalArgumentException.class, () ->
                    PersonEditCandidate.prepare(source, new PersonId(id), input));
        }
        assertSame(source, liveData.exportState());
    }

    private static PersonEditInput keepAll() {
        return new PersonEditInput(Field.keep(), Field.keep(), Field.keep(), Field.keep(), Field.keep(), Field.keep());
    }

    private static PersonEditInput sameValues(PersonRecord original) {
        ContactDetails contacts = original.getContactDetails();
        return new PersonEditInput(Field.set(contacts.getName()), suppliedOptional(contacts.getPhone()),
                suppliedOptional(contacts.getEmail()), suppliedOptional(contacts.getAddress()),
                original instanceof Student student ? Field.set(student.getLevel()) : Field.keep(),
                original instanceof Student student ? Field.set(student.getParentPhone()) : Field.keep());
    }

    private static <T> Field<T> suppliedOptional(Optional<T> value) {
        return value.map(Field::set).orElseGet(Field::clear);
    }

    private static PersonEditInput identityEdit(PersonRole role, Field<Name> name, Field<Phone> phone) {
        return new PersonEditInput(name, role == PersonRole.STUDENT ? Field.keep() : phone,
                Field.keep(), Field.keep(), Field.keep(), role == PersonRole.STUDENT ? phone : Field.keep());
    }

    private static Phone identifyingPhone(PersonRecord person) {
        return person instanceof Student student ? student.getParentPhone()
                : person.getContactDetails().getPhone().orElseThrow();
    }

    private static PersonRecord withIdentity(PersonRecord original, Name name, Phone phone) {
        ContactDetails previous = original.getContactDetails();
        ContactDetails contacts = new ContactDetails(name,
                original instanceof Student ? previous.getPhone() : Optional.of(phone),
                previous.getEmail(), previous.getAddress());
        return switch (original.getRole()) {
            case STUDENT -> new Student(original.getId(), contacts, ((Student) original).getLevel(), phone);
            case TUTOR -> new Tutor(original.getId(), contacts);
            case PARENT -> new Parent(original.getId(), contacts);
        };
    }

    private static PersonRecord person(PonHubDataState source, String id) {
        return source.people().getPeople().stream().filter(person -> person.getId().equals(new PersonId(id)))
                .findFirst().orElseThrow();
    }

    private static PonHubDataState replaceFixturePerson(PonHubDataState source, PersonRecord replacement) {
        List<PersonRecord> people = new ArrayList<>(source.people().getPeople());
        people.set(people.indexOf(person(source, replacement.getId().toString())), replacement);
        return new PonHubDataState(new PeopleRegistryState(people, source.people().getLastAllocatedSequences()),
                source.lessons(), source.attendance(), source.lastAllocatedLessonSequence());
    }

    private static void assertRetainedRelationships(PonHubDataState source, PonHubDataState candidate) {
        assertEquals(source.people().getLastAllocatedSequences(), candidate.people().getLastAllocatedSequences());
        assertEquals(source.lessons(), candidate.lessons());
        assertEquals(source.attendance(), candidate.attendance());
        assertEquals(source.lastAllocatedLessonSequence(), candidate.lastAllocatedLessonSequence());
    }

    private static PonHubDataState sourceWithHistory() {
        List<PersonRecord> people = List.of(
                new Tutor(new PersonId("T4"), contacts("Mei Lim", "00987654")),
                new Student(new PersonId("S7"), contacts("Alex Tan", "00345678"),
                        new EducationLevel("P1"), new Phone("00123456")),
                new Parent(new PersonId("P2"), contacts("Pat Tan", "00123456")),
                new Student(new PersonId("S9"), new ContactDetails(new Name("Jamie Tan")),
                        new EducationLevel("S1"), new Phone("00765432")),
                new Tutor(new PersonId("T6"), new ContactDetails(new Name("Arun Das"),
                        Optional.of(new Phone("00876543")), Optional.empty(), Optional.empty())),
                new Parent(new PersonId("P5"), new ContactDetails(new Name("Robin Tan"),
                        Optional.of(new Phone("00765432")), Optional.empty(), Optional.empty())));
        Lesson current = new Lesson(new LessonId("L3"), new PersonId("T4"),
                new LessonTimeSlot(LessonDay.MONDAY, new LessonTime("1600"), new LessonTime("1700")),
                new Subject("Mathematics"), new Room("R1"), Set.of(new PersonId("S7"), new PersonId("S9")));
        Lesson former = new Lesson(new LessonId("L8"), new PersonId("T4"),
                new LessonTimeSlot(LessonDay.TUESDAY, new LessonTime("1600"), new LessonTime("1700")),
                new Subject("Science"), new Room("R2"), Set.of());
        Attendance currentAttendance = new Attendance(new PersonId("S7"), current.getId(),
                LocalDate.of(2026, 10, 5), AttendanceStatus.PRESENT);
        Attendance formerAttendance = new Attendance(new PersonId("S7"), former.getId(),
                LocalDate.of(2026, 10, 6), AttendanceStatus.ABSENT);
        return new PonHubDataState(new PeopleRegistryState(people,
                Map.of(PersonRole.STUDENT, 20L, PersonRole.TUTOR, 10L, PersonRole.PARENT, 8L)),
                List.of(current, former), List.of(currentAttendance, formerAttendance), 30);
    }

    private static ContactDetails contacts(String name, String phone) {
        return new ContactDetails(new Name(name), Optional.of(new Phone(phone)),
                Optional.of(new Email("existing@example.com")), Optional.of(new Address("Existing Road")));
    }
}
