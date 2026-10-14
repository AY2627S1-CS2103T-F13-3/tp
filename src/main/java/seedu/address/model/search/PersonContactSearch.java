package seedu.address.model.search;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.util.List;
import java.util.Set;

import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Student;

/**
 * Read-only own-contact matching over canonical person records in their supplied order.
 * Relationship filters require the later relationship-search integration.
 */
public final class PersonContactSearch {
    static final Set<SearchField> OWN_FIELDS = Set.of(SearchField.NAME, SearchField.LEVEL,
            SearchField.PHONE, SearchField.PARENT_PHONE, SearchField.EMAIL, SearchField.ADDRESS);

    private PersonContactSearch() {
    }

    /**
     * Returns one immutable snapshot of matching people without changing the supplied records or view.
     * A caller can use this result to update the current people view when the search command is activated.
     *
     * @throws IllegalArgumentException if the category is not a person role or a relationship filter is supplied
     */
    public static List<PersonRecord> search(List<PersonRecord> people, SearchCriteria criteria) {
        requireNonNull(people);
        requireNonNull(criteria);
        checkArgument(criteria.getCategory() != SearchCategory.LESSON,
                "Person contact search requires c/student, c/parent, or c/tutor.");
        for (SearchField field : criteria.getFilters().keySet()) {
            checkArgument(OWN_FIELDS.contains(field),
                    "Search prefix '" + field.getPrefix() + "' is not available for person contact search yet.");
        }

        PersonRole role = PersonRole.valueOf(criteria.getCategory().name());
        return people.stream()
                .filter(person -> person.getRole() == role && matchesOwnFields(person, criteria))
                .toList();
    }

    private static boolean matchesOwnFields(PersonRecord person, SearchCriteria criteria) {
        ContactDetails contact = person.getContactDetails();
        if (!criteria.matches(SearchField.NAME, contact.getName().fullName)
                || !criteria.matches(SearchField.PHONE, contact.getPhone().map(phone -> phone.value).orElse(null))
                || !criteria.matches(SearchField.EMAIL, contact.getEmail().map(email -> email.value).orElse(null))
                || !criteria.matches(SearchField.ADDRESS,
                        contact.getAddress().map(address -> address.value).orElse(null))) {
            return false;
        }
        if (person instanceof Student student) {
            return criteria.matches(SearchField.LEVEL, student.getLevel().getValue())
                    && criteria.matches(SearchField.PARENT_PHONE, student.getParentPhone().value);
        }
        return true;
    }
}
