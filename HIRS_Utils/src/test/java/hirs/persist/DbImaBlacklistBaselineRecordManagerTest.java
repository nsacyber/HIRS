package hirs.persist;

import hirs.data.persist.baseline.Baseline;
import hirs.data.persist.Digest;
import hirs.data.persist.enums.DigestAlgorithm;
import hirs.data.persist.ImaBlacklistRecord;
import hirs.data.persist.baseline.ImaBlacklistBaseline;

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
 * <code>DbImaBlacklistBaselineRecordManagerTest</code> is a unit test class for the
 * <code>DbImaBlacklistBaselineRecordManager</code> class.
 */
public class DbImaBlacklistBaselineRecordManagerTest extends SpringPersistenceTest {

    private static final Logger LOGGER = LogManager.getLogger(DBBaselineManagerTest.class);
    private static final String BASELINE_NAME = "Test Baseline";
    private static final String BASELINE_NAME2 = "Test Baseline2";
    private static final String PATH = "/this/is/a/file";
    private static final String HASH = "41746eadf23c3bff9c16581c17a12da2ddd87e9b";
    private static final String HASH_TWO = "55d55d925115d3193e45320bea0b6ef5b2afc99a";

    private ImaBlacklistBaseline baseline;

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
        mgr.saveBaseline(new ImaBlacklistBaseline(BASELINE_NAME));
        baseline = (ImaBlacklistBaseline) mgr.getBaseline(BASELINE_NAME);
    }
    /**
     * Resets the test state to a known good state. This currently only resets the database by
     * removing all <code>Baseline</code> and <code>ImaBlacklistRecord</code> objects.
     */
    @AfterMethod
    public void resetTestState() {
        LOGGER.debug("reset test state");
        LOGGER.debug("deleting all baselines");
        DBManager dbManager = new DBManager<>(Baseline.class, sessionFactory);
        dbManager.deleteAll();
        dbManager = new DBManager<>(ImaBlacklistRecord.class, sessionFactory);
        dbManager.deleteAll();
        LOGGER.debug("all baselines removed");
    }

    /**
     * Tests that the <code>DbImaBlacklistBaselineRecordManager</code> can save a
     * <code>Baseline</code>.
     *
     * @throws ImaBaselineRecordManagerException
     *          if any unexpected errors occur
     */
    @Test
    public void testSave() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testSave test started");
        final ImaBlacklistRecord record = new ImaBlacklistRecord(PATH, createDigest(HASH), "",
                baseline);
        final ImaBlacklistBaselineRecordManager mgr =
                new DbImaBlacklistBaselineRecordManager(sessionFactory);
        final ImaBlacklistRecord r2 = mgr.saveRecord(record);
        BaselineManager baseMgr = new DBBaselineManager(sessionFactory);
        ImaBlacklistBaseline imaBaseline =
                (ImaBlacklistBaseline) baseMgr.getCompleteBaseline(baseline.getName());
        Assert.assertTrue(imaBaseline.getRecords().contains(r2));
        Assert.assertEquals(r2, record);
        Assert.assertNotNull(mgr.getRecord(r2.getId()));
    }

    /**
     * Tests that the <code>DbImaBlacklistBaselineRecordManager</code> can save a
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

        final ImaBlacklistRecord record = new ImaBlacklistRecord(longPath,
                createDigest(HASH), "", baseline);
        final ImaBlacklistBaselineRecordManager mgr =
                new DbImaBlacklistBaselineRecordManager(sessionFactory);
        final ImaBlacklistRecord r2 = mgr.saveRecord(record);
        BaselineManager baseMgr = new DBBaselineManager(sessionFactory);
        ImaBlacklistBaseline imaBaseline =
                (ImaBlacklistBaseline) baseMgr.getCompleteBaseline(baseline.getName());
        Assert.assertTrue(imaBaseline.getRecords().contains(r2));
        Assert.assertEquals(r2, record);
        Assert.assertNotNull(mgr.getRecord(r2.getId()));

        Assert.assertEquals(longPath, r2.getPath());
    }


    /**
     * Tests that the <code>DbImaBlacklistBaselineRecordManager</code> throws a
     * <code>ImaBaselineRecordManagerException</code> if a <code>ImaBaselineRecord</code> is saved
     * twice.
     *
     * @throws ImaBaselineRecordManagerException
     *          if any unexpected errors occur
     */
    @Test(expectedExceptions = ImaBaselineRecordManagerException.class)
    public void testSaveTwice() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testSaveTwice test started");
        final ImaBlacklistBaselineRecordManager mgr =
                new DbImaBlacklistBaselineRecordManager(sessionFactory);
        final ImaBlacklistRecord record = new ImaBlacklistRecord(PATH, createDigest(HASH));
        record.setBaseline(baseline);
        final ImaBlacklistRecord r2 = mgr.saveRecord(record);
        mgr.saveRecord(r2);
    }

    /**
     * Tests that the <code>DbImaBlacklistBaselineRecordManager</code> throws a
     * <code>ImaBaselineRecordManagerException</code> if a <code>ImaBlacklistRecord</code> is saved
     * with the same path, hash, and baseline as an existing <code>ImaBlacklistRecord</code>.
     *
     * @throws ImaBaselineRecordManagerException
     *          if any unexpected errors occur
     */
    @Test(expectedExceptions = ImaBaselineRecordManagerException.class)
    public void testSaveSameRecord() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testSaveSameRecord test started");
        final ImaBlacklistBaselineRecordManager mgr =
                new DbImaBlacklistBaselineRecordManager(sessionFactory);
        final ImaBlacklistRecord record =
                new ImaBlacklistRecord(PATH, createDigest(HASH), "", baseline);
        final ImaBlacklistRecord record2 =
                new ImaBlacklistRecord(PATH, createDigest(HASH), "", baseline);
        mgr.saveRecord(record);
        mgr.saveRecord(record2);
    }

    /**
     * Tests that the <code>DbImaBlacklistBaselineRecordManager</code> throws a
     * <code>ImaBaselineRecordManagerException</code> if the baseline parameter is null.
     *
     * @throws ImaBaselineRecordManagerException
     *          if any unexpected errors occur
     */
    @Test(expectedExceptions = ImaBaselineRecordManagerException.class)
    public void testSaveNullRecord() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testSaveNullRecord test started");
        final ImaBlacklistBaselineRecordManager mgr =
                new DbImaBlacklistBaselineRecordManager(sessionFactory);
        mgr.saveRecord(null);
    }

    /**
     * Tests that the <code>DbImaBlacklistBaselineRecordManager</code> can update a
     * <code>ImaBaselineRecord</code>.  In order to maintain control over updating. the records
     * aren't really updated, they are just deleted and saved again.  The ID will be different.
     *
     * @throws ImaBaselineRecordManagerException
     *          if any unexpected errors occur
     */
    @Test
    public void testDeleteAndSave() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testDeleteAndSave test started");
        final ImaBlacklistBaselineRecordManager mgr =
                new DbImaBlacklistBaselineRecordManager(sessionFactory);
        final ImaBlacklistRecord record =
                new ImaBlacklistRecord(PATH, createDigest(HASH), "", baseline);
        final ImaBlacklistRecord record2 = new ImaBlacklistRecord(PATH, createDigest(HASH_TWO), "",
                baseline);
        final ImaBlacklistRecord r2 = mgr.saveRecord(record);
        final ImaBlacklistRecord r3 = mgr.saveRecord(record2);
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
        final ImaBlacklistBaselineRecordManager mgr =
                new DbImaBlacklistBaselineRecordManager(sessionFactory);
        BaselineManager baselineMgr = new DBBaselineManager(sessionFactory);
        ImaBlacklistBaseline baselineTwo = (ImaBlacklistBaseline) baselineMgr.saveBaseline(
                new ImaBlacklistBaseline(BASELINE_NAME2));
        final ImaBlacklistRecord record = new ImaBlacklistRecord(PATH, createDigest(HASH), "",
                baseline);
        final ImaBlacklistRecord record2 = new ImaBlacklistRecord(PATH, createDigest(HASH_TWO), "",
                baselineTwo);
        final ImaBlacklistRecord r2 = mgr.saveRecord(record);
        final ImaBlacklistRecord r3 = mgr.saveRecord(record2);
        Assert.assertNotNull(mgr.getRecord(r2.getId()));
        Assert.assertNotNull(mgr.getRecord(r3.getId()));
    }

    /**
     * Tests that an <code>ImaBlacklistRecord</code> that is not an associated with a baseline,
     * will not be persisted.
     *
     * @throws ImaBaselineRecordManagerException
     *          if any unexpected errors occur
     */
    @Test(expectedExceptions = ImaBaselineRecordManagerException.class)
    public void testSaveRecordNullBaseline() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testSaveRecordNullBaseline test started");
        final ImaBlacklistBaselineRecordManager mgr =
                new DbImaBlacklistBaselineRecordManager(sessionFactory);
        final ImaBlacklistRecord record = new ImaBlacklistRecord(PATH, createDigest(HASH));
        final ImaBlacklistRecord r2 = mgr.saveRecord(record);
        mgr.saveRecord(r2);
        Assert.fail("second save did not fail");
    }

    /**
     * Tests that the <code>DbImaBlacklistBaselineRecordManager</code> can update an empty
     * ImaBaseline with new records.
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

        final ImaBlacklistBaselineRecordManager recordMgr =
                new DbImaBlacklistBaselineRecordManager(sessionFactory);
        recordMgr.saveRecord(new ImaBlacklistRecord(PATH, createDigest(HASH), "", baseline));

        ImaBlacklistBaseline retrievedBaseline =
                (ImaBlacklistBaseline) mgr.getCompleteBaseline(baseline.getName());
        Assert.assertEquals(retrievedBaseline.getRecords().size(), 1);
        ImaBlacklistRecord firstRecord = new ArrayList<>(retrievedBaseline.getRecords())
                .get(0);
        Assert.assertEquals(firstRecord.getPath(), PATH);
        Assert.assertEquals(firstRecord.getHash(), createDigest(HASH));
    }

    /**
     * Tests that the <code>DbImaBlacklistBaselineRecordManager</code> can get a
     * <code>ImaBlacklistRecord</code>.
     *
     * @throws ImaBaselineRecordManagerException
     *          if any unexpected errors occur
     */
    @Test
    public void testGetById() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testGetById test started");
        final ImaBlacklistBaselineRecordManager mgr =
                new DbImaBlacklistBaselineRecordManager(sessionFactory);
        final ImaBlacklistRecord record = new ImaBlacklistRecord(PATH, createDigest(HASH), "",
                baseline);
        Assert.assertNull(mgr.getRecord(PATH, createDigest(HASH), baseline));
        mgr.saveRecord(record);
        final ImaBlacklistRecord getRecord = mgr.getRecord(record.getId());
        Assert.assertNotNull(getRecord);
        Assert.assertEquals(getRecord, record);
        Assert.assertEquals(getRecord.getId(), record.getId());
    }

    /**
     * Tests that the <code>DbImaBlacklistBaselineRecordManager</code> can get a
     * <code>ImaBlacklistRecord</code>.
     *
     * @throws ImaBaselineRecordManagerException
     *          if any unexpected errors occur
     */
    @Test
    public void testGetByPathHashBaseline() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testGetByPathHashBaseline test started");
        final ImaBlacklistBaselineRecordManager mgr =
                new DbImaBlacklistBaselineRecordManager(sessionFactory);
        final ImaBlacklistRecord record = new ImaBlacklistRecord(PATH, createDigest(HASH), "",
                baseline);
        Assert.assertNull(mgr.getRecord(PATH, createDigest(HASH), baseline));
        mgr.saveRecord(record);
        final ImaBlacklistRecord getRecord = mgr.getRecord(PATH, createDigest(HASH),
                baseline);
        Assert.assertNotNull(getRecord);
        Assert.assertEquals(getRecord, record);
        Assert.assertEquals(getRecord.getId(), record.getId());
    }

    /**
     * Tests that the <code>DbImaBlacklistBaselineRecordManager</code> returns null when an
     * unknown <code>ImaBlacklistRecord</code> is searched for.
     *
     * @throws ImaBaselineRecordManagerException
     *             if any unexpected errors occur
     */
    @Test
    public void testGetUnknown() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testGetUnknown test started");
        final ImaBlacklistBaselineRecordManager mgr =
                new DbImaBlacklistBaselineRecordManager(sessionFactory);
        Assert.assertNull(mgr.getRecord(PATH, createDigest(HASH), baseline));
    }

    /**
     * Tests that the <code>DbImaBlacklistBaselineRecordManager</code> returns null when the values
     * are set to null.
     *
     * @throws ImaBaselineRecordManagerException
     *             if any unexpected errors occur
     */
    @Test
    public void testGetNull() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testGetNull test started");
        final ImaBlacklistBaselineRecordManager mgr =
                new DbImaBlacklistBaselineRecordManager(sessionFactory);
        Assert.assertNull(mgr.getRecord(null, null, null));
    }

    /**
     * Tests that the <code>DbImaBlacklistBaselineRecordManager</code> can delete a
     * <code>Baseline</code>.
     *
     * @throws ImaBaselineRecordManagerException if any unexpected errors occur
     */
    @Test
    public void testDelete() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testDelete test started");
        final ImaBlacklistBaselineRecordManager mgr =
                new DbImaBlacklistBaselineRecordManager(sessionFactory);
        final ImaBlacklistRecord record = new ImaBlacklistRecord(PATH, createDigest(HASH));
        record.setBaseline(baseline);
        ImaBlacklistRecord r2 = mgr.saveRecord(record);
        Assert.assertNotNull(mgr.getRecord(r2.getId()));
        boolean deleted = mgr.deleteRecord(r2);
        Assert.assertTrue(deleted);
        Assert.assertNull(mgr.getRecord(r2.getId()));
    }

    /**
     * Tests that the <code>DbImaBlacklistBaselineRecordManager</code> returns false when an
     * unknown <code>Baseline</code> name is provided.
     *
     * @throws ImaBaselineRecordManagerException
     *             if any unexpected errors occur
     */
    @Test
    public void testDeleteUnknown() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testDeleteUnknown test started");
        final ImaBlacklistBaselineRecordManager mgr =
                new DbImaBlacklistBaselineRecordManager(sessionFactory);
        boolean deleted = mgr.deleteRecord(new ImaBlacklistRecord(PATH, createDigest(HASH), "",
                baseline));
        Assert.assertTrue(deleted);
        deleted = mgr.deleteRecord(2L);
        Assert.assertFalse(deleted);
    }

    /**
     * Tests that the <code>DbImaBlacklistBaselineRecordManager</code> returns false when null is
     * provided for the name.
     *
     * @throws ImaBaselineRecordManagerException
     *             if any unexpected errors occur
     */
    @Test(expectedExceptions = ImaBaselineRecordManagerException.class)
    public void testDeleteNull() throws ImaBaselineRecordManagerException {
        LOGGER.debug("testDelete test started");
        final ImaBlacklistBaselineRecordManager mgr =
                new DbImaBlacklistBaselineRecordManager(sessionFactory);
        boolean deleted = mgr.deleteRecord((ImaBlacklistRecord) null);
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
