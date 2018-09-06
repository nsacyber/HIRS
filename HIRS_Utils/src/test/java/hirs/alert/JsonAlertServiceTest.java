package hirs.alert;

import static org.apache.logging.log4j.LogManager.getLogger;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import hirs.appraiser.HIRSAppraiser;
import hirs.data.persist.Alert;
import hirs.data.persist.AppraisalResult;
import hirs.data.persist.AppraisalStatus;
import hirs.data.persist.ReportSummary;
import hirs.data.persist.alert.AlertMonitor;
import hirs.data.persist.alert.AlertServiceConfig;
import hirs.data.persist.alert.JsonAlertMonitor;
import hirs.persist.AlertMonitorManager;
import hirs.persist.AlertServiceConfigManager;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Provides tests for JsonAlertService.
 */
public class JsonAlertServiceTest {

    private static final Logger LOGGER = getLogger(JsonAlertServiceTest.class);

    @InjectMocks
    private JsonAlertService service;

    @Mock
    private AlertMonitorManager monitorManager;

    @Mock
    private AlertServiceConfigManager configManager;

    /**
     * Prepares a testing environment.
     */
    @BeforeMethod
    public void beforeMethod() {
        service = new JsonAlertService();
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test the addMonitor() and getMonitor() methods.
     */
    @Test
    public final void addMonitor() {
        AlertMonitor monitor = mock(AlertMonitor.class);
        service.addMonitor(monitor);
        verify(monitor).setAlertServiceType(JsonAlertService.NAME);
        verify(monitorManager).saveAlertMonitor(monitor);
        verifyNoMoreInteractions(monitor, monitorManager, configManager);
    }

    /**
     * Tests the alert () and send() methods. The CompositeAlertService tests the alert sent to
     * the HibernateAlertService. This tests the JSON capability.
     */
    @Test
    public final void alert() {
        final Alert alert = new Alert("Test Composite Alert Service Alert");
        AlertServiceConfig config = new AlertServiceConfig(JsonAlertService.NAME);
        AlertMonitor monitor = new JsonAlertMonitor("Test Monitor");

        // use an enabled config for this test
        config.enable();
        List<AlertServiceConfig> configs = Collections.singletonList(config);
        when(configManager.getAlertServiceConfigList(AlertServiceConfig.class)).thenReturn(configs);

        // use our test monitor
        List<AlertMonitor> monitors = Collections.singletonList(monitor);
        when(monitorManager.getAlertMonitorList(AlertMonitor.class)).thenReturn(monitors);

        service.alert(alert);

        // verify mock interactions
        verify(configManager).getAlertServiceConfigList(AlertServiceConfig.class);
        verify(monitorManager).getAlertMonitorList(AlertMonitor.class);
        verifyNoMoreInteractions(monitorManager, configManager);
    }

    /**
     * Tests the alert notification to alert services.
     */
    @Test
    public final void alertSummary() {
        LOGGER.debug("creating test alert");

        final ReportSummary reportSummary = new ReportSummary();
        AppraisalResult appraisalResult =
            new AppraisalResult(HIRSAppraiser.class, AppraisalStatus.Status.FAIL, "test result");
        Set<AppraisalResult> appraisalResults = Collections.singleton(appraisalResult);
        reportSummary.setAppraisalResults(appraisalResults);

        service.alertSummary(reportSummary);
    }

    /**
     * Tests the addMonitor() and deleteMonitor() methods.
     */
    @Test
    public final void deleteMonitor() {
        LOGGER.debug("Deleting an alert monitor");
        JsonAlertMonitor am = new JsonAlertMonitor("TestAM1");
        service.deleteMonitor(am);
        verify(monitorManager).deleteAlertMonitor("TestAM1");
    }

    /**
     * Tests updating of the service config.
     */
    @Test
    public final void updateConfig() {
        AlertServiceConfig config = new AlertServiceConfig(JsonAlertService.NAME);
        service.updateConfig(config);
        verify(configManager).updateAlertServiceConfig(config);
        Assert.assertEquals(config.getType(), JsonAlertService.NAME);
    }

    /**
     * Tests that after reloading monitors, the monitor list is indeed updated.
     */
    @Test
    public final void getMonitors() {

        // some example monitors
        AlertMonitor monitor1 = new JsonAlertMonitor("monitor1");
        AlertMonitor monitor2 = new JsonAlertMonitor("monitor2");
        AlertMonitor monitor3 = new JsonAlertMonitor("monitor3");
        List<AlertMonitor> monitors = Arrays.asList(monitor1, monitor2, monitor3);

        // perform reloading, ensure that the monitor list is empty.
        Assert.assertTrue(service.getMonitors().isEmpty());

        // prepare mock interactions
        when(monitorManager.getAlertMonitorList(AlertMonitor.class)).thenReturn(monitors);

        // perform the test
        service.reloadProperties();

        // ensure that the monitors exist
        Assert.assertTrue(service.getMonitors().contains(monitor1));
        Assert.assertTrue(service.getMonitors().contains(monitor2));
        Assert.assertTrue(service.getMonitors().contains(monitor3));

        // verify mock interactions
        verify(monitorManager).getAlertMonitorList(AlertMonitor.class);
    }

    /**
     * Test the getName() methods.
     */
    @Test
    public final void name() {
        LOGGER.debug("Testing get name");
        Assert.assertEquals(service.getName(), JsonAlertService.NAME);
    }
}
