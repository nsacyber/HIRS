package hirs;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * <code>IntegrityReportRequestTest</code> is a unit test class for the
 * <code>IntegrityReportRequest</code> class.
 */
public class IntegrityReportRequestTest {

    /**
     * Tests instantiation of IntegrityReportRequest object.
     */
    @Test
    public final void integrityReportRequest() {
        IntegrityReportRequest reportRequest = new IntegrityReportRequest();
        Assert.assertNotNull(reportRequest);
    }

    /**
     * Tests adding ReportRequests to <code>IntegrityReportRequest</code>.
     */
    @Test
    public final void addReportRequest() {
        IMAReportRequest imaReportRequest = new IMAReportRequest();
        IntegrityReportRequest reportRequest = new IntegrityReportRequest();
        reportRequest.addReportRequest(imaReportRequest);
        List<ReportRequest> requests = reportRequest.getReportRequest();
        Assert.assertEquals(requests.get(0).getReportType(),
                imaReportRequest.getReportType());
    }

    /**
     * Tests adding ReportRequests to <code>IntegrityReportRequest</code> when
     * passing in a null ReportRequest.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void addReportRequestNull() {
        IntegrityReportRequest reportRequest = new IntegrityReportRequest();
        reportRequest.addReportRequest(null);
        List<ReportRequest> requests = reportRequest.getReportRequest();
        Assert.assertEquals(requests.size(), 0);
    }

    /**
     * Tests retrieving <code>ReportRequest</code> from
     * <code>IntegrityReportRequest</code>.
     */
    @Test
    public final void getReportRequest() {
        IMAReportRequest imaReportRequest = new IMAReportRequest();
        IntegrityReportRequest reportRequest = new IntegrityReportRequest();
        reportRequest.addReportRequest(imaReportRequest);
        List<ReportRequest> requests = reportRequest.getReportRequest();
        Assert.assertEquals(requests.get(0).getReportType(),
                imaReportRequest.getReportType());
    }

    /**
     * Tests the accessor and mutator for the waitForAppraisalCompletionEnabled field.
     */
    @Test
    public final void waitForAppraisalCompletionEnabledField() {
        IntegrityReportRequest reportRequest = new IntegrityReportRequest();
        reportRequest.setWaitForAppraisalCompletionEnabled(false);
        Assert.assertFalse(reportRequest.isWaitForAppraisalCompletionEnabled());
        reportRequest.setWaitForAppraisalCompletionEnabled(true);
        Assert.assertTrue(reportRequest.isWaitForAppraisalCompletionEnabled());
    }
}
