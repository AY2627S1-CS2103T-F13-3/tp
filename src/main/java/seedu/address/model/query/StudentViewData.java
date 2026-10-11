package seedu.address.model.query;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.Student;

/** Immutable roster projection capturing one current student record. */
final class StudentViewData implements StudentView {
    private final Student student;

    StudentViewData(Student student) {
        this.student = requireNonNull(student);
    }

    @Override
    public PersonId getStudentId() {
        return student.getId();
    }

    @Override
    public String getName() {
        return student.getName().fullName;
    }

    @Override
    public EducationLevel getEducationLevel() {
        return student.getLevel();
    }

    @Override
    public String getParentPhone() {
        return student.getParentPhone().value;
    }

    @Override
    public Optional<String> getPhone() {
        return student.getContactDetails().getPhone().map(value -> value.value);
    }

    @Override
    public Optional<String> getEmail() {
        return student.getContactDetails().getEmail().map(value -> value.value);
    }

    @Override
    public Optional<String> getAddress() {
        return student.getContactDetails().getAddress().map(value -> value.value);
    }
}
