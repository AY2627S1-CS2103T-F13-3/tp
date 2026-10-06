package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class PersonRoleTest {
    @Test
    public void parse_normalizedRole_resolvesEveryRole() {
        assertEquals(PersonRole.STUDENT, PersonRole.parse(" StUdEnT "));
        assertEquals(PersonRole.TUTOR, PersonRole.parse("TUTOR"));
        assertEquals(PersonRole.PARENT, PersonRole.parse("\tparent\n"));

        for (PersonRole role : PersonRole.values()) {
            assertEquals(role, PersonRole.parse(role.getValue()));
        }
    }

    @Test
    public void parse_invalidRole_throwsUsefulException() {
        String[] invalidRoles = {"", " ", "all", "students", "teacher", "s", "student parent"};
        for (String invalidRole : invalidRoles) {
            IllegalArgumentException exception =
                    assertThrows(IllegalArgumentException.class, () -> PersonRole.parse(invalidRole));
            assertEquals(PersonRole.MESSAGE_CONSTRAINTS, exception.getMessage());
        }

        assertThrows(NullPointerException.class, () -> PersonRole.parse(null));
    }

    @Test
    public void idPrefixes_distinctRoles_createRoleSpecificIds() {
        assertEquals("S1", PersonId.of(PersonRole.STUDENT, 1).getValue());
        assertEquals("T1", PersonId.of(PersonRole.TUTOR, 1).getValue());
        assertEquals("P1", PersonId.of(PersonRole.PARENT, 1).getValue());
    }
}
