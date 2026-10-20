package seedu.address.logic.commands;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.PonHubData;
import seedu.address.model.person.Name;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Tutor;

/** Resolves a Tutor using the shared normalized-full-name and optional exact-phone rules. */
final class TutorResolver {
    static final String MESSAGE_TUTOR_NOT_FOUND = "No tutor found with the full name '%s'.";
    static final String MESSAGE_TUTOR_PHONE_NOT_FOUND = "No tutor named '%s' has the exact phone '%s'.";
    static final String MESSAGE_AMBIGUOUS_TUTOR =
            "More than one tutor is named '%s'. Supply tp/TUTOR_PHONE to select one.";

    private TutorResolver() {
    }

    /** Resolves exactly one tutor, without falling back when an exact phone is supplied. */
    static Tutor resolve(PonHubData data, Name name, Optional<Phone> phone) throws CommandException {
        requireAllNonNull(data, name, phone);
        String normalizedName = name.fullName.toLowerCase(Locale.ROOT);
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
            return nameMatches.stream().filter(tutor -> tutor.getPhone().equals(exactPhone)).findFirst()
                    .orElseThrow(() -> new CommandException(String.format(MESSAGE_TUTOR_PHONE_NOT_FOUND,
                            name, exactPhone)));
        }
        if (nameMatches.size() > 1) {
            throw new CommandException(String.format(MESSAGE_AMBIGUOUS_TUTOR, name));
        }
        return nameMatches.getFirst();
    }
}
