package hirs.persist;

import hirs.appraiser.Appraiser;

import hirs.data.persist.Device;
import hirs.data.persist.DeviceGroup;
import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.Policy;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import hirs.data.persist.TestPolicy;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import hirs.appraiser.TestAppraiser;

import hirs.data.persist.DeviceTest;

/**
 * Unit tests for the <code>DBDeviceGroupManager</code> class.
 */
public final class DBDeviceGroupManagerTest extends SpringPersistenceTest {
    private static final Logger LOGGER = LogManager.getLogger(DBDeviceGroupManagerTest.class);

    private final String deviceGroupName = "Test Device Group";
    private final String deviceName = "Test Device";

    /**
     * Default constructor that does nothing.
     */
    public DBDeviceGroupManagerTest() {
        /* do nothing */
    }

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an
     * in-memory database that is used for testing.
     */
    @BeforeClass
    public void setup() {
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterClass
    public void tearDown() {
    }

    /**
     * Resets the test state to a known good state. This currently only resets
     * the database by removing all <code>Device</code> and
     * <code>DeviceInfoReport</code> objects.
     */
    @AfterMethod
    public void resetTestState() {
        DBUtility.removeAllInstances(sessionFactory, Device.class);
        DBUtility.removeAllInstances(sessionFactory, DeviceInfoReport.class);
        DBUtility.removeAllInstances(sessionFactory, PolicyMapper.class);
        DBUtility.removeAllInstances(sessionFactory, Appraiser.class);
        DBUtility.removeAllInstances(sessionFactory, Policy.class);
        DBUtility.removeAllInstances(sessionFactory, DeviceGroup.class);
    }

    /**
     * Tests that the <code>DBDeviceGroupManager</code> can save a
     * <code>DeviceGroup</code>.
     */
    @Test
    public void testSave() {
        LOGGER.debug("testSave test started");
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceGroup.class), 0);
        final DeviceGroup deviceGroup = new DeviceGroup(deviceGroupName);
        final DeviceGroupManager mgr = new DBDeviceGroupManager(sessionFactory);
        final DeviceGroup savedDeviceGroup = mgr.saveDeviceGroup(deviceGroup);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceGroup.class), 1);
        Assert.assertEquals(savedDeviceGroup, deviceGroup);
        Assert.assertTrue(
                DBUtility.isInDatabase(sessionFactory, DeviceGroup.class, deviceGroupName)
        );
        final UUID deviceGroupID = savedDeviceGroup.getId();
        Assert.assertNotNull(deviceGroupID);
    }

    /**
     * Tests that the <code>DBDeviceGroupManager</code> can save a
     * <code>DeviceGroup</code>.
     * @throws Exception if error occurs while creating test Device
     */
    @Test
    public void testSaveWithDevices() throws Exception {
        LOGGER.debug("testSave test started");
        final String deviceName2 = "Test Device 2";
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceGroup.class), 0);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 0);

        final DeviceGroup deviceGroup = new DeviceGroup(deviceGroupName);
        Device device1 = getSavedTestDevice(deviceName);
        Device device2 = getSavedTestDevice(deviceName2);
        deviceGroup.addDevice(device1);
        deviceGroup.addDevice(device2);

        final DeviceGroupManager mgr = new DBDeviceGroupManager(sessionFactory);
        final DeviceGroup savedDeviceGroup = mgr.saveDeviceGroup(deviceGroup);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 2);
        Assert.assertEquals(savedDeviceGroup, deviceGroup);
        Assert.assertTrue(
                DBUtility.isInDatabase(sessionFactory, DeviceGroup.class, deviceGroupName)
        );

        final UUID deviceGroupID = savedDeviceGroup.getId();
        Assert.assertNotNull(deviceGroupID);
        Assert.assertEquals(savedDeviceGroup.getDevices(),
                deviceGroup.getDevices());
    }

    /**
     * Tests that the <code>DBDeviceGroupManager</code> can save a
     * <code>DeviceGroup</code>.
     * @throws Exception if error occurs while creating test Device
     */
    @Test
    public void testRemoveAndSaveWithDevices() throws Exception {
        LOGGER.debug("testSave test started");
        final String deviceName2 = "Test Device 2";
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceGroup.class), 0);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 0);

        final DeviceGroup deviceGroup = new DeviceGroup(deviceGroupName);
        Device device1 = getSavedTestDevice(deviceName);
        Device device2 = getSavedTestDevice(deviceName2);
        deviceGroup.addDevice(device1);
        deviceGroup.addDevice(device2);

        final DeviceGroupManager mgr = new DBDeviceGroupManager(sessionFactory);
        final DeviceGroup savedDeviceGroup = mgr.saveDeviceGroup(deviceGroup);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 2);
        Assert.assertEquals(savedDeviceGroup, deviceGroup);
        Assert.assertTrue(
                DBUtility.isInDatabase(sessionFactory, DeviceGroup.class, deviceGroupName)
        );

        final UUID deviceGroupID = savedDeviceGroup.getId();
        Assert.assertNotNull(deviceGroupID);
        Assert.assertEquals(savedDeviceGroup.getDevices(),
                deviceGroup.getDevices());

        Assert.assertEquals(device1.getDeviceGroup().getId(), deviceGroupID);
        Assert.assertEquals(device2.getDeviceGroup().getId(), deviceGroupID);

        savedDeviceGroup.removeDevice(device1);
        mgr.deleteDeviceGroup(deviceName);
        Assert.assertTrue(DBUtility.isInDatabase(sessionFactory, Device.class, deviceName));
    }

    /**
     * Tests that the <code>DBDeviceGroupManager</code> can save a
     * <code>DeviceGroup</code>.
     * @throws Exception if error occurs while creating test Device
     */
    @Test
    public void testChangeDeviceGroups() throws Exception {
        LOGGER.debug("testSave test started");
        final String deviceName2 = "Test Device 2";
        final String deviceGroupName2 = "Test Device Group 2";
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceGroup.class), 0);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 0);

        final DeviceGroup deviceGroup = new DeviceGroup(deviceGroupName);
        Device device1 = getSavedTestDevice(deviceName);
        Device device2 = getSavedTestDevice(deviceName2);
        deviceGroup.addDevice(device1);
        deviceGroup.addDevice(device2);

        final DeviceGroupManager mgr = new DBDeviceGroupManager(sessionFactory);
        final DeviceGroup savedDeviceGroup = mgr.saveDeviceGroup(deviceGroup);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 2);
        Assert.assertEquals(savedDeviceGroup, deviceGroup);
        Assert.assertTrue(
                DBUtility.isInDatabase(sessionFactory, DeviceGroup.class, deviceGroupName)
        );

        final DeviceGroup deviceGroup2 = new DeviceGroup(deviceGroupName2);
        final DeviceManager deviceMgr = new DBDeviceManager(sessionFactory);
        final DeviceGroup savedDeviceGroup2 = mgr.saveDeviceGroup(deviceGroup2);
        device1.setDeviceGroup(deviceGroup2);
        deviceMgr.updateDevice(device1);

        Assert.assertFalse(savedDeviceGroup.getDevices().contains(device1));
        Assert.assertTrue(savedDeviceGroup2.getDevices().contains(device1));
    }

    /**
     * Tests that the <code>DBDeviceGroupManager</code> throws a
     * <code>DeviceGroupManagerException</code> if a <code>DeviceGroup</code> is
     * saved twice.
     */
    @Test(expectedExceptions = DeviceGroupManagerException.class)
    public void testSaveTwice() {
        LOGGER.debug("testSaveTwice test started");
        final DeviceGroup deviceGroup = new DeviceGroup(deviceGroupName);
        final DeviceGroupManager mgr = new DBDeviceGroupManager(sessionFactory);
        final DeviceGroup savedDeviceGroup = mgr.saveDeviceGroup(deviceGroup);
        mgr.saveDeviceGroup(savedDeviceGroup);
        Assert.fail("second save did not fail");
    }

    /**
     * Tests that the <code>DBDeviceGroupManager</code> throws a
     * <code>DeviceGroupManagerException</code> if a <code>DeviceGroup</code> is
     * saved with the same name as an existing <code>DeviceGroup</code>.
     */
    @Test(expectedExceptions = DeviceGroupManagerException.class)
    public void testSaveSameName() {
        final DeviceGroup deviceGroup = new DeviceGroup(deviceGroupName);
        final DeviceGroup deviceGroup2 = new DeviceGroup(deviceGroupName);
        final DeviceGroupManager mgr = new DBDeviceGroupManager(sessionFactory);
        final DeviceGroup savedDeviceGroup = mgr.saveDeviceGroup(deviceGroup);
        Assert.assertNotNull(savedDeviceGroup);
        mgr.saveDeviceGroup(deviceGroup2);
        Assert.fail("second save did not fail");
    }

    /**
     * Tests that the <code>DBDeviceGroupManager</code> throws a
     * <code>DeviceGroupManagerException</code> if the device parameter is null.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testSaveNullDevice() {
        LOGGER.debug("testSaveNullDevice test started");
        final DeviceManager mgr = new DBDeviceManager(sessionFactory);
        mgr.saveDevice(null);
        Assert.fail("save did not fail");
    }

    /**
     * Tests that the <code>DBDeviceGroupManager</code> can update a
     * <code>DeviceGroup</code>.
     * @throws Exception if error occurs while creating test Device
     */
    @Test
    public void testUpdate() throws Exception {
        LOGGER.debug("testUpdate test started");
        LOGGER.debug("asserting db is empty");
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceGroup.class), 0);

        LOGGER.debug("saving new device group in db");
        DeviceGroup deviceGroup = new DeviceGroup(deviceGroupName);
        deviceGroup.setWaitForAppraisalCompletionEnabled(true);
        final DeviceGroupManager mgr = new DBDeviceGroupManager(sessionFactory);
        DeviceGroup savedDeviceGroup = mgr.saveDeviceGroup(deviceGroup);

        Assert.assertTrue(savedDeviceGroup.isWaitForAppraisalCompletionEnabled());
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceGroup.class), 1);

        LOGGER.debug("updating device group with new device");
        Device device = getSavedTestDevice(deviceName);
        savedDeviceGroup.addDevice(device);
        savedDeviceGroup.setWaitForAppraisalCompletionEnabled(false);
        updateDevice(device);

        LOGGER.debug("verifying updated device group");
        final DeviceGroup updatedDeviceGroup =
                mgr.getDeviceGroup(deviceGroupName);
        Assert.assertEquals(savedDeviceGroup, updatedDeviceGroup);
        Assert.assertEquals(savedDeviceGroup.getDevices(),
                updatedDeviceGroup.getDevices());
        Assert.assertFalse(updatedDeviceGroup.isWaitForAppraisalCompletionEnabled());

        //Includes the DefaultGroup that is always present
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceGroup.class), 2);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 1);
    }

    /**
     * This tests that when a <code>Device</code> is removed from the
     * <code>DeviceGroup</code> then the device is not removed from the
     * database.
     * @throws Exception if error occurs while creating test Device
     */
    @Test
    public void testUpdateNullReport() throws Exception {
        LOGGER.debug("testUpdate test started");
        LOGGER.debug("asserting db is empty");
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 0);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceInfoReport.class), 0);

        LOGGER.debug("saving new device group in db");
        final DeviceGroup deviceGroup = new DeviceGroup(deviceGroupName);
        deviceGroup.addDevice(getSavedTestDevice(deviceName));
        final DeviceGroupManager mgr = new DBDeviceGroupManager(sessionFactory);
        final DeviceGroup savedDeviceGroup = mgr.saveDeviceGroup(deviceGroup);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceGroup.class), 2);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 1);

        LOGGER.debug("removing device from device group");
        savedDeviceGroup.removeDevice(deviceName);
        mgr.updateDeviceGroup(savedDeviceGroup);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceGroup.class), 2);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 1);
    }

    /**
     * Tests that the <code>DBDeviceGroupManager</code> fails to update a
     * <code>DeviceGroup</code> that has the same name as an existing
     * <code>DeviceGroup</code>.
     */
    @Test
    public void testUpdateSameName() {
        LOGGER.debug("testUpdateSameName test started");
        final String name1 = "Test Device Group 1";
        final String name2 = "Test Device Group 2";
        final DeviceGroupManager mgr = new DBDeviceGroupManager(sessionFactory);
        DeviceGroup d1 = new DeviceGroup(name1);
        DeviceGroup d2 = new DeviceGroup(name2);
        mgr.saveDeviceGroup(d1);
        d2 = mgr.saveDeviceGroup(d2);
        d2.setName(name1);
        DeviceGroupManagerException expected = null;
        try {
            mgr.updateDeviceGroup(d2);
        } catch (DeviceGroupManagerException e) {
            expected = e;
        }
        Assert.assertNotNull(expected);
        Assert.assertTrue(DBUtility.isInDatabase(sessionFactory, DeviceGroup.class, name1));
        Assert.assertTrue(DBUtility.isInDatabase(sessionFactory, DeviceGroup.class, name2));
    }

    /**
     * Tests that the <code>DBDeviceGroupManager</code> throws a
     * <code>DeviceManagerException</code> if the device parameter is null.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testUpdateNullDevice() {
        LOGGER.debug("testUpdateNullDevice test started");
        final DeviceManager mgr = new DBDeviceManager(sessionFactory);
        mgr.updateDevice(null);
        Assert.fail("save did not fail");
    }

    /**
     * Tests that the <code>DBDeviceGroupManager</code> returns null when null
     * name is passed to get.
     */
    @Test
    public void testGetNull() {
        LOGGER.debug("testGetNull test started");
        final DeviceGroupManager mgr = new DBDeviceGroupManager(sessionFactory);
        Assert.assertNull(mgr.getDeviceGroup(null));
    }

    /**
     * Tests that the <code>DBDeviceGroupManager</code> returns null when an
     * unknown device group name is passed to get.
     */
    @Test
    public void testGetUnknown() {
        LOGGER.debug("testGetUnknown test started");
        final String unknown = "Some Unknown Device Group";
        final DeviceGroupManager mgr = new DBDeviceGroupManager(sessionFactory);
        Assert.assertNull(mgr.getDeviceGroup(unknown));
    }

    /**
     * Tests that Groups assigned to a particular policy can be returned.
     *
     * @throws Exception
     *                if error occurs while creating test Device
     */
    @Test
    public void testGetGroupsForPolicy() throws Exception {
        LOGGER.debug("testGetGroupsForPolicy test started");

        final DeviceGroupManager groupManager = new DBDeviceGroupManager(sessionFactory);
        final PolicyManager policyManager = new DBPolicyManager(sessionFactory);
        final AppraiserManager appraiserManager = new DBAppraiserManager(sessionFactory);

        Appraiser appraiser = new TestAppraiser("Test Appraiser");
        Appraiser savedAppraiser = appraiserManager.saveAppraiser(appraiser);

        TestPolicy policy = new TestPolicy("TEST_POLICY");
        policy = (TestPolicy) policyManager.savePolicy(policy);

        DeviceGroup deviceGroup = createDeviceGroup("Test Device Group", "Test Device");
        DeviceGroup deviceGroup2 = createDeviceGroup("Test Device Group 2", "Test Device 2");
        policyManager.setPolicy(savedAppraiser, deviceGroup, policy);
        policyManager.setPolicy(savedAppraiser, deviceGroup2, policy);

        Set<DeviceGroup> groups = groupManager.getGroupsAssignedToPolicy(policy);

        Assert.assertEquals(groups.size(), 2);
        Assert.assertTrue(groups.contains(deviceGroup));
        Assert.assertTrue(groups.contains(deviceGroup2));
    }

    /**
     * Tests that the <code>DBDeviceGroupManager</code> returns a complete list
     * of all the device groups that it manages.
     */
    @Test
    public void testGetDeviceList() {
        LOGGER.debug("testGetDeviceList test started");
        final DeviceGroupManager mgr = new DBDeviceGroupManager(sessionFactory);
        final String[] names = {"DeviceGroup1", "DeviceGroup2",
                "DeviceGroup3", "DeviceGroup4"};
        final Set<DeviceGroup> deviceGroupSet = new HashSet<>();
        for (String name : names) {
            final DeviceGroup deviceGroup = new DeviceGroup(name);
            mgr.saveDeviceGroup(deviceGroup);
            deviceGroupSet.add(deviceGroup);
        }
        final Set<DeviceGroup> dbNames = mgr.getDeviceGroupSet();
        Assert.assertEquals(dbNames, deviceGroupSet);
    }


    /**
     * Tests that the <code>DBDeviceGroupManager</code> returns an empty list
     * when get list is called when there are no items in the database.
     */
    @Test
    public void testGetDeviceGroupListEmptyList() {
        LOGGER.debug("testGetDeviceGroupListEmptyList test started");
        final DeviceGroupManager mgr = new DBDeviceGroupManager(sessionFactory);
        final Set<DeviceGroup> deviceGroupSet = new HashSet<>();
        final Set<DeviceGroup> dbDeviceGroupSet = mgr.getDeviceGroupSet();
        Assert.assertEquals(deviceGroupSet, dbDeviceGroupSet);
    }

/**
     * Tests that when a <code>DeviceGroup</code> is deleted, the
     * <code>Device</code>s that were part of the <code>DeviceGroup</code> are
     * not deleted.
     *
     * @throws Exception if error occurs while creating test device
     */
    @Test
    public void testDeleteDeviceGroup() throws Exception {
        LOGGER.debug("testDeleteDeviceGroup");
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceGroup.class), 0);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 0);

        final DeviceGroup deviceGroup = new DeviceGroup(deviceGroupName);
        Device device = getSavedTestDevice(deviceName);
        deviceGroup.addDevice(device);

        final DeviceGroupManager mgr = new DBDeviceGroupManager(sessionFactory);
        final DeviceGroup savedDeviceGroup = mgr.saveDeviceGroup(deviceGroup);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 1);
        Assert.assertEquals(savedDeviceGroup, deviceGroup);
        Assert.assertTrue(
                DBUtility.isInDatabase(sessionFactory, DeviceGroup.class, deviceGroupName)
        );
        Assert.assertTrue(DBUtility.isInDatabase(sessionFactory, Device.class, deviceName));

        final UUID deviceGroupID = savedDeviceGroup.getId();
        Assert.assertNotNull(deviceGroupID);
        Assert.assertEquals(savedDeviceGroup.getDevices(),
                deviceGroup.getDevices());
        boolean deleteSuccessful = mgr.deleteDeviceGroup(deviceGroupName);
        Assert.assertTrue(deleteSuccessful);
        Assert.assertFalse(
                DBUtility.isInDatabase(sessionFactory, DeviceGroup.class, deviceGroupName)
        );

        Assert.assertTrue(DBUtility.isInDatabase(sessionFactory, Device.class, deviceName));
        final DeviceManager deviceManager = new DBDeviceManager(sessionFactory);
        Device savedDevice = deviceManager.getDevice(deviceName);
        Assert.assertEquals(savedDevice.getDeviceGroup().getName(), DeviceGroup.DEFAULT_GROUP);
    }

    /**
     * Tests that the <code>DBDeviceGroupManager</code> can successfully handle
     * delete calls when the device group name is unknown.
     */
    @Test
    public void testDeleteUnknown() {
        LOGGER.debug("testDeleteUnknown test started");
        final String unknown = "My Unknown Device Group";
        final DeviceGroupManager mgr = new DBDeviceGroupManager(sessionFactory);
        Assert.assertNull(mgr.getDeviceGroup(unknown));
        Assert.assertFalse(mgr.deleteDeviceGroup(unknown));
    }

    /**
     * Tests that the <code>DBDeviceGroupManager</code> can successfully handle
     * delete calls when the device name is null.
     */
    @Test
    public void testDeleteNull() {
        LOGGER.debug("testDeleteNull test started");
        final DeviceGroupManager mgr = new DBDeviceGroupManager(sessionFactory);
        Assert.assertFalse(mgr.deleteDeviceGroup(null));
    }

    private Device getSavedTestDevice(final String name) throws Exception {
        final DeviceGroupManager groupManager = new DBDeviceGroupManager(sessionFactory);
        DeviceGroup group = groupManager.getDeviceGroup(DeviceGroup.DEFAULT_GROUP);
        if (group == null) {
            group = groupManager.saveDeviceGroup(new DeviceGroup(DeviceGroup.DEFAULT_GROUP));
        }
        final Device device = DeviceTest.getTestDevice(name);
        device.setDeviceGroup(group);
        final DeviceManager mgr = new DBDeviceManager(sessionFactory);
        return mgr.saveDevice(device);
    }

    private void updateDevice(final Device device) {
        final DeviceManager mgr = new DBDeviceManager(sessionFactory);
        mgr.updateDevice(device);
    }

    private DeviceGroup createDeviceGroup(final String deviceGroupName,
            final String deviceName) throws Exception {
        DeviceGroupManager deviceGroupManager =
                new DBDeviceGroupManager(sessionFactory);
        DeviceManager deviceManager = new DBDeviceManager(sessionFactory);
        Device device = DeviceTest.getTestDevice(deviceName);
        DeviceGroup deviceGroup = new DeviceGroup(deviceGroupName);
        deviceGroup = deviceGroupManager.saveDeviceGroup(deviceGroup);
        deviceGroup.addDevice(device);
        device.setDeviceGroup(deviceGroup);
        deviceManager.saveDevice(device);

        deviceGroupManager.updateDeviceGroup(deviceGroup);
        return deviceGroup;
    }
}
