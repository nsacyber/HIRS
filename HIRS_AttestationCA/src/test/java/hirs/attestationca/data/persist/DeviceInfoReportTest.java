package hirs.attestationca.data.persist;

import hirs.attestationca.persist.SpringPersistenceTest;
import hirs.attestationca.servicemanager.DBReportManager;
import hirs.attestationca.entity.DeviceInfoReport;
import hirs.data.persist.info.FirmwareInfo;
import hirs.data.persist.info.HardwareInfo;
import hirs.data.persist.info.NetworkInfo;
import hirs.data.persist.info.OSInfo;
import hirs.data.persist.info.TPMInfo;
import hirs.foss.XMLCleaner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.UUID;

/**
 * DeviceInfoReportTest is a unit test class for DeviceInfoReports.
 */
public class DeviceInfoReportTest extends SpringPersistenceTest {
    private final NetworkInfo networkInfo = createTestNetworkInfo();
    private final OSInfo osInfo = createTestOSInfo();
    private final FirmwareInfo firmwareInfo = createTestFirmwareInfo();
    private final HardwareInfo hardwareInfo = createTestHardwareInfo();
    private final TPMInfo tpmInfo = createTPMInfo();
    private static final String TEST_IDENTITY_CERT = "/tpm/sample_identity_cert.cer";

    private static final Logger LOGGER = LogManager.getLogger(DeviceInfoReportTest.class);

    private static final String EXPECTED_CLIENT_VERSION = "Test.Version";

    private ReportManager reportManager;

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an
     * in-memory database that is used for testing.
     */
    @BeforeClass
    public final void setup() {
        LOGGER.debug("retrieving session factory");
        reportManager = new DBReportManager(sessionFactory);
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterClass
    public final void tearDown() {
        LOGGER.debug("closing session factory");
    }

    /**
     * Tests instantiation of a DeviceInfoReport.
     */
    @Test
    public final void deviceInfoReport() {
        new DeviceInfoReport(networkInfo, osInfo, firmwareInfo, hardwareInfo, tpmInfo);
    }

    /**
     * Tests that NetworkInfo cannot be null.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void networkInfoNull() {
        new DeviceInfoReport(null, osInfo, firmwareInfo, hardwareInfo, tpmInfo);
    }

    /**
     * Tests that OSInfo cannot be null.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void osInfoNull() {
        new DeviceInfoReport(networkInfo, null, firmwareInfo, hardwareInfo, tpmInfo);
    }

    /**
     * Tests that FirmwareInfo cannot be null.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void firmwareInfoNull() {
        new DeviceInfoReport(networkInfo, osInfo, null, hardwareInfo, tpmInfo);
    }

    /**
     * Tests that HardwareInfo cannot be null.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void hardwareInfoNull() {
        new DeviceInfoReport(networkInfo, osInfo, firmwareInfo, null, tpmInfo);
    }

    /**
     * Tests that TPMInfo may be null.
     */
    @Test
    public final void tpmInfoNull() {
        new DeviceInfoReport(networkInfo, osInfo, firmwareInfo, hardwareInfo, null);
    }

    /**
     * Tests that the getters for DeviceInfoReport work as expected.
     */
    @Test
    public final void testGetters() {
        DeviceInfoReport deviceInfoReport =
                new DeviceInfoReport(networkInfo, osInfo, firmwareInfo, hardwareInfo, tpmInfo);
        Assert.assertEquals(deviceInfoReport.getNetworkInfo(), networkInfo);
        Assert.assertEquals(deviceInfoReport.getOSInfo(), osInfo);
        Assert.assertEquals(deviceInfoReport.getFirmwareInfo(), firmwareInfo);
        Assert.assertEquals(deviceInfoReport.getHardwareInfo(), hardwareInfo);
        Assert.assertEquals(deviceInfoReport.getTPMInfo(), tpmInfo);
        Assert.assertEquals(deviceInfoReport.getClientApplicationVersion(),
                EXPECTED_CLIENT_VERSION);
    }

    /**
     * Tests that the XML is generated correctly.
     *
     * @throws JAXBException
     *             in case there are errors marshalling/unmarshalling
     */
    @Test
    public final void marshalUnmarshalTest() throws JAXBException {
        DeviceInfoReport deviceInfoReport =
                new DeviceInfoReport(networkInfo, osInfo, firmwareInfo, hardwareInfo, tpmInfo);
        String xml = getXMLFromReport(deviceInfoReport);
        DeviceInfoReport deviceInfoReportFromXML = getReportFromXML(xml);
        Assert.assertEquals(deviceInfoReportFromXML.getNetworkInfo(), networkInfo);
        Assert.assertEquals(deviceInfoReportFromXML.getOSInfo(), osInfo);
        Assert.assertEquals(deviceInfoReport.getFirmwareInfo(), firmwareInfo);
        Assert.assertEquals(deviceInfoReport.getHardwareInfo(), hardwareInfo);
        Assert.assertEquals(deviceInfoReportFromXML.getTPMInfo(), tpmInfo);
        Assert.assertEquals(deviceInfoReport.getClientApplicationVersion(),
                EXPECTED_CLIENT_VERSION);
    }

    /**
     * Tests that a <code>DeviceInfoReport</code> can be saved in the
     * <code>ReportManager</code>.
     */
    @Test
    public final void testSaveReport() {
        final DeviceInfoReport deviceInfoReport = new DeviceInfoReport(
                networkInfo, osInfo, firmwareInfo, hardwareInfo, tpmInfo);
        final DeviceInfoReport savedReport = (DeviceInfoReport) reportManager
                .saveReport(deviceInfoReport);
        Assert.assertEquals(savedReport.getNetworkInfo(), networkInfo);
        Assert.assertEquals(savedReport.getOSInfo(), osInfo);
        Assert.assertEquals(savedReport.getFirmwareInfo(), firmwareInfo);
        Assert.assertEquals(savedReport.getHardwareInfo(), hardwareInfo);
        Assert.assertEquals(savedReport.getTPMInfo(), tpmInfo);
        Assert.assertEquals(deviceInfoReport.getClientApplicationVersion(),
                EXPECTED_CLIENT_VERSION);
    }

    /**
     * Tests that a <code>DeviceInfoReport</code> can be saved in the
     * <code>ReportManager</code> and then retrieved.
     */
    @Test
    public final void testGetReport() {
        final DeviceInfoReport deviceInfoReport =
                new DeviceInfoReport(networkInfo, osInfo, firmwareInfo, hardwareInfo, tpmInfo);
        final DeviceInfoReport savedReport =
                (DeviceInfoReport) reportManager.saveReport(deviceInfoReport);
        final DeviceInfoReport getReport =
                (DeviceInfoReport) reportManager.getReport(savedReport.getId());
        Assert.assertEquals(getReport.getNetworkInfo(), networkInfo);
        Assert.assertEquals(getReport.getOSInfo(), osInfo);
        Assert.assertEquals(getReport.getFirmwareInfo(), firmwareInfo);
        Assert.assertEquals(getReport.getHardwareInfo(), hardwareInfo);
        Assert.assertEquals(getReport.getTPMInfo(), tpmInfo);
        Assert.assertEquals(getReport.getClientApplicationVersion(),
                EXPECTED_CLIENT_VERSION);
    }

    /**
     * Tests that a <code>DeviceInfoReport</code> can be saved in the
     * <code>ReportManager</code> and then deleted.
     */
    @Test
    public final void testDeleteReport() {
        final DeviceInfoReport deviceInfoReport =
                new DeviceInfoReport(networkInfo, osInfo, firmwareInfo, hardwareInfo, tpmInfo);
        final DeviceInfoReport savedReport =
                (DeviceInfoReport) reportManager.saveReport(deviceInfoReport);
        Assert.assertNotNull(savedReport);
        final UUID id = savedReport.getId();
        Assert.assertNotNull(reportManager.getReport(id));
        Assert.assertTrue(reportManager.deleteReport(id));
        Assert.assertNull(reportManager.getReport(id));
    }

    /**
     * Creates a DeviceInfoReport instance usable for testing.
     *
     * @return a test DeviceInfoReport
     */
    public static DeviceInfoReport getTestReport() {
        return new DeviceInfoReport(
                createTestNetworkInfo(), createTestOSInfo(), createTestFirmwareInfo(),
                createTestHardwareInfo(), createTPMInfo()
        );
    }

    /**
     * Creates a test instance of NetworkInfo.
     *
     * @return network information for a fake device
     */
    public static NetworkInfo createTestNetworkInfo() {
        try {
            final String hostname = "test.hostname";
            final InetAddress ipAddress =
                    InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
            final byte[] macAddress = new byte[] {11, 22, 33, 44, 55, 66};
            return new NetworkInfo(hostname, ipAddress, macAddress);

        } catch (UnknownHostException e) {
            LOGGER.error("error occurred while creating InetAddress");
            return null;
        }

    }

    /**
     * Creates a test instance of OSInfo.
     *
     * @return OS information for a fake device
     */
    public static OSInfo createTestOSInfo() {
        return new OSInfo("test os name", "test os version", "test os arch",
                "test distribution", "test distribution release");
    }

    /**
     * Creates a test instance of FirmwareInfo.
     *
     * @return Firmware information for a fake device
     */
    public static FirmwareInfo createTestFirmwareInfo() {
        return new FirmwareInfo("test bios vendor", "test bios version", "test bios release date");
    }

    /**
     * Creates a test instance of HardwareInfo.
     *
     * @return Hardware information for a fake device
     */
    public static HardwareInfo createTestHardwareInfo() {
        return new HardwareInfo("test manufacturer", "test product name", "test version",
                "test really long serial number with many characters", "test really long chassis "
                + "serial number with many characters",
                "test really long baseboard serial number with many characters");
    }

    /**
     * Creates a test instance of TPMInfo.
     *
     * @return TPM information for a fake device
     */
    public static final TPMInfo createTPMInfo() {
        final short num1 = 1;
        final short num2 = 2;
        final short num3 = 3;
        final short num4 = 4;
        return new TPMInfo("test os make", num1, num2, num3, num4,
                getTestIdentityCertificate());
    }

    private static X509Certificate getTestIdentityCertificate() {
        X509Certificate certificateValue = null;
        InputStream istream = null;
        istream = DeviceInfoReportTest.class.getResourceAsStream(
                TEST_IDENTITY_CERT
        );
        try {
            if (istream == null) {
                throw new FileNotFoundException(TEST_IDENTITY_CERT);
            }
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            certificateValue = (X509Certificate) cf.generateCertificate(
                    istream);

        } catch (Exception e) {
            return null;
        } finally {
            if (istream != null) {
                try {
                    istream.close();
                } catch (IOException e) {
                    LOGGER.error("test certificate file could not be closed");
                }
            }
        }
        return certificateValue;
    }

    private String getXMLFromReport(final DeviceInfoReport deviceInfoReport)
            throws JAXBException {
        String xml = null;
        JAXBContext context = JAXBContext.newInstance(DeviceInfoReport.class);
        Marshaller marshaller = context.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(deviceInfoReport, writer);
        xml = writer.toString();
        xml = XMLCleaner.stripNonValidXMLCharacters(xml);
        return xml;
    }

    private DeviceInfoReport getReportFromXML(final String xml)
            throws JAXBException {
        DeviceInfoReport deviceInfoReport;
        JAXBContext context;
        context = JAXBContext.newInstance(DeviceInfoReport.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader reader = new StringReader(xml);
        deviceInfoReport = (DeviceInfoReport) unmarshaller.unmarshal(reader);
        return deviceInfoReport;
    }
}
