package hirs.utils.rim.unsignedRim.cbor.ietfCoswid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the CoswidConfigValidator class.
 * These tests verify that the validator correctly identifies valid and invalid CoSWID config files.
 */
public class CoswidConfigValidatorTest {
    /**
     * Tests that a known good CoSWID config file is validated as valid.
     * @throws IOException if an I/O error occurs while reading the file
     */
    @Test
    public final void testGoodCoswidConfig() throws IOException {
        String coswidConfigFile = "coswid/config/coswid_rim_1_good.json";
        ObjectMapper mapper = new ObjectMapper();

        ClassLoader classLoader = getClass().getClassLoader();
        InputStream coswidConfigStream = classLoader.getResourceAsStream(coswidConfigFile);
        byte[] coswidData = coswidConfigStream.readAllBytes();

        JsonNode rootNode = mapper.readTree(coswidData);
        CoswidConfigValidator configValidator = new CoswidConfigValidator();
        boolean isValid = configValidator.isValid(rootNode);
        assertTrue(isValid);
        final int invalidFieldCountExpected = 0;
        assertEquals(invalidFieldCountExpected, configValidator.getInvalidFieldCount());
    }

    /**
     * Tests that a single invalid field in a CoSWID config file is detected.
     * @throws IOException if an I/O error occurs while reading the file
     */
    @Test
    public final void testSingleBadField() throws IOException {
        String coswidConfigFile = "coswid/config/coswid_rim_bad_tagId.json";
        ObjectMapper mapper = new ObjectMapper();

        ClassLoader classLoader = getClass().getClassLoader();
        InputStream coswidConfigStream = classLoader.getResourceAsStream(coswidConfigFile);
        byte[] coswidData = coswidConfigStream.readAllBytes();

        JsonNode rootNode = mapper.readTree(coswidData);
        CoswidConfigValidator configValidator = new CoswidConfigValidator();
        boolean isValid = configValidator.isValid(rootNode);
        assertFalse(isValid);
        final int invalidFieldCountExpected = 1;
        assertEquals(invalidFieldCountExpected, configValidator.getInvalidFieldCount());
    }

    /**
     * Tests that a CoSWID config file with multiple invalid fields is correctly reported.
     * @throws IOException if an I/O error occurs while reading the file
     */
    @Test
    public final void testMultipleBadFields() throws IOException {
        String coswidConfigFile = "coswid/config/coswid_rim_all_bad.json";
        ObjectMapper mapper = new ObjectMapper();

        ClassLoader classLoader = getClass().getClassLoader();
        InputStream coswidConfigStream = classLoader.getResourceAsStream(coswidConfigFile);
        byte[] coswidData = coswidConfigStream.readAllBytes();

        JsonNode rootNode = mapper.readTree(coswidData);
        CoswidConfigValidator configValidator = new CoswidConfigValidator();
        boolean isValid = configValidator.isValid(rootNode);
        assertFalse(isValid);
        final int invalidFieldCountExpected = 27;
        assertEquals(invalidFieldCountExpected, configValidator.getInvalidFieldCount());
    }
}
