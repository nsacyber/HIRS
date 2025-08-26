package hirs.utils.rim.unsignedRim.cbor.ietfCorim;

import com.authlete.cbor.CBORDecoder;
import com.authlete.cbor.CBORTaggedItem;
import hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid.Comid;
import hirs.utils.rim.unsignedRim.common.IanaHashAlg;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
     */
    @Test
    public final void parseCoRIMWithoutCoMID() throws IOException {
        // Read CoRIM from file
        String corimFile = "corim/corim_expected_without_comid.cbor";
        ClassLoader classLoader = getClass().getClassLoader();
        URL corimUrl = classLoader.getResource(corimFile);
        assert corimUrl != null;
        byte[] expectedCorim = Files.readAllBytes(Path.of(corimUrl.getPath()));

        // Parse CoRIM (remove tag first)
        CBORDecoder decoder = new CBORDecoder(expectedCorim, 0, expectedCorim.length);
        CBORTaggedItem taggedUnsignedCorim = (CBORTaggedItem) decoder.next();
        CoRimParser corimParser = new CoRimParser(taggedUnsignedCorim.getTagContent().encode());

        // Check select attributes
        assertEquals(corimParser.id, "Test CoRIM 1");
        Object[] testDependentRim = corimParser.dependentRims.get(1);
        assertEquals(testDependentRim[0], "https://testURI1");
        assertEquals(testDependentRim[1], IanaHashAlg.SHA_256.getAlgId());
        String testThumbprint = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";
        assertArrayEquals((byte[]) testDependentRim[2], Hex.decode(testThumbprint));
        assertEquals(corimParser.profile, "https://testprofile");
        final long notAfterTimestamp = 1749227400L;
        assertEquals(corimParser.notAfter, notAfterTimestamp);
        assertEquals(corimParser.entityName, "Test Inc.");
        assertEquals(corimParser.entityRegId, "https://testing");
        assertEquals(corimParser.entityRole, "tag-creator");
    }

    /**
     * Tests the ability of the CoRIM parser to correctly parse an unsigned CoRIM
     * that includes a CoMID tag.
     */
    @Test
    public final void parseCoRIMWithCoMID() throws IOException {
        // Read CoRIM from file
        String corimFile = "corim/corim_expected_with_comid.cbor";
        ClassLoader classLoader = getClass().getClassLoader();
        URL corimUrl = classLoader.getResource(corimFile);
        assert corimUrl != null;
        byte[] expectedCorim = Files.readAllBytes(Path.of(corimUrl.getPath()));

        // Parse CoRIM (remove tag first)
        CBORDecoder decoder = new CBORDecoder(expectedCorim, 0, expectedCorim.length);
        CBORTaggedItem taggedUnsignedCorim = (CBORTaggedItem) decoder.next();
        CoRimParser corimParser = new CoRimParser(taggedUnsignedCorim.getTagContent().encode());

        // Check select attributes
        assertEquals(corimParser.id, "Test CoRIM 2");
        Object[] testDependentRim = corimParser.dependentRims.get(0);
        assertEquals(testDependentRim[0], "[https://testURI1, https://testURI2, https://testURI3]");
        assertEquals(corimParser.profile, "https://testprofile");
        final long notAfterTimestamp = 1751376600L;
        assertEquals(corimParser.notAfter, notAfterTimestamp);
        assertEquals(corimParser.entityName, "ACME Inc.");
        assertEquals(corimParser.entityRegId, "https://testing");
        assertEquals(corimParser.entityRole, "tag-creator");

        // Check select CoMID attributes
        checkCoMIDAttributes(corimParser.comidList.get(0));
    }

    /**
     * Validates the attributes of a CoMID object to ensure they match expected values.
     * This is used to verify the correctness of CoMID data after parsing.
     *
     * @param comid The CoMID object to validate.
     */
    public static void checkCoMIDAttributes(final Comid comid) {
        assertEquals(comid.getLanguage(), Locale.US);
        String testUUID = "742429b4-22ea-4d3b-baca-1e46fffa7379";
        assertEquals(comid.getTagIdentity().getTagIdUUID(), UUID.fromString(testUUID));
        String testVendor = comid.getTriples().getReferenceTriples().get(0).getRefEnv().getComidClass()
                .getVendor();
        assertEquals(testVendor, "ACME Inc.");
        String testModel = comid.getTriples().getReferenceTriples().get(0).getRefEnv().getComidClass()
                .getModel();
        assertEquals(testModel, "ACME 9000");
        int testMkey = comid.getTriples().getReferenceTriples().get(0).getRefClaims().get(1).getMkeyInt();
        assertEquals(testMkey, 2);
        IanaHashAlg testDigestAlg = comid.getTriples().getReferenceTriples().get(0).getRefClaims().get(1)
                .getMval().getDigests().get(0).getAlg();
        assertEquals(testDigestAlg, IanaHashAlg.SHA_256);
        byte[] testDigestVal = comid.getTriples().getReferenceTriples().get(0).getRefClaims().get(1).getMval()
                .getDigests().get(0).getVal();
        String expectedDigestVal = "17b66b2c3ae27ec5b0d20ae4987dadff4d1b81941c010c94675a60fb97b4e700";
        assertArrayEquals(testDigestVal, Hex.decode(expectedDigestVal));
    }
}
