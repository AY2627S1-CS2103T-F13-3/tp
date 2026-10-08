package seedu.address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JavaFxNativeLibraryLoaderTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    public void getPlatformDirectory_supportedPlatformsAndAliases_returnsPackagedDirectory() {
        assertEquals("windows-x86_64",
                JavaFxNativeLibraryLoader.getPlatformDirectory("Windows 11", "amd64"));
        assertEquals("linux-x86_64",
                JavaFxNativeLibraryLoader.getPlatformDirectory("Linux", "x86_64"));
        assertEquals("macos-x86_64",
                JavaFxNativeLibraryLoader.getPlatformDirectory("Mac OS X", "x86_64"));
        assertEquals("macos-aarch64",
                JavaFxNativeLibraryLoader.getPlatformDirectory("Mac OS X", "aarch64"));
        assertEquals("macos-aarch64",
                JavaFxNativeLibraryLoader.getPlatformDirectory("Darwin", "arm64"));
    }

    @Test
    public void getPlatformDirectory_unsupportedCombinations_throwsIllegalStateException() {
        assertThrows(IllegalStateException.class, () ->
                JavaFxNativeLibraryLoader.getPlatformDirectory("Windows 11", "aarch64"));
        assertThrows(IllegalStateException.class, () ->
                JavaFxNativeLibraryLoader.getPlatformDirectory("Linux", "aarch64"));
        assertThrows(IllegalStateException.class, () ->
                JavaFxNativeLibraryLoader.getPlatformDirectory("Solaris", "x86_64"));
        assertThrows(NullPointerException.class, () ->
                JavaFxNativeLibraryLoader.getPlatformDirectory(null, "x86_64"));
        assertThrows(NullPointerException.class, () ->
                JavaFxNativeLibraryLoader.getPlatformDirectory("Linux", null));
    }

    @Test
    public void extractPlatformLibraries_extractsOnlySelectedDirectoryWithoutPathTraversal() throws IOException {
        Path jarPath = temporaryDirectory.resolve("fixture.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jarPath))) {
            addEntry(output, "META-INF/javafx-natives/macos-aarch64/libglass.dylib", "glass-arm64");
            addEntry(output, "META-INF/javafx-natives/macos-aarch64/nested/libprism.dylib", "prism-arm64");
            addEntry(output, "META-INF/javafx-natives/macos-x86_64/libglass.dylib", "glass-x64");
            addEntry(output, "seedu/address/Main.class", "not-a-library");
        }

        Path destination = temporaryDirectory.resolve("natives");
        Files.createDirectories(destination);
        int extractedCount;
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            extractedCount = JavaFxNativeLibraryLoader.extractPlatformLibraries(
                    jarFile, "macos-aarch64", destination);
        }

        assertEquals(2, extractedCount);
        assertEquals("glass-arm64", Files.readString(destination.resolve("libglass.dylib")));
        assertEquals("prism-arm64", Files.readString(destination.resolve("libprism.dylib")));
        assertFalse(Files.exists(destination.resolve("Main.class")));
        try (var extractedFiles = Files.list(destination)) {
            assertTrue(extractedFiles.allMatch(path -> path.getParent().equals(destination)));
        }
    }

    private static void addEntry(JarOutputStream output, String name, String contents) throws IOException {
        output.putNextEntry(new JarEntry(name));
        output.write(contents.getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
    }
}
