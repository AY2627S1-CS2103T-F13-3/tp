package seedu.address.commons.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AtomicFileWriterTest {
    @TempDir
    public Path directory;

    @Test
    public void write_newNestedPathAndReplacement_preservesUtf8() throws IOException {
        Path target = directory.resolve("nested/data/book.json");
        FileUtil.writeToFile(target, "original");
        FileUtil.writeToFile(target, "你好 café\nnew content");
        assertEquals("你好 café\nnew content", Files.readString(target));
        assertOnlyTargetRemains(target);
    }

    @Test
    public void write_partialTemporaryWriteFailure_preservesDestination() throws IOException {
        Path target = existingTarget();
        byte[] original = Files.readAllBytes(target);
        AtomicFileWriter writer = new AtomicFileWriter() {
            @Override
            void writeTemporary(Path temporary, String content) throws IOException {
                Files.writeString(temporary, "partial");
                throw new IOException("Disk full");
            }
        };
        assertThrows(IOException.class, () -> writer.write(target, "replacement"));
        assertArrayEquals(original, Files.readAllBytes(target));
        assertOnlyTargetRemains(target);
    }

    @Test
    public void write_firstSaveFailure_leavesDestinationAbsent() throws IOException {
        Path target = directory.resolve("new.json");
        AtomicFileWriter writer = new AtomicFileWriter() {
            @Override
            void writeTemporary(Path temporary, String content) throws IOException {
                throw new AccessDeniedException(temporary.toString());
            }
        };
        assertThrows(AccessDeniedException.class, () -> writer.write(target, "replacement"));
        assertFalse(Files.exists(target));
        try (var files = Files.list(directory)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    public void write_replacementFailures_preserveDestinationAndException() throws IOException {
        Path target = existingTarget();
        byte[] original = Files.readAllBytes(target);
        for (IOException failure : List.of(new IOException("Move failed"),
                new AccessDeniedException(target.toString()),
                new AtomicMoveNotSupportedException("temporary", target.toString(), "Unsupported"))) {
            AtomicFileWriter writer = new AtomicFileWriter() {
                @Override
                void replace(Path temporary, Path destination) throws IOException {
                    assertEquals(target.getParent(), temporary.getParent());
                    assertEquals("replacement", Files.readString(temporary));
                    assertArrayEquals(original, Files.readAllBytes(destination));
                    throw failure;
                }
            };
            assertSame(failure, assertThrows(IOException.class, () -> writer.write(target, "replacement")));
            assertArrayEquals(original, Files.readAllBytes(target));
            assertOnlyTargetRemains(target);
        }
    }

    @Test
    public void write_cleanupFailure_preservesPrimaryException() throws IOException {
        Path target = existingTarget();
        IOException primary = new IOException("Write failed");
        IOException cleanup = new IOException("Cleanup failed");
        AtomicFileWriter writer = new AtomicFileWriter() {
            @Override
            void writeTemporary(Path temporary, String content) throws IOException {
                throw primary;
            }

            @Override
            void deleteTemporary(Path temporary) throws IOException {
                throw cleanup;
            }
        };
        assertSame(primary, assertThrows(IOException.class, () -> writer.write(target, "replacement")));
        assertArrayEquals(new Throwable[]{cleanup}, primary.getSuppressed());
        assertEquals("original", Files.readString(target));
    }

    @Test
    public void write_parentIsFile_preservesExistingFile() throws IOException {
        Path parent = existingTarget();
        assertThrows(IOException.class, () -> new AtomicFileWriter().write(parent.resolve("child"), "new"));
        assertEquals("original", Files.readString(parent));
    }

    private Path existingTarget() throws IOException {
        Path target = directory.resolve("book.json");
        Files.writeString(target, "original");
        return target;
    }

    private void assertOnlyTargetRemains(Path target) throws IOException {
        try (var files = Files.list(target.getParent())) {
            assertEquals(List.of(target), files.toList());
        }
    }
}
