package seedu.address.ui;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import seedu.address.model.search.PersonSearchMatch;

/**
 * Formats matching relationships as wrapping card text, never as person-command indices.
 */
final class PersonSearchContextFormatter {
    private PersonSearchContextFormatter() {
    }

    static String format(PersonSearchMatch match) {
        String student = match.student().map(value -> "Student: " + value.getName() + " (" + value.getId() + ")")
                .orElse("");
        String lesson = match.lesson().map(value -> "Lesson: " + value.getLessonId() + " · " + value.getSubject()
                + " · " + value.getTimeSlot() + " · Tutor: " + value.getTutorName() + " (" + value.getTutorId()
                + ") · Room: " + value.getRoom()).orElse("");
        return Stream.of(student, lesson).filter(value -> !value.isEmpty()).collect(Collectors.joining(" · "));
    }
}
