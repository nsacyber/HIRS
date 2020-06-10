package hirs.data.persist;

import hirs.data.persist.baseline.ImaBlacklistBaseline;
import hirs.persist.DBManager;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests {@link ImaBlacklistRecord} functions as expected.
 */
public class ImaBlacklistRecordTest extends SpringPersistenceTest {
    /**
     * A sample path.
     */
    public static final String PATH = "/tmp/test";

    /**
     * A sample partial path.
     */
    public static final String PARTIAL_PATH = "test";

    /**
     * A sample hash.
     */
    public static final Digest HASH = DigestTest.getTestSHA1Digest();

    /**
     * A sample description.
     */
    public static final String DESC = "dangerous file";

    /**
     * Tests that a new ImaBlacklistRecord can be constructed with the expected variety of
     * parameters.
     */
    @Test
    public void testConstructors() {
        new ImaBlacklistRecord(PATH);
        new ImaBlacklistRecord(PATH, DESC);
        new ImaBlacklistRecord(HASH);
        new ImaBlacklistRecord(HASH, DESC);
        new ImaBlacklistRecord(PATH, HASH);
        new ImaBlacklistRecord(PATH, HASH, DESC);
    }

    /**
     * Tests that constructing an ImaBlacklistRecord object with a null path parameter results
     * in the class throwing an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullPath() {
        new ImaBlacklistRecord((String) null);
    }

    /**
     * Tests that constructing an ImaBlacklistRecord object with an empty path parameter results
     * in the class throwing an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testEmptyPath() {
        new ImaBlacklistRecord("");
    }

    /**
     * Tests that constructing an ImaBlacklistRecord object with a blank path parameter results
     * in the class throwing an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBlankPath() {
        new ImaBlacklistRecord("   ");
    }

    /**
     * Tests that constructing an ImaBlacklistRecord object with a null path parameter and a
     * description results in the class throwing an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullPathWithDesc() {
        new ImaBlacklistRecord((String) null, DESC);
    }

    /**
     * Tests that constructing an ImaBlacklistRecord object with a null path parameter and a
     * null description results in the class throwing an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullPathWithNullDesc() {
        new ImaBlacklistRecord((String) null, (String) null);
    }

    /**
     * Tests that constructing an ImaBlacklistRecord object with a null hash parameter results in
     * the class throwing an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullHash() {
        new ImaBlacklistRecord((Digest) null);
    }

    /**
     * Tests that constructing an ImaBlacklistRecord object with a null hash parameter and a
     * description results in the class throwing an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullHashWithDesc() {
        new ImaBlacklistRecord((Digest) null, DESC);
    }

    /**
     * Tests that constructing an ImaBlacklistRecord object with a null hash parameter and a
     * null description results in the class throwing an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullHashWithNullDesc() {
        new ImaBlacklistRecord((Digest) null, (String) null);
    }

    /**
     * Tests that constructing an ImaBlacklistRecord object with a null path and a null hash
     * parameter results in the class throwing an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullPathAndNullHash() {
        new ImaBlacklistRecord((String) null, (Digest) null);
    }

    /**
     * Tests that constructing an ImaBlacklistRecord object with a null path and a null hash
     * parameter, with a description, results in the class throwing an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullPathAndNullHashWithDesc() {
        new ImaBlacklistRecord((String) null, (Digest) null, DESC);
    }

    /**
     * Tests that constructing an ImaBlacklistRecord object with a null path and a null hash
     * parameter, with a null description, results in the class throwing an
     * IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullPathAndNullHashWithNullDesc() {
        new ImaBlacklistRecord((String) null, (Digest) null, (String) null);
    }

    /**
     * Tests that ImaBlacklistRecord's getters function as expected given just a path.
     */
    @Test
    public void testConstructAndGetOnlyPath() {
        ImaBlacklistRecord record = new ImaBlacklistRecord(PATH);
        Assert.assertEquals(record.getPath(), PATH);
        Assert.assertEquals(record.getPartialPath(), PARTIAL_PATH);
        Assert.assertNull(record.getHash());
        Assert.assertNull(record.getDescription());
    }

    /**
     * Tests that ImaBlacklistRecord's getters function as expected given a path and a
     * description.
     */
    @Test
    public void testConstructAndGetPathAndDesc() {
        ImaBlacklistRecord record = new ImaBlacklistRecord(PATH, DESC);
        Assert.assertEquals(record.getPath(), PATH);
        Assert.assertEquals(record.getPartialPath(), PARTIAL_PATH);
        Assert.assertNull(record.getHash());
        Assert.assertEquals(record.getDescription(), DESC);
    }

    /**
     * Tests that ImaBlacklistRecord's getters function as expected given just a hash.
     */
    @Test
    public void testConstructAndGetOnlyHash() {
        ImaBlacklistRecord record = new ImaBlacklistRecord(HASH);
        Assert.assertNull(record.getPath());
        Assert.assertNull(record.getPartialPath());
        Assert.assertEquals(record.getHash(), HASH);
        Assert.assertNull(record.getDescription());
    }

    /**
     * Tests that ImaBlacklistRecord's getters function as expected given a hash and a
     * description.
     */
    @Test
    public void testConstructAndGetHashAndDesc() {
        ImaBlacklistRecord record = new ImaBlacklistRecord(HASH, DESC);
        Assert.assertNull(record.getPath());
        Assert.assertNull(record.getPartialPath());
        Assert.assertEquals(record.getHash(), HASH);
        Assert.assertEquals(record.getDescription(), DESC);
    }

    /**
     * Tests that ImaBlacklistRecord's getters function as expected given a path and a hash.
     */
    @Test
    public void testConstructAndGetPathAndHash() {
        ImaBlacklistRecord record = new ImaBlacklistRecord(PATH, HASH);
        Assert.assertEquals(record.getPath(), PATH);
        Assert.assertEquals(record.getPartialPath(), PARTIAL_PATH);
        Assert.assertEquals(record.getHash(), HASH);
        Assert.assertNull(record.getDescription());
    }

    /**
     * Tests that ImaBlacklistRecord's getters function as expected given a path, hash,
     * and description.
     */
    @Test
    public void testConstructAndGetPathAndHashAndDesc() {
        ImaBlacklistRecord record = new ImaBlacklistRecord(PATH, HASH, DESC);
        Assert.assertEquals(record.getPath(), PATH);
        Assert.assertEquals(record.getPartialPath(), PARTIAL_PATH);
        Assert.assertEquals(record.getHash(), HASH);
        Assert.assertEquals(record.getDescription(), DESC);
    }

    /**
     * Test that a blacklist baseline can be associated with a record.
     */
    @Test
    public void testGetSetBlacklistBaseline() {
        ImaBlacklistRecord record = new ImaBlacklistRecord(PATH, HASH, DESC);
        Assert.assertNull(record.getBaseline());
        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline("Test");
        record.setBaseline(baseline);
        Assert.assertEquals(record.getBaseline(), baseline);
    }

    /**
     * Tests that ImaBlacklistRecord's equals method correctly compares instance of the class.
     */
    @Test
    public void testEquals() {
        ImaBlacklistRecord pathRec = new ImaBlacklistRecord(PATH);
        ImaBlacklistRecord pathDescRec = new ImaBlacklistRecord(PATH, DESC);
        ImaBlacklistRecord hashRec = new ImaBlacklistRecord(HASH);
        ImaBlacklistRecord hashDescRec = new ImaBlacklistRecord(HASH, DESC);
        ImaBlacklistRecord pathHashRec = new ImaBlacklistRecord(PATH, HASH);
        ImaBlacklistRecord pathHashDescRec = new ImaBlacklistRecord(PATH, HASH, DESC);

        Assert.assertEquals(pathRec, pathRec);
        Assert.assertEquals(pathDescRec, pathDescRec);
        Assert.assertEquals(pathRec, pathDescRec);

        Assert.assertEquals(hashRec, hashRec);
        Assert.assertEquals(hashDescRec, hashDescRec);
        Assert.assertEquals(hashRec, hashDescRec);

        Assert.assertEquals(pathHashRec, pathHashRec);
        Assert.assertEquals(pathHashDescRec, pathHashDescRec);
        Assert.assertEquals(pathHashRec, pathHashDescRec);

        Assert.assertNotEquals(pathRec, hashRec);
        Assert.assertNotEquals(pathRec, hashDescRec);
        Assert.assertNotEquals(pathRec, pathHashRec);
        Assert.assertNotEquals(pathRec, pathHashDescRec);

        Assert.assertNotEquals(hashRec, pathRec);
        Assert.assertNotEquals(hashRec, pathDescRec);
        Assert.assertNotEquals(hashRec, pathHashRec);
        Assert.assertNotEquals(hashRec, pathHashDescRec);

        Assert.assertNotEquals(pathHashRec, pathRec);
        Assert.assertNotEquals(pathHashRec, pathDescRec);
        Assert.assertNotEquals(pathHashRec, hashRec);
        Assert.assertNotEquals(pathHashRec, hashDescRec);

        ImaBlacklistRecord otherPathRec = new ImaBlacklistRecord("/tmp/other");
        Assert.assertNotEquals(pathRec, otherPathRec);

        ImaBlacklistRecord otherHashRec = new ImaBlacklistRecord(
                DigestTest.getTestSHA1Digest((byte) 2)
        );
        Assert.assertNotEquals(hashRec, otherHashRec);

        ImaBlacklistRecord otherDescPathRec = new ImaBlacklistRecord(PATH, "Other description");
        Assert.assertEquals(pathRec, otherDescPathRec);
    }

    /**
     * Tests that ImaBlacklistRecord's equals method correctly hashes instance of the class.
     */
    @Test
    public void testHashCode() {
        ImaBlacklistRecord pathRec = new ImaBlacklistRecord(PATH);
        ImaBlacklistRecord hashRec = new ImaBlacklistRecord(HASH);
        ImaBlacklistRecord pathHashRec = new ImaBlacklistRecord(PATH, HASH);

        assertHashCodeEquals(pathRec, pathRec);
        assertHashCodeEquals(hashRec, hashRec);
        assertHashCodeEquals(pathHashRec, pathHashRec);
    }

    private static void assertHashCodeEquals(final Object obj1, final Object obj2) {
        Assert.assertEquals(obj1.hashCode(), obj2.hashCode());
    }

    /**
     * Simple tests to ensure ImaBlacklistRecords can be persisted and retrieved.
     */
    @Test
    public void testPersistBlacklistRecords() {
        DBManager<ImaBlacklistRecord> blacklistRecordMan = new DBManager<>(
                ImaBlacklistRecord.class, sessionFactory
        );

        ImaBlacklistRecord pathRec = new ImaBlacklistRecord(PATH);
        ImaBlacklistRecord pathDescRec = new ImaBlacklistRecord(PATH, DESC);
        ImaBlacklistRecord hashRec = new ImaBlacklistRecord(HASH);
        ImaBlacklistRecord hashDescRec = new ImaBlacklistRecord(HASH, DESC);
        ImaBlacklistRecord pathHashRec = new ImaBlacklistRecord(PATH, HASH);
        ImaBlacklistRecord pathHashDescRec = new ImaBlacklistRecord(PATH, HASH, DESC);

        testPersistence(blacklistRecordMan, pathRec);
        testPersistence(blacklistRecordMan, pathDescRec);
        testPersistence(blacklistRecordMan, hashRec);
        testPersistence(blacklistRecordMan, hashDescRec);
        testPersistence(blacklistRecordMan, pathHashRec);
        testPersistence(blacklistRecordMan, pathHashDescRec);
    }

    private void testPersistence(
            final DBManager<ImaBlacklistRecord> blacklistRecordMan,
            final ImaBlacklistRecord record
    ) {
        ImaBlacklistRecord savedRecord = blacklistRecordMan.save(record);

        Assert.assertEquals(savedRecord, record);
        Assert.assertEquals(savedRecord.getPath(), record.getPath());
        Assert.assertEquals(savedRecord.getHash(), record.getHash());
        Assert.assertEquals(savedRecord.getDescription(), record.getDescription());

        Assert.assertTrue(blacklistRecordMan.delete(savedRecord));
    }

    /**
     * Retrieve an {@link ImaBlacklistRecord} for test purposes.
     *
     * @return a test ImaBlacklistRecord
     */
    public static ImaBlacklistRecord getTestBlacklistRecord() {
        return new ImaBlacklistRecord(PATH, HASH, DESC);
    }
}
