package ponhub.storage;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.logging.Logger;

import ponhub.commons.util.FileUtil;

/**
 * Serializes cooperating runtime saves and rejects snapshots based on obsolete file contents.
 * The persistent empty sidecar must not be deleted while any instance is running.
 */
final class CanonicalDataFileGuard {
    private static final Logger logger = Logger.getLogger(CanonicalDataFileGuard.class.getName());
    private static final String LOCK_SUFFIX = ".ponhub-lock";

    private final Path path;
    private final Path target;
    private String expectedJson;
    private String conflict;

    CanonicalDataFileGuard(Path path, String expectedJson) throws IOException {
        this.path = path;
        target = resolveTarget(path.toAbsolutePath().normalize());
        if (target.getFileName().toString().endsWith(LOCK_SUFFIX)) {
            throw new IOException("The .ponhub-lock suffix is reserved for save coordination files.");
        }
        this.expectedJson = expectedJson;
    }

    /**
     * Checks and replaces under one short operating-system lock, retaining the existing recovery writer.
     * Only a successful replacement advances this session's expected contents.
     */
    void save(String json) throws IOException {
        if (conflict != null) {
            throw new IOException(conflict);
        }
        Files.createDirectories(target.getParent());
        Path lockPath = target.resolveSibling(target.getFileName() + LOCK_SUFFIX);
        boolean hasSaved = false;
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
            if (channel.size() != 0) {
                throw new IOException("Save coordination file is not empty: " + lockPath
                        + ". Preserve it and close other instances before investigating.");
            }
            try (FileLock lock = channel.tryLock()) {
                if (lock == null) {
                    throw busy();
                }
                if (!target.equals(resolveTarget(path.toAbsolutePath().normalize()))
                        || !Objects.equals(expectedJson, readCurrentJson())) {
                    conflict = "The data file changed since this window loaded or last saved: " + path.toAbsolutePath()
                            + ". This command was not saved or applied. Close other PonHub windows, back up the current"
                            + " file, then restart and inspect the saved records before re-entering this command."
                            + " Do not copy this window's stale data over the file. Further saves from this window"
                            + " are blocked until restart.";
                    throw new IOException(conflict);
                }
                FileUtil.writeToFile(target, json);
                expectedJson = json;
                hasSaved = true;
            }
        } catch (OverlappingFileLockException failure) {
            throw busy();
        } catch (IOException failure) {
            if (!hasSaved) {
                throw failure;
            }
            // Closing a lock/channel after committed replacement must not discard the live transaction.
            logger.warning("Data was saved, but save coordination cleanup failed: " + failure);
        }
    }

    private IOException busy() {
        return new IOException("Another PonHub window is saving " + path.toAbsolutePath()
                + ". This command was not saved or applied. Wait for it to finish and retry;"
                + " if the file changed, restart this window before re-entering the command.");
    }

    private String readCurrentJson() throws IOException {
        try {
            return Files.readString(target);
        } catch (NoSuchFileException missing) {
            if (Files.notExists(target, LinkOption.NOFOLLOW_LINKS)) {
                return null;
            }
            throw missing;
        }
    }

    private static Path resolveTarget(Path path) throws IOException {
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return path.toRealPath();
        }
        Path parent = path.getParent();
        return parent == null ? path : resolveTarget(parent).resolve(path.getFileName());
    }
}
