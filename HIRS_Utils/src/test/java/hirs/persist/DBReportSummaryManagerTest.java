package hirs.persist;

import hirs.appraiser.HIRSAppraiser;
import hirs.appraiser.IMAAppraiser;
import hirs.data.persist.AppraisalResult;
import hirs.data.persist.AppraisalStatus;
import hirs.data.persist.ReportSummary;

import org.apache.commons.lang3.RandomStringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

/**
 *
 */
public class DBReportSummaryManagerTest extends SpringPersistenceTest {
    private static final String APPRAISAL_PASS_MESSAGE = "test appraisal success";
    private static final String LARGE_APPRAISAL_MESSAGE = RandomStringUtils.random(1000000);
    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an
     * in-memory database that is used for testing.
     */
    @BeforeClass
    public final void setup() {
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterClass
    public final void tearDown() {
    }

    /**
     * Tests that successful appraisal result is reflected in <code>ReportSummary</code>.
     */
    @Test
    public final void testPersistSuccessfulReportSummary() {
        testPersistReportSummary(APPRAISAL_PASS_MESSAGE);
    }

    /**
     * Tests that a large appraisal result message is persisted in <code>ReportSummary</code>.
     */
    @Test
    public final void testPersistLargeAppraisalResultMessage() {
        testPersistReportSummary(LARGE_APPRAISAL_MESSAGE);
    }

    private void testPersistReportSummary(final String appraisalResultMessage) {
        ReportSummary reportSummary = new ReportSummary();
        AppraisalResult appraisalResult = new AppraisalResult(HIRSAppraiser.class,
                AppraisalStatus.Status.PASS,
                appraisalResultMessage);
        Set<AppraisalResult> appraisalResults = Collections.singleton(appraisalResult);
        reportSummary.setAppraisalResults(appraisalResults);
        reportSummary.setTimestamp(new Date());

        DBReportSummaryManager repoSumMan = new DBReportSummaryManager(sessionFactory);
        reportSummary = repoSumMan.saveReportSummary(reportSummary);

        ReportSummary retrievedReportSummary = repoSumMan.getReportSummary(reportSummary.getId());
        AppraisalStatus.Status appStatus =
                reportSummary.getHirsAppraisalResult().getAppraisalStatus();
        Assert.assertEquals(appStatus, AppraisalStatus.Status.PASS);
        Assert.assertEquals(retrievedReportSummary.getAppraisalResult(HIRSAppraiser.class),
                appraisalResult);
        Assert.assertNull(retrievedReportSummary.getAppraisalResult(IMAAppraiser.class));
    }
}
