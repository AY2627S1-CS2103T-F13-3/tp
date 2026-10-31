package ponhub.ui;

import static java.util.Objects.requireNonNull;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import ponhub.model.query.LessonView;

/**
 * Displays one immutable shared lesson projection, identified by stable ID rather than a people-list position.
 */
public class LessonCard extends UiPart<Region> {
    private static final String FXML = "LessonCard.fxml";

    @FXML
    private Label lessonHeading;
    @FXML
    private Label lessonIdentity;
    @FXML
    private Label lessonSchedule;
    @FXML
    private Label lessonTutor;
    @FXML
    private Label lessonRoom;
    @FXML
    private Label lessonRosterSize;

    /**
     * Creates a wrapping card with schedule, tutor identity, room and current enrollment count.
     */
    public LessonCard(LessonView lesson) {
        super(FXML);
        requireNonNull(lesson);
        lessonHeading.setText(lesson.getSubject().toString());
        lessonIdentity.setText("Lesson " + lesson.getLessonId());
        lessonSchedule.setText(lesson.getTimeSlot().toString());
        lessonTutor.setText(lesson.getTutorName() + " · " + lesson.getTutorId());
        lessonRoom.setText(lesson.getRoom().toString());
        lessonRosterSize.setText(Integer.toString(lesson.getRosterSize()));
    }
}
