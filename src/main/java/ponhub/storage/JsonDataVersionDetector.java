package ponhub.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Classifies JSON before domain decoding. This does not validate records or authorize saving.
 */
public final class JsonDataVersionDetector {
    public static final String VERSION_FIELD = "schemaVersion";
    public static final int CURRENT_VERSION = 1;
    // Container depth, including the root; the supported schema needs at most four levels.
    static final int MAX_NESTING_DEPTH = 64;
    // Every supported counter fits in 19 digits. Bound other numeric tokens before tree construction.
    static final int MAX_NUMERIC_TOKEN_LENGTH = 64;

    private static final JsonFactory factory = new JsonFactory()
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
     * Documents exceeding the nesting or numeric-token limit are malformed, including unrecognized fields.
     * Root versions are classified lexically, so even an oversized future version needs no BigInteger.
     *
     * @param json non-null file contents.
     * @return explicit format or rejection reason
     */
    public static Format detect(String json) {
        requireNonNull(json);
        try (JsonParser parser = factory.createParser(json)) {
            return scanDocument(parser);
        } catch (IOException e) {
            return Format.MALFORMED_JSON;
        }
    }

    /**
     * Scans and classifies without constructing a tree or converting numeric values.
     * Uses the same parser settings so duplicate fields and invalid syntax still fail closed.
     * Requires exactly one root value, rejecting empty input and trailing documents before tree parsing.
     */
    private static Format scanDocument(JsonParser parser) throws IOException {
        int depth = 0;
        int roots = 0;
        boolean rootObject = false;
        String field = null;
        Map<String, JsonToken> fields = new HashMap<>();
        Format version = null;
        JsonToken token;
        while ((token = parser.nextToken()) != null) {
            if (depth == 0) {
                if (++roots > 1) {
                    return Format.MALFORMED_JSON;
                }
                rootObject = token == JsonToken.START_OBJECT;
            }
            boolean rootValue = rootObject && depth == 1 && token != JsonToken.FIELD_NAME
                    && token != JsonToken.END_OBJECT;
            if (rootObject && depth == 1 && token == JsonToken.FIELD_NAME) {
                field = parser.getCurrentName();
            } else if (rootValue) {
                fields.put(field, token);
                if (VERSION_FIELD.equals(field)) {
                    version = classifyVersion(parser, token);
                }
            }
            if (token.isNumeric() && !(rootValue && VERSION_FIELD.equals(field))
                    && parser.getTextLength() > MAX_NUMERIC_TOKEN_LENGTH) {
                return Format.MALFORMED_JSON;
            }
            if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
                depth++;
                if (depth > MAX_NESTING_DEPTH) {
                    return Format.MALFORMED_JSON;
                }
            } else if (token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) {
                depth--;
            }
        }
        if (roots != 1) {
            return Format.MALFORMED_JSON;
        }
        if (!rootObject) {
            return Format.UNRECOGNIZED_FORMAT;
        }
        if (version != null) {
            return version;
        }
        boolean legacyFields = fields.size() == 1
                || (fields.size() == 2 && fields.get("_comment") == JsonToken.VALUE_STRING);
        return legacyFields && fields.get("persons") == JsonToken.START_ARRAY
                ? Format.LEGACY_AB3 : Format.UNRECOGNIZED_FORMAT;
    }

    /**
     * Classifies the schema-version token without numeric conversion.
     * JSON syntax already excludes leading zeroes and plus signs; only literal 1 is supported.
     */
    private static Format classifyVersion(JsonParser parser, JsonToken token) throws IOException {
        if (token != JsonToken.VALUE_NUMBER_INT) {
            return Format.INVALID_VERSION;
        }
        char first = parser.getTextCharacters()[parser.getTextOffset()];
        if (first == '-' || first == '0') {
            return Format.INVALID_VERSION;
        }
        return parser.getTextLength() == 1 && first == '1'
                ? Format.PONHUB_V1_CANDIDATE : Format.UNSUPPORTED_VERSION;
    }
}
