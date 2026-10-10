package seedu.address.model;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;

import seedu.address.model.attendance.Attendance;
import seedu.address.model.attendance.AttendanceKey;
import seedu.address.model.lesson.Lesson;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.person.PeopleRegistry;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;

/**
 * Canonical data foundation, not yet wired into the active ModelManager or JSON storage.
 * Accepts only validated immutable state at the replacement boundary.
 * Intended for the application's single model thread, not concurrent writers.
 */
public final class PonHubData {
    private PonHubDataState state;

    public PonHubData() {
        this(new PonHubDataState(new PeopleRegistry().exportState(), List.of(), List.of(), 0));
    }

    public PonHubData(PonHubDataState initialState) {
        resetData(initialState);
    }

    public PonHubData(PonHubData other) {
        this(requireNonNull(other).exportState());
    }

    /**
     * Returns a complete immutable snapshot that cannot change with subsequent edits.
     */
    public PonHubDataState exportState() {
        return state;
    }

    /**
     * Restores validated state, including allocation counters. This is a restore boundary, not a normal edit.
     * Immutable state can be shared safely; neither this container nor its caller can change its collections.
     */
    public void resetData(PonHubDataState replacement) {
        state = requireNonNull(replacement);
    }

    /**
     * Restores another container, safely including this container itself.
     */
    public void resetData(PonHubData replacement) {
        resetData(requireNonNull(replacement).exportState());
    }

    public List<PersonRecord> getPeople() {
        return state.people().getPeople();
    }

    public List<Lesson> getLessons() {
        return state.lessons();
    }

    public List<Attendance> getAttendance() {
        return state.attendance();
    }

    /**
     * Returns the attendance record for the supplied key, or an empty optional if absent.
     *
     * @throws NullPointerException if the key is null.
     */
    public Optional<Attendance> getAttendance(AttendanceKey key) {
        requireNonNull(key);
        return getAttendance().stream().filter(record -> record.getKey().equals(key)).findFirst();
    }

    /**
     * Returns the person with the supplied stable ID, or an empty optional if absent.
     *
     * @throws NullPointerException if the ID is null.
     */
    public Optional<PersonRecord> getPerson(PersonId id) {
        requireNonNull(id);
        return getPeople().stream().filter(person -> person.getId().equals(id)).findFirst();
    }

    /**
     * Returns the lesson with the supplied stable ID, or an empty optional if absent.
     *
     * @throws NullPointerException if the ID is null.
     */
    public Optional<Lesson> getLesson(LessonId id) {
        requireNonNull(id);
        return getLessons().stream().filter(lesson -> lesson.getId().equals(id)).findFirst();
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof PonHubData data && state.equals(data.state);
    }

    @Override
    public int hashCode() {
        return state.hashCode();
    }
}
