package ponhub.testutil;

import java.util.Optional;

import ponhub.model.PonHubData;
import ponhub.model.PonHubDataState;
import ponhub.model.person.ContactDetails;
import ponhub.model.person.Name;
import ponhub.model.person.PeopleRegistry;
import ponhub.model.person.Phone;

/**
 * Adds ambiguous parent names/numbers and an unassigned child to the shared lesson fixture.
 */
public final class RelationshipSearchTestData {
    private RelationshipSearchTestData() {
    }

    /**
     * Supplies shared lessons, cross-sibling counterexamples and exact-number parent associations.
     */
    public static PonHubData createData() {
        var base = LessonQueryTestData.createData().exportState();
        PeopleRegistry people = new PeopleRegistry(base.people());
        people.addParent(contact("Pat Tan", "0987654")); // Same name, different exact number, unlinked.
        people.addParent(contact("Unassigned Parent", "00888888")); // S3 has no lessons.
        people.addParent(contact("Other Parent", "00987654")); // Same number as P1, distinct identity.
        return new PonHubData(new PonHubDataState(people.exportState(), base.lessons(), base.attendance(),
                base.lastAllocatedLessonSequence()));
    }

    private static ContactDetails contact(String name, String phone) {
        return new ContactDetails(new Name(name), Optional.of(new Phone(phone)), Optional.empty(), Optional.empty());
    }
}
