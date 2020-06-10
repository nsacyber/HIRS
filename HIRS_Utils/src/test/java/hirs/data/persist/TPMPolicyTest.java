package hirs.data.persist;

import hirs.data.persist.baseline.TpmWhiteListBaseline;
import hirs.data.persist.baseline.TPMBaseline;
import hirs.data.persist.enums.AlertSeverity;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import hirs.tpm.TPMBaselineGenerator;
import org.hibernate.Session;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Assert;

/**
 * Unit test class for TPMPolicy class.
 */
public class TPMPolicyTest extends HibernateTest<TPMPolicy> {
    private static final String BASELINE_PATH = "/tpm/TPMTestBaseline.csv";
    private static final int INVALID_PCR_MASK = 0x1FFFFFF;
    private static final int TWO = 2;
    private static final int THREE = 3;

    /**
     * Sets up a Hibernate session factory to be used in the tests in this class.
     */
    @BeforeClass
    public final void initFactory() {
    }

    /**
     * Tests Instantiation of TPMPolicy object with expected attributes values.
     */
    @Test
    public final void tpmPolicy() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        Assert.assertNotNull(tpmPolicy);
        Assert.assertEquals(tpmPolicy.getAppraisePcrMask(), 0);
        Assert.assertEquals(tpmPolicy.getReportPcrMask(), TPMPolicy.ALL_PCR_MASK);
        Assert.assertEquals(tpmPolicy.isAppraiseFullReport(), true);
        Assert.assertEquals(tpmPolicy.getTpmWhiteListBaselines().size(), 0);
        Assert.assertEquals(tpmPolicy.getName(), "TestTPMPolicy");
    }

    /**
     * Tests that TPMPolicy constructor throws a NullPointerException with null
     * name.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void tpmPolicyNullName() {
        new TPMPolicy(null);
    }

    /**
     * Tests Setting and getting the policy TPMBaseline.
     *
     * @throws Exception
     *             thrown if error generated reading input stream
     */
    @Test
    public final void setTpmBaseline() throws Exception {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        TpmWhiteListBaseline baseline;
        String name = "TestTpmPolicyBaseline";
        baseline = createTestWhiteListBaseline(name, BASELINE_PATH);
        tpmPolicy.setTpmWhiteListBaseline(baseline);
        Collection<TpmWhiteListBaseline> baselines = tpmPolicy.getTpmWhiteListBaselines();
        Assert.assertEquals(baselines.size(), 1);
        Assert.assertTrue(baselines.contains(baseline));
    }

    /**
     * Tests Setting and getting multiple TPMBaselines to/from the policy.
     *
     * @throws Exception
     *             thrown if error generated reading input stream
     */
    @Test
    public final void setTpmBaselines() throws Exception {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        TpmWhiteListBaseline baseline =
                createTestWhiteListBaseline("TestTpmPolicyBaseline", BASELINE_PATH);
        TpmWhiteListBaseline anotherBaseline = createTestWhiteListBaseline(
                "AnotherTestTpmPolicyBaseline", BASELINE_PATH
        );
        tpmPolicy.setTpmWhiteListBaselines(Arrays.asList(baseline, anotherBaseline));
        Collection<TpmWhiteListBaseline> baselines = tpmPolicy.getTpmWhiteListBaselines();
        Assert.assertEquals(baselines.size(), 2);
        Assert.assertTrue(baselines.contains(baseline));
        Assert.assertTrue(baselines.contains(anotherBaseline));
    }

    /**
     * Tests that if we attempt to set the policy's baseline to null, a NullPointerException is
     * thrown.
     */
    @Test(expectedExceptions = PolicyException.class)
    public final void setTpmBaselineNull() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.setTpmWhiteListBaseline(null);
    }

    /**
     * Tests that if we attempt to set the policy's baselines to null, a NullPointerException is
     * thrown.
     */
    @Test(expectedExceptions = PolicyException.class)
    public final void setTpmBaselinesNull() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.setTpmWhiteListBaselines(null);
    }

    /**
     * Tests that if setting the policy's TPMBaselines to an empty set, the policy will contain
     * no baselines.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void setTpmBaselineEmptyCollection() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.setTpmWhiteListBaselines(Collections.EMPTY_LIST);
        Assert.assertEquals(tpmPolicy.getBaselines().size(), 0);
    }


    /**
     * Tests that adding a device-specific PCR works correctly.
     */
    @Test
    public final void addToDeviceSpecificPCRs() {
        final int pcr4 = 4;
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.addToDeviceSpecificPCRs(pcr4);
        Set<Integer> deviceSpecificPCRs = tpmPolicy.getDeviceSpecificPCRs();
        Assert.assertEquals(deviceSpecificPCRs.size(), 1);
        Assert.assertEquals(deviceSpecificPCRs.toArray()[0], pcr4);
    }

    /**
     * Tests that adding an invalid PCR ID value throws an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void addToDeviceSpecificPCRsInvalidValue() {
        final int pcr35 = 35;
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.addToDeviceSpecificPCRs(pcr35);
    }

    /**
     * Tests that adding a negative PCR ID value causes an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void addToDeviceSpecificPCRsNegativeValue() {
        final int pcrId = -1;
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.addToDeviceSpecificPCRs(pcrId);
    }

    /**
     * Tests addToDeviceSpecificPCRs() silently ignores adding duplicate PCR IDs.
     */
    @Test
    public final void addToDeviceSpecificPCRsDuplicates() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        final int pcrId = 4;
        tpmPolicy.addToDeviceSpecificPCRs(pcrId);
        tpmPolicy.addToDeviceSpecificPCRs(pcrId);
        Set<Integer> deviceSpecificPCRList = tpmPolicy.getDeviceSpecificPCRs();
        Assert.assertNotNull(deviceSpecificPCRList);
        Assert.assertEquals(deviceSpecificPCRList.size(), 1);
        Assert.assertTrue(deviceSpecificPCRList.contains(pcrId));
    }

    /**
     * Tests that isInDeviceSpecificPCRs() returns true if the ID is found.
     */
    @Test
    public final void isInDeviceSpecificPCRs() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        final int pcr4 = Integer.valueOf(4);
        final int anotherPcr4 = 4;
        tpmPolicy.addToDeviceSpecificPCRs(pcr4);
        Assert.assertTrue(tpmPolicy.isInDeviceSpecificPCRs(anotherPcr4));
    }

    /**
     * Tests that isInDeviceSpecificPCRs() returns false if the ID is not found.
     */
    @Test
    public final void isInDeviceSpecificPCRsReturnsFalse() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        final int pcr4 = 4;
        final int pcr10 = 10;
        tpmPolicy.addToDeviceSpecificPCRs(pcr4);
        Assert.assertFalse(tpmPolicy.isInDeviceSpecificPCRs(pcr10));
    }

    /**
     * Tests that removeFromDeviceSpecificPCRs removes appropriate PCR ID from the baseline.
     */
    @Test
    public final void removeFromDeviceSpecificPCRs() {
        final int pcrToRemove = 0;
        final int pcrToKeep = 10;
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.addToDeviceSpecificPCRs(pcrToRemove);
        tpmPolicy.addToDeviceSpecificPCRs(pcrToKeep);
        Assert.assertTrue(tpmPolicy.removeFromDeviceSpecificPCRs(pcrToRemove));
        Assert.assertTrue(tpmPolicy.isInDeviceSpecificPCRs(pcrToKeep));
        Assert.assertFalse(tpmPolicy.isInDeviceSpecificPCRs(pcrToRemove));
    }

    /**
     * Tests that a TPM policy with no associated baselines can be persisted and retrieved.
     *
     * @throws Exception if an error is encountered while reading the baseline's input stream
     */
    @Test
    public final void persistNoBaselines() throws Exception {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");

        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        UUID policyId = (UUID) session.save(tpmPolicy);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        TPMPolicy policy = (TPMPolicy) session.get(TPMPolicy.class, policyId);
        session.getTransaction().commit();

        Assert.assertEquals(policy.getBaselines().size(), 0);
    }

    /**
     * Tests that a TPM policy with multiple associated baselines can be persisted and retrieved
     * with all baseline associations intact.
     *
     * @throws Exception if an error is encountered while reading the baseline's input stream
     */
    @Test
    public final void persistMultipleBaselines() throws Exception {
        TpmWhiteListBaseline baseline =
                createTestWhiteListBaseline("TestTpmPolicyBaseline", BASELINE_PATH);
        TpmWhiteListBaseline anotherBaseline = createTestWhiteListBaseline(
                "AnotherTestTpmPolicyBaseline", BASELINE_PATH
        );
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.setTpmWhiteListBaselines(Arrays.asList(baseline, anotherBaseline));

        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        session.save(baseline);
        session.save(anotherBaseline);
        UUID policyId = (UUID) session.save(tpmPolicy);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        TPMPolicy policy = (TPMPolicy) session.get(TPMPolicy.class, policyId);
        session.getTransaction().commit();

        Assert.assertEquals(policy.getBaselines().size(), 2);
        Assert.assertTrue(policy.getBaselines().contains(baseline));
        Assert.assertTrue(policy.getBaselines().contains(anotherBaseline));
    }

    /**
     * Tests that TPM policies that share baselines can be persisted and retrieved with
     * all baseline associations intact.
     *
     * @throws Exception if an error is encountered while reading the baseline's input stream
     */
    @Test
    public final void persistMultipleBaselinesMultiplePolicies() throws Exception {
        TpmWhiteListBaseline baseline1 =
                createTestWhiteListBaseline("TestTpmPolicyBaseline1", BASELINE_PATH);
        TpmWhiteListBaseline baseline2 =
                createTestWhiteListBaseline("TestTpmPolicyBaseline2", BASELINE_PATH);
        TpmWhiteListBaseline baseline3 =
                createTestWhiteListBaseline("TestTpmPolicyBaseline3", BASELINE_PATH);
        TpmWhiteListBaseline baseline4 =
                createTestWhiteListBaseline("TestTpmPolicyBaseline4", BASELINE_PATH);

        TPMPolicy tpmPolicyA = new TPMPolicy("TestTPMPolicyA");
        TPMPolicy tpmPolicyB = new TPMPolicy("TestTPMPolicyB");

        tpmPolicyA.setTpmWhiteListBaselines(Arrays.asList(baseline1, baseline2, baseline3));
        tpmPolicyB.setTpmWhiteListBaselines(Arrays.asList(baseline3, baseline4));

        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        session.save(baseline1);
        session.save(baseline2);
        session.save(baseline3);
        session.save(baseline4);
        UUID policyAId = (UUID) session.save(tpmPolicyA);
        UUID policyBId = (UUID) session.save(tpmPolicyB);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        tpmPolicyA = (TPMPolicy) session.get(TPMPolicy.class, policyAId);
        tpmPolicyB = (TPMPolicy) session.get(TPMPolicy.class, policyBId);
        session.getTransaction().commit();

        Assert.assertEquals(tpmPolicyA.getBaselines().size(), THREE);
        Assert.assertEquals(tpmPolicyB.getBaselines().size(), TWO);
        Assert.assertTrue(tpmPolicyA.getBaselines().containsAll(
                Arrays.asList(baseline1, baseline2, baseline3)
        ));
        Assert.assertTrue(tpmPolicyB.getBaselines().containsAll(
                Arrays.asList(baseline3, baseline4)
        ));
    }

    /**
     * Tests that a TPM policy with multiple associated baselines can be updated to contain no
     * associated baselines.
     *
     * @throws Exception if an error is encountered while reading the baseline's input stream
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void updatePolicyToNoBaselines() throws Exception {
        TpmWhiteListBaseline baseline =
                createTestWhiteListBaseline("TestTpmPolicyBaseline", BASELINE_PATH);
        TpmWhiteListBaseline anotherBaseline = createTestWhiteListBaseline(
                "AnotherTestTpmPolicyBaseline", BASELINE_PATH
        );
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.setTpmWhiteListBaselines(Arrays.asList(baseline, anotherBaseline));

        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        session.save(baseline);
        session.save(anotherBaseline);
        UUID policyId = (UUID) session.save(tpmPolicy);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        TPMPolicy policy = (TPMPolicy) session.get(TPMPolicy.class, policyId);
        session.getTransaction().commit();

        policy.setTpmWhiteListBaselines(Collections.EMPTY_LIST);

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        session.update(policy);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        policy = (TPMPolicy) session.get(TPMPolicy.class, policyId);
        session.getTransaction().commit();

        Assert.assertEquals(policy.getBaselines().size(), 0);
    }

    /**
     * Tests setting and getting the appraisalFullReport flag.
     */
    @Test
    public final void setAppraiseFullReport() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.setAppraiseFullReport(false);
        Assert.assertFalse(tpmPolicy.isAppraiseFullReport());
    }

    /**
     * Tests setting and getting the validateSignature flag.
     */
    @Test
    public final void setValidateSignature() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.setValidateSignature(true);
        Assert.assertTrue(tpmPolicy.isValidateSignature());
    }

    /**
     * Tests that the default validateSignature flag is true.
     */
    @Test
    public final void defaultValidateSignature() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        Assert.assertTrue(tpmPolicy.isValidateSignature());
    }

    /**
     * Tests setting and getting the detectKernelUpdate flag.
     */
    @Test
    public final void testSetDetectKernelUpdate() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        Assert.assertTrue(tpmPolicy.isDetectKernelUpdateEnabled(),
            "The default setting was expected to be true.");
        tpmPolicy.setDetectKernelUpdate(false);
        Assert.assertFalse(tpmPolicy.isDetectKernelUpdateEnabled());
    }

    /**
     * Tests setting and getting the alertOnKernelUpdate flag.
     */
    @Test
    public final void testSetAlertOnKernelUpdate() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        Assert.assertTrue(tpmPolicy.isAlertOnKernelUpdateEnabled(),
            "The default setting was expected to be true.");
        tpmPolicy.setAlertOnKernelUpdate(false);
        Assert.assertFalse(tpmPolicy.isAlertOnKernelUpdateEnabled());
    }

    /**
     * Tests setting and getting the kernel update alert severity.
     */
    @Test
    public final void testSetKernelUpdateAlertSeverity() {
        final AlertSeverity defaultSeverity = AlertSeverity.UNSPECIFIED;
        final AlertSeverity newSeverity = AlertSeverity.INFO;
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        Assert.assertEquals(tpmPolicy.getKernelUpdateAlertSeverity(), defaultSeverity);
        tpmPolicy.setKernelUpdateAlertSeverity(newSeverity);
        Assert.assertEquals(tpmPolicy.getKernelUpdateAlertSeverity(), newSeverity);
    }

    /**
     * Tests setting the AppraisePcrMask.
     */
    @Test
    public final void setAppraisePcrMask() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.setAppraisePcrMask(TPMPolicy.ALL_PCR_MASK);
        Assert.assertEquals(tpmPolicy.getAppraisePcrMask(),
                TPMPolicy.ALL_PCR_MASK);
    }

    /**
     * Tests that invalid appraiseMask value causes IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void setInvalidAppraiseMask() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.setAppraisePcrMask(INVALID_PCR_MASK);
    }

    /**
     * Tests setting the ReportPcrMask.
     */
    @Test
    public final void setReportPcrMask() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.setReportPcrMask(TPMPolicy.ALL_PCR_MASK);
        Assert.assertEquals(tpmPolicy.getReportPcrMask(),
                TPMPolicy.ALL_PCR_MASK);
    }

    /**
     * Tests that invalid reportMask value causes IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void setInvalidReportMask() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.setReportPcrMask(INVALID_PCR_MASK);
    }

    /**
     * Tests that zeroed reportMask value causes IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void setZeroedReportMask() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.setReportPcrMask(0);
    }

    /**
     * Tests that isPcrReported() returns true if the ID is found.
     */
    @Test
    public final void isPcrReported() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        final int pcr4 = 4;
        final int pcr8 = 8;
        // Set Reported PCRs to 0-7
        tpmPolicy.setReportPcrMask(0x0000FF);
        Assert.assertTrue(tpmPolicy.isPcrReported(pcr4));
        Assert.assertFalse(tpmPolicy.isPcrReported(pcr8));
    }

    /**
     * Tests setting the KernelPcrMask.
     */
    @Test
    public final void testSetKernelPcrMask() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.setKernelPcrMask(TPMPolicy.ALL_PCR_MASK);
        Assert.assertEquals(tpmPolicy.getKernelPcrMask(),
                TPMPolicy.ALL_PCR_MASK);
    }

    /**
     * Tests that invalid kernelPcrMask value causes IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void setInvalidKernelPcrMask() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.setKernelPcrMask(INVALID_PCR_MASK);
    }

    /**
    * Tests that an accurate PCR mask can be calculated.
    */
    @Test
    public final void testCalculatePcrMask() {
        final List<Integer> list = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 10, 15);
        final int expected = 0x0084FF;

        Assert.assertEquals(TPMPolicy.calculatePcrMask(list), expected);
    }

    /**
     * Tests setting PCR Appraised.
     */
    @Test
    public final void setPcrAppraised() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        Assert.assertEquals(tpmPolicy.getAppraisePcrMask(), 0);
        for (int i = 0; i < 24; i++) {
            tpmPolicy.setPcrAppraised(i);
        }
        Assert.assertEquals(tpmPolicy.getAppraisePcrMask(), 0xFFFFFF);
    }

    /**
     * Tests that setPcrAppraised throws an exception if a PCR is attempting to be set for
     * appraisal, but it's not being actively reported by the Policy.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void setPcrAppraisedNotReporting() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.setReportPcrMask(1);
        Assert.assertEquals(tpmPolicy.getAppraisePcrMask(), 0);
        tpmPolicy.setPcrAppraised(2);
    }

    /**
     * Tests that setPcrAppraised throws an exception if a PCR is attempting to be set for
     * appraisal, but it's a valid PCR between 0-23.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void setPcrAppraisedInvalidPcr() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.setPcrAppraised(24);
    }

    /**
     * Tests setting PCR not appraised.
     */
    @Test
    public final void setPcrNotAppraised() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.setAppraisePcrMask(0xFFFFFF);
        Assert.assertEquals(tpmPolicy.getAppraisePcrMask(), 0xFFFFFF);
        for (int i = 0; i < 24; i++) {
            tpmPolicy.setPcrNotAppraised(i);
        }
        Assert.assertEquals(tpmPolicy.getAppraisePcrMask(), 0);
    }

    /**
     * Tests that isPcrAppraised() returns true if the ID is found.
     */
    @Test
    public final void isPcrAppraised() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        final int pcr4 = 4;
        final int pcr8 = 8;
        // Set Appraised PCRs to 0-7
        tpmPolicy.setAppraisePcrMask(0x0000FF);
        Assert.assertTrue(tpmPolicy.isPcrAppraised(pcr4));
        Assert.assertFalse(tpmPolicy.isPcrAppraised(pcr8));
    }

    /**
     * Tests clearing all PCR appraisal values.
     */
    @Test
    public final void clearAllPcrAppraisalValues() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        for (int i = 0; i < 24; i++) {
            tpmPolicy.setPcrAppraised(i);
            tpmPolicy.addToDeviceSpecificPCRs(i);
        }
        tpmPolicy.clearAllPcrAppraisalValues();
        Assert.assertEquals(tpmPolicy.getAppraisePcrMask(), 0);
        Assert.assertEquals(tpmPolicy.getDeviceSpecificPCRs().size(), 0);
    }

    /**
     * Test setting default device specific PCRs.
     */
    @Test
    public final void setDefaultPcrAppraisalValues() {
        TPMPolicy tpmPolicy = new TPMPolicy("TestTPMPolicy");
        tpmPolicy.setDefaultPcrAppraisalValues();
        Set<Integer> defaults = new HashSet<>();
        for (int i : TPMPolicy.DEFAULT_DEVICE_SPECIFIC_PCRS) {
            defaults.add(i);
            Assert.assertTrue(tpmPolicy.isPcrAppraised(i));
        }
        Assert.assertTrue(tpmPolicy.getDeviceSpecificPCRs().containsAll(defaults));
        Assert.assertEquals(tpmPolicy.getDeviceSpecificPCRs().size(), defaults.size());
    }

    @Override
    protected final TPMPolicy getDefault(final Session session) {
        final TPMPolicy policy = new TPMPolicy("Test Policy");
        TpmWhiteListBaseline baseline = null;

        try {
            baseline = createTestWhiteListBaseline("TestTpmPolicyBaseline",
                    BASELINE_PATH);
        } catch (Exception e) {
            Assert.fail("Failed to Open TPMTestBaseline.csv file as resource");
        }

        final Serializable baselineID = session.save(baseline);
        baseline = (TpmWhiteListBaseline) session.get(TpmWhiteListBaseline.class, baselineID);

        policy.setTpmWhiteListBaseline(baseline);
        policy.setAppraiseFullReport(true);
        policy.setReportPcrMask(TPMPolicy.ALL_PCR_MASK);
        policy.setAppraisePcrMask(TPMPolicy.ALL_PCR_MASK);
        return policy;
    }

    @Override
    protected final Class<?> getDefaultClass() {
        final TPMPolicy policy = new TPMPolicy("TPM Test Policy");
        return policy.getClass();
    }

    @Override
    protected final void assertGetEqual(final TPMPolicy defaultObject,
            final TPMPolicy retrieved) {
        Assert.assertEquals(retrieved, defaultObject);
        Assert.assertEquals(retrieved.isAppraiseFullReport(), defaultObject.isAppraiseFullReport());
        Assert.assertEquals(retrieved.getAppraisePcrMask(), defaultObject.getAppraisePcrMask());
        Assert.assertEquals(retrieved.getReportPcrMask(), defaultObject.getReportPcrMask());
        Assert.assertEquals(retrieved.getTpmWhiteListBaselines(),
                defaultObject.getTpmWhiteListBaselines());
    }

    @Override
    protected final void update(final TPMPolicy object) {
        object.setAppraiseFullReport(false);

    }

    @Override
    protected final void assertUpdateEqual(final TPMPolicy defaultObject,
            final TPMPolicy update) {
        Assert.assertFalse(update.isAppraiseFullReport());
    }

    @Override
    protected final Class<?>[] getCleanupClasses() {
        return new Class<?>[] {TPMPolicy.class, TPMBaseline.class};
    }

    private TpmWhiteListBaseline createTestWhiteListBaseline(final String name, final String path)
            throws Exception {
        InputStream in = null;
        TPMBaselineGenerator baselineCreator = new TPMBaselineGenerator();
        try {
            in = this.getClass().getResourceAsStream(path);
            return baselineCreator.generateWhiteListBaselineFromCSVFile(name, in);
        } catch (Exception e) {
            throw e;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
