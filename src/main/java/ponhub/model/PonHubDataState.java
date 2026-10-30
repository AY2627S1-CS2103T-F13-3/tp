package ponhub.model;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.AppUtil.checkArgument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import ponhub.model.attendance.Attendance;
import ponhub.model.attendance.AttendanceKey;
import ponhub.model.lesson.Lesson;
import ponhub.model.lesson.LessonDay;
import ponhub.model.lesson.LessonId;
import ponhub.model.lesson.LessonTime;
import ponhub.model.person.PeopleRegistryState;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.PersonRole;

/**
 * Immutable complete operational snapshot. Collection order and allocation history participate in equality.
 * A counter of Long.MAX_VALUE represents exhaustion; counters must never be inferred from retained records.
 * Global tutor, room and student schedule rules are validated here so restored state cannot bypass
 * operational invariants.
 * Attendance dates must match their lesson weekday and fit the persisted YYYY-MM-DD format.
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
            checkArgument(record.getDate().getYear() >= 1 && record.getDate().getYear() <= 9999,
                    "Attendance date must have a year between 0001 and 9999.");
            checkArgument(lesson.getTimeSlot().getDay().matches(record.getDate()),
                    "Attendance date does not match lesson weekday.");
        }
        validateLessonSchedules(lessons);
    }

    private static void requireRole(Map<PersonId, PersonRecord> people, PersonId id, PersonRole role) {
        PersonRecord person = people.get(id);
        checkArgument(person != null && person.getRole() == role, "Missing or wrong-role person reference: " + id);
    }

    /**
     * Checks only bookings sharing a weekday and resource, preserving creation-order conflict selection.
     * Earlier bookings are conflict-free, so only the predecessor and starts within the new slot can overlap.
     */
    private static void validateLessonSchedules(List<Lesson> lessons) {
        Map<ScheduleKey, NavigableMap<LessonTime, Booking>> schedules = new HashMap<>();
        for (int index = 0; index < lessons.size(); index++) {
            Lesson lesson = lessons.get(index);
            LessonTime start = lesson.getTimeSlot().getStartTime();
            LessonTime end = lesson.getTimeSlot().getEndTime();
            List<Object> resources = new ArrayList<>(lesson.getEnrolledStudentIds());
            resources.add(lesson.getTutorId());
            resources.add(lesson.getRoom());
            List<NavigableMap<LessonTime, Booking>> currentSchedules = new ArrayList<>();
            Booking firstConflict = null;
            for (Object resource : resources) {
                var bookings = schedules.computeIfAbsent(new ScheduleKey(lesson.getTimeSlot().getDay(), resource),
                        unused -> new TreeMap<>());
                currentSchedules.add(bookings);
                var predecessor = bookings.lowerEntry(start);
                if (predecessor != null
                        && predecessor.getValue().lesson().getTimeSlot().getEndTime().compareTo(start) > 0) {
                    firstConflict = earlier(firstConflict, predecessor.getValue());
                }
                for (Booking overlapping : bookings.subMap(start, true, end, false).values()) {
                    firstConflict = earlier(firstConflict, overlapping);
                }
            }
            if (firstConflict != null) {
                rejectOverlap(firstConflict.lesson(), lesson);
            }
            Booking current = new Booking(lesson, index);
            currentSchedules.forEach(bookings -> bookings.put(start, current));
        }
    }

    /**
     * Selects the first earlier lesson in canonical creation order, independently of time/resource order.
     */
    private static Booking earlier(Booking first, Booking candidate) {
        return first == null || candidate.index() < first.index() ? candidate : first;
    }

    /**
     * Retains the existing Tutor, room, then roster-order Student diagnostic precedence for a conflicting pair.
     */
    private static void rejectOverlap(Lesson earlier, Lesson lesson) {
        if (lesson.getTutorId().equals(earlier.getTutorId())) {
            throw new IllegalArgumentException("Tutor schedule overlap between " + earlier.getId()
                    + " and " + lesson.getId() + ".");
        }
        if (lesson.getRoom().equals(earlier.getRoom())) {
            throw new IllegalArgumentException("Room schedule overlap between " + earlier.getId()
                    + " and " + lesson.getId() + ".");
        }
        for (PersonId student : lesson.getEnrolledStudentIds()) {
            if (earlier.hasEnrolledStudent(student)) {
                throw new IllegalArgumentException("Student " + student + " schedule overlap between "
                        + earlier.getId() + " and " + lesson.getId() + ".");
            }
        }
    }

    /**
     * Groups a weekday's bookings by normalized Room or role-qualified PersonId; those types cannot alias.
     *
     * @param day Booking weekday.
     * @param resource Immutable Room or PersonId identifying the booked resource.
     */
    private record ScheduleKey(LessonDay day, Object resource) {
    }

    /**
     * Retains the source position so diagnostic selection does not depend on chronological sorting.
     *
     * @param lesson Previously validated lesson.
     * @param index Position in the canonical lesson list.
     */
    private record Booking(Lesson lesson, int index) {
    }
}
