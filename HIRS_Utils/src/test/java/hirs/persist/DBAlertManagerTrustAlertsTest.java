package hirs.persist;

import hirs.data.persist.Alert;
import hirs.data.persist.Device;
import hirs.data.persist.DeviceGroup;
import hirs.data.persist.IntegrityReport;
import hirs.data.persist.Report;

import hirs.data.persist.SpringPersistenceTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import hirs.data.persist.TestReport;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for the DBAlertManager class that test the getTrustAlerts() method.
 */
public class DBAlertManagerTrustAlertsTest extends SpringPersistenceTest {

    private static final Logger LOGGER = LogManager.getLogger(DBAlertManagerTrustAlertsTest.class);

    private AlertManager mgr;
    private DeviceGroup group;

    private Device device1;
    private Device device2;
    private Report report1D1;
    private Report report2D1;
    private Report report1D2;

    private Alert alert1;
    private Alert alert2;
    private Alert alert3;
    private Alert alert4;
    private Alert alert5;
    private Alert reportlessAlert;

    /**
     * Initializes test data for the tests.
     */
    @BeforeMethod
    public void setup() {
        mgr = new DBAlertManager(sessionFactory);
        ReportManager reportManager = new DBReportManager(sessionFactory);
        DeviceManager deviceManager = new DBDeviceManager(sessionFactory);
        group = createGroup(DeviceGroup.DEFAULT_GROUP);

        device1 = new Device("Number1");
        device1.setDeviceGroup(group);
        device2 = new Device("2");
        device2.setDeviceGroup(group);

        deviceManager.saveDevice(device1);
        deviceManager.saveDevice(device2);

        report1D1 = new TestReport();
        report2D1 = new TestReport();
        report1D2 = new TestReport();

        reportManager.saveReport(report1D1);
        reportManager.saveReport(report2D1);
        reportManager.saveReport(report1D2);

        alert1 = createAlert(mgr, "aaa1", report1D1, device1);
        alert2 = createAlert(mgr, "aaa2", report1D1, device1);
        alert3 = createAlert(mgr, "bbb2", report1D1, device1);
        alert4 = createAlert(mgr, "aaa3", report2D1, device1);
        alert5 = createAlert(mgr, "aaa55", report1D2, device2);
        reportlessAlert = createAlert(mgr, "aaa4", null, device1);

        List<Alert> allAlerts = mgr.getAlertList();
        Assert.assertEquals(allAlerts.size(), 6);
    }

    /**
     * Resets the test state to a known good state.
     */
    @AfterMethod
    public void resetTestState() {
        DBUtility.removeAllInstances(sessionFactory, Alert.class);
        DBUtility.removeAllInstances(sessionFactory, TestReport.class);
        DBUtility.removeAllInstances(sessionFactory, Device.class);
        DBUtility.removeAllInstances(sessionFactory, Report.class);
        DBUtility.removeAllInstances(sessionFactory, IntegrityReport.class);
        DBUtility.removeAllInstances(sessionFactory, DeviceGroup.class);
    }

    /**
     * Tests that getting the trusted alerts with a null device throws an exception.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void getTrustAlertsNullDeviceException() {
        mgr.getTrustAlerts(null, new TestReport(), null);
    }

    /**
     * Tests that getting the trusted alert count with a null device throws an exception.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void getTrustAlertCountNullDeviceException() {
        mgr.getTrustAlertCount(null, new TestReport(), null);
    }

    /**
     * Tests that getting the trusted alerts with a null report throws an exception.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void getTrustAlertsNullReportException() {
        mgr.getTrustAlerts(new Device("test-device"), null, null);
    }

    /**
     * Tests that getting the trusted alert count with a null report throws an exception.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void getTrustAlertCountNullReportException() {
        mgr.getTrustAlertCount(new Device("test-device"), null, null);
    }

    /**
     * Tests that getting the trusted alerts and trusted alert count is accurate if there were
     * never any alerts.
     */
    @Test
    public void getTrustAlertsNoAlerts() {
        final int expectedAlertCount = 0;
        DeviceManager deviceManager = new DBDeviceManager(sessionFactory);
        ReportManager reportManager = new DBReportManager(sessionFactory);

        Device newDevice = new Device("new-device");
        newDevice.setDeviceGroup(group);

        deviceManager.saveDevice(newDevice);

        Report newReport = new TestReport();

        reportManager.saveReport(newReport);

        List<Alert> alerts = mgr.getTrustAlerts(newDevice, newReport, null);
        int alertCount = mgr.getTrustAlertCount(newDevice, newReport, null);
        Assert.assertEquals(alerts.size(), expectedAlertCount);
        Assert.assertEquals(alertCount, expectedAlertCount);
    }

    /**
     * Tests getting the trust alerts for a device and report.
     */
    @Test
    public void getTrustAlertsDevice1Report1() {
        final int expectedAlertCount = 4;
        List<Alert> report1TrustAlerts = mgr.getTrustAlerts(device1, report1D1, null);
        int alertCount = mgr.getTrustAlertCount(device1, report1D1, null);

        Assert.assertEquals(report1TrustAlerts.size(), expectedAlertCount);
        Assert.assertEquals(alertCount, expectedAlertCount);
        Assert.assertTrue(report1TrustAlerts.contains(alert1));
        Assert.assertTrue(report1TrustAlerts.contains(alert2));
        Assert.assertTrue(report1TrustAlerts.contains(alert3));
        Assert.assertTrue(report1TrustAlerts.contains(reportlessAlert));
    }

    /**
     * Tests getting the trust alerts for a device and report.
     */
    @Test
    public void getTrustAlertsDevice1Report2() {
        final int expectedAlertCount = 2;
        List<Alert> report2TrustAlerts = mgr.getTrustAlerts(device1, report2D1, null);
        int alertCount = mgr.getTrustAlertCount(device1, report2D1, null);

        Assert.assertEquals(report2TrustAlerts.size(), expectedAlertCount);
        Assert.assertEquals(alertCount, expectedAlertCount);
        Assert.assertTrue(report2TrustAlerts.contains(alert4));
        Assert.assertTrue(report2TrustAlerts.contains(reportlessAlert));
    }

    /**
     * Tests getting the trust alerts for a device and report.
     */
    @Test
    public void getTrustAlertsDevice2Report1() {
        final int expectedAlertCount = 1;
        List<Alert> device2TrustAlerts = mgr.getTrustAlerts(device2, report1D2, null);
        int alertCount = mgr.getTrustAlertCount(device2, report1D2, null);

        Assert.assertEquals(device2TrustAlerts.size(), expectedAlertCount);
        Assert.assertEquals(alertCount, expectedAlertCount);
        Assert.assertTrue(device2TrustAlerts.contains(alert5));
    }

    /**
     * Tests that when an alert is archived, it is not included in the set of
     * trust alerts.
     */
    @Test
    public void trustAlertsExcludeArchivedAlerts() {
        final int expectedAlertCountReport1 = 3;
        final int expectedAlertCountReport2 = 2;
        // archive an alert, and check the alert set.
        List<Alert> resolveList = new ArrayList<>();
        resolveList.add(alert2);
        mgr.resolveAlerts(resolveList);

        // alert 2 should not be present, since we just resolved it
        List<Alert> report1TrustAlerts = mgr.getTrustAlerts(device1, report1D1, null);
        int report1AlertCount = mgr.getTrustAlertCount(device1, report1D1, null);
        Assert.assertEquals(report1TrustAlerts.size(), expectedAlertCountReport1);
        Assert.assertEquals(report1AlertCount, expectedAlertCountReport1);
        Assert.assertTrue(report1TrustAlerts.contains(alert1));
        Assert.assertTrue(report1TrustAlerts.contains(alert3));
        Assert.assertTrue(report1TrustAlerts.contains(reportlessAlert));

        // the trust alerts for report 2 should be unchanged
        List<Alert> report2TrustAlerts = mgr.getTrustAlerts(device1, report2D1, null);
        int report2AlertCount = mgr.getTrustAlertCount(device1, report2D1, null);

        Assert.assertEquals(report2TrustAlerts.size(), expectedAlertCountReport2);
        Assert.assertEquals(report2AlertCount, expectedAlertCountReport2);
        Assert.assertTrue(report2TrustAlerts.contains(alert4));
        Assert.assertTrue(report2TrustAlerts.contains(reportlessAlert));
    }

    /**
     * Tests that using an optional critierion with the trust alert query
     * will include matched alerts.
     */
    @Test
    public void optionalCritierionUsedWithTrustAlerts() {
        final int expectedAlertCount = 5;
        // get alerts with an optional criterion (alert for this device,
        // but associated with a report other than report1D1
        Criterion criterion = Restrictions.eq("details", alert4.getDetails());
        List<Alert> report1CriteriaTrustAlerts = mgr.getTrustAlerts(device1, report1D1, criterion);
        int alertCount = mgr.getTrustAlertCount(device1, report1D1, criterion);

        Assert.assertEquals(report1CriteriaTrustAlerts.size(), expectedAlertCount);
        Assert.assertEquals(alertCount, expectedAlertCount);
        Assert.assertTrue(report1CriteriaTrustAlerts.contains(alert1));
        Assert.assertTrue(report1CriteriaTrustAlerts.contains(alert2));
        Assert.assertTrue(report1CriteriaTrustAlerts.contains(alert3));
        Assert.assertTrue(report1CriteriaTrustAlerts.contains(reportlessAlert));
        // this is the alert that should be included due to the criterion provided
        Assert.assertTrue(report1CriteriaTrustAlerts.contains(alert4));
    }

    private Alert createAlert(final AlertManager mgr,
                              final String details, final Report report, final Device device)
            throws AlertManagerException {
        LOGGER.debug("creating alert in db");
        String alertDetails = details;
        if (details == null) {
            alertDetails = "default";
        }
        final Alert alert = new Alert(alertDetails);
        alert.setReport(report);
        if (null != device) {
            alert.setDeviceName(device.getName());
        }
        return mgr.saveAlert(alert);
    }

    private DeviceGroup createGroup(final String name)  {
        DeviceGroup deviceGroup = new DeviceGroup(name);
        final DeviceGroupManager groupManager = new DBDeviceGroupManager(sessionFactory);
        return groupManager.saveDeviceGroup(deviceGroup);
    }
}
