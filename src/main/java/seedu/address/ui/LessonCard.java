package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import java.util.Locale;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import seedu.address.model.attendance.AttendanceStatus;
import seedu.address.model.query.LessonDetailView;
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
    @FXML
    private VBox rosterDetails;
    @FXML
    private VBox formerDetails;

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
        if (lesson instanceof LessonDetailView detail) {
            showDetails(detail);
        }
    }

    private void showDetails(LessonDetailView detail) {
        rosterDetails.setManaged(true);
        rosterDetails.setVisible(true);
        rosterDetails.getChildren().add(label("Current roster:"));
        if (detail.currentRoster().isEmpty()) {
            rosterDetails.getChildren().add(label("No students currently enrolled."));
        }
        for (var entry : detail.currentRoster()) {
            String status = detail.date().isEmpty() ? ""
                    : " — " + entry.status().map(LessonCard::displayStatus).orElse("unrecorded");
            rosterDetails.getChildren().add(label(entry.student().getId() + " · "
                    + entry.student().getName().fullName + status));
        }

        formerDetails.setManaged(true);
        formerDetails.setVisible(true);
        formerDetails.getChildren().add(label("Former members' retained attendance:"));
        if (detail.formerAttendance().isEmpty()) {
            formerDetails.getChildren().add(label("No retained attendance for former members."));
        }
        for (var record : detail.formerAttendance()) {
            formerDetails.getChildren().add(label(record.date() + " · " + record.student().getId() + " · "
                    + record.student().getName().fullName + " — " + displayStatus(record.status())));
        }
    }

    private static String displayStatus(AttendanceStatus status) {
        return status.name().toLowerCase(Locale.ROOT);
    }

    private static Label label(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().add("cell_small_label");
        return label;
    }
}
