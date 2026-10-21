package seedu.address.logic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.query.AttendanceHistoryEntry;

/** Source-session replay of the UG journey; no native or packaged acceptance claim. */
public final class GuideWorkflow {
    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void run(CanonicalPeopleSession session, String command) throws Exception {
        System.out.println(command + " => " + session.execute(command).getFeedbackToUser().replace('\n', ' '));
    }

    public static void main(String[] args) throws Exception {
        Path file = Path.of(args[0]);
        require(!Files.exists(file), "Use a new disposable file");
        CanonicalPeopleSession session = new CanonicalPeopleSession(file);
        run(session, "help");
        run(session, "list");
        require(!Files.exists(file), "Read-only startup wrote operational data");
        for (String command : List.of(
                "add r/tutor n/Mei Lim p/92345678 e/mei@example.com",
                "addlesson d/Mon st/1600 et/1730 s/Math tu/Mei Lim rm/R1",
                "add r/student n/Alex Tan l/S2 pp/91234567",
                "add r/student n/Jamie Tan l/P1 pp/91234567",
                "list r/student", "enrol 1 lid/L1", "enrol 2 lid/L1", "lessons", "lessons si/1",
                "mark 1 lid/L1 d/2026-10-05 s/present",
                "mark 2 lid/L1 d/2026-10-05 s/absent",
                "showlesson lid/L1 d/2026-10-05",
                "mark 1 lid/L1 d/2026-10-05 s/absent", "unenrol 1 lid/L1",
                "lessons si/1", "lessons", "history 1 lid/L1")) {
            run(session, command);
        }
        var former = session.getDisplayedHistory().getFirst();
        require(former.getStudentId().toString().equals("S1") && !former.isCurrentlyEnrolled()
                && former.getStatus() == AttendanceHistoryEntry.Status.ABSENT, "Former history mismatch");
        run(session, "unmark 1 lid/L1 d/2026-10-05");
        byte[] saved = Files.readAllBytes(file);
        run(session, "unmark 1 lid/L1 d/2026-10-05");
        require(Arrays.equals(saved, Files.readAllBytes(file)), "No-op unmark wrote data");
        try {
            session.execute("deletelesson lid/L1");
            throw new AssertionError("Referenced Lesson deletion accepted");
        } catch (CommandException expected) {
            System.out.println("deletelesson lid/L1 => REJECT: " + expected.getMessage());
        }
        require(Arrays.equals(saved, Files.readAllBytes(file)), "Rejected deletion changed bytes");
        for (String command : List.of(
                "addlesson d/Tue st/1600 et/1730 s/Math tu/Mei Lim rm/R1",
                "deletelesson lid/L2", "add r/parent n/Pat Tan p/91234567",
                "list r/student", "list", "help ADD")) {
            run(session, command);
        }
        require(session.execute("exit").isExit(), "Exit did not signal shutdown");
        CanonicalPeopleSession restarted = new CanonicalPeopleSession(file);
        require(restarted.getDataSnapshot().equals(session.getDataSnapshot()), "Restart snapshot mismatch");
        run(restarted, "list r/student");
        require(restarted.getPeople().stream().map(p -> p.getId().toString()).toList().equals(List.of("S1", "S2")),
                "Restart changed stable IDs or student positions");
        run(restarted, "lessons");
        run(restarted, "history 2 lid/L1");
        var current = restarted.getDisplayedHistory().getFirst();
        require(current.getStudentId().toString().equals("S2") && current.isCurrentlyEnrolled()
                && current.getStatus() == AttendanceHistoryEntry.Status.ABSENT, "Jamie history mismatch");
        require(restarted.getDataSnapshot().lessons().size() == 1, "Deleted empty Lesson returned");
        System.out.println("PASS UG quick start, DG attendance state path, no-op/rejection bytes and restart");
    }
}
