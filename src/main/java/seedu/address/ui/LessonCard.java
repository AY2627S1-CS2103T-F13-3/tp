package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import seedu.address.model.query.LessonView;

/**
 * Displays one immutable shared lesson projection, identified by stable ID rather than a people-list position.
 */
public class LessonCard extends UiPart<Region> {
    private static final String FXML = "LessonCard.fxml";

    @FXML
    private Label lessonHeading;
    @FXML
    private Label lessonSchedule;
    @FXML
    private Label lessonTutor;
    @FXML
    private Label lessonRoom;
    @FXML
    private Label lessonRosterSize;

    /**
     * Creates a wrapping card with schedule, tutor identity, room and current enrolment count.
     */
    public LessonCard(LessonView lesson) {
        super(FXML);
        requireNonNull(lesson);
        lessonHeading.setText(lesson.getLessonId() + " · " + lesson.getSubject());
        lessonSchedule.setText("Schedule: " + lesson.getTimeSlot());
        lessonTutor.setText("Tutor: " + lesson.getTutorName() + " (" + lesson.getTutorId() + ")");
        lessonRoom.setText("Room: " + lesson.getRoom());
        lessonRosterSize.setText("Students enrolled: " + lesson.getRosterSize());
    }
}
