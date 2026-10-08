package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Name;
import seedu.address.model.person.Parent;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Student;
import seedu.address.model.person.Tutor;
import seedu.address.ui.PersonRecordListData;

public class PersonIndexResolverTest {

    private final Tutor tutor = new Tutor(new PersonId("T7"), createDetails("Mei Lim"));
    private final Student firstStudent = createStudent("S9", "Alex Tan");
    private final Parent parent = new Parent(new PersonId("P2"), createDetails("Pat Tan"));
    private final Student secondStudent = createStudent("S3", "Jamie Tan");
    private final List<PersonRecord> mixedPeople = List.of(tutor, firstStudent, parent, secondStudent);

    @Test
    public void resolve_mixedView_returnsActualRecordsWithoutMutatingView() throws Exception {
        List<PersonRecord> visiblePeople = new ArrayList<>(mixedPeople);
        for (int i = 0; i < mixedPeople.size(); i++) {
            assertSame(mixedPeople.get(i), PersonIndexResolver.resolve(Index.fromOneBased(i + 1), visiblePeople));
        }
        assertEquals(mixedPeople, visiblePeople);
        assertEquals(new PersonId("S9"), PersonIndexResolver.resolve(Index.fromOneBased(2), visiblePeople).getId());
    }

    @Test
    public void resolve_filteredProjection_usesCurrentPositionsRatherThanUnfilteredOrderOrIdSuffix() throws Exception {
        PersonRecordListData students = new PersonRecordListData(mixedPeople, Optional.of(PersonRole.STUDENT));
        List<PersonRecord> visibleStudents = students.getEntries().stream()
                .map(PersonRecordListData.Entry::person).toList();

        assertEquals(1, students.getEntries().get(0).displayedIndex());
        assertEquals(2, students.getEntries().get(1).displayedIndex());
        assertSame(firstStudent, PersonIndexResolver.resolve(Index.fromOneBased(1), visibleStudents));
        assertSame(secondStudent, PersonIndexResolver.resolve(Index.fromOneBased(2), visibleStudents));
        assertSame(secondStudent, PersonIndexResolver.resolveStudent(Index.fromOneBased(2), visibleStudents));
        assertEquals(List.of(new PersonId("S9"), new PersonId("S3")),
                visibleStudents.stream().map(PersonRecord::getId).toList());
    }

    @Test
    public void resolve_emptyAndOutOfRangeViews_throwsCheckedErrorWithoutChangingView() {
        List<PersonRecord> emptyPeople = List.of();
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX, () ->
                PersonIndexResolver.resolve(Index.fromOneBased(1), emptyPeople));
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX, () ->
                PersonIndexResolver.resolveStudent(Index.fromOneBased(1), emptyPeople));

        List<PersonRecord> visiblePeople = new ArrayList<>(mixedPeople);
        for (Index index : new Index[]{Index.fromOneBased(5), Index.fromOneBased(Integer.MAX_VALUE),
            Index.fromZeroBased(Integer.MAX_VALUE)}) {
            assertThrows(CommandException.class, Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX, () ->
                    PersonIndexResolver.resolve(index, visiblePeople));
            assertThrows(CommandException.class, Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX, () ->
                    PersonIndexResolver.resolveStudent(index, visiblePeople));
        }
        assertEquals(mixedPeople, visiblePeople);
    }

    @Test
    public void resolveStudent_mixedView_keepsPeopleListPositions() throws Exception {
        assertSame(firstStudent, PersonIndexResolver.resolveStudent(Index.fromOneBased(2), mixedPeople));
        assertSame(secondStudent, PersonIndexResolver.resolveStudent(Index.fromOneBased(4), mixedPeople));
    }

    @Test
    public void resolveStudent_tutorAndParentPositions_rejectsWithoutHiddenStudentFiltering() {
        List<PersonRecord> visiblePeople = new ArrayList<>(mixedPeople);
        for (int position : new int[]{1, 3}) {
            assertThrows(CommandException.class, PersonIndexResolver.MESSAGE_NOT_STUDENT, () ->
                    PersonIndexResolver.resolveStudent(Index.fromOneBased(position), visiblePeople));
        }
        assertEquals(mixedPeople, visiblePeople);
    }

    @Test
    public void resolve_nullArgumentsOrSelectedRecord_throwsNullPointerException() {
        Index firstIndex = Index.fromOneBased(1);
        assertThrows(NullPointerException.class, () -> PersonIndexResolver.resolve(null, mixedPeople));
        assertThrows(NullPointerException.class, () -> PersonIndexResolver.resolve(firstIndex, null));
        assertThrows(NullPointerException.class, () -> PersonIndexResolver.resolveStudent(null, mixedPeople));
        assertThrows(NullPointerException.class, () -> PersonIndexResolver.resolveStudent(firstIndex, null));

        List<PersonRecord> peopleWithNull = new ArrayList<>(mixedPeople);
        peopleWithNull.set(0, null);
        assertThrows(NullPointerException.class, () -> PersonIndexResolver.resolve(firstIndex, peopleWithNull));
        assertThrows(NullPointerException.class, () -> PersonIndexResolver.resolveStudent(firstIndex, peopleWithNull));
    }

    @Test
    public void resolveStudent_viewRefresh_preservesCapturedRecordAndStableIdentity() throws Exception {
        PersonRecordListData allPeople = new PersonRecordListData(mixedPeople, Optional.empty());
        List<PersonRecord> visiblePeople = new ArrayList<>(allPeople.getEntries().stream()
                .map(PersonRecordListData.Entry::person).toList());
        Index selectedIndex = Index.fromOneBased(2);
        Student selectedStudent = PersonIndexResolver.resolveStudent(selectedIndex, visiblePeople);

        PersonRecordListData refreshed = new PersonRecordListData(List.of(secondStudent, parent), Optional.empty());
        visiblePeople.clear();
        visiblePeople.addAll(refreshed.getEntries().stream().map(PersonRecordListData.Entry::person).toList());

        assertSame(firstStudent, selectedStudent);
        assertEquals(new PersonId("S9"), selectedStudent.getId());
        assertSame(parent, PersonIndexResolver.resolve(selectedIndex, visiblePeople));
        assertThrows(CommandException.class, PersonIndexResolver.MESSAGE_NOT_STUDENT, () ->
                PersonIndexResolver.resolveStudent(selectedIndex, visiblePeople));
    }

    private static Student createStudent(String id, String name) {
        return new Student(new PersonId(id), new ContactDetails(new Name(name)),
                new EducationLevel("S2"), new Phone("00123456"));
    }

    private static ContactDetails createDetails(String name) {
        return new ContactDetails(new Name(name), Optional.of(new Phone("00123456")),
                Optional.empty(), Optional.empty());
    }
}
