package seedu.address.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ApplicationPathsTest {
    @TempDir
    Path directory;

    @Test
    public void getHomeDirectory_packagedSource_usesJarFolderInsteadOfWorkingDirectory() throws Exception {
        Path release = Files.createDirectory(directory.resolve("release files"));
        Path jar = Files.createFile(release.resolve("ponhub.jar"));
        Path elsewhere = Files.createDirectory(directory.resolve("other working folder"));

        assertEquals(release.toAbsolutePath(), ApplicationPaths.getHomeDirectory(jar, elsewhere));
    }

    @Test
    public void getHomeDirectory_developmentClasses_usesWorkingDirectoryInsteadOfCompiledClasses() throws Exception {
        Path classes = Files.createDirectories(directory.resolve("build/classes/java/main"));
        Path working = Files.createDirectory(directory.resolve("project"));

        assertEquals(working.toAbsolutePath(), ApplicationPaths.getHomeDirectory(classes, working));
    }
}
