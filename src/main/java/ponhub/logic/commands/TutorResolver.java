package ponhub.logic.commands;

import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.util.List;
import java.util.Optional;

import ponhub.logic.commands.exceptions.CommandException;
import ponhub.model.PonHubData;
import ponhub.model.person.ContactDetails;
import ponhub.model.person.Name;
import ponhub.model.person.Phone;
import ponhub.model.person.Tutor;

/**
 * Resolves a Tutor using the shared normalized-full-name and optional exact-phone rules.
 */
final class TutorResolver {
    static final String MESSAGE_TUTOR_NOT_FOUND = "No tutor found with the full name '%s'.";
    static final String MESSAGE_TUTOR_PHONE_NOT_FOUND = "No tutor named '%s' has the exact phone '%s'.";
    static final String MESSAGE_AMBIGUOUS_TUTOR =
            "More than one tutor is named '%s'. Supply tp/TUTOR_PHONE to select one.";
    private static final String MESSAGE_AMBIGUOUS_TUTOR_PHONE =
            "More than one tutor matches the name '%s' and phone '%s'. "
                    + "Edit a tutor's name or phone to distinguish them.";

    private TutorResolver() {
    }

    /**
     * Resolves exactly one tutor, without falling back when an exact phone is supplied.
     */
    static Tutor resolve(PonHubData data, Name name, Optional<Phone> phone) throws CommandException {
        requireAllNonNull(data, name, phone);
        String normalizedName = new ContactDetails(name).getNormalizedName();
        List<Tutor> nameMatches = data.getPeople().stream()
                .filter(Tutor.class::isInstance)
                .map(Tutor.class::cast)
                .filter(tutor -> tutor.getContactDetails().getNormalizedName().equals(normalizedName))
                .toList();
        if (nameMatches.isEmpty()) {
            throw new CommandException(String.format(MESSAGE_TUTOR_NOT_FOUND, name));
        }
        if (phone.isPresent()) {
            Phone exactPhone = phone.orElseThrow();
            nameMatches = nameMatches.stream().filter(tutor -> tutor.getPhone().equals(exactPhone)).toList();
            if (nameMatches.isEmpty()) {
                throw new CommandException(String.format(MESSAGE_TUTOR_PHONE_NOT_FOUND, name, exactPhone));
            }
            if (nameMatches.size() > 1) {
                throw new CommandException(String.format(MESSAGE_AMBIGUOUS_TUTOR_PHONE, name, exactPhone));
            }
        }
        if (nameMatches.size() > 1) {
            throw new CommandException(String.format(MESSAGE_AMBIGUOUS_TUTOR, name));
        }
        return nameMatches.getFirst();
    }
}
