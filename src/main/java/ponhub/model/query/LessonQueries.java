package ponhub.model.query;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.AppUtil.checkArgument;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import ponhub.model.PonHubData;
import ponhub.model.PonHubDataState;
import ponhub.model.attendance.Attendance;
import ponhub.model.lesson.Lesson;
import ponhub.model.lesson.LessonId;
import ponhub.model.person.PersonId;
import ponhub.model.person.PersonRecord;
import ponhub.model.person.PersonRole;
import ponhub.model.person.Phone;
import ponhub.model.person.Student;
import ponhub.model.search.SearchCategory;
import ponhub.model.search.SearchCriteria;
import ponhub.model.search.SearchField;

/**
 * Indexed, read-only lesson projections over one canonical root.
 * Indexes and subscribed results refresh only when that root publishes a complete snapshot.
 * This component has model lifetime and runs on the single model/JavaFX application thread.
 * Active retrieval and relationship search share these projections without owning another writable store.
 */
public final class LessonQueries implements LessonQuerySource {
    private static final List<SearchField> EXACT_FIELDS =
            List.of(SearchField.DAY, SearchField.START_TIME, SearchField.END_TIME);
    private static final Comparator<LessonView> SCHEDULE_ORDER = Comparator
            .comparing((LessonView lesson) -> lesson.getTimeSlot().getDay())
            .thenComparing(lesson -> lesson.getTimeSlot().getStartTime())
            .thenComparing(LessonView::getLessonId);
    private static final Comparator<AttendanceHistoryEntry> HISTORY_ORDER = Comparator
            .comparing(AttendanceHistoryEntry::getDate).reversed()
            .thenComparing(AttendanceHistoryEntry::getLessonId);

    private final ResultList<LessonView> catalogue = new ResultList<>();
    private final ResultList<LessonView> filtered = new ResultList<>();
    private final Map<PersonId, PersonRecord> peopleById = new HashMap<>();
    private final Map<LessonId, LessonView> lessonsById = new HashMap<>();
    private final Map<PersonId, List<LessonView>> studentLessons = new HashMap<>();
    private final Map<PersonId, List<LessonView>> tutorSchedules = new HashMap<>();
    private final Map<LessonId, List<StudentView>> rosters = new HashMap<>();
    private final Map<String, List<Student>> studentsByParentPhone = new HashMap<>();
    private final Map<SearchField, Map<String, List<LessonView>>> exactIndexes = new EnumMap<>(SearchField.class);
    private final Map<PersonId, ResultList<LessonView>> studentSubscriptions = new HashMap<>();
    private final Map<PersonId, ResultList<LessonView>> tutorSubscriptions = new HashMap<>();
    private final Map<LessonId, ResultList<StudentView>> rosterSubscriptions = new HashMap<>();
    private final Map<PersonId, List<AttendanceHistoryEntry>> histories = new HashMap<>();
    private final Map<PersonId, ResultList<AttendanceHistoryEntry>> historySubscriptions = new HashMap<>();
    private final Map<HistoryKey, ResultList<AttendanceHistoryEntry>> lessonHistorySubscriptions = new HashMap<>();
    private SearchCriteria activeFilter = new SearchCriteria(SearchCategory.LESSON, Map.of());

    /**
     * Connects query results to the supplied canonical root's committed publication boundary.
     */
    public LessonQueries(PonHubData source) {
        requireNonNull(source);
        refresh(source.exportState());
        source.stateProperty().addListener((observable, previous, current) -> refresh(current));
    }

    @Override
    public ObservableList<LessonView> getLessonList() {
        return catalogue.readOnly;
    }

    @Override
    public ObservableList<LessonView> getFilteredLessonList() {
        return filtered.readOnly;
    }

    @Override
    public Optional<LessonView> findLessonById(LessonId lessonId) {
        return Optional.ofNullable(lessonsById.get(requireNonNull(lessonId)));
    }

    @Override
    public ObservableList<LessonView> getStudentLessons(PersonId studentId) {
        requirePerson(studentId, PersonRole.STUDENT);
        return subscribed(studentId, studentSubscriptions, studentLessons);
    }

    @Override
    public ObservableList<StudentView> getLessonRoster(LessonId lessonId) {
        requireNonNull(lessonId);
        checkArgument(lessonsById.containsKey(lessonId), "The lesson does not exist: " + lessonId);
        return subscribed(lessonId, rosterSubscriptions, rosters);
    }

    @Override
    public ObservableList<LessonView> getTutorSchedule(PersonId tutorId) {
        requirePerson(tutorId, PersonRole.TUTOR);
        return subscribed(tutorId, tutorSubscriptions, tutorSchedules);
    }

    @Override
    public ObservableList<AttendanceHistoryEntry> getAttendanceHistory(PersonId studentId) {
        requirePerson(studentId, PersonRole.STUDENT);
        return subscribed(studentId, historySubscriptions, histories);
    }

    @Override
    public ObservableList<AttendanceHistoryEntry> getAttendanceHistory(PersonId studentId, LessonId lessonId) {
        requirePerson(studentId, PersonRole.STUDENT);
        requireNonNull(lessonId);
        checkArgument(lessonsById.containsKey(lessonId), "The lesson does not exist: " + lessonId);
        HistoryKey key = new HistoryKey(studentId, lessonId);
        return lessonHistorySubscriptions.computeIfAbsent(key, unused -> {
            ResultList<AttendanceHistoryEntry> result = new ResultList<>();
            result.replace(historyForLesson(key));
            return result;
        }).readOnly;
    }

    /**
     * Returns the immutable criteria used by the active lesson result list.
     */
    public SearchCriteria getLessonFilter() {
        return activeFilter;
    }

    /**
     * Updates only lesson view state. Invalid criteria leave the previous filter and results intact.
     * A category-only lesson filter restores the complete catalogue.
     *
     * @throws NullPointerException if criteria is null
     * @throws IllegalArgumentException if criteria selects a category other than lesson
     */
    public void setLessonFilter(SearchCriteria criteria) {
        List<LessonView> matches = searchLessons(criteria);
        activeFilter = criteria;
        filtered.replace(matches);
    }

    /**
     * Returns an immutable snapshot without changing the active filter or any model state.
     * Exact weekday/time buckets narrow candidates; every supplied condition still matches the same lesson.
     * A student-name condition examines that lesson's current roster only.
     *
     * @throws NullPointerException if criteria is null
     * @throws IllegalArgumentException if criteria selects a category other than lesson
     */
    public List<LessonView> searchLessons(SearchCriteria criteria) {
        requireNonNull(criteria);
        checkArgument(criteria.getCategory() == SearchCategory.LESSON, "Lesson queries require c/lesson criteria.");
        List<LessonView> candidates = catalogue.values;
        for (SearchField field : EXACT_FIELDS) {
            Optional<String> value = criteria.getValue(field);
            if (value.isPresent()) {
                List<LessonView> bucket = exactIndexes.get(field).getOrDefault(value.get(), List.of());
                if (bucket.size() < candidates.size()) {
                    candidates = bucket;
                }
            }
        }
        return candidates.stream().filter(lesson -> matches(criteria, lesson)).toList();
    }

    private boolean matches(SearchCriteria criteria, LessonView lesson) {
        return criteria.matches(SearchField.DAY, lesson.getTimeSlot().getDay().toString())
                && criteria.matches(SearchField.START_TIME, lesson.getTimeSlot().getStartTime().toString())
                && criteria.matches(SearchField.END_TIME, lesson.getTimeSlot().getEndTime().toString())
                && matchesTimeWindow(criteria, lesson)
                && criteria.matches(SearchField.SUBJECT, lesson.getSubject().getValue())
                && criteria.matches(SearchField.TUTOR_NAME, lesson.getTutorName())
                && criteria.matches(SearchField.ROOM, lesson.getRoom().getValue())
                && (criteria.getValue(SearchField.STUDENT_NAME).isEmpty()
                    || rosters.get(lesson.getLessonId()).stream()
                        .anyMatch(student -> criteria.matches(SearchField.STUDENT_NAME, student.getName())));
    }

    /**
     * Tests positive interval overlap; a shared endpoint alone does not overlap.
     */
    private boolean matchesTimeWindow(SearchCriteria criteria, LessonView lesson) {
        Optional<String> from = criteria.getValue(SearchField.FROM_TIME);
        if (from.isEmpty()) {
            return true;
        }
        String to = criteria.getValue(SearchField.TO_TIME).orElseThrow();
        return lesson.getTimeSlot().getStartTime().toString().compareTo(to) < 0
                && lesson.getTimeSlot().getEndTime().toString().compareTo(from.orElseThrow()) > 0;
    }

    private void requirePerson(PersonId id, PersonRole role) {
        requireNonNull(id);
        checkArgument(id.getRole() == role && peopleById.containsKey(id),
                "Unknown or wrong-role " + role + " ID: " + id);
    }

    /**
     * Returns an immutable current snapshot of students linked by exact parent phone, including leading zeros.
     */
    public List<Student> getStudentsByParentPhone(Phone phone) {
        return studentsByParentPhone.getOrDefault(requireNonNull(phone).value, List.of());
    }

    /**
     * Resolves immutable Student details from the same stable-ID index used by roster projections.
     */
    public Student getStudentRecord(PersonId studentId) {
        requirePerson(studentId, PersonRole.STUDENT);
        return (Student) peopleById.get(studentId);
    }

    private void refresh(PonHubDataState snapshot) {
        peopleById.clear();
        lessonsById.clear();
        studentLessons.clear();
        tutorSchedules.clear();
        rosters.clear();
        histories.clear();
        studentsByParentPhone.clear();
        exactIndexes.clear();
        EXACT_FIELDS.forEach(field -> exactIndexes.put(field, new HashMap<>()));
        snapshot.people().getPeople().forEach(person -> peopleById.put(person.getId(), person));

        List<LessonView> entries = new ArrayList<>();
        Map<LessonId, Lesson> lessons = new HashMap<>();
        for (Lesson lesson : snapshot.lessons()) {
            lessons.put(lesson.getId(), lesson);
            LessonView entry = new LessonViewData(lesson, peopleById.get(lesson.getTutorId()).getName().fullName);
            entries.add(entry);
            lessonsById.put(lesson.getId(), entry);
            rosters.put(lesson.getId(), new ArrayList<>());
            tutorSchedules.computeIfAbsent(lesson.getTutorId(), unused -> new ArrayList<>()).add(entry);
            for (PersonId studentId : lesson.getEnrolledStudentIds()) {
                studentLessons.computeIfAbsent(studentId, unused -> new ArrayList<>()).add(entry);
            }
            index(SearchField.DAY, lesson.getTimeSlot().getDay().toString().toLowerCase(Locale.ROOT), entry);
            index(SearchField.START_TIME, lesson.getTimeSlot().getStartTime().toString(), entry);
            index(SearchField.END_TIME, lesson.getTimeSlot().getEndTime().toString(), entry);
        }
        // Walking people once supplies roster creation order without scanning all people for each lesson.
        for (PersonRecord person : snapshot.people().getPeople()) {
            if (person instanceof Student student) {
                studentsByParentPhone.computeIfAbsent(student.getParentPhone().value, unused -> new ArrayList<>())
                        .add(student);
                StudentView entry = new StudentViewData(student);
                for (LessonView lesson : studentLessons.getOrDefault(student.getId(), List.of())) {
                    rosters.get(lesson.getLessonId()).add(entry);
                }
            }
        }
        for (Attendance attendance : snapshot.attendance()) {
            Lesson lesson = lessons.get(attendance.getLessonId());
            histories.computeIfAbsent(attendance.getStudentId(), unused -> new ArrayList<>())
                    .add(new AttendanceHistoryEntryData(attendance,
                            lesson.getEnrolledStudentIds().contains(attendance.getStudentId())));
        }
        histories.values().forEach(history -> history.sort(HISTORY_ORDER));
        tutorSchedules.values().forEach(schedule -> schedule.sort(SCHEDULE_ORDER));
        studentsByParentPhone.replaceAll((phone, students) -> List.copyOf(students));
        // All derived indexes are complete before any observable list publishes replacement entries.
        catalogue.replace(entries);
        refreshSubscriptions(studentSubscriptions, studentLessons);
        refreshSubscriptions(tutorSubscriptions, tutorSchedules);
        refreshSubscriptions(rosterSubscriptions, rosters);
        refreshSubscriptions(historySubscriptions, histories);
        List.copyOf(lessonHistorySubscriptions.entrySet()).forEach(entry ->
                entry.getValue().replace(historyForLesson(entry.getKey())));
        filtered.replace(searchLessons(activeFilter));
    }

    private List<AttendanceHistoryEntry> historyForLesson(HistoryKey key) {
        return histories.getOrDefault(key.studentId(), List.of()).stream()
                .filter(entry -> entry.getLessonId().equals(key.lessonId())).toList();
    }

    /**
     * Identifies a retained-history subscription for one student and one shared Lesson.
     */
    private record HistoryKey(PersonId studentId, LessonId lessonId) {
    }

    private void index(SearchField field, String value, LessonView entry) {
        exactIndexes.get(field).computeIfAbsent(value, unused -> new ArrayList<>()).add(entry);
    }

    private static <K, T> ObservableList<T> subscribed(K key, Map<K, ResultList<T>> subscriptions,
            Map<K, List<T>> index) {
        return subscriptions.computeIfAbsent(key, unused -> {
            ResultList<T> result = new ResultList<>();
            result.replace(index.getOrDefault(key, List.of()));
            return result;
        }).readOnly;
    }

    private static <K, T> void refreshSubscriptions(Map<K, ResultList<T>> subscriptions, Map<K, List<T>> index) {
        // List callbacks may subscribe to more query results, so do not traverse the live subscription map.
        List.copyOf(subscriptions.entrySet()).forEach(entry ->
                entry.getValue().replace(index.getOrDefault(entry.getKey(), List.of())));
    }

    /**
     * Keeps the observable object held by callers stable while its immutable entries are replaced.
     */
    private static final class ResultList<T> {
        private final ObservableList<T> values = FXCollections.observableArrayList();
        private final ObservableList<T> readOnly = FXCollections.unmodifiableObservableList(values);

        private void replace(List<T> replacement) {
            values.setAll(replacement);
        }
    }
}
