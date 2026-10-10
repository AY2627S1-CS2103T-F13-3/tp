package seedu.address.model;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import seedu.address.model.attendance.Attendance;
import seedu.address.model.attendance.AttendanceKey;
import seedu.address.model.lesson.Lesson;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.person.PeopleRegistryState;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;

/**
 * Immutable complete operational snapshot. Collection order and allocation history participate in equality.
 * A counter of Long.MAX_VALUE represents exhaustion; counters must never be inferred from retained records.
 * Global tutor and room schedule rules are validated here so restored state cannot bypass operational invariants.
 * Attendance-date and student-enrolment rules belong to their feature layers.
 *
 * @param people Ordered people and complete per-role allocation history.
 * @param lessons Ordered shared lessons, each owning its roster.
 * @param attendance Ordered dated records, including former enrolments.
 * @param lastAllocatedLessonSequence Last allocated lesson ID, or zero before the first allocation.
 */
public record PonHubDataState(PeopleRegistryState people, List<Lesson> lessons, List<Attendance> attendance,
        long lastAllocatedLessonSequence) {

    /**
     * Copies incoming collections and validates identities, references, and global schedules before publishing state.
     * Records are immutable and can be shared. Historical attendance does not require current enrolment.
     */
    public PonHubDataState {
        requireNonNull(people);
        lessons = List.copyOf(lessons);
        attendance = List.copyOf(attendance);
        checkArgument(lastAllocatedLessonSequence >= 0, "Lesson allocation history must be nonnegative.");

        Map<PersonId, PersonRecord> peopleById = new HashMap<>();
        people.getPeople().forEach(person -> peopleById.put(person.getId(), person));
        Map<LessonId, Lesson> lessonsById = new HashMap<>();
        for (Lesson lesson : lessons) {
            checkArgument(lessonsById.putIfAbsent(lesson.getId(), lesson) == null, "Duplicate lesson ID.");
            checkArgument(lesson.getId().getSequenceNumber() <= lastAllocatedLessonSequence,
                    "Lesson ID exceeds allocation history.");
            requireRole(peopleById, lesson.getTutorId(), PersonRole.TUTOR);
            for (PersonId student : lesson.getEnrolledStudentIds()) {
                requireRole(peopleById, student, PersonRole.STUDENT);
            }
        }
        Set<AttendanceKey> keys = new HashSet<>();
        for (Attendance record : attendance) {
            checkArgument(keys.add(record.getKey()), "Duplicate attendance key.");
            requireRole(peopleById, record.getStudentId(), PersonRole.STUDENT);
            Lesson lesson = lessonsById.get(record.getLessonId());
            checkArgument(lesson != null, "Attendance refers to a missing lesson.");
        }
        validateLessonSchedules(lessons);
    }

    private static void requireRole(Map<PersonId, PersonRecord> people, PersonId id, PersonRole role) {
        PersonRecord person = people.get(id);
        checkArgument(person != null && person.getRole() == role, "Missing or wrong-role person reference: " + id);
    }

    /**
     * Rejects overlapping bookings for the same tutor or normalized room.
     */
    private static void validateLessonSchedules(List<Lesson> lessons) {
        for (int i = 0; i < lessons.size(); i++) {
            Lesson lesson = lessons.get(i);
            for (int j = 0; j < i; j++) {
                Lesson earlierLesson = lessons.get(j);
                if (!lesson.getTimeSlot().overlaps(earlierLesson.getTimeSlot())) {
                    continue;
                }
                checkArgument(!lesson.getTutorId().equals(earlierLesson.getTutorId()),
                        "Tutor schedule overlap between " + earlierLesson.getId() + " and " + lesson.getId() + ".");
                checkArgument(!lesson.getRoom().equals(earlierLesson.getRoom()),
                        "Room schedule overlap between " + earlierLesson.getId() + " and " + lesson.getId() + ".");
            }
        }
    }
}
