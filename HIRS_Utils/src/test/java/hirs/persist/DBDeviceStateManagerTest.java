package hirs.persist;

import hirs.data.persist.Device;
import hirs.data.persist.DeviceGroup;
import hirs.data.persist.DeviceState;
import hirs.data.persist.IMADeviceState;
import hirs.data.persist.SpringPersistenceTest;
import hirs.data.persist.TPMDeviceState;
import hirs.data.persist.TPMReport;

import org.testng.Assert;
import hirs.data.persist.TPMReportTest;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Tests the methods of <code>DBDeviceStateManager</code>.
 */
public final class DBDeviceStateManagerTest extends SpringPersistenceTest {

    private static final String BOOTCYCLE_ID = "Mon Apr 20 09:32";
    private static final int INDEX = 100;
    private static final byte[] PCR_STATE = new byte[] {0x00, 0x01, 0x02,
            0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D,
            0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14};

    private Device device;
    private DeviceManager deviceManager;
    private DeviceStateManager stateManager;

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
     * Initializes the test state by creating a new <code>DBDeviceStateManager</code> and storing
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
        stateManager = new DBDeviceStateManager(sessionFactory);
    }

    /**
     * Resets the test state to a known good state.
     */
    @AfterMethod
    public void resetTestState() {
        DBUtility.removeAllInstances(sessionFactory, TPMDeviceState.class);
        DBUtility.removeAllInstances(sessionFactory, IMADeviceState.class);
        DBUtility.removeAllInstances(sessionFactory, Device.class);
        DBUtility.removeAllInstances(sessionFactory, DeviceGroup.class);
    }

    /**
     * Tests that save can save two kinds of states that has not been reset.
     */
    @Test
    public void testSaveDefaultState() {
        Assert.assertEquals(DBUtility.getCount(sessionFactory, IMADeviceState.class), 0);
        final IMADeviceState imaState = new IMADeviceState(device);
        final IMADeviceState imaSavedState = (IMADeviceState) stateManager.saveState(imaState);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, IMADeviceState.class), 1);
        Assert.assertEquals(imaSavedState, imaState);

        Assert.assertEquals(DBUtility.getCount(sessionFactory, TPMDeviceState.class), 0);
        final TPMDeviceState tpmState = new TPMDeviceState(device);
        final TPMDeviceState tpmSavedState = (TPMDeviceState) stateManager.saveState(tpmState);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, TPMDeviceState.class), 1);
        Assert.assertEquals(tpmSavedState, tpmState);

        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceState.class), 2);
    }

    /**
     * Tests that <code>TPMDeviceState</code> cannot be saved twice.
     */
    @Test(expectedExceptions = DeviceStateManagerException.class)
    public void testSaveTPMStateTwice() {
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceState.class), 0);
        final TPMDeviceState state = new TPMDeviceState(device);
        updateTPMState(state);
        stateManager.saveState(state);
        stateManager.saveState(state);
    }

    /**
     * Tests that <code>IMADeviceState</code> cannot be saved twice.
     */
    @Test(expectedExceptions = DeviceStateManagerException.class)
    public void testSaveIMAStateTwice() {
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceState.class), 0);
        final IMADeviceState state = new IMADeviceState(device);
        updateIMAState(state);
        stateManager.saveState(state);
        stateManager.saveState(state);
    }

    /**
     * Tests that <code>Device</code> must be unique in TPM state.
     */
    @Test(expectedExceptions = DeviceStateManagerException.class)
    public void testSaveTPMStateWithSameDevice() {
        final TPMDeviceState state = new TPMDeviceState(device);
        final TPMDeviceState state2 = new TPMDeviceState(device);
        stateManager.saveState(state);
        stateManager.saveState(state2);
    }

    /**
     * Tests that <code>Device</code> must be unique in IMA state.
     */
    @Test(expectedExceptions = DeviceStateManagerException.class)
    public void testSaveIMAStateWithSameDevice() {
        final IMADeviceState state = new IMADeviceState(device);
        final IMADeviceState state2 = new IMADeviceState(device);
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
     * Tests that different types of state can be retrieved from database.
     */
    @Test
    public void testGet() {
        final TPMDeviceState tpmState = new TPMDeviceState(device);
        updateTPMState(tpmState);
        stateManager.saveState(tpmState);

        final IMADeviceState imaState = new IMADeviceState(device);
        updateIMAState(imaState);
        stateManager.saveState(imaState);

        final TPMDeviceState savedTPMState =
                (TPMDeviceState) stateManager.getState(device, TPMDeviceState.class);
        Assert.assertNotNull(savedTPMState);
        Assert.assertEquals(savedTPMState.getDevice(), device);

        final IMADeviceState savedIMAState =
                (IMADeviceState) stateManager.getState(device, IMADeviceState.class);
        Assert.assertNotNull(savedIMAState);
        Assert.assertEquals(savedIMAState.getDevice(), device);
    }

    /**
     * Tests that a NullPointerException is thrown if <code>Device</code> is null.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testGetNull() {
        stateManager.getState(null, IMADeviceState.class);
    }

    /**
     * Tests that null is returned when <code>Device</code> is unknown.
     */
    @Test
    public void testGetUnknown() {
        final TPMDeviceState savedState = (TPMDeviceState) stateManager.getState(device,
                TPMDeviceState.class);
        Assert.assertNull(savedState);
    }

    /**
     * Tests that different types of state can be retrieved from database in a list.
     */
    @Test
    public void testGetList() {
        final TPMDeviceState tpmState = new TPMDeviceState(device);
        updateTPMState(tpmState);
        stateManager.saveState(tpmState);

        final IMADeviceState imaState = new IMADeviceState(device);
        updateIMAState(imaState);
        stateManager.saveState(imaState);

        final List<DeviceState> states = stateManager.getStates(device);
        Assert.assertNotNull(states);
        Assert.assertEquals(states.size(), 2);
        Assert.assertTrue(states.contains(tpmState));
        Assert.assertTrue(states.contains(imaState));
    }

    /**
     * Tests that a NullPointerException is thrown if <code>Device</code> is null.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testGetListNull() {
        stateManager.getStates(null);
    }

    /**
     * Tests that null is returned when <code>Device</code> is unknown.
     */
    @Test
    public void testGetListUnknown() {
        final List<DeviceState> states = stateManager.getStates(device);
        Assert.assertEquals(states.size(), 0);
    }

    /**
     * Tests that the state can be updated.
     */
    @Test
    public void testUpdateState() {
        Assert.assertEquals(DBUtility.getCount(sessionFactory, TPMDeviceState.class), 0);
        final TPMDeviceState state = new TPMDeviceState(device);
        TPMDeviceState savedState = (TPMDeviceState) stateManager.saveState(state);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, TPMDeviceState.class), 1);
        Assert.assertEquals(savedState, state);
        Assert.assertNull(state.getReport());
        Assert.assertEquals(state.getTPMMeasurementRecords().size(), 0);
        updateTPMState(savedState);
        stateManager.updateState(savedState);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, TPMDeviceState.class), 1);
        savedState = (TPMDeviceState) stateManager.getState(device, TPMDeviceState.class);
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
        updateTPMState(state);
        final TPMDeviceState savedState = (TPMDeviceState) stateManager.saveState(state);

        final IMADeviceState imaDeviceState = new IMADeviceState(device);
        updateIMAState(imaDeviceState);
        final IMADeviceState savedIMAState = (IMADeviceState) stateManager.saveState(
                imaDeviceState);

        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceState.class), 2);
        Assert.assertEquals(savedState, state);
        Assert.assertEquals(savedIMAState, imaDeviceState);

        final boolean deleted = stateManager.deleteState(device, TPMDeviceState.class);
        Assert.assertTrue(deleted);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, TPMDeviceState.class), 0);
        // IMA Device state should still be in the database.
        Assert.assertEquals(DBUtility.getCount(sessionFactory, DeviceState.class), 1);
    }

    /**
     * Tests that delete returns false for unknown <code>Device</code>.
     */
    @Test
    public void testDeleteUnknown() {
        Assert.assertEquals(DBUtility.getCount(sessionFactory, TPMDeviceState.class), 0);
        final boolean deleted = stateManager.deleteState(device, TPMDeviceState.class);
        Assert.assertFalse(deleted);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, TPMDeviceState.class), 0);
    }

    /**
     * Tests that delete raises a NullPointerException for null <code>Device</code>.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testDeleteNull() {
        Assert.assertEquals(DBUtility.getCount(sessionFactory, TPMDeviceState.class), 0);
        stateManager.deleteState(null, TPMDeviceState.class);
    }

    private void updateTPMState(final TPMDeviceState state) {
        final DBReportManager reportManager = new DBReportManager(sessionFactory);
        TPMReport report = TPMReportTest.getTestReport();
        TPMReport savedReport = (TPMReport) reportManager.save(report);
        state.setTPMReport(savedReport);
    }

    private void updateIMAState(final IMADeviceState state) {
        state.setBootcycleId(BOOTCYCLE_ID);
        state.setIndex(INDEX);
        state.setPcrState(PCR_STATE);
    }

}
