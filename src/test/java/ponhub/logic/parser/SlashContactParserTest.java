package ponhub.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ponhub.logic.parser.PersonEditInput.Field;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.lesson.Subject;
import ponhub.model.person.Address;
import ponhub.model.person.Name;

/**
 * Checks field-aware slash literals without weakening command-specific prefix grammar.
 */
public class SlashContactParserTest {
    @Test
    public void parse_peopleNamesAndAddresses_retainsValuesInAnyOrder() throws Exception {
        RoleAwareAddParser add = new RoleAwareAddParser();
        for (String name : List.of("Ravi s/o Kumar", "Anita d/o Kumar", "José s/o 李明")) {
            var input = add.parse("a/Blk 10, #01/02 n/" + name + " pp/91234567 r/student l/P1");
            assertEquals(new Name(name), input.contactDetails().getName());
            assertEquals(Optional.of(new Address("Blk 10, #01/02")), input.contactDetails().getAddress());
            var edit = new PersonEditParser().parse("1 a/Block A12/34B n/" + name);
            assertEquals(Field.set(new Name(name)), edit.edit().name());
            assertEquals(Field.set(new Address("Block A12/34B")), edit.edit().address());
            assertEquals(Field.keep(), edit.edit().phone());
        }
        assertEquals(Field.clear(), new PersonEditParser().parse("1 n/Ravi s/o Kumar a/").edit().address());
    }

    @Test
    public void parse_lessonTutorMarkers_keepsSubjectAndDayPrefixesSeparate() throws Exception {
        for (String name : List.of("Ravi s/o Kumar", "Anita d/o Kumar")) {
            var add = new AddLessonParser().parse("tu/" + name
                    + " d/Mon s/Math st/0900 et/1000 rm/R1 tp/00123456");
            assertEquals(new Name(name), add.tutorName());
            var edit = new LessonEditParser().parse("tu/" + name + " d/Tue s/Science lid/L1");
            assertEquals(Optional.of(new Name(name)), edit.tutorName());
            assertEquals("Science", edit.subject().orElseThrow().toString());
        }
    }

    @Test
    public void parse_markerLikeUnknownAndRepeatedTokens_preservesPeopleErrors() {
        String add = "r/student n/Ravi s/o Kumar l/P1 pp/91234567";
        String edit = "1 n/Ravi d/o Kumar a/Blk #01/02";
        for (String prefix : List.of("x/", "N/", "S/", "#x/")) {
            assertEquals("Unknown add prefix '" + prefix + "'.", assertThrows(ParseException.class, () ->
                    new RoleAwareAddParser().parse(add + " " + prefix + "value")).getMessage());
            assertEquals("Unknown edit prefix '" + prefix + "'.", assertThrows(ParseException.class, () ->
                    new PersonEditParser().parse(edit + " " + prefix + "value")).getMessage());
        }
        assertEquals("Unknown add prefix 's/'.", assertThrows(ParseException.class, () ->
                new RoleAwareAddParser().parse("r/student n/Ravi s/other Kumar l/P1 pp/91234567"))
                .getMessage());
        assertEquals("Repeated add prefix 'n/'.", assertThrows(ParseException.class, () ->
                new RoleAwareAddParser().parse(add + " n/Other")).getMessage());
        assertEquals("Repeated edit prefix 'a/'.", assertThrows(ParseException.class, () ->
                new PersonEditParser().parse(edit + " a/1/2")).getMessage());
        assertEquals("Unknown edit prefix 's/'.", assertThrows(ParseException.class, () ->
                new PersonEditParser().parse("1 a/Ravi s/o Kumar")).getMessage());
    }

    @Test
    public void parse_unknownAndRepeatedTokensAfterTutorMarker_preservesLessonErrors() {
        String add = "d/Mon st/0900 et/1000 s/Math tu/Ravi s/o Kumar rm/R1";
        String edit = "lid/L1 tu/Anita d/o Kumar";
        assertEquals("Unknown addlesson prefix 'x/'.", assertThrows(ParseException.class, () ->
                new AddLessonParser().parse(add + " x/value")).getMessage());
        assertEquals("Repeated addlesson prefix 's/'.", assertThrows(ParseException.class, () ->
                new AddLessonParser().parse(add + " s/Other")).getMessage());
        assertEquals("Unknown editlesson prefix 'TU/'.", assertThrows(ParseException.class, () ->
                new LessonEditParser().parse(edit + " TU/Other")).getMessage());
        assertEquals("Repeated editlesson prefix 'tu/'.", assertThrows(ParseException.class, () ->
                new LessonEditParser().parse(edit + " tu/Other")).getMessage());
    }

    @Test
    public void parse_subjectStartingWithOAfterTutor_readsRequiredSubject() throws Exception {
        // s/ is required by addlesson, so a lowercase s/o in the tutor value is the subject when no other s/ exists.
        for (String arguments : List.of("d/Wed st/1100 et/1200 tu/Bob Ong s/o level math rm/R3",
                "d/Wed st/1100 et/1200 s/o level math tu/Bob Ong rm/R3",
                "tu/Bob Ong s/o level math d/Wed st/1100 et/1200 rm/R3")) {
            LessonCreationInput input = new AddLessonParser().parse(arguments);
            assertEquals(new Name("Bob Ong"), input.tutorName(), arguments);
            assertEquals(new Subject("o level math"), input.subject(), arguments);
        }
        // Supported horizontal Unicode spaces between prefixes behave like an ordinary space.
        for (String space : List.of("\u00a0", "\u2003", "\u202f")) {
            LessonCreationInput input = new AddLessonParser()
                    .parse("d/Wed st/1100 et/1200 tu/Bob Ong" + space + "s/o level math rm/R3");
            assertEquals(new Name("Bob Ong"), input.tutorName(), space);
            assertEquals(new Subject("o level math"), input.subject(), space);
        }
        // Boundary: a single-letter subject directly before the next prefix.
        LessonCreationInput shortSubject = new AddLessonParser().parse("d/Thu st/1100 et/1200 tu/Bob Ong s/o rm/R5");
        assertEquals(new Name("Bob Ong"), shortSubject.tutorName());
        assertEquals(new Subject("o"), shortSubject.subject());
        // The last marker starts the subject, so an earlier one stays in the legal name.
        LessonCreationInput bothMarkers = new AddLessonParser()
                .parse("d/Thu st/1100 et/1200 tu/Ravi s/o Kumar s/o level math rm/R5");
        assertEquals(new Name("Ravi s/o Kumar"), bothMarkers.tutorName());
        assertEquals(new Subject("o level math"), bothMarkers.subject());
        // Without s/ or tu/, the missing subject is still reported.
        assertEquals("Missing required addlesson prefix 's/'.", assertThrows(ParseException.class, () ->
                new AddLessonParser().parse("d/Thu st/1100 et/1200 rm/R5")).getMessage());
    }

    @Test
    public void parse_tutorLegalNameWithSeparateSubject_keepsMarkersInName() throws Exception {
        for (String arguments : List.of("d/Mon st/0900 et/1000 tu/Ravi s/o Kumar s/Math rm/R1",
                "d/Mon st/0900 et/1000 s/Math tu/Ravi s/o Kumar rm/R1",
                "d/Mon st/0900 et/1000 s/o level math tu/Anita d/o Kumar rm/R1")) {
            LessonCreationInput input = new AddLessonParser().parse(arguments);
            assertEquals(arguments.contains("Anita") ? new Name("Anita d/o Kumar") : new Name("Ravi s/o Kumar"),
                    input.tutorName(), arguments);
        }
        // editlesson has no required subject, so its tutor value keeps a legal-name marker.
        assertEquals(Optional.of(new Name("Ravi s/o Kumar")),
                new LessonEditParser().parse("lid/L1 tu/Ravi s/o Kumar").tutorName());
    }

    @Test
    public void parse_incompleteMarkersAndNonAsciiInternalSpaces_rejects() {
        for (String value : List.of("Ravi s/", "Ravi d/", "Ravi s/oKumar", "Ravi s/x Kumar",
                "Ravi s/o\u00a0Kumar", "Ravi d/o\u2003Kumar", "Ravi s/o")) {
            assertThrows(ParseException.class, () -> new PersonEditParser().parse("1 n/" + value), value);
        }
    }

    @Test
    public void parse_numericAddressParts_skipsOnlyDigitSurroundedSlashes() throws Exception {
        PersonEditParser edit = new PersonEditParser();
        for (String address : List.of("1/2", "Blk 12/34", "Block A12/34B", "Blk #01/02", "1/2/3")) {
            assertEquals(Field.set(new Address(address)), edit.parse("1 a/" + address).edit().address());
        }
        for (String address : List.of("A/B", "#01//02", "/", "1/", "A/2", "1/A", "1/２")) {
            assertThrows(ParseException.class, () -> edit.parse("1 a/Block " + address), address);
        }
        // A supported prefix still begins its field, even after a numeric slash component.
        var parsed = edit.parse("1 a/Blk #01/02 n/House");
        assertEquals(Field.set(new Name("House")), parsed.edit().name());
        assertEquals(Field.set(new Address("Blk #01/02")), parsed.edit().address());
    }
}
