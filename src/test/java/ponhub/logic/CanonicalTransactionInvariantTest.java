package ponhub.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ponhub.logic.commands.CommandResult;
import ponhub.logic.commands.exceptions.CommandException;
import ponhub.model.PeopleView;
import ponhub.model.PonHubData;
import ponhub.model.PonHubDataState;
import ponhub.model.person.PersonRole;
import ponhub.model.search.SearchCategory;
import ponhub.model.search.SearchCriteria;
import ponhub.model.search.SearchField;

/** Exercises production assertions in fresh JVMs rather than relying on the test worker's assertion flags. */
public class CanonicalTransactionInvariantTest {
    @TempDir
    Path directory;

    @Test
    public void assertionsEnabled_detectsLiveDataAndFilterMisuseBeforeSaving() throws Exception {
        runProbe(true);
    }

    @Test
    public void assertionsDisabled_preservesValidNoOpProtectionAndFailedSaveBehavior() throws Exception {
        runProbe(false);
    }

    private void runProbe(boolean enabled) throws Exception {
        String classpath = System.getProperty("ponhub.test.runtimeClasspath");
        assertNotNull(classpath, "Run through Gradle to supply the complete runtime classpath");
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", executable);
        Path output = directory.resolve("assertions-" + enabled + ".txt");
        List<String> command = new ArrayList<>(List.of(java.toString()));
        // Keep the worker's working directory and coverage destination, as in LogsCenterTest's child checks.
        ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(argument -> argument.matches("-javaagent:(?:.*[/\\\\])?jacocoagent\\.jar(?:=.*)?"))
                .forEach(command::add);
        command.addAll(List.of(enabled ? "-ea" : "-da", "-cp", classpath,
                InvariantProbe.class.getName(), Boolean.toString(enabled)));
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "Invariant probe timed out");
            String text = Files.readString(output);
            assertEquals(0, process.exitValue(), text);
            assertTrue(text.contains("PASS assertions=" + enabled), text);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
            }
        }
    }

    /** Child entry point uses explicit checks, which remain active under both -ea and -da. */
    public static final class InvariantProbe {
        public static void main(String[] arguments) throws Exception {
            boolean enabled = Boolean.parseBoolean(arguments[0]);
            check(CanonicalCommandExecutor.class.desiredAssertionStatus() == enabled, "Wrong assertion mode");
            verifyValidAndNoOp();
            verifyFailure(false);
            verifyFailure(true);
            if (enabled) {
                verifyMisuse("data");
                verifyMisuse("role");
                verifyMisuse("search");
            }
            System.out.println("PASS assertions=" + enabled);
        }

        private static void verifyValidAndNoOp() throws Exception {
            PonHubData live = new PonHubData();
            PeopleView view = new PeopleView(live);
            PonHubDataState before = live.exportState();
            PonHubDataState candidate = withCounter(before, 1);
            int[] saves = {0};
            CanonicalCommandExecutor executor = new CanonicalCommandExecutor(view, state -> {
                check(before.equals(live.exportState()), "Published before saving");
                check(state.equals(candidate), "Wrong saved snapshot");
                saves[0]++;
            }, Optional::empty);
            executor.execute((staged, stagedView) -> {
                staged.resetData(candidate);
                stagedView.setRoleFilter(Optional.of(PersonRole.STUDENT));
                return new CommandResult("prepared");
            });
            check(saves[0] == 1 && live.exportState().equals(candidate), "Valid candidate not published");
            check(view.getRoleFilter().equals(Optional.of(PersonRole.STUDENT)), "Staged filter not published");
            executor.execute((staged, stagedView) -> new CommandResult("no-op"));
            executor.execute((staged, stagedView) -> {
                stagedView.setRoleFilter(Optional.of(PersonRole.TUTOR));
                return new CommandResult("view only");
            });
            check(saves[0] == 1, "Read/view-only command saved data");
            check(view.getRoleFilter().equals(Optional.of(PersonRole.TUTOR)), "View-only filter not published");
            try {
                executor.execute((staged, stagedView) -> {
                    staged.resetData(withCounter(before, -1));
                    return new CommandResult("invalid snapshot");
                });
                throw new AssertionError("Runtime snapshot validation was disabled");
            } catch (IllegalArgumentException expected) {
                check(saves[0] == 1 && live.exportState().equals(candidate), "Invalid snapshot was saved/published");
            }
        }

        private static void verifyFailure(boolean protectedSession) throws Exception {
            PonHubData live = new PonHubData();
            PeopleView view = new PeopleView(live);
            PonHubDataState before = live.exportState();
            int[] saves = {0};
            CanonicalCommandExecutor executor = new CanonicalCommandExecutor(view, state -> {
                saves[0]++;
                throw new IOException("Synthetic save failure");
            }, () -> protectedSession ? Optional.of("Protected session") : Optional.empty());
            try {
                executor.execute((staged, stagedView) -> {
                    staged.resetData(withCounter(before, 1));
                    stagedView.setRoleFilter(Optional.of(PersonRole.STUDENT));
                    return new CommandResult("must not publish");
                });
                throw new AssertionError("Expected runtime rejection with either assertion mode");
            } catch (CommandException expected) {
                check(protectedSession ? expected.getMessage().equals("Protected session")
                        : expected.getCause() instanceof IOException, "Wrong failure category");
            }
            check(saves[0] == (protectedSession ? 0 : 1), "Incorrect save attempt count");
            check(before.equals(live.exportState()) && view.getRoleFilter().isEmpty(), "Published failed staging");
        }

        private static void verifyMisuse(String kind) throws Exception {
            PonHubData live = new PonHubData();
            PeopleView view = new PeopleView(live);
            if (kind.equals("search")) {
                view.setRoleFilter(Optional.of(PersonRole.STUDENT));
            }
            PonHubDataState before = live.exportState();
            int[] saves = {0};
            CanonicalCommandExecutor executor = new CanonicalCommandExecutor(view, state -> saves[0]++,
                    Optional::empty);
            try {
                executor.execute((staged, stagedView) -> {
                    if (!kind.equals("search")) {
                        staged.resetData(withCounter(before, 2));
                    }
                    if (kind.equals("role")) {
                        view.setRoleFilter(Optional.of(PersonRole.STUDENT));
                    } else if (kind.equals("search")) {
                        view.setSearchCriteria(new SearchCriteria(SearchCategory.STUDENT,
                                Map.of(SearchField.NAME, "Synthetic")));
                    } else {
                        live.resetData(withCounter(before, 1));
                    }
                    return new CommandResult("faulty callback");
                });
                throw new AssertionError("Misuse was not detected");
            } catch (AssertionError expected) {
                check("Staged commands must leave live data and filters unchanged".equals(expected.getMessage()),
                        "Missing production invariant failure");
            }
            check(saves[0] == 0, "Misuse reached the writer");
            check(live.exportState().lastAllocatedLessonSequence() != 2, "Published staged misuse");
        }

        private static PonHubDataState withCounter(PonHubDataState before, long counter) {
            return new PonHubDataState(before.people(), List.of(), List.of(), counter);
        }

        private static void check(boolean condition, String message) {
            if (!condition) {
                throw new AssertionError(message);
            }
        }
    }
}
