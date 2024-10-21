package hirs.attestationca.persist.entity.userdefined.report;

import hirs.attestationca.persist.entity.userdefined.AbstractUserdefinedEntityTest;
import hirs.attestationca.persist.entity.userdefined.info.FirmwareInfo;
import hirs.attestationca.persist.entity.userdefined.info.HardwareInfo;
import hirs.attestationca.persist.entity.userdefined.info.NetworkInfo;
import hirs.attestationca.persist.entity.userdefined.info.OSInfo;
import hirs.attestationca.persist.entity.userdefined.info.TPMInfo;
import hirs.utils.VersionHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test class for DeviceInfoReports.
 */
public class DeviceInfoReportTest extends AbstractUserdefinedEntityTest {
    private static final String EXPECTED_CLIENT_VERSION = VersionHelper.getVersion();
    private final NetworkInfo networkInfo = createTestNetworkInfo();
    private final OSInfo osInfo = createTestOSInfo();
    private final FirmwareInfo firmwareInfo = createTestFirmwareInfo();
    private final HardwareInfo hardwareInfo = createTestHardwareInfo();
    private final TPMInfo tpmInfo = createTPMInfo();

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
}
