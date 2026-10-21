package ponhub.model.query;

import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import ponhub.model.attendance.AttendanceStatus;
import ponhub.model.lesson.Lesson;
import ponhub.model.lesson.LessonId;
import ponhub.model.lesson.LessonTimeSlot;
import ponhub.model.lesson.Room;
import ponhub.model.lesson.Subject;
import ponhub.model.person.PersonId;
import ponhub.model.person.Student;

/** One immutable shared-lesson detail snapshot with current roster and distinct retained former-member records. */
public record LessonDetailView(Lesson lesson, String tutorName, Optional<LocalDate> date,
        List<RosterStatus> currentRoster, List<FormerAttendance> formerAttendance) implements LessonView {
    /** Copies the two context lists so a displayed detail never changes before canonical publication. */
    public LessonDetailView {
        requireAllNonNull(lesson, tutorName, date, currentRoster, formerAttendance);
        currentRoster = List.copyOf(currentRoster);
        formerAttendance = List.copyOf(formerAttendance);
    }

    @Override
    public LessonId getLessonId() {
        return lesson.getId();
    }

    @Override
    public PersonId getTutorId() {
        return lesson.getTutorId();
    }

    @Override
    public String getTutorName() {
        return tutorName;
    }

    @Override
    public LessonTimeSlot getTimeSlot() {
        return lesson.getTimeSlot();
    }

    @Override
    public Subject getSubject() {
        return lesson.getSubject();
    }

    @Override
    public Room getRoom() {
        return lesson.getRoom();
    }

    @Override
    public int getRosterSize() {
        return currentRoster.size();
    }

    /** A current member has no recorded status when the selected date has no attendance entry. */
    public record RosterStatus(Student student, Optional<AttendanceStatus> status) {
        public RosterStatus {
            requireAllNonNull(student, status);
        }
    }

    /** A retained record for a student who is absent from the current roster. */
    public record FormerAttendance(Student student, LocalDate date, AttendanceStatus status) {
        public FormerAttendance {
            requireAllNonNull(student, date, status);
        }
    }
}
