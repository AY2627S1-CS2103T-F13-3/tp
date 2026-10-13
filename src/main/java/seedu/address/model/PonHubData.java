package seedu.address.model;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import seedu.address.model.attendance.Attendance;
import seedu.address.model.attendance.AttendanceKey;
import seedu.address.model.attendance.AttendanceStatus;
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
 * Canonical operational data root used by the active session and protected JSON storage.
 * Accepts only validated immutable state at the replacement boundary.
 * Intended for the application's single model thread, not concurrent writers.
 */
public final class PonHubData {
    private static final String MESSAGE_MISSING_LESSON = "The lesson does not exist: ";
    private static final String MESSAGE_MISSING_STUDENT = "The student does not exist: ";
    private static final String MESSAGE_MISSING_TUTOR = "The tutor does not exist: ";

    private final ReadOnlyObjectWrapper<PonHubDataState> state = new ReadOnlyObjectWrapper<>();

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
        return state.get();
    }

    /**
     * Observes complete, validated state publication without exposing a writable property.
     * Query projections refresh here after a committed change or restore; staged changes use another container.
     */
    public ReadOnlyObjectProperty<PonHubDataState> stateProperty() {
        return state.getReadOnlyProperty();
    }

    /**
     * Restores validated state, including allocation counters. This is a restore boundary, not a normal edit.
     * Immutable state can be shared safely; neither this container nor its caller can change its collections.
     */
    public void resetData(PonHubDataState replacement) {
        state.set(requireNonNull(replacement));
    }

    /**
     * Restores another container, safely including this container itself.
     */
    public void resetData(PonHubData replacement) {
        resetData(requireNonNull(replacement).exportState());
    }

    public List<PersonRecord> getPeople() {
        return state.get().people().getPeople();
    }

    public List<Lesson> getLessons() {
        return state.get().lessons();
    }

    public List<Attendance> getAttendance() {
        return state.get().attendance();
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
     * Removes an unreferenced person while retaining every allocation counter and surviving relationship.
     *
     * @return the removed immutable record.
     * @throws NullPointerException if the ID is null.
     * @throws IllegalArgumentException if the person is missing or has lesson or attendance references.
     */
    public PersonRecord removePerson(PersonId id) {
        PersonRecord person = getPerson(id).orElseThrow(() ->
                new IllegalArgumentException("The person does not exist: " + id));
        Optional<String> blocker = getPersonDeletionBlocker(person);
        checkArgument(blocker.isEmpty(), blocker.orElse(""));
        PeopleRegistry people = new PeopleRegistry(state.get().people());
        people.remove(id, this::isPersonReferenced);
        PonHubDataState replacement = new PonHubDataState(people.exportState(), getLessons(), getAttendance(),
                state.get().lastAllocatedLessonSequence());
        state.set(replacement);
        return person;
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

        PonHubDataState replacement = new PonHubDataState(state.get().people(), lessons, state.get().attendance(),
                lessonId.getSequenceNumber());
        state.set(replacement);
        return lesson;
    }

    /**
     * Adds an existing student to one existing lesson without changing either stable identity.
     * The complete replacement state is validated before publication, including student schedule clashes.
     *
     * @throws NullPointerException if an argument is null.
     * @throws IllegalArgumentException if either record is missing, the person is not a Student, the membership
     *     already exists, or the resulting student schedule overlaps.
     */
    public Lesson enrolStudent(PersonId studentId, LessonId lessonId) {
        requireAllNonNull(studentId, lessonId);
        requirePerson(studentId, PersonRole.STUDENT, MESSAGE_MISSING_STUDENT);
        Lesson lesson = requireLesson(lessonId);
        checkArgument(!lesson.hasEnrolledStudent(studentId),
                "Student " + studentId + " is already enrolled in lesson " + lessonId + ".");
        return replaceLesson(lesson.withEnrolledStudent(studentId));
    }

    /**
     * Removes an existing student's current membership while retaining the lesson and all dated attendance.
     *
     * @throws NullPointerException if an argument is null.
     * @throws IllegalArgumentException if either record is missing, the person is not a Student, or the membership
     *     does not exist.
     */
    public Lesson unenrolStudent(PersonId studentId, LessonId lessonId) {
        requireAllNonNull(studentId, lessonId);
        requirePerson(studentId, PersonRole.STUDENT, MESSAGE_MISSING_STUDENT);
        Lesson lesson = requireLesson(lessonId);
        checkArgument(lesson.hasEnrolledStudent(studentId),
                "Student " + studentId + " is not enrolled in lesson " + lessonId + ".");
        return replaceLesson(lesson.withoutEnrolledStudent(studentId));
    }

    /**
     * Creates or corrects one dated attendance record. A new record requires current lesson membership, while an
     * existing record can be corrected after unenrolment. Repeating the existing status changes no state.
     *
     * @return the created, corrected or unchanged attendance record.
     * @throws NullPointerException if an argument is null.
     * @throws IllegalArgumentException if a referenced record is missing, the date has the wrong weekday, or a new
     *     record is requested for a Student who is not currently enrolled.
     */
    public Attendance markAttendance(PersonId studentId, LessonId lessonId, LocalDate date,
            AttendanceStatus status) {
        requireAllNonNull(studentId, lessonId, date, status);
        requirePerson(studentId, PersonRole.STUDENT, MESSAGE_MISSING_STUDENT);
        Lesson lesson = requireAttendanceLesson(lessonId, date);

        AttendanceKey key = new AttendanceKey(studentId, lessonId, date);
        Optional<Attendance> existing = getAttendance(key);
        if (existing.isPresent() && existing.get().getStatus() == status) {
            return existing.get();
        }
        checkArgument(existing.isPresent() || lesson.hasEnrolledStudent(studentId),
                "Student " + studentId + " must be enrolled in lesson " + lessonId
                        + " before new attendance can be recorded.");

        Attendance replacement = existing.map(record -> record.withStatus(status))
                .orElseGet(() -> new Attendance(key, status));
        List<Attendance> attendance = new ArrayList<>(getAttendance());
        if (existing.isPresent()) {
            attendance.set(attendance.indexOf(existing.get()), replacement);
        } else {
            attendance.add(replacement);
        }
        PonHubDataState replacementState = new PonHubDataState(state.get().people(), state.get().lessons(),
                attendance, state.get().lastAllocatedLessonSequence());
        state.set(replacementState);
        return replacement;
    }

    /**
     * Removes one dated attendance record without changing the Student, Lesson or current membership.
     * An existing historical record can be removed after unenrolment. A missing record changes no state.
     *
     * @return the removed record, or an empty optional if the attendance key was not recorded.
     * @throws NullPointerException if an argument is null.
     * @throws IllegalArgumentException if a referenced record is missing or the date has the wrong weekday.
     */
    public Optional<Attendance> unmarkAttendance(PersonId studentId, LessonId lessonId, LocalDate date) {
        requireAllNonNull(studentId, lessonId, date);
        requirePerson(studentId, PersonRole.STUDENT, MESSAGE_MISSING_STUDENT);
        requireAttendanceLesson(lessonId, date);

        AttendanceKey key = new AttendanceKey(studentId, lessonId, date);
        Optional<Attendance> existing = getAttendance(key);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        List<Attendance> attendance = new ArrayList<>(getAttendance());
        attendance.remove(existing.get());
        PonHubDataState replacementState = new PonHubDataState(state.get().people(), state.get().lessons(),
                attendance, state.get().lastAllocatedLessonSequence());
        state.set(replacementState);
        return existing;
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
        return getPersonDeletionBlocker(person).isPresent();
    }

    /**
     * Identifies the first retained dependency that prevents deleting a person.
     */
    private Optional<String> getPersonDeletionBlocker(PersonRecord person) {
        requireNonNull(person);
        PersonId personId = person.getId();
        if (person.getRole() == PersonRole.STUDENT) {
            Optional<Lesson> enrolment = getLessons().stream()
                    .filter(lesson -> lesson.hasEnrolledStudent(personId)).findFirst();
            if (enrolment.isPresent()) {
                return Optional.of("Student " + personId + " cannot be deleted while enrolled in lesson "
                        + enrolment.get().getId() + ". Unenrol the student first.");
            }
            Optional<Attendance> attendance = getAttendance().stream()
                    .filter(record -> record.getStudentId().equals(personId)).findFirst();
            if (attendance.isPresent()) {
                Attendance record = attendance.get();
                return Optional.of("Student " + personId + " cannot be deleted while retained attendance exists for "
                        + "lesson " + record.getLessonId() + " on " + record.getDate() + ".");
            }
        } else if (person.getRole() == PersonRole.TUTOR) {
            Optional<Lesson> assignedLesson = getLessons().stream()
                    .filter(lesson -> lesson.getTutorId().equals(personId)).findFirst();
            if (assignedLesson.isPresent()) {
                return Optional.of("Tutor " + personId + " cannot be deleted while assigned to lesson "
                        + assignedLesson.get().getId() + ". Empty lessons also block deletion.");
            }
        }
        return Optional.empty();
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
        long lastAllocatedSequence = state.get().lastAllocatedLessonSequence();
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

    /**
     * Returns an existing lesson after checking that the supplied attendance date matches its recurring weekday.
     */
    private Lesson requireAttendanceLesson(LessonId lessonId, LocalDate date) {
        requireNonNull(date);
        Lesson lesson = requireLesson(lessonId);
        checkArgument(date.getDayOfWeek().name().equals(lesson.getTimeSlot().getDay().name()),
                "Attendance date " + date + " does not match lesson " + lessonId + "'s "
                        + lesson.getTimeSlot().getDay() + " schedule.");
        return lesson;
    }

    /**
     * Installs one replacement lesson at its existing catalogue position after validating the complete snapshot.
     */
    private Lesson replaceLesson(Lesson replacement) {
        requireNonNull(replacement);
        List<Lesson> lessons = new ArrayList<>(getLessons());
        int lessonIndex = lessons.indexOf(requireLesson(replacement.getId()));
        lessons.set(lessonIndex, replacement);
        PonHubDataState replacementState = new PonHubDataState(state.get().people(), lessons,
                state.get().attendance(), state.get().lastAllocatedLessonSequence());
        state.set(replacementState);
        return replacement;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof PonHubData data && state.get().equals(data.state.get());
    }

    @Override
    public int hashCode() {
        return state.get().hashCode();
    }
}
