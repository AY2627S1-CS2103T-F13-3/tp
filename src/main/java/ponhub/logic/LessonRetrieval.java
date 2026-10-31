package ponhub.logic;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import ponhub.commons.core.index.Index;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.LessonsInput;
import ponhub.logic.parser.ShowLessonInput;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.attendance.Attendance;
import ponhub.model.attendance.AttendanceStatus;
import ponhub.model.lesson.Lesson;
import ponhub.model.lesson.LessonId;
import ponhub.model.person.PersonId;
import ponhub.model.person.Student;
import ponhub.model.query.LessonDetailView;
import ponhub.model.query.LessonOrder;
import ponhub.model.query.LessonQueries;
import ponhub.model.query.LessonView;
import ponhub.model.search.SearchCriteria;

/**
 * Selects live lesson results from the canonical query source without changing the people view.
 * The UI keeps one read-only list while commands select catalogue, student, search or lesson-detail results.
 */
public final class LessonRetrieval {
    /**
     * The selected lesson source, independent of the visible history pane and people selection.
     */
    public enum ResultKind {
        ALL_LESSONS, STUDENT_LESSONS, SEARCH, DETAIL
    }

    private final LessonQueries queries;
    private final PeopleView peopleView;
    private final PonHubData data;
    private final ObservableList<LessonView> displayed = FXCollections.observableArrayList();
    private final ObservableList<LessonView> readOnlyDisplayed = FXCollections.unmodifiableObservableList(displayed);
    private final ObservableList<LessonView> detail = FXCollections.observableArrayList();
    private final ListChangeListener<LessonView> refresh = change -> refreshDisplayed();
    private LessonOrder order = LessonOrder.CREATION;
    private ObservableList<LessonView> selected;
    private LessonId selectedDetailId;
    private Optional<LocalDate> selectedDate = Optional.empty();
    private final ReadOnlyStringWrapper context = new ReadOnlyStringWrapper("All lessons");
    private final ReadOnlyObjectWrapper<ResultKind> resultKind = new ReadOnlyObjectWrapper<>(ResultKind.ALL_LESSONS);

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
     * Identifies the catalogue, stable student, search filters or selected lesson/date.
     * People filtering does not change this context.
     */
    public ReadOnlyStringProperty contextProperty() {
        return context.getReadOnlyProperty();
    }

    /**
     * Identifies the selected source without relying on its display wording or the current people selection.
     * Failed requests and committed source refreshes retain this kind.
     */
    public ReadOnlyObjectProperty<ResultKind> resultKindProperty() {
        return resultKind.getReadOnlyProperty();
    }

    /**
     * Returns the full catalogue or one current-view student's lessons.
     * A supplied index is resolved once against the current people list, then converted to a stable ID.
     * The returned observable result is read-only and remains current after canonical publication.
     */
    public ObservableList<LessonView> list(Optional<Index> studentIndex) throws CommandException {
        listInput(new LessonsInput(studentIndex, LessonOrder.CREATION));
        return selected;
    }

    /**
     * Shows the requested catalogue or Student enrollments in the selected presentation order.
     */
    public ObservableList<LessonView> listInput(LessonsInput input) throws CommandException {
        requireNonNull(input);
        Optional<Index> studentIndex = input.studentIndex();
        String suffix = input.order() == LessonOrder.SCHEDULE ? " · schedule order" : "";
        if (studentIndex.isEmpty()) {
            context.set("All lessons" + suffix);
            select(queries.getLessonList(), ResultKind.ALL_LESSONS, input.order());
            return readOnlyDisplayed;
        }
        var student = PersonIndexResolver.resolveStudent(studentIndex.get(), peopleView.getPeople());
        context.set("Student " + student.getId() + " · current lessons" + suffix);
        select(queries.getStudentLessons(student.getId()), ResultKind.STUDENT_LESSONS, input.order());
        return readOnlyDisplayed;
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
        return select(queries.getFilteredLessonList(), ResultKind.SEARCH);
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
        select(detail, ResultKind.DETAIL);
        return snapshot;
    }

    /**
     * A committed replacement updates only the selected detail; a failed save publishes nothing.
     */
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
        Lesson selectedLesson = lesson.orElseThrow();
        if (selectedDate.isPresent()
                && !selectedLesson.getTimeSlot().getDay().matches(selectedDate.orElseThrow())) {
            selectedDate = Optional.empty();
            context.set("Lesson " + selectedDetailId + " · current roster and retained attendance");
        }
        detail.setAll(buildDetail(selectedLesson, selectedDate));
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
    private ObservableList<LessonView> select(ObservableList<LessonView> source, ResultKind kind) {
        return select(source, kind, LessonOrder.CREATION);
    }

    private ObservableList<LessonView> select(ObservableList<LessonView> source, ResultKind kind,
            LessonOrder requestedOrder) {
        order = requestedOrder;
        if (selected != source) {
            selected.removeListener(refresh);
            selected = source;
            selected.addListener(refresh);
        }
        refreshDisplayed();
        resultKind.set(kind);
        return source;
    }

    private void refreshDisplayed() {
        displayed.setAll(order.apply(selected));
    }
}
