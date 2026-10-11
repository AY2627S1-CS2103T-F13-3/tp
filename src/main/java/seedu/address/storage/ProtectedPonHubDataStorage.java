package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.PonHubData;
import seedu.address.model.PonHubDataState;

/**
 * Session-scoped canonical startup and write protection, prepared for coordinated runtime activation.
 * Rejected files remain locked for this instance even if repaired or removed externally.
 * Preferences are outside this operational storage boundary.
 */
public final class ProtectedPonHubDataStorage {
    /**
     * Distinguishes startup outcomes without treating rejected files as new stores.
     */
    public enum LoadStatus {
        NOT_LOADED, MISSING, LOADED, UNREADABLE, CORRUPT, UNSUPPORTED_VERSION, LEGACY
    }

    private final Path path;
    private final JsonPonHubDataStorage storage;
    private final FileReader reader;
    private LoadStatus status = LoadStatus.NOT_LOADED;
    private String loadError;

    public ProtectedPonHubDataStorage(Path path) {
        this(path, Files::readString);
    }

    ProtectedPonHubDataStorage(Path path, FileReader reader) {
        this.path = requireNonNull(path);
        this.reader = requireNonNull(reader);
        storage = new JsonPonHubDataStorage(path);
    }

    public LoadStatus getLoadStatus() {
        return status;
    }

    public Path getDataFilePath() {
        return path;
    }

    /**
     * Returns guidance whenever operational writes are forbidden, including before startup validation.
     */
    public Optional<String> getDataLoadError() {
        if (status == LoadStatus.NOT_LOADED) {
            return Optional.of("Load and validate canonical data before saving.");
        }
        return Optional.ofNullable(loadError);
    }

    /**
     * Loads a fully validated snapshot or creates an unwritten fresh root only for confirmed absence.
     * A session may attempt loading once; create a new instance after deliberate recovery and restart.
     * No fallback state is returned for rejected data, and unrelated programming exceptions propagate.
     */
    public PonHubDataState load() throws DataLoadingException {
        if (status != LoadStatus.NOT_LOADED) {
            throw new IllegalStateException("Canonical startup was already attempted; restart to reload.");
        }
        String json;
        try {
            json = reader.read(path);
        } catch (NoSuchFileException missing) {
            if (Files.notExists(path, LinkOption.NOFOLLOW_LINKS)) {
                PonHubDataState loadedState = new PonHubData().exportState();
                status = LoadStatus.MISSING;
                return loadedState;
            }
            throw reject(LoadStatus.UNREADABLE, missing);
        } catch (IOException failure) {
            throw reject(LoadStatus.UNREADABLE, failure);
        }
        JsonDataVersionDetector.Format format = JsonDataVersionDetector.detect(json);
        switch (format) {
            case LEGACY_AB3:
                throw reject(LoadStatus.LEGACY, new IOException("Unversioned AB3 data requires manual re-entry."));
            case UNSUPPORTED_VERSION:
                throw reject(LoadStatus.UNSUPPORTED_VERSION, new IOException("Unsupported schemaVersion."));
            case PONHUB_V1_CANDIDATE:
                break;
            default:
                throw reject(LoadStatus.CORRUPT, new IOException("Invalid JSON format: " + format));
        }
        PonHubDataState loadedState;
        try {
            loadedState = JsonPonHubDataCodec.decode(json);
        } catch (IOException failure) {
            throw reject(LoadStatus.CORRUPT, failure);
        }
        status = LoadStatus.LOADED;
        return loadedState;
    }

    /**
     * Saves only after successful startup validation; protected sessions cannot bypass the lock.
     * The complete snapshot goes through the canonical codec, including its unsupported-collection checks.
     */
    public void saveData(PonHubDataState state) throws IOException {
        Optional<String> error = getDataLoadError();
        if (error.isPresent()) {
            throw new IOException(error.get());
        }
        storage.saveData(state);
    }

    private DataLoadingException reject(LoadStatus reason, IOException failure) {
        status = reason;
        String recovery = reason == LoadStatus.LEGACY
                ? "Back up the original file. Configure a separate, previously unused canonical data path and"
                    + " restart, then manually re-enter records with explicit roles. Automatic import is unavailable."
                : "Back up the original file. Restore a known-good supported file, correct its JSON/access permissions,"
                    + " or use a compatible build on a separate copy, then restart.";
        loadError = "Could not load canonical data at " + path.toAbsolutePath() + " (" + reason + "). "
                + "The original file has not been changed. Operational writes are blocked for this session;"
                + " only help, list and exit should be available. An empty view is not your saved data. "
                + recovery + " Do not delete the original to bypass protection. Cause: " + failure.getMessage();
        return new DataLoadingException(new IOException(loadError, failure));
    }

    /**
     * File access seam for deterministic access-failure tests.
     */
    @FunctionalInterface
    interface FileReader {
        String read(Path path) throws IOException;
    }
}
