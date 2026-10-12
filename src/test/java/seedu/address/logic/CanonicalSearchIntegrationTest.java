package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.SearchCriteriaParser;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.PeopleView;
import seedu.address.model.PonHubData;
import seedu.address.model.PonHubDataState;
import seedu.address.model.person.Address;
import seedu.address.model.person.ContactDetails;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Parent;
import seedu.address.model.person.PeopleRegistryState;
import seedu.address.model.person.PersonId;
import seedu.address.model.person.PersonRecord;
import seedu.address.model.person.PersonRole;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Student;
import seedu.address.model.person.Tutor;
import seedu.address.storage.JsonPonHubDataStorage;
import seedu.address.storage.ProtectedPonHubDataStorage;

/**
 * Verifies active routing, stable view/index semantics and persistence boundaries for own-contact searches.
 */
public class CanonicalSearchIntegrationTest {
    @TempDir
    Path directory;

    @Test
    public void execute_categories_searchWholeRootAndPreserveBytesAndObservableIdentity() throws Exception {
        Path path = saveFixture();
        byte[] bytes = Files.readAllBytes(path);
        CanonicalPeopleSession session = readOnlySession(path);
        var people = session.getPeople();
        PonHubDataState before = session.getDataSnapshot();

        session.execute("list r/parent");
        assertEquals("Found 2 student(s).", session.execute("search c/StUdEnT").getFeedbackToUser());
        assertEquals(List.of("S9", "S3"), ids(session));
        assertEquals(new PersonId("S3"), PersonIndexResolver.resolveStudent(Index.fromOneBased(2), people).getId());
        assertEquals("Found 1 tutor(s).", session.execute("search c/tutor").getFeedbackToUser());
        assertEquals(List.of("T7"), ids(session));
        assertThrows(CommandException.class, () -> PersonIndexResolver.resolveStudent(Index.fromOneBased(1), people));
        assertEquals("Found 1 parent(s).", session.execute("search c/parent").getFeedbackToUser());
        assertEquals(List.of("P2"), ids(session));
        assertSame(people, session.getPeople());
        assertEquals(before, session.getDataSnapshot());
        assertArrayEquals(bytes, Files.readAllBytes(path));
    }

    @Test
    public void execute_allOwnFilters_combinesOnOnePersonAndNormalizesText() throws Exception {
        CanonicalPeopleSession session = readOnlySession(saveFixture());
        assertEquals("Found 1 student(s).", session.execute("search c/student n/alex   TAN l/s2"
                + " p/00012345 pp/00123456 e/@EXAMPLE a/main   ROAD").getFeedbackToUser());
        assertEquals(List.of("S3"), ids(session));
        assertEquals(new PersonId("S3"), PersonIndexResolver.resolveStudent(
                Index.fromOneBased(1), session.getPeople()).getId());
        session.execute("search c/student n/Alex p/00012345 pp/00999999");
        assertTrue(session.getPeople().isEmpty());
        session.execute("search c/tutor n/mei p/00111111 e/MEI@ a/upper   road");
        assertEquals(List.of("T7"), ids(session));
        session.execute("search c/parent n/pat p/00123456 e/@EXAMPLE a/main road");
        assertEquals(List.of("P2"), ids(session));
    }

    @Test
    public void execute_absentOptionalFieldsAndExactPhones_returnsSuccessfulEmptyResults() throws Exception {
        CanonicalPeopleSession session = readOnlySession(saveFixture());
        for (String command : List.of("search c/student n/Missing p/00012345",
                "search c/student n/Missing e/@example", "search c/student n/Missing a/road",
                "search c/student p/12345", "search c/student pp/123456", "search c/student l/P1")) {
            assertEquals("Found 0 student(s).", session.execute(command).getFeedbackToUser());
            assertTrue(session.getPeople().isEmpty());
        }
    }

    @Test
    public void execute_sameRoleSearchChangesAndList_clearPreviousCriteria() throws Exception {
        CanonicalPeopleSession session = readOnlySession(saveFixture());
        session.execute("search c/student n/Alex");
        assertEquals(List.of("S3"), ids(session));
        session.execute("search c/student n/Missing");
        assertEquals(List.of("S9"), ids(session));
        session.execute("list r/student");
        assertEquals(List.of("S9", "S3"), ids(session));
        session.execute("search c/parent n/no-match");
        session.execute("list");
        assertEquals(List.of("T7", "S9", "P2", "S3"), ids(session));
    }

    @Test
    public void execute_invalidAndUnavailableFilters_preservesCurrentViewAndBytes() throws Exception {
        Path path = saveFixture();
        byte[] bytes = Files.readAllBytes(path);
        CanonicalPeopleSession session = readOnlySession(path);
        session.execute("search c/student n/Alex");
        for (String command : List.of("search", "search c/student n/", "search c/student n/Alex n/Other",
                "search c/student n/Alex xyz/no", "search c/tutor l/S2", "search c/parent pp/00123456",
                "search c/student p/12", "search c/unknown", "search c/student n/Alex\n")) {
            assertThrows(ParseException.class, () -> session.execute(command));
            assertEquals(List.of("S3"), ids(session));
        }
        for (String command : List.of("search c/student d/Mon", "search c/parent sn/Alex",
                "search c/tutor rm/R1")) {
            CommandException failure = assertThrows(CommandException.class, () -> session.execute(command));
            assertTrue(failure.getMessage().contains("not available"));
            assertEquals(List.of("S3"), ids(session));
        }
        assertEquals("Found 0 lesson(s).", session.execute("search c/lesson").getFeedbackToUser());
        assertArrayEquals(bytes, Files.readAllBytes(path));
        assertEquals(List.of("S3"), ids(session));
    }

    @Test
    public void execute_help_retainsSearchAndDocumentsOnlyAvailableSlice() throws Exception {
        CanonicalPeopleSession session = readOnlySession(saveFixture());
        session.execute("search c/student n/Alex");
        assertTrue(session.execute("help").getFeedbackToUser().contains("search -"));
        String help = session.execute("help SEARCH").getFeedbackToUser();
        assertTrue(help.contains("pp/PARENT_PHONE"));
        assertTrue(help.contains("not available yet"));
        assertEquals(List.of("S3"), ids(session));
    }

    @Test
    public void execute_missingAndProtectedFiles_doNotCreateOrRewriteData() throws Exception {
        Path path = directory.resolve("missing/ponhub.json");
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        assertEquals("Found 0 student(s).", session.execute("search c/student").getFeedbackToUser());
        session.execute("help search");
        assertFalse(Files.exists(path.getParent()));

        Files.writeString(directory.resolve("bad.json"), "{");
        CanonicalPeopleSession protectedSession = new CanonicalPeopleSession(directory.resolve("bad.json"));
        assertThrows(CommandException.class, () -> protectedSession.execute("search c/student"));
        assertThrows(ParseException.class, () -> protectedSession.execute("help search"));
        assertFalse(protectedSession.execute("help").getFeedbackToUser().contains("search -"));
        assertEquals("{", Files.readString(directory.resolve("bad.json")));
    }

    @Test
    public void execute_failedAddAndRetry_preservesSearchThenResetsToAllRolesAfterSave() throws Exception {
        Path path = saveFixture();
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        byte[] bytes = Files.readAllBytes(path);
        int[] writes = {0};
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            if (writes[0]++ == 0) {
                throw new IOException("Injected search-view save failure");
            }
            storage.saveData(state);
        });
        session.execute("search c/student n/Alex");
        String add = "add r/parent n/New Parent p/00987654";
        assertThrows(CommandException.class, () -> session.execute(add));
        assertEquals(List.of("S3"), ids(session));
        session.execute("help search");
        assertEquals(List.of("S3"), ids(session));
        assertArrayEquals(bytes, Files.readAllBytes(path));
        session.execute(add);
        assertEquals(List.of("T7", "S9", "P2", "S3", "P3"), ids(session));
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
        assertEquals(2, writes[0]);
    }

    @Test
    public void execute_addLessonDuringSearch_preservesPeopleAndRefreshesPersistedLessonQueries() throws Exception {
        Path path = saveFixture();
        CanonicalPeopleSession session = new CanonicalPeopleSession(path);
        session.execute("search c/student n/Alex l/S2 p/00012345 pp/00123456 e/@example a/main road");
        var people = session.getPeople();
        List<PersonRecord> matches = List.copyOf(people);
        var queries = session.getLessonQueries();
        var lessons = queries.getLessonList();

        assertEquals("New lesson added: L1.", session.execute(
                "addlesson d/Mon st/0900 et/1000 s/Math tu/Mei Lim tp/00111111 rm/R1").getFeedbackToUser());

        assertSame(people, session.getPeople());
        assertEquals(matches, session.getPeople());
        assertEquals(List.of("S3"), ids(session));
        assertEquals(Optional.of(PersonRole.STUDENT), session.getRoleFilter());
        assertEquals(new PersonId("S3"), PersonIndexResolver.resolveStudent(Index.fromOneBased(1), people).getId());
        assertSame(queries, session.getLessonQueries());
        assertSame(lessons, queries.getLessonList());
        assertEquals(1, lessons.size());
        assertEquals(0, lessons.getFirst().getRosterSize());
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
        session.execute("help addlesson");
        assertEquals(matches, session.getPeople());
    }

    @Test
    public void execute_failedLessonSaveAndRetry_preservesSearchBytesQueriesAndNextLessonId() throws Exception {
        Path path = saveFixture();
        JsonPonHubDataStorage storage = new JsonPonHubDataStorage(path);
        byte[] bytes = Files.readAllBytes(path);
        int[] writes = {0};
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            if (writes[0]++ == 0) {
                throw new IOException("Injected lesson save failure during search");
            }
            storage.saveData(state);
        });
        session.execute("search c/student n/Alex l/S2 p/00012345 pp/00123456 e/@example a/main road");
        var people = session.getPeople();
        List<PersonRecord> matches = List.copyOf(people);
        PonHubDataState before = session.getDataSnapshot();
        var lessons = session.getLessonQueries().getLessonList();
        String command = "addlesson d/Tue st/0900 et/1000 s/Math tu/Mei Lim tp/00111111 rm/R1";

        CommandException failure = assertThrows(CommandException.class, () -> session.execute(command));
        assertTrue(failure.getMessage().contains("Injected lesson save failure during search"));
        assertSame(people, session.getPeople());
        assertEquals(matches, session.getPeople());
        assertEquals(Optional.of(PersonRole.STUDENT), session.getRoleFilter());
        assertEquals(before, session.getDataSnapshot());
        assertArrayEquals(bytes, Files.readAllBytes(path));
        assertSame(lessons, session.getLessonQueries().getLessonList());
        assertTrue(lessons.isEmpty());

        assertEquals("New lesson added: L1.", session.execute(command).getFeedbackToUser());
        assertEquals(matches, session.getPeople());
        assertEquals(List.of("S3"), ids(session));
        assertEquals(Optional.of(PersonRole.STUDENT), session.getRoleFilter());
        assertSame(people, session.getPeople());
        assertEquals(1, lessons.size());
        assertEquals(2, writes[0]);
        assertEquals(session.getDataSnapshot(), new CanonicalPeopleSession(path).getDataSnapshot());
    }

    @Test
    public void refresh_replacedAndRestoredRoot_keepsCompleteSearchAndObservableList() throws Exception {
        PonHubData data = new PonHubData(fixture());
        PeopleView view = new PeopleView(data);
        view.setSearchCriteria(new SearchCriteriaParser().parse("c/student n/Alex p/00012345"));
        var people = view.getPeople();
        var criteria = view.getSearchCriteria();
        data.resetData(new PonHubData().exportState());
        view.refresh();
        assertTrue(people.isEmpty());
        assertEquals(criteria, view.getSearchCriteria());
        data.resetData(fixture());
        view.refresh();
        assertSame(people, view.getPeople());
        assertEquals(List.of(new PersonId("S3")), people.stream().map(PersonRecord::getId).toList());
        assertEquals(criteria, view.getSearchCriteria());
    }

    @Test
    public void transaction_operationalChange_preservesSearchAndRecomputesAgainstCommittedRoot() throws Exception {
        PonHubData data = new PonHubData(fixture());
        PeopleView view = new PeopleView(data);
        view.setSearchCriteria(new SearchCriteriaParser().parse("c/student n/Alex"));
        var people = view.getPeople();
        var transaction = view.beginTransaction();
        assertEquals(List.of("S3"), transaction.getStagedPeopleView().getPeople().stream()
                .map(person -> person.getId().toString()).toList());
        transaction.getStagedModel().resetData(new PonHubDataState(new PeopleRegistryState(
                fixture().people().getPeople().stream().filter(person -> !person.getId().toString().equals("S3"))
                        .toList(), fixture().people().getLastAllocatedSequences()), List.of(), List.of(), 0));
        transaction.commit();
        assertSame(people, view.getPeople());
        assertTrue(view.getPeople().isEmpty());
        assertTrue(view.getSearchCriteria().isPresent());
        assertEquals(Optional.of(PersonRole.STUDENT), view.getRoleFilter());
    }

    private Path saveFixture() throws Exception {
        Path path = directory.resolve("ponhub.json");
        new JsonPonHubDataStorage(path).saveData(fixture());
        return path;
    }

    private CanonicalPeopleSession readOnlySession(Path path) {
        return new CanonicalPeopleSession(new ProtectedPonHubDataStorage(path), state -> {
            throw new AssertionError("Search/help/list must not save");
        });
    }

    private static List<String> ids(CanonicalPeopleSession session) {
        return session.getPeople().stream().map(person -> person.getId().toString()).toList();
    }

    private static PonHubDataState fixture() {
        List<PersonRecord> people = List.of(
                new Tutor(new PersonId("T7"), contacts("Mei Lim", "00111111", "mei@example.com", "Upper Road")),
                new Student(new PersonId("S9"), new ContactDetails(new Name("Missing Contacts")),
                        new EducationLevel("S2"), new Phone("00999999")),
                new Parent(new PersonId("P2"), contacts("Pat Tan", "00123456", "pat@example.com", "Main Road")),
                new Student(new PersonId("S3"), contacts("Alex  Tan", "00012345", "alex@example.com", "Main  Road"),
                        new EducationLevel("S2"), new Phone("00123456")));
        return new PonHubDataState(new PeopleRegistryState(people,
                Map.of(PersonRole.TUTOR, 7L, PersonRole.STUDENT, 9L, PersonRole.PARENT, 2L)), List.of(), List.of(), 0);
    }

    private static ContactDetails contacts(String name, String phone, String email, String address) {
        return new ContactDetails(new Name(name), Optional.of(new Phone(phone)), Optional.of(new Email(email)),
                Optional.of(new Address(address)));
    }
}
