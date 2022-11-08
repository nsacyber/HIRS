package hirs.data.persist;

import hirs.appraiser.HIRSAppraiser;
import hirs.appraiser.IMAAppraiser;
import hirs.appraiser.TPMAppraiser;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;


/**
 * ReportSummaryTest is a unit test class for ReportSummary.
 */
public class ReportSummaryTest {

    private static final Logger LOGGER = LogManager.getLogger(ReportSummaryTest.class);
    private static final String APPRAISAL_PASS_MESSAGE = "test appraisal success";
    private static final String APPRAISAL_FAIL_MESSAGE = "test appraisal failure";
    private static final String DEFAULT_CLIENT_HOSTNAME = "test.hostname";
    private static final String DEFAULT_FILE_PATH = "/test/file/path";
    private static final Timestamp DEFAULT_TIMESTAMP = new Timestamp(Calendar
            .getInstance().getTime().getTime());

    /**
     * Tests that a ReportSummary hostname can be retrieved and set.
     */
    @Test
    public final void testClientHostname() {
        ReportSummary reportSummary = new ReportSummary();
        reportSummary.setClientHostname(DEFAULT_CLIENT_HOSTNAME);
        Assert.assertEquals(reportSummary.getClientHostname(),
                DEFAULT_CLIENT_HOSTNAME);
    }

    /**
     * Tests that a ReportSummary timestamp can be retrieved and set.
     */
    @Test
    public final void testTimestamp() {
        ReportSummary reportSummary = new ReportSummary();
        reportSummary.setTimestamp(DEFAULT_TIMESTAMP);
        Assert.assertEquals(reportSummary.getTimestamp(), new Date(DEFAULT_TIMESTAMP.getTime()));
    }

    /**
     * Tests that the appraisal result can be retrieved and set.
     */
    @Test
    public final void testPassingAppraisalResult() {
        ReportSummary reportSummary = new ReportSummary();
        AppraisalResult appraisalResult = new AppraisalResult(HIRSAppraiser.class,
                                                              AppraisalStatus.Status.PASS,
                                                              APPRAISAL_PASS_MESSAGE);
        Set<AppraisalResult> appraisalResults = Collections.singleton(appraisalResult);
        reportSummary.setAppraisalResults(appraisalResults);

        AppraisalResult appResult =
                reportSummary.getHirsAppraisalResult();
        Assert.assertEquals(appResult.getAppraisalStatus(), AppraisalStatus.Status.PASS);
        Assert.assertNull(reportSummary.getAppraisalResult(IMAAppraiser.class));
    }

    /**
     * Tests that a set of AppraisalResults containing a failing AppraisalStatus results in a
     * non-successful appraisal.
     */
    @Test
    public final void testFailingAppraisalResult() {
        ReportSummary reportSummary = new ReportSummary();
        AppraisalResult appraisalResultPass = new AppraisalResult(IMAAppraiser.class,
                                                                  AppraisalStatus.Status.PASS,
                                                                  APPRAISAL_PASS_MESSAGE);
        AppraisalResult appraisalResultFail = new AppraisalResult(TPMAppraiser.class,
                                                                  AppraisalStatus.Status.FAIL,
                                                                  APPRAISAL_FAIL_MESSAGE);
        Set<AppraisalResult> appraisalResults = new HashSet<>();
        appraisalResults.add(appraisalResultPass);
        appraisalResults.add(appraisalResultFail);
        reportSummary.setAppraisalResults(appraisalResults);
        AppraisalResult appResult =
                reportSummary.getHirsAppraisalResult();
        Assert.assertEquals(appResult.getAppraisalStatus(), AppraisalStatus.Status.FAIL);
    }

    /**
     * Tests that a set of AppraisalResults containing an error AppraisalStatus results in a
     * non-successful appraisal.
     */
    @Test
    public final void testErrorAppraisalResult() {
        ReportSummary reportSummary = new ReportSummary();
        AppraisalResult appraisalResultPass = new AppraisalResult(HIRSAppraiser.class,
                                                                  AppraisalStatus.Status.PASS,
                                                                  APPRAISAL_PASS_MESSAGE);
        AppraisalResult appraisalResultError = new AppraisalResult(HIRSAppraiser.class,
                                                                   AppraisalStatus.Status.ERROR,
                                                                   APPRAISAL_FAIL_MESSAGE);
        Set<AppraisalResult> appraisalResults = new HashSet<>();
        appraisalResults.add(appraisalResultPass);
        appraisalResults.add(appraisalResultError);
        reportSummary.setAppraisalResults(appraisalResults);
        AppraisalResult appResult =
                reportSummary.getHirsAppraisalResult();
        Assert.assertEquals(appResult.getAppraisalStatus(), AppraisalStatus.Status.ERROR);
    }
}
