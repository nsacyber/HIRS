package hirs.persist;

import hirs.data.persist.Device;
import hirs.data.persist.DeviceGroup;
import hirs.data.persist.SpringPersistenceTest;
import hirs.data.persist.TPMDeviceState;
import hirs.data.persist.TPMReport;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import hirs.data.persist.TPMReportTest;

/**
 * Tests the methods of <code>DBTPMDeviceStateManager</code>.
 */
public final class DBTPMDeviceStateManagerTest extends SpringPersistenceTest {
    private Device device;
    private DeviceManager deviceManager;
    private TPMDeviceStateManager stateManager;

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an in-memory database that
     * is used for testing.
     */
    @BeforeClass
    public void setup() {
        deviceManager = new DBDeviceManager(sessionFactory);
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterClass
    public void tearDown() {

    }

    /**
     * Initializes the test state by creating a new <code>DBTPMDeviceStateManager</code> and storing
     * a test <code>Device</code> in the database.
     */
    @BeforeMethod
    public void initTestState() {
        device = new Device("My Test Device");
        final DeviceGroupManager groupManager = new DBDeviceGroupManager(sessionFactory);
        DeviceGroup group =
                groupManager.saveDeviceGroup(new DeviceGroup(DeviceGroup.DEFAULT_GROUP));
        device.setDeviceGroup(group);
        device = deviceManager.saveDevice(device);
        stateManager = new DBTPMDeviceStateManager(sessionFactory);
    }

    /**
     * Resets the test state to a known good state.
     */
    @AfterMethod
    public void resetTestState() {
        DBUtility.removeAllInstances(sessionFactory, TPMDeviceState.class);
        DBUtility.removeAllInstances(sessionFactory, Device.class);
        DBUtility.removeAllInstances(sessionFactory, DeviceGroup.class);
    }

    /**
     * Tests that save can save state that has not been reset.
     */
    @Test
    public void testSaveDefaultState() {
        Assert.assertEquals(DBUtility.getCount(sessionFactory, TPMDeviceState.class), 0);
        final TPMDeviceState state = new TPMDeviceState(device);
        final TPMDeviceState savedState = stateManager.saveState(state);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, TPMDeviceState.class), 1);
        Assert.assertEquals(savedState, state);
    }

    /**
     * Tests that <code>TPMDeviceState</code> cannot be saved twice.
     */
    @Test(expectedExceptions = TPMDeviceStateManagerException.class)
    public void testSaveTwice() {
        Assert.assertEquals(DBUtility.getCount(sessionFactory, TPMDeviceState.class), 0);
        final TPMDeviceState state = new TPMDeviceState(device);
        updateState(state);
        stateManager.saveState(state);
        stateManager.saveState(state);
    }

    /**
     * Tests that <code>Device</code> must be unique in state.
     */
    @Test(expectedExceptions = TPMDeviceStateManagerException.class)
    public void testSaveWithSameDevice() {
        final TPMDeviceState state = new TPMDeviceState(device);
        final TPMDeviceState state2 = new TPMDeviceState(device);
        stateManager.saveState(state);
        stateManager.saveState(state2);
    }

    /**
     * Tests that null cannot be used for state.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testSaveNullState() {
        stateManager.saveState(null);
    }

    /**
     * Tests that state can be retrieved from database.
     */
    @Test
    public void testGet() {
        final TPMDeviceState state = new TPMDeviceState(device);
        updateState(state);
        stateManager.saveState(state);
        final TPMDeviceState savedState = stateManager.getState(device);
        Assert.assertNotNull(savedState);
        Assert.assertEquals(savedState.getDevice(), device);
        Assert.assertNotNull(savedState.getReport());
        Assert.assertEquals(savedState.getTPMMeasurementRecords().size(), 1);
    }

    /**
     * Tests that a NullPointerException is thrown if <code>Device</code> is null.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testGetNull() {
        stateManager.getState(null);
    }

    /**
     * Tests that null is returned when <code>Device</code> is unknown.
     */
    @Test
    public void testGetUnknown() {
        final TPMDeviceState savedState = stateManager.getState(device);
        Assert.assertNull(savedState);
    }

    /**
     * Tests that the state can be updated.
     */
    @Test
    public void testUpdateState() {
        Assert.assertEquals(DBUtility.getCount(sessionFactory, TPMDeviceState.class), 0);
        final TPMDeviceState state = new TPMDeviceState(device);
        TPMDeviceState savedState = stateManager.saveState(state);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, TPMDeviceState.class), 1);
        Assert.assertEquals(savedState, state);
        Assert.assertNull(state.getReport());
        Assert.assertEquals(state.getTPMMeasurementRecords().size(), 0);
        updateState(savedState);
        stateManager.updateState(savedState);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, TPMDeviceState.class), 1);
        savedState = stateManager.getState(device);
        Assert.assertNotNull(savedState.getReport());
        Assert.assertEquals(savedState.getTPMMeasurementRecords().size(), 1);
    }

    /**
     * Tests that <code>NullPointerException</code> is raised when state is null.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testUpdateStateWithNullState() {
        stateManager.updateState(null);
    }

    /**
     * Tests that state can be added and then deleted.
     */
    @Test
    public void testDelete() {
        Assert.assertEquals(DBUtility.getCount(sessionFactory, TPMDeviceState.class), 0);
        final TPMDeviceState state = new TPMDeviceState(device);
        updateState(state);
        final TPMDeviceState savedState = stateManager.saveState(state);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, TPMDeviceState.class), 1);
        Assert.assertEquals(savedState, state);
        final boolean deleted = stateManager.deleteState(device);
        Assert.assertTrue(deleted);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, TPMDeviceState.class), 0);
    }

    /**
     * Tests that delete returns false for unknown <code>Device</code>.
     */
    @Test
    public void testDeleteUnknown() {
        Assert.assertEquals(DBUtility.getCount(sessionFactory, TPMDeviceState.class), 0);
        final boolean deleted = stateManager.deleteState(device);
        Assert.assertFalse(deleted);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, TPMDeviceState.class), 0);
    }

    /**
     * Tests that delete raises a NullPointerException for null <code>Device</code>.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testDeleteNull() {
        Assert.assertEquals(DBUtility.getCount(sessionFactory, TPMDeviceState.class), 0);
        stateManager.deleteState(null);
    }

    private void updateState(final TPMDeviceState state) {
        final DBReportManager reportManager = new DBReportManager(sessionFactory);
        TPMReport report = TPMReportTest.getTestReport();
        TPMReport savedReport = (TPMReport) reportManager.save(report);
        state.setTPMReport(savedReport);
    }


}
