package hirs.attestationca.persist;

import hirs.attestationca.servicemanager.DBDeviceManager;
import hirs.attestationca.servicemanager.DBReportRequestStateManager;
import hirs.attestationca.entity.Device;
import hirs.attestationca.data.persist.DeviceTest;
import hirs.attestationca.entity.ReportRequestState;
import hirs.data.persist.type.ReportRequestType;
import hirs.persist.DBManagerException;
import hirs.persist.DBUtility;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Test class for {@link hirs.persist.DBReportRequestStateManager}.
 */
public class DBReportRequestStateManagerTest extends SpringPersistenceTest {
    private static final int NUMBER_OF_DEVICES = 5;

    private List<Device> testDevices = new ArrayList<>();

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an in-memory database that
     * is used for testing.
     *
     * @throws Exception if there is a problem instantiating a Device
     */
    @BeforeClass
    public final void setup() throws Exception {
        Device testDevice;
        DBDeviceManager dbDeviceManager = new DBDeviceManager(sessionFactory);
        for (int i = 0; i < NUMBER_OF_DEVICES; i++) {
            testDevice = DeviceTest.getTestDevice("Device " + i);
            testDevices.add(dbDeviceManager.save(testDevice));
        }
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterClass
    public final void tearDown() {

    }

    /**
     * Resets the test state to a known good state. This currently only resets the database by
     * removing all <code>ReportRequestState</code> objects.
     */
    @AfterMethod
    public final void resetTestState() {
        DBUtility.removeAllInstances(sessionFactory, ReportRequestState.class);
    }

    /**
     * This method tests that a report request state can be stored for a device, and then
     * retrieved.
     */
    @Test
    public final void getStoredStateForDevice() {
        DBReportRequestStateManager mgr = new DBReportRequestStateManager(sessionFactory);
        Device device = testDevices.get(0);
        ReportRequestState state = getTestReportRequestState(device);
        state.setDueDate(ReportRequestState.MINUTE_MS_INTERVAL);
        mgr.saveState(state);

        ReportRequestState retrievedState = mgr.getState(device);
        Assert.assertNotNull(retrievedState);
        Assert.assertEquals(retrievedState.getDevice(), device);
    }

    /**
     * This method tests that getState() returns null if there was no state previously
     * associated with a Device.
     */
    @Test
    public final void testGetNonexistentState() {
        DBReportRequestStateManager mgr = new DBReportRequestStateManager(sessionFactory);
        Assert.assertNull(mgr.getState(testDevices.get(0)));
    }


    /**
     * This method tests that attempting to associate a new state with a Device that already has an
     * associated state will throw a DBManagerException.
     */
    @Test(expectedExceptions = DBManagerException.class)
    public final void testSaveAnotherStateForDevice() {
        DBReportRequestStateManager mgr = new DBReportRequestStateManager(sessionFactory);
        ReportRequestState state = getTestReportRequestState(
                testDevices.get(0)
        );
        mgr.saveState(state);
        ReportRequestState anotherState = getTestReportRequestState(
                testDevices.get(0)
        );
        mgr.saveState(anotherState);
    }

    /**
     * This method tests that updateState() will save an unpersisted object.
     */
    @Test
    public final void testUpdateNonexistentState() {
        DBReportRequestStateManager mgr = new DBReportRequestStateManager(sessionFactory);
        ReportRequestState newState = getTestReportRequestState(
                testDevices.get(0)
        );
        newState.setDueDate(ReportRequestState.MINUTE_MS_INTERVAL);
        mgr.update(newState);
        Assert.assertEquals(mgr.getState(testDevices.get(0)), newState);
    }


    /**
     * This method tests that updateState() will update an already-saved state.
     */
    @Test
    public final void testUpdateExistentState() {
        DBReportRequestStateManager mgr = new DBReportRequestStateManager(sessionFactory);
        ReportRequestState deviceState = getTestReportRequestState(testDevices.get(0));
        deviceState.setDueDate(ReportRequestState.MINUTE_MS_INTERVAL);
        ReportRequestState newState = mgr.saveState(deviceState);
        newState.setReportRequestType(ReportRequestType.ON_DEMAND_REPORT);
        mgr.update(newState);
        Assert.assertEquals(mgr.getState(testDevices.get(0)).getReportRequestType(),
                ReportRequestType.ON_DEMAND_REPORT);
    }

    /**
     * This method tests that attempting to delete a nonexistent state has no effect.
     */
    @Test
    public final void testDeleteNonexistentState() {
        DBReportRequestStateManager mgr = new DBReportRequestStateManager(sessionFactory);
        ReportRequestState state = getTestReportRequestState(testDevices.get(0));
        mgr.deleteState(state);
        Assert.assertEquals(mgr.getList(ReportRequestState.class).size(), 0);
    }

    /**
     * This method tests that deleting a previously persisted state functions properly.
     */
    @Test
    public final void testDeleteExistentState() {
        DBReportRequestStateManager mgr = new DBReportRequestStateManager(sessionFactory);
        ReportRequestState deviceState = getTestReportRequestState(testDevices.get(0));
        deviceState.setDueDate(ReportRequestState.MINUTE_MS_INTERVAL);
        ReportRequestState state = mgr.saveState(deviceState);
        mgr.deleteState(state);
        Assert.assertEquals(mgr.getList(ReportRequestState.class).size(), 0);
    }

    /**
     * Tests that the DB manager accurately provides a list of late devices based on the
     * check date.
     */
    @Test
    public final void testGetLateDeviceStates() {
        final int timeOffsetMs = 5000;
        Date checkDate = new Date();
        DBReportRequestStateManager mgr = new DBReportRequestStateManager(sessionFactory);
        ReportRequestState lateState = getTestReportRequestState(testDevices.get(0));
        lateState.setDueDate(new Date(checkDate.getTime() - timeOffsetMs));
        ReportRequestState nonLateState = getTestReportRequestState(testDevices.get(1));
        nonLateState.setDueDate(new Date(checkDate.getTime() + timeOffsetMs));

        mgr.saveState(lateState);
        mgr.saveState(nonLateState);

        List<ReportRequestState> lateDeviceStates = mgr.getLateDeviceStates();
        Assert.assertEquals(lateDeviceStates.size(), 1);

        Assert.assertEquals(lateDeviceStates.get(0).getDevice(), lateState.getDevice());
    }

    private static ReportRequestState getTestReportRequestState(final Device device) {
        ReportRequestState testState = new ReportRequestState();
        testState.setDevice(device);
        return testState;
    }
}
