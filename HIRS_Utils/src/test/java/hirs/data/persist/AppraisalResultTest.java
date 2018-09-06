package hirs.data.persist;

import hirs.appraiser.HIRSAppraiser;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * A unit test class for <code>AppraisalResult</code>.
 */
public class AppraisalResultTest {

    private static final String TEST_MESSAGE = "test AppraisalResult message";
    private static final String TEST_ALTERED_MESSAGE = "altered test AppraisalResult message";

    /**
     * Tests the <code>AppraiserService</code> getter method.
     */
    @Test
    public void testGetAppraiser() {
        AppraisalResult result =
            new AppraisalResult(HIRSAppraiser.class, AppraisalStatus.Status.PASS, TEST_MESSAGE);
        Assert.assertEquals(result.getAppraiser(), HIRSAppraiser.class);
    }

    /**
     * Tests the <code>AppraiserService</code> getter method.
     */
    @Test
    public void testGetNullAppraiser() {
        AppraisalResult result =
            new AppraisalResult(null, AppraisalStatus.Status.PASS, TEST_MESSAGE);
        Assert.assertEquals(result.getAppraiser(), null);
    }

    /**
     * Tests the <code>AppraisalStatus</code> getter method.
     */
    @Test
    public void testGetAppraisalStatus() {
        AppraisalResult resultPass =
            new AppraisalResult(HIRSAppraiser.class, AppraisalStatus.Status.PASS, TEST_MESSAGE);
        AppraisalResult resultFail =
            new AppraisalResult(HIRSAppraiser.class, AppraisalStatus.Status.FAIL, TEST_MESSAGE);
        AppraisalResult resultError =
            new AppraisalResult(HIRSAppraiser.class, AppraisalStatus.Status.ERROR, TEST_MESSAGE);
        Assert.assertEquals(resultPass.getAppraisalStatus(), AppraisalStatus.Status.PASS);
        Assert.assertEquals(resultFail.getAppraisalStatus(), AppraisalStatus.Status.FAIL);
        Assert.assertEquals(resultError.getAppraisalStatus(), AppraisalStatus.Status.ERROR);
    }

    /**
     * Tests the <code>AppraisalStatus</code> setter method.
     */
    @Test
    public void testSetAppraisalStatus() {
        AppraisalResult result =
            new AppraisalResult(HIRSAppraiser.class, AppraisalStatus.Status.PASS, TEST_MESSAGE);
        result.setAppraisalStatus(AppraisalStatus.Status.FAIL);
        Assert.assertEquals(result.getAppraisalStatus(), AppraisalStatus.Status.FAIL);
    }

    /**
     * Tests the String message getter method.
     */
    @Test
    public void testGetAppraisalResultMessage() {
        AppraisalResult result =
            new AppraisalResult(HIRSAppraiser.class, AppraisalStatus.Status.PASS, TEST_MESSAGE);
        Assert.assertEquals(result.getAppraisalResultMessage(), TEST_MESSAGE);
    }

    /**
     * Tests the String message setter method.
     */
    @Test
    public void testSetAppraisalResultMessage() {
        AppraisalResult result =
            new AppraisalResult(HIRSAppraiser.class, AppraisalStatus.Status.PASS, TEST_MESSAGE);
        result.setAppraisalResultMessage(TEST_ALTERED_MESSAGE);
        Assert.assertEquals(result.getAppraisalResultMessage(), TEST_ALTERED_MESSAGE);
    }

    /**
     * Tests the <code>ReportSummary</code> getter and setter methods.
     */
    @Test
    public void testGetAndSetResultSummary() {
        ReportSummary reportSummary = new ReportSummary();
        AppraisalResult result =
            new AppraisalResult(HIRSAppraiser.class, AppraisalStatus.Status.PASS, TEST_MESSAGE);
        result.setReportSummary(reportSummary);
        Assert.assertSame(result.getReportSummary(), reportSummary);
    }
}
