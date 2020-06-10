package hirs.data.persist;

import hirs.data.persist.baseline.ImaBaseline;
import hirs.data.persist.baseline.SimpleImaBaseline;
import hirs.data.persist.baseline.Baseline;
import hirs.data.persist.enums.AlertSeverity;
import hirs.data.persist.enums.AlertSource;
import hirs.data.persist.enums.AlertType;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * A unit test class for <code>Alert</code>.
 */
public final class AlertTest {

    private static final String TEST_DETAILS = "the details of the test alert";
    private static final String TEST_EXPECTED = "the value I expected";
    private static final String TEST_RECEIVED = "the value I actually received";
    private static final String TEST_DEVICE_NAME = "MyTestDevice";
    private static final String RESOLVE_DESCRIPTION = "Record was added to baseline";
    private static final String TEST_BASELINE_NAME = "Alert Test Baseline";
    private static final String TEST_DISPLAY_TITLE = "Display Title";

     /**
     * Tests that default values are applied by the public Alert constructor.
     */
    @Test
    public void testAlertDefaults() {
        Alert alert = new Alert(TEST_DETAILS);
        Assert.assertEquals(alert.getSeverity(), AlertSeverity.UNSPECIFIED);
        Assert.assertEquals(alert.getType(), AlertType.UNSPECIFIED);
        Assert.assertEquals(alert.getSource(), AlertSource.UNSPECIFIED);
        Assert.assertNull(alert.getDisplayTitle());
    }

    /**
     * Test that the deviceName can be set and retrieved.
     */
    @Test
    public void testDeviceName() {
        Alert alert = new Alert(TEST_DETAILS);
        alert.setDeviceName(TEST_DEVICE_NAME);
        Assert.assertEquals(alert.getDeviceName(), TEST_DEVICE_NAME);
    }

    /**
     * Test that the details can be set and retrieved.
     */
    @Test
    public void testDetails() {
        Alert alert = new Alert(TEST_DETAILS);
        Assert.assertEquals(alert.getDetails(), TEST_DETAILS);
    }

    /**
     * Test that the details can be set and retrieved.
     */
    @Test
    public void testDisplayTitle() {
        Alert alert = new Alert(TEST_DETAILS);
        alert.setDisplayTitle(TEST_DISPLAY_TITLE);
        Assert.assertEquals(alert.getDisplayTitle(), TEST_DISPLAY_TITLE);
    }

    /**
     * Test that the <code>Report</code> can be set and retrieved.
     */
    @Test
    public void testReport() {
        Alert alert = new Alert(TEST_DETAILS);
        IntegrityReport report = new IntegrityReport();
        alert.setReport(report);
        Assert.assertEquals(alert.getReport().getId(), report.getId());
    }

    /**
     * Test that the <code>Policy</code>'s id can be set and retrieved.
     */
    @Test
    public void testPolicyId() {
        Alert alert = new Alert(TEST_DETAILS);
        UUID policyId = UUID.randomUUID();
        alert.setPolicyId(policyId);
        Assert.assertEquals(alert.getPolicyId(), policyId);
    }

    /**
     * Test that the id and severity of a <code>Baseline</code> can be set and retrieved.
     */
    @Test
    public void testBaselineIdAndSeverity() {
        Alert alert = new Alert(TEST_DETAILS);
        ImaBaseline baseline = new SimpleImaBaseline(TEST_BASELINE_NAME);
        baseline.setSeverity(AlertSeverity.SEVERE);
        alert.setBaselineIdsAndSeverity(Collections.singleton(baseline));
        Assert.assertEquals(alert.getBaselineIds().iterator().next(), baseline.getId());
        Assert.assertEquals(alert.getSeverity(), baseline.getSeverity());
    }

    /**
     * Test that the source can be set and retrieved.
     */
    @Test
    public void testSource() {
        Alert alert = new Alert(TEST_DETAILS);
        alert.setSource(AlertSource.IMA_APPRAISER);
        Assert.assertEquals(alert.getSource(), AlertSource.IMA_APPRAISER);
    }

    /**
     * Test that the type can be set and retrieved.
     */
    @Test
    public void testType() {
        Alert alert = new Alert(TEST_DETAILS);
        alert.setType(AlertType.REPORT_REQUESTS_MISSING);
        Assert.assertEquals(alert.getType(),
                AlertType.REPORT_REQUESTS_MISSING);
    }

    /**
     * Tests that the expected and received values can be set and retrieved.
     */
    @Test
    public void testExpectedReceived() {
        Alert alert = new Alert(TEST_DETAILS);
        alert.setExpectedAndReceived(TEST_EXPECTED, TEST_RECEIVED);
        Assert.assertEquals(alert.getExpected(), TEST_EXPECTED);
        Assert.assertEquals(alert.getReceived(), TEST_RECEIVED);
    }

    /**
     * Tests that the severity can be set and retrieved.
     */
    @Test
    public void testSeverity() {
        Alert alert = new Alert(TEST_DETAILS);
        Assert.assertEquals(alert.getSeverity(), AlertSeverity.UNSPECIFIED);
    }

    /**
     * Tests that the severity can be set independently from a baseline.
     */
    @Test
    public void testSetSeverity() {
        final AlertSeverity baselineSeverity = AlertSeverity.SEVERE;
        final AlertSeverity alertSeverity = AlertSeverity.LOW;

        // Set up a baseline with a severity
        ImaBaseline baseline = new SimpleImaBaseline(TEST_BASELINE_NAME);
        baseline.setSeverity(baselineSeverity);
        HashSet<Baseline> baselineSet = new HashSet<>();
        baselineSet.add(baseline);

        // Track the status of the severity value
        Alert alert = new Alert(TEST_DETAILS);
        Assert.assertEquals(alert.getSeverity(), AlertSeverity.UNSPECIFIED);
        alert.setBaselineIdsAndSeverity(baselineSet);
        Assert.assertEquals(alert.getSeverity(), baselineSeverity);
        alert.setSeverity(alertSeverity);
        Assert.assertEquals(alert.getSeverity(), alertSeverity);
    }

    /**
     * Tests that the archivedTime is set properly and will return true when resolution is checked.
     */
    @Test
    public void testArchive() {
        Alert alert = new Alert(TEST_DETAILS);
        final long t0 = System.currentTimeMillis();
        //Resolves
        Assert.assertEquals(alert.archive(), true);
        final long t1 = System.currentTimeMillis();
        Assert.assertEquals(alert.isArchived(), true);
        Assert.assertNotEquals(alert.getArchivedTime(), null);
        Assert.assertTrue(t0 <= alert.getArchivedTime().getTime());
        Assert.assertTrue(alert.getArchivedTime().getTime() <= t1);
    }

    /**
     * Tests that the archivedTime and archivedDescription are set properly and will return true
     * when resolution is checked.
     */
    @Test
    public void testArchiveWithDescription() {
        Alert alert = new Alert(TEST_DETAILS);
        final long t0 = System.currentTimeMillis();
        //Resolves
        Assert.assertEquals(alert.archive(RESOLVE_DESCRIPTION), true);
        final long t1 = System.currentTimeMillis();
        Assert.assertEquals(alert.isArchived(), true);
        Assert.assertNotEquals(alert.getArchivedTime(), null);
        Assert.assertTrue(t0 <= alert.getArchivedTime().getTime());
        Assert.assertTrue(alert.getArchivedTime().getTime() <= t1);
        Assert.assertEquals(alert.getArchivedDescription(), RESOLVE_DESCRIPTION);
    }

    /**
     * Tests that the archivedTime is set to null when restore is called and that isArchived
     * will return false.  Also, tests that an archived description will be removed if set.
     */
    @Test
    public void testRestore() {
        Alert alert = new Alert(TEST_DETAILS);
        //Resolves
        Assert.assertTrue(alert.archive(RESOLVE_DESCRIPTION));
        Assert.assertTrue(alert.isArchived());
        Assert.assertNotEquals(alert.getArchivedTime(), null);
        Assert.assertEquals(alert.getArchivedDescription(), RESOLVE_DESCRIPTION);
        //Unresolves
        Assert.assertTrue(alert.restore());
        Assert.assertFalse(alert.isArchived());
        Assert.assertEquals(alert.getArchivedTime(), null);
        Assert.assertEquals(alert.getArchivedDescription(), null);
    }

    /**
     * Tests that archiving, restoring, and then once again archiving an Alert will work
     * as expected.  Alert should have a archivedTime after resolve(), time should be null
     * after restore(), and then once again, have a time after archive() is called the
     * second time.
     */
    @Test
    public void testArchiveRestoreSuccession() {
        Alert alert = new Alert(TEST_DETAILS);
        //Resolves
        Assert.assertEquals(alert.archive(), true);
        Assert.assertEquals(alert.isArchived(), true);
        Assert.assertNotEquals(alert.getArchivedTime(), null);
        //Unresolves
        Assert.assertEquals(alert.restore(), true);
        Assert.assertEquals(alert.isArchived(), false);
        Assert.assertEquals(alert.getArchivedTime(), null);
        //Resolves
        Assert.assertEquals(alert.archive(), true);
        Assert.assertEquals(alert.isArchived(), true);
        Assert.assertNotEquals(alert.getArchivedTime(), null);
    }

    /**
     * Attempts to archive an Alert twice.  First, time in successful, but second time should
     * return false.  Additionally, the archivedTime should still be the same as it was when the
     * Alert was first archived.
     */
    @Test
    public void testMultipleArchiveAttempts() {
        Alert alert = new Alert(TEST_DETAILS);
        //Resolves
        Assert.assertEquals(alert.archive(), true);
        Assert.assertEquals(alert.isArchived(), true);
        Date resolvedTime = alert.getArchivedTime();
        Assert.assertNotEquals(resolvedTime, null);
        //Attempts to resolve for second time
        Assert.assertEquals(alert.archive(), false);
        Assert.assertEquals(alert.isArchived(), true);
        Assert.assertEquals(alert.getArchivedTime(), resolvedTime);
    }

    /**
     * Attempts to restore an Alert twice.  First time AFTER the Alert was archived should be
     * successful, but subsequent calls to restore() should fail.  After each call the
     * archivedTime should be set to null.
     */
    @Test
    public void testMultipleRestoreAttempts() {
        Alert alert = new Alert(TEST_DETAILS);
        Assert.assertEquals(alert.restore(), false);
        Assert.assertEquals(alert.isArchived(), false);
        Assert.assertEquals(alert.getArchivedTime(), null);
        //Resolves
        Assert.assertEquals(alert.archive(), true);
        Assert.assertEquals(alert.isArchived(), true);
        Assert.assertNotEquals(alert.getArchivedTime(), null);
        //Unresolves
        Assert.assertEquals(alert.restore(), true);
        Assert.assertEquals(alert.isArchived(), false);
        Assert.assertEquals(alert.getArchivedTime(), null);
        //Attempts to unresolve for second time
        Assert.assertEquals(alert.restore(), false);
        Assert.assertEquals(alert.isArchived(), false);
        Assert.assertEquals(alert.getArchivedTime(), null);
    }

}
