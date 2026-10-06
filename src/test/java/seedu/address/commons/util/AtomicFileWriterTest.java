package seedu.address.commons.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
                new AccessDeniedException(target.toString()))) {
            AtomicFileWriter writer = new AtomicFileWriter() {
                @Override
                void moveAtomically(Path temporary, Path destination) throws IOException {
                    assertEquals(target.getParent().toRealPath(), temporary.getParent());
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

    @Test
    public void write_symbolicLink_preservesLinkAndUpdatesTarget() throws IOException {
        Path target = existingTarget();
        Path link = directory.resolve("link.json");
        createLink(link, target.getFileName());
        FileUtil.writeToFile(link, "new data");
        assertTrue(Files.isSymbolicLink(link));
        assertEquals("new data", Files.readString(target));
        assertEquals("new data", Files.readString(link));
    }

    @Test
    public void write_danglingLink_rejectsWithoutReplacingLink() throws IOException {
        Path link = directory.resolve("dangling.json");
        Path absent = directory.resolve("absent.json");
        createLink(link, absent);
        IOException error = assertThrows(IOException.class, () -> FileUtil.writeToFile(link, "new data"));
        assertTrue(error.getMessage().contains("Cannot resolve"));
        assertTrue(Files.isSymbolicLink(link));
        assertFalse(Files.exists(absent));
    }

    @Test
    public void write_cyclicLinks_rejectsWithoutReplacingLinks() throws IOException {
        Path first = directory.resolve("first.json");
        Path second = directory.resolve("second.json");
        createLink(first, second);
        createLink(second, first);
        assertThrows(IOException.class, () -> FileUtil.writeToFile(first, "new data"));
        assertTrue(Files.isSymbolicLink(first));
        assertTrue(Files.isSymbolicLink(second));
    }

    @Test
    public void write_linkedParentAndFallback_preservesLinks() throws IOException {
        Path realParent = Files.createDirectory(directory.resolve("real"));
        Path linkedParent = directory.resolve("linked");
        createLink(linkedParent, realParent);
        Path target = realParent.resolve("data.json");
        AtomicFileWriter writer = new FallbackWriter();
        writer.write(linkedParent.resolve("data.json"), "first");
        Path fileLink = directory.resolve("file.json");
        createLink(fileLink, target);
        writer.write(fileLink, "second");
        assertEquals("second", Files.readString(target));
        assertTrue(Files.isSymbolicLink(fileLink));
        assertTrue(Files.isSymbolicLink(linkedParent));
        assertOnlyTargetRemains(target);
    }

    @Test
    public void write_linkWithFailedMove_preservesTargetAndLink() throws IOException {
        Path target = existingTarget();
        Path link = directory.resolve("link.json");
        createLink(link, target);
        AtomicFileWriter writer = new AtomicFileWriter() {
            @Override
            void moveAtomically(Path temporary, Path destination) throws IOException {
                throw new AccessDeniedException(destination.toString());
            }
        };
        assertThrows(AccessDeniedException.class, () -> writer.write(link, "new data"));
        assertTrue(Files.isSymbolicLink(link));
        assertEquals("original", Files.readString(target));
    }

    @Test
    public void write_unsupportedAtomicMove_savesNewAndExistingFiles() throws IOException {
        Path target = directory.resolve("new.json");
        AtomicFileWriter writer = new FallbackWriter();
        writer.write(target, "first");
        writer.write(target, "second");
        assertEquals("second", Files.readString(target));
        assertOnlyTargetRemains(target);
    }

    @Test
    public void write_backupFailure_doesNotAttemptReplacement() throws IOException {
        Path target = existingTarget();
        AtomicFileWriter writer = new FallbackWriter() {
            @Override
            void copyFile(Path source, Path destination) throws IOException {
                Files.writeString(destination, "partial backup");
                throw new IOException("Backup failed");
            }

            @Override
            void moveNormally(Path temporary, Path destination) {
                throw new AssertionError("Must not move without a complete backup");
            }
        };
        assertThrows(IOException.class, () -> writer.write(target, "new data"));
        assertEquals("original", Files.readString(target));
        assertOnlyTargetRemains(target);
    }

    @Test
    public void write_fallbackMoveDamagesTarget_restoresOldBytes() throws IOException {
        Path target = existingTarget();
        AtomicFileWriter writer = new FallbackWriter() {
            @Override
            void moveNormally(Path temporary, Path destination) throws IOException {
                Files.writeString(destination, "partial new data");
                throw new IOException("Move failed");
            }
        };
        assertThrows(IOException.class, () -> writer.write(target, "new data"));
        assertEquals("original", Files.readString(target));
        assertOnlyTargetRemains(target);
    }

    @Test
    public void write_firstFallbackMoveFails_removesPartialDestination() throws IOException {
        Path target = directory.resolve("new.json");
        AtomicFileWriter writer = new FallbackWriter() {
            @Override
            void moveNormally(Path temporary, Path destination) throws IOException {
                Files.writeString(destination, "partial");
                throw new IOException("Move failed");
            }
        };
        assertThrows(IOException.class, () -> writer.write(target, "new data"));
        assertFalse(Files.exists(target));
        try (var files = Files.list(directory)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    public void write_restoreFails_retainsCompleteBackupAndReportsPath() throws IOException {
        Path target = existingTarget();
        AtomicFileWriter writer = new FallbackWriter() {
            @Override
            void moveNormally(Path temporary, Path destination) throws IOException {
                Files.writeString(destination, "partial");
                throw new IOException("Move failed");
            }

            @Override
            void copyFile(Path source, Path destination) throws IOException {
                if (source.toString().endsWith(".bak")) {
                    throw new IOException("Restore failed");
                }
                super.copyFile(source, destination);
            }
        };
        IOException error = assertThrows(IOException.class, () -> writer.write(target, "new data"));
        try (var files = Files.list(directory)) {
            Path backup = files.filter(path -> path.toString().endsWith(".bak")).findFirst().orElseThrow();
            assertEquals("original", Files.readString(backup));
            assertTrue(error.getMessage().contains(backup.toRealPath().toString()));
        }
        assertEquals("Move failed", error.getCause().getMessage());
        assertEquals("Restore failed", error.getSuppressed()[0].getMessage());
    }

    @Test
    public void write_backupCleanupFails_stillReportsSaveSuccess() throws IOException {
        Path target = existingTarget();
        AtomicFileWriter writer = new FallbackWriter() {
            @Override
            void deleteTemporary(Path path) throws IOException {
                throw new IOException("Cleanup denied");
            }
        };
        writer.write(target, "new data");
        assertEquals("new data", Files.readString(target));
    }

    private void createLink(Path link, Path target) throws IOException {
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | IOException exception) {
            assumeTrue(false, "Symbolic links unavailable: " + exception);
        }
    }

    private static class FallbackWriter extends AtomicFileWriter {
        @Override
        void moveAtomically(Path temporary, Path target) throws IOException {
            throw new AtomicMoveNotSupportedException(temporary.toString(), target.toString(), "Test provider");
        }
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
