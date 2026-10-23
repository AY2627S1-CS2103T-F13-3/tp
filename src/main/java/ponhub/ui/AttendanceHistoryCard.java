package ponhub.ui;

import static java.util.Objects.requireNonNull;

import java.util.Locale;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import ponhub.model.query.AttendanceHistoryEntry;

/** Displays one immutable dated attendance record with its stable identities and membership context. */
public class AttendanceHistoryCard extends UiPart<Region> {
    private static final String FXML = "AttendanceHistoryCard.fxml";

    @FXML
    private Label attendanceDate;
    @FXML
    private Label attendanceIdentity;
    @FXML
    private Label attendanceStatus;
    @FXML
    private Label attendanceMembership;

    /** Creates a read-only card for one recorded attendance entry. */
    public AttendanceHistoryCard(AttendanceHistoryEntry entry) {
        super(FXML);
        requireNonNull(entry);
        attendanceDate.setText(entry.getDate().toString());
        attendanceIdentity.setText("Student " + entry.getStudentId() + " · Lesson " + entry.getLessonId());
        attendanceStatus.setText(entry.getStatus().name().toLowerCase(Locale.ROOT));
        attendanceMembership.setText(entry.isCurrentlyEnrolled() ? "currently enrolled" : "former enrolment");
    }
}
