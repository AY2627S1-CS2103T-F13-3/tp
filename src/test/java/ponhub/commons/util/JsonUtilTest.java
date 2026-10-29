package ponhub.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.annotation.JsonValue;

import ponhub.model.UserPrefs;
import ponhub.testutil.SerializableTestClass;
import ponhub.testutil.TestUtil;

/**
 * Tests JSON Read and Write
 */
public class JsonUtilTest {

    private static final Path SERIALIZATION_FILE = TestUtil.getFilePathInSandboxFolder("serialize.json");

    @Test
    public void serializeObjectToJsonFile_noExceptionThrown() throws IOException {
        SerializableTestClass serializableTestClass = new SerializableTestClass();
        serializableTestClass.setTestValues();

        JsonUtil.serializeObjectToJsonFile(SERIALIZATION_FILE, serializableTestClass);

        assertEquals(FileUtil.readFromFile(SERIALIZATION_FILE), SerializableTestClass.JSON_STRING_REPRESENTATION);
    }

    @Test
    public void deserializeObjectFromJsonFile_noExceptionThrown() throws IOException {
        FileUtil.writeToFile(SERIALIZATION_FILE, SerializableTestClass.JSON_STRING_REPRESENTATION);

        SerializableTestClass serializableTestClass = JsonUtil
                .deserializeObjectFromJsonFile(SERIALIZATION_FILE, SerializableTestClass.class);

        assertEquals(serializableTestClass.getName(), SerializableTestClass.getNameTestValue());
        assertEquals(serializableTestClass.getListOfLocalDateTimes(), SerializableTestClass.getListTestValues());
        assertEquals(serializableTestClass.getMapOfIntegerToString(), SerializableTestClass.getHashMapTestValues());
    }

    @Test
    public void saveJsonFile_serializationFailure_preservesDestination(@TempDir Path directory) throws IOException {
        Path target = directory.resolve("data.json");
        Files.writeString(target, "original");
        assertThrows(IOException.class, () -> JsonUtil.saveJsonFile(new UnserializableValue(), target));
        assertEquals("original", Files.readString(target));
    }

    @Test
    public void fromJsonString_numericBoundary_checksIgnoredIntegersFractionsAndExponents() throws Exception {
        int limit = JsonUtil.MAX_NUMERIC_TOKEN_LENGTH;
        for (String number : new String[]{"9".repeat(limit), "-" + "9".repeat(limit - 1),
            "0." + "9".repeat(limit - 2), "1e" + "9".repeat(limit - 2)}) {
            assertEquals(new UserPrefs(), JsonUtil.fromJsonString("{\"extra\":" + number + "}", UserPrefs.class));
            IOException error = assertThrows(IOException.class, () -> JsonUtil.fromJsonString(
                    "{\"extra\":" + number + "9}", UserPrefs.class));
            assertEquals("JSON numeric token exceeds 64 characters.", error.getMessage());
        }
    }

    @Test
    public void fromJsonString_nestingBoundary_protectsIgnoredValues() throws Exception {
        int limit = JsonUtil.MAX_NESTING_DEPTH;
        String atLimit = "{\"extra\":" + "[".repeat(limit - 1) + "0" + "]".repeat(limit - 1) + "}";
        assertEquals(new UserPrefs(), JsonUtil.fromJsonString(atLimit, UserPrefs.class));
        String tooDeep = "{\"extra\":" + "[".repeat(limit) + "0" + "]".repeat(limit) + "}";
        assertThrows(IOException.class, () -> JsonUtil.fromJsonString(tooDeep, UserPrefs.class));
    }

    private static class UnserializableValue {
        @JsonValue
        public String value() {
            throw new IllegalStateException("Cannot serialize");
        }
    }

    //TODO: @Test jsonUtil_readJsonStringToObjectInstance_correctObject()

    //TODO: @Test jsonUtil_writeThenReadObjectToJson_correctObject()
}
