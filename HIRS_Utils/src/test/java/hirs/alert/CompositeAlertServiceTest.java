package hirs.alert;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hirs.data.persist.Alert;
import hirs.data.persist.ReportSummary;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Suite of tests for {@link CompositeAlertService}.
 */
public class CompositeAlertServiceTest {

    // object in test
    @InjectMocks
    private CompositeAlertService alertService;

    // mock dependency
    @Mock
    private AlertManager alertManager;

    // mock dependency
    @Mock
    private ManagedAlertService enabledService;

    // mock dependency
    @Mock
    private ManagedAlertService disabledService;

    /**
     * Prepares a test environment.
     */
    @BeforeMethod
    public final void beforeTest() {
        // initialize the mocks
        MockitoAnnotations.initMocks(this);

        // assign the managed alert services to the composite service
        ReflectionTestUtils.setField(alertService, "alertServices",
                Arrays.asList(enabledService, disabledService));
    }

    /**
     * Ensures that the test level mocks were not used in any unintended manor.
     */
    @AfterMethod
    public final void afterTest() {
        verifyNoMoreInteractions(alertManager, enabledService, disabledService);
    }

    /**
     * Tests {@link CompositeAlertService#alert(Alert)}. Ensures that only enabled alert services
     * will be invoked.
     */
    @Test
    @SuppressFBWarnings(
            value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT",
            justification = "These method calls are made for verification purposes with Mockito"
    )
    public final void testAlert() {
        // prepare test level mocks
        Alert alert = mock(Alert.class);

        // prepare the configurations
        when(enabledService.isEnabled()).thenReturn(true);
        when(disabledService.isEnabled()).thenReturn(false);

        // stub out the alert method on the managed service.
        doNothing().when(enabledService).alert(any(Alert.class));

        // perform test
        alertService.alert(alert);

        // verify that the configurations were used
        verify(enabledService).isEnabled();
        verify(disabledService).isEnabled();

        // verify that the alert was saved.
        verify(alertManager).saveAlert(alert);

        // verify that the enabled service config was checked and the service alerted
        verify(enabledService).alert(alert);

        // ensure that mocks were used correctly.
        verifyNoMoreInteractions(alert, enabledService, disabledService);
    }

    /**
     * Tests {@link CompositeAlertService#alert(Alert)}. Ensures that only enabled alert services
     * will be invoked.
     */
    @Test
    @SuppressFBWarnings(
            value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT",
            justification = "These method calls are made for verification purposes with Mockito"
    )
    public final void testAlertSummary() {
        // prepare test level mocks
        ReportSummary reportSummary = mock(ReportSummary.class);

        // prepare the configurations
        when(enabledService.isEnabled()).thenReturn(true);
        when(disabledService.isEnabled()).thenReturn(false);

        // stub out the enabled service alert summary
        doNothing().when(enabledService).alertSummary(any(ReportSummary.class));

        // perform test
        alertService.alertSummary(reportSummary);

        // verify that the configurations were used
        verify(enabledService).isEnabled();
        verify(disabledService).isEnabled();

        // verify that the enabled service config was checked and the service alerted
        verify(enabledService).alertSummary(reportSummary);

        // ensure that mocks were used correctly.
        verifyNoMoreInteractions(reportSummary, enabledService, disabledService);
    }

}
