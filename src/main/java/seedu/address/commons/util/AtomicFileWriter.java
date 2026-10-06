package seedu.address.commons.util;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Writes a complete temporary sibling file before atomically replacing the destination.
 * Unsupported atomic replacement fails without falling back to a non-atomic write.
 */
class AtomicFileWriter {

    /**
     * Saves UTF-8 content. Parent directories may be created even if the save fails.
     */
    void write(Path file, String content) throws IOException {
        requireNonNull(file);
        requireNonNull(content);
        Path target = file.toAbsolutePath();
        Path parent = target.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".ponhub-", ".tmp");

        try {
            writeTemporary(temporary, content);
            replace(temporary, target);
        } catch (IOException | RuntimeException failure) {
            try {
                deleteTemporary(temporary);
            } catch (IOException | RuntimeException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
        // A successful move consumes the temporary file; no further cleanup is needed.
    }

    /** Writes and closes the temporary file before replacement. */
    void writeTemporary(Path temporary, String content) throws IOException {
        Files.writeString(temporary, content, StandardCharsets.UTF_8);
    }

    /**
     * Requests atomic replacement. Existing-target support depends on the filesystem provider.
     */
    void replace(Path temporary, Path target) throws IOException {
        Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Removes a temporary file after a failed save. */
    void deleteTemporary(Path temporary) throws IOException {
        Files.deleteIfExists(temporary);
    }
}
