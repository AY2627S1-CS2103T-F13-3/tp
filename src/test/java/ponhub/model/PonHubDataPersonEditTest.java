package ponhub.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import ponhub.model.attendance.Attendance;
import ponhub.model.attendance.AttendanceStatus;
import ponhub.model.lesson.Lesson;
import ponhub.model.lesson.LessonDay;
import ponhub.model.lesson.LessonId;
import ponhub.model.lesson.LessonTime;
import ponhub.model.lesson.LessonTimeSlot;
import ponhub.model.lesson.Room;
import ponhub.model.lesson.Subject;
import ponhub.model.person.ContactDetails;
import ponhub.model.person.EducationLevel;
import ponhub.model.person.Name;
import ponhub.model.person.Parent;
import ponhub.model.person.PeopleRegistry;
import ponhub.model.person.PeopleRegistryState;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.PersonRole;
import ponhub.model.person.Phone;
import ponhub.model.person.Student;
import ponhub.model.person.Tutor;
import ponhub.model.person.exceptions.DuplicatePersonException;

public class PonHubDataPersonEditTest {

    @Test
    public void replacePerson_eachRole_retainsPositionRelationshipsAndCounters() {
        PonHubDataState source = sourceWithHistory();
        for (PersonRecord original : source.people().getPeople()) {
            PonHubData data = new PonHubData(source);
            PersonRecord replacement = renamed(original, "Changed Name");
            List<PersonRecord> expectedPeople = new ArrayList<>(source.people().getPeople());
            expectedPeople.set(expectedPeople.indexOf(original), replacement);
            assertSame(replacement, data.replacePerson(replacement));
            assertEquals(expectedPeople, data.getPeople());
            assertEquals(source.people().getLastAllocatedSequences(),
                    data.exportState().people().getLastAllocatedSequences());
            assertEquals(source.lessons(), data.getLessons());
            assertEquals(source.attendance(), data.getAttendance());
            assertEquals(source.lastAllocatedLessonSequence(), data.exportState().lastAllocatedLessonSequence());
            assertNotEquals(source, data.exportState());
            assertEquals(original, source.people().getPeople().get(expectedPeople.indexOf(replacement)));
        }
    }

    @Test
    public void replacePerson_equalAndChangedRecords_publishesOnlyOneCompleteChangedSnapshot() {
        PonHubDataState source = sourceWithHistory();
        PonHubData data = new PonHubData(source);
        AtomicInteger notifications = new AtomicInteger();
        data.stateProperty().addListener((observable, oldState, newState) -> {
            notifications.incrementAndGet();
            assertSame(source, oldState);
            assertSame(data.exportState(), newState);
            assertEquals(source.lessons(), newState.lessons());
            assertEquals(source.attendance(), newState.attendance());
        });
        PersonRecord original = source.people().getPeople().getFirst();
        assertSame(original, data.replacePerson(renamed(original, original.getName().fullName)));
        assertSame(source, data.exportState());
        assertEquals(0, notifications.get());
        PersonRecord replacement = renamed(original, "TUTOR");
        assertSame(replacement, data.replacePerson(replacement));
        assertEquals(1, notifications.get());
        assertSame(replacement, data.getPeople().getFirst());
    }

    @Test
    public void replacePerson_nullMissingDuplicateAndUnsupported_preservesInstalledSnapshot() {
        PonHubDataState source = sourceWithHistory();
        PonHubData data = new PonHubData(source);
        AtomicInteger notifications = new AtomicInteger();
        data.stateProperty().addListener((observable, oldState, newState) -> notifications.incrementAndGet());
        assertThrows(NullPointerException.class, () -> data.replacePerson(null));
        assertThrows(IllegalArgumentException.class, () -> data.replacePerson(
                new Tutor(new PersonId("T99"), contacts("Missing", "00123456"))));
        Tutor duplicate = new Tutor(new PersonId("T1"), contacts("Other Tutor", "00987654"));
        assertThrows(DuplicatePersonException.class, () -> data.replacePerson(duplicate));
        PersonRecord unsupported = new PersonRecord() {
            @Override
            public PersonId getId() {
                return new PersonId("S1");
            }

            @Override
            public ContactDetails getContactDetails() {
                return new ContactDetails(new Name("Unsupported"));
            }

            @Override
            public boolean isDuplicateOf(PersonRecord other) {
                return false;
            }
        };
        assertThrows(IllegalArgumentException.class, () -> data.replacePerson(unsupported));
        assertSame(source, data.exportState());
        assertEquals(0, notifications.get());
    }

    private static PersonRecord renamed(PersonRecord original, String name) {
        ContactDetails oldContacts = original.getContactDetails();
        ContactDetails contacts = new ContactDetails(new Name(name), oldContacts.getPhone(),
                oldContacts.getEmail(), oldContacts.getAddress());
        return switch (original) {
            case Student student -> new Student(student.getId(), contacts, student.getLevel(),
                    student.getParentPhone());
            case Tutor tutor -> new Tutor(tutor.getId(), contacts);
            case Parent parent -> new Parent(parent.getId(), contacts);
            default -> throw new IllegalArgumentException("Unsupported test record.");
        };
    }

    private static PonHubDataState sourceWithHistory() {
        PeopleRegistry people = new PeopleRegistry();
        Tutor tutor = people.addTutor(contacts("Tutor", "00123456"));
        Student student = people.addStudent(new ContactDetails(new Name("Student")),
                new EducationLevel("P1"), new Phone("00012345"));
        people.addParent(contacts("Parent", "00012345"));
        people.addTutor(contacts("Other Tutor", "00987654"));
        Lesson lesson = new Lesson(new LessonId("L1"), tutor.getId(), new LessonTimeSlot(LessonDay.MONDAY,
                new LessonTime("0900"), new LessonTime("1000")), new Subject("Math"), new Room("R1"),
                Set.of(student.getId()));
        Attendance attendance = new Attendance(student.getId(), lesson.getId(), LocalDate.of(2026, 10, 5),
                AttendanceStatus.PRESENT);
        PeopleRegistryState peopleState = new PeopleRegistryState(people.exportState().getPeople(),
                Map.of(PersonRole.STUDENT, 20L, PersonRole.TUTOR, Long.MAX_VALUE, PersonRole.PARENT, 8L));
        return new PonHubDataState(peopleState, List.of(lesson), List.of(attendance), 30);
    }

    private static ContactDetails contacts(String name, String phone) {
        return new ContactDetails(new Name(name), Optional.of(new Phone(phone)), Optional.empty(), Optional.empty());
    }
}
