package hirs.persist;

import hirs.data.persist.baseline.Baseline;
import hirs.data.persist.Digest;
import hirs.data.persist.enums.DigestAlgorithm;
import hirs.data.persist.baseline.IMABaselineRecord;
import hirs.data.persist.baseline.SimpleImaBaseline;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;

import hirs.data.persist.SpringPersistenceTest;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * <code>DbImaBaselineRecordManagerTest</code> is a unit test class for the
 * <code>DbImaBaselineRecordManager</code> class.
 */
public class DbImaBaselineRecordManagerTest extends SpringPersistenceTest {

    private static final Logger LOGGER = LogManager.getLogger(DBBaselineManagerTest.class);
    private static final String BASELINE_NAME = "Test Baseline";
    private static final String BASELINE_NAME2 = "Test Baseline2";
    private static final String PATH = "/this/is/a/file";
    private static final String HASH = "41746eadf23c3bff9c16581c17a12da2ddd87e9b";
    private static final String HASH_TWO = "55d55d925115d3193e45320bea0b6ef5b2afc99a";

    private SimpleImaBaseline baseline;

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an
     * in-memory database that is used for testing.
     */
    @BeforeClass
    public void setup() {
        LOGGER.debug("retrieving session factory");
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterClass
    public void tearDown() {
        LOGGER.debug("closing session factory");
    }

    /**
     *
     */
    @BeforeMethod
    public void setupMethod() {
        BaselineManager mgr = new DBBaselineManager(sessionFactory);
        mgr.saveBaseline(new SimpleImaBaseline(BASELINE_NAME));
        baseline = (SimpleImaBaseline) mgr.getBaseline(BASELINE_NAME);
    }
    /**
     * Resets the test state to a known good state. This currently only resets the database by
     * removing all <code>Baseline</code> and <code>IMABaselineRecord</code> objects.
     */
    @AfterMethod
    public void resetTestState() {
        LOGGER.debug("reset test state");
        LOGGER.debug("deleting all baselines");
        DBManager dbManager = new DBManager<>(Baseline.class, sessionFactory);
        dbManager.deleteAll();
        dbManager = new DBManager<>(IMABaselineRecord.class, sessionFactory);
        dbManager.deleteAll();
        LOGGER.debug("all baselines removed");
    }

    /**
     * Tests that the <code>DbImaBaselineRecordManager</code> can save a
     * <code>Baseline</code>.
     *
     * @throws ImaBaselineRecordManagerException
     *          if any unexpected errors occur
     */
    @Test
    public void testSave() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testSave test started");
        final IMABaselineRecord record = new IMABaselineRecord(PATH, createDigest(HASH), baseline);
        final ImaBaselineRecordManager mgr = new DbImaBaselineRecordManager(sessionFactory);
        final IMABaselineRecord r2 = mgr.saveRecord(record);
        BaselineManager baseMgr = new DBBaselineManager(sessionFactory);
        SimpleImaBaseline imaBaseline =
                (SimpleImaBaseline) baseMgr.getCompleteBaseline(baseline.getName());
        Assert.assertTrue(imaBaseline.getBaselineRecords().contains(r2));
        Assert.assertEquals(r2, record);
        Assert.assertNotNull(mgr.getRecord(r2.getId()));
    }

    /**
     * Tests that the <code>DbImaBaselineRecordManager</code> can save a
     * <code>Baseline</code> with a long path (> 255 char).
     *
     * @throws ImaBaselineRecordManagerException
     *          if any unexpected errors occur
     */
    @Test
    public void testSaveLongPath() throws ImaBaselineRecordManagerException {
        SecureRandom random = new SecureRandom();

        // 1000 char length string (2^5=32)
        String longPath = new BigInteger(5000, random).toString(32);

        final IMABaselineRecord record = new IMABaselineRecord(longPath,
                createDigest(HASH), baseline);
        final ImaBaselineRecordManager mgr = new DbImaBaselineRecordManager(sessionFactory);
        final IMABaselineRecord r2 = mgr.saveRecord(record);
        BaselineManager baseMgr = new DBBaselineManager(sessionFactory);
        SimpleImaBaseline imaBaseline =
                (SimpleImaBaseline) baseMgr.getCompleteBaseline(baseline.getName());
        Assert.assertTrue(imaBaseline.getBaselineRecords().contains(r2));
        Assert.assertEquals(r2, record);
        Assert.assertNotNull(mgr.getRecord(r2.getId()));

        Assert.assertEquals(longPath, r2.getPath());
    }


    /**
     * Tests that the <code>DbImaBaselineRecordManager</code> throws a
     * <code>ImaBaselineRecordManagerException</code> if a <code>ImaBaselineRecord</code> is saved
     * twice.
     *
     * @throws ImaBaselineRecordManagerException
     *          if any unexpected errors occur
     */
    @Test(expectedExceptions = ImaBaselineRecordManagerException.class)
    public void testSaveTwice() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testSaveTwice test started");
        final ImaBaselineRecordManager mgr = new DbImaBaselineRecordManager(sessionFactory);
        final IMABaselineRecord record = new IMABaselineRecord(PATH, createDigest(HASH));
        record.setBaselineForRecordManager(baseline);
        final IMABaselineRecord r2 = mgr.saveRecord(record);
        mgr.saveRecord(r2);
    }

    /**
     * Tests that the <code>DbImaBaselineRecordManager</code> throws a
     * <code>ImaBaselineRecordManagerException</code> if a <code>IMABaselineRecord</code> is saved
     * with the same path, hash, and baseline as an existing <code>IMABaselineRecord</code>.
     *
     * @throws ImaBaselineRecordManagerException
     *          if any unexpected errors occur
     */
    @Test(expectedExceptions = ImaBaselineRecordManagerException.class)
    public void testSaveSameRecord() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testSaveSameRecord test started");
        final ImaBaselineRecordManager mgr = new DbImaBaselineRecordManager(sessionFactory);
        final IMABaselineRecord record = new IMABaselineRecord(PATH, createDigest(HASH), baseline);
        final IMABaselineRecord record2 = new IMABaselineRecord(PATH, createDigest(HASH), baseline);
        mgr.saveRecord(record);
        mgr.saveRecord(record2);
    }

    /**
     * Tests that the <code>DbImaBaselineRecordManager</code> throws a
     * <code>ImaBaselineRecordManagerException</code> if the baseline parameter is null.
     *
     * @throws ImaBaselineRecordManagerException
     *          if any unexpected errors occur
     */
    @Test(expectedExceptions = ImaBaselineRecordManagerException.class)
    public void testSaveNullRecord() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testSaveNullRecord test started");
        final ImaBaselineRecordManager mgr = new DbImaBaselineRecordManager(sessionFactory);
        mgr.saveRecord(null);
    }

    /**
     * Tests that the <code>DbImaBaselineRecordManager</code> can update a
     * <code>ImaBaselineRecord</code>.  In order to maintain control over updating. the records
     * aren't really updated, they are just deleted and saved again.  The ID will be different.
     *
     * @throws ImaBaselineRecordManagerException
     *          if any unexpected errors occur
     */
    @Test
    public void testDeleteAndSave() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testDeleteAndSave test started");
        final ImaBaselineRecordManager mgr = new DbImaBaselineRecordManager(sessionFactory);
        final IMABaselineRecord record = new IMABaselineRecord(PATH, createDigest(HASH), baseline);
        final IMABaselineRecord record2 = new IMABaselineRecord(PATH, createDigest(HASH_TWO),
                baseline);
        final IMABaselineRecord r2 = mgr.saveRecord(record);
        final IMABaselineRecord r3 = mgr.saveRecord(record2);
        if (!mgr.deleteRecord(r2)) {
            Assert.fail("record was not deleted during update test.");
        }
        Assert.assertNull(mgr.getRecord(r2.getId()));
        Assert.assertNotNull(mgr.getRecord(r3.getId()));
    }

    /**
     * Tests that <code>IMABaselineRecords</code>s with the same path and hash but different
     * baselines will still be persisted appropriately.
     *
     * @throws ImaBaselineRecordManagerException
     *          if any unexpected errors occur
     */
    @Test
    public void testSaveDifferentBaselines() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testSaveDifferentBaselines test started");
        final ImaBaselineRecordManager mgr = new DbImaBaselineRecordManager(sessionFactory);
        BaselineManager baselineMgr = new DBBaselineManager(sessionFactory);
        SimpleImaBaseline baselineTwo = (SimpleImaBaseline) baselineMgr.saveBaseline(
                new SimpleImaBaseline(BASELINE_NAME2));
        final IMABaselineRecord record = new IMABaselineRecord(PATH, createDigest(HASH),
                baseline);
        final IMABaselineRecord record2 = new IMABaselineRecord(PATH, createDigest(HASH_TWO),
                baselineTwo);
        final IMABaselineRecord r2 = mgr.saveRecord(record);
        final IMABaselineRecord r3 = mgr.saveRecord(record2);
        Assert.assertNotNull(mgr.getRecord(r2.getId()));
        Assert.assertNotNull(mgr.getRecord(r3.getId()));
    }

    /**
     * Tests that an <code>IMABaselineRecord</code> that is not an associated with a baseline,
     * will not be persisted.
     *
     * @throws ImaBaselineRecordManagerException
     *          if any unexpected errors occur
     */
    @Test(expectedExceptions = ImaBaselineRecordManagerException.class)
    public void testSaveRecordNullBaseline() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testSaveRecordNullBaseline test started");
        final ImaBaselineRecordManager mgr = new DbImaBaselineRecordManager(sessionFactory);
        final IMABaselineRecord record = new IMABaselineRecord(PATH, createDigest(HASH));
        final IMABaselineRecord r2 = mgr.saveRecord(record);
        mgr.saveRecord(r2);
        Assert.fail("second save did not fail");
    }

    /**
     * Tests that the <code>DbImaBaselineRecordManager</code> can update an empty ImaBaseline with
     * new records.
     *
     * @throws ImaBaselineRecordManagerException
     *          if any unexpected errors occur
     * @throws UnsupportedEncodingException
     *          if the platform does not support UTF-8 encoding
     */
    @Test
    public void testAddingRecordsWillAddToImaBaseline() throws ImaBaselineRecordManagerException,
            UnsupportedEncodingException {
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);

        final ImaBaselineRecordManager recordMgr = new DbImaBaselineRecordManager(sessionFactory);
        recordMgr.saveRecord(new IMABaselineRecord(PATH, createDigest(HASH), baseline));

        SimpleImaBaseline retrievedBaseline =
                (SimpleImaBaseline) mgr.getCompleteBaseline(baseline.getName());
        Assert.assertEquals(retrievedBaseline.getBaselineRecords().size(), 1);
        IMABaselineRecord firstRecord = new ArrayList<>(retrievedBaseline.getBaselineRecords())
                .get(0);
        Assert.assertEquals(firstRecord.getPath(), PATH);
        Assert.assertEquals(firstRecord.getHash(), createDigest(HASH));
    }

    /**
     * Tests that the <code>DbImaBaselineRecordManager</code> can get a
     * <code>IMABaselineRecord</code>.
     *
     * @throws ImaBaselineRecordManagerException
     *          if any unexpected errors occur
     */
    @Test
    public void testGetById() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testGetById test started");
        final ImaBaselineRecordManager mgr = new DbImaBaselineRecordManager(sessionFactory);
        final IMABaselineRecord record = new IMABaselineRecord(PATH, createDigest(HASH), baseline);
        Assert.assertNull(mgr.getRecord(PATH, createDigest(HASH), baseline));
        mgr.saveRecord(record);
        final IMABaselineRecord getRecord = mgr.getRecord(record.getId());
        Assert.assertNotNull(getRecord);
        Assert.assertEquals(getRecord, record);
        Assert.assertEquals(getRecord.getId(), record.getId());
    }

    /**
     * Tests that the <code>DbImaBaselineRecordManager</code> can get a
     * <code>IMABaselineRecord</code>.
     *
     * @throws ImaBaselineRecordManagerException
     *          if any unexpected errors occur
     */
    @Test
    public void testGetByPathHashBaseline() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testGetByPathHashBaseline test started");
        final ImaBaselineRecordManager mgr = new DbImaBaselineRecordManager(sessionFactory);
        final IMABaselineRecord record = new IMABaselineRecord(PATH, createDigest(HASH), baseline);
        Assert.assertNull(mgr.getRecord(PATH, createDigest(HASH), baseline));
        mgr.saveRecord(record);
        final IMABaselineRecord getRecord = mgr.getRecord(PATH, createDigest(HASH),
                baseline);
        Assert.assertNotNull(getRecord);
        Assert.assertEquals(getRecord, record);
        Assert.assertEquals(getRecord.getId(), record.getId());
    }

    /**
     * Tests that the <code>DbImaBaselineRecordManager</code> returns null when an
     * unknown <code>IMABaselineRecord</code> is searched for.
     *
     * @throws ImaBaselineRecordManagerException
     *             if any unexpected errors occur
     */
    @Test
    public void testGetUnknown() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testGetUnknown test started");
        final ImaBaselineRecordManager mgr = new DbImaBaselineRecordManager(sessionFactory);
        Assert.assertNull(mgr.getRecord(PATH, createDigest(HASH), baseline));
    }

    /**
     * Tests that the <code>DbImaBaselineRecordManager</code> returns null when the values are set
     * to null.
     *
     * @throws ImaBaselineRecordManagerException
     *             if any unexpected errors occur
     */
    @Test
    public void testGetNull() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testGetNull test started");
        final ImaBaselineRecordManager mgr = new DbImaBaselineRecordManager(sessionFactory);
        Assert.assertNull(mgr.getRecord(null, null, null));
    }

    /**
     * Tests that the <code>DbImaBaselineRecordManager</code> can delete a
     * <code>Baseline</code>.
     *
     * @throws ImaBaselineRecordManagerException if any unexpected errors occur
     */
    @Test
    public void testDelete() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testDelete test started");
        final ImaBaselineRecordManager mgr = new DbImaBaselineRecordManager(sessionFactory);
        final IMABaselineRecord record = new IMABaselineRecord(PATH, createDigest(HASH));
        record.setBaselineForRecordManager(baseline);
        IMABaselineRecord r2 = mgr.saveRecord(record);
        Assert.assertNotNull(mgr.getRecord(r2.getId()));
        boolean deleted = mgr.deleteRecord(r2);
        Assert.assertTrue(deleted);
        Assert.assertNull(mgr.getRecord(r2.getId()));
    }

    /**
     * Tests that the <code>DbImaBaselineRecordManager</code> returns false when an
     * unknown <code>Baseline</code> name is provided.
     *
     * @throws ImaBaselineRecordManagerException
     *             if any unexpected errors occur
     */
    @Test
    public void testDeleteUnknown() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testDeleteUnknown test started");
        final ImaBaselineRecordManager mgr = new DbImaBaselineRecordManager(sessionFactory);
        boolean deleted = mgr.deleteRecord(new IMABaselineRecord(PATH, createDigest(HASH),
                baseline));
        Assert.assertTrue(deleted);
        deleted = mgr.deleteRecord(2L);
        Assert.assertFalse(deleted);
    }

    /**
     * Tests that the <code>DbImaBaselineRecordManager</code> returns false when null is
     * provided for the name.
     *
     * @throws ImaBaselineRecordManagerException
     *             if any unexpected errors occur
     */
    @Test(expectedExceptions = ImaBaselineRecordManagerException.class)
    public void testDeleteNull() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testDelete test started");
        final ImaBaselineRecordManager mgr = new DbImaBaselineRecordManager(sessionFactory);
        boolean deleted = mgr.deleteRecord((IMABaselineRecord) null);
        Assert.assertFalse(deleted);
    }

    private Digest createDigest(final String hash) {
        try {
            final byte[] digestBytes = Hex.decodeHex(hash.toCharArray());
            return new Digest(DigestAlgorithm.SHA1, digestBytes);
        } catch (DecoderException de) {
            LOGGER.debug(de);
            throw new RuntimeException(de);
        }
    }
}
