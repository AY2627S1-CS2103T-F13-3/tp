package seedu.address;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Selects and extracts the JavaFX native libraries packaged for the current platform.
 */
public final class JavaFxNativeLibraryLoader {

    static final String NATIVE_ROOT = "META-INF/javafx-natives/";

    private JavaFxNativeLibraryLoader() {
    }

    /**
     * Makes the current platform's packaged JavaFX native libraries available before JavaFX starts.
     * Running from exploded IDE/Gradle classes needs no extraction because its normal runtime classpath is retained.
     */
    public static void prepare() {
        Path codeSource = getCodeSource();
        if (!Files.isRegularFile(codeSource) || !codeSource.getFileName().toString().endsWith(".jar")) {
            return;
        }

        String platformDirectory = getPlatformDirectory(
                System.getProperty("os.name"), System.getProperty("os.arch"));
        try {
            Path nativeDirectory = Files.createTempDirectory("ponhub-javafx-");
            nativeDirectory.toFile().deleteOnExit();
            try (JarFile jarFile = new JarFile(codeSource.toFile())) {
                int extractedLibraryCount = extractPlatformLibraries(jarFile, platformDirectory, nativeDirectory);
                if (extractedLibraryCount == 0) {
                    throw new IllegalStateException("No JavaFX native libraries were packaged for "
                            + platformDirectory);
                }
            }
            prependJavaLibraryPath(nativeDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to prepare the packaged JavaFX native libraries", exception);
        }
    }

    static String getPlatformDirectory(String osName, String architecture) {
        requireNonNull(osName);
        requireNonNull(architecture);
        String normalizedOs = osName.toLowerCase(Locale.ROOT);
        String normalizedArchitecture = architecture.toLowerCase(Locale.ROOT);
        boolean isX64 = normalizedArchitecture.equals("x86_64") || normalizedArchitecture.equals("amd64");
        boolean isArm64 = normalizedArchitecture.equals("aarch64") || normalizedArchitecture.equals("arm64");

        if (normalizedOs.startsWith("windows") && isX64) {
            return "windows-x86_64";
        }
        if (normalizedOs.startsWith("linux") && isX64) {
            return "linux-x86_64";
        }
        if (normalizedOs.startsWith("mac") || normalizedOs.startsWith("darwin")) {
            if (isX64) {
                return "macos-x86_64";
            }
            if (isArm64) {
                return "macos-aarch64";
            }
        }
        throw new IllegalStateException("Unsupported JavaFX platform: " + osName + " " + architecture);
    }

    static int extractPlatformLibraries(JarFile jarFile, String platformDirectory, Path destination)
            throws IOException {
        requireNonNull(jarFile);
        requireNonNull(platformDirectory);
        requireNonNull(destination);
        String prefix = NATIVE_ROOT + platformDirectory + "/";
        int extractedLibraryCount = 0;
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory() || !entry.getName().startsWith(prefix)) {
                continue;
            }
            Path fileName = Path.of(entry.getName()).getFileName();
            if (fileName == null) {
                continue;
            }
            Path extractedLibrary = destination.resolve(fileName.toString());
            try (var libraryStream = jarFile.getInputStream(entry)) {
                Files.copy(libraryStream, extractedLibrary, StandardCopyOption.REPLACE_EXISTING);
            }
            extractedLibrary.toFile().deleteOnExit();
            extractedLibraryCount++;
        }
        return extractedLibraryCount;
    }

    private static Path getCodeSource() {
        try {
            return Path.of(JavaFxNativeLibraryLoader.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Unable to locate the running PonHub artifact", exception);
        }
    }

    private static void prependJavaLibraryPath(Path nativeDirectory) {
        String currentPath = System.getProperty("java.library.path", "");
        String separator = System.getProperty("path.separator");
        String updatedPath = nativeDirectory.toAbsolutePath().toString();
        if (!currentPath.isEmpty()) {
            updatedPath += separator + currentPath;
        }
        System.setProperty("java.library.path", updatedPath);
    }
}
