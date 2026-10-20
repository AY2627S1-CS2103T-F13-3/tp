package seedu.address.logic;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.ShowLessonInput;
import seedu.address.model.PeopleView;
import seedu.address.model.PonHubData;
import seedu.address.model.attendance.Attendance;
import seedu.address.model.attendance.AttendanceStatus;
import seedu.address.model.lesson.Lesson;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.Student;
import seedu.address.model.query.LessonDetailView;
import seedu.address.model.query.LessonQueries;
import seedu.address.model.query.LessonView;
import seedu.address.model.search.SearchCriteria;

/**
 * Selects live lesson results from the canonical query source without changing the people view.
 * The UI keeps one read-only list while commands switch its source between catalogue, student and search results.
 */
public final class LessonRetrieval {
    private final LessonQueries queries;
    private final PeopleView peopleView;
    private final PonHubData data;
    private final ObservableList<LessonView> displayed = FXCollections.observableArrayList();
    private final ObservableList<LessonView> readOnlyDisplayed = FXCollections.unmodifiableObservableList(displayed);
    private final ObservableList<LessonView> detail = FXCollections.observableArrayList();
    private final ListChangeListener<LessonView> refresh = change -> displayed.setAll(change.getList());
    private ObservableList<LessonView> selected;
    private LessonId selectedDetailId;
    private Optional<LocalDate> selectedDate = Optional.empty();
    private final ReadOnlyStringWrapper context = new ReadOnlyStringWrapper("All lessons");

    /**
     * Uses the query and people views supplied by the same canonical runtime adapter.
     * Initially displays the complete catalogue.
     */
    public LessonRetrieval(LessonQueries queries, PeopleView peopleView, PonHubData data) {
        this.queries = requireNonNull(queries);
        this.peopleView = requireNonNull(peopleView);
        this.data = requireNonNull(data);
        selected = queries.getLessonList();
        selected.addListener(refresh);
        displayed.setAll(selected);
        data.stateProperty().addListener((observable, previous, current) -> refreshDetail());
    }

    /**
     * Returns the same read-only UI list across commands, committed changes and rollback.
     */
    public ObservableList<LessonView> getDisplayedLessons() {
        return readOnlyDisplayed;
    }

    /**
     * Identifies the selected catalogue, stable student or normalized search filters independently of people filtering.
     */
    public ReadOnlyStringProperty contextProperty() {
        return context.getReadOnlyProperty();
    }

    /**
     * Returns the full catalogue or one current-view student's lessons.
     * A supplied index is resolved once against the current people list, then converted to a stable ID.
     * The returned observable result is read-only and remains current after canonical publication.
     */
    public ObservableList<LessonView> list(Optional<Index> studentIndex) throws CommandException {
        requireNonNull(studentIndex);
        if (studentIndex.isEmpty()) {
            context.set("All lessons");
            return select(queries.getLessonList());
        }
        var student = PersonIndexResolver.resolveStudent(studentIndex.get(), peopleView.getPeople());
        context.set("Student " + student.getId() + " · current lessons");
        return select(queries.getStudentLessons(student.getId()));
    }

    /**
     * Applies lesson search to a separate read-only result view without changing the people view or stored data.
     * An invalid category leaves the previous lesson filter intact.
     */
    public ObservableList<LessonView> search(SearchCriteria criteria) {
        queries.setLessonFilter(requireNonNull(criteria));
        String filters = criteria.getFilters().entrySet().stream()
                .map(entry -> entry.getKey().getPrefix() + entry.getValue()).collect(Collectors.joining(", "));
        context.set(filters.isEmpty() ? "Lesson search: all lessons" : "Lesson search: " + filters);
        return select(queries.getFilteredLessonList());
    }

    /**
     * Displays one shared lesson and its current roster without changing the people view or operational data.
     * A dated request labels missing current-roster records as unrecorded and retains former-member records.
     * Validation completes before replacing the previous lesson result.
     */
    public LessonDetailView show(ShowLessonInput input) throws CommandException {
        requireNonNull(input);
        Lesson lesson = data.getLesson(input.lessonId()).orElseThrow(() ->
                new CommandException("The lesson does not exist: " + input.lessonId()));
        if (input.date().isPresent()
                && !lesson.getTimeSlot().getDay().matches(input.date().orElseThrow())) {
            throw new CommandException("Date " + input.date().orElseThrow() + " must be a "
                    + lesson.getTimeSlot().getDay() + " for lesson " + lesson.getId() + ".");
        }
        LessonDetailView snapshot = buildDetail(lesson, input.date());
        selectedDetailId = lesson.getId();
        selectedDate = input.date();
        detail.setAll(snapshot);
        context.set("Lesson " + lesson.getId() + input.date().map(date -> " · " + date).orElse("")
                + " · current roster and retained attendance");
        select(detail);
        return snapshot;
    }

    /** A committed replacement updates only the selected detail; a failed save publishes nothing. */
    private void refreshDetail() {
        if (selected != detail) {
            return;
        }
        Optional<Lesson> lesson = data.getLesson(selectedDetailId);
        if (lesson.isEmpty()) {
            detail.clear();
            context.set("Lesson " + selectedDetailId + " is no longer available");
            return;
        }
        detail.setAll(buildDetail(lesson.orElseThrow(), selectedDate));
    }

    private LessonDetailView buildDetail(Lesson lesson, Optional<LocalDate> date) {
        List<Student> roster = data.getLessonRoster(lesson.getId());
        Set<PersonId> currentIds = roster.stream().map(Student::getId).collect(Collectors.toSet());
        List<Attendance> records = data.getAttendanceForLesson(lesson.getId()).stream()
                .filter(record -> date.isEmpty() || date.orElseThrow().equals(record.getDate())).toList();
        Map<PersonId, AttendanceStatus> statuses = date.isEmpty() ? Map.of() : records.stream()
                .collect(Collectors.toMap(Attendance::getStudentId, Attendance::getStatus));
        List<LessonDetailView.RosterStatus> current = roster.stream()
                .map(student -> new LessonDetailView.RosterStatus(student,
                        Optional.ofNullable(statuses.get(student.getId())))).toList();
        List<LessonDetailView.FormerAttendance> former = records.stream()
                .filter(record -> !currentIds.contains(record.getStudentId()))
                .sorted(Comparator.comparing(Attendance::getDate).reversed()
                        .thenComparingLong(record -> record.getStudentId().getSequence()))
                .map(record -> new LessonDetailView.FormerAttendance(
                        (Student) data.getPerson(record.getStudentId()).orElseThrow(),
                        record.getDate(), record.getStatus())).toList();
        String tutorName = data.getPerson(lesson.getTutorId()).orElseThrow().getName().fullName;
        return new LessonDetailView(lesson, tutorName, date, current, former);
    }

    /**
     * Detaches the previous source so unrelated query updates cannot replace the selected results.
     */
    private ObservableList<LessonView> select(ObservableList<LessonView> source) {
        if (selected != source) {
            selected.removeListener(refresh);
            selected = source;
            selected.addListener(refresh);
            displayed.setAll(selected);
        }
        return source;
    }
}
