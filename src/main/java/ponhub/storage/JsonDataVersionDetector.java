package ponhub.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Classifies JSON before domain decoding. This does not validate records or authorize saving.
 */
public final class JsonDataVersionDetector {
    public static final String VERSION_FIELD = "schemaVersion";
    public static final int CURRENT_VERSION = 1;
    // Container depth, including the root; the supported schema needs at most four levels.
    static final int MAX_NESTING_DEPTH = 64;

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

    private JsonDataVersionDetector() {
    }

    /**
     * Describes the format boundary checked by {@link ProtectedPonHubDataStorage} before domain decoding.
     */
    public enum Format {
        LEGACY_AB3,
        PONHUB_V1_CANDIDATE,
        MALFORMED_JSON,
        UNRECOGNIZED_FORMAT,
        INVALID_VERSION,
        UNSUPPORTED_VERSION
    }

    /**
     * Classifies a complete JSON document without modifying it or constructing model objects.
     * A versioned candidate still needs complete envelope and domain validation by {@link JsonPonHubDataCodec}.
     * A legacy classification identifies only the root shape, not the validity of individual contacts.
     * File absence and read failures must be handled separately by the caller, never as empty JSON.
     * Documents exceeding the container nesting limit are malformed, including unrecognized fields.
     *
     * @param json non-null file contents
     * @return explicit format or rejection reason
     */
    public static Format detect(String json) {
        requireNonNull(json);
        JsonNode root;
        try (JsonParser parser = mapper.getFactory().createParser(json)) {
            if (!hasBoundedNesting(json)) {
                return Format.MALFORMED_JSON;
            }
            root = mapper.readTree(parser);
            if (root == null || parser.nextToken() != null) {
                return Format.MALFORMED_JSON;
            }
        } catch (IOException e) {
            return Format.MALFORMED_JSON;
        }
        if (!root.isObject()) {
            return Format.UNRECOGNIZED_FORMAT;
        }
        // Inspect the version first: a future file may also contain a legacy-looking persons array.
        if (root.has(VERSION_FIELD)) {
            JsonNode version = root.get(VERSION_FIELD);
            if (!version.isIntegralNumber() || version.bigIntegerValue().signum() <= 0) {
                return Format.INVALID_VERSION;
            }
            return version.canConvertToInt() && version.intValue() == CURRENT_VERSION
                    ? Format.PONHUB_V1_CANDIDATE : Format.UNSUPPORTED_VERSION;
        }
        // Do not let unknown root fields be silently discarded by the inherited permissive codec.
        boolean hasOnlyLegacyFields = root.size() == 1
                || (root.size() == 2 && root.has("_comment") && root.get("_comment").isTextual());
        if (hasOnlyLegacyFields && root.has("persons") && root.get("persons").isArray()) {
            return Format.LEGACY_AB3;
        }
        return Format.UNRECOGNIZED_FORMAT;
    }

    /**
     * Scans tokens iteratively before recursive tree construction, counting objects and arrays rather than text.
     * Uses the same parser settings so duplicate fields and invalid syntax still fail closed.
     */
    private static boolean hasBoundedNesting(String json) throws IOException {
        try (JsonParser parser = mapper.getFactory().createParser(json)) {
            int depth = 0;
            JsonToken token;
            while ((token = parser.nextToken()) != null) {
                if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
                    depth++;
                    if (depth > MAX_NESTING_DEPTH) {
                        return false;
                    }
                } else if (token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) {
                    depth--;
                }
            }
            return true;
        }
    }
}
