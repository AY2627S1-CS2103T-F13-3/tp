package seedu.address.logic;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.HistoryArgumentsParser.Input;
import seedu.address.model.PeopleView;
import seedu.address.model.PonHubData;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.person.PersonId;
import seedu.address.model.query.AttendanceHistoryEntry;
import seedu.address.model.query.LessonQuerySource;

/**
 * Keeps the selected Student's retained history live in a separate read-only result view.
 */
public final class AttendanceRetrieval {
    private final LessonQuerySource queries;
    private final PeopleView peopleView;
    private final ObservableList<AttendanceHistoryEntry> displayed = FXCollections.observableArrayList();
    private final ObservableList<AttendanceHistoryEntry> readOnlyDisplayed =
            FXCollections.unmodifiableObservableList(displayed);
    private final ListChangeListener<AttendanceHistoryEntry> refresh = change -> displayed.setAll(change.getList());
    private final ReadOnlyStringWrapper context = new ReadOnlyStringWrapper("Attendance history");
    private final ReadOnlyBooleanWrapper active = new ReadOnlyBooleanWrapper(false);
    private ObservableList<AttendanceHistoryEntry> selected;
    private PersonId selectedStudentId;
    private Optional<LessonId> selectedLessonId = Optional.empty();

    /**
     * Connects history results and selection validity to the same canonical root and people view.
     */
    public AttendanceRetrieval(LessonQuerySource queries, PeopleView peopleView, PonHubData data) {
        this.queries = requireNonNull(queries);
        this.peopleView = requireNonNull(peopleView);
        requireNonNull(data).stateProperty().addListener((observable, previous, current) -> invalidateIfMissing(data));
    }

    public ObservableList<AttendanceHistoryEntry> getDisplayedHistory() {
        return readOnlyDisplayed;
    }

    public ReadOnlyStringProperty contextProperty() {
        return context.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty activeProperty() {
        return active.getReadOnlyProperty();
    }

    /** Resolves the current people index once and selects a stable-ID query. */
    public ObservableList<AttendanceHistoryEntry> show(Input input) throws CommandException {
        requireNonNull(input);
        var student = PersonIndexResolver.resolveStudent(input.studentIndex(), peopleView.getPeople());
        Optional<LessonId> lessonId = input.lessonId();
        ObservableList<AttendanceHistoryEntry> source;
        try {
            source = lessonId.isPresent()
                    ? queries.getAttendanceHistory(student.getId(), lessonId.get())
                    : queries.getAttendanceHistory(student.getId());
        } catch (IllegalArgumentException exception) {
            throw new CommandException(exception.getMessage());
        }
        if (selected != source) {
            if (selected != null) {
                selected.removeListener(refresh);
            }
            selected = source;
            selected.addListener(refresh);
            displayed.setAll(selected);
        }
        selectedStudentId = student.getId();
        selectedLessonId = lessonId;
        context.set("Student " + student.getId() + " · attendance history"
                + lessonId.map(id -> " · " + id).orElse(""));
        active.set(true);
        return readOnlyDisplayed;
    }

    /** Lesson retrieval restores the independent lesson view. */
    public void hide() {
        active.set(false);
    }

    /**
     * Clears a deleted selection only after committed publication, including already-empty query results.
     */
    private void invalidateIfMissing(PonHubData data) {
        if (selectedStudentId == null || (data.getPerson(selectedStudentId).isPresent()
                && selectedLessonId.map(id -> data.getLesson(id).isPresent()).orElse(true))) {
            return;
        }
        selected.removeListener(refresh);
        selected = null;
        selectedStudentId = null;
        selectedLessonId = Optional.empty();
        displayed.clear();
        context.set("Attendance history");
        hide();
    }
}
