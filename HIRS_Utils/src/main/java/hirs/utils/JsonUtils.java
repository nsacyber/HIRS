package hirs.utils;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import hirs.utils.tpm.eventlog.uefi.UefiGuid;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A utility class for common JSON operations using the {@link com.eclipsesource}
 * library.
 */
@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonUtils {

    /**
     * Getter for the JSON Object that is associated with the elementName value
     * mapped in the associated JSON file.
     * Default {@link java.nio.charset.Charset} is UTF 8
     *
     * @param jsonPath the object holding the location of the file to parse.
     * @param elementName the specific object to pull from the file
     * @return a JSON object
     */
    public static JsonObject getSpecificJsonObject(final Path jsonPath, final String elementName) {
        // find the file and load it
        return getSpecificJsonObject(jsonPath, elementName, StandardCharsets.UTF_8);
    }

    /**
     * Getter for the JSON Object that is associated with the elementName value
     * mapped in the associated JSON file.
     * Default {@link java.nio.charset.Charset} is UTF 8
     *
     * @param jsonPath the object holding the location of the file to parse.
     * @param elementName the specific object to pull from the file
     * @param charset the character set to use
     * @return a JSON object
     */
    public static JsonObject getSpecificJsonObject(final Path jsonPath,
                                                   final String elementName,
                                                   final Charset charset) {
        // find the file and load it
        JsonObject jsonObject = getJsonObject(jsonPath, charset);

        if (jsonObject != null && jsonObject.get(elementName) != null) {
            return jsonObject.get(elementName).asObject();
        }

        return new JsonObject();
    }

    /**
     * Getter for the JSON Object that is mapped in the associated JSON file.
     * Default {@link java.nio.charset.Charset} is UTF 8
     *
     * @param jsonPath the object holding the location of the file to parse.
     * @return a JSON object
     */
    public static JsonObject getJsonObject(final Path jsonPath) {
        return getJsonObject(jsonPath, StandardCharsets.UTF_8);
    }

    /**
     * Getter for the JSON Object that is mapped in the associated JSON file.
     *
     * @param jsonPath the object holding the location of the file to parse.
     * @param charset the character set to use
     * @return a JSON object
     */
    public static JsonObject getJsonObject(final Path jsonPath, final Charset charset) {
        // find the file and load it
        JsonObject jsonObject = new JsonObject();

        if (Files.notExists(jsonPath)) {
            log.warn(String.format("No file found at %s.", jsonPath.toString()));
        } else {
            try {
                InputStream inputStream = new FileInputStream(jsonPath.toString());
                jsonObject = Json.parse(new InputStreamReader(inputStream,
                        charset)).asObject();
            } catch (IOException ex) {
                // add log file thing here indication issue with JSON File
                jsonObject = new JsonObject();
            }
        }

        return jsonObject;
    }

    /**
     * Getter for the JSON Object that is associated with the elementName value
     * mapped in the associated JSON file.
     * Default {@link java.nio.charset.Charset} is UTF 8
     *
     * @param jsonFilename the object holding the name of the file in classpath to parse.
     * @param elementName the specific object to pull from the file
     * @return a JSON object
     */
    public static JsonObject getSpecificJsonObject(final String jsonFilename, final String elementName) {
        // find the file and load it
        return getSpecificJsonObject(jsonFilename, elementName, StandardCharsets.UTF_8);
    }

    /**
     * Getter for the JSON Object that is associated with the elementName value
     * mapped in the associated JSON file.
     * Default {@link java.nio.charset.Charset} is UTF 8
     *
     * @param jsonFilename the object holding the name of the file in classpath to parse.
     * @param elementName the specific object to pull from the file
     * @param charset the character set to use
     * @return a JSON object
     */
    public static JsonObject getSpecificJsonObject(final String jsonFilename,
                                                   final String elementName,
                                                   final Charset charset) {
        // find the file and load it
        JsonObject jsonObject = getJsonObject(jsonFilename, charset);

        if (jsonObject != null && jsonObject.get(elementName) != null) {
            return jsonObject.get(elementName).asObject();
        }

        return new JsonObject();
    }

    /**
     * Getter for the JSON Object that is mapped in the associated JSON file.
     * Default {@link java.nio.charset.Charset} is UTF 8
     *
     * @param jsonFilename the object holding the name of the file in classpath to parse.
     * @return a JSON object
     */
    public static JsonObject getJsonObject(final String jsonFilename) {
        return getJsonObject(jsonFilename, StandardCharsets.UTF_8);
    }

    /**
     * Getter for the JSON Object that is mapped in the associated JSON file.
     *
     * @param jsonFilename the object holding the name of the file in classpath to parse.
     * @param charset the character set to use
     * @return a JSON object
     */
    public static JsonObject getJsonObject(final String jsonFilename, final Charset charset) {
        JsonObject jsonObject = new JsonObject();

        InputStream jsonIs = UefiGuid.class
                    .getClassLoader().getResourceAsStream(jsonFilename);
        if (jsonIs != null) {
            try {
                jsonObject = Json.parse(new InputStreamReader(jsonIs,
                        charset)).asObject();
            } catch (IOException ex) {
                jsonObject = new JsonObject();
            }
        }

        return jsonObject;
    }
}
