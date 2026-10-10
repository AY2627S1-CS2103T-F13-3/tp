package seedu.address.model;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import seedu.address.model.attendance.Attendance;
import seedu.address.model.attendance.AttendanceKey;
import seedu.address.model.lesson.Lesson;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.lesson.LessonTimeSlot;
import seedu.address.model.lesson.Room;
import seedu.address.model.lesson.Subject;
import seedu.address.model.person.PeopleRegistry;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Student;

/**
 * Canonical operational data root, not yet wired into the active ModelManager or JSON storage.
 * Accepts only validated immutable state at the replacement boundary.
 * Intended for the application's single model thread, not concurrent writers.
 */
public final class PonHubData {
    private static final String MESSAGE_MISSING_LESSON = "The lesson does not exist: ";
    private static final String MESSAGE_MISSING_STUDENT = "The student does not exist: ";
    private static final String MESSAGE_MISSING_TUTOR = "The tutor does not exist: ";

    private PonHubDataState state;

    public PonHubData() {
        this(new PonHubDataState(new PeopleRegistry().exportState(), List.of(), List.of(), 0));
    }

    public PonHubData(PonHubDataState initialState) {
        resetData(initialState);
    }

    public PonHubData(PonHubData other) {
        this(requireNonNull(other).exportState());
    }

    /**
     * Returns a complete immutable snapshot that cannot change with subsequent edits.
     */
    public PonHubDataState exportState() {
        return state;
    }

    /**
     * Restores validated state, including allocation counters. This is a restore boundary, not a normal edit.
     * Immutable state can be shared safely; neither this container nor its caller can change its collections.
     */
    public void resetData(PonHubDataState replacement) {
        state = requireNonNull(replacement);
    }

    /**
     * Restores another container, safely including this container itself.
     */
    public void resetData(PonHubData replacement) {
        resetData(requireNonNull(replacement).exportState());
    }

    public List<PersonRecord> getPeople() {
        return state.people().getPeople();
    }

    public List<Lesson> getLessons() {
        return state.lessons();
    }

    public List<Attendance> getAttendance() {
        return state.attendance();
    }

    /**
     * Returns the attendance record for the supplied key, or an empty optional if absent.
     *
     * @throws NullPointerException if the key is null.
     */
    public Optional<Attendance> getAttendance(AttendanceKey key) {
        requireNonNull(key);
        return getAttendance().stream().filter(record -> record.getKey().equals(key)).findFirst();
    }

    /**
     * Returns the person with the supplied stable ID, or an empty optional if absent.
     *
     * @throws NullPointerException if the ID is null.
     */
    public Optional<PersonRecord> getPerson(PersonId id) {
        requireNonNull(id);
        return getPeople().stream().filter(person -> person.getId().equals(id)).findFirst();
    }

    /**
     * Returns the lesson with the supplied stable ID, or an empty optional if absent.
     *
     * @throws NullPointerException if the ID is null.
     */
    public Optional<Lesson> getLesson(LessonId id) {
        requireNonNull(id);
        return getLessons().stream().filter(lesson -> lesson.getId().equals(id)).findFirst();
    }

    /**
     * Creates an empty-roster lesson for an existing tutor and returns the committed record.
     * The complete candidate state is validated before installation, so a rejected creation changes neither the
     * lesson catalogue nor its allocation history.
     *
     * @throws NullPointerException if an argument is null.
     * @throws IllegalArgumentException if the tutor does not exist or the lesson conflicts with another lesson.
     * @throws IllegalStateException if every positive lesson ID has been allocated.
     */
    public Lesson addLesson(PersonId tutorId, LessonTimeSlot timeSlot, Subject subject, Room room) {
        requireAllNonNull(tutorId, timeSlot, subject, room);
        requirePerson(tutorId, PersonRole.TUTOR, MESSAGE_MISSING_TUTOR);

        LessonId lessonId = getNextLessonId();
        Lesson lesson = new Lesson(lessonId, tutorId, timeSlot, subject, room, Set.of());
        List<Lesson> lessons = new ArrayList<>(getLessons());
        lessons.add(lesson);

        PonHubDataState replacement = new PonHubDataState(state.people(), lessons, state.attendance(),
                lessonId.getSequenceNumber());
        state = replacement;
        return lesson;
    }

    /**
     * Returns the current roster of an existing lesson in global people creation order.
     * The returned list is an immutable snapshot.
     *
     * @throws NullPointerException if the lesson ID is null.
     * @throws IllegalArgumentException if the lesson does not exist.
     */
    public List<Student> getLessonRoster(LessonId lessonId) {
        Lesson lesson = requireLesson(lessonId);
        Set<PersonId> enrolledStudentIds = lesson.getEnrolledStudentIds();
        return getPeople().stream()
                .filter(Student.class::isInstance)
                .map(Student.class::cast)
                .filter(student -> enrolledStudentIds.contains(student.getId()))
                .toList();
    }

    /**
     * Returns an existing student's current lessons in catalogue creation order.
     * The returned list is an immutable snapshot.
     *
     * @throws NullPointerException if the student ID is null.
     * @throws IllegalArgumentException if the student does not exist.
     */
    public List<Lesson> getStudentLessons(PersonId studentId) {
        requirePerson(studentId, PersonRole.STUDENT, MESSAGE_MISSING_STUDENT);
        return getLessons().stream()
                .filter(lesson -> lesson.getEnrolledStudentIds().contains(studentId))
                .toList();
    }

    /**
     * Returns all retained attendance records for an existing student in storage order.
     * The returned list is an immutable snapshot.
     *
     * @throws NullPointerException if the student ID is null.
     * @throws IllegalArgumentException if the student does not exist.
     */
    public List<Attendance> getAttendanceForStudent(PersonId studentId) {
        requirePerson(studentId, PersonRole.STUDENT, MESSAGE_MISSING_STUDENT);
        return getAttendance().stream()
                .filter(record -> record.getStudentId().equals(studentId))
                .toList();
    }

    /**
     * Returns all retained attendance records for an existing lesson in storage order.
     * The returned list is an immutable snapshot.
     *
     * @throws NullPointerException if the lesson ID is null.
     * @throws IllegalArgumentException if the lesson does not exist.
     */
    public List<Attendance> getAttendanceForLesson(LessonId lessonId) {
        requireLesson(lessonId);
        return getAttendance().stream()
                .filter(record -> record.getLessonId().equals(lessonId))
                .toList();
    }

    /**
     * Returns whether canonical lesson or attendance state references the supplied person.
     * This method is suitable for use as the relationship predicate passed to {@link PeopleRegistry#remove}.
     * Students are referenced by roster membership or attendance, tutors by assigned lessons, and parents are not
     * directly referenced by either collection.
     */
    public boolean isPersonReferenced(PersonRecord person) {
        requireNonNull(person);
        PersonId personId = person.getId();
        return switch (person.getRole()) {
            case STUDENT -> getLessons().stream()
                    .anyMatch(lesson -> lesson.getEnrolledStudentIds().contains(personId))
                    || getAttendance().stream().anyMatch(record -> record.getStudentId().equals(personId));
            case TUTOR -> getLessons().stream().anyMatch(lesson -> lesson.getTutorId().equals(personId));
            case PARENT -> false;
        };
    }

    /**
     * Returns whether an existing lesson has roster or attendance references that block deletion.
     *
     * @throws NullPointerException if the lesson ID is null.
     * @throws IllegalArgumentException if the lesson does not exist.
     */
    public boolean isLessonReferenced(LessonId lessonId) {
        Lesson lesson = requireLesson(lessonId);
        return !lesson.getEnrolledStudentIds().isEmpty()
                || getAttendance().stream().anyMatch(record -> record.getLessonId().equals(lessonId));
    }

    /**
     * Returns the next uncommitted lesson ID without changing allocation state.
     */
    private LessonId getNextLessonId() {
        long lastAllocatedSequence = state.lastAllocatedLessonSequence();
        if (lastAllocatedSequence == Long.MAX_VALUE) {
            throw new IllegalStateException(LessonId.MESSAGE_SEQUENCE_EXHAUSTED);
        }
        return LessonId.fromSequenceNumber(lastAllocatedSequence + 1);
    }

    /**
     * Returns the existing person after checking the role required by the caller.
     */
    private PersonRecord requirePerson(PersonId personId, PersonRole role, String missingPersonMessage) {
        requireNonNull(personId);
        Optional<PersonRecord> person = getPerson(personId);
        checkArgument(person.isPresent() && person.get().getRole() == role, missingPersonMessage + personId);
        return person.get();
    }

    /**
     * Returns the existing lesson or rejects the supplied ID.
     */
    private Lesson requireLesson(LessonId lessonId) {
        requireNonNull(lessonId);
        Optional<Lesson> lesson = getLesson(lessonId);
        checkArgument(lesson.isPresent(), MESSAGE_MISSING_LESSON + lessonId);
        return lesson.get();
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof PonHubData data && state.equals(data.state);
    }

    @Override
    public int hashCode() {
        return state.hashCode();
    }
}
