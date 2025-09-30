package hirs.utils.rim.unsignedRim.cbor.ietfCorim;

import com.authlete.cbor.CBORDecoder;
import com.authlete.cbor.CBORTaggedItem;
import hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid.Comid;
import hirs.utils.rim.unsignedRim.common.IanaHashAlg;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests CoRIM parser functionality, including ability to parse CoMID tags and extract measurements.
 */
public class CorimParserTest {
    /**
     * Tests the ability of the CoRIM parser to correctly parse an unsigned CoRIM
     * that does not include a CoMID tag.
     *
     * @throws IOException        if there are any issues trying to read the provided files
     * @throws URISyntaxException if there are any issues interpreting the provided paths
     */
    @Test
    public final void parseCoRIMWithoutCoMID() throws IOException, URISyntaxException {
        // Read CoRIM from file
        final String corimFile = "corim/corim_expected_without_comid.cbor";
        final ClassLoader classLoader = getClass().getClassLoader();
        final URL corimUrl = classLoader.getResource(corimFile);
        assert corimUrl != null;
        final Path corimUrlPath = Paths.get(corimUrl.toURI());
        final byte[] actualCoRIM = Files.readAllBytes(corimUrlPath);

        // Parse CoRIM (remove tag first)
        CBORDecoder decoder = new CBORDecoder(actualCoRIM, 0, actualCoRIM.length);
        CBORTaggedItem taggedUnsignedCorim = (CBORTaggedItem) decoder.next();
        CoRimParser corimParser = new CoRimParser(taggedUnsignedCorim.getTagContent().encode());

        // Check select attributes
        assertEquals("Test CoRIM 1", corimParser.id);
        Object[] testDependentRim = corimParser.dependentRims.get(1);
        assertEquals("https://testURI1", testDependentRim[0]);
        assertEquals(testDependentRim[1], IanaHashAlg.SHA_256.getAlgId());
        String testThumbprint = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";
        assertArrayEquals((byte[]) testDependentRim[2], Hex.decode(testThumbprint));
        assertEquals("https://testprofile", corimParser.profile);
        final long notAfterTimestamp = 1749227400L;
        assertEquals(notAfterTimestamp, corimParser.notAfter);
        assertEquals("Test Inc.", corimParser.entityName);
        assertEquals("https://testing", corimParser.entityRegId);
        assertEquals("tag-creator", corimParser.entityRole);
    }

    /**
     * Tests the ability of the CoRIM parser to correctly parse an unsigned CoRIM
     * that includes a CoMID tag.
     *
     * @throws IOException        if there are any issues trying to read the provided files
     * @throws URISyntaxException if there are any issues interpreting the provided paths
     */
    @Test
    public final void parseCoRIMWithCoMID() throws IOException, URISyntaxException {
        // Read CoRIM from file
        final String corimFile = "corim/corim_expected_with_comid.cbor";
        final ClassLoader classLoader = getClass().getClassLoader();
        final URL corimUrl = classLoader.getResource(corimFile);
        assert corimUrl != null;
        final Path corimUrlPath = Paths.get(corimUrl.toURI());
        final byte[] actualCoRIM = Files.readAllBytes(corimUrlPath);

        // Parse CoRIM (remove tag first)
        CBORDecoder decoder = new CBORDecoder(actualCoRIM, 0, actualCoRIM.length);
        CBORTaggedItem taggedUnsignedCorim = (CBORTaggedItem) decoder.next();
        CoRimParser corimParser = new CoRimParser(taggedUnsignedCorim.getTagContent().encode());

        // Check select attributes
        assertEquals("Test CoRIM 2", corimParser.id);
        Object[] testDependentRim = corimParser.dependentRims.get(0);
        assertEquals("[https://testURI1, https://testURI2, https://testURI3]", testDependentRim[0]);
        assertEquals("https://testprofile", corimParser.profile);
        final long notAfterTimestamp = 1751376600L;
        assertEquals(notAfterTimestamp, corimParser.notAfter);
        assertEquals("ACME Inc.", corimParser.entityName);
        assertEquals("https://testing", corimParser.entityRegId);
        assertEquals("tag-creator", corimParser.entityRole);

        // Check select CoMID attributes
        checkCoMIDAttributes(corimParser.comidList.get(0));
    }

    /**
     * Validates the attributes of a CoMID object to ensure they match expected values.
     * This is used to verify the correctness of CoMID data after parsing.
     *
     * @param comid The CoMID object to validate.
     */
    private void checkCoMIDAttributes(final Comid comid) {
        assertEquals(Locale.US, comid.getLanguage());

        final String testUUID = "742429b4-22ea-4d3b-baca-1e46fffa7379";
        assertEquals(comid.getTagIdentity().getTagIdUUID(), UUID.fromString(testUUID));

        final String testVendor = comid.getTriples().getReferenceTriples().get(0).getRefEnv().getComidClass()
                .getVendor();
        assertEquals("ACME Inc.", testVendor);

        final String testModel = comid.getTriples().getReferenceTriples().get(0).getRefEnv().getComidClass()
                .getModel();
        assertEquals("ACME 9000", testModel);

        final int testMkey = comid.getTriples().getReferenceTriples().get(0).getRefClaims().get(1).getMkeyInt();
        assertEquals(2, testMkey);

        final IanaHashAlg testDigestAlg = comid.getTriples().getReferenceTriples().get(0).getRefClaims().get(1)
                .getMval().getDigests().get(0).getAlg();
        assertEquals(IanaHashAlg.SHA_256, testDigestAlg);

        final byte[] testDigestVal = comid.getTriples().getReferenceTriples().get(0).getRefClaims().get(1).getMval()
                .getDigests().get(0).getVal();
        final String expectedDigestVal = "17b66b2c3ae27ec5b0d20ae4987dadff4d1b81941c010c94675a60fb97b4e700";
        assertArrayEquals(testDigestVal, Hex.decode(expectedDigestVal));
    }
}
