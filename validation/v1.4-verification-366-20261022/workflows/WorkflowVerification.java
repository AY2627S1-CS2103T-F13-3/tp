package ponhub.logic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import ponhub.logic.commands.exceptions.CommandException;
import ponhub.logic.parser.exceptions.ParseException;
import ponhub.model.PonHubDataState;
import ponhub.model.person.Student;
import ponhub.model.query.AttendanceHistoryEntry;
import ponhub.model.query.LessonDetailView;
import ponhub.storage.JsonPonHubDataStorage;
import ponhub.storage.ProtectedPonHubDataStorage;

/**
 * Replays independently specified UG/DG contracts without a native display or production changes.
 */
public final class WorkflowVerification {
    private static int commands;
    private static int assertions;
    private static int groups;

    private WorkflowVerification() {
    }

    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void startGroup(String description) {
        groups++;
        System.out.println("\nGROUP " + groups + ": " + description);
    }

    private static String run(CanonicalPeopleSession session, String command) throws Exception {
        commands++;
        String feedback = session.execute(command).getFeedbackToUser();
        System.out.println(command + " => " + feedback.replace('\n', ' '));
        return feedback;
    }

    private static void expect(CanonicalPeopleSession session, String command, String expected) throws Exception {
        require(run(session, command).equals(expected), "Unexpected feedback for " + command);
    }

    private static List<String> getPeopleIds(CanonicalPeopleSession session) {
        return session.getPeople().stream().map(person -> person.getId().toString()).toList();
    }

    private static List<String> getLessonIds(CanonicalPeopleSession session) {
        return session.getDisplayedLessons().stream().map(lesson -> lesson.getLessonId().toString()).toList();
    }

    private static void expectPeople(CanonicalPeopleSession session, String... expected) {
        require(getPeopleIds(session).equals(List.of(expected)), "People mismatch: " + getPeopleIds(session));
    }

    private static void expectLessons(CanonicalPeopleSession session, String... expected) {
        require(getLessonIds(session).equals(List.of(expected)), "Lesson mismatch: " + getLessonIds(session));
    }

    private static void reject(CanonicalPeopleSession session, String command) throws Exception {
        PonHubDataState state = session.getDataSnapshot();
        byte[] bytes = Files.readAllBytes(session.getDataFilePath());
        var visiblePeople = List.copyOf(session.getPeople());
        var visibleLessons = List.copyOf(session.getDisplayedLessons());
        var visibleHistory = List.copyOf(session.getDisplayedHistory());
        String lessonContext = session.lessonResultContextProperty().get();
        String historyContext = session.historyResultContextProperty().get();
        commands++;
        try {
            session.execute(command);
            throw new AssertionError("Accepted invalid command: " + command);
        } catch (CommandException | ParseException expected) {
            System.out.println(command.replace("\n", "\\n") + " => REJECT: "
                    + expected.getMessage().replace('\n', ' '));
        }
        require(state.equals(session.getDataSnapshot()), "Rejected command changed aggregate: " + command);
        require(Arrays.equals(bytes, Files.readAllBytes(session.getDataFilePath())),
                "Rejected command changed bytes: " + command);
        require(visiblePeople.equals(session.getPeople()), "Rejected command changed people: " + command);
        require(visibleLessons.equals(session.getDisplayedLessons()), "Rejected command changed lessons: " + command);
        require(visibleHistory.equals(session.getDisplayedHistory()), "Rejected command changed history: " + command);
        require(lessonContext.equals(session.lessonResultContextProperty().get()), "Lesson context changed");
        require(historyContext.equals(session.historyResultContextProperty().get()), "History context changed");
    }

    private static void expectNoOp(CanonicalPeopleSession session, String command, String expected) throws Exception {
        PonHubDataState state = session.getDataSnapshot();
        byte[] bytes = Files.readAllBytes(session.getDataFilePath());
        expect(session, command, expected);
        require(state.equals(session.getDataSnapshot()), "No-op changed aggregate");
        require(Arrays.equals(bytes, Files.readAllBytes(session.getDataFilePath())), "No-op changed bytes");
    }

    private static void verifyProtected(Path directory, Path validFile) throws Exception {
        startGroup("Corrupt, legacy and unsupported startup protection; same-session lock and recovery restart");
        String[] invalidInputs = {"{", "{\"persons\":[]}", "{\"schemaVersion\":99}"};
        for (int i = 0; i < invalidInputs.length; i++) {
            Path file = directory.resolve("protected-" + i + ".json");
            Files.writeString(file, invalidInputs[i]);
            CanonicalPeopleSession protectedSession = new CanonicalPeopleSession(file);
            require(protectedSession.getDataLoadError().isPresent(), "Rejected input was writable");
            require(protectedSession.getCommandWords().equals(List.of("help", "list", "exit")),
                    "Protected command catalogue leaked write routes");
            require(run(protectedSession, "help").contains("restart"), "Missing recovery guidance");
            run(protectedSession, "list r/student");
            for (String command : List.of("edit 1 n/Changed", "editlesson lid/L1 s/Changed", "add",
                    "enrol 1 lid/L1", "mark 1 lid/L1 d/2026-10-05 s/present", "search c/student",
                    "help edit", "help editlesson")) {
                reject(protectedSession, command);
            }
            Files.copy(validFile, file, StandardCopyOption.REPLACE_EXISTING);
            reject(protectedSession, "edit 1 n/Changed");
            CanonicalPeopleSession recovered = new CanonicalPeopleSession(file);
            require(recovered.getDataLoadError().isEmpty(), "Restored data did not load on restart");
            require(recovered.getDataSnapshot().equals(new CanonicalPeopleSession(validFile).getDataSnapshot()),
                    "Recovered data differs from valid backup");
        }
    }

    private static void verifyFailedSave(Path file) throws Exception {
        startGroup("Injected failed-save transactions, unchanged active results and one successful retry");
        AtomicBoolean shouldFail = new AtomicBoolean(true);
        AtomicInteger saves = new AtomicInteger();
        CanonicalPeopleSession session = new CanonicalPeopleSession(new ProtectedPonHubDataStorage(file), state -> {
            saves.incrementAndGet();
            if (shouldFail.get()) {
                throw new IOException("Issue 366 injected write failure");
            }
            new JsonPonHubDataStorage(file).saveData(state);
        });
        run(session, "search c/student n/Alex");
        run(session, "search c/lesson s/Math");
        for (String command : List.of("edit 1 n/Alex Failed", "editlesson lid/L2 s/Failed",
                "add r/parent n/Failed Parent p/98765432", "enrol 1 lid/L4")) {
            reject(session, command);
        }
        require(saves.get() == 4, "Failure seam did not receive each intended write");
        shouldFail.set(false);
        expect(session, "edit 1 n/Alex Retried", "Edited student: Alex Retried (S1).");
        require(saves.get() == 5, "Retry did not attempt exactly one additional save");
        require(session.getDataSnapshot().equals(new CanonicalPeopleSession(file).getDataSnapshot()),
                "Successful retry did not survive reload");
        expectNoOp(session, "edit 1 n/Alex  Retried", "No changes to student: Alex Retried (S1).");
        require(saves.get() == 5, "No-op called persistence writer");
    }

    /**
     * Runs the source-session campaign in a previously empty disposable directory.
     *
     * @param args one fresh disposable directory for the campaign data files.
     */
    public static void main(String[] args) throws Exception {
        Path directory = Path.of(args[0]);
        Files.createDirectories(directory);
        Path file = directory.resolve("workflow.json");
        require(!Files.exists(file), "Use a fresh disposable directory");
        CanonicalPeopleSession session = new CanonicalPeopleSession(file);

        startGroup("Missing-file startup and read-only commands");
        run(session, "help list");
        expect(session, "list", "Showing 0 person(s).");
        expect(session, "lessons", "Showing 0 lesson(s).");
        expect(session, "search c/student", "Found 0 student(s).");
        require(!Files.exists(file), "Read-only commands created operational data");

        startGroup("All roles, distinct duplicate keys, leading-zero phones and ambiguous Tutor selection");
        for (String command : List.of("add r/tutor n/Mei Lim p/92345678", "add r/tutor n/Ravi Kumar p/93456789",
                "add r/tutor n/Mei Lim p/94567890", "add r/student n/Alex Tan l/S2 pp/00123456 p/98887777",
                "add r/student n/Jamie Tan l/P1 pp/00123456", "add r/student n/Alex Tan l/P3 pp/00987654",
                "add r/parent n/Pat Tan p/00123456", "add r/parent n/Other Parent p/0123456")) {
            run(session, command);
        }
        expectPeople(session, "T1", "T2", "T3", "S1", "S2", "S3", "P1", "P2");
        reject(session, "add r/student n/ALEX  TAN l/P4 pp/00123456");
        reject(session, "addlesson d/Mon st/1600 et/1730 s/Math tu/Mei Lim rm/R1");
        run(session, "addlesson d/Mon st/1600 et/1730 s/Math tu/Mei Lim tp/92345678 rm/R1");
        reject(session, "addlesson d/Mon st/1700 et/1800 s/English tu/Mei Lim tp/92345678 rm/R9");
        reject(session, "addlesson d/Mon st/1700 et/1800 s/English tu/Ravi Kumar rm/R1");
        run(session, "addlesson d/Tue st/1600 et/1730 s/Math tu/Mei Lim tp/92345678 rm/R1");
        run(session, "addlesson d/Mon st/1800 et/1900 s/Science tu/Mei Lim tp/92345678 rm/R1");
        run(session, "addlesson d/Mon st/1900 et/2000 s/Math tu/Mei Lim tp/92345678 rm/R2");
        require(session.getDataSnapshot().lastAllocatedLessonSequence() == 4, "Rejected creation consumed ID");
        run(session, "list r/student");
        run(session, "enrol 2 lid/L1");
        run(session, "enrol 1 lid/L2");
        run(session, "enrol 1 lid/L3");
        reject(session, "enrol 2 lid/L1");

        startGroup("UG compound search matrix; same-Lesson/same-child matching and independent result panels");
        run(session, "search c/student n/alex l/s2 pp/00123456");
        expectPeople(session, "S1");
        run(session, "search c/student d/Mon s/Math");
        expectPeople(session, "S2");
        run(session, "search c/parent sn/Alex d/Mon s/Math");
        expectPeople(session);
        run(session, "search c/tutor n/mei sn/Alex d/Mon s/Math");
        expectPeople(session);
        run(session, "search c/lesson d/Mon s/Math");
        expectLessons(session, "L1", "L4");
        expectPeople(session);
        run(session, "search c/parent sn/Alex");
        expectPeople(session, "P1");
        run(session, "search c/parent p/0123456 sn/Alex");
        expectPeople(session);
        run(session, "list r/student");
        run(session, "enrol 1 lid/L1");
        run(session, "search c/parent sn/Alex d/Mon s/Math");
        expectPeople(session, "P1");
        run(session, "search c/tutor n/mei sn/Alex d/Mon s/Math");
        expectPeople(session, "T1");
        run(session, "search c/lesson d/Mon st/1600 s/Math tu/mei rm/r1 sn/a");
        expectLessons(session, "L1");
        expectPeople(session, "T1");

        startGroup("Current displayed positions, rejected selectors and parser validation preserve state/views/bytes");
        run(session, "search c/student n/Jamie");
        expectPeople(session, "S2");
        expect(session, "edit 1 l/P2", "Edited student: Jamie Tan (S2).");
        require(((Student) session.getPeople().getFirst()).getLevel().toString().equals("P2"),
                "Displayed position was treated as stable-ID suffix");
        for (String command : List.of("edit S2 n/Wrong", "edit 0 n/Wrong", "edit 2 n/Wrong", "edit 1",
                "edit 1 n/", "edit 1 pp/", "edit 1 p/abc", "edit 1 r/tutor", "edit 1 n/A n/B",
                "edit 1 a/Block x/value", "search c/student sn/Alex", "search c/tutor tu/Mei",
                "search c/student p/", "search c/student n/A n/B", "search c/lesson st/1800 et/1700",
                "showlesson lid/L999", "showlesson lid/L1 d/2026-10-06", "help add extra", "ADD",
                "list\n")) {
            reject(session, command);
        }
        run(session, "list r/tutor");
        reject(session, "enrol 1 lid/L1");
        reject(session, "history 1 lid/L1");
        reject(session, "edit 1 p/");
        reject(session, "edit 1 l/P2");
        reject(session, "edit 1 pp/00123456");
        run(session, "list r/parent");
        reject(session, "edit 1 p/");
        reject(session, "edit 1 l/P2");

        startGroup("Person optional clears, normalized no-op, Tutor label and exact Parent relationship refresh");
        run(session, "list r/student");
        run(session, "edit 1 e/alex@example.com a/10 Main Road");
        run(session, "edit 1 p/ e/ a/");
        require(session.getPeople().getFirst().getContactDetails().getPhone().isEmpty(), "Phone clear failed");
        require(session.getPeople().getFirst().getContactDetails().getEmail().isEmpty(), "Email clear failed");
        require(session.getPeople().getFirst().getContactDetails().getAddress().isEmpty(), "Address clear failed");
        expectNoOp(session, "edit 1 n/Alex  Tan", "No changes to student: Alex Tan (S1).");
        reject(session, "edit 1 n/Jamie Tan pp/00123456");
        run(session, "list r/tutor");
        run(session, "showlesson lid/L1 d/2026-10-05");
        run(session, "edit 1 n/Corrected Tutor");
        require(session.getDisplayedLessons().getFirst().getTutorName().equals("Corrected Tutor"),
                "Tutor label did not refresh");
        run(session, "search c/parent sn/Jamie");
        expectPeople(session, "P1");
        run(session, "edit 1 p/90000000");
        expectPeople(session);
        run(session, "list r/student");
        run(session, "edit 2 pp/90000000");
        run(session, "search c/parent sn/Jamie");
        expectPeople(session, "P1");

        startGroup("Lesson search refresh, partial endpoints, invalid/ambiguous replacements and retained identities");
        run(session, "search c/student d/Mon s/Math");
        expectPeople(session, "S1", "S2");
        run(session, "search c/lesson d/Mon st/1600 s/Math tu/corrected rm/r1 sn/a");
        run(session, "editlesson lid/L1 s/English");
        expectPeople(session);
        expectLessons(session);
        run(session, "editlesson lid/L1 s/Math");
        expectPeople(session, "S1", "S2");
        expectLessons(session, "L1");
        run(session, "editlesson lid/L1 st/1630");
        run(session, "editlesson lid/L1 et/1745");
        expectNoOp(session, "editlesson lid/L1 s/ Math d/MON st/1630 et/1745 rm/r1",
                "No changes to lesson: L1.");
        for (String command : List.of("editlesson lid/L1", "editlesson lid/L999 s/Math",
                "editlesson lid/L1 st/1800", "editlesson lid/L1 et/1600", "editlesson lid/L1 s/",
                "editlesson lid/L1 tp/92345678", "editlesson lid/L1 tu/No Such Tutor",
                "editlesson lid/L1 tu/Mei Lim tp/92345678", "editlesson lid/L1 d/Mon st/1830 et/1930")) {
            reject(session, command);
        }
        require(session.getDataSnapshot().lastAllocatedLessonSequence() == 4, "Edit allocated Lesson ID");

        startGroup("Independent dated attendance, no-op, correction and current/unrecorded/former distinctions");
        run(session, "list r/student");
        run(session, "mark 1 lid/L1 d/2026-10-05 s/present");
        run(session, "mark 2 lid/L1 d/2026-10-05 s/absent");
        expectNoOp(session, "mark 1 lid/L1 d/2026-10-05 s/present",
                "Attendance for Student S1 in lesson L1 on 2026-10-05 is already marked present. No changes made.");
        run(session, "mark 1 lid/L1 d/2026-10-05 s/absent");
        require(session.getDataSnapshot().attendance().size() == 2, "Correction appended attendance");
        reject(session, "mark 1 lid/L1 d/2026-10-06 s/present");
        reject(session, "mark 1 lid/L1 d/2026-02-30 s/present");
        run(session, "showlesson lid/L1 d/2026-10-12");
        LessonDetailView emptyDate = (LessonDetailView) session.getDisplayedLessons().getFirst();
        require(emptyDate.currentRoster().stream().allMatch(row -> row.status().isEmpty()),
                "Missing attendance appeared absent");
        run(session, "history 1 lid/L1");
        run(session, "unenrol 1 lid/L1");
        AttendanceHistoryEntry former = session.getDisplayedHistory().getFirst();
        require(!former.isCurrentlyEnrolled() && former.getStatus() == AttendanceHistoryEntry.Status.ABSENT,
                "Former history did not refresh");
        run(session, "mark 1 lid/L1 d/2026-10-05 s/present");
        reject(session, "mark 1 lid/L1 d/2026-10-12 s/present");
        reject(session, "editlesson lid/L1 d/Tue");
        run(session, "showlesson lid/L1 d/2026-10-05");
        LessonDetailView detail = (LessonDetailView) session.getDisplayedLessons().getFirst();
        require(detail.currentRoster().size() == 1 && detail.formerAttendance().size() == 1,
                "Current and former sections not independent");
        run(session, "search c/lesson sn/Alex d/Mon s/Math");
        expectLessons(session);
        reject(session, "delete 1");
        reject(session, "deletelesson lid/L1");

        startGroup("Restart persistence, retained-history deletion guards and monotonic Lesson IDs");
        PonHubDataState saved = session.getDataSnapshot();
        session = new CanonicalPeopleSession(file);
        require(saved.equals(session.getDataSnapshot()), "First restart changed saved aggregate");
        run(session, "list r/student");
        run(session, "history 1 lid/L1");
        require(!session.getDisplayedHistory().getFirst().isCurrentlyEnrolled(), "Former membership did not reload");
        run(session, "unmark 1 lid/L1 d/2026-10-05");
        expectNoOp(session, "unmark 1 lid/L1 d/2026-10-05",
                "No attendance record exists for Student S1 in lesson L1 on 2026-10-05. No changes made.");
        run(session, "unenrol 2 lid/L1");
        reject(session, "deletelesson lid/L1");
        run(session, "unmark 2 lid/L1 d/2026-10-05");
        run(session, "showlesson lid/L1 d/2026-10-05");
        run(session, "editlesson lid/L1 d/Wed");
        require(((LessonDetailView) session.getDisplayedLessons().getFirst()).date().isEmpty(),
                "Allowed weekday change retained invalid selected date");
        run(session, "deletelesson lid/L1");
        expect(session, "addlesson d/Mon st/1600 et/1730 s/Math tu/Corrected Tutor rm/R1",
                "New lesson added: L5.");
        run(session, "lessons");
        expectLessons(session, "L2", "L3", "L4", "L5");

        startGroup("Read-only restart byte stability and all-role index reset");
        byte[] readBytes = Files.readAllBytes(file);
        saved = session.getDataSnapshot();
        session = new CanonicalPeopleSession(file);
        for (String command : List.of("list r/tutor", "lessons", "search c/lesson s/Math", "help editlesson",
                "list r/student", "lessons si/1", "history 1", "showlesson lid/L5 d/2026-10-05",
                "list r/parent", "list")) {
            run(session, command);
        }
        expectPeople(session, "T1", "T2", "T3", "S1", "S2", "S3", "P1", "P2");
        require(saved.equals(session.getDataSnapshot()), "Read commands changed aggregate");
        require(Arrays.equals(readBytes, Files.readAllBytes(file)), "Read commands changed persisted bytes");
        require(session.execute("exit").isExit(), "Exit did not signal shutdown");
        commands++;
        System.out.println("exit => shutdown requested");

        verifyProtected(directory, file);
        verifyFailedSave(file);
        System.out.println("\nPASS: " + groups + " scenario groups, " + commands + " command submissions, "
                + assertions + " assertions. Source/session evidence only; no native-display claim.");
    }
}
