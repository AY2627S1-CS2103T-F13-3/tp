package seedu.address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises startup link-resolution bounds without initializing preferences or operational storage. */
public class MainAppPathResolutionTest {
    @TempDir
    Path directory;

    @Test
    public void selectDataPath_cyclicLinks_rejectsDirectAndMissingDescendantPaths() throws IOException {
        Path first = directory.resolve("first");
        Path second = directory.resolve("second");
        createLink(first, second.getFileName());
        createLink(second, first.getFileName());
        Path original = directory.resolve("keep.json");
        Files.writeString(original, "preserve original data");

        for (Path selected : List.of(first, first.resolve("missing/data.json"))) {
            IOException error = assertTimeoutPreemptively(Duration.ofSeconds(2), () -> assertThrows(
                    IOException.class, () -> MainApp.selectDataPath(Map.of("data", selected.toString()), directory)));
            assertTrue(error.getMessage().contains("Too many symbolic links"), error.getMessage());
            assertEquals(second.getFileName(), Files.readSymbolicLink(first));
            assertEquals(first.getFileName(), Files.readSymbolicLink(second));
            assertEquals("preserve original data", Files.readString(original));
            try (var files = Files.list(directory)) {
                assertEquals(3, files.count());
            }
        }
    }

    @Test
    public void selectDataPath_linkChainLimit_acceptsFortyAndRejectsFortyOneWithoutWriting() throws IOException {
        Path destination = directory.resolve("missing.json");
        for (int i = 40; i >= 0; i--) {
            Path next = i == 40 ? destination : directory.resolve("link-" + (i + 1));
            createLink(directory.resolve("link-" + i), next.getFileName());
        }

        Path accepted = directory.toRealPath().resolve("link-1");
        assertEquals(accepted, MainApp.selectDataPath(Map.of("data", "link-1"), directory));
        IOException error = assertTimeoutPreemptively(Duration.ofSeconds(2), () -> assertThrows(
                IOException.class, () -> MainApp.selectDataPath(Map.of("data", "link-0"), directory)));

        assertTrue(error.getMessage().contains("Too many symbolic links"), error.getMessage());
        assertTrue(Files.notExists(destination, LinkOption.NOFOLLOW_LINKS));
        for (int i = 0; i <= 40; i++) {
            Path next = i == 40 ? destination : directory.resolve("link-" + (i + 1));
            assertEquals(next.getFileName(), Files.readSymbolicLink(directory.resolve("link-" + i)));
        }
        try (var files = Files.list(directory)) {
            assertEquals(41, files.count());
        }
    }

    private void createLink(Path link, Path target) throws IOException {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(false, "Symbolic links unavailable: " + exception);
        }
    }
}
