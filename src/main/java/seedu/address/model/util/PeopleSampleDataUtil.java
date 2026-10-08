package seedu.address.model.util;

import java.util.Optional;

import seedu.address.model.person.Address;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.PeopleRegistry;
import seedu.address.model.person.Phone;

/**
 * Prepares representative canonical people for isolated development and future runtime integration.
 * The samples contain no lessons or attendance and are not installed into the active application.
 */
public final class PeopleSampleDataUtil {

    private PeopleSampleDataUtil() {
    }

    /**
     * Returns a fresh registry with deterministic IDs, allocation state, and mixed-role creation order.
     * Samples include siblings, ambiguous tutor names, distinct students sharing a name, and optional contacts.
     * Callers may mutate the returned registry without affecting another invocation.
     */
    public static PeopleRegistry getSamplePeopleRegistry() {
        PeopleRegistry people = new PeopleRegistry();
        people.addTutor(new ContactDetails(new Name("Mei Lim"), Optional.of(new Phone("00112233")),
                Optional.of(new Email("Mei.Lim@example.com")), Optional.of(new Address("12 Pasir Ris Drive 1"))));
        people.addStudent(new ContactDetails(new Name("Alex Tan")), new EducationLevel("P3"),
                new Phone("00987654"));
        people.addParent(new ContactDetails(new Name("Pat Tan"), Optional.of(new Phone("00987654")),
                Optional.of(new Email("Pat.Tan@example.com")), Optional.empty()));
        people.addStudent(new ContactDetails(new Name("Jamie Tan"), Optional.of(new Phone("00012345")),
                Optional.of(new Email("Jamie.Tan@example.com")), Optional.of(new Address("8 Tampines Avenue 2"))),
                new EducationLevel("S2"), new Phone("00987654"));
        people.addTutor(new ContactDetails(new Name("Mei Lim"), Optional.of(new Phone("00999888")),
                Optional.empty(), Optional.empty()));
        people.addStudent(new ContactDetails(new Name("Alex Tan")), new EducationLevel("JC1"),
                new Phone("00888888"));
        return people;
    }
}
