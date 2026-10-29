package ponhub.commons.util;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ponhub.commons.core.LogsCenter;
import ponhub.commons.exceptions.DataLoadingException;

/**
 * Converts a Java object instance to JSON and vice versa
 */
public class JsonUtil {

    static final int MAX_NUMERIC_TOKEN_LENGTH = 64;
    static final int MAX_NESTING_DEPTH = 64;
    static final int MAX_ERROR_MESSAGE_LENGTH = 512;

    private static final Logger logger = LogsCenter.getLogger(JsonUtil.class);

    private static ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    static <T> void serializeObjectToJsonFile(Path jsonFile, T objectToSerialize) throws IOException {
        FileUtil.writeToFile(jsonFile, toJsonString(objectToSerialize));
    }

    static <T> T deserializeObjectFromJsonFile(Path jsonFile, Class<T> classOfObjectToDeserialize)
            throws IOException {
        return fromJsonString(FileUtil.readFromFile(jsonFile), classOfObjectToDeserialize);
    }

    /**
     * Returns the JSON object from the given file or {@code Optional.empty()} object if the file is not found.
     * If any values are missing from the file, default values will be used, as long as the file is a valid JSON file.
     *
     * @param filePath cannot be null.
     * @param classOfObjectToDeserialize JSON file has to correspond to the structure in the class given here.
     * @throws DataLoadingException if loading failed or the file contains a null root value.
     */
    public static <T> Optional<T> readJsonFile(
            Path filePath, Class<T> classOfObjectToDeserialize) throws DataLoadingException {
        requireNonNull(filePath);

        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        logger.info("JSON file " + filePath + " found.");

        T jsonFile;

        try {
            jsonFile = deserializeObjectFromJsonFile(filePath, classOfObjectToDeserialize);
            if (jsonFile == null) {
                throw new IOException("JSON file must contain a non-null object.");
            }
        } catch (IOException e) {
            // Jackson messages can include the complete offending value. Do not retain an unbounded cause.
            String detail = e.getClass().getSimpleName() + ": " + e.getMessage();
            if (detail.length() > MAX_ERROR_MESSAGE_LENGTH) {
                detail = detail.substring(0, MAX_ERROR_MESSAGE_LENGTH) + " [truncated]";
            }
            logger.warning("Error reading from jsonFile file " + filePath + ": " + detail);
            throw new DataLoadingException(new IOException(detail));
        }

        return Optional.of(jsonFile);
    }

    /**
     * Saves the Json object to the specified file.
     * Overwrites existing file if it exists, creates a new file if it doesn't.
     * @param jsonFile cannot be null
     * @param filePath cannot be null
     * @throws IOException if there was an error during writing to the file
     */
    public static <T> void saveJsonFile(T jsonFile, Path filePath) throws IOException {
        requireNonNull(filePath);
        requireNonNull(jsonFile);

        serializeObjectToJsonFile(filePath, jsonFile);
    }


    /**
     * Converts a given string representation of JSON data to an instance of a class
     * @param <T> The generic type to create an instance of
     * @return The instance of T with the specified values in the JSON string
     */
    public static <T> T fromJsonString(String json, Class<T> instanceClass) throws IOException {
        validateResourceLimits(json);
        return objectMapper.readValue(json, instanceClass);
    }

    /**
     * Bounds preference numbers before Jackson converts them, including values in ignored fields.
     * Scan token text only: numeric getters can construct BigInteger before reporting an int overflow.
     * A depth bound also protects the subsequent object reader and its unknown-field skipping.
     */
    private static void validateResourceLimits(String json) throws IOException {
        try (JsonParser parser = objectMapper.getFactory().createParser(json)) {
            int depth = 0;
            JsonToken token;
            while ((token = parser.nextToken()) != null) {
                if (token.isNumeric() && parser.getTextLength() > MAX_NUMERIC_TOKEN_LENGTH) {
                    throw new IOException("JSON numeric token exceeds " + MAX_NUMERIC_TOKEN_LENGTH + " characters.");
                }
                if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
                    if (++depth > MAX_NESTING_DEPTH) {
                        throw new IOException("JSON nesting exceeds " + MAX_NESTING_DEPTH + " containers.");
                    }
                } else if (token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) {
                    depth--;
                }
            }
        }
    }

    /**
     * Converts a given instance of a class into its JSON data string representation
     * @param instance The T object to be converted into the JSON string
     * @param <T> The generic type to create an instance of
     * @return JSON data representation of the given class instance, in string
     */
    public static <T> String toJsonString(T instance) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(instance);
    }

}
