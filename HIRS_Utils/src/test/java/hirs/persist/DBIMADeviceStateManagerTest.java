package hirs.persist;

import hirs.data.persist.Device;
import hirs.data.persist.DeviceGroup;
import hirs.data.persist.IMADeviceState;

import hirs.data.persist.SpringPersistenceTest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;

/**
 * Tests the methods of <code>DBIMADeviceStateManager</code>.
 */
public final class DBIMADeviceStateManagerTest extends SpringPersistenceTest {
    private static final String BOOTCYCLE_ID = "Mon Apr 20 09:32";
    private static final int INDEX = 100;
    private static final byte[] PCR_STATE = new byte[] {0x00, 0x01, 0x02,
            0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D,
            0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14};
    private static final Date NOW_DATE = new Date();

    private Device device;
    private DeviceManager deviceManager;
    private IMADeviceStateManager stateManager;

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an
     * in-memory database that is used for testing.
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
     * Initializes the test state by creating a new
     * <code>DBIMADeviceStateManager</code> and storing a test
     * <code>Device</code> in the database.
     */
    @BeforeMethod
    public void initTestState() {
        device = new Device("My Test Device");
        final DeviceGroupManager groupManager = new DBDeviceGroupManager(sessionFactory);
        DeviceGroup group =
                groupManager.saveDeviceGroup(new DeviceGroup(DeviceGroup.DEFAULT_GROUP));
        device.setDeviceGroup(group);
        device = deviceManager.saveDevice(device);
        stateManager = new DBIMADeviceStateManager(sessionFactory);
    }

    /**
     * Resets the test state to a known good state. This currently only resets
     * the database by removing all <code>Device</code> and
     * <code>IMADeviceState</code> objects.
     */
    @AfterMethod
    public void resetTestState() {
        DBUtility.removeAllInstances(sessionFactory, IMADeviceState.class);
        DBUtility.removeAllInstances(sessionFactory, Device.class);
        DBUtility.removeAllInstances(sessionFactory, DeviceGroup.class);
    }

    /**
     * Tests that save can save state that has not been reset.
     */
    @Test
    public void testSaveDefaultState() {
        Assert.assertEquals(DBUtility.getCount(sessionFactory, IMADeviceState.class), 0);
        final IMADeviceState state = new IMADeviceState(device);
        final IMADeviceState savedState = stateManager.saveState(state);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, IMADeviceState.class), 1);
        Assert.assertEquals(savedState, state);
    }

    /**
     * Tests that save can save state that bootcycle ID and other properties
     * set.
     */
    @Test
    public void testSaveState() {
        Assert.assertEquals(DBUtility.getCount(sessionFactory, IMADeviceState.class), 0);
        final IMADeviceState state = new IMADeviceState(device);
        updateState(state);
        final IMADeviceState savedState = stateManager.saveState(state);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, IMADeviceState.class), 1);
        Assert.assertEquals(savedState, state);
    }

    /**
     * Tests that <code>IMADeviceState</code> cannot be saved twice.
     */
    @Test(expectedExceptions = IMADeviceStateManagerException.class)
    public void testSaveTwice() {
        Assert.assertEquals(DBUtility.getCount(sessionFactory, IMADeviceState.class), 0);
        final IMADeviceState state = new IMADeviceState(device);
        updateState(state);
        stateManager.saveState(state);
        stateManager.saveState(state);
        Assert.fail("second save did not fail");
    }

    /**
     * Tests that <code>Device</code> must be unique in state.
     */
    @Test(expectedExceptions = IMADeviceStateManagerException.class)
    public void testSaveWithSameDevice() {
        final IMADeviceState state = new IMADeviceState(device);
        final IMADeviceState state2 = new IMADeviceState(device);
        stateManager.saveState(state);
        stateManager.saveState(state2);
        Assert.fail("second save did not fail");
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
        final IMADeviceState state = new IMADeviceState(device);
        updateState(state);
        stateManager.saveState(state);
        final IMADeviceState savedState = stateManager.getState(device);
        Assert.assertNotNull(savedState);
        Assert.assertEquals(state.getDevice(), device);
        Assert.assertEquals(state.getBootcycleId(), BOOTCYCLE_ID);
        Assert.assertEquals(state.getIndex(), INDEX);
        Assert.assertEquals(state.getPcrState(), PCR_STATE);
    }

    /**
     * Tests that a <code>NullPointerException</code> is thrown when <code>Device</code> is null.
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
        final IMADeviceState savedState = stateManager.getState(device);
        Assert.assertNull(savedState);
    }

    /**
     * Tests that the state can be updated.
     */
    @Test
    public void testUpdateState() {
        Assert.assertEquals(DBUtility.getCount(sessionFactory, IMADeviceState.class), 0);
        final IMADeviceState state = new IMADeviceState(device);
        IMADeviceState savedState = stateManager.saveState(state);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, IMADeviceState.class), 1);
        Assert.assertEquals(savedState, state);
        Assert.assertEquals(state.getBootcycleId(), null);
        Assert.assertEquals(state.getIndex(), 0);
        Assert.assertEquals(state.getPcrState(), null);
        Assert.assertEquals(state.getMostRecentFullReportDate(), null);

        updateState(savedState);
        stateManager.updateState(savedState);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, IMADeviceState.class), 1);
        savedState = stateManager.getState(device);
        Assert.assertEquals(savedState.getBootcycleId(), BOOTCYCLE_ID);
        Assert.assertEquals(savedState.getIndex(), INDEX);
        Assert.assertEquals(savedState.getPcrState(), PCR_STATE);
        Assert.assertEquals(savedState.getMostRecentFullReportDate(), NOW_DATE);
    }

    /**
     * Tests that <code>NullPointerException</code> is raised when state is
     * null.
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
        Assert.assertEquals(DBUtility.getCount(sessionFactory, IMADeviceState.class), 0);
        final IMADeviceState state = new IMADeviceState(device);
        updateState(state);
        final IMADeviceState savedState = stateManager.saveState(state);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, IMADeviceState.class), 1);
        Assert.assertEquals(savedState, state);
        final boolean deleted = stateManager.deleteState(device);
        Assert.assertTrue(deleted);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, IMADeviceState.class), 0);
    }

    /**
     * Tests that delete returns false for unknown <code>Device</code>.
     */
    @Test
    public void testDeleteUnknown() {
        Assert.assertEquals(DBUtility.getCount(sessionFactory, IMADeviceState.class), 0);
        final boolean deleted = stateManager.deleteState(device);
        Assert.assertFalse(deleted);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, IMADeviceState.class), 0);
    }

    /**
     * Tests that delete throws a <code>NullPointerException</code> for null <code>Device</code>.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testDeleteNull() {
        Assert.assertEquals(DBUtility.getCount(sessionFactory, IMADeviceState.class), 0);
        stateManager.deleteState(null);
    }

    private void updateState(final IMADeviceState state) {
        state.setBootcycleId(BOOTCYCLE_ID);
        state.setIndex(INDEX);
        state.setPcrState(PCR_STATE);
        state.setMostRecentFullReportDate(NOW_DATE);
    }

}
