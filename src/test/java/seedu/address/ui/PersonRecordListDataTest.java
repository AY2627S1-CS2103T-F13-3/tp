package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

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

public class PersonRecordListDataTest {

    private final Student firstStudent = createStudent("S9", "Alex Tan");
    private final Tutor tutor = new Tutor(new PersonId("T7"), createDetails("Mei Lim"));
    private final Parent parent = new Parent(new PersonId("P2"), createDetails("Pat Tan"));
    private final Student secondStudent = createStudent("S3", "Jamie Tan");
    private final List<PersonRecord> mixedPeople = List.of(firstStudent, tutor, parent, secondStudent);

    @Test
    public void constructor_allRoles_preservesGlobalOrderAndStableIds() {
        PersonRecordListData data = new PersonRecordListData(mixedPeople, Optional.empty());

        assertEquals(Optional.empty(), data.getRoleFilter());
        assertEquals(4, data.getCount());
        assertEquals("Listed 4 person(s).", data.getSummary());
        assertEntries(data, mixedPeople);
        assertEquals(List.of(new PersonId("S9"), new PersonId("T7"), new PersonId("P2"), new PersonId("S3")),
                data.getEntries().stream().map(entry -> entry.person().getId()).toList());
    }

    @Test
    public void constructor_studentFilter_preservesOrderAndRenumbersDisplayPositions() {
        PersonRecordListData all = new PersonRecordListData(mixedPeople, Optional.empty());
        PersonRecordListData students = new PersonRecordListData(mixedPeople, Optional.of(PersonRole.STUDENT));

        assertEquals(Optional.of(PersonRole.STUDENT), students.getRoleFilter());
        assertEquals(2, students.getCount());
        assertEquals("Listed 2 person(s) with role: student.", students.getSummary());
        assertEntries(students, List.of(firstStudent, secondStudent));
        assertEquals(4, all.getEntries().get(3).displayedIndex());
        assertEquals(2, students.getEntries().get(1).displayedIndex());
        assertSame(all.getEntries().get(3).person(), students.getEntries().get(1).person());
        assertEquals(new PersonId("S3"), students.getEntries().get(1).person().getId());
    }

    @Test
    public void constructor_singlePersonAndRoleFilters_reportsCountAndSelectedRole() {
        PersonRecordListData single = new PersonRecordListData(List.of(parent), Optional.empty());
        assertEntries(single, List.of(parent));
        assertEquals("Listed 1 person(s).", single.getSummary());

        PersonRecordListData tutors = new PersonRecordListData(mixedPeople, Optional.of(PersonRole.TUTOR));
        assertEntries(tutors, List.of(tutor));
        assertEquals("Listed 1 person(s) with role: tutor.", tutors.getSummary());

        PersonRecordListData parents = new PersonRecordListData(mixedPeople, Optional.of(PersonRole.PARENT));
        assertEntries(parents, List.of(parent));
        assertEquals("Listed 1 person(s) with role: parent.", parents.getSummary());

        PersonRecordListData students = new PersonRecordListData(List.of(firstStudent),
                Optional.of(PersonRole.STUDENT));
        assertEquals("Listed 1 person(s) with role: student.", students.getSummary());
    }

    @Test
    public void constructor_emptyOrNoMatchingRole_returnsEmptyProjection() {
        PersonRecordListData empty = new PersonRecordListData(List.of(), Optional.empty());
        assertTrue(empty.getEntries().isEmpty());
        assertEquals(0, empty.getCount());
        assertEquals("Listed 0 person(s). No persons to display.", empty.getSummary());

        for (PersonRole role : PersonRole.values()) {
            PersonRecordListData filtered = new PersonRecordListData(List.of(), Optional.of(role));
            assertEquals(Optional.of(role), filtered.getRoleFilter());
            assertTrue(filtered.getEntries().isEmpty());
            assertEquals(0, filtered.getCount());
            assertEquals("Listed 0 person(s) with role: " + role.getValue() + ". No persons to display.",
                    filtered.getSummary());
        }

        PersonRecordListData noStudents = new PersonRecordListData(List.of(tutor, parent),
                Optional.of(PersonRole.STUDENT));
        assertTrue(noStudents.getEntries().isEmpty());
        assertEquals(0, noStudents.getCount());
        assertEquals("Listed 0 person(s) with role: student. No persons to display.", noStudents.getSummary());
    }

    @Test
    public void constructor_mutableSource_createsIndependentUnmodifiableSnapshot() {
        List<PersonRecord> sourcePeople = new ArrayList<>(mixedPeople);
        PersonRecordListData data = new PersonRecordListData(sourcePeople, Optional.of(PersonRole.STUDENT));

        assertEquals(mixedPeople, sourcePeople);
        assertEntries(data, List.of(firstStudent, secondStudent));
        sourcePeople.clear();
        sourcePeople.add(parent);
        assertEntries(data, List.of(firstStudent, secondStudent));
        assertEquals(2, data.getCount());
        assertThrows(UnsupportedOperationException.class, () -> data.getEntries().clear());
        assertThrows(UnsupportedOperationException.class, () -> data.getEntries().add(
                new PersonRecordListData.Entry(parent, 3)));
        assertThrows(UnsupportedOperationException.class, () -> data.getEntries().set(0,
                new PersonRecordListData.Entry(parent, 1)));
    }

    @Test
    public void constructor_nullInputs_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new PersonRecordListData(null, Optional.empty()));
        assertThrows(NullPointerException.class, () -> new PersonRecordListData(mixedPeople, null));
        List<PersonRecord> peopleWithNull = new ArrayList<>(mixedPeople);
        peopleWithNull.add(null);
        assertThrows(NullPointerException.class, () -> new PersonRecordListData(peopleWithNull, Optional.empty()));
    }

    @Test
    public void constructor_duplicateStableIds_rejectsBeforeFiltering() {
        Student sameIdDifferentFields = createStudent("S9", "Another Student");
        assertThrows(IllegalArgumentException.class, () -> new PersonRecordListData(
                List.of(firstStudent, sameIdDifferentFields), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new PersonRecordListData(
                List.of(firstStudent, sameIdDifferentFields), Optional.of(PersonRole.TUTOR)));
    }

    @Test
    public void constructor_unsupportedRecord_rejectsEvenWhenFilteredOut() {
        PersonRecord unsupported = new PersonRecord() {
            @Override
            public PersonId getId() {
                return new PersonId("T1");
            }

            @Override
            public ContactDetails getContactDetails() {
                return createDetails("Unsupported Tutor");
            }

            @Override
            public boolean isDuplicateOf(PersonRecord other) {
                return false;
            }
        };

        assertThrows(IllegalArgumentException.class, () -> new PersonRecordListData(
                List.of(firstStudent, unsupported), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new PersonRecordListData(
                List.of(firstStudent, unsupported), Optional.of(PersonRole.STUDENT)));
    }

    @Test
    public void entry_nullPersonOrNonpositiveIndex_rejectsInvalidDisplayData() {
        assertThrows(NullPointerException.class, () -> new PersonRecordListData.Entry(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new PersonRecordListData.Entry(firstStudent, 0));
        assertThrows(IllegalArgumentException.class, () -> new PersonRecordListData.Entry(firstStudent, -1));
    }

    private static void assertEntries(PersonRecordListData data, List<PersonRecord> expectedPeople) {
        assertEquals(expectedPeople.size(), data.getEntries().size());
        for (int i = 0; i < expectedPeople.size(); i++) {
            assertSame(expectedPeople.get(i), data.getEntries().get(i).person());
            assertEquals(i + 1, data.getEntries().get(i).displayedIndex());
        }
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
