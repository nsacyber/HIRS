package hirs.persist;

import hirs.data.persist.Alert;
import hirs.data.persist.Device;
import hirs.data.persist.DeviceGroup;
import hirs.data.persist.DeviceState;
import hirs.data.persist.enums.HealthStatus;
import hirs.data.persist.Report;
import hirs.data.persist.ReportSummary;

import org.hibernate.HibernateException;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Qualifier;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import hirs.data.persist.TestReport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link hirs.persist.DeviceHealthManagerImpl} class.
 */
public class DeviceHealthManagerImplTest {

    private static final String HOST_NAME = "known-device";

    @InjectMocks
    private DeviceHealthManagerImpl deviceHealthManager;

    @Mock
    private DeviceManager deviceManager;

    @Mock
    private AlertManager alertManager;

    @Mock
    @Qualifier(PersistenceConfiguration.DEVICE_STATE_MANAGER_BEAN_NAME)
    private DeviceStateManager deviceStateManager;

    @Mock
    private ReportSummaryManager reportSummaryManager;
    private DeviceGroup deviceGroup;
    private Device device;

    /**
     * Sets up the test state. This currently store a <code>Device</code> in the database.
     */
    @BeforeMethod
    public void setupTestState() {
        MockitoAnnotations.initMocks(this);

        deviceGroup = new DeviceGroup(DeviceGroup.DEFAULT_GROUP);
        device = new Device(HOST_NAME);
        device.setDeviceGroup(deviceGroup);
        when(deviceManager.getDevice(HOST_NAME)).thenReturn(device);
    }

    /**
     * Tests that an exception is thrown if attempting to update the health of a device
     * that doesn't exist.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void exceptionThrownIfDeviceNotFound() {
        deviceHealthManager.updateHealth("no-device-asdfsf");
    }

    /**
     * Tests that device health will be unknown if there are no reports for that device yet.
     */
    @Test
    public void deviceHealthUnknownWithNoReportForDevice() {
        deviceHealthManager.updateHealth(HOST_NAME);
        Assert.assertEquals(device.getHealthStatus(), HealthStatus.UNKNOWN);
    }

    /**
     * Tests that device health will be unknown if an Exception occurs while retrieving the report
     * summary.
     *
     * @throws Exception if getNewestReport is not mocked properly
     */
    @Test
    public void deviceHealthUnknownIfFailureToGetReportSummary() throws Exception {
        when(reportSummaryManager.getNewestReport(HOST_NAME))
                .thenThrow(new HibernateException("bad db"));

        deviceHealthManager.updateHealth(HOST_NAME);
        Assert.assertEquals(device.getHealthStatus(), HealthStatus.UNKNOWN);
    }

    /**
     * Tests that device health will be trusted if there were no alerts associated with the latest
     * report.
     *
     * @throws Exception if getNewestReport is not mocked properly
     */
    @Test
    public void deviceHealthTrustedIfNoAlertsForReport() throws Exception {
        ReportSummary summary = new ReportSummary();
        Report report = new TestReport();
        summary.setReport(report);
        when(reportSummaryManager.getNewestReport(HOST_NAME)).thenReturn(summary);

        deviceHealthManager.updateHealth(HOST_NAME);
        Assert.assertEquals(device.getHealthStatus(), HealthStatus.TRUSTED);
    }

    /**
     * Tests that device health will be untrusted if there were alerts for the latest report. Also
     * ensures that no additional criterion are used if there are no device states.
     *
     * @throws Exception if getNewestReport is not mocked properly
     */
    @Test
    public void deviceHealthUntrustedIfAlertsForReportAndNoDeviceStates() throws Exception {
        ReportSummary summary = new ReportSummary();
        Report report = new TestReport();
        summary.setReport(report);
        when(reportSummaryManager.getNewestReport(HOST_NAME)).thenReturn(summary);

        when(alertManager.getTrustAlertCount(device, report, null)).thenReturn(1);

        deviceHealthManager.updateHealth(HOST_NAME);
        Assert.assertEquals(device.getHealthStatus(), HealthStatus.UNTRUSTED);
    }

    /**
     * Tests that device health will be untrusted if there were alerts for the latest report. Also
     * ensures that one additional criterion is used if there is one device state with criterion.
     *
     * @throws Exception if getNewestReport is not mocked properly
     */
    @Test
    public void deviceHealthUntrustedIfAlertsForReportAndHasSingleDeviceStates() throws Exception {

        DeviceState mockState = mock(DeviceState.class);
        Criterion testCriterion = mock(Criterion.class);
        when(mockState.getDeviceTrustAlertCriterion()).thenReturn(testCriterion);

        List<DeviceState> trustStateList = new ArrayList<>();
        trustStateList.add(mockState);
        when(deviceStateManager.getStates(device)).thenReturn(trustStateList);

        ReportSummary summary = new ReportSummary();
        Report report = new TestReport();
        summary.setReport(report);
        when(reportSummaryManager.getNewestReport(HOST_NAME)).thenReturn(summary);

        ArgumentCaptor<Disjunction> argumentCaptor = ArgumentCaptor.forClass(Disjunction.class);

        when(alertManager.getTrustAlertCount(eq(device), eq(report), argumentCaptor.capture()))
                .thenReturn(1);

        deviceHealthManager.updateHealth(HOST_NAME);
        Assert.assertEquals(device.getHealthStatus(), HealthStatus.UNTRUSTED);

        Disjunction expected = argumentCaptor.getValue();
        Iterator<Criterion> conditionIter = expected.conditions().iterator();
        boolean criterionsFound = false;
        while (conditionIter.hasNext()) {
            Criterion criterion = conditionIter.next();
            Assert.assertSame(criterion, testCriterion);
            criterionsFound = true;
        }

        Assert.assertTrue(criterionsFound);
    }

    /**
     * Tests that device health will be untrusted if there were alerts for the latest report. Also
     * ensures that one additional criterion is used if there are two device states, but only one
     * state has criterion.
     *
     * @throws Exception if getNewestReport is not mocked properly
     */
    @Test
    public void deviceHealthUntrustedIfAlertsForReportAndHasMultipleDeviceStates() throws
            Exception {
        DeviceState mockStateWithCriterion = mock(DeviceState.class);
        DeviceState mockStateNoCriterion = mock(DeviceState.class);
        Criterion testCriterion = mock(Criterion.class);
        when(mockStateWithCriterion.getDeviceTrustAlertCriterion()).thenReturn(testCriterion);

        List<DeviceState> trustStateList = new ArrayList<>();
        trustStateList.add(mockStateWithCriterion);
        trustStateList.add(mockStateNoCriterion);
        when(deviceStateManager.getStates(device)).thenReturn(trustStateList);

        ReportSummary summary = new ReportSummary();
        Report report = new TestReport();
        summary.setReport(report);
        when(reportSummaryManager.getNewestReport(HOST_NAME)).thenReturn(summary);

        ArgumentCaptor<Disjunction> argumentCaptor = ArgumentCaptor.forClass(Disjunction.class);

        when(alertManager.getTrustAlertCount(eq(device), eq(report), argumentCaptor.capture()))
                .thenReturn(1);

        deviceHealthManager.updateHealth(HOST_NAME);
        Assert.assertEquals(device.getHealthStatus(), HealthStatus.UNTRUSTED);

        Disjunction expected = argumentCaptor.getValue();
        Iterator<Criterion> conditionIter = expected.conditions().iterator();
        boolean criterionsFound = false;
        while (conditionIter.hasNext()) {
            Criterion criterion = conditionIter.next();
            Assert.assertSame(criterion, testCriterion);
            criterionsFound = true;
        }

        Assert.assertTrue(criterionsFound);
    }

    /**
     * Tests that a null pointer exception is thrown if null is passed in.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void deviceHealthUpdateAlertListNull() {
        List<Alert> alertList = null;
        deviceHealthManager.updateHealth(alertList);
    }

    /**
     * Tests that if an alert is passed into updateHealth, the device from that alert will have the
     * health updated.
     *
     * @throws Exception if getNewestReport is not mocked properly
     */
    @Test
    public void deviceHealthUpdateAlertListUntrustedThenTrusted() throws Exception {
        Alert alert = new Alert("alert details");
        alert.setDeviceName(HOST_NAME);
        List<Alert> alertList = new ArrayList<>();
        alertList.add(alert);

        ReportSummary summary = new ReportSummary();
        Report report = new TestReport();
        summary.setReport(report);
        when(reportSummaryManager.getNewestReport(HOST_NAME)).thenReturn(summary);

        when(alertManager.getTrustAlertCount(device, report, null)).thenReturn(alertList.size());

        deviceHealthManager.updateHealth(HOST_NAME);
        Assert.assertEquals(device.getHealthStatus(), HealthStatus.UNTRUSTED);

        when(alertManager.getTrustAlertCount(device, report, null)).thenReturn(0);

        deviceHealthManager.updateHealth(alertList);

        Assert.assertEquals(device.getHealthStatus(), HealthStatus.TRUSTED);
    }

    /**
     * Tests that if alerts not related to a device are passed in, the device's health will not
     * be updated.
     *
     * @throws Exception if getNewestReport is not mocked properly
     */
    @Test
    public void deviceHealthUpdateAlertListUnknownDeviceInAlert() throws Exception {
        Device newDevice = new Device("new-device");
        Alert alert = new Alert("alert details");
        alert.setDeviceName(newDevice.getName());
        List<Alert> alertList = new ArrayList<>();
        alertList.add(alert);

        when(deviceManager.getDevice(newDevice.getName())).thenReturn(newDevice);

        ReportSummary summary = new ReportSummary();
        Report report = new TestReport();
        summary.setReport(report);
        when(reportSummaryManager.getNewestReport(HOST_NAME)).thenReturn(summary);

        when(alertManager.getTrustAlertCount(device, report, null)).thenReturn(alertList.size());

        deviceHealthManager.updateHealth(HOST_NAME);
        Assert.assertEquals(device.getHealthStatus(), HealthStatus.UNTRUSTED);

        // If our alert list contained an alert for this device, this would be called and update
        // the device health to TRUSTED
        when(alertManager.getTrustAlertCount(device, report, null)).thenReturn(0);

        // However, our alert list contains an alert for a different device, so ...
        deviceHealthManager.updateHealth(alertList);

        // ... the device health should still be untrusted
        Assert.assertEquals(device.getHealthStatus(), HealthStatus.UNTRUSTED);
    }
}
