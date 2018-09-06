package hirs.data.persist;

import java.text.ParseException;
import java.util.Set;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * ImaBaselineRecordTest is a unit test class for the IMABaselineRecord class.
 */
public class ImaBaselineRecordTest {

    private static final Logger LOGGER
            = LogManager.getLogger(IMAMeasurementRecordTest.class);
    private static final String PATH = "IMATestRecord";
    private static final String HASH =
            "3d5f3c2f7f3003d2e4baddc46ed4763a4954f648";

    /**
     * Tests instantiation of IMABaselineRecord object.
     *
     * @throws ParseException
     *             if error generated parsing date
     */
    @Test
    public final void imaBaselineRecord() throws ParseException {
        final String path = "IMATestRecord";
        final String hash = "3d5f3c2f7f3003d2e4baddc46ed4763a4954f648";
        new IMABaselineRecord(path, getDigest(hash));
    }

    /**
     * Tests that IMABaselineRecord constructor throws a NullPointerException
     * with null path.
     *
     * @throws ParseException
     *             if error generated parsing date
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void imaBaselineRecordNullPath() throws ParseException {
        final String path = null;
        final String hash = "3d5f3c2f7f3003d2e4baddc46ed4763a4954f648";
        new IMABaselineRecord(path, getDigest(hash));
    }

    /**
     * Tests that IMABaselineRecord constructor throws a NullPointerException
     * with null hash.
     *
     * @throws ParseException
     *             if error generated parsing date
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void imaBaselineRecordNullHash() throws ParseException {
        final String path = "IMATestRecord";
        new IMABaselineRecord(path, null);
    }

    /**
     * Tests that getHash() returns the file hash.
     *
     * @throws ParseException
     *             if error generated parsing date fields
     */
    @Test
    public final void getHash() throws ParseException {
        final IMABaselineRecord record = getBaselineRecord();
        final Digest hash = record.getHash();
        Assert.assertNotNull(hash);
        Assert.assertEquals(hash, getDigest(HASH));
    }

    /**
     * Tests that constructing an IMABaselineRecord object with an empty path parameter results
     * in the class throwing an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testEmptyPath() {
        new IMABaselineRecord("", getDigest(HASH));
    }

    /**
     * Tests that constructing an ImaBaselineRecord object with a blank path parameter results
     * in the class throwing an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBlankPath() {
        new IMABaselineRecord("   ", getDigest(HASH));
    }

    /**
     * Tests that the initial baseline set for a record is null.
     *
     * @throws ParseException
     *             if error generated parsing date fields
     */
    @Test
    public final void getDefaultBaselineIsNull() throws ParseException {
        final IMABaselineRecord record = getBaselineRecord();
        Assert.assertNull(record.getBaseline());
    }

    /**
     * Tests that a record that is not a part of a baseline is added to the
     * baselines set of records when
     * {@link IMABaselineRecord#setBaseline(ImaBaseline)}
     * is called.
     *
     * @throws ParseException
     *             if error generated parsing date fields
     */
    @Test
    public final void setBaseline() throws ParseException {
        final IMABaselineRecord record = getBaselineRecord();
        Assert.assertNull(record.getBaseline());
        final SimpleImaBaseline baseline = new SimpleImaBaseline("TestBaseline");
        Assert.assertTrue(baseline.getBaselineRecords().isEmpty());

        record.setBaseline(baseline);
        final Set<IMABaselineRecord> imaRecords = baseline.getBaselineRecords();
        Assert.assertTrue(imaRecords.contains(record));
        Assert.assertEquals(imaRecords.size(), 1);
        Assert.assertTrue(record.getBaseline() == baseline);
    }

    /**
     * Tests that when a record is already associated with a baseline and then a
     * new baseline is set for the record that the record is removed from the
     * old baseline and added to the new baseline.
     *
     * @throws ParseException
     *          if error generated parsing date fields
     */
    @Test
    public final void setNewBaseline() throws ParseException {
        final IMABaselineRecord record = getBaselineRecord();
        Assert.assertNull(record.getBaseline());
        final SimpleImaBaseline oldBaseline = new SimpleImaBaseline("OldBaseline");
        Assert.assertTrue(oldBaseline.getBaselineRecords().isEmpty());
        final SimpleImaBaseline newBaseline = new SimpleImaBaseline("NewBaseline");
        Assert.assertTrue(newBaseline.getBaselineRecords().isEmpty());

        record.setBaseline(oldBaseline);
        Set<IMABaselineRecord> imaRecords = oldBaseline.getBaselineRecords();
        Assert.assertTrue(imaRecords.contains(record));
        Assert.assertEquals(imaRecords.size(), 1);
        Assert.assertTrue(record.getBaseline() == oldBaseline);
        Assert.assertTrue(newBaseline.getBaselineRecords().isEmpty());

        record.setBaseline(newBaseline);
        imaRecords = newBaseline.getBaselineRecords();
        Assert.assertTrue(imaRecords.contains(record));
        Assert.assertEquals(imaRecords.size(), 1);
        Assert.assertTrue(record.getBaseline() == newBaseline);
        Assert.assertTrue(oldBaseline.getBaselineRecords().isEmpty());
    }

    /**
     * Tests that getPath() returns the file path.
     *
     * @throws ParseException
     *             if error generated parsing date fields
     */
    @Test
    public final void getPath() throws ParseException {
        final IMABaselineRecord record = getBaselineRecord();
        final String path = record.getPath();
        Assert.assertNotNull(path);
        Assert.assertEquals(path, PATH);
    }

     /**
     * Tests that two <code>IMABaselineRecord</code>s are equal if they have the
     * same name and the same path.
     *
     * @throws ParseException
     *             if error generated parsing date fields
     */
    @Test
    public final void testEquals() throws ParseException {
        IMABaselineRecord r1 = getBaselineRecord();
        IMABaselineRecord r2 = getBaselineRecord();
        Assert.assertEquals(r1, r2);
        Assert.assertEquals(r2, r1);
        Assert.assertEquals(r1, r1);
        Assert.assertEquals(r2, r2);
    }

    /**
     * Tests that two <code>IMABaselineRecord</code>s are not equal if the names
     * are different.
     *
     * @throws ParseException
     *             if error generated parsing date fields
     */
    @Test
    public final void testNotEqualsName() throws ParseException {
        IMABaselineRecord r1 = getBaselineRecord();
        IMABaselineRecord r2 = getBaselineRecord("/usr/lib/test123", null);
        Assert.assertNotEquals(r1, r2);
        Assert.assertNotEquals(r2, r1);
        Assert.assertEquals(r1, r1);
        Assert.assertEquals(r2, r2);
    }

    /**
     * Tests that two <code>IMABaselineRecord</code>s are not equal if the
     * hashes are different.
     *
     * @throws ParseException
     *             if error generated parsing date fields
     */
    @Test
    public final void testNotEqualsHash() throws ParseException {
        final String hash2 = "aacc3c2f7f3003d2e4baddc46ed4763a4954f648";
        IMABaselineRecord r1 = getBaselineRecord();
        IMABaselineRecord r2 = getBaselineRecord(null, hash2);
        Assert.assertNotEquals(r1, r2);
        Assert.assertNotEquals(r2, r1);
        Assert.assertEquals(r1, r1);
        Assert.assertEquals(r2, r2);
    }

    /**
     * Tests that the hash code of two <code>IMABaselineRecord</code>s is the
     * same if the names and paths are the same.
     *
     * @throws ParseException
     *             if error generated parsing date fields
     */
    @Test
    public final void testHashCodeEquals() throws ParseException {
        IMABaselineRecord r1 = getBaselineRecord();
        IMABaselineRecord r2 = getBaselineRecord();
        Assert.assertEquals(r1.hashCode(), r2.hashCode());
        Assert.assertEquals(r2.hashCode(), r1.hashCode());
        Assert.assertEquals(r1.hashCode(), r1.hashCode());
        Assert.assertEquals(r2.hashCode(), r2.hashCode());
    }

    /**
     * Tests that the hash code of two <code>IMABaselineRecord</code>s is
     * different if they have different names.
     *
     * @throws ParseException
     *             if error generated parsing date fields
     */
    @Test
    public final void testHashCodeNotEqualsNames() throws ParseException {
        IMABaselineRecord r1 = getBaselineRecord();
        IMABaselineRecord r2 = getBaselineRecord("/usr/lib/test123", null);
        Assert.assertNotEquals(r1.hashCode(), r2.hashCode());
        Assert.assertNotEquals(r2.hashCode(), r1.hashCode());
        Assert.assertEquals(r1.hashCode(), r1.hashCode());
        Assert.assertEquals(r2.hashCode(), r2.hashCode());
    }

    /**
     * Tests that the hash code of two <code>IMABaselineRecord</code>s is
     * different if they have different hashes.
     *
     * @throws ParseException
     *             if error generated parsing date fields
     */
    @Test
    public final void testHashCodeNotEqualsHashes() throws ParseException {
        final String hash2 = "aacc3c2f7f3003d2e4baddc46ed4763a4954f648";
        IMABaselineRecord r1 = getBaselineRecord();
        IMABaselineRecord r2 = getBaselineRecord(null, hash2);
        Assert.assertNotEquals(r1.hashCode(), r2.hashCode());
        Assert.assertNotEquals(r2.hashCode(), r1.hashCode());
        Assert.assertEquals(r1.hashCode(), r1.hashCode());
        Assert.assertEquals(r2.hashCode(), r2.hashCode());
    }

    /**
     * Tests that the getPartialPath correctly extracts the filename
     * for a valid input.
     */
    @Test
    public final void testPartialPathFilename() {
        final String path = "/usr/bin/foo";
        String filename = "foo";
        Assert.assertEquals(filename, IMABaselineRecord.getPartialPath(path));
    }

    /**
     * Tests that the getPartialPath fails for a null path argument.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void testPartialPathNullPath() {
        IMABaselineRecord.getPartialPath(null);
    }

    /**
     * Tests that the getPartialPath does not fail for a "/" path argument.
     */
    @Test
    public final void testPartialPathSlashDir() {
        final String path = "/";
        String filename = IMABaselineRecord.getPartialPath(path);
        Assert.assertEquals(filename, "");
    }

    /**
     * Generate a <code>Digest</code> from a hex string.
     *
     * @param hex string of length 40
     * @return the SHA-1 digest representing the given hex string
     */
    public static Digest getDigest(final String hex) {
        byte[] hash;
        try {
            hash = Hex.decodeHex(hex.toCharArray());
        } catch (DecoderException e) {
            LOGGER.error("invalid string hash", e);
            throw new RuntimeException("invalid string hash", e);
        }
        return new Digest(DigestAlgorithm.SHA1, hash);
    }

    private IMABaselineRecord getBaselineRecord() throws ParseException {
        return getBaselineRecord(null, null);
    }

    private IMABaselineRecord getBaselineRecord(final String path,
            final String hash) throws ParseException {
        final String baselinePath;
        if (path == null) {
            baselinePath = PATH;
        } else {
            baselinePath = path;
        }
        final String baselineHash;
        if (hash == null) {
            baselineHash = HASH;
        } else {
            baselineHash = hash;
        }
        final Digest digest = getDigest(baselineHash);
        return new IMABaselineRecord(baselinePath, digest);
    }
}
