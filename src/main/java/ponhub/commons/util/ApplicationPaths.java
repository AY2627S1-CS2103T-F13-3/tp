package ponhub.commons.util;

import static java.util.Objects.requireNonNull;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Locates the application folder independently of startup and logging initialization.
 * Packaged data, preferences and logs belong beside the JAR or in its descendants.
 */
public final class ApplicationPaths {
    private ApplicationPaths() {
    }

    /**
     * Returns the containing JAR directory, or the working directory when running development classes.
     * The returned path is absolute and normalized.
     *
     * @throws IllegalStateException if the runtime code-source location cannot be converted to a file path.
     */
    public static Path getHomeDirectory() {
        try {
            Path sourceLocation = Path.of(ApplicationPaths.class.getProtectionDomain().getCodeSource()
                    .getLocation().toURI());
            return getHomeDirectory(sourceLocation, Path.of(""));
        } catch (URISyntaxException failure) {
            throw new IllegalStateException("Cannot locate the application folder.", failure);
        }
    }

    /**
     * Resolves a supplied code source and working directory without changing process-global properties.
     * Archive files remain anchored to their containing folder regardless of the process working directory.
     */
    static Path getHomeDirectory(Path sourceLocation, Path workingDirectory) {
        Path source = requireNonNull(sourceLocation).toAbsolutePath().normalize();
        Path currentDirectory = requireNonNull(workingDirectory).toAbsolutePath().normalize();
        return Files.isDirectory(source) ? currentDirectory : source.getParent();
    }
}
