package hirs.tpm.eventlog.events;

import hirs.tpm.eventlog.TCGEventLogTest;
import hirs.tpm.eventlog.uefi.UefiGuid;
import hirs.tpm.eventlog.uefi.UefiPartition;
import hirs.utils.HexUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;

/**
 * Class for testing TCG Event Log Event processing.
 */
public class TCGEventLogEventsTest {
    // Variable files collected using an Event Parsing tool
    private static final String EVENT_SPECID = "/tcgeventlog/events/EvEfiSpecId.txt";
    private static final String EVENT_BOOTSERVICES
            = "/tcgeventlog/events/EvBootServicesApplication.txt";
    private static final String EVENT_GPT_PARTITION
            = "/tcgeventlog/events/EvEfiGptPartition.txt";
    private static final String EVENT_HANDOFF_TABLES = "/tcgeventlog/events/EvHandoffTables.txt";
    private static final String UEFI_POST_CODE = "/tcgeventlog/events/EvPostCode.txt";
    private static final Logger LOGGER
            = LogManager.getLogger(TCGEventLogTest.class);
    private static final String JSON_FILE = "/tcgeventlog/uefi/vendor-table.json";

    /**
     * Initializes a <code>SessionFactory</code>.
     */
    @BeforeClass
    public static final void setup() {
        LOGGER.debug("retrieving session factory");

    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterClass
    public static final void tearDown() {
        LOGGER.debug("closing session factory");
    }

    /**
     * Tests the processing of a SpecIDEvent event.
     *
     * @throws IOException when processing the test fails
     */
    @Test
    public final void testSpecIDEvent() throws IOException {
        LOGGER.debug("Testing the SpecID Event Processing");
        String event = IOUtils.toString(this.getClass().getResourceAsStream(EVENT_SPECID), "UTF-8");
        byte[] eventBytes = HexUtils.hexStringToByteArray(event);
        EvEfiSpecIdEvent specEvent = new EvEfiSpecIdEvent(eventBytes);
        Assert.assertTrue(specEvent.isCryptoAgile());
        Assert.assertEquals(specEvent.getSignature(), "Spec ID Event03");
    }

    /**
     * Tests the processing of a Boot Services App event.
     *
     * @throws IOException              when processing the test fails
     * @throws NoSuchAlgorithmException if an unknown algorithm is encountered.
     * @throws CertificateException     if a certificate fails to parse.
     */
    @Test
    public final void testEvBootServicesApp() throws IOException {
        LOGGER.debug("Testing the parsing of Boot Services Application Event");
        String event = IOUtils.toString(this.getClass().getResourceAsStream(EVENT_BOOTSERVICES),
                "UTF-8");
        byte[] eventBytes = HexUtils.hexStringToByteArray(event);
        EvEfiBootServicesApp bootService = new EvEfiBootServicesApp(eventBytes);
        String address = HexUtils.byteArrayToHexString(bootService.getImagePhysicalAddress());
        Assert.assertEquals(address, "1820d45800000000");
        String path = bootService.getDevicePath().toString();
        Assert.assertTrue(path.contains("PIWG Firmware Volume "
                + "b6ede22c-de30-45fa-bb09-ca202c1654b7"));
    }

    /**
     * Tests the processing of a Boot Services App event.
     *
     * @throws IOException              when processing the test fails
     * @throws NoSuchAlgorithmException if an unknown algorithm is encountered.
     * @throws CertificateException     if a certificate fails to parse.
     */
    @Test
    public final void testEvGptPartiton() throws IOException {
        LOGGER.debug("Testing the parsing of Boot Services Application Event");
        String event = IOUtils.toString(this.getClass().getResourceAsStream(EVENT_GPT_PARTITION),
                "UTF-8");
        byte[] eventBytes = HexUtils.hexStringToByteArray(event);
        EvEfiGptPartition partition = new EvEfiGptPartition(eventBytes);
        ArrayList<UefiPartition> partitonList = partition.getPartitionList();
        int partNumber = 1;
        for (UefiPartition parition : partitonList) {
            UefiGuid guidPart = parition.getPartitionTypeGUID();
            UefiGuid guidUnique = parition.getUniquePartitionGUID();
            String name = parition.getName();
            if (partNumber == 1) {
                Assert.assertTrue(guidPart.toString().
                        contains("de94bba4-06d1-4d40-a16a-bfd50179d6a"));
                Assert.assertTrue(guidUnique.toString().
                        contains("42cc8787-db23-4e45-9981-701adc801dc7"));
                Assert.assertEquals(name, "Basic data partition");
            }
            if (partNumber == 2) {
                Assert.assertTrue(guidPart.toString().
                        contains("c12a7328-f81f-11d2-ba4b-00a0c93ec93b"));
                Assert.assertTrue(guidUnique.toString().
                        contains("8ca7623c-041e-4fab-8c12-f49a86b85d73"));
                Assert.assertEquals(name, "EFI system partition");
            }
            if (partNumber++ == 3) {
                Assert.assertTrue(guidPart.toString().
                        contains("e3c9e316-0b5c-4db8-817d-f92df00215ae"));
                Assert.assertTrue(guidUnique.toString().
                        contains("d890cfff-320c-4f45-b6cf-a4d8bee6d9cb"));
                Assert.assertEquals(name, "Microsoft reserved partition");
            }
        }
    }

    /**
     * Tests the processing of a Hand off Table event.
     *
     * @throws IOException when processing the test fails
     */
    @Test
    public final void testHandOffTables() throws IOException, URISyntaxException {
        LOGGER.debug("Testing the Hand Off Tables Event Processing");
        String event = IOUtils.toString(this.getClass().
                getResourceAsStream(EVENT_HANDOFF_TABLES), "UTF-8");
        byte[] eventBytes = HexUtils.hexStringToByteArray(event);
        EvEfiHandoffTable hTable = new EvEfiHandoffTable(eventBytes,
                Paths.get(this.getClass().getResource(JSON_FILE).toURI()));
        Assert.assertEquals(hTable.getNumberOfTables(), 1);
        String tableInfo = hTable.toString();
        LOGGER.error(tableInfo);
        Assert.assertTrue(tableInfo.
                contains("UEFI industry standard table type = SMBIOS3_TABLE_GUID"));
    }

    /**
     * Tests the processing of a Post Code event.
     *
     * @throws IOException when processing the test fails
     */
    @Test
    public final void testPostCode() throws IOException {
        LOGGER.debug("Testing the Post Code Event Processing");
        String event = IOUtils.toString(this.getClass().getResourceAsStream(UEFI_POST_CODE),
                "UTF-8");
        byte[] eventBytes = HexUtils.hexStringToByteArray(event);
        EvPostCode pCode = new EvPostCode(eventBytes);
        Assert.assertTrue(pCode.isString());
        String postCode = pCode.toString();
        Assert.assertEquals(postCode, "ACPI DATA");
    }
}
