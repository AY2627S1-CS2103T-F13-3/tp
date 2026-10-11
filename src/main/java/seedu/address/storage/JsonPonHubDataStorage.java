package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.util.FileUtil;
import seedu.address.model.PonHubDataState;

/**
 * Standalone canonical codec/file access. Runtime callers use {@link ProtectedPonHubDataStorage}
 * for startup validation and session write protection; coordinated routing activation remains separate.
 */
public final class JsonPonHubDataStorage {
    private final Path filePath;

    public JsonPonHubDataStorage(Path filePath) {
        this.filePath = requireNonNull(filePath);
    }

    public Path getDataFilePath() {
        return filePath;
    }

    /**
     * Returns a fully validated candidate; only confirmed absence returns empty. Never writes during loading.
     */
    public Optional<PonHubDataState> readData() throws DataLoadingException {
        try {
            return Optional.of(JsonPonHubDataCodec.decode(Files.readString(filePath)));
        } catch (NoSuchFileException missing) {
            if (Files.notExists(filePath, LinkOption.NOFOLLOW_LINKS)) {
                return Optional.empty();
            }
            throw new DataLoadingException(missing);
        } catch (IOException failure) {
            throw new DataLoadingException(failure);
        }
    }

    /**
     * Encodes before writing; shares the existing safe UTF-8 file writer.
     */
    public void saveData(PonHubDataState state) throws IOException {
        String json = JsonPonHubDataCodec.encode(state);
        FileUtil.writeToFile(filePath, json);
    }
}
