package hirs.data.persist;

import hirs.data.persist.enums.DigestAlgorithm;
import hirs.data.persist.baseline.ImaBaseline;
import hirs.data.persist.baseline.ImaIgnoreSetBaseline;
import hirs.data.persist.baseline.ImaAcceptableRecordBaseline;
import hirs.data.persist.baseline.IMABaselineRecord;
import hirs.data.persist.baseline.SimpleImaBaseline;
import hirs.data.persist.baseline.Baseline;
import java.io.InputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import hirs.appraiser.AppraiserTestUtil;
import hirs.ima.SimpleImaBaselineGenerator;

import java.util.UUID;

/**
 * IMAPolicyTest is a unit test class for IMAPolicy.
 */
public class IMAPolicyTest extends HibernateTest<IMAPolicy> {
    private static final Logger LOGGER = LogManager.getLogger(IMAPolicyTest.class);
    private static final String BASELINE_PATH = "/ima/IMATestBaseline.csv";

    private IMAPolicy policy1 = new IMAPolicy("policy1");
    private IMAPolicy policy2 = new IMAPolicy("policy2");
    private IMABaselineRecord record1 = createTestIMARecord("file1",
            "feedbeefedeedebfeededbeefebeedebfeeddeeb");
    private IMABaselineRecord record2 = createTestIMARecord("file2",
            "baadbaadbaaaaaaaaaaaaaaaaaaaaaaaaaaaaaad");
    private IMABaselineRecord record1Copy = createTestIMARecord("file1",
            "feedbeefedeedebfeededbeefebeedebfeeddeeb");
    private IMABaselineRecord record2Copy = createTestIMARecord("file2",
            "baadbaadbaaaaaaaaaaaaaaaaaaaaaaaaaaaaaad");
    private ImaIgnoreSetRecord oRecord1 = new ImaIgnoreSetRecord("file1", "description one");
    private ImaIgnoreSetRecord oRecord2 = new ImaIgnoreSetRecord("file2", "description two");
    private ImaIgnoreSetRecord oRecord1Copy = new ImaIgnoreSetRecord("file1", "description one");
    private ImaIgnoreSetRecord oRecord2Copy = new ImaIgnoreSetRecord("file2", "description two");
    private SimpleImaBaseline baseline1 = new SimpleImaBaseline("baseline1");
    private SimpleImaBaseline baseline2 = new SimpleImaBaseline("baseline2");
    private ImaIgnoreSetBaseline oSet1 = new ImaIgnoreSetBaseline("oset1");
    private ImaIgnoreSetBaseline oSet2 = new ImaIgnoreSetBaseline("oset2");
    private List<ImaAcceptableRecordBaseline> baselineList1 = new LinkedList<>();
    private List<ImaAcceptableRecordBaseline> baselineList2 = new LinkedList<>();
    private List<ImaIgnoreSetBaseline> oSetList1 = new LinkedList<>();
    private List<ImaIgnoreSetBaseline> oSetList2 = new LinkedList<>();

    @Autowired
    private SessionFactory factory;

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an
     * in-memory database that is used for testing.
     */
    @BeforeClass
    public final void initFactory() {
        LOGGER.debug("retrieving session factory");
    }

    /**
     * Reset the test objects in local memory (not in the database).
     */
    @BeforeMethod
    public final void reset() {
        initializeTestObjects();
    }
    /**
     * Tests instantiation of IMAPolicy object.
     */
    @Test
    public final void imaPolicy() {
        new IMAPolicy("TestIMAPolicy");
    }

    /**
     * Tests that IMAPolicy constructor throws a NullPointerException with null
     * name.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void imaPolicyNullName() {
        new IMAPolicy(null);
    }

    /**
     * Tests getting the white list of the policy.
     */
    @Test
    public final void getWhiteListIMAPolicy() {
        IMAPolicy imaPolicy = new IMAPolicy("TestIMAPolicy");
        Assert.assertEquals(null, imaPolicy.getWhitelist());
    }

    /**
     * Tests setting the white list in the policy.
     *
     * @throws Exception
     *             thrown if error generated reading input stream
     */
    @Test
    public final void setWhiteListIMAPolicy() throws Exception {
        IMAPolicy imaPolicy = new IMAPolicy("TestIMAPolicy");
        ImaAcceptableRecordBaseline baseline;
        String name = "TestWhiteListIMABaseline";
        baseline = createTestBaseline(name, BASELINE_PATH);
        imaPolicy.setWhitelist(baseline);
        Assert.assertEquals(name, imaPolicy.getWhitelist().getName());
    }

    /**
     * Tests persistence of whitelists by setting equivalent whitelists to two
     * policies and checking whether they are still the same after they are
     * saved and retrieved.
     */
    @Test
    public final void persistWhiteLists() {
        LOGGER.debug("testing whitelist persistence");
        baseline1.addToBaseline(record1);
        baseline1.addToBaseline(record2);
        baseline2.addToBaseline(record1Copy);
        baseline2.addToBaseline(record2Copy);
        baselineList1.add(baseline1);
        baselineList1.add(baseline2);
        baselineList2.add(baseline1);
        baselineList2.add(baseline2);
        policy1.setWhitelists(baselineList1);
        policy2.setWhitelists(baselineList2);

        Session session = factory.getCurrentSession();
        session.beginTransaction();
        session.save(baseline1);
        session.save(baseline2);
        final UUID pid1 = (UUID) session.save(policy1);
        final UUID pid2 = (UUID) session.save(policy2);
        session.getTransaction().commit();

        session = factory.getCurrentSession();
        session.beginTransaction();
        IMAPolicy getPolicy1 = (IMAPolicy) session.get(IMAPolicy.class, pid1);
        session.getTransaction().commit();
        session = factory.getCurrentSession();
        session.beginTransaction();
        IMAPolicy getPolicy2 = (IMAPolicy) session.get(IMAPolicy.class, pid2);
        session.getTransaction().commit();

        Assert.assertEquals(getPolicy1.getWhitelists(),
                getPolicy2.getWhitelists());
    }

    /**
     * Tests that the policy handles getting a request to set the white list to
     * null.
     */
    @Test
    public final void setWhiteListIMAPolicyNull() {
        IMAPolicy imaPolicy = new IMAPolicy("TestIMAPolicy");
        imaPolicy.setWhitelist(null);
        Assert.assertEquals(null, imaPolicy.getWhitelist());
    }

    /**
     * Tests getting the required set of the policy.
     */
    @Test
    public final void getRequiredSetIMAPolicy() {
        IMAPolicy imaPolicy = new IMAPolicy("TestIMAPolicy");
        Assert.assertEquals(null, imaPolicy
                .getRequiredSet());
    }

    /**
     * Tests setting the required set in the policy.
     *
     * @throws Exception
     *             thrown if error generated reading input stream
     */
    @Test
    public final void setRequiredSetIMAPolicy() throws Exception {
        IMAPolicy imaPolicy = new IMAPolicy("TestIMAPolicy");
        SimpleImaBaseline baseline;
        String name = "TestRequiredSetIMABaseline";
        baseline = createTestBaseline(name, BASELINE_PATH);
        imaPolicy.setRequiredSet(baseline);
        Assert.assertEquals(name, imaPolicy.getRequiredSet().getName());
    }

    /**
     * Tests persistence of required sets by setting equivalent required sets to
     * two policies and checking whether they are still the same after they are
     * saved and retrieved.
     */
    @Test
    public final void persistRequiredSets() {
        LOGGER.debug("testing required set persistence");
        baseline1.addToBaseline(record1);
        baseline1.addToBaseline(record2);
        baseline2.addToBaseline(record1Copy);
        baseline2.addToBaseline(record2Copy);
        baselineList1.add(baseline1);
        baselineList1.add(baseline2);
        baselineList2.add(baseline1);
        baselineList2.add(baseline2);
        policy1.setRequiredSets(baselineList1);
        policy2.setRequiredSets(baselineList2);

        Session session = factory.getCurrentSession();
        session.beginTransaction();
        session.save(baseline1);
        session.save(baseline2);
        final UUID pid1 = (UUID) session.save(policy1);
        final UUID pid2 = (UUID) session.save(policy2);
        session.getTransaction().commit();

        session = factory.getCurrentSession();
        session.beginTransaction();
        IMAPolicy getPolicy1 = (IMAPolicy) session.get(IMAPolicy.class, pid1);
        session.getTransaction().commit();
        session = factory.getCurrentSession();
        session.beginTransaction();
        IMAPolicy getPolicy2 = (IMAPolicy) session.get(IMAPolicy.class, pid2);
        session.getTransaction().commit();

        Assert.assertEquals(getPolicy1.getRequiredSets(),
                getPolicy2.getRequiredSets());
    }

    /**
     * Tests that the policy handles getting a request to set the required set
     * to null.
     */
    @Test
    public final void setRequiredSetIMAPolicyNull() {
        IMAPolicy imaPolicy = new IMAPolicy("TestIMAPolicy");
        imaPolicy.setRequiredSet(null);
    }

    /**
     * Tests getting the ignore set of the policy.
     */
    @Test
    public final void getIgnoreSetIMAPolicy() {
        IMAPolicy imaPolicy = new IMAPolicy("TestIMAPolicy");
        Assert.assertEquals(null, imaPolicy
                .getImaIgnoreSetBaseline());
    }

    /**
     * Tests setting the ignore set in the policy.
     */
    @Test
    public final void setIgnoreSetIMAPolicy() {
        IMAPolicy imaPolicy = new IMAPolicy("TestIMAPolicy");
        String name = "TestIgnoreSetPolicy";
        ImaIgnoreSetBaseline optionalSetPolicy;
        optionalSetPolicy = new ImaIgnoreSetBaseline(name);
        imaPolicy.setImaIgnoreSetBaseline(optionalSetPolicy);
        Assert.assertEquals(name, imaPolicy.getImaIgnoreSetBaseline().getName());
    }

    /**
     * Tests that the policy handles getting a request to set the ignore set
     * to null.
     *
     * @throws Exception
     *             thrown if error generated reading input stream
     */
    @Test
    public final void setIgnoreSetIMAPolicyNull() throws Exception {
        IMAPolicy imaPolicy = new IMAPolicy("TestIMAPolicy");
        imaPolicy.setImaIgnoreSetBaseline(null);
    }

    /**
     * Tests setting and getting the ignore sets of multiple policies
     * returns the same lists when the policies are persisted.
     */
    @Test
    public final void persistIgnoreSets() {
        LOGGER.debug("testing ignore set persistence");
        oSet1.addToBaseline(oRecord1);
        oSet1.addToBaseline(oRecord2);
        oSet2.addToBaseline(oRecord1Copy);
        oSet2.addToBaseline(oRecord2Copy);
        oSetList1.add(oSet1);
        oSetList1.add(oSet2);
        oSetList2.add(oSet1);
        oSetList2.add(oSet2);
        policy1.setIgnoreSets(oSetList1);
        policy2.setIgnoreSets(oSetList2);

        Session session = factory.getCurrentSession();
        session.beginTransaction();
        session.save(oSet1);
        session.save(oSet2);
        final UUID pid1 = (UUID) session.save(policy1);
        final UUID pid2 = (UUID) session.save(policy2);
        session.getTransaction().commit();

        session = factory.getCurrentSession();
        session.beginTransaction();
        IMAPolicy getPolicy1 = (IMAPolicy) session.get(IMAPolicy.class, pid1);
        session.getTransaction().commit();
        session = factory.getCurrentSession();
        session.beginTransaction();
        IMAPolicy getPolicy2 = (IMAPolicy) session.get(IMAPolicy.class, pid2);
        session.getTransaction().commit();

        Assert.assertEquals(getPolicy1.getIgnoreSets(),
                getPolicy2.getIgnoreSets());
    }

    /**
     * Tests that the default value for PCR validation is returned correctly.
     */
    @Test
    public final void isDefaultValidatePcr() {
        IMAPolicy imaPolicy = new IMAPolicy("TestIMAPolicy");
        Assert.assertFalse(imaPolicy.isValidatePcr());
    }

    /**
     * Tests that the PCR validation property can be set.
     */
    @Test
    public final void setValidatePcr() {
        IMAPolicy imaPolicy = new IMAPolicy("TestIMAPolicy");
        imaPolicy.setValidatePcr(true);
        Assert.assertTrue(imaPolicy.isValidatePcr());
    }

    /**
     * Tests that the deltaReportEnable flag can be set.
     */
    @Test
    public final void setDeltaReportEnable() {
        IMAPolicy imaPolicy = new IMAPolicy("TestIMAPolicy");
        Assert.assertTrue(imaPolicy.isDeltaReportEnable());
        imaPolicy.setDeltaReportEnable(false);
        Assert.assertFalse(imaPolicy.isDeltaReportEnable());
    }

    /**
     * Tests getting the deltaReportEnable flag.
     */
    @Test
    public final void isDeltaReportEnable() {
        IMAPolicy imaPolicy = new IMAPolicy("TestIMAPolicy");
        Assert.assertTrue(imaPolicy.isDeltaReportEnable());
    }

    /**
     * Tests that the partialPathEnable flag can be set.
     */
    @Test
    public final void setPartialPathEnable() {
        IMAPolicy imaPolicy = new IMAPolicy("TestIMAPolicy");
        Assert.assertTrue(imaPolicy.isPartialPathEnable());
        imaPolicy.setPartialPathEnable(false);
        Assert.assertFalse(imaPolicy.isPartialPathEnable());
    }

    /**
     * Tests getting the partialPathEnable flag.
     */
    @Test
    public final void isPartialPathEnable() {
        IMAPolicy imaPolicy = new IMAPolicy("TestIMAPolicy");
        Assert.assertTrue(imaPolicy.isPartialPathEnable());
    }

    @Override
    protected final Class<?> getDefaultClass() {
        final IMAPolicy policy = new IMAPolicy("Test Policy");
        return policy.getClass();
    }

    @Override
    protected final IMAPolicy getDefault(final Session session) {
        final IMAPolicy policy = new IMAPolicy("Test Policy");

        SimpleImaBaseline whitelist = AppraiserTestUtil.getTestWhitelist();
        final Serializable whitelistID = session.save(whitelist);
        whitelist = (SimpleImaBaseline) session.get(ImaBaseline.class, whitelistID);
        SimpleImaBaseline requiredSet = AppraiserTestUtil.getTestRequiredSet();
        final Serializable requiredSetID = session.save(requiredSet);
        requiredSet = (SimpleImaBaseline) session.get(ImaBaseline.class,
                requiredSetID);
        ImaIgnoreSetBaseline ignoreSet = AppraiserTestUtil.getTestIgnoreSet();
        final Serializable ignoreSetID = session.save(ignoreSet);
        ignoreSet = (ImaIgnoreSetBaseline) session.get(ImaIgnoreSetBaseline.class,
                ignoreSetID);

        policy.setWhitelist(whitelist);
        policy.setRequiredSet(requiredSet);
        policy.setImaIgnoreSetBaseline(ignoreSet);
        policy.setFailOnUnknowns(true);
        return policy;
    }

    @Override
    protected final void update(final IMAPolicy object) {
        object.setFailOnUnknowns(true);
        object.setValidatePcr(true);
    }

    @Override
    protected final void assertGetEqual(final IMAPolicy defaultObject,
            final IMAPolicy retrieved) {
        Assert.assertEquals(defaultObject, retrieved);
        Assert.assertEquals(retrieved.getWhitelist(),
                defaultObject.getWhitelist());
        Assert.assertEquals(retrieved.getRequiredSet(),
                defaultObject.getRequiredSet());
        Assert.assertEquals(retrieved.getImaIgnoreSetBaseline(),
                defaultObject.getImaIgnoreSetBaseline());
    }

    @Override
    protected final void assertUpdateEqual(final IMAPolicy defaultObject,
            final IMAPolicy update) {
        Assert.assertTrue(update.isFailOnUnknowns());
        Assert.assertTrue(update.isValidatePcr());
    }

    @Override
    protected final Class<?>[] getCleanupClasses() {
        return new Class<?>[] {IMAPolicy.class, Baseline.class,
                ImaIgnoreSetBaseline.class, IMABaselineRecord.class};
    }

    private void initializeTestObjects() {
        policy1 = new IMAPolicy("policy1");
        policy2 = new IMAPolicy("policy2");
        record1 = createTestIMARecord("file1",
                 "feedbeefedeedebfeededbeefebeedebfeeddeeb");
        record2 = createTestIMARecord("file2",
                "baadbaadbaaaaaaaaaaaaaaaaaaaaaaaaaaaaaad");
        record1Copy = createTestIMARecord("file1",
                "feedbeefedeedebfeededbeefebeedebfeeddeeb");
        record2Copy = createTestIMARecord("file2",
                "baadbaadbaaaaaaaaaaaaaaaaaaaaaaaaaaaaaad");
        oRecord1 = new ImaIgnoreSetRecord("file1", "description one");
        oRecord2 = new ImaIgnoreSetRecord("file2", "description two");
        oRecord1Copy = new ImaIgnoreSetRecord("file1", "description one");
        oRecord2Copy = new ImaIgnoreSetRecord("file2", "description two");
        baseline1 = new SimpleImaBaseline("baseline1");
        baseline2 = new SimpleImaBaseline("baseline2");
        oSet1 = new ImaIgnoreSetBaseline("oset1");
        oSet2 = new ImaIgnoreSetBaseline("oset2");
        baselineList1 = new LinkedList<>();
        baselineList2 = new LinkedList<>();
        oSetList1 = new LinkedList<>();
        oSetList2 = new LinkedList<>();
    }

    /**
     * Creates a baseline for testing purposes.
     *
     * @param path
     *            to baseline file to process
     * @return
     * @throws Exception
     */
    private SimpleImaBaseline createTestBaseline(final String name, final String path)
            throws Exception {
        InputStream in = null;
        SimpleImaBaselineGenerator baselineCreator = new SimpleImaBaselineGenerator();
        try {
            in = this.getClass().getResourceAsStream(BASELINE_PATH);
            return baselineCreator.generateBaselineFromCSVFile(name, in);
        } catch (Exception e) {
            throw e;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private IMABaselineRecord createTestIMARecord(final String fileName,
                final String sha1) {
        try {
            final byte[] hash = Hex.decodeHex(sha1.toCharArray());
            final Digest digest = new Digest(DigestAlgorithm.SHA1, hash);
            return new IMABaselineRecord(fileName, digest);
        } catch (Exception e) {
            LOGGER.error("unexpected exception", e);
            throw new RuntimeException("unexpected exception", e);
        }
    }
}
