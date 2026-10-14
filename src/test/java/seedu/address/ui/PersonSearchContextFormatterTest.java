package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.FxTestUtil.runOnFxThread;
import static seedu.address.testutil.LessonQueryTestData.createData;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import seedu.address.model.PeopleView;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.Student;
import seedu.address.model.query.LessonQueries;
import seedu.address.model.search.PersonSearchMatch;

public class PersonSearchContextFormatterTest {
    @Test
    public void context_formatsStableStudentAndLessonDetailsWithoutCommandPositions() throws Exception {
        var data = createData();
        var student = (Student) data.getPerson(new PersonId("S1")).orElseThrow();
        var lesson = new LessonQueries(data).getLessonList().get(1);
        var lessonOnly = new PersonSearchMatch(Optional.empty(), Optional.of(lesson));
        var studentOnly = new PersonSearchMatch(Optional.of(student), Optional.empty());
        assertEquals("Student: Alex Tan (S1)", PersonSearchContextFormatter.format(studentOnly));
        assertEquals("Lesson: L10 · Math · Mon 0900-1000 · Tutor: Mei Lim (T1) · Room: R1",
                PersonSearchContextFormatter.format(lessonOnly));
        assertTrue(PersonSearchContextFormatter.format(new PersonSearchMatch(Optional.of(student), Optional.of(lesson)))
                .contains("Student: Alex Tan (S1) · Lesson: L10"));
        runOnFxThread(() -> {
            Region card = new PersonRecordCard(student, 1, List.of(lessonOnly)).getRoot();
            new Scene(card, 260, 600);
            card.applyCss();
            card.resize(260, 600);
            card.layout();
            assertTrue(card.lookupAll(".label").stream().map(Label.class::cast)
                    .anyMatch(label -> label.isWrapText() && label.getText().contains("Lesson: L10")));
            assertThrows(NullPointerException.class, () -> new PersonRecordCard(student, 1, null));
            Region listPanel = new PersonRecordListPanel(new PeopleView(data).getPeople()).getRoot();
            assertEquals(7, ((ListView<?>) listPanel.lookup("#personListView")).getItems().size());
            assertThrows(NullPointerException.class, () ->
                    new PersonRecordListPanel((ObservableList<PersonRecord>) null));
            assertThrows(NullPointerException.class, () ->
                    new PersonRecordListPanel(new PeopleView(data).getPeople(), null));
        });
    }
}
