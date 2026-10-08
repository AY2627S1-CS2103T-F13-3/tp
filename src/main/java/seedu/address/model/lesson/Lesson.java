package seedu.address.model.lesson;

import static seedu.address.commons.util.AppUtil.checkArgument;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRole;

/**
 * An immutable shared recurring lesson and its current student roster.
 * The roster is owned only by this lesson; student records do not copy lesson membership.
 */
public final class Lesson {

    public static final String MESSAGE_TUTOR_ID_CONSTRAINTS = "Lessons require a tutor person ID.";
    public static final String MESSAGE_STUDENT_ID_CONSTRAINTS = "Lesson rosters require student person IDs.";

    private final LessonId id;
    private final PersonId tutorId;
    private final LessonTimeSlot timeSlot;
    private final Subject subject;
    private final Room room;
    private final Set<PersonId> enrolledStudentIds;

    /**
     * Creates a shared lesson with an immutable snapshot of its current roster.
     *
     * @throws NullPointerException if a required value, the roster, or a roster entry is null
     * @throws IllegalArgumentException if the tutor ID or a roster ID has the wrong role
     */
    public Lesson(LessonId id, PersonId tutorId, LessonTimeSlot timeSlot, Subject subject, Room room,
                  Set<PersonId> enrolledStudentIds) {
        requireAllNonNull(id, tutorId, timeSlot, subject, room, enrolledStudentIds);
        checkArgument(tutorId.getRole() == PersonRole.TUTOR, MESSAGE_TUTOR_ID_CONSTRAINTS);

        LinkedHashSet<PersonId> rosterCopy = new LinkedHashSet<>();
        for (PersonId studentId : enrolledStudentIds) {
            requireAllNonNull(studentId);
            checkArgument(studentId.getRole() == PersonRole.STUDENT, MESSAGE_STUDENT_ID_CONSTRAINTS);
            rosterCopy.add(studentId);
        }

        this.id = id;
        this.tutorId = tutorId;
        this.timeSlot = timeSlot;
        this.subject = subject;
        this.room = room;
        this.enrolledStudentIds = Collections.unmodifiableSet(rosterCopy);
    }

    /**
     * Creates an independent lesson value with the same persisted fields and roster.
     */
    public Lesson(Lesson other) {
        this(other.id, other.tutorId, other.timeSlot, other.subject, other.room, other.enrolledStudentIds);
    }

    public LessonId getId() {
        return id;
    }

    public PersonId getTutorId() {
        return tutorId;
    }

    public LessonTimeSlot getTimeSlot() {
        return timeSlot;
    }

    public Subject getSubject() {
        return subject;
    }

    public Room getRoom() {
        return room;
    }

    /**
     * Returns the immutable roster snapshot retained by this lesson.
     */
    public Set<PersonId> getEnrolledStudentIds() {
        return enrolledStudentIds;
    }

    /**
     * Returns whether the supplied student is currently in this lesson's roster.
     */
    public boolean hasEnrolledStudent(PersonId studentId) {
        requireAllNonNull(studentId);
        checkArgument(studentId.getRole() == PersonRole.STUDENT, MESSAGE_STUDENT_ID_CONSTRAINTS);
        return enrolledStudentIds.contains(studentId);
    }

    /**
     * Returns a replacement lesson with the supplied complete roster and the same stable identity and schedule.
     */
    public Lesson withEnrolledStudentIds(Set<PersonId> replacementStudentIds) {
        return new Lesson(id, tutorId, timeSlot, subject, room, replacementStudentIds);
    }

    /**
     * Returns a replacement lesson containing the supplied student. This lesson remains unchanged.
     */
    public Lesson withEnrolledStudent(PersonId studentId) {
        requireAllNonNull(studentId);
        checkArgument(studentId.getRole() == PersonRole.STUDENT, MESSAGE_STUDENT_ID_CONSTRAINTS);
        LinkedHashSet<PersonId> replacementStudentIds = new LinkedHashSet<>(enrolledStudentIds);
        replacementStudentIds.add(studentId);
        return withEnrolledStudentIds(replacementStudentIds);
    }

    /**
     * Returns a replacement lesson without the supplied student. This lesson remains unchanged.
     */
    public Lesson withoutEnrolledStudent(PersonId studentId) {
        requireAllNonNull(studentId);
        checkArgument(studentId.getRole() == PersonRole.STUDENT, MESSAGE_STUDENT_ID_CONSTRAINTS);
        LinkedHashSet<PersonId> replacementStudentIds = new LinkedHashSet<>(enrolledStudentIds);
        replacementStudentIds.remove(studentId);
        return withEnrolledStudentIds(replacementStudentIds);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Lesson otherLesson)) {
            return false;
        }
        return id.equals(otherLesson.id)
                && tutorId.equals(otherLesson.tutorId)
                && timeSlot.equals(otherLesson.timeSlot)
                && subject.equals(otherLesson.subject)
                && room.equals(otherLesson.room)
                && enrolledStudentIds.equals(otherLesson.enrolledStudentIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tutorId, timeSlot, subject, room, enrolledStudentIds);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("id", id)
                .add("tutorId", tutorId)
                .add("timeSlot", timeSlot)
                .add("subject", subject)
                .add("room", room)
                .add("enrolledStudentIds", enrolledStudentIds)
                .toString();
    }
}
