package seedu.address;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

@ResourceLock("java.library.path")
public class JavaFxNativeLibraryLoaderTest {

    @TempDir
    private Path temporaryDirectory;
    private String originalJavaLibraryPath;

    @BeforeEach
    public void saveJavaLibraryPath() {
        originalJavaLibraryPath = System.getProperty("java.library.path");
    }

    @AfterEach
    public void restoreJavaLibraryPath() {
        if (originalJavaLibraryPath == null) {
            System.clearProperty("java.library.path");
        } else {
            System.setProperty("java.library.path", originalJavaLibraryPath);
        }
    }

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
        assertThrows(IllegalStateException.class, () ->
                JavaFxNativeLibraryLoader.getPlatformDirectory("Mac OS X", "riscv64"));
        assertThrows(NullPointerException.class, () ->
                JavaFxNativeLibraryLoader.getPlatformDirectory(null, "x86_64"));
        assertThrows(NullPointerException.class, () ->
                JavaFxNativeLibraryLoader.getPlatformDirectory("Linux", null));
    }

    @Test
    public void extractPlatformLibraries_extractsOnlySelectedDirectoryWithoutPathTraversal() throws IOException {
        Path jarPath = temporaryDirectory.resolve("fixture.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jarPath))) {
            addDirectory(output, "META-INF/javafx-natives/macos-aarch64/");
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

    @Test
    public void prepare_nonJarCodeSources_skipsExtraction() throws IOException {
        System.setProperty("java.library.path", "unchanged");

        assertDoesNotThrow(() -> JavaFxNativeLibraryLoader.prepare());
        Path regularFile = temporaryDirectory.resolve("ponhub.txt");
        Files.writeString(regularFile, "not a JAR");
        JavaFxNativeLibraryLoader.prepare(regularFile, "Mac OS X", "aarch64");

        assertEquals("unchanged", System.getProperty("java.library.path"));
        try (var files = Files.list(temporaryDirectory)) {
            assertFalse(files.anyMatch(JavaFxNativeLibraryLoaderTest::isNativeDirectory));
        }
    }

    @Test
    public void prepare_uppercaseJar_extractsInsideJarHomeAndPrependsLibraryPath() throws IOException {
        Path jarPath = createNativeJar("PONHUB.JAR", "macos-aarch64", "libglass.dylib", "glass-arm64");
        System.setProperty("java.library.path", "existing-library-path");

        JavaFxNativeLibraryLoader.prepare(jarPath, "Mac OS X", "aarch64");

        Path nativeDirectory = getOnlyNativeDirectory();
        assertEquals(temporaryDirectory, nativeDirectory.getParent());
        assertEquals("glass-arm64", Files.readString(nativeDirectory.resolve("libglass.dylib")));
        String expectedLibraryPath = nativeDirectory.toAbsolutePath()
                + System.getProperty("path.separator") + "existing-library-path";
        assertEquals(expectedLibraryPath, System.getProperty("java.library.path"));
    }

    @Test
    public void prepare_emptyLibraryPath_usesOnlyExtractedDirectory() throws IOException {
        Path jarPath = createNativeJar("ponhub.jar", "macos-aarch64", "libglass.dylib", "glass-arm64");
        System.setProperty("java.library.path", "");

        JavaFxNativeLibraryLoader.prepare(jarPath, "Darwin", "arm64");

        assertEquals(getOnlyNativeDirectory().toAbsolutePath().toString(),
                System.getProperty("java.library.path"));
    }

    @Test
    public void prepare_missingPlatformLibraries_throwsIllegalStateException() throws IOException {
        Path jarPath = createNativeJar("ponhub.jar", "macos-x86_64", "libglass.dylib", "glass-x64");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                JavaFxNativeLibraryLoader.prepare(jarPath, "Mac OS X", "aarch64"));

        assertTrue(exception.getMessage().contains("macos-aarch64"));
        assertEquals(temporaryDirectory, getOnlyNativeDirectory().getParent());
    }

    @Test
    public void prepare_invalidJar_wrapsIoException() throws IOException {
        Path jarPath = temporaryDirectory.resolve("broken.jar");
        Files.writeString(jarPath, "not a JAR");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                JavaFxNativeLibraryLoader.prepare(jarPath, "Mac OS X", "aarch64"));

        assertTrue(exception.getCause() instanceof IOException);
        assertEquals(temporaryDirectory, getOnlyNativeDirectory().getParent());
    }

    private Path createNativeJar(String jarName, String platformDirectory, String libraryName, String contents)
            throws IOException {
        Path jarPath = temporaryDirectory.resolve(jarName);
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jarPath))) {
            addEntry(output, JavaFxNativeLibraryLoader.NATIVE_ROOT + platformDirectory + "/" + libraryName,
                    contents);
        }
        return jarPath;
    }

    private Path getOnlyNativeDirectory() throws IOException {
        List<Path> nativeDirectories;
        try (var files = Files.list(temporaryDirectory)) {
            nativeDirectories = files.filter(JavaFxNativeLibraryLoaderTest::isNativeDirectory).toList();
        }
        assertEquals(1, nativeDirectories.size());
        return nativeDirectories.get(0);
    }

    private static boolean isNativeDirectory(Path path) {
        return Files.isDirectory(path)
                && path.getFileName().toString().startsWith(JavaFxNativeLibraryLoader.NATIVE_DIRECTORY_PREFIX);
    }

    private static void addDirectory(JarOutputStream output, String name) throws IOException {
        output.putNextEntry(new JarEntry(name));
        output.closeEntry();
    }

    private static void addEntry(JarOutputStream output, String name, String contents) throws IOException {
        output.putNextEntry(new JarEntry(name));
        output.write(contents.getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
    }
}
