package ponhub.logic.parser;

import static java.util.Objects.requireNonNull;
import static ponhub.commons.util.AppUtil.checkArgument;
import static ponhub.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Optional;

import ponhub.model.person.Address;
import ponhub.model.person.EducationLevel;
import ponhub.model.person.Email;
import ponhub.model.person.Name;
import ponhub.model.person.Phone;

/**
 * Immutable person-edit intent containing validated field values without a role, ID or writable model state.
 * Omitted, cleared and supplied values remain distinct until applied to the target record.
 * Role-specific required fields are checked when preparing the complete replacement.
 */
public record PersonEditInput(Field<Name> name, Field<Phone> phone, Field<Email> email,
        Field<Address> address, Field<EducationLevel> level, Field<Phone> parentPhone) {

    /**
     * Creates edit intent with an explicit operation for every field.
     * Keeping every field is permitted here; the command parser owns the supplied-prefix requirement.
     *
     * @throws NullPointerException if any field operation is null.
     */
    public PersonEditInput {
        requireAllNonNull(name, phone, email, address, level, parentPhone);
    }

    /**
     * An immutable keep, clear or set operation on one validated field value.
     *
     * @param isSupplied Whether the caller explicitly supplies an operation for this field.
     * @param value The supplied value, or empty for a clear or omitted operation.
     */
    public record Field<T>(boolean isSupplied, Optional<T> value) {

        /**
         * Creates a field operation, rejecting a value attached to an omitted operation.
         *
         * @throws NullPointerException if the optional value is null.
         * @throws IllegalArgumentException if an omitted operation contains a value.
         */
        public Field {
            requireNonNull(value);
            checkArgument(isSupplied || value.isEmpty(), "An omitted edit field cannot contain a value.");
        }

        /**
         * Returns an operation that retains the existing value.
         */
        public static <T> Field<T> keep() {
            return new Field<>(false, Optional.empty());
        }

        /**
         * Returns an operation that explicitly removes the value, subject to the target's required-field rules.
         */
        public static <T> Field<T> clear() {
            return new Field<>(true, Optional.empty());
        }

        /**
         * Returns an operation that supplies one non-null validated value.
         */
        public static <T> Field<T> set(T value) {
            return new Field<>(true, Optional.of(value));
        }

        /**
         * Returns the supplied value or clear operation, or the existing value when omitted.
         *
         * @throws NullPointerException if the existing optional is null.
         */
        public Optional<T> resolve(Optional<T> existing) {
            requireNonNull(existing);
            return isSupplied ? value : existing;
        }
    }
}
