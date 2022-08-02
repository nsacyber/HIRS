package hirs.attestationca.data.persist;

import hirs.data.persist.Device;
import hirs.data.persist.enums.HealthStatus;
import hirs.persist.ScheduledJobInfo;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static hirs.data.persist.DeviceGroup.DEFAULT_REPORT_DELAY_THRESHOLD;
import static hirs.data.persist.DeviceGroup.MINIMUM_THRESHOLD_INTERVAL_MS;

/**
 * Unit tests for <code>DeviceGroup</code>s.
 *
 */
public class DeviceGroupTest {

    /**
     * Tests instantiation of a <code>DeviceGroup</code> object.
     */
    @Test
    public final void testDeviceGroup() {
        new DeviceGroup("TestDeviceGroup", "TestDeviceGroupDescription");
    }

    /**
     * Tests that a NullPointerException is thrown with a null name.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void testDeviceGroupNullName() {
        new DeviceGroup(null);
    }

    /**
     * Tests getName() returns the correct name.
     */
    @Test
    public final void testGetName() {
        final String name = "TestDeviceGroup";
        DeviceGroup deviceGroup = new DeviceGroup(name);
        Assert.assertEquals(deviceGroup.getName(), name);
    }

    /**
     * Tests getDescription() returns the correct description.
     */
    @Test
    public final void testGetDescription() {
        final String name = "TestDeviceGroup";
        final String description = "TestDescription";
        DeviceGroup deviceGroup = new DeviceGroup(name, description);
        Assert.assertEquals(deviceGroup.getDescription(), description);
    }

    /**
     * Tests getDescription() returns empty if there is no description.
     */
    @Test
    public final void testGetDescriptionNull() {
        final String name = "TestDeviceGroup";
        DeviceGroup deviceGroup = new DeviceGroup(name);
        Assert.assertEquals("", deviceGroup.getDescription());
    }

    /**
     * Tests that the name of a device group can be set.
     */
    @Test
    public final void testSetName() {
        final String name = "TestDeviceGroup";
        DeviceGroup deviceGroup = new DeviceGroup(name);
        Assert.assertEquals(deviceGroup.getName(), name);

        final String newName = "TestNewName";
        deviceGroup.setName(newName);
        Assert.assertEquals(deviceGroup.getName(), newName);
    }

    /**
     * Tests that the description of a device group can be set.
     */
    @Test
    public final void testSetDescription() {
        final String name = "TestDeviceGroup";
        final String description = "TestDescription";
        DeviceGroup deviceGroup = new DeviceGroup(name, description);
        Assert.assertEquals(deviceGroup.getDescription(), description);

        final String newDescription = "TestNewDescription";
        deviceGroup.setDescription(newDescription);
        Assert.assertEquals(deviceGroup.getDescription(), newDescription);
    }

    /**
     * Tests that the description of a device group can be set to null.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void testSetDescriptionNull() {
        final String name = "TestDeviceGroup";
        DeviceGroup deviceGroup = new DeviceGroup(name);
        deviceGroup.setDescription(null);
    }

    /**
     * Tests that a name cannot be set to null.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void setNameNull() {
        final String name = "TestDeviceGroup";
        DeviceGroup deviceGroup = new DeviceGroup(name);
        deviceGroup.setName(null);
    }

    /**
     * Tests a <code>DeviceGroup</code> with no description.
     */
    @Test
    public final void testDeviceGroupNullDescription() {
        new DeviceGroup("TestDeviceGroup");
    }


    /**
     * Tests that the ScheduleJobInfo field can be get and set, and has a valid default value.
     */
    @Test
    public final void scheduleJobInfoProperty() {
        DeviceGroup group = new DeviceGroup("periodic_group");
        Assert.assertNotNull(group.getScheduledJobInfo());

        final long frequencyMs = 777;
        ScheduledJobInfo jobInfo = new ScheduledJobInfo(frequencyMs);
        group.setScheduledJobInfo(jobInfo);

        Assert.assertSame(group.getScheduledJobInfo(), jobInfo);
    }
    /**
     * Tests is and set <code>enablePeriodicReportDelayAlert</code> of a <code>DeviceGroup</code>.
     */
    @Test
    public final void testEnablePeriodicReportDelayAlert() {
        DeviceGroup deviceGroup = new DeviceGroup("TestDeviceGroup");
        Assert.assertFalse(deviceGroup.isEnablePeriodicReportDelayAlert(),
                "Enable Periodic Report Delay Alert was not false after constructor.");

        deviceGroup.setEnablePeriodicReportDelayAlert(true);
        Assert.assertTrue(deviceGroup.isEnablePeriodicReportDelayAlert(),
                "Enable Periodic Report Delay Alert did not match after being set.");

        deviceGroup.setEnablePeriodicReportDelayAlert(false);
        Assert.assertFalse(deviceGroup.isEnablePeriodicReportDelayAlert(),
                "Enable Periodic Report Delay Alert did not match after being set.");

    }

    private void testMinPeriodicReportDelayThreshold(final DeviceGroup group, final long interval) {
        try {
            group.setPeriodicReportDelayThreshold(interval);
            Assert.fail("Setting Periodic Report Delay Threshold below minimum did not throw"
                    + " exception.");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("Periodic Report Delay Threshold must be greater than or equal to "
                    + MINIMUM_THRESHOLD_INTERVAL_MS + " milliseconds. Received " + interval,
                    ex.getMessage(),
                    "Periodic Report Delay Threshold interval error message was incorrect.");
        }
    }

    /**
     * Tests get and set <code>periodicReportInterval</code> of a <code>DeviceGroup</code>.
     */
    @Test
    public final void testPeriodicReportDelayThreshold() {
        DeviceGroup deviceGroup = new DeviceGroup("TestDeviceGroup");
        Assert.assertEquals(deviceGroup.getPeriodicReportDelayThreshold(),
                DEFAULT_REPORT_DELAY_THRESHOLD, "Periodic Report Delay Threshold was not "
                + DEFAULT_REPORT_DELAY_THRESHOLD + " after constructor.");

        testMinPeriodicReportDelayThreshold(deviceGroup, 0);
        testMinPeriodicReportDelayThreshold(deviceGroup, MINIMUM_THRESHOLD_INTERVAL_MS - 1);

        deviceGroup.setPeriodicReportDelayThreshold(MINIMUM_THRESHOLD_INTERVAL_MS);
        Assert.assertEquals(MINIMUM_THRESHOLD_INTERVAL_MS,
                deviceGroup.getPeriodicReportDelayThreshold(),
                "Periodic Report Delay Threshold did not match after being set.");

    }

    /**
     * Tests that a <code>Device</code> can be added to a device group.
     *
     * @throws Exception if error occurs while creating test Device
     */
    @Test
    public final void testAddDevice() throws Exception {
        Device device = DeviceTest.getTestDevice("TestDevice");
        DeviceGroup deviceGroup = new DeviceGroup("TestDeviceGroup");
        deviceGroup.addDevice(device);
        Set<Device> devices = deviceGroup.getDevices();
        Assert.assertNotNull(devices);
        Assert.assertEquals(devices.size(), 1);
        Assert.assertEquals(deviceGroup, device.getDeviceGroup());
        Assert.assertEquals(devices.iterator().next(), device);
    }

    /**
     * Tests that a <code>NullPointerException</code> is thrown if a null device is added.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void testAddDeviceNull() {
        DeviceGroup deviceGroup = new DeviceGroup("TestDeviceGroup");
        deviceGroup.addDevice(null);
    }

    /**
     * Tests that the device group will ignore a duplicate device that is added.
     *
     * @throws Exception if error occurs while creating test Device
     */
    @Test
    public final void testAddDuplicateDevice() throws Exception {
        Device device = DeviceTest.getTestDevice("TestDevice");
        DeviceGroup deviceGroup = new DeviceGroup("TestDeviceGroup");
        deviceGroup.addDevice(device);
        deviceGroup.addDevice(device);
        Set<Device> devices = deviceGroup.getDevices();
        Assert.assertNotNull(devices);
        Assert.assertEquals(devices.size(), 1);
    }

    /**
     * Tests that a device can be successfully removed.
     *
     * @throws Exception if error occurs while creating test Device
     */
    @Test
    public final void testRemoveDevice() throws Exception {
        Device device = DeviceTest.getTestDevice("TestDevice");
        Device device2 = DeviceTest.getTestDevice("TestDevice2");
        DeviceGroup deviceGroup = new DeviceGroup("TestDeviceGroup");
        deviceGroup.addDevice(device);
        deviceGroup.addDevice(device2);
        Set<Device> devices = deviceGroup.getDevices();
        Assert.assertEquals(devices.size(), 2);

        Assert.assertTrue(deviceGroup.removeDevice(device));
        Assert.assertEquals(devices.size(), 1);
        Set<Device> expectedSet = new HashSet<>(Arrays.asList(device2));
        Assert.assertEquals(devices, expectedSet);
        Assert.assertEquals(device.getDeviceGroup(), null);
        Assert.assertEquals(device2.getDeviceGroup(), deviceGroup);
    }

    /**
     * Tests that a device can be successfully removed using the name of the device.
     *
     * @throws Exception if error occurs while creating test Device
     */
    @Test
    public final void testRemoveDeviceByName() throws Exception {
        String testDeviceName = "TestDevice";
        Device device = DeviceTest.getTestDevice(testDeviceName);
        Device device2 = DeviceTest.getTestDevice("TestDevice2");
        DeviceGroup deviceGroup = new DeviceGroup("TestDeviceGroup");
        deviceGroup.addDevice(device);
        deviceGroup.addDevice(device2);
        Set<Device> devices = deviceGroup.getDevices();
        Assert.assertEquals(devices.size(), 2);

        Assert.assertTrue(deviceGroup.removeDevice(testDeviceName));
        Assert.assertEquals(devices.size(), 1);
        Set<Device> expectedSet = new HashSet<>(Arrays.asList(device2));
        Assert.assertEquals(devices, expectedSet);
    }

    /**
     * Tests that removeDevice() returns false and doesn't delete anything if passed null as a
     * parameter (cast as a Device).
     *
     * @throws Exception if error occurs while creating test Device
     */
    @Test
    public final void testRemoveDeviceNull() throws Exception {
        Device device = DeviceTest.getTestDevice("TestDevice");
        DeviceGroup deviceGroup = new DeviceGroup("TestDeviceGroup");
        deviceGroup.addDevice(device);
        Assert.assertEquals(deviceGroup.getDevices().size(), 1);

        Assert.assertFalse(deviceGroup.removeDevice((Device) null));
    }

    /**
     * Tests that removeDevice() returns false and doesn't delete anything if an unknown device is
     * requested to be removed.
     *
     * @throws Exception if error occurs while creating test Device
     */
    @Test
    public final void testRemoveDeviceUnknown() throws Exception {
        Device device = DeviceTest.getTestDevice("TestDevice");
        Device device2 = DeviceTest.getTestDevice("TestDevice2");
        DeviceGroup deviceGroup = new DeviceGroup("TestDeviceGroup");
        deviceGroup.addDevice(device);
        Set<Device> expectedSet = new HashSet<>(Arrays.asList(device));
        Set<Device> devices = deviceGroup.getDevices();
        Assert.assertEquals(devices, expectedSet);
        Assert.assertEquals(device.getDeviceGroup(), deviceGroup);
        Assert.assertEquals(devices.iterator().next(), device);

        Assert.assertFalse(deviceGroup.removeDevice(device2));
        Assert.assertEquals(devices, expectedSet);
        Assert.assertEquals(device.getDeviceGroup(), deviceGroup);
        Assert.assertEquals(devices.iterator().next(), device);
        Assert.assertNull(device2.getDeviceGroup());
    }

    /**
     * Tests that removeDevice() returns false and doesn't delete anything if passed null as a
     * parameter (cast a String).
     *
     * @throws Exception if error occurs while creating test Device
     */
    @Test
    public final void testRemoveDeviceNullString() throws Exception {
        Device device = DeviceTest.getTestDevice("TestDevice");
        DeviceGroup deviceGroup = new DeviceGroup("TestDeviceGroup");
        deviceGroup.addDevice(device);
        Assert.assertEquals(deviceGroup.getDevices().size(), 1);

        Assert.assertFalse(deviceGroup.removeDevice((String) null));
    }

    /**
     * Tests that false if returned if the device is attempted to be removed twice.
     *
     * @throws Exception if error occurs while creating test Device
     */
    @Test
    public final void testRemoveDeviceTwice() throws Exception {
        Device device = DeviceTest.getTestDevice("TestDevice");
        Device device2 = DeviceTest.getTestDevice("TestDevice2");
        DeviceGroup deviceGroup = new DeviceGroup("TestDeviceGroup");
        deviceGroup.addDevice(device);
        deviceGroup.addDevice(device2);
        Set<Device> devices = deviceGroup.getDevices();
        Assert.assertEquals(devices.size(), 2);

        Assert.assertTrue(deviceGroup.removeDevice(device));
        Assert.assertFalse(deviceGroup.removeDevice(device));

        Assert.assertEquals(devices.size(), 1);
        Set<Device> expectedSet = new HashSet<>(Arrays.asList(device2));
        Assert.assertEquals(devices, expectedSet);
    }

    /**
     * Verifies that the enableWaitForAppraisalCompletion property can be set and get correctly.
     * Also verifies default value.
     *
     * @throws Exception if error occurs while creating test Device
     */
    @Test
    public void testEnableWaitForAppraisalCompletionProperty() throws Exception {
        Device device = DeviceTest.getTestDevice("TestDevice");
        DeviceGroup deviceGroup = new DeviceGroup("TestDeviceGroup");
        deviceGroup.addDevice(device);

        Assert.assertFalse(deviceGroup.isWaitForAppraisalCompletionEnabled());

        deviceGroup.setWaitForAppraisalCompletionEnabled(true);
        Assert.assertTrue(deviceGroup.isWaitForAppraisalCompletionEnabled());

        deviceGroup.setWaitForAppraisalCompletionEnabled(false);
        Assert.assertFalse(deviceGroup.isWaitForAppraisalCompletionEnabled());
    }

    /**
     * Tests that the group health is unknown if there are no devices.
     */
    @Test
    public void groupHealthWithNoDevices() {
        DeviceGroup deviceGroup = new DeviceGroup("TestDeviceGroup");
        Assert.assertEquals(deviceGroup.getHealthStatus(), HealthStatus.UNKNOWN);
    }

    /**
     * Tests that the group health is trusted if all devices are trusted.
     *
     * @throws Exception if error occurs while creating test Device
     */
    @Test
    public void groupHealthTrusted() throws Exception {
        DeviceGroup deviceGroup = new DeviceGroup("TestDeviceGroup");
        Device device1 = DeviceTest.getTestDevice("d1");
        device1.setHealthStatus(HealthStatus.TRUSTED);
        Device device2 = DeviceTest.getTestDevice("d2");
        device2.setHealthStatus(HealthStatus.TRUSTED);
        Device device3 = DeviceTest.getTestDevice("d3");
        device3.setHealthStatus(HealthStatus.TRUSTED);

        deviceGroup.addDevice(device1);
        deviceGroup.addDevice(device2);
        deviceGroup.addDevice(device3);

        Assert.assertEquals(deviceGroup.getHealthStatus(), HealthStatus.TRUSTED);
    }

    /**
     * Tests that the group health is untrusted if a device is untrusted and the remaining devices
     * are trusted.
     *
     * @throws Exception if error occurs while creating test Device
     */
    @Test
    public void groupHealthUntrustedNoUnknowns() throws Exception {
        DeviceGroup deviceGroup = new DeviceGroup("TestDeviceGroup");
        Device device1 = DeviceTest.getTestDevice("d1");
        device1.setHealthStatus(HealthStatus.TRUSTED);
        Device device2 = DeviceTest.getTestDevice("d2");
        device2.setHealthStatus(HealthStatus.UNTRUSTED);
        Device device3 = DeviceTest.getTestDevice("d3");
        device3.setHealthStatus(HealthStatus.TRUSTED);

        deviceGroup.addDevice(device1);
        deviceGroup.addDevice(device2);
        deviceGroup.addDevice(device3);

        Assert.assertEquals(deviceGroup.getHealthStatus(), HealthStatus.UNTRUSTED);
    }

    /**
     * Tests that the group health is untrusted if a device is untrusted and another device has
     * unknown trust.
     *
     * @throws Exception if error occurs while creating test Device
     */
    @Test
    public void groupHealthUntrustedSomeUnknowns() throws Exception {
        DeviceGroup deviceGroup = new DeviceGroup("TestDeviceGroup");
        Device device1 = DeviceTest.getTestDevice("d1");
        device1.setHealthStatus(HealthStatus.TRUSTED);
        Device device2 = DeviceTest.getTestDevice("d2");
        device2.setHealthStatus(HealthStatus.UNTRUSTED);
        Device device3 = DeviceTest.getTestDevice("d3");
        device3.setHealthStatus(HealthStatus.UNKNOWN);

        deviceGroup.addDevice(device1);
        deviceGroup.addDevice(device2);
        deviceGroup.addDevice(device3);

        Assert.assertEquals(deviceGroup.getHealthStatus(), HealthStatus.UNTRUSTED);
    }

    /**
     * Tests that the group health is unknown if a device has unknown trust and no devices are
     * untrusted.
     *
     * @throws Exception if error occurs while creating test Device
     */
    @Test
    public void groupHealthUntrusted() throws Exception {
        DeviceGroup deviceGroup = new DeviceGroup("TestDeviceGroup");
        Device device1 = DeviceTest.getTestDevice("d1");
        device1.setHealthStatus(HealthStatus.TRUSTED);
        Device device2 = DeviceTest.getTestDevice("d2");
        device2.setHealthStatus(HealthStatus.TRUSTED);
        Device device3 = DeviceTest.getTestDevice("d3");
        device3.setHealthStatus(HealthStatus.UNKNOWN);

        deviceGroup.addDevice(device1);
        deviceGroup.addDevice(device2);
        deviceGroup.addDevice(device3);

        Assert.assertEquals(deviceGroup.getHealthStatus(), HealthStatus.UNKNOWN);
    }

    /**
     * Tests that getNumberOfDevices returns an accurate count.
     * @throws Exception if DeviceTest.getTestDevice has a problem.
     */
    @Test
    public void testGetNumberOfDevices() throws Exception {
        final int numDevices = 3;
        DeviceGroup deviceGroup = new DeviceGroup("TestDeviceGroup");
        Device device1 = DeviceTest.getTestDevice("d1");
        Device device2 = DeviceTest.getTestDevice("d2");
        Device device3 = DeviceTest.getTestDevice("d3");

        deviceGroup.addDevice(device1);
        deviceGroup.addDevice(device2);
        deviceGroup.addDevice(device3);

        Assert.assertEquals(deviceGroup.getNumberOfDevices(), numDevices);
    }

    /**
     * Tests that getNumberOfDevices returns an accurate count.
     * @throws Exception if DeviceTest.getTestDevice has a problem.
     */
    @Test
    public void testGetNumberOfTrustedDevices() throws Exception {
        final int numTrustedDevices = 2;
        DeviceGroup deviceGroup = new DeviceGroup("TestDeviceGroup");
        Device device1 = DeviceTest.getTestDevice("d1");
        device1.setHealthStatus(HealthStatus.TRUSTED);
        Device device2 = DeviceTest.getTestDevice("d1");
        device2.setHealthStatus(HealthStatus.UNTRUSTED);
        Device device3 = DeviceTest.getTestDevice("d2");
        device3.setHealthStatus(HealthStatus.TRUSTED);
        Device device4 = DeviceTest.getTestDevice("d3");
        device4.setHealthStatus(HealthStatus.UNKNOWN);

        deviceGroup.addDevice(device1);
        deviceGroup.addDevice(device2);
        deviceGroup.addDevice(device3);
        deviceGroup.addDevice(device4);

        Assert.assertEquals(deviceGroup.getNumberOfTrustedDevices(), numTrustedDevices);
    }

    /**
     * Tests that getAllDevices do not return the device group.
     *
     * @throws Exception if error occurs while creating test Device
     */
    @Test
    public final void testGetAllDevices() throws Exception {
        Device device = DeviceTest.getTestDevice("TestDevice");
        Device device2 = DeviceTest.getTestDevice("TestDevice2");
        DeviceGroup deviceGroup = new DeviceGroup("TestDeviceGroup");
        deviceGroup.addDevice(device);
        deviceGroup.addDevice(device2);
        Set<Device> devices = deviceGroup.getAllDevices();
        Assert.assertEquals(devices.size(), 2);
        Assert.assertNull(devices.iterator().next().getDeviceGroup());

    }
}
