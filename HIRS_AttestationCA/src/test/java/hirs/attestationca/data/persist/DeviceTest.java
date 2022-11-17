package hirs.attestationca.data.persist;

import hirs.data.persist.AppraisalStatus;
import hirs.attestationca.entity.Device;
import hirs.attestationca.entity.DeviceInfoReport;
import hirs.data.persist.enums.HealthStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

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
     * @throws Exception in case there are errors getting a report
     *
     * @return device
     */
    public static Device getTestDevice(final String name) throws Exception {
        final DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        return new Device(name, deviceInfo);
    }

    /**
     * Tests that the device constructor can take a name.
     */
    @Test
    public void testDevice() {
        final String name = "my-laptop";
        final Device device = new Device(name);
        Assert.assertNotNull(device);
    }

    /**
     * Tests that the name cannot be null to the constructor.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testDeviceNullName() {
        final String name = null;
        new Device(name);
    }

    /**
     * Tests that a name and device info report can be passed into the
     * constructor.
     *
     * @throws Exception
     *             in case there are errors getting a report
     *
     */
    @Test
    public void testDeviceNameAndInfo() throws Exception {
        final String name = "my-laptop";
        final DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        new Device(name, deviceInfo);
    }

    /**
     * Tests that the device name can be supplied and device info be null.
     */
    @Test
    public void testDeviceNameAndNullInfo() {
        final String name = "my-laptop";
        final DeviceInfoReport deviceInfo = null;
        new Device(name, deviceInfo);
    }

    /**
     * Tests that name cannot be null to name and device info constructor.
     *
     * @throws Exception
     *             in case there are errors getting a report
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testDeviceNullNameAndInfo() throws Exception {
        final String name = null;
        final DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        new Device(name, deviceInfo);
    }

    /**
     * Tests that name and device info cannot be null to name and device info
     * constructor.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testDeviceNullNameAndNullInfo() {
        final String name = null;
        final DeviceInfoReport deviceInfo = null;
        new Device(name, deviceInfo);
    }

    /**
     * Tests that get device info report returns the device info report.
     *
     * @throws Exception
     *             in case there are errors getting a report
     */
    @Test
    public void testGetDeviceInfo() throws Exception {
        final String name = "my-laptop";
        final DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        final Device device = new Device(name, deviceInfo);
        Assert.assertEquals(device.getDeviceInfo(), deviceInfo);
    }

    /**
     * Tests that device info can be set.
     *
     * @throws Exception
     *             in case there are errors getting a report
     */
    @Test
    public void testSetDeviceInfo() throws Exception {
        final String name = "my-laptop";
        final Device device = new Device(name, null);
        Assert.assertNull(device.getDeviceInfo());
        final DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        device.setDeviceInfo(deviceInfo);
        Assert.assertEquals(device.getDeviceInfo(), deviceInfo);
    }

    /**
     * Tests that get device info report returns the device info report.
     *
     * @throws Exception
     *             in case there are errors getting a report
     */
    @Test
    public void testSetNullDeviceInfo() throws Exception {
        final String name = "my-laptop";
        final DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        final Device device = new Device(name, deviceInfo);
        Assert.assertEquals(device.getDeviceInfo(), deviceInfo);
        device.setDeviceInfo(null);
        Assert.assertNull(device.getDeviceInfo());
    }

    /**
     * Tests that retrieving a null LastReportTimestamp will not trigger an exception.
     *
     * @throws Exception
     *      In case there is an error getting a report
     */
    @Test
    public void testNullLastReportTimeStamp() throws Exception {
        final String name = "my-laptop";
        final DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        final Device device = new Device(name, deviceInfo);
        Assert.assertNull(device.getLastReportTimestamp());
        //Successful if test does not throw Exception
    }

    /**
     * Tests that the default constructor returns a device with non-overridden device state.
     */
    @Test
    public void testOverrideStateDefault() {
        final Device device = new Device("test-device");
        Assert.assertFalse(device.isStateOverridden());
    }

    /**
     * Tests that a device can have its state overridden with a user-defined reason, then reset to
     * a non-overridden state.
     */
    @Test
    public void testOverrideStateReason() {
        final Device device = new Device("test-device");
        final String reason = "user-defined reason";
        device.overrideState(reason);
        Assert.assertTrue(device.isStateOverridden());
        Assert.assertEquals(device.getOverrideReason(), reason);
        device.clearOverride();
        Assert.assertFalse(device.isStateOverridden());
        Assert.assertTrue(device.getOverrideReason().contains("Device state override cleared"));
    }

    /**
     * Tests that attempting to set the health status to null will result in a NPE.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testSetHealthStatusNull() {
        final Device device  = new Device("test-device");
        device.setHealthStatus(null);
    }

    /**
     * Tests that setting the health status works correctly.
     */
    @Test
    public void testSetHealthStatus() {
        final Device device  = new Device("test-device");
        device.setHealthStatus(HealthStatus.TRUSTED);
        Assert.assertEquals(HealthStatus.TRUSTED, device.getHealthStatus());
    }

    /**
     * Tests that getting the health status works correctly.
     */
    @Test
    public void testGetHealthStatus() {
        final Device device = new Device("test-device");
        Assert.assertEquals(HealthStatus.UNKNOWN, device.getHealthStatus());
    }


    /**
     * Tests the hash codes are the same for two devices that have the same
     * names.
     *
     * @throws Exception
     *             in case there are errors getting a report
     */
    @Test
    public void testDeviceHashCode() throws Exception {
        final String name = "my-laptop";
        final String otherName = "my-laptop";
        final DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        final Device device = new Device(name, deviceInfo);
        final Device other = new Device(otherName, deviceInfo);
        Assert.assertTrue(device.hashCode() == other.hashCode());
    }

    /**
     * Tests the hash codes are different for two devices that have different
     * names.
     *
     * @throws Exception
     *             in case there are errors getting a report
     */
    @Test
    public void testDeviceHashCodeNotEqual() throws Exception {
        final String name = "my-laptop";
        final String otherName = "my-other-laptop";
        final DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        final Device device = new Device(name, deviceInfo);
        final Device other = new Device(otherName, deviceInfo);
        Assert.assertFalse(device.hashCode() == other.hashCode());
    }

    /**
     * Tests equals returns true for two devices that have the same name.
     *
     * @throws Exception
     *             in case there are errors getting a report
     */
    @Test
    public void testDeviceEquals() throws Exception {
        final String name = "my-laptop";
        final String otherName = "my-laptop";
        final DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        final Device device = new Device(name, deviceInfo);
        final Device other = new Device(otherName, deviceInfo);
        Assert.assertTrue(device.equals(other));
        Assert.assertTrue(other.equals(device));
    }

    /**
     * Tests that equals returns false for two devices that have different
     * names.
     *
     * @throws Exception
     *             in case there are errors getting a report
     */
    @Test
    public void testDeviceEqualsNotEqual() throws Exception {
        final String name = "my-laptop";
        final String otherName = "my-other-laptop";
        final DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        final Device device = new Device(name, deviceInfo);
        final Device other = new Device(otherName, deviceInfo);
        Assert.assertFalse(device.equals(other));
        Assert.assertFalse(other.equals(device));
    }

    /**
     * Tests that the XML is generated correctly.
     *
     * @throws Exception
     *             in case there are errors marshalling or getting report
     */
    @Test
    public void marshalUnmarshalTest() throws Exception {
        final String name = "my-laptop";
        final DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        final Device device = new Device(name, deviceInfo);
        final String xml = device.toXML();
        final Device deviceFromXML = Device.getInstance(xml);
        Assert.assertEquals(deviceFromXML, device);
    }

    /**
     * Tests that the default setting of the supply chain validation status is unknown.
     */
    @Test
    public void testGetDefaultSupplyChainStatus() {
        String name = "my-laptop";
        DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        Device dev = new Device(name, deviceInfo);
        Assert.assertEquals(AppraisalStatus.Status.UNKNOWN, dev.getSupplyChainStatus());
    }

    /**
     * Tests that the supply chain validation status getters and setters work.
     */
    @Test
    public void testSetAndGetSupplyChainStatus() {
        String name = "my-laptop";
        DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        Device dev = new Device(name, deviceInfo);
        dev.setSupplyChainStatus(AppraisalStatus.Status.PASS);
        Assert.assertEquals(AppraisalStatus.Status.PASS, dev.getSupplyChainStatus());
    }

    /**
     * Tests that the supply chain validation status setters won't accept a null value.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testSetNullSupplyChainStatus() {
        String name = "my-laptop";
        DeviceInfoReport deviceInfo = DeviceInfoReportTest.getTestReport();
        Device dev = new Device(name, deviceInfo);
        dev.setSupplyChainStatus(null);
    }
}
