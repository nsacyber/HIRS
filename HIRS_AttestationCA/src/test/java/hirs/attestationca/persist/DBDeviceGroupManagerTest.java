package hirs.attestationca.persist;

import hirs.appraiser.Appraiser;
import hirs.attestationca.servicemanager.DBDeviceManager;
import hirs.attestationca.entity.Device;
import hirs.attestationca.entity.DeviceInfoReport;
import hirs.data.persist.policy.Policy;
import hirs.persist.DBUtility;
import hirs.persist.PolicyMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
}
