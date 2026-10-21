package ponhub.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import ponhub.logic.parser.PersonEditInput.Field;
import ponhub.model.person.Address;
import ponhub.model.person.EducationLevel;
import ponhub.model.person.Email;
import ponhub.model.person.Name;
import ponhub.model.person.Phone;

public class PersonEditInputTest {

    @Test
    public void constructor_mixedFieldOperations_preservesEachIntent() {
        Name name = new Name("Jamie Tan");
        Phone phone = new Phone("00123456");
        EducationLevel level = new EducationLevel("P2");
        PersonEditInput input = new PersonEditInput(Field.set(name), Field.set(phone), Field.clear(),
                Field.keep(), Field.set(level), Field.keep());

        assertEquals(Field.set(name), input.name());
        assertEquals(Field.set(phone), input.phone());
        assertEquals(Field.<Email>clear(), input.email());
        assertEquals(Field.<Address>keep(), input.address());
        assertEquals(Field.set(level), input.level());
        assertEquals(Field.<Phone>keep(), input.parentPhone());
    }

    @Test
    public void constructor_allFieldsOmitted_acceptsModelLevelNoOp() {
        PersonEditInput input = new PersonEditInput(Field.keep(), Field.keep(), Field.keep(),
                Field.keep(), Field.keep(), Field.keep());

        assertFalse(input.name().isSupplied());
        assertFalse(input.phone().isSupplied());
        assertFalse(input.email().isSupplied());
        assertFalse(input.address().isSupplied());
        assertFalse(input.level().isSupplied());
        assertFalse(input.parentPhone().isSupplied());
    }

    @Test
    public void constructor_nullComponent_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new PersonEditInput(null, Field.keep(), Field.keep(),
                Field.keep(), Field.keep(), Field.keep()));
        assertThrows(NullPointerException.class, () -> new PersonEditInput(Field.keep(), null, Field.keep(),
                Field.keep(), Field.keep(), Field.keep()));
        assertThrows(NullPointerException.class, () -> new PersonEditInput(Field.keep(), Field.keep(), null,
                Field.keep(), Field.keep(), Field.keep()));
        assertThrows(NullPointerException.class, () -> new PersonEditInput(Field.keep(), Field.keep(), Field.keep(),
                null, Field.keep(), Field.keep()));
        assertThrows(NullPointerException.class, () -> new PersonEditInput(Field.keep(), Field.keep(), Field.keep(),
                Field.keep(), null, Field.keep()));
        assertThrows(NullPointerException.class, () -> new PersonEditInput(Field.keep(), Field.keep(), Field.keep(),
                Field.keep(), Field.keep(), null));
    }

    @Test
    public void fieldFactory_keepClearAndSet_remainDistinct() {
        Phone phone = new Phone("00123456");
        Field<Phone> omitted = Field.keep();
        Field<Phone> cleared = Field.clear();
        Field<Phone> supplied = Field.set(phone);

        assertEquals(new Field<Phone>(false, Optional.empty()), omitted);
        assertEquals(new Field<Phone>(true, Optional.empty()), cleared);
        assertEquals(new Field<>(true, Optional.of(phone)), supplied);
        assertFalse(omitted.isSupplied());
        assertTrue(cleared.isSupplied());
        assertTrue(supplied.isSupplied());
        assertEquals(Optional.empty(), omitted.value());
        assertEquals(Optional.empty(), cleared.value());
        assertEquals(Optional.of(phone), supplied.value());
        assertNotEquals(omitted, cleared);
        assertNotEquals(cleared, supplied);
    }

    @Test
    public void fieldConstructor_inconsistentOmission_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Field<>(false, Optional.of(new Name("Alex Tan"))));
    }

    @Test
    public void fieldConstructor_nullOptionalOrSetValue_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Field<Name>(false, null));
        assertThrows(NullPointerException.class, () -> new Field<Name>(true, null));
        assertThrows(NullPointerException.class, () -> Field.<Name>set(null));
    }

    @Test
    public void resolve_omittedField_keepsPresentOrAbsentExistingValue() {
        Optional<Name> present = Optional.of(new Name("Alex Tan"));
        Optional<Name> absent = Optional.empty();

        assertSame(present, Field.<Name>keep().resolve(present));
        assertSame(absent, Field.<Name>keep().resolve(absent));
    }

    @Test
    public void resolve_suppliedField_replacesOrClearsPresentAndAbsentExistingValue() {
        Optional<Email> present = Optional.of(new Email("old@example.com"));
        Email replacement = new Email("new@example.com");

        assertEquals(Optional.of(replacement), Field.set(replacement).resolve(present));
        assertEquals(Optional.of(replacement), Field.set(replacement).resolve(Optional.empty()));
        assertEquals(Optional.empty(), Field.<Email>clear().resolve(present));
        assertEquals(Optional.empty(), Field.<Email>clear().resolve(Optional.empty()));
    }

    @Test
    public void resolve_nullExistingOptional_rejectsEveryIntent() {
        assertThrows(NullPointerException.class, () -> Field.<Name>keep().resolve(null));
        assertThrows(NullPointerException.class, () -> Field.<Name>clear().resolve(null));
        assertThrows(NullPointerException.class, () -> Field.set(new Name("Alex Tan")).resolve(null));
    }
}
