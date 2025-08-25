package hirs.utils.rim.unsignedRim.cbor.ietfCorim;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Tests basic CoRIM building functionality, including CoMIDs.
 */
public class CorimBuilderTest {
    /**
     * Tests basic attributes of a CoRIM object without an included CoMID.
     */
    @Test
    public final void buildCorimWithoutComid() throws IOException {
        String corimConfigFile = "corim/config/corim_fields_without_comid.json";
        String expectedCborFile = "corim/corim_expected_without_comid.cbor";

        ClassLoader classLoader = getClass().getClassLoader();
        URL resourceUrl = classLoader.getResource(corimConfigFile);

        // Create unsigned CoRIM using configuration file
        assert resourceUrl != null;
        byte[] outputCorim = CoRimBuilder.build(resourceUrl.getPath());

        // Read expected CoRIM bytes from file
        URL expectedResourceUrl = classLoader.getResource(expectedCborFile);
        assert expectedResourceUrl != null;
        byte[] expectedCorim = Files.readAllBytes(Path.of(expectedResourceUrl.getPath()));

        // Verify that output matches expected CBOR
        assertArrayEquals(outputCorim, expectedCorim);
    }

    /**
     * Tests basic attributes of a CoRIM object with an included CoMID.
     */
    @Test
    public final void buildCorimWithComid() throws IOException {
        String corimConfigFile = "corim/config/corim_fields_with_comid.json";
        String expectedCborFile = "corim/corim_expected_with_comid.cbor";

        ClassLoader classLoader = getClass().getClassLoader();
        URL resourceUrl = classLoader.getResource(corimConfigFile);

        // Create unsigned CoRIM using configuration file
        assert resourceUrl != null;
        byte[] outputCorim = CoRimBuilder.build(resourceUrl.getPath());

        // Read expected CoRIM bytes from file
        URL expectedResourceUrl = classLoader.getResource(expectedCborFile);
        assert expectedResourceUrl != null;
        byte[] expectedCorim = Files.readAllBytes(Path.of(expectedResourceUrl.getPath()));

        // Verify that output matches expected CBOR
        assertArrayEquals(outputCorim, expectedCorim);
    }
}
