package ponhub.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ponhub.storage.JsonDataVersionDetector.Format.INVALID_VERSION;
import static ponhub.storage.JsonDataVersionDetector.Format.LEGACY_AB3;
import static ponhub.storage.JsonDataVersionDetector.Format.MALFORMED_JSON;
import static ponhub.storage.JsonDataVersionDetector.Format.PONHUB_V1_CANDIDATE;
import static ponhub.storage.JsonDataVersionDetector.Format.UNRECOGNIZED_FORMAT;
import static ponhub.storage.JsonDataVersionDetector.Format.UNSUPPORTED_VERSION;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class JsonDataVersionDetectorTest {
    @Test
    public void detect_nestingBoundary_countsContainersIncludingRoot() {
        int limit = JsonDataVersionDetector.MAX_NESTING_DEPTH;
        String atLimit = "{\"schemaVersion\":1,\"extra\":" + "[".repeat(limit - 1)
                + "0" + "]".repeat(limit - 1) + "}";
        assertEquals(PONHUB_V1_CANDIDATE, JsonDataVersionDetector.detect(atLimit));
        String aboveLimit = "{\"schemaVersion\":1,\"extra\":" + "[".repeat(limit)
                + "0" + "]".repeat(limit) + "}";
        assertEquals(MALFORMED_JSON, JsonDataVersionDetector.detect(aboveLimit));
        assertEquals(UNRECOGNIZED_FORMAT, JsonDataVersionDetector.detect("[".repeat(limit)
                + "0" + "]".repeat(limit)));
        assertEquals(MALFORMED_JSON, JsonDataVersionDetector.detect("[".repeat(limit + 1)
                + "0" + "]".repeat(limit + 1)));
    }

    @Test
    public void detect_excessivelyNestedContainers_rejectsWithoutRecursiveTreeConstruction() {
        for (String value : new String[]{"[".repeat(50_000) + "0" + "]".repeat(50_000),
            "{\"child\":".repeat(50_000) + "0" + "}".repeat(50_000),
            "[{\"child\":".repeat(25_000) + "0" + "}]".repeat(25_000)}) {
            assertEquals(MALFORMED_JSON, JsonDataVersionDetector.detect(value));
            assertEquals(MALFORMED_JSON, JsonDataVersionDetector.detect(
                    "{\"schemaVersion\":1,\"people\":" + value + "}"));
            assertEquals(MALFORMED_JSON, JsonDataVersionDetector.detect(
                    "{\"schemaVersion\":99,\"extra\":" + value + "}"));
        }
    }

    @Test
    public void detect_containerCharactersInStringsAndWideArrays_doNotCountAsNesting() {
        String text = "[{}]".repeat(50_000) + "\\\"[\\\\]";
        assertEquals(LEGACY_AB3, JsonDataVersionDetector.detect(
                "{\"persons\":[],\"_comment\":\"" + text + "\"}"));
        assertEquals(LEGACY_AB3, JsonDataVersionDetector.detect(
                "{\"persons\":[" + "0,".repeat(10_000) + "0]}"));
    }

    @Test
    public void detect_existingLegacyFixture_identifiesLegacy() throws Exception {
        Path fixture = Path.of("src", "test", "data", "JsonDataVersionDetectorTest", "legacyAddressBook.json");
        assertEquals(LEGACY_AB3, JsonDataVersionDetector.detect(Files.readString(fixture)));
        assertEquals(LEGACY_AB3, JsonDataVersionDetector.detect("{\"persons\": []}"));
    }

    @Test
    public void detect_legacyRecords_doesNotDecodeOrInferRoles() {
        assertEquals(LEGACY_AB3, JsonDataVersionDetector.detect("{\"persons\": [null, {\"name\": \"Alex\"}]}"));
    }

    @Test
    public void detect_supportedVersion_requiresFurtherValidation() {
        assertEquals(PONHUB_V1_CANDIDATE, JsonDataVersionDetector.detect("{\"schemaVersion\": 1}"));
        assertEquals(PONHUB_V1_CANDIDATE, JsonDataVersionDetector.detect(
                "{\"schemaVersion\": 1, \"persons\": null}"));
    }

    @Test
    public void detect_invalidVersion_rejectsWithoutCoercion() {
        for (String value : new String[]{"null", "\"\"", "\" \"", "\"1\"", "1.0", "1e0",
            "true", "[]", "{}", "0", "-1"}) {
            assertEquals(INVALID_VERSION, JsonDataVersionDetector.detect(
                    "{\"schemaVersion\":" + value + ",\"persons\":[]}"), value);
        }
    }

    @Test
    public void detect_futureVersion_neverFallsBackToLegacy() {
        for (String value : new String[]{"2", "2147483648", "999999999999999999999999999999999"}) {
            assertEquals(UNSUPPORTED_VERSION, JsonDataVersionDetector.detect(
                    "{\"schemaVersion\":" + value + ",\"persons\":[],\"lessons\":[]}"), value);
        }
    }

    @Test
    public void detect_foreignOrWrongShapedRoot_rejects() {
        for (String json : new String[]{"null", "[]", "1", "true", "\"text\"", "{}",
            "{\"persons\":null}", "{\"persons\":{}}", "{\"people\":[]}",
            "{\"persons\":[],\"lessons\":[]}", "{\"version\":1,\"persons\":[]}",
            "{\"persons\":[],\"_comment\":null}", "{\"persons\":[],\"_comment\":{}}",
            "{\"persons\":[],\"_comment\":\"note\",\"lessons\":[]}"}) {
            assertEquals(UNRECOGNIZED_FORMAT, JsonDataVersionDetector.detect(json), json);
        }
    }

    @Test
    public void detect_malformedOrMultipleDocuments_rejects() {
        for (String json : new String[]{"", " \n", "{", "{\"persons\":[}", "{\"persons\":[],}",
            "{\"persons\":[]} {}", "{\"schemaVersion\":1} null", "{\"schemaVersion\":1} garbage"}) {
            assertEquals(MALFORMED_JSON, JsonDataVersionDetector.detect(json), json);
        }
    }

    @Test
    public void detect_duplicateFields_rejectsAmbiguousVersionAndRecords() {
        for (String json : new String[]{"{\"schemaVersion\":2,\"schemaVersion\":1}",
            "{\"persons\":[],\"persons\":[]}", "{\"persons\":[{\"name\":\"A\",\"name\":\"B\"}]}"}) {
            assertEquals(MALFORMED_JSON, JsonDataVersionDetector.detect(json), json);
        }
    }

    @Test
    public void detect_nullInput_throws() {
        assertThrows(NullPointerException.class, () -> JsonDataVersionDetector.detect(null));
    }
}
