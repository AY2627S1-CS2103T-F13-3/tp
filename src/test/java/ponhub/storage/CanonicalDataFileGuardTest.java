package ponhub.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ponhub.model.PonHubData;
import ponhub.model.PonHubDataState;

/**
 * Checks stale snapshots and real operating-system save coordination, including independent JVMs.
 */
public class CanonicalDataFileGuardTest {
    @TempDir
    Path directory;

    @Test
    public void save_twoMissingSessions_preservesWinnerAndRequiresRestart() throws Exception {
        Path path = directory.resolve("new folder/data.json");
        ProtectedPonHubDataStorage first = new ProtectedPonHubDataStorage(path);
        ProtectedPonHubDataStorage second = new ProtectedPonHubDataStorage(path);
        PonHubDataState empty = first.load();
        second.load();
        assertFalse(Files.exists(path.getParent()));

        PonHubDataState changed = new PonHubDataState(empty.people(), List.of(), List.of(), 3);
        first.saveData(changed);
        byte[] saved = Files.readAllBytes(path);
        IOException conflict = assertThrows(IOException.class, () -> second.saveData(empty));
        assertTrue(conflict.getMessage().contains("restart"));
        assertTrue(conflict.getMessage().contains(path.toAbsolutePath().toString()));
        assertArrayEquals(saved, Files.readAllBytes(path));

        // Even restoring the old missing-file condition cannot silently unlock the stale window.
        Files.delete(path);
        assertThrows(IOException.class, () -> second.saveData(empty));
        assertFalse(Files.exists(path));
        first = new ProtectedPonHubDataStorage(path);
        first.load();
        first.saveData(changed);
        assertEquals(changed, new ProtectedPonHubDataStorage(path).load());
    }

    @Test
    public void save_existingSessionsAndExternalEdit_keepsNewerContents() throws Exception {
        Path path = directory.resolve("data.json");
        PonHubDataState empty = new PonHubData().exportState();
        new JsonPonHubDataStorage(path).saveData(empty);
        ProtectedPonHubDataStorage first = new ProtectedPonHubDataStorage(path);
        ProtectedPonHubDataStorage second = new ProtectedPonHubDataStorage(directory.resolve("./data.json"));
        first.load();
        second.load();
        PonHubDataState changed = new PonHubDataState(empty.people(), List.of(), List.of(), 4);
        first.saveData(changed);
        first.saveData(empty);
        first.saveData(changed);
        String saved = Files.readString(path);
        assertThrows(IOException.class, () -> second.saveData(empty));
        assertEquals(saved, Files.readString(path));
        assertEquals(changed, new ProtectedPonHubDataStorage(path).load());

        Files.writeString(path, saved + "\n");
        assertThrows(IOException.class, () -> first.saveData(empty));
        assertEquals(saved + "\n", Files.readString(path));
    }

    @Test
    public void save_deletedLoadedFile_doesNotRecreateStaleSnapshot() throws Exception {
        Path path = directory.resolve("data.json");
        PonHubDataState empty = new PonHubData().exportState();
        new JsonPonHubDataStorage(path).saveData(empty);
        ProtectedPonHubDataStorage storage = new ProtectedPonHubDataStorage(path);
        storage.load();
        Files.delete(path);
        assertThrows(IOException.class, () -> storage.saveData(empty));
        assertFalse(Files.exists(path));
    }

    @Test
    public void save_lockBusyOrInvalidSidecar_neverChangesDataAndCanRetry() throws Exception {
        Path path = directory.resolve("data.json");
        CanonicalDataFileGuard guard = new CanonicalDataFileGuard(path, null);
        Path lockPath = directory.resolve("data.json.ponhub-lock");
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                var lock = channel.lock()) {
            assertTrue(lock.isValid());
            assertTrue(assertThrows(IOException.class, () -> guard.save("new data"))
                    .getMessage().contains("Another PonHub window"));
        }
        assertFalse(Files.exists(path));
        Files.writeString(lockPath, "preserve unexpected contents");
        assertThrows(IOException.class, () -> guard.save("new data"));
        assertEquals("preserve unexpected contents", Files.readString(lockPath));
        assertFalse(Files.exists(path));
        Files.writeString(lockPath, "");
        guard.save("new data");
        assertEquals("new data", Files.readString(path));
        assertEquals(0, Files.size(lockPath));
        assertThrows(IOException.class, () -> new CanonicalDataFileGuard(lockPath, ""));
    }

    @Test
    public void save_failedReplacement_doesNotAdvanceExpectedContents() throws Exception {
        Path path = directory.resolve("data.json");
        CanonicalDataFileGuard guard = new CanonicalDataFileGuard(path, null);
        Files.createDirectory(path);
        assertThrows(IOException.class, () -> guard.save("new data"));
        Files.delete(path);
        guard.save("retry");
        assertEquals("retry", Files.readString(path));
    }

    @Test
    public void save_symbolicLinkAlias_usesTargetGuardAndRejectsRedirectedDestination() throws Exception {
        Path target = directory.resolve("target.json");
        Files.writeString(target, "original");
        Path alias = directory.resolve("alias.json");
        createLink(alias, target);
        CanonicalDataFileGuard direct = new CanonicalDataFileGuard(target, "original");
        CanonicalDataFileGuard linked = new CanonicalDataFileGuard(alias, "original");
        direct.save("newer");
        assertThrows(IOException.class, () -> linked.save("stale"));
        assertEquals("newer", Files.readString(alias));
        assertTrue(Files.isSymbolicLink(alias));
        assertFalse(Files.exists(directory.resolve("alias.json.ponhub-lock")));

        CanonicalDataFileGuard beforeRedirect = new CanonicalDataFileGuard(alias, "newer");
        Path other = directory.resolve("other.json");
        Files.writeString(other, "newer");
        Files.delete(alias);
        createLink(alias, other);
        assertThrows(IOException.class, () -> beforeRedirect.save("redirected"));
        assertEquals("newer", Files.readString(target));
        assertEquals("newer", Files.readString(other));
    }

    @Test
    public void save_symbolicLinkSidecar_preservesItsTarget() throws Exception {
        Path path = directory.resolve("data.json");
        Path original = directory.resolve("preserve.txt");
        Files.writeString(original, "keep");
        createLink(directory.resolve("data.json.ponhub-lock"), original);
        CanonicalDataFileGuard guard = new CanonicalDataFileGuard(path, null);
        assertThrows(IOException.class, () -> guard.save("replacement"));
        assertEquals("keep", Files.readString(original));
        assertFalse(Files.exists(path));
    }

    @Test
    public void save_terminatedLockHolder_releasesOperatingSystemLock() throws Exception {
        Path path = directory.resolve("data.json");
        Path ready = directory.resolve("holder-ready");
        Path log = directory.resolve("holder.log");
        Process holder = new ProcessBuilder(javaExecutable(), "-cp",
                System.getProperty("ponhub.test.runtimeClasspath"), LockProbe.class.getName(),
                directory.resolve("data.json.ponhub-lock").toString(), ready.toString())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            awaitFile(ready);
            CanonicalDataFileGuard guard = new CanonicalDataFileGuard(path, null);
            assertTrue(assertThrows(IOException.class, () -> guard.save("blocked"))
                    .getMessage().contains("Another PonHub window"));
            assertFalse(Files.exists(path));
            holder.destroyForcibly();
            assertTrue(holder.waitFor(20, TimeUnit.SECONDS));
            guard.save("recovered");
            assertEquals("recovered", Files.readString(path));
        } finally {
            holder.destroyForcibly();
        }
    }

    @Test
    public void save_independentProcesses_onlyOneStaleSnapshotWins() throws Exception {
        Path path = directory.resolve("shared.json");
        for (boolean existing : List.of(false, true)) {
            if (existing) {
                Files.writeString(path, "original");
            }
            Path start = directory.resolve("start-" + existing);
            Path firstReady = directory.resolve("first-" + existing);
            Path secondReady = directory.resolve("second-" + existing);
            Path firstLog = directory.resolve("first-" + existing + ".log");
            Path secondLog = directory.resolve("second-" + existing + ".log");
            Process first = launch(path, firstReady, start, firstLog, "first");
            Process second = launch(path, secondReady, start, secondLog, "second");
            try {
                awaitFile(firstReady);
                awaitFile(secondReady);
                Files.createFile(start);
                assertTrue(first.waitFor(30, TimeUnit.SECONDS));
                assertTrue(second.waitFor(30, TimeUnit.SECONDS));
                assertEquals(0, first.exitValue(), Files.readString(firstLog));
                assertEquals(0, second.exitValue(), Files.readString(secondLog));
                String results = Files.readString(firstLog) + Files.readString(secondLog);
                assertEquals(1, results.lines().filter("SAVED"::equals).count(), results);
                assertEquals(1, results.lines().filter("REJECTED"::equals).count(), results);
                assertTrue(List.of("first", "second").contains(Files.readString(path)));
                // A completed JVM leaves a harmless sidecar, not a stale operating-system lock.
                new CanonicalDataFileGuard(path, Files.readString(path)).save("restart");
                assertEquals("restart", Files.readString(path));
            } finally {
                first.destroyForcibly();
                second.destroyForcibly();
            }
        }
    }

    private Process launch(Path path, Path ready, Path start, Path log, String value) throws IOException {
        return new ProcessBuilder(javaExecutable(),
                "-cp", System.getProperty("ponhub.test.runtimeClasspath"), SaveProbe.class.getName(),
                path.toString(), ready.toString(), start.toString(), value)
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
    }

    private static String javaExecutable() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    private static void createLink(Path link, Path target) throws IOException {
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | IOException failure) {
            assumeTrue(false, "Symbolic links unavailable: " + failure);
        }
    }

    private static void awaitFile(Path path) throws InterruptedException, IOException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (!Files.exists(path)) {
            if (System.nanoTime() >= deadline) {
                throw new IOException("Timed out waiting for child process: " + path);
            }
            Thread.sleep(20);
        }
    }

    /**
     * Independently captures a snapshot before two JVMs race to save different replacements.
     */
    public static class SaveProbe {
        public static void main(String[] args) throws Exception {
            Path path = Path.of(args[0]);
            CanonicalDataFileGuard guard = new CanonicalDataFileGuard(path,
                    Files.exists(path) ? Files.readString(path) : null);
            Files.createFile(Path.of(args[1]));
            awaitFile(Path.of(args[2]));
            try {
                guard.save(args[3]);
                System.out.println("SAVED");
            } catch (IOException conflict) {
                System.out.println("REJECTED");
            }
        }
    }

    /**
     * Holds a real cross-process lock until forcibly terminated by the parent test.
     */
    public static class LockProbe {
        public static void main(String[] args) throws Exception {
            try (FileChannel channel = FileChannel.open(Path.of(args[0]),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE); var lock = channel.lock()) {
                if (!lock.isValid()) {
                    throw new IOException("Could not hold the save lock");
                }
                Files.createFile(Path.of(args[1]));
                System.in.read();
            }
        }
    }
}
