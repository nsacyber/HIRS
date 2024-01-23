package hirs.attestationca.persist.entity.userdefined;

import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReportTest;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.attestationca.persist.enums.HealthStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * This is the test class for the <code>Device</code> class.
 *
 */
public final class DeviceTest {
    /**
     * Utility method for getting a <code>Device</code> that can be used for
     * testing.
     *
     * @param name name for the <code>Device</code>
     *
     * @return device
     */
    public static Device getTestDevice(final String name) {
        final DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        return new Device(name, deviceInfo, HealthStatus.UNKNOWN, AppraisalStatus.Status.UNKNOWN, null, false, null, null);
    }

    /**
     * Tests that the device constructor can take a name.
     */
    @Test
    public void testDevice() {
        final String name = "my-laptop";
        final Device device = new Device(name, null, HealthStatus.UNKNOWN, AppraisalStatus.Status.UNKNOWN, null, false, null , null);
        assertNotNull(device);
    }

    /**
     * Tests that a name and device info report can be passed into the
     * constructor.
     */
    @Test
    public void testDeviceNameAndInfo() {
        final String name = "my-laptop";
        final DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        new Device(name, deviceInfo, HealthStatus.UNKNOWN, AppraisalStatus.Status.UNKNOWN, null, false, null, null);
    }

    /**
     * Tests that the device name can be supplied and device info be null.
     */
    @Test
    public void testDeviceNameAndNullInfo() {
        final String name = "my-laptop";
        final DeviceInfoReport deviceInfo = null;
        new Device(name, deviceInfo, HealthStatus.UNKNOWN, AppraisalStatus.Status.UNKNOWN, null, false, null, null);
    }

    /**
     * Tests that get device info report returns the device info report.
     */
    @Test
    public void testGetDeviceInfo() {
        final String name = "my-laptop";
        final DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        final Device device = new Device(name, deviceInfo, HealthStatus.UNKNOWN, AppraisalStatus.Status.UNKNOWN, null, false, null, null);
        assertEquals(deviceInfo, device.getDeviceInfo());
    }

    /**
     * Tests that device info can be set.
     */
    @Test
    public void testSetDeviceInfo() {
        final String name = "my-laptop";
        final Device device = new Device(name, null, HealthStatus.UNKNOWN, AppraisalStatus.Status.UNKNOWN, null, false, null, null);
        assertNull(device.getDeviceInfo());
        final DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        device.setDeviceInfo(deviceInfo);
        assertEquals(deviceInfo, device.getDeviceInfo());
    }

    /**
     * Tests that get device info report returns the device info report.
     */
    @Test
    public void testSetNullDeviceInfo() {
        final String name = "my-laptop";
        final DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        final Device device = new Device(name, deviceInfo, HealthStatus.UNKNOWN, AppraisalStatus.Status.UNKNOWN, null, false, null, null);
        assertEquals(deviceInfo, device.getDeviceInfo());
        device.setDeviceInfo(null);
        assertNull(device.getDeviceInfo());
    }

    /**
     * Tests that retrieving a null LastReportTimestamp will not trigger an exception.
     */
    @Test
    public void testNotNullLastReportTimeStamp() {
        final String name = "my-laptop";
        final DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        final Device device = new Device(name, deviceInfo, HealthStatus.UNKNOWN, AppraisalStatus.Status.UNKNOWN, null, false, null, null);
        assertNotNull(device.getLastReportTimestamp());
    }

    /**
     * Tests that setting and getting the health status works correctly.
     */
    @Test
    public void testSetHealthStatus() {
        final Device device  = new Device("test-device", null, HealthStatus.UNKNOWN, AppraisalStatus.Status.UNKNOWN, null, false, null, null);
        device.setHealthStatus(HealthStatus.TRUSTED);
        assertEquals(HealthStatus.TRUSTED, device.getHealthStatus());
    }

    /**
     * Tests equals returns true for two devices that have the same name.
     */
    @Test
    public void testDeviceEquals() {
        final String name = "my-laptop";
        final String otherName = "my-laptop";
        final DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        final Device device = new Device(name, deviceInfo, HealthStatus.UNKNOWN, AppraisalStatus.Status.UNKNOWN, null, false, null, null);
        final Device other = new Device(otherName, deviceInfo, HealthStatus.UNKNOWN, AppraisalStatus.Status.UNKNOWN, null, false, null, null);
        assertEquals(device, other);
    }

    /**
     * Tests that the default setting of the supply chain validation status is unknown.
     */
    @Test
    public void testGetDefaultSupplyChainStatus() {
        String name = "my-laptop";
        DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        final Device device = new Device(name, deviceInfo, HealthStatus.UNKNOWN, AppraisalStatus.Status.UNKNOWN, null, false, null, null);
        assertEquals(AppraisalStatus.Status.UNKNOWN, device.getSupplyChainValidationStatus());
    }

    /**
     * Tests that the supply chain validation status getters and setters work.
     */
    @Test
    public void testSetAndGetSupplyChainStatus() {
        String name = "my-laptop";
        DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        final Device device = new Device(name, deviceInfo, HealthStatus.UNKNOWN, AppraisalStatus.Status.UNKNOWN, null, false, null, null);
        device.setSupplyChainValidationStatus(AppraisalStatus.Status.PASS);
        assertEquals(AppraisalStatus.Status.PASS, device.getSupplyChainValidationStatus());
    }
}
