package seedu.address.model.query;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import seedu.address.model.lesson.Lesson;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.lesson.LessonTimeSlot;
import seedu.address.model.lesson.Room;
import seedu.address.model.lesson.Subject;
import seedu.address.model.person.PersonId;

/** Immutable lesson projection capturing one validated record and its current tutor name. */
final class LessonViewData implements LessonView {
    private final Lesson lesson;
    private final String tutorName;

    LessonViewData(Lesson lesson, String tutorName) {
        requireAllNonNull(lesson, tutorName);
        this.lesson = lesson;
        this.tutorName = tutorName;
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
        return lesson.getEnrolledStudentIds().size();
    }
}
