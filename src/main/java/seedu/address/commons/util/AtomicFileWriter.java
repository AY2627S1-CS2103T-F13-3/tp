package seedu.address.commons.util;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Logger;

/**
 * Stages complete UTF-8 files and prefers atomic replacement, with a recoverable backup fallback.
 */
class AtomicFileWriter {
    private static final Logger logger = Logger.getLogger(AtomicFileWriter.class.getName());

    /**
     * Saves content through existing symbolic links. Parent directories may remain after failure.
     */
    void write(Path file, String content) throws IOException {
        requireNonNull(file);
        requireNonNull(content);
        Path target = resolveTarget(file);
        Path temporary = Files.createTempFile(target.getParent(), ".ponhub-", ".tmp");
        try {
            writeTemporary(temporary, content);
            replace(temporary, target);
        } catch (IOException | RuntimeException failure) {
            cleanupAfterFailure(temporary, failure);
            throw failure;
        }
    }

    private Path resolveTarget(Path file) throws IOException {
        Path absolute = file.toAbsolutePath();
        try {
            Files.readAttributes(absolute, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (NoSuchFileException missing) {
            Files.createDirectories(absolute.getParent());
            return absolute.getParent().toRealPath().resolve(absolute.getFileName());
        }
        // Do not treat a dangling/cyclic link or a denied lookup as a new ordinary file.
        Path target;
        try {
            target = absolute.toRealPath();
        } catch (IOException failure) {
            throw new IOException("Cannot resolve save destination " + absolute
                    + "; check symbolic links and access permissions.", failure);
        }
        if (!Files.isRegularFile(target)) {
            throw new IOException("Save destination is not a regular file: " + target);
        }
        return target;
    }

    /** Writes and closes the temporary file before replacement. */
    void writeTemporary(Path temporary, String content) throws IOException {
        Files.writeString(temporary, content, StandardCharsets.UTF_8);
    }

    /** Falls back only when the provider explicitly reports unsupported atomic moves. */
    void replace(Path temporary, Path target) throws IOException {
        try {
            moveAtomically(temporary, target);
        } catch (AtomicMoveNotSupportedException unsupported) {
            replaceWithBackup(temporary, target);
        }
    }

    private void replaceWithBackup(Path temporary, Path target) throws IOException {
        Path backup = null;
        try {
            Files.readAttributes(target, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            backup = Files.createTempFile(target.getParent(), ".ponhub-backup-", ".bak");
        } catch (NoSuchFileException missing) {
            // First save: there is no previous file to back up.
        }
        if (backup != null) {
            try {
                copyFile(target, backup);
            } catch (IOException | RuntimeException failure) {
                cleanupAfterFailure(backup, failure);
                throw failure;
            }
        }
        try {
            moveNormally(temporary, target);
        } catch (IOException | RuntimeException failure) {
            try {
                if (backup == null) {
                    Files.deleteIfExists(target);
                } else {
                    copyFile(backup, target);
                }
            } catch (IOException | RuntimeException recoveryFailure) {
                IOException reported = new IOException("Save failed and automatic recovery failed for " + target
                        + (backup == null ? ". Check for a partial file before restarting."
                        : ". Previous complete data is retained at " + backup
                                + "; restore it before continuing or restarting."), failure);
                reported.addSuppressed(recoveryFailure);
                throw reported;
            }
            if (backup != null) {
                cleanupAfterFailure(backup, failure);
            }
            throw failure;
        }
        if (backup != null) {
            try {
                deleteTemporary(backup);
            } catch (IOException | RuntimeException cleanupFailure) {
                // Replacement already succeeded: do not report the committed save as failed.
                logger.warning("Save succeeded, but backup remains at " + backup + ": " + cleanupFailure);
            }
        }
    }

    /** Requests an atomic move without an unsafe implicit retry. */
    void moveAtomically(Path temporary, Path target) throws IOException {
        Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Moves a staged file after a complete recovery backup has been created, if needed. */
    void moveNormally(Path temporary, Path target) throws IOException {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Copies old data to a backup, or restores it after a failed ordinary move. */
    void copyFile(Path source, Path target) throws IOException {
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void cleanupAfterFailure(Path file, Throwable failure) {
        try {
            deleteTemporary(file);
        } catch (IOException | RuntimeException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }

    /** Removes a temporary or backup file when it is no longer needed for recovery. */
    void deleteTemporary(Path temporary) throws IOException {
        Files.deleteIfExists(temporary);
    }
}
