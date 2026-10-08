package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.Address;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Phone;
import seedu.address.testutil.PersonBuilder;
import seedu.address.testutil.PersonUtil;

public class RoleAwareAddParserTest {

    private static final String STUDENT_ARGUMENTS = "r/student n/Alex Tan l/S2 pp/00123456";
    private static final String ALL_STUDENT_ARGUMENTS = STUDENT_ARGUMENTS
            + " p/00345678 e/Alex@example.com a/Block 1";

    private final RoleAwareAddParser parser = new RoleAwareAddParser();

    @Test
    public void parse_minimalStudent_keepsOwnContactsAbsent() throws Exception {
        PersonAdditionInput input = parser.parse(STUDENT_ARGUMENTS);
        assertEquals(PersonRole.STUDENT, input.role());
        assertEquals(new ContactDetails(new Name("Alex Tan")), input.contactDetails());
        assertEquals(Optional.of(new EducationLevel("S2")), input.level());
        assertEquals(Optional.of(new Phone("00123456")), input.parentPhone());
    }

    @Test
    public void parse_minimalTutorAndParent_requiresOwnPhoneWithoutStudentFields() throws Exception {
        for (PersonRole role : new PersonRole[]{PersonRole.TUTOR, PersonRole.PARENT}) {
            PersonAdditionInput input = parser.parse("r/" + role.getValue() + " n/Mei Lim p/00987654");
            assertEquals(role, input.role());
            assertEquals(new Name("Mei Lim"), input.contactDetails().getName());
            assertEquals(Optional.of(new Phone("00987654")), input.contactDetails().getPhone());
            assertEquals(Optional.empty(), input.contactDetails().getEmail());
            assertEquals(Optional.empty(), input.contactDetails().getAddress());
            assertEquals(Optional.empty(), input.level());
            assertEquals(Optional.empty(), input.parentPhone());
        }
    }

    @Test
    public void parse_suppliedStudentContacts_preservesEachOptionalValue() throws Exception {
        ContactDetails contacts = new ContactDetails(new Name("Alex Tan"), Optional.of(new Phone("00345678")),
                Optional.of(new Email("Alex@example.com")), Optional.of(new Address("Block 1")));
        assertEquals(contacts, parser.parse(ALL_STUDENT_ARGUMENTS).contactDetails());
        assertEquals(Optional.of(new Phone("00345678")),
                parser.parse(STUDENT_ARGUMENTS + " p/00345678").contactDetails().getPhone());
        assertEquals(Optional.of(new Email("Alex@example.com")),
                parser.parse(STUDENT_ARGUMENTS + " e/Alex@example.com").contactDetails().getEmail());
        assertEquals(Optional.of(new Address("Block 1")),
                parser.parse(STUDENT_ARGUMENTS + " a/Block 1").contactDetails().getAddress());
    }

    @Test
    public void parse_tutorAndParentOptionalContacts_preservesPresentValues() throws Exception {
        for (String role : new String[]{"tutor", "parent"}) {
            ContactDetails contacts = parser.parse("r/" + role + " n/Mei Lim p/00987654"
                    + " e/Mei@example.com a/Block 2").contactDetails();
            assertEquals(Optional.of(new Email("Mei@example.com")), contacts.getEmail());
            assertEquals(Optional.of(new Address("Block 2")), contacts.getAddress());
        }
    }

    @Test
    public void parse_reorderedPrefixesAndMixedCaseValues_returnsEquivalentInput() throws Exception {
        assertEquals(parser.parse(ALL_STUDENT_ARGUMENTS), parser.parse(
                "a/Block 1 e/Alex@example.com p/00345678 pp/00123456 l/s2 n/Alex Tan r/StUdEnT"));
        assertEquals(PersonRole.TUTOR, parser.parse("n/Mei Lim p/00987654 r/TUTOR").role());
        assertEquals(PersonRole.PARENT, parser.parse("p/00987654 r/PaReNt n/Mei Lim").role());
        assertEquals(Optional.of(new EducationLevel("JC2")),
                parser.parse("r/student n/Alex Tan l/jc2 pp/00123456").level());
    }

    @Test
    public void parse_surroundingSpaces_preservesDisplayCaseAndPhoneZeros() throws Exception {
        assertEquals(parser.parse(ALL_STUDENT_ARGUMENTS), parser.parse(
                "   r/ student   n/ Alex Tan   l/ S2   pp/ 00123456   p/ 00345678"
                        + "   e/ Alex@example.com   a/ Block 1   "));
    }

    @Test
    public void parse_phoneLengthBoundaries_acceptsOwnAndParentNumbers() throws Exception {
        for (String phone : new String[]{"001", "000123456789012"}) {
            PersonAdditionInput student = parser.parse("r/student n/Alex Tan l/S2 pp/" + phone + " p/" + phone);
            assertEquals(Optional.of(new Phone(phone)), student.parentPhone());
            assertEquals(Optional.of(new Phone(phone)), student.contactDetails().getPhone());
            for (String role : new String[]{"tutor", "parent"}) {
                assertEquals(Optional.of(new Phone(phone)),
                        parser.parse("r/" + role + " n/Mei Lim p/" + phone).contactDetails().getPhone());
            }
        }
    }

    @Test
    public void parse_missingRequiredPrefixes_reportsMissingField() {
        String[][] cases = {
            {"", "r/"},
            {"   ", "r/"},
            {"n/Alex Tan l/S2 pp/00123456", "r/"},
            {"r/student l/S2 pp/00123456", "n/"},
            {"r/student n/Alex Tan pp/00123456", "l/"},
            {"r/student n/Alex Tan l/S2", "pp/"},
            {"r/tutor n/Mei Lim", "p/"},
            {"r/parent n/Pat Tan", "p/"}
        };
        for (String[] testCase : cases) {
            assertThrows(ParseException.class, "Missing required add prefix '" + testCase[1] + "'.", () ->
                    parser.parse(testCase[0]));
        }
    }

    @Test
    public void parse_invalidRole_throwsParseException() {
        for (String role : new String[]{"students", "contact", "teacher", "S1", "student/tutor"}) {
            assertThrows(ParseException.class, PersonRole.MESSAGE_CONSTRAINTS, () ->
                    parser.parse("r/" + role + " n/Alex Tan l/S2 pp/00123456"));
        }
    }

    @Test
    public void parse_blankRequiredAndOptionalValues_reportsBlankField() {
        String[][] cases = {
            {"r/ n/Alex Tan l/S2 pp/00123456", "r/"},
            {"r/student n/ l/S2 pp/00123456", "n/"},
            {"r/student n/Alex Tan l/ pp/00123456", "l/"},
            {"r/student n/Alex Tan l/S2 pp/   ", "pp/"},
            {"r/tutor n/Mei Lim p/   ", "p/"},
            {STUDENT_ARGUMENTS + " p/   ", "p/"},
            {STUDENT_ARGUMENTS + " e/   ", "e/"},
            {STUDENT_ARGUMENTS + " a/   ", "a/"},
            {STUDENT_ARGUMENTS + " p/\u2003", "p/"},
            {STUDENT_ARGUMENTS + " e/\u2003", "e/"},
            {STUDENT_ARGUMENTS + " a/\u2003", "a/"}
        };
        for (String[] testCase : cases) {
            assertThrows(ParseException.class, "Add prefix '" + testCase[1] + "' requires a non-blank value.", () ->
                    parser.parse(testCase[0]));
        }
    }

    @Test
    public void parse_repeatedPrefixes_rejectsEqualAndDifferentValues() {
        String[][] fields = {
            {"r/", "student"}, {"n/", "Alex Tan"}, {"l/", "S2"}, {"pp/", "00123456"},
            {"p/", "00345678"}, {"e/", "Alex@example.com"}, {"a/", "Block 1"}
        };
        for (String[] field : fields) {
            for (String value : new String[]{field[1], "other"}) {
                assertThrows(ParseException.class, "Repeated add prefix '" + field[0] + "'.", () ->
                        parser.parse(ALL_STUDENT_ARGUMENTS + " " + field[0] + value));
            }
        }
    }

    @Test
    public void parse_unknownAndUppercasePrefixes_cannotBeAbsorbedByAddress() {
        for (String prefix : new String[]{"t/", "x/", "sid/", "R/", "N/", "PP/"}) {
            assertThrows(ParseException.class, "Unknown add prefix '" + prefix + "'.", () ->
                    parser.parse(ALL_STUDENT_ARGUMENTS + " " + prefix + "value"));
        }
        assertThrows(ParseException.class, "Unknown add prefix 'R/'.", () ->
                parser.parse("R/student n/Alex Tan l/S2 pp/00123456"));
    }

    @Test
    public void parse_unknownPrefixesAfterUnicodeSpaces_cannotBeAbsorbedByAddress() {
        for (String space : new String[]{"\u2003", "\u2002", "\u3000", "\u00A0", "\u202F"}) {
            for (String prefix : new String[]{"x/", "sid/", "R/"}) {
                assertThrows(ParseException.class, "Unknown add prefix '" + prefix + "'.", () ->
                        parser.parse(STUDENT_ARGUMENTS + " a/Block" + space + prefix + "value"));
            }
        }
    }

    @Test
    public void parse_repeatedPrefixAfterUnicodeSpace_rejects() {
        assertThrows(ParseException.class, "Repeated add prefix 'a/'.", () ->
                parser.parse(ALL_STUDENT_ARGUMENTS + "\u2003a/Block 2"));
    }

    @Test
    public void parse_unicodeBoundarySpaces_returnsEquivalentInput() throws Exception {
        for (String space : new String[]{"\u2003", "\u2002", "\u3000", "\u00A0", "\u202F"}) {
            String arguments = space + "r/" + space + "student" + space + "n/" + space + "Alex Tan"
                    + space + "l/" + space + "S2" + space + "pp/" + space + "00123456"
                    + space + "p/" + space + "00345678" + space + "e/" + space + "Alex@example.com"
                    + space + "a/" + space + "Block 1" + space;
            assertEquals(parser.parse(ALL_STUDENT_ARGUMENTS), parser.parse(arguments));
            assertThrows(ParseException.class, "Add prefix 'a/' requires a non-blank value.", () ->
                    parser.parse(STUDENT_ARGUMENTS + " a/" + space));
        }
    }

    @Test
    public void parse_internalAddressSpaces_preservesContent() throws Exception {
        String address = "Block  1, Unit #02-03";
        PersonAdditionInput input = parser.parse(STUDENT_ARGUMENTS + " a/" + address + "\u2003p/00345678");
        assertEquals(Optional.of(new Address(address)), input.contactDetails().getAddress());
        assertEquals(Optional.of(new Phone("00345678")), input.contactDetails().getPhone());
    }

    @Test
    public void parse_longInternalAddressSpaces_preservesContent() throws Exception {
        String address = "Block" + " ".repeat(20000) + "1";
        PersonAdditionInput input = parser.parse(STUDENT_ARGUMENTS + " a/\u2003" + address + "\u00A0p/00345678");
        assertEquals(Optional.of(new Address(address)), input.contactDetails().getAddress());
        assertEquals(Optional.of(new Phone("00345678")), input.contactDetails().getPhone());
    }

    @Test
    public void parse_studentFieldsForOtherRoles_reportsInappropriatePrefix() {
        for (String role : new String[]{"tutor", "parent"}) {
            for (String prefix : new String[]{"l/", "pp/"}) {
                assertThrows(ParseException.class, "Add prefix '" + prefix + "' is only allowed for students.", () ->
                        parser.parse("r/" + role + " n/Mei Lim p/00987654 " + prefix + "value"));
            }
        }
    }

    @Test
    public void parse_preambleAndMalformedSyntax_throwsParseException() {
        for (String input : new String[]{"student", "add " + STUDENT_ARGUMENTS, "anything " + STUDENT_ARGUMENTS,
            "r /student n/Alex Tan l/S2 pp/00123456", "rstudent n/Alex Tan l/S2 pp/00123456"}) {
            assertThrows(ParseException.class, RoleAwareAddParser.MESSAGE_PREAMBLE, () -> parser.parse(input));
        }
        assertThrows(ParseException.class, () -> parser.parse("r/studentn/Alex Tan l/S2 pp/00123456"));
    }

    @Test
    public void parse_controlsAndLineBreaks_rejectsBeforeTrimming() {
        for (String control : new String[]{"\t", "\n", "\r", "\r\n", "\u0000", "\u001F", "\u007F",
            "\u0085", "\u2028", "\u2029"}) {
            for (String input : new String[]{control, control + STUDENT_ARGUMENTS, STUDENT_ARGUMENTS + control,
                "r/" + control + "student n/Alex Tan l/S2 pp/00123456", STUDENT_ARGUMENTS + " a/Block" + control}) {
                assertThrows(ParseException.class, RoleAwareAddParser.MESSAGE_SINGLE_LINE, () -> parser.parse(input));
            }
        }
    }

    @Test
    public void parse_invalidOwnAndParentPhones_throwsParseException() {
        for (String phone : new String[]{"00", "0001234567890123", "001 234", "+001234", "001-234", "abc"}) {
            assertThrows(ParseException.class, () -> parser.parse("r/student n/Alex Tan l/S2 pp/" + phone));
            assertThrows(ParseException.class, () -> parser.parse(STUDENT_ARGUMENTS + " p/" + phone));
            for (String role : new String[]{"tutor", "parent"}) {
                assertThrows(ParseException.class, () -> parser.parse("r/" + role + " n/Mei Lim p/" + phone));
            }
        }
        assertThrows(ParseException.class, ContactDetails.MESSAGE_PHONE_CONSTRAINTS, () ->
                parser.parse("r/student n/Alex Tan l/S2 pp/0001234567890123"));
    }

    @Test
    public void parse_invalidLevelAndSharedContactValues_throwsParseException() {
        for (String level : new String[]{"P0", "P7", "S6", "JC3", "Primary 1"}) {
            assertThrows(ParseException.class, EducationLevel.MESSAGE_CONSTRAINTS, () ->
                    parser.parse("r/student n/Alex Tan l/" + level + " pp/00123456"));
        }
        assertThrows(ParseException.class, Name.MESSAGE_CONSTRAINTS, () ->
                parser.parse("r/student n/@Alex l/S2 pp/00123456"));
        assertThrows(ParseException.class, Email.MESSAGE_CONSTRAINTS, () ->
                parser.parse(STUDENT_ARGUMENTS + " e/not-an-email"));
    }

    @Test
    public void parse_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }

    @Test
    public void parseCommand_roleAwareAdd_remainsDormantWhileLegacyAddAndHelpWork() throws Exception {
        AddressBookParser commandParser = new AddressBookParser();
        Person legacyPerson = new PersonBuilder().build();
        assertEquals(new AddCommand(legacyPerson),
                commandParser.parseCommand(PersonUtil.getAddCommand(legacyPerson)));
        assertEquals(new HelpCommand("add"), commandParser.parseCommand("help add"));
        assertEquals(new HelpCommand(), commandParser.parseCommand("help"));
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddCommand.MESSAGE_USAGE);
        assertThrows(ParseException.class, expectedMessage, () ->
                commandParser.parseCommand("add " + ALL_STUDENT_ARGUMENTS));
    }
}
