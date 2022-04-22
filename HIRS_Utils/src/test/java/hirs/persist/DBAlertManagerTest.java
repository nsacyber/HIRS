package hirs.persist;

import hirs.FilteredRecordsList;
import hirs.data.persist.Alert;
import hirs.data.persist.baseline.Baseline;
import hirs.data.persist.Device;
import hirs.data.persist.DeviceGroup;
import hirs.data.persist.Report;
import hirs.data.persist.baseline.SimpleImaBaseline;
import hirs.data.persist.SpringPersistenceTest;
import hirs.data.persist.TestReport;
import hirs.data.persist.baseline.TpmWhiteListBaseline;
import hirs.data.persist.enums.AlertSeverity;
import hirs.data.persist.enums.AlertSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Unit tests for the DBAlertManager class.
 */
public final class DBAlertManagerTest extends SpringPersistenceTest {

    private static final Logger LOGGER = LogManager.getLogger(DBAlertManagerTest.class);
    private static final String ALERT_DETAILS = "Test Alert Details";
    private static final UUID UNKNOWN_ALERT
            = UUID.fromString("deadbeef-9dad-11d1-80b4-00c04fd430c8");
    private static final String TEST_BASELINE_NAME = "Alert Test Baseline";

    /**
     * Creates a new <code>DBAlertManagerTest</code>.
     */
    public DBAlertManagerTest() {
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
     * the database by removing all <code>Alert</code> objects.
     */
    @AfterMethod
    public void resetTestState() {
        DBUtility.removeAllInstances(sessionFactory, Alert.class);
        DBUtility.removeAllInstances(sessionFactory, TestReport.class);
        DBUtility.removeAllInstances(sessionFactory, Baseline.class);
    }

    /**
     * Tests that the <code>DBAlertManager</code> can save a
     * <code>Alert</code>.
     *
     * @throws AlertManagerException if any unexpected errors occur
     */
    @Test
    public void testSave() throws AlertManagerException {
        final Alert alert = new Alert(ALERT_DETAILS);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Alert.class), 0);
        final AlertManager mgr = new DBAlertManager(sessionFactory);
        final Alert a2 = mgr.saveAlert(alert);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Alert.class), 1);
        Assert.assertEquals(a2, alert);
        final UUID alertId = a2.getId();
        Assert.assertNotNull(alertId);
        Assert.assertTrue(DBUtility.isInDatabase(sessionFactory, Alert.class, "id", alertId));
    }

    /**
     * Tests that the <code>DBAlertManager</code> throws a
     * <code>AlertManagerException</code> if the alert parameter is null.
     *
     * @throws AlertManagerException if any unexpected errors occur
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testSaveNullAlert() throws AlertManagerException {
        final AlertManager mgr = new DBAlertManager(sessionFactory);
        mgr.saveAlert(null);
        Assert.fail("save did not fail");
    }

    /**
     * Tests that the {@link DBAlertManager} will return the total number of {@link Alert}s
     * associated with the id of a particular {@link Baseline} and a list of those {@link Alert}s.
     *
     * @throws AlertManagerException if any unexpected errors occur
     */
    @Test
    public void testGetAlertsForBaseline() throws AlertManagerException {
        final String baselineName = "baselineWithAlerts";
        final AlertManager mgr = new DBAlertManager(sessionFactory);
        final BaselineManager bMgr = new DBBaselineManager(sessionFactory);

        Baseline baseline = bMgr.saveBaseline(new SimpleImaBaseline(baselineName));

        Alert alert = new Alert("Alert One");
        alert.setBaselineIdsAndSeverity(Collections.singleton(baseline));

        Alert alertTwo = new Alert("Alert Two");
        alertTwo.setBaselineIdsAndSeverity(Collections.singleton(baseline));

        //Alert with baseline 'null' to verify "Alerts for Baseline" methods
        Alert alertThree = new Alert("Alert Three");

        mgr.saveAlert(alert);
        mgr.saveAlert(alertTwo);
        mgr.saveAlert(alertThree);

        //Asserts the return value is 2
        Assert.assertEquals(mgr.getTotalAlertsForBaseline(baseline), 2);

        //Asserts size of List
        Assert.assertEquals(mgr.getAlertsForBaseline(baseline).size(), 2);
    }

    /**
     * Tests that the query for getting the ordered list of alerts correclty distinguishes
     * resolved and unresolved alerts.
     *
     * @throws AlertManagerException if any unexpected errors occur
     */
    @Test
    public void testGetOrderedAlertListResolvedAndUnresolved() throws AlertManagerException {
        final AlertManager mgr = new DBAlertManager(sessionFactory);
        final String[] details = {"AlertOne", "AlertTwo", "AlertThree"};
        final Alert[] expectedAlerts = new Alert[details.length];
        final Map<String, Boolean> searchColumns = new HashMap<>();
        searchColumns.put("id", Boolean.FALSE);
        searchColumns.put("details", Boolean.TRUE);
        for (int i = 0; i < details.length; ++i) {
            expectedAlerts[i] = createAlert(mgr, details[i]);
        }

        List<Alert> alertsToResolve = new ArrayList<>();
        alertsToResolve.add(expectedAlerts[1]);
        mgr.resolveAlerts(alertsToResolve);

        final FilteredRecordsList<Alert> unresolvedAlerts =
                mgr.getOrderedAlertList("", "id", true, 0, 3,
                "", AlertManager.AlertListType.UNRESOLVED_ALERTS, searchColumns, null, null);
        Assert.assertEquals(unresolvedAlerts.size(), expectedAlerts.length - 1);

        List<UUID> alertIdList = Arrays.asList(unresolvedAlerts.get(0).getId(),
                unresolvedAlerts.get(1).getId());

        Assert.assertTrue(alertIdList.contains(expectedAlerts[0].getId()));
        Assert.assertTrue(alertIdList.contains(expectedAlerts[2].getId()));

        final FilteredRecordsList<Alert> resolvedAlerts =
                mgr.getOrderedAlertList("", "id", true, 0, 3,
                        "", AlertManager.AlertListType.RESOLVED_ALERTS, searchColumns, null, null);
        Assert.assertEquals(resolvedAlerts.size(), 1);
        Assert.assertEquals(resolvedAlerts.get(0).getId(), expectedAlerts[1].getId());

    }

    /**
     * Tests that a list of <code>Alert</code> names can be retrieved from
     * the repository based on the ID column and in ascending order.
     *
     * @throws AlertManagerException if any unexpected errors occur
     */
    @Test
    public void testGetOrderedAlertListAsc() throws AlertManagerException {
        final AlertManager mgr = new DBAlertManager(sessionFactory);
        final String[] details = {"AlertOne", "AlertTwo", "AlertThree"};
        final Alert[] expectedAlerts = new Alert[details.length];
        final Map<String, Boolean> searchColumns = new HashMap<>();
        searchColumns.put("id", Boolean.FALSE);
        searchColumns.put("details", Boolean.TRUE);
        for (int i = 0; i < details.length; ++i) {
            expectedAlerts[i] = createAlert(mgr, details[i]);
        }
        final FilteredRecordsList<Alert> alerts = mgr.getOrderedAlertList("", "id", true, 0, 3,
                "", AlertManager.AlertListType.UNRESOLVED_ALERTS, searchColumns, null, null);
        Assert.assertEquals(alerts.size(), expectedAlerts.length);
        String idOne = alerts.get(0).getId().toString();
        String idTwo = alerts.get(1).getId().toString();
        String idThree = alerts.get(2).getId().toString();
        Assert.assertTrue(idOne.compareTo(idTwo) < 0);
        Assert.assertTrue(idTwo.compareTo(idThree) < 0);
    }

    /**
     * Tests that a list of <code>Alert</code>s can be retrieved from
     * the repository based on the ID column and in descending order.
     *
     * @throws AlertManagerException if any unexpected errors occur
     */
    @Test
    public void testGetOrderedAlertListDesc() throws AlertManagerException {
        final AlertManager mgr = new DBAlertManager(sessionFactory);
        final String[] details = {"AlertOne", "AlertTwo", "AlertThree"};
        final Alert[] expectedAlerts = new Alert[details.length];
        final Map<String, Boolean> searchColumns = new HashMap<>();
        searchColumns.put("id", Boolean.FALSE);
        searchColumns.put("details", Boolean.TRUE);
        for (int i = 0; i < details.length; ++i) {
            expectedAlerts[i] = createAlert(mgr, details[i]);
        }
        final FilteredRecordsList<Alert> alerts = mgr.getOrderedAlertList("", "id", false, 0, 3,
                "", AlertManager.AlertListType.UNRESOLVED_ALERTS, searchColumns, null, null);
        Assert.assertEquals(alerts.size(), expectedAlerts.length);
        String idOne = alerts.get(0).getId().toString();
        String idTwo = alerts.get(1).getId().toString();
        String idThree = alerts.get(2).getId().toString();
        Assert.assertTrue(idOne.compareTo(idTwo) > 0);
        Assert.assertTrue(idTwo.compareTo(idThree) > 0);
    }

    /**
     * Tests that a list of <code>Alert</code>s can be retrieved from the
     * repository based on searching through the strings in the displayed
     * columns.
     *
     * @throws AlertManagerException if any unexpected errors occur
     */
    @Test
    public void testGetOrderedAlertListSearch() throws AlertManagerException {
        final AlertManager mgr = new DBAlertManager(sessionFactory);
        final String[] details = {"AlertOne", "AlertTwo", "AlertThree"};
        final Map<String, Boolean> searchColumns = new HashMap<>();
        searchColumns.put("id", Boolean.FALSE);
        searchColumns.put("details", Boolean.TRUE);
        for (int i = 0; i < details.length; ++i) {
            createAlert(mgr, details[i]);
        }
        final FilteredRecordsList<Alert> alerts = mgr.getOrderedAlertList(null, "id", false, 0,
                3, "Three", AlertManager.AlertListType.UNRESOLVED_ALERTS, searchColumns,
                null, null);
        Assert.assertEquals(alerts.size(), 1);
        Assert.assertEquals(alerts.get(0).getDetails(), "AlertThree");
    }

    /**
     * Tests that a list of <code>Alert</code>s can be retrieved from the
     * repository based on paging the results by telling the query to start
     * at record x and a set number of records.
     *
     * @throws AlertManagerException if any unexpected errors occur
     */
    @Test
    public void testGetOrderedAlertListPaging() throws AlertManagerException {
        LOGGER.debug("testGetOrderedListPaging test started");
        final AlertManager mgr = new DBAlertManager(sessionFactory);
        final String[] details = {"AlertOne", "AlertTwo", "AlertThree"};
        final Map<String, Boolean> searchColumns = new HashMap<>();
        searchColumns.put("id", Boolean.FALSE);
        searchColumns.put("details", Boolean.TRUE);
        for (int i = 0; i < details.length; ++i) {
            createAlert(mgr, details[i]);
        }
        final FilteredRecordsList<Alert> alerts = mgr.getOrderedAlertList("", "id", false, 2, 3,
                "", AlertManager.AlertListType.UNRESOLVED_ALERTS, searchColumns, null, null);
        Assert.assertEquals(alerts.size(), 1);
    }

    /**
     * Tests that a list of <code>Alert</code>s can be retrieved from the
     * repository even when a null report ID is passed in.
     *
     * @throws AlertManagerException if any unexpected errors occur
     */
    @Test
    public void testGetOrderedAlertListNullReport() throws AlertManagerException {
        final AlertManager mgr = new DBAlertManager(sessionFactory);
        final String[] details = {"AlertOne", "AlertTwo", "AlertThree"};
        final Map<String, Boolean> searchColumns = new HashMap<>();
        searchColumns.put("id", Boolean.FALSE);
        searchColumns.put("details", Boolean.TRUE);
        for (int i = 0; i < details.length; ++i) {
            createAlert(mgr, details[i]);
        }
        final FilteredRecordsList<Alert> alerts = mgr.getOrderedAlertList(null, "id", false, 0, 3,
                "", AlertManager.AlertListType.UNRESOLVED_ALERTS, searchColumns, null, null);
        Assert.assertEquals(alerts.size(), 3);
    }

    /**
     * Tests that a list of <code>Alert</code>s can be retrieved for a specific report.
     *
     * @throws AlertManagerException if any unexpected errors occur.
     */
    @Test
    public void testGetOrderedAlertListForReport() throws AlertManagerException {
        final AlertManager mgr = new DBAlertManager(sessionFactory);
        final ReportManager reportManager = new DBReportManager(sessionFactory);

        Report report1 = new TestReport();
        Report report2 = new TestReport();
        reportManager.saveReport(report1);
        reportManager.saveReport(report2);

        // verify these were persisted with different IDs.
        Assert.assertNotEquals(report1.getId(), report2.getId());

        final String[] details = {"AlertOne", "AlertTwo", "AlertThree"};
        final Map<String, Boolean> searchColumns = new HashMap<>();
        searchColumns.put("id", Boolean.FALSE);
        searchColumns.put("details", Boolean.TRUE);
        for (int i = 0; i < details.length; ++i) {

            if (i == 0) {
                createAlert(mgr, details[i], report1);
            } else {
                createAlert(mgr, details[i], report2);
            }
        }
        final FilteredRecordsList<Alert> report1Alerts =
                mgr.getOrderedAlertList(report1.getId().toString(),
                        "id", false, 0, 3, "", AlertManager.AlertListType.UNRESOLVED_ALERTS,
                        searchColumns, null, null);
        final FilteredRecordsList<Alert> report2Alerts =
                mgr.getOrderedAlertList(report2.getId().toString(),
                        "id", false, 0, 3, "", AlertManager.AlertListType.UNRESOLVED_ALERTS,
                        searchColumns, null, null);

        Assert.assertEquals(report1Alerts.size(), 1);
        Assert.assertEquals(report2Alerts.size(), 2);
    }

    /**
     * Tests that a list of <code>Alert</code>s can be resolved by calling
     * resolveAlerts() from the DBAlertManager.
     *
     * @throws AlertManagerException if any unexpected errors occur
     */
    @Test
    public void testResolveAlerts() throws AlertManagerException {
        final AlertManager mgr = new DBAlertManager(sessionFactory);
        final String[] details = {"AlertOne", "AlertTwo", "AlertThree"};
        final Alert[] expectedAlerts = new Alert[details.length];
        for (int i = 0; i < details.length; ++i) {
            expectedAlerts[i] = createAlert(mgr, details[i]);
            Assert.assertEquals(expectedAlerts[i].isArchived(), false);
        }
        mgr.resolveAlerts(Arrays.asList(expectedAlerts));
        Alert alertOne = mgr.getAlert(expectedAlerts[0].getId());
        Alert alertTwo = mgr.getAlert(expectedAlerts[1].getId());
        Alert alertThree = mgr.getAlert(expectedAlerts[2].getId());
        Assert.assertTrue(alertOne.isArchived());
        Assert.assertTrue(alertTwo.isArchived());
        Assert.assertTrue(alertThree.isArchived());
    }

    /**
     * Tests that a list of <code>Alert</code>s can be resolved by calling resolveAlerts() from the
     * DBAlertManager and that a provided description will be applied to them as well.
     *
     * @throws AlertManagerException if any unexpected errors occur
     */
    @Test
    public void testResolveAlertsWithDescription() throws AlertManagerException {
        final AlertManager mgr = new DBAlertManager(sessionFactory);
        final String description = "Record was added to baseline.";
        final String[] details = {"AlertOne", "AlertTwo", "AlertThree"};
        final Alert[] expectedAlerts = new Alert[details.length];
        for (int i = 0; i < details.length; ++i) {
            expectedAlerts[i] = createAlert(mgr, details[i]);
            Assert.assertEquals(expectedAlerts[i].isArchived(), false);
        }
        mgr.resolveAlerts(Arrays.asList(expectedAlerts), description);
        Alert alertOne = mgr.getAlert(expectedAlerts[0].getId());
        Alert alertTwo = mgr.getAlert(expectedAlerts[1].getId());
        Alert alertThree = mgr.getAlert(expectedAlerts[2].getId());
        Assert.assertTrue(alertOne.isArchived());
        Assert.assertTrue(alertTwo.isArchived());
        Assert.assertTrue(alertThree.isArchived());
        Assert.assertEquals(alertOne.getArchivedDescription(), description);
        Assert.assertEquals(alertTwo.getArchivedDescription(), description);
        Assert.assertEquals(alertThree.getArchivedDescription(), description);
    }

    /**
     * Tests that a null list of type <code>Alert</code>s will be handled correctly if they are
     * attempted to be resolved by calling resolveAlerts() from the DBAlertManager.
     *
     * @throws AlertManagerException if any unexpected errors occur
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testResolveAlertsNull() throws AlertManagerException {
        final AlertManager mgr = new DBAlertManager(sessionFactory);
        List<Alert> expectedAlerts = null;
        mgr.resolveAlerts(expectedAlerts);
    }

    /**
     * Tests that the <code>DBAlertManager</code> can get a
     * <code>Alert</code>.
     *
     * @throws AlertManagerException if any unexpected errors occur
     */
    @Test
    public void testGet() throws AlertManagerException {
        final AlertManager mgr = new DBAlertManager(sessionFactory);
        final Alert testAlert = new Alert(ALERT_DETAILS);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Alert.class), 0);
        final Alert alert = mgr.saveAlert(testAlert);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Alert.class), 1);
        final Alert getAlert = mgr.getAlert(alert.getId());
        Assert.assertNotNull(getAlert);
        Assert.assertEquals(getAlert, alert);
        Assert.assertEquals(getAlert.getId(), alert.getId());
    }

    /**
     * Tests that the <code>DBAlertManager</code> returns null when an
     * unknown <code>Alert</code> is searched for.
     *
     * @throws AlertManagerException if any unexpected errors occur
     */
    @Test
    public void testGetUnknown() throws AlertManagerException {
        final AlertManager mgr = new DBAlertManager(sessionFactory);
        Assert.assertNull(mgr.getAlert(UNKNOWN_ALERT));
    }

    /**
     * Tests that a list of <code>Alert</code> names can be retrieved from
     * the repository.
     *
     * @throws AlertManagerException if any unexpected errors occur
     */
    @Test
    public void testGetAlertList() throws AlertManagerException {
        final AlertManager mgr = new DBAlertManager(sessionFactory);
        final String[] details = {"Alert1", "Alert2", "Alert3"};
        final Alert[] expectedAlerts = new Alert[details.length];
        for (int i = 0; i < details.length; ++i) {
            expectedAlerts[i] = createAlert(mgr, details[i]);
        }
        final List<Alert> alerts = mgr.getAlertList();
        Assert.assertEquals(alerts.size(), expectedAlerts.length);
        for (int i = 0; i < expectedAlerts.length; ++i) {
            Assert.assertTrue(alerts.contains(expectedAlerts[i]));
        }
    }

    /**
     * Tests that the unresolved alert count is zero when the alert table is empty.
     *
     * @throws AlertManagerException
     */
    @Test
    public void testCountUnresolvedAlertsEmpty() {
        final DBAlertManager mgr = new DBAlertManager(sessionFactory);
        Assert.assertEquals(mgr.getAlertList().size(), 0);
        Assert.assertEquals(mgr.countUnresolvedAlerts(new Device("test-server")), 0);
    }

    /**
     * Tests that the unresolved alert count is correct when the alert table has some unresolved
     * alerts.
     *
     * @throws AlertManagerException
     */
    @Test
    public void testCountUnresolvedAlertsUnresolved() {
        String deviceName = "test-server";
        Device device = new Device(deviceName);
        final DBAlertManager mgr = new DBAlertManager(sessionFactory);
        final String[] details = {"Alert1", "Alert2", "Alert3"};
        final Alert[] expectedAlerts = new Alert[details.length];
        for (int i = 0; i < details.length; ++i) {
            expectedAlerts[i] = new Alert(details[i]);
            expectedAlerts[i].setDeviceName(deviceName);
            mgr.saveAlert(expectedAlerts[i]);
        }

        Assert.assertEquals(mgr.countUnresolvedAlerts(device), details.length);
    }

    /**
     * Tests that the unresolved alert count is zero when the alert table has some resolved alerts.
     *
     * @throws AlertManagerException
     */
    @Test
    public void testCountUnresolvedAlertsResolved() {
        String deviceName = "test-server";
        Device device = new Device(deviceName);
        final DBAlertManager mgr = new DBAlertManager(sessionFactory);
        final String[] details = {"Alert1", "Alert2", "Alert3"};
        final Alert[] expectedAlerts = new Alert[details.length];
        for (int i = 0; i < details.length; ++i) {
            expectedAlerts[i] = new Alert(details[i]);
            expectedAlerts[i].setDeviceName(deviceName);
            expectedAlerts[i].archive();
            mgr.saveAlert(expectedAlerts[i]);
        }

        Assert.assertEquals(mgr.countUnresolvedAlerts(device), 0);
    }

    /**
     * Tests that the unresolved alert count is correct when the alert table has some resolved
     * alerts and some unresolved alerts.
     *
     * @throws AlertManagerException
     */
    @Test
    public void testCountUnresolvedAlertsMixed() {

        String deviceName = "test-server";
        Device device = new Device(deviceName);
        final DBAlertManager mgr = new DBAlertManager(sessionFactory);

        // create some resolved alerts and some unresolved alerts
        final String[] resolvedDetails = {"Resolved1", "Resolved2"};
        final Alert[] resolvedAlerts = new Alert[resolvedDetails.length];
        for (int i = 0; i < resolvedDetails.length; ++i) {
            resolvedAlerts[i] = new Alert(resolvedDetails[i]);
            resolvedAlerts[i].setDeviceName(deviceName);
            resolvedAlerts[i].archive();
            mgr.saveAlert(resolvedAlerts[i]);
        }
        final String[] unresolvedDetails = {"Unresolved1", "Unresolved2", "Unresolved3"};
        final Alert[] unresolvedAlerts = new Alert[unresolvedDetails.length];
        for (int i = 0; i < unresolvedDetails.length; ++i) {
            unresolvedAlerts[i] = new Alert(unresolvedDetails[i]);
            unresolvedAlerts[i].setDeviceName(deviceName);
            mgr.saveAlert(unresolvedAlerts[i]);
        }

        Assert.assertEquals(mgr.countUnresolvedAlerts(device), unresolvedDetails.length);
    }

    /**
     * Tests that the unresolved alert count is correct when the alert table has some resolved
     * alerts and some unresolved alerts for multiple devices.
     *
     * @throws AlertManagerException
     */
    @Test
    public void testCountUnresolvedAlertsMultipleDevices() {

        String device1Name = "test-server1";
        Device device1 = new Device(device1Name);
        String device2Name = "test-server2";
        Device device2 = new Device(device2Name);

        final DBAlertManager mgr = new DBAlertManager(sessionFactory);

        // create some resolved alerts and some unresolved alerts for device1
        Alert newAlert;
        final String[] resolvedDetails1 = {"Resolved1", "Resolved2"};
        final ArrayList<Alert> resolvedAlerts1 = new ArrayList<>();
        for (int i = 0; i < resolvedDetails1.length; ++i) {
            newAlert = new Alert(resolvedDetails1[i]);
            newAlert.setDeviceName(device1Name);
            mgr.saveAlert(newAlert);
            resolvedAlerts1.add(newAlert);
        }
        mgr.resolveAlerts(resolvedAlerts1);

        final String[] unresolvedDetails1 = {"Unresolved1", "Unresolved2", "Unresolved3"};
        for (int i = 0; i < unresolvedDetails1.length; ++i) {
            newAlert = new Alert(unresolvedDetails1[i]);
            newAlert.setDeviceName(device1Name);
            mgr.saveAlert(newAlert);
        }

        // create some resolved alerts and some unresolved alerts for device2
        final String[] resolvedDetails2 = {"Resolved3", "Resolved4", "Resolved5", "Resolved6"};
        final ArrayList<Alert> resolvedAlerts2 = new ArrayList<>();
        for (int i = 0; i < resolvedDetails2.length; ++i) {
            newAlert = new Alert(resolvedDetails2[i]);
            newAlert.setDeviceName(device2Name);
            mgr.saveAlert(newAlert);
            resolvedAlerts2.add(newAlert);
        }
        mgr.resolveAlerts(resolvedAlerts2);

        final String[] unresolvedDetails2 = {"Unresolved4", "Unresolved5", "Unresolved6",
                "Unresolved7", "Unresolved8"};
        for (int i = 0; i < unresolvedDetails2.length; ++i) {
            newAlert = new Alert(unresolvedDetails2[i]);
            newAlert.setDeviceName(device2Name);
            mgr.saveAlert(newAlert);
        }

        Assert.assertEquals(mgr.countUnresolvedAlerts(device1), unresolvedDetails1.length);
        Assert.assertEquals(mgr.countUnresolvedAlerts(device2), unresolvedDetails2.length);
    }

    /**
     * Tests that the unresolved alert count is correct when the alert table has some unresolved
     * alerts from two different alert sources.
     *
     * @throws AlertManagerException
     */
    @Test
    public void testCountUnresolvedAlertsMultipleSources() {

        String deviceName = "test-server";
        Device device = new Device(deviceName);

        final DBAlertManager mgr = new DBAlertManager(sessionFactory);
        Alert newAlert;

        final String[] unresolvedDetails1 = {"Unresolved1", "Unresolved2", "Unresolved3"};
        for (int i = 0; i < unresolvedDetails1.length; ++i) {
            newAlert = new Alert(unresolvedDetails1[i]);
            newAlert.setDeviceName(deviceName);
            newAlert.setSource(AlertSource.IMA_APPRAISER);
            mgr.saveAlert(newAlert);
        }

        final String[] unresolvedDetails2 = {"Unresolved4", "Unresolved5", "Unresolved6",
                "Unresolved7", "Unresolved8"};
        for (int i = 0; i < unresolvedDetails2.length; ++i) {
            newAlert = new Alert(unresolvedDetails2[i]);
            newAlert.setDeviceName(deviceName);
            newAlert.setSource(AlertSource.TPM_APPRAISER);
            mgr.saveAlert(newAlert);
        }

        Assert.assertEquals(mgr.countUnresolvedAlerts(device, AlertSource.IMA_APPRAISER),
                unresolvedDetails1.length);
        Assert.assertEquals(mgr.countUnresolvedAlerts(device, AlertSource.TPM_APPRAISER),
                unresolvedDetails2.length);
    }

    /**
     * Tests the performance impact of counting unresolved alerts. The maxMagnitude variable is used
     * to set the upper limit of how many alerts to save in the database. If maxMagnitude is set to
     * x, this test will measure the time taken to generate 10^0 + 10^1 + ... + 10^x alerts and save
     * them to the database. Setting maxMagnitude to 5 on development machine seems to take about 7
     * seconds and setting it to 6 seems to take about 48 seconds.
     */
    @Test(groups = { "performance" })
    public void testCountUnresolvedAlertsPerformance() {

        String baseName = "test-server";
        final DBAlertManager mgr = new DBAlertManager(sessionFactory);
        long startTime;
        long endTime;

        int maxMagnitude = 5;
        for (int x = 0; x < maxMagnitude + 1; x++) {
            startTime = System.currentTimeMillis();
            String deviceName = String.format("%s%d", baseName, x);
            Device device = new Device(deviceName);
            int n = (int) Math.pow((double) 10, (double) x);
            int resolved = n / 2;
            LOGGER.info("n is {}", n);
            // create and save n alerts, resolving the first half
            for (int i = 0; i < n; i++) {
                Alert alert = new Alert(String.format("%s%d", deviceName, i));
                alert.setDeviceName(deviceName);
                if (i < resolved) {
                    alert.archive();
                }
                mgr.saveAlert(alert);
            }
            endTime = System.currentTimeMillis();
            LOGGER.info("10^{} setup took {} milliseconds", x, endTime - startTime);
            startTime = System.currentTimeMillis();
            Assert.assertEquals(mgr.countUnresolvedAlerts(device), n - resolved);
            endTime = System.currentTimeMillis();
            LOGGER.info("10^{} query took {} milliseconds", x, endTime - startTime);
        }
    }

    /**
     * Tests that the number of devices with unresolved alerts can be counted correctly.
     */
//    @Test
    public void testCountUnresolvedDevices() {

        final DBAlertManager alertManager = new DBAlertManager(sessionFactory);

        final DBDeviceGroupManager groupManager = new DBDeviceGroupManager(sessionFactory);
        DeviceGroup deviceGroup = new DeviceGroup("test device group");
        DeviceGroup defaultGroup = groupManager.getDeviceGroup(DeviceGroup.DEFAULT_GROUP);
        if (defaultGroup == null) {
            defaultGroup = groupManager.saveDeviceGroup(new DeviceGroup(DeviceGroup.DEFAULT_GROUP));
        }
        final DBDeviceManager deviceManager = new DBDeviceManager(sessionFactory);
        final String[] deviceNames = {"device1", "device2", "device3", "device4", "device5"};

        // create some devices and throw an alert for each one
        final ArrayList<Device> devices = new ArrayList<>();
        final ArrayList<Alert> alerts = new ArrayList<>();
        for (int i = 0; i < deviceNames.length; i++) {
            Device device = new Device(deviceNames[i]);
            device.setDeviceGroup(defaultGroup);
            device = deviceManager.saveDevice(device);
            devices.add(device);
            deviceGroup.addDevice(device);
            Alert alert = new Alert("some alert");
            alerts.add(alert);
            alert.setDeviceName(deviceNames[i]);
            alertManager.save(alert);
        }

        deviceGroup = groupManager.saveDeviceGroup(deviceGroup);
        deviceManager.updateDeviceList(new HashSet<>(devices));

//        Assert.assertEquals(alertManager.countUnresolvedDevices(deviceGroup),
//                devices.size());

        // resolve the alerts for some of the devices
        final ArrayList<Alert> alertsToResolve = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            alertsToResolve.add(alerts.get(i));
            devices.remove(i);
        }
        alertManager.resolveAlerts(alertsToResolve);
//        Assert.assertEquals(alertManager.countUnresolvedDevices(deviceGroup),
//                devices.size());
    }

    /**
     * Tests the null <code>DeviceGroup</code> check in countUnresolvedDevices.
     */
//    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCountUnresolvedDevicesNullDeviceGroup() {
        final DBAlertManager alertManager = new DBAlertManager(sessionFactory);
//        alertManager.countUnresolvedDevices(null);
    }

    private Alert createAlert(final AlertManager mgr,
            final String details) throws AlertManagerException {
        return createAlert(mgr, details, null);
    }

    private Alert createAlert(final AlertManager mgr,
                              final String details, final Report report)
            throws AlertManagerException {
        LOGGER.debug("creating alert in db");
        String alertDetails = details;
        if (details == null) {
            alertDetails = ALERT_DETAILS;
        }
        final Alert alert = new Alert(alertDetails);
        alert.setReport(report);
        return mgr.saveAlert(alert);
    }

    /**
     * Test that the id and severity of a collection of <code>Baseline</code>s can be set and
     * retrieved.
     */
    @Test
    public void testBaselineIdsAndSeverity() {
        Alert alert = new Alert(ALERT_DETAILS);
        Set<Baseline> baselines = initBaselines(AlertSeverity.SEVERE, AlertSeverity.SEVERE,
                AlertSeverity.SEVERE, AlertSeverity.SEVERE);

        alert.setBaselineIdsAndSeverity(baselines);
        Set<UUID> alertBaselines = alert.getBaselineIds();

        for (UUID id : alertBaselines) {
            Assert.assertTrue(foundMatchingBaselineId(id, baselines));
        }

        Assert.assertEquals(alert.getSeverity(), AlertSeverity.SEVERE);
    }

    /**
     * Test that the most critical severity level associated with the alert is retrieved.  The most
     * critical severity level is SEVERE.
     */
    @Test
    public void testBaselineIdsAndSevereSeverity() {
        Alert alert = new Alert(ALERT_DETAILS);
        Set<Baseline> baselines = initBaselines(AlertSeverity.SEVERE, AlertSeverity.HIGH,
                AlertSeverity.INFO, AlertSeverity.UNSPECIFIED);

        alert.setBaselineIdsAndSeverity(baselines);
        Set<UUID> alertBaselines = alert.getBaselineIds();

        for (UUID id : alertBaselines) {
            Assert.assertTrue(foundMatchingBaselineId(id, baselines));
        }

        Assert.assertEquals(alert.getSeverity(), AlertSeverity.SEVERE);
    }

    /**
     * Test that the most critical severity level associated with the alert is retrieved. The most
     * critical severity level is HIGH.
     */
    @Test
    public void testBaselineIdsAndHighSeverity() {
        Alert alert = new Alert(ALERT_DETAILS);
        Set<Baseline> baselines = initBaselines(AlertSeverity.INFO, AlertSeverity.HIGH,
                AlertSeverity.INFO, AlertSeverity.UNSPECIFIED);

        alert.setBaselineIdsAndSeverity(baselines);
        Set<UUID> alertBaselines = alert.getBaselineIds();

        for (UUID id : alertBaselines) {
            Assert.assertTrue(foundMatchingBaselineId(id, baselines));
        }

        Assert.assertEquals(alert.getSeverity(), AlertSeverity.HIGH);
    }

    /**
     * Test that the most critical severity level associated with the alert is retrieved.  The most
     * critical severity level is LOW.
     */
    @Test
    public void testBaselineIdsAndLowSeverity() {
        Alert alert = new Alert(ALERT_DETAILS);
        Set<Baseline> baselines = initBaselines(AlertSeverity.INFO, AlertSeverity.LOW,
                AlertSeverity.INFO, AlertSeverity.UNSPECIFIED);

        alert.setBaselineIdsAndSeverity(baselines);
        Set<UUID> alertBaselines = alert.getBaselineIds();

        for (UUID id : alertBaselines) {
            Assert.assertTrue(foundMatchingBaselineId(id, baselines));
        }

        Assert.assertEquals(alert.getSeverity(), AlertSeverity.LOW);
    }

    /**
     * Test that the most critical severity level associated with the alert is retrieved. The most
     * critical severity level is INFO.
     */
    @Test
    public void testBaselineIdsAndInfoSeverity() {
        Alert alert = new Alert(ALERT_DETAILS);
        Set<Baseline> baselines = initBaselines(AlertSeverity.INFO, AlertSeverity.INFO,
                AlertSeverity.INFO, AlertSeverity.UNSPECIFIED);

        alert.setBaselineIdsAndSeverity(baselines);
        Set<UUID> alertBaselines = alert.getBaselineIds();

        for (UUID id : alertBaselines) {
            Assert.assertTrue(foundMatchingBaselineId(id, baselines));
        }

        Assert.assertEquals(alert.getSeverity(), AlertSeverity.INFO);
    }

    private boolean foundMatchingBaselineId(final UUID baselineId, final Collection<Baseline>
            tpmBaselines) {
        for (Baseline baseline : tpmBaselines) {
            if (baseline.getId() == baselineId) {
                return true;
            }
        }
        return false;
    }
    private Set<Baseline> initBaselines(final AlertSeverity severity,
                                        final AlertSeverity severity2,
                                        final AlertSeverity severity3,
                                        final AlertSeverity severity4) {
        final BaselineManager bMgr = new DBBaselineManager(sessionFactory);
        Baseline baseline = bMgr.saveBaseline(new TpmWhiteListBaseline(TEST_BASELINE_NAME + "1"));
        Baseline baseline2 = bMgr.saveBaseline(new TpmWhiteListBaseline(TEST_BASELINE_NAME + "2"));
        Baseline baseline3 = bMgr.saveBaseline(new TpmWhiteListBaseline(TEST_BASELINE_NAME + "3"));
        Baseline baseline4 = bMgr.saveBaseline(new TpmWhiteListBaseline(TEST_BASELINE_NAME + "4"));

        baseline.setSeverity(severity);
        baseline2.setSeverity(severity2);
        baseline3.setSeverity(severity3);
        baseline4.setSeverity(severity4);

        Set<Baseline> baselines = new HashSet<>();
        baselines.add(baseline);
        baselines.add(baseline2);
        baselines.add(baseline3);
        baselines.add(baseline4);

        return baselines;
    }
}
