package hirs.data.persist;

import hirs.data.persist.enums.DigestAlgorithm;
import hirs.data.persist.baseline.ImaIgnoreSetBaseline;
import hirs.data.persist.baseline.Baseline;
import hirs.ima.matching.BatchImaMatchStatus;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import hirs.persist.ImaBaselineRecordManager;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;

/**
 * ImaIgnoreSetBaselineTest is a unit test class for ImaIgnoreSetBaseline.
 */
public class ImaIgnoreSetBaselineTest extends SpringPersistenceTest {

    private static final Logger LOGGER = LogManager.getLogger(ImaIgnoreSetBaselineTest.class);
    private static final String PATH_RECORD_1 = "/path/to/file1";
    private static final String PATH_RECORD_3 = "/path/to/file2";
    private static final String HASH_RECORD_2 = "20003c2f7f3003d2e4baddc46ed4763a49540002";

    private ImaBaselineRecordManager recordManager;
    private IMAPolicy policyEnabledPartialPath;
    private IMAPolicy policyDisabledPartialPath;

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an
     * in-memory database that is used for testing.
     */
    @BeforeClass
    public final void setup() {
        LOGGER.debug("retrieving session factory");
        recordManager = mock(ImaBaselineRecordManager.class);
        policyEnabledPartialPath = new IMAPolicy("Test Policy - Partial Path Enabled");
        policyEnabledPartialPath.setPartialPathEnable(true);
        policyDisabledPartialPath = new IMAPolicy("Test Policy - Partial Path Disabled");
        policyDisabledPartialPath.setPartialPathEnable(false);
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterClass
    public final void tearDown() {
        LOGGER.debug("closing session factory");
    }

    /**
     * Resets the test state to a known good state. This currently only resets
     * the database by removing all <code>Policy</code> objects.
     */
    @AfterMethod
    public final void resetTestState() {
        LOGGER.debug("reset test state");
        LOGGER.debug("deleting all policies");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final List<?> baselines = session.createCriteria(Baseline.class).list();
        for (Object o : baselines) {
            LOGGER.debug("deleting baseline: {}", o);
            session.delete(o);
        }
        LOGGER.debug("all policies removed");
        session.getTransaction().commit();
    }

    /**
     * Tests instantiation of ImaIgnoreSetBaseline object.
     */
    @Test
    public final void imaIgnoreSetBaseline() {
        new ImaIgnoreSetBaseline("TestImaIgnoreSetBaseline");
    }

    /**
     * Tests that ImaIgnoreSetBaseline constructor throws a NullPointerException
     * with null name.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void ignoreSetPolicyNullName() {
        new ImaIgnoreSetBaseline(null);
    }

    /**
     * Tests adding IMA optional records to baseline.
     */
    @Test
    public final void addPath() {
        ImaIgnoreSetBaseline imaIgnoreSetBaseline =
                new ImaIgnoreSetBaseline("TestImaIgnoreSetBaseline");
        ImaIgnoreSetRecord imaRecord = new ImaIgnoreSetRecord("/path/to/file", "description");
        imaIgnoreSetBaseline.addToBaseline(imaRecord);
        Set<ImaIgnoreSetRecord> imaRecords = imaIgnoreSetBaseline.getImaIgnoreRecords();
        Assert.assertNotNull(imaRecords);
        Assert.assertTrue(imaRecords.contains(imaRecord));
        Assert.assertEquals(imaRecords.size(), 1);
    }

    /**
     * Tests that addPath() handles duplicate IMA records.
     */
    @Test
    public final void addDuplicateRecords() {
        ImaIgnoreSetBaseline imaIgnoreSetBaseline =
                new ImaIgnoreSetBaseline("TestImaIgnoreSetBaseline");
        ImaIgnoreSetRecord imaRecord = new ImaIgnoreSetRecord("/path/to/file", "description");
        imaIgnoreSetBaseline.addToBaseline(imaRecord);
        imaIgnoreSetBaseline.addToBaseline(imaRecord);
        Set<ImaIgnoreSetRecord> imaRecords = imaIgnoreSetBaseline.getImaIgnoreRecords();
        Assert.assertNotNull(imaRecords);
        Assert.assertTrue(imaRecords.contains(imaRecord));
        Assert.assertEquals(imaRecords.size(), 1);
    }

    /**
     * Tests that getName() returns the name of ImaIgnoreSetBaseline.
     */
    @Test
    public final void getName() {
        final String name = "TestImaIgnoreSetBaseline";
        ImaIgnoreSetBaseline imaIgnoreSetBaseline = new ImaIgnoreSetBaseline(name);
        Assert.assertEquals(imaIgnoreSetBaseline.getName(), name);
    }

    /**
     * Asserts that contains returns a MATCH when only the full file path matches
     * and partial paths are enabled.
     */
    @Test
    public final void containsFullPath() {
        final String name = "TestImaIgnoreSetBaseline";
        final ImaIgnoreSetBaseline baseline = new ImaIgnoreSetBaseline(name);
        addContainsRecords(baseline);
        final String sha1 = "10003c2f7f3003d2e4baddc46ed4763a49540001";
        final IMAMeasurementRecord record = getIMAMeasurementRecord(PATH_RECORD_1, sha1);
        Assert.assertTrue(containsMatch(baseline, record, policyEnabledPartialPath));
    }

    /**
     * Asserts that contains can treat the record path as a regex and use it for a successful match
     * and an unsuccessful match.
     */
    @Test
    public final void fullPathRegex() {
        final ImaIgnoreSetBaseline baseline = new ImaIgnoreSetBaseline("TestBaseline");
        baseline.addToBaseline(new ImaIgnoreSetRecord(".*\\/bin\\/.*\\.pyc", "compiled python"));
        final String sha1 = "10003c2f7f3003d2e4baddc46ed4763a49540001";
        final IMAMeasurementRecord record =
                getIMAMeasurementRecord("/usr/bin/scripts/runner.pyc", sha1);
        Assert.assertTrue(containsMatch(baseline, record, policyDisabledPartialPath));
        Assert.assertFalse(containsUnknown(baseline, record, policyDisabledPartialPath));

        final IMAMeasurementRecord record2 =
                getIMAMeasurementRecord("/usr/bin/scripts/runner.py", sha1);
        Assert.assertFalse(containsMatch(baseline, record2, policyDisabledPartialPath));
        Assert.assertTrue(containsUnknown(baseline, record2, policyDisabledPartialPath));
    }

    /**
     * Asserts that an ignore record with a malformed regex pattern as its path cannot be added to
     * an ignore set, and an <code>IllegalArgumentException</code> gets thrown.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void malformedRegex() {
        final ImaIgnoreSetBaseline baseline = new ImaIgnoreSetBaseline("TestBaseline");
        baseline.addToBaseline(new ImaIgnoreSetRecord(".**\\/bin\\/.*\\.pyc", "compiled python"));
    }

    /**
     * Asserts that contains returns true when only the full file path matches
     * and partial paths are disabled.
     */
    @Test
    public final void containsFullPathDisabled() {
        final String name = "TestImaIgnoreSetBaseline";
        final ImaIgnoreSetBaseline baseline = new ImaIgnoreSetBaseline(name);
        addContainsRecords(baseline);
        final String sha1 = "10003c2f7f3003d2e4baddc46ed4763a49540001";
        final IMAMeasurementRecord record =
                getIMAMeasurementRecord(PATH_RECORD_1, sha1);
        Assert.assertTrue(containsMatch(baseline, record, policyDisabledPartialPath));
        Assert.assertFalse(containsUnknown(baseline, record, policyDisabledPartialPath));
    }

    /**
     * Assert that contains returns false when record is not found.
     */
    @Test
    public final void containsUnknown() {
        final String name = "TestImaIgnoreSetBaseline";
        final ImaIgnoreSetBaseline baseline = new ImaIgnoreSetBaseline(name);
        addContainsRecords(baseline);
        final String path = "/some/unknown/file/1";
        final String sha1 = "dead3c2f7f3003d2e4baddc46ed4763a4950beef";
        final IMAMeasurementRecord record = getIMAMeasurementRecord(path, sha1);
        Assert.assertFalse(containsMatch(baseline, record, policyEnabledPartialPath));
    }

    /**
     * Tests that no match is found when a measurement record with a mismatched
     * hash and a full path is compared to a baseline measurement with the
     * same filename in a different directory while partial paths are enabled.
     */
    @Test
    public final void containsFullUnknown() {
        final String name = "TestImaIgnoreSetBaseline";
        final ImaIgnoreSetBaseline ignoreSet = new ImaIgnoreSetBaseline(name);
        ignoreSet.addToBaseline(new ImaIgnoreSetRecord("/etc/resolv.conf", "description one"));
        final IMAMeasurementRecord record =
                getIMAMeasurementRecord("/var/run/resolv.conf", HASH_RECORD_2);
        Assert.assertFalse(containsMatch(ignoreSet, record, policyEnabledPartialPath));
    }

    /**
     * Tests that no match is found when a measurement record with a mismatched
     * hash and a full path is compared to a baseline measurement with the
     * same filename in a different directory while partial paths are disabled.
     */
    @Test
    public final void containsFullUnknownDisabled() {
        final String name = "TestImaIgnoreSetBaseline";
        final ImaIgnoreSetBaseline ignoreSet = new ImaIgnoreSetBaseline(name);
        ignoreSet.addToBaseline(new ImaIgnoreSetRecord("/etc/resolv.conf", "description one"));
        final IMAMeasurementRecord record =
                getIMAMeasurementRecord("/var/run/resolv.conf", HASH_RECORD_2);
        Assert.assertFalse(containsMatch(ignoreSet, record, policyDisabledPartialPath));
    }

    /**
     * Assert that contains throws an IllegalArgumentException when a null measurement record is
     * passed to it.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void testContainsNullRecord() {
        final String name = "TestImaIgnoreSetBaseline";
        final ImaIgnoreSetBaseline baseline = new ImaIgnoreSetBaseline(name);
        addContainsRecords(baseline);
        containsMatch(baseline, null, policyEnabledPartialPath);
    }

    /**
     * Tests that a match is found when a measurement record with a mismatched
     * hash and a partial path is compared to a baseline measurement with a full
     * path while partial paths are enabled.
     */
    @Test
    public final void containsPartialMeasurementRecord() {
        final String name = "TestImaIgnoreSetBaseline";
        final ImaIgnoreSetBaseline ignoreSet = new ImaIgnoreSetBaseline(name);
        ignoreSet.addToBaseline(new ImaIgnoreSetRecord("/etc/resolv.conf", "description one"));
        final IMAMeasurementRecord record =
                getIMAMeasurementRecord("resolv.conf", HASH_RECORD_2);
        Assert.assertTrue(containsMatch(ignoreSet, record, policyEnabledPartialPath));
    }

    /**
     * Tests that a match is not found when a measurement record with a mismatched
     * hash and a partial path is compared to a baseline measurement with a full
     * path while partial paths are disabled.
     */
    @Test
    public final void containsPartialMeasurementRecordDisabled() {
        final String name = "TestImaIgnoreSetBaseline";
        final ImaIgnoreSetBaseline ignoreSet = new ImaIgnoreSetBaseline(name);
        ignoreSet.addToBaseline(new ImaIgnoreSetRecord("/etc/resolv.conf", "description one"));
        final IMAMeasurementRecord record =
                getIMAMeasurementRecord("resolv.conf", HASH_RECORD_2);
        Assert.assertFalse(containsMatch(ignoreSet, record, policyDisabledPartialPath));
    }

    /**
     * Tests that a match is found when a measurement record with a mismatched
     * hash and a full path is compared to a baseline measurement with a partial
     * path while partial paths are enabled.
     */
    @Test
    public final void containsPartialBaselineRecord() {
        final String name = "TestImaIgnoreSetBaseline";
        final ImaIgnoreSetBaseline ignoreSet = new ImaIgnoreSetBaseline(name);
        ignoreSet.addToBaseline(new ImaIgnoreSetRecord("resolv.conf", "description one"));
        final IMAMeasurementRecord record =
                getIMAMeasurementRecord("/etc/resolv.conf", HASH_RECORD_2);
        Assert.assertTrue(containsMatch(ignoreSet, record, policyEnabledPartialPath));
    }

    /**
     * Tests that a match is not found when a measurement record with a mismatched
     * hash and a full path is compared to a baseline measurement with a partial
     * path while partial paths are disabled.
     */
    @Test
    public final void containsPartialBaselineRecordDisabled() {
        final String name = "TestImaIgnoreSetBaseline";
        final ImaIgnoreSetBaseline ignoreSet = new ImaIgnoreSetBaseline(name);
        ignoreSet.addToBaseline(new ImaIgnoreSetRecord("resolv.conf", "description one"));
        final IMAMeasurementRecord record =
                getIMAMeasurementRecord("/etc/resolv.conf", HASH_RECORD_2);
        Assert.assertFalse(containsMatch(ignoreSet, record, policyDisabledPartialPath));
    }

    /**
     * Tests that a match is found when a measurement record with a mismatched
     * hash and a partial path is compared to a baseline measurement with the
     * same partial path while partial paths are enabled.
     */
    @Test
    public final void containsExactPartial() {
        final String name = "TestImaIgnoreSetBaseline";
        final ImaIgnoreSetBaseline ignoreSet = new ImaIgnoreSetBaseline(name);
        ignoreSet.addToBaseline(new ImaIgnoreSetRecord("resolv.conf", "description one"));
        final IMAMeasurementRecord record =
                getIMAMeasurementRecord("resolv.conf", HASH_RECORD_2);
        Assert.assertTrue(containsMatch(ignoreSet, record, policyEnabledPartialPath));
    }

    /**
     * Tests that a match is found when a measurement record with a mismatched
     * hash and a partial path is compared to a baseline measurement with the
     * same partial path while partial paths are disabled.
     */
    @Test
    public final void containsExactPartialDisabled() {
        final String name = "TestImaIgnoreSetBaseline";
        final ImaIgnoreSetBaseline ignoreSet = new ImaIgnoreSetBaseline(name);
        ignoreSet.addToBaseline(new ImaIgnoreSetRecord("resolv.conf", "description one"));
        final IMAMeasurementRecord record =
                getIMAMeasurementRecord("resolv.conf", HASH_RECORD_2);
        Assert.assertTrue(containsMatch(ignoreSet, record, policyDisabledPartialPath));
    }

    /**
     * Tests that neither partial path measurement records with full path
     * baseline records, nor partial path baseline records with full path
     * measurement records can be used when partial paths are disabled.
     */
    @Test
    public final void containsPartialDisabled() {
        final String name1 = "TestImaIgnoreSetBaseline1";
        final ImaIgnoreSetBaseline ignoreSet1 = new ImaIgnoreSetBaseline(name1);
        ignoreSet1.addToBaseline(new ImaIgnoreSetRecord("/etc/resolv.conf", "description one"));
        final IMAMeasurementRecord record1 =
                getIMAMeasurementRecord("resolv.conf", HASH_RECORD_2);

        Assert.assertFalse(containsMatch(ignoreSet1, record1, policyDisabledPartialPath));

        final String name2 = "TestImaIgnoreSetBaseline2";
        final ImaIgnoreSetBaseline ignoreSet2 = new ImaIgnoreSetBaseline(name2);
        ignoreSet2.addToBaseline(new ImaIgnoreSetRecord("resolv.conf", "description one"));
        final IMAMeasurementRecord record2 =
                getIMAMeasurementRecord("/etc/resolv.conf", HASH_RECORD_2);

        Assert.assertFalse(containsMatch(ignoreSet2, record2, policyDisabledPartialPath));
    }

    /**
     * Tests that removeRecord() removes an ima record from the baseline.
     */
    @Test
    public final void removeRecord() {
        boolean removed = false;
        ImaIgnoreSetBaseline imaIgnoreSetBaseline =
                new ImaIgnoreSetBaseline("TestImaIgnoreSetBaseline");
        ImaIgnoreSetRecord imaOptionalRecord =
                new ImaIgnoreSetRecord("/path/to/file", "description one");
        ImaIgnoreSetRecord imaOptionalRecord2 =
                new ImaIgnoreSetRecord("/path/to/file2", "description two");
        imaIgnoreSetBaseline.addToBaseline(imaOptionalRecord);
        imaIgnoreSetBaseline.addToBaseline(imaOptionalRecord2);
        Set<ImaIgnoreSetRecord> records = imaIgnoreSetBaseline.getImaIgnoreRecords();
        Assert.assertTrue(records.contains(imaOptionalRecord));
        Assert.assertTrue(records.contains(imaOptionalRecord2));
        Assert.assertEquals(records.size(), 2);
        removed = imaIgnoreSetBaseline.removeFromBaseline(imaOptionalRecord);
        Assert.assertTrue(removed);
        imaOptionalRecord.setOnlyBaseline(imaIgnoreSetBaseline);
        Assert.assertFalse(records.contains(imaOptionalRecord));
        Assert.assertTrue(records.contains(imaOptionalRecord2));
        Assert.assertEquals(records.size(), 1);
    }

    /**
     * Tests that removeRecord() handles attempt to remove record not found in
     * baseline.
     */
    @Test
    public final void removeRecordNotFound() {
        boolean removed = false;
        ImaIgnoreSetBaseline imaIgnoreSetBaseline =
                new ImaIgnoreSetBaseline("TestImaIgnoreSetBaseline");
        ImaIgnoreSetRecord imaOptionalRecord =
                new ImaIgnoreSetRecord("/path/to/file", "description one");
        ImaIgnoreSetRecord imaOptionalRecord2 =
                new ImaIgnoreSetRecord("/path/to/file2", "description two");
        imaIgnoreSetBaseline.addToBaseline(imaOptionalRecord);
        removed = imaIgnoreSetBaseline.removeFromBaseline(imaOptionalRecord2);
        Assert.assertFalse(removed);
    }

    /**
     * Tests that removeRecord() handles invalid ima record.
     */
    @Test
    public final void removeFromPolicyNullRecord() {
        ImaIgnoreSetBaseline imaIgnoreSetBaseline =
                new ImaIgnoreSetBaseline("TestImaIgnoreSetBaseline");
        ImaIgnoreSetRecord imaOptionalRecord =
                new ImaIgnoreSetRecord("/path/to/file", "description one");
        imaIgnoreSetBaseline.addToBaseline(imaOptionalRecord);
        imaOptionalRecord = null;
        Assert.assertFalse(imaIgnoreSetBaseline.removeFromBaseline(imaOptionalRecord));
    }

    /**
     * Tests that a <code>ImaIgnoreSetBaseline</code> can be saved using Hibernate.
     */
    @Test
    public final void testSaveImaIgnoreSetBaseline() {
            LOGGER.debug("save ImaIgnoreSetBaseline test started");
            Session session = sessionFactory.getCurrentSession();
            session.beginTransaction();
            final ImaIgnoreSetBaseline baseline = getDefaultBaseline();
            session.save(baseline);
            session.getTransaction().commit();
    }

    /**
     * Tests that a <code>ImaIgnoreSetBaseline</code> containing at least one
     * <code>ImaIgnoreSetRecord</code> can be saved using Hibernate.
     */
    @Test
    public final void testSaveImaIgnoreSetBaselineWithRecords() {
            LOGGER.debug("save ImaIgnoreSetBaseline test started");
            Session session = sessionFactory.getCurrentSession();
            session.beginTransaction();
            final ImaIgnoreSetBaseline baseline = getDefaultBaseline();
            ImaIgnoreSetRecord imaOptionalRecord =
                new ImaIgnoreSetRecord("/path/to/file", "description one");
            baseline.addToBaseline(imaOptionalRecord);
            session.save(baseline);
            session.getTransaction().commit();
    }

    /**
     * Tests that an <code>ImaIgnoreSetBaseline</code> can be saved and retrieved.
     * This saves a <code>ImaIgnoreSetBaseline</code> in the repo. Then a new
     * session is created, and the baseline is retrieved and its properties
     * verified.
     */
    @Test
    public final void testGetImaIgnoreSetBaseline() {
        LOGGER.debug("get ImaIgnoreSetBaseline test started");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final ImaIgnoreSetBaseline baseline = getDefaultBaseline();
        LOGGER.debug("saving ImaIgnoreSetBaseline");
        final UUID id = (UUID) session.save(baseline);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        LOGGER.debug("getting baseline");
        final ImaIgnoreSetBaseline testBaseline =
                (ImaIgnoreSetBaseline) session.get(ImaIgnoreSetBaseline.class, id);
        session.getTransaction().commit();

        LOGGER.debug("verifying baseline's properties");
        Assert.assertEquals(testBaseline.getName(), baseline.getName());
        Assert.assertEquals(testBaseline.getImaIgnoreRecords(),
                baseline.getImaIgnoreRecords());
        Assert.assertEquals(testBaseline.getImaIgnoreRecords().size(), 2);
    }

    /**
     * Tests that a baseline can be saved and then later updated. This saves the
     * baseline, retrieves it, adds a record to it, and then retrieves it and
     * verifies it.
     */
    @Test
    public final void testUpdateImaIgnoreSetBaseline() {
        LOGGER.debug("update ImaIgnoreSetBaseline test started");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final ImaIgnoreSetBaseline baseline = getDefaultBaseline();
        LOGGER.debug("saving ImaIgnoreSetBaseline");
        final UUID id = (UUID) session.save(baseline);
        session.getTransaction().commit();

        final ImaIgnoreSetRecord addedRecord =
                new ImaIgnoreSetRecord("/some/added/file", "description one");
        LOGGER.debug("updating baseline");
        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        ImaIgnoreSetBaseline testBaseline =
                (ImaIgnoreSetBaseline) session.get(ImaIgnoreSetBaseline.class, id);
        testBaseline.addToBaseline(addedRecord);
        session.update(testBaseline);
        session.getTransaction().commit();

        LOGGER.debug("getting baseline");
        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        testBaseline = (ImaIgnoreSetBaseline) session.get(ImaIgnoreSetBaseline.class,
                id);
        session.getTransaction().commit();

        final Set<ImaIgnoreSetRecord> expectedRecords =
                new HashSet<>(baseline.getImaIgnoreRecords());
        expectedRecords.add(addedRecord);
        Assert.assertEquals(testBaseline.getImaIgnoreRecords(), expectedRecords);
    }

    /**
     * Tests that a <code>ImaIgnoreSetBaseline</code> can be stored in the
     * repository and deleted.
     */
    @Test
    public final void testDeleteImaIgnoreSetBaseline() {
        LOGGER.debug("delete ImaIgnoreSetBaseline test started");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final ImaIgnoreSetBaseline baseline = getDefaultBaseline();
        LOGGER.debug("saving baseline");
        final UUID id = (UUID) session.save(baseline);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        LOGGER.debug("getting baseline");
        final ImaIgnoreSetBaseline p2 =
                (ImaIgnoreSetBaseline) session.get(ImaIgnoreSetBaseline.class, id);
        session.delete(p2);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        LOGGER.debug("getting ImaIgnoreSetBaseline again");
        final ImaIgnoreSetBaseline p3 =
                (ImaIgnoreSetBaseline) session.get(ImaIgnoreSetBaseline.class, id);
        session.getTransaction().commit();
        Assert.assertNull(p3);
    }

    /**
     * Tests that a <code>ImaIgnoreSetBaseline</code> can be stored in the
     * repository with records and that those records can be deleted.
     */
    @Test
    public final void testDeleteImaIgnoreSetRecordsFromBaseline() {
        LOGGER.debug("delete ImaIgnoreSetBaseline test started");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final ImaIgnoreSetBaseline baseline = getDefaultBaseline();
        final ImaIgnoreSetRecord record =
                new ImaIgnoreSetRecord("/some/added/file", "description one");
        baseline.addToBaseline(record);
        LOGGER.debug("saving baseline");
        final UUID id = (UUID) session.save(baseline);
        session.getTransaction().commit();
        Assert.assertEquals(baseline.getImaIgnoreRecords().size(), 3);

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        LOGGER.debug("getting baseline");
        final ImaIgnoreSetBaseline p2 =
                (ImaIgnoreSetBaseline) session.get(ImaIgnoreSetBaseline.class, id);
        p2.removeFromBaseline(record);
        session.update(p2);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        LOGGER.debug("getting ImaIgnoreSetBaseline again");
        final ImaIgnoreSetBaseline p3 =
                (ImaIgnoreSetBaseline) session.get(ImaIgnoreSetBaseline.class, id);
        session.getTransaction().commit();
        Assert.assertEquals(p3.getImaIgnoreRecords().size(), 2);
    }

    /**
     * Tests that an ImaIgnoreSetRecord can be created successfully from an IMAMeasurementRecord.
     */
    @Test
    public final void testImaIgnoreSetRecordFromString() {
        String reportRecordOne  = getIMAMeasurementRecord(PATH_RECORD_1, HASH_RECORD_2).toString();
        String reportRecordTwo = getIMAMeasurementRecord(PATH_RECORD_3, HASH_RECORD_2).toString();
        String description = "This is a test description";

        ImaIgnoreSetRecord ignoreRecord = ImaIgnoreSetRecord.fromString(reportRecordOne, null);
        Assert.assertEquals(ignoreRecord.getPath(), PATH_RECORD_1);

        ignoreRecord = ImaIgnoreSetRecord.fromString(reportRecordTwo, description);
        Assert.assertEquals(ignoreRecord.getPath(), PATH_RECORD_3);
        Assert.assertEquals(ignoreRecord.getDescription(), description);
    }

    private IMAMeasurementRecord getIMAMeasurementRecord(final String path,
            final String sha1) {
        try {
            final byte[] hash = Hex.decodeHex(sha1.toCharArray());
            final Digest digest = new Digest(DigestAlgorithm.SHA1, hash);
            return new IMAMeasurementRecord(path, digest);
        } catch (Exception e) {
            LOGGER.error("unexpected exception", e);
            throw new RuntimeException("unexpected exception", e);
        }
    }

    private ImaIgnoreSetBaseline getDefaultBaseline() {
        final ImaIgnoreSetBaseline baseline = new ImaIgnoreSetBaseline("Test Policy");
        addContainsRecords(baseline);
        return baseline;
    }

    private void addContainsRecords(final ImaIgnoreSetBaseline baseline) {
        baseline.addToBaseline(new ImaIgnoreSetRecord(PATH_RECORD_1, "description one"));
        baseline.addToBaseline(new ImaIgnoreSetRecord(PATH_RECORD_3, "description two"));
    }

    private boolean containsMatch(
            final ImaIgnoreSetBaseline baseline,
            final IMAMeasurementRecord record,
            final IMAPolicy imaPolicy
    ) {
        BatchImaMatchStatus status = baseline.contains(
                Collections.singletonList(record),
                recordManager,
                imaPolicy
        );
        return status.foundMatch(record);
    }

    private boolean containsUnknown(
            final ImaIgnoreSetBaseline baseline,
            final IMAMeasurementRecord record,
            final IMAPolicy imaPolicy
    ) {
        BatchImaMatchStatus status = baseline.contains(
                Collections.singletonList(record),
                recordManager,
                imaPolicy
        );
        return status.foundOnlyUnknown(record);
    }
}
