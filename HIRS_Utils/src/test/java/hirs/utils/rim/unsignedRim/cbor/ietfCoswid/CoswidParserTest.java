package hirs.utils.rim.unsignedRim.cbor.ietfCoswid;

import hirs.utils.rim.unsignedRim.common.measurement.Measurement;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the basic parsing of an unsigned CoSWID object.
 */
public class CoswidParserTest {
    /**
     * Tests the variable assignment of various CoSWID attributes.
     *
     * @throws IOException if there are issues parsing the coswid file
     */
    @Test
    public final void testCoswidParse() throws IOException {
        String coswidConfigFile = "coswid/LVFS_sbom.coswid";
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream coswidConfigStream = classLoader.getResourceAsStream(coswidConfigFile);
        byte[] coswidData = coswidConfigStream.readAllBytes();

        CoswidParser cparser = new CoswidParser(coswidData);
        Coswid coswid = cparser.getCoswid();
        List<String> roles = coswid.getRoleCoswid();
        assertEquals("tag-creator", roles.get(0));
        assertEquals("software-creator", roles.get(1));
        assertEquals("maintainer", roles.get(2));

        assertEquals("en-US", coswid.getLang());
        assertEquals("fab60fae-7ae9-4b56-9c50-44c7e6836af5", coswid.getTagId());
        assertEquals("Test2.efi", coswid.getSoftwareName());
        assertTrue(coswid.isCorpus());
        assertFalse(coswid.isPatch());
        assertEquals("1", coswid.getSoftwareVersion());
        assertEquals("TestRUs", coswid.getEntityName());
        assertEquals("TestRUs.com", coswid.getRegId());
    }

    /**
     * Tests the parsing of the payload within a CoSWID object.
     *
     * @throws IOException if there are issues parsing the coswid payload
     */
    @Test
    public final void testCoswidPayload() throws IOException {
        HexFormat hexTool = HexFormat.of();

        String coswidConfigFile = "coswid/coswid_rim_1.coswid";
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream coswidConfigStream = classLoader.getResourceAsStream(coswidConfigFile);
        byte[] coswidData = coswidConfigStream.readAllBytes();

        CoswidParser cparser = new CoswidParser(coswidData);
        Coswid coswid = cparser.getCoswid();
        List<Measurement> measurements = coswid.getMeasurements();

        Measurement rimEntry = measurements.getFirst();
        String manufacturer = rimEntry.getManufacturer();
        String model = rimEntry.getModel();
        String revision = rimEntry.getRevision();
        String serialNumber = rimEntry.getSerialNumber(); // Doesn't exist for coswid
        String type = rimEntry.getMeasurementType().toString();
        int algInt = rimEntry.getAlg().getAlgId();
        String alg = rimEntry.getAlg().getAlgName();
        //String type = rimEntry.measTypeToString(rimEntry.getMeasurementType());
        //int algInt = rimEntry.getAlgInt();
        //String alg = IanaHashAlg.getAlgName(algInt);
        byte[] digestBytes = rimEntry.getMeasurementBytes();
        String digest = hexTool.formatHex(digestBytes);

        assertEquals("sha-256", alg);
        assertEquals("4479ca722623f8c47b703996ced3cbd981b06b1ae8a897db70137e0b7c546848", digest);
        //assertEquals("ProductA",Model);  // maps to coswid "product"
        //assertEquals("HIRS",Manufacturer); // maps to coswid "entity-name" if role contains software-creator
        //else its "reg-id"
        assertEquals("1", revision);
        //assertEquals("ietf_coswid",type);  // should be "ietf_coswid"
    }
}
