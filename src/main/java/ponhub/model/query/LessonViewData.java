package ponhub.model.query;

import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import ponhub.model.lesson.Lesson;
import ponhub.model.lesson.LessonId;
import ponhub.model.lesson.LessonTimeSlot;
import ponhub.model.lesson.Room;
import ponhub.model.lesson.Subject;
import ponhub.model.person.PersonId;

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
