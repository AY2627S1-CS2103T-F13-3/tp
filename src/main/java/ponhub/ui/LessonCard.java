package ponhub.ui;

import static java.util.Objects.requireNonNull;

import java.util.Locale;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ponhub.model.attendance.AttendanceStatus;
import ponhub.model.query.LessonDetailView;
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
        lessonHeading.setText(lesson.getSubject().toString());
        lessonIdentity.setText("Lesson " + lesson.getLessonId());
        lessonSchedule.setText(lesson.getTimeSlot().toString());
        lessonTutor.setText(lesson.getTutorName() + " · " + lesson.getTutorId());
        lessonRoom.setText(lesson.getRoom().toString());
        lessonRosterSize.setText(Integer.toString(lesson.getRosterSize()));
        if (lesson instanceof LessonDetailView detail) {
            showDetails(detail);
        }
    }

    private void showDetails(LessonDetailView detail) {
        rosterDetails.setManaged(true);
        rosterDetails.setVisible(true);
        rosterDetails.getChildren().add(label("Current roster", "card_section_heading"));
        if (detail.currentRoster().isEmpty()) {
            rosterDetails.getChildren().add(label("No students currently enrolled.", "card_empty_label"));
        }
        for (var entry : detail.currentRoster()) {
            String status = detail.date().isEmpty() ? null
                    : "Attendance: " + entry.status().map(LessonCard::displayStatus).orElse("unrecorded");
            rosterDetails.getChildren().add(detailEntry(entry.student().getId() + " · "
                    + entry.student().getName().fullName, status));
        }

        formerDetails.setManaged(true);
        formerDetails.setVisible(true);
        formerDetails.getChildren().add(label("Former members' retained attendance", "card_section_heading"));
        if (detail.formerAttendance().isEmpty()) {
            formerDetails.getChildren().add(label("No retained attendance for former members.",
                    "card_empty_label"));
        }
        for (var record : detail.formerAttendance()) {
            formerDetails.getChildren().add(detailEntry(record.student().getId() + " · "
                    + record.student().getName().fullName,
                    "Date: " + record.date() + " · Status: " + displayStatus(record.status())));
        }
    }

    private static String displayStatus(AttendanceStatus status) {
        return status.name().toLowerCase(Locale.ROOT);
    }

    private static VBox detailEntry(String primary, String secondary) {
        VBox entry = new VBox(2);
        entry.getStyleClass().add("card_entry");
        entry.getChildren().add(label(primary, "card_entry_primary"));
        if (secondary != null) {
            entry.getChildren().add(label(secondary, "card_entry_secondary"));
        }
        return entry;
    }

    private static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().addAll("cell_small_label", styleClass);
        return label;
    }
}
