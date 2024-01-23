package hirs.attestationca.persist.entity.userdefined.report;

import hirs.attestationca.persist.entity.userdefined.info.OSInfo;
import hirs.attestationca.persist.entity.userdefined.info.TPMInfo;
import hirs.attestationca.persist.entity.userdefined.info.NetworkInfo;
import hirs.attestationca.persist.entity.userdefined.info.HardwareInfo;
import hirs.attestationca.persist.entity.userdefined.info.FirmwareInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * DeviceInfoReportTest is a unit test class for DeviceInfoReports.
 */
public class DeviceInfoReportTest {
    private final NetworkInfo networkInfo = createTestNetworkInfo();
    private final OSInfo osInfo = createTestOSInfo();
    private final FirmwareInfo firmwareInfo = createTestFirmwareInfo();
    private final HardwareInfo hardwareInfo = createTestHardwareInfo();
    private final TPMInfo tpmInfo = createTPMInfo();
    private static final String TEST_IDENTITY_CERT = "/tpm/sample_identity_cert.cer";

    private static final Logger LOGGER = LogManager.getLogger(DeviceInfoReportTest.class);

    private static final String EXPECTED_CLIENT_VERSION = "Test.Version";

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
    @Test
    public final void networkInfoNull() {
        assertThrows(NullPointerException.class, () ->
                new DeviceInfoReport(null, osInfo, firmwareInfo, hardwareInfo, tpmInfo));
    }

    /**
     * Tests that OSInfo cannot be null.
     */
    @Test
    public final void osInfoNull() {
        assertThrows(NullPointerException.class, () ->
                new DeviceInfoReport(networkInfo, null, firmwareInfo, hardwareInfo, tpmInfo));
    }

    /**
     * Tests that FirmwareInfo cannot be null.
     */
    @Test
    public final void firmwareInfoNull() {
        assertThrows(NullPointerException.class, () ->
                new DeviceInfoReport(networkInfo, osInfo, null, hardwareInfo, tpmInfo));
    }

    /**
     * Tests that HardwareInfo cannot be null.
     */
    @Test
    public final void hardwareInfoNull() {
        assertThrows(NullPointerException.class, () ->
                new DeviceInfoReport(networkInfo, osInfo, firmwareInfo, null, tpmInfo));
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
        assertEquals(networkInfo, deviceInfoReport.getNetworkInfo());
        assertEquals(osInfo, deviceInfoReport.getOSInfo());
        assertEquals(firmwareInfo, deviceInfoReport.getFirmwareInfo());
        assertEquals(hardwareInfo, deviceInfoReport.getHardwareInfo());
        assertEquals(tpmInfo, deviceInfoReport.getTpmInfo());
        assertEquals(EXPECTED_CLIENT_VERSION, deviceInfoReport.getClientApplicationVersion());
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
}
