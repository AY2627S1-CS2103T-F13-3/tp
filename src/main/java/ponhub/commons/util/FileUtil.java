package ponhub.commons.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes and reads files
 */
public class FileUtil {

    private static final String CHARSET = "UTF-8";

    /**
     * Assumes file exists
     */
    public static String readFromFile(Path file) throws IOException {
        return new String(Files.readAllBytes(file), CHARSET);
    }

    /**
     * Safely saves a UTF-8 string, creating parent directories and following existing symbolic links.
     * Prefers atomic replacement and uses a recovery backup when atomic moves are unsupported.
     */
    public static void writeToFile(Path file, String content) throws IOException {
        new AtomicFileWriter().write(file, content);
    }

}
