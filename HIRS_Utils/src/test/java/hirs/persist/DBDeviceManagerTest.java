package hirs.persist;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import hirs.data.persist.Device;
import hirs.data.persist.DeviceGroup;
import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.DeviceTest;
import hirs.data.persist.enums.HealthStatus;
import hirs.data.persist.info.NetworkInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * <code>DBDeviceManagerTest</code> is a unit test class for the
 * <code>DBDeviceManager</code> class.
 */
public final class DBDeviceManagerTest extends SpringPersistenceTest {
    private static final Logger LOGGER = LogManager.getLogger(DBDeviceManagerTest.class);

    private final String deviceName = "My Cool Device";

    /**
     * Default constructor that does nothing.
     */
    public DBDeviceManagerTest() {
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
        DBUtility.removeAllInstances(sessionFactory, DeviceGroup.class);
        DBUtility.removeAllInstances(sessionFactory, DeviceInfoReport.class);
    }

    /**
     * Tests that the <code>DBDeviceManager</code> can save a
     * <code>Device</code>.
     *
     * @throws Exception
     *              if any unexpected errors occur in getting a Device
     */
    @Test
    public void testSave() throws Exception {
        LOGGER.debug("testSave test started");
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 0);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceInfoReport.class), 0);
        final Device device = DeviceTest.getTestDevice(deviceName);
        final DeviceGroup group = createGroup(DeviceGroup.DEFAULT_GROUP);
        device.setDeviceGroup(group);
        final DeviceManager mgr = new DBDeviceManager(sessionFactory);
        final Device d2 = mgr.saveDevice(device);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 1);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceInfoReport.class), 1);
        Assert.assertEquals(d2, device);
        Assert.assertTrue(DBUtility.isInDatabase(sessionFactory, Device.class, deviceName));
        final UUID reportId = d2.getDeviceInfo().getId();
        Assert.assertNotNull(reportId);
        Assert.assertTrue(
                DBUtility.isInDatabase(sessionFactory, DeviceInfoReport.class, "id", reportId)
        );
    }

    /**
     * Tests that the <code>DBDeviceManager</code> throws a
     * <code>DeviceManagerException</code> if a <code>Device</code> is saved
     * twice.
     *
     * @throws DeviceManagerException if any unexpected errors occur
     * @throws Exception
     *          if any unexpected errors occur
     */
    @Test(expectedExceptions = DeviceManagerException.class)
    public void testSaveTwice() throws DeviceManagerException, Exception {
        LOGGER.debug("testSaveTwice test started");
        final Device device = new Device(deviceName);
        final DeviceManager mgr = new DBDeviceManager(sessionFactory);
        final DeviceGroup group = createGroup(DeviceGroup.DEFAULT_GROUP);
        device.setDeviceGroup(group);
        final Device b2 = mgr.saveDevice(device);
        mgr.saveDevice(b2);
        Assert.fail("second save did not fail");
    }

    /**
     * Tests that the <code>DBDeviceManager</code> throws a
     * <code>DeviceManagerException</code> if a <code>Device</code> is saved
     * with the same name as an existing <code>Device</code>.
     *
     * @throws DeviceManagerException if any unexpected errors occur
     */
    @Test(expectedExceptions = DeviceManagerException.class)
    public void testSaveSameName() throws DeviceManagerException {
        LOGGER.debug("testSaveSameName test started");
        final Device device = new Device(deviceName);
        final Device device2 = new Device(deviceName);
        final DeviceManager mgr = new DBDeviceManager(sessionFactory);
        final Device b2 = mgr.saveDevice(device);
        Assert.assertNotNull(b2);
        mgr.saveDevice(device2);
        Assert.fail("second save did not fail");
    }

    /**
     * Tests that the <code>DBDeviceManager</code> throws a
     * <code>DeviceManagerException</code> if the device parameter is null.
     *
     * @throws DeviceManagerException if any unexpected errors occur
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testSaveNullDevice() throws DeviceManagerException {
        LOGGER.debug("testSaveNullDevice test started");
        final DeviceManager mgr = new DBDeviceManager(sessionFactory);
        mgr.saveDevice(null);
        Assert.fail("save did not fail");
    }

    /**
     * Tests that when a <code>Device</code> is deleted, the
     * <code>Device</code> is removed from the DB.
     *
     * @throws Exception if error occurs while creating test device
     */
    @Test
    public void testDeleteDevice() throws Exception {
        LOGGER.debug("testDeleteDevice");
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 0);

        final Device device = new Device(deviceName);
        final DeviceManager mgr = new DBDeviceManager(sessionFactory);
        final DeviceGroup group = createGroup(DeviceGroup.DEFAULT_GROUP);
        device.setDeviceGroup(group);
        final Device savedDevice = mgr.saveDevice(device);

        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 1);
        Assert.assertTrue(DBUtility.isInDatabase(sessionFactory, Device.class, deviceName));

        final UUID deviceID = savedDevice.getId();
        Assert.assertNotNull(deviceID);
        boolean deleteSuccessful = mgr.deleteDevice(deviceName);
        Assert.assertTrue(deleteSuccessful);
        Assert.assertFalse(
                DBUtility.isInDatabase(sessionFactory, DeviceGroup.class, deviceName)
        );
    }

    /**
     * Tests that the <code>DBDeviceManager</code> can update a
     * <code>Device</code>.
     *
     * @throws Exception if any unexpected errors occur
     */
    @Test
    public void testUpdate() throws Exception {
        LOGGER.debug("testUpdate test started");
        LOGGER.debug("asserting db is empty");
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 0);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceInfoReport.class), 0);

        LOGGER.debug("saving new device in db");
        final Device device = DeviceTest.getTestDevice(deviceName);
        final DeviceGroup group = createGroup(DeviceGroup.DEFAULT_GROUP);
        device.setDeviceGroup(group);
        final DeviceManager mgr = new DBDeviceManager(sessionFactory);
        final Device d2 = mgr.saveDevice(device);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 1);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceInfoReport.class), 1);

        LOGGER.debug("updating device with new network info and state");
        final DeviceInfoReport report = d2.getDeviceInfo();
        final NetworkInfo networkInfo = new NetworkInfo(null, null, null);
        final DeviceInfoReport updateReport = new DeviceInfoReport(networkInfo,
                report.getOSInfo(), report.getFirmwareInfo(), report.getHardwareInfo(),
                report.getTPMInfo());
        d2.setDeviceInfo(updateReport);
        d2.setHealthStatus(HealthStatus.TRUSTED);
        mgr.updateDevice(d2);

        LOGGER.debug("verifying updated device");
        final Device dbDevice = mgr.getDevice(deviceName);
        Assert.assertEquals(d2, dbDevice);
        Assert.assertEquals(networkInfo, dbDevice.getDeviceInfo().getNetworkInfo());
        Assert.assertEquals(dbDevice.getHealthStatus(), HealthStatus.TRUSTED);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 1);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceInfoReport.class), 1);
    }

    /**
     * This tests that when a <code>Device</code> is updated with a null
     * <code>DeviceInfoReport</code> then the old report is removed from the
     * database.
     *
     * @throws Exception
     *              if any unexpected errors occur in getting a Device
     */
    @Test
    public void testUpdateNullReport() throws Exception {
        LOGGER.debug("testUpdate test started");
        LOGGER.debug("asserting db is empty");
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 0);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceInfoReport.class), 0);

        LOGGER.debug("saving new device in db");
        final Device device = DeviceTest.getTestDevice(deviceName);
        final DeviceGroup group = createGroup(DeviceGroup.DEFAULT_GROUP);
        device.setDeviceGroup(group);
        final DeviceManager mgr = new DBDeviceManager(sessionFactory);
        final Device d2 = mgr.saveDevice(device);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 1);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceInfoReport.class), 1);

        LOGGER.debug("updating device will null device info");
        d2.setDeviceInfo(null);
        mgr.updateDevice(d2);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Device.class), 1);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceInfoReport.class), 0);
    }

    /**
     * Tests that the <code>DBDeviceManager</code> fails to update a
     * <code>Device</code> that has the same name as an existing
     * <code>Device</code>.
     *
     * @throws DeviceManagerException if any unexpected errors occur
     * @throws Exception
     *          if any unexpected errors occur
     */
    @Test
    public void testUpdateSameName() throws DeviceManagerException, Exception {
        LOGGER.debug("testUpdateSameName test started");
        final String name1 = "Test Device 1";
        final String name2 = "Test Device 2";
        final DeviceManager mgr = new DBDeviceManager(sessionFactory);
        final DeviceGroup group = createGroup(DeviceGroup.DEFAULT_GROUP);
        Device d1 = new Device(name1);
        Device d2 = new Device(name2);
        d1.setDeviceGroup(group);
        d2.setDeviceGroup(group);
        mgr.saveDevice(d1);
        d2 = mgr.saveDevice(d2);
        d2.setName(name1);
        DeviceManagerException expected = null;
        try {
            mgr.updateDevice(d2);
        } catch (DeviceManagerException e) {
            expected = e;
        }
        Assert.assertNotNull(expected);
        Assert.assertTrue(DBUtility.isInDatabase(sessionFactory, Device.class, name1));
        Assert.assertTrue(DBUtility.isInDatabase(sessionFactory, Device.class, name2));
    }

    /**
     * Tests that the <code>DBDeviceManager</code> throws a
     * <code>DeviceManagerException</code> if the device parameter is null.
     *
     * @throws DeviceManagerException if any unexpected errors occur
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testUpdateNullDevice() throws DeviceManagerException {
        LOGGER.debug("testUpdateNullDevice test started");
        final DeviceManager mgr = new DBDeviceManager(sessionFactory);
        mgr.updateDevice(null);
        Assert.fail("save did not fail");
    }

    /**
     * Tests that the <code>DBDeviceManager</code> can successfully return a
     * <code>Device</code> from the database.
     *
     * @throws Exception
     *              if any unexpected errors occur in getting a Device
     */
    @Test
    public void testGet() throws Exception {
        LOGGER.debug("testGet test started");
        final Device device = DeviceTest.getTestDevice(deviceName);
        final DeviceManager mgr = new DBDeviceManager(sessionFactory);
        final ReportManager reportMgr = new DBReportManager(sessionFactory);
        final DeviceGroup group = createGroup(DeviceGroup.DEFAULT_GROUP);
        device.setDeviceGroup(group);
        final Device d2 = mgr.saveDevice(device);
        final UUID reportId = d2.getDeviceInfo().getId();
        final Device dbDevice = mgr.getDevice(d2.getName());
        Assert.assertEquals(dbDevice, d2);
        final DeviceInfoReport dbReport = (DeviceInfoReport) reportMgr
                .getReport(reportId);
        Assert.assertEquals(dbReport, device.getDeviceInfo());
        Assert.assertEquals(dbDevice.getHealthStatus(), HealthStatus.UNKNOWN);
    }

    /**
     * Tests that the <code>DBDeviceManager</code> returns null when null name
     * is passed to get.
     */
    @Test
    public void testGetNull() {
        LOGGER.debug("testGetNull test started");
        final DeviceManager mgr = new DBDeviceManager(sessionFactory);
        Assert.assertNull(mgr.getDevice(null));
    }

    /**
     * Tests that the <code>DBDeviceManager</code> returns null when an unknown
     * device name is passed to get.
     */
    @Test
    public void testGetUnknown() {
        LOGGER.debug("testGetUnknown test started");
        final String unknown = "Some Unknown Device";
        final DeviceManager mgr = new DBDeviceManager(sessionFactory);
        Assert.assertNull(mgr.getDevice(unknown));
    }

    /**
     * Tests that the <code>DBDeviceManager</code> returns a complete list of
     * all the names of the devices that it manages.
     *
     * @throws Exception
     *              if any unexpected errors occur in getting a Device
     */
    @Test
    public void testGetDeviceNameList() throws Exception {
        LOGGER.debug("testGetDeviceNameList test started");
        final DeviceManager mgr = new DBDeviceManager(sessionFactory);
        final String[] names = {"Device1", "Device2", "Device3", "Device4"};
        final List<String> namesList = new LinkedList<>();
        final DeviceGroup group = createGroup(DeviceGroup.DEFAULT_GROUP);

        for (String name : names) {
            final Device device = DeviceTest.getTestDevice(name);
            device.setDeviceGroup(group);
            mgr.saveDevice(device);
            namesList.add(name);
        }
        final List<String> dbNames = mgr.getDeviceNameList();
        Collections.sort(dbNames);
        Assert.assertEquals(dbNames, namesList);
    }

    /**
     * Tests that the <code>DBDeviceManager</code> returns an empty list when
     * get list is called when there are no items in the database.
     */
    @Test
    public void testGetDeviceNameListEmpty() {
        LOGGER.debug("testGetDeviceNameListEmpty test started");
        final DeviceManager mgr = new DBDeviceManager(sessionFactory);
        final List<String> namesList = new LinkedList<>();
        final List<String> dbNames = mgr.getDeviceNameList();
        Assert.assertEquals(dbNames, namesList);
    }
    /**
     * Tests that the <code>DBDeviceManager</code> returns a complete list of
     * all the devices that it manages.
     *
     * @throws Exception
     *              if any unexpected errors occur in getting a Device
     */

    @Test
    public void testGetDeviceSet() throws Exception {
        LOGGER.debug("testGetDeviceSet test started");
        final DeviceManager mgr = new DBDeviceManager(sessionFactory);
        final String[] names = {"Device1", "Device2", "Device3", "Device4"};
        final Device[] expectedDevices = new Device[names.length];
        final DeviceGroup group = createGroup(DeviceGroup.DEFAULT_GROUP);
        for (int i = 0; i < names.length; ++i) {
            final Device device = DeviceTest.getTestDevice(names[i]);
            expectedDevices[i] = device;
            device.setDeviceGroup(group);
            mgr.saveDevice(device);
        }
        final Set<Device> devices = mgr.getDeviceList();
        Assert.assertEquals(devices.size(), expectedDevices.length);
        for (int i = 0; i < expectedDevices.length; ++i) {
            Assert.assertTrue(devices.contains(expectedDevices[i]));
        }
    }

    /**
     * Tests that the <code>DBDeviceManager</code> returns an empty list when
     * get list is called when there are no items in the database.
     */
    @Test
    public void testGetDeviceSetEmpty() {
        LOGGER.debug("testGetDeviceSetEmpty test started");
        final DeviceManager mgr = new DBDeviceManager(sessionFactory);
        final Set<Device> devicesList = new HashSet<>();
        final Set<Device> devices = mgr.getDeviceList();
        Assert.assertEquals(devices, devicesList);
    }

    private DeviceGroup createGroup(final String name) throws Exception {
        DeviceGroup group = new DeviceGroup(name);
        final DeviceGroupManager groupManager = new DBDeviceGroupManager(sessionFactory);
        return groupManager.saveDeviceGroup(group);
    }
}
