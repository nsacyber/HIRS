package hirs.utils.rim.unsignedRim.cbor.ietfCorim;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Tests basic CoRIM building functionality, including CoMIDs.
 */
public class CorimBuilderTest {
    /**
     * Tests basic attributes of a CoRIM object without an included CoMID.
     *
     * @throws IOException        if there are any issues trying to read the provided files
     * @throws URISyntaxException if there are any issues interpreting the provided paths
     */
    @Test
    public final void buildCorimWithoutComid() throws IOException, URISyntaxException {
        final String corimConfigFile = "corim/config/corim_fields_without_comid.json";
        final String expectedCborFile = "corim/corim_expected_without_comid.cbor";

        final ClassLoader classLoader = getClass().getClassLoader();
        final URL resourceUrl = classLoader.getResource(corimConfigFile);

        // Create unsigned CoRIM using configuration file
        assert resourceUrl != null;
        final String resourceUrlPath = Paths.get(resourceUrl.toURI()).toString();
        final byte[] outputCorim = CoRimBuilder.build(resourceUrlPath);

        // Read expected CoRIM bytes from file
        final URL expectedResourceUrl = classLoader.getResource(expectedCborFile);
        assert expectedResourceUrl != null;
        final Path expectedResourceUrlPath = Paths.get(expectedResourceUrl.toURI());
        final byte[] expectedCorim = Files.readAllBytes(expectedResourceUrlPath);

        // Verify that output matches expected CBOR
        assertArrayEquals(outputCorim, expectedCorim);
    }

    /**
     * Tests basic attributes of a CoRIM object with an included CoMID.
     *
     * @throws IOException        if there are any issues trying to read the provided files
     * @throws URISyntaxException if there are any issues interpreting the provided paths
     */
    @Test
    public final void buildCorimWithComid() throws IOException, URISyntaxException {
        final String corimConfigFile = "corim/config/corim_fields_with_comid.json";
        final String expectedCborFile = "corim/corim_expected_with_comid.cbor";

        final ClassLoader classLoader = getClass().getClassLoader();
        final URL resourceUrl = classLoader.getResource(corimConfigFile);

        // Create unsigned CoRIM using configuration file
        assert resourceUrl != null;
        final String resourceUrlPath = Paths.get(resourceUrl.toURI()).toString();
        final byte[] outputCorim = CoRimBuilder.build(resourceUrlPath);

        // Read expected CoRIM bytes from file
        final URL expectedResourceUrl = classLoader.getResource(expectedCborFile);
        assert expectedResourceUrl != null;
        final Path expectedResourceUrlPath = Paths.get(expectedResourceUrl.toURI());
        final byte[] expectedCorim = Files.readAllBytes(expectedResourceUrlPath);

        // Verify that output matches expected CBOR
        assertArrayEquals(outputCorim, expectedCorim);
    }
}
