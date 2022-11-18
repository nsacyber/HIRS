package hirs.attestationca.persist;

import hirs.attestationca.data.persist.DeviceTest;
import hirs.attestationca.servicemanager.DBDeviceManager;
import hirs.attestationca.entity.Device;
import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.enums.HealthStatus;
import hirs.data.persist.info.NetworkInfo;
import hirs.persist.DBUtility;
import hirs.persist.DeviceManagerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
}
