package hirs.data.persist;

import java.text.ParseException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Unit tests for the class <code>IMAMeasurementRecord</code>.
 */
public class IMAMeasurementRecordTest {

    private static final Logger LOGGER
            = LogManager.getLogger(IMAMeasurementRecordTest.class);
    private static final String DEFAULT_PATH = "/path/to/my/file";
    private static final String DEFAULT_HASH =
            "098306e430e1121d3eb2967df6227b011c79c722";
    private static final ExamineState DEFAULT_STATE = ExamineState.UNEXAMINED;

    /**
     * Tests that <code>IMAMeasurementRecord</code> can be created.
     */
    @Test
    public final void imaMeasurementRecord() {
        new IMAMeasurementRecord(DEFAULT_PATH, getDefaultDigest());
    }

    /**
     * Tests that <code>IMAMeasurementRecord</code> throws
     * <code>NullPointerException</code> for null name.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void imaMeasurementRecordNullName() {
        new IMAMeasurementRecord(null, getDefaultDigest());
    }

    /**
     * Tests that <code>IMAMeasurementRecord</code> throws
     * <code>NullPointerException</code> for null name.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void imaMeasurementRecordNullHash() {
        new IMAMeasurementRecord(DEFAULT_PATH, null);
    }

    /**
     * Tests that <code>getPath</code> returns the correct path.
     */
    @Test
    public final void getPath() {
        final IMAMeasurementRecord record = getDefaultRecord();
        Assert.assertEquals(record.getPath(), DEFAULT_PATH);
    }

    /**
     * Tests that <code>getHash</code> returns the correct hash.
     */
    @Test
    public final void getHash() {
        final IMAMeasurementRecord record = getDefaultRecord(DEFAULT_PATH, DEFAULT_HASH);
        Assert.assertEquals(record.getHash(), getDefaultDigest());
    }

    /**
     * Tests that <code>getExamineState</code> returns the correct state.
     */
    @Test
    public final void getExamineState() {
        final IMAMeasurementRecord record = getDefaultRecord();
        Assert.assertEquals(record.getExamineState(), DEFAULT_STATE);
    }

    /**
     * Tests shows that two <code>IMAMeasurementRecord</code>s are not equal even if they have
     * the same name and the same path.  They must have the same ID.
     */
    @Test
    public final void testEquals() {
        IMAMeasurementRecord r1 = getDefaultRecord();
        IMAMeasurementRecord r2 = getDefaultRecord();
        Assert.assertNotEquals(r1, r2);
        Assert.assertNotEquals(r2, r1);
        Assert.assertEquals(r1, r1);
        Assert.assertEquals(r2, r2);
    }

    /**
     * Tests that two <code>IMAMeasurementRecord</code>s are not equal if the
     * names are different.
     */
    @Test
    public final void testNotEqualsName() {
        final String path = "/some/other/file";
        IMAMeasurementRecord r1 = getDefaultRecord();
        IMAMeasurementRecord r2 =
                new IMAMeasurementRecord(path, getDefaultDigest());
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
     * @throws DecoderException
     *             if unexpectedly unable to convert hash string to byte array
     */
    @Test
    public final void testNotEqualsHash() throws ParseException,
            DecoderException {
        final String hash = "aacc3c2f7f3003d2e4baddc46ed4763a4954f648";
        final Digest digest = new Digest(DigestAlgorithm.SHA1,
                Hex.decodeHex(hash.toCharArray()));
        final IMAMeasurementRecord r1 = getDefaultRecord();
        final IMAMeasurementRecord r2 =
                new IMAMeasurementRecord(DEFAULT_PATH, digest);
        Assert.assertNotEquals(r1, r2);
        Assert.assertNotEquals(r2, r1);
        Assert.assertEquals(r1, r1);
        Assert.assertEquals(r2, r2);
    }

    /**
     * Tests that the hash code of two <code>IMABaselineRecord</code>s are not the
     * same if the names and paths are the same.  The ID of the objects has to be the
     * same for equality.
     *
     * @throws ParseException
     *             if error generated parsing date fields
     */
    @Test
    public final void testHashCodeEquals() throws ParseException {
        IMAMeasurementRecord r1 = getDefaultRecord();
        IMAMeasurementRecord r2 = getDefaultRecord();
        Assert.assertNotEquals(r1.hashCode(), r2.hashCode());
        Assert.assertNotEquals(r2.hashCode(), r1.hashCode());
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
        final String path = "/some/other/file";
        IMAMeasurementRecord r1 = getDefaultRecord();
        IMAMeasurementRecord r2 =
                new IMAMeasurementRecord(path, getDefaultDigest());
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
     * @throws DecoderException
     *             if unexpectedly unable to convert hash string to byte array
     */
    @Test
    public final void testHashCodeNotEqualsHashes() throws ParseException,
            DecoderException {
        final String hash = "aacc3c2f7f3003d2e4baddc46ed4763a4954f648";
        final Digest digest = new Digest(DigestAlgorithm.SHA1,
                Hex.decodeHex(hash.toCharArray()));
        IMAMeasurementRecord r1 = getDefaultRecord();
        IMAMeasurementRecord r2 =
                new IMAMeasurementRecord(DEFAULT_PATH, digest);
        Assert.assertNotEquals(r1.hashCode(), r2.hashCode());
        Assert.assertNotEquals(r2.hashCode(), r1.hashCode());
        Assert.assertEquals(r1.hashCode(), r1.hashCode());
        Assert.assertEquals(r2.hashCode(), r2.hashCode());
    }

    /**
     * Tests that the ExamineState can be successfully set to EXAMINED.
     */
    @Test
    public final void testSetExamineStateExamined() {
        final ExamineState state = ExamineState.EXAMINED;
        IMAMeasurementRecord r1 = getDefaultRecord();
        r1.setExamineState(state);
        Assert.assertEquals(r1.getExamineState(), state);
    }

    /**
     * Tests that the ExamineState can be successfully set to IGNORED.
     */
    @Test
    public final void testSetExamineStateIgnored() {
        final ExamineState state = ExamineState.IGNORED;
        IMAMeasurementRecord r1 = getDefaultRecord();
        r1.setExamineState(state);
        Assert.assertEquals(r1.getExamineState(), state);
    }

    /**
     * Tests that the ExamineState is successfully initialized to UNEXAMINED.
     */
    @Test
    public final void testSetExamineStateInitial() {
        IMAMeasurementRecord r1 = getDefaultRecord();
        Assert.assertEquals(r1.getExamineState(), ExamineState.UNEXAMINED);
    }

    /**
     * Tests that setting the ExamineState to UNEXAMINED throws an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void testSetExamineStateUnexamined() {
        final ExamineState state = ExamineState.UNEXAMINED;
        IMAMeasurementRecord r1 = getDefaultRecord();
        r1.setExamineState(state);
    }

    private IMAMeasurementRecord getDefaultRecord() {
        return getDefaultRecord(DEFAULT_PATH, DEFAULT_HASH);
    }

    private IMAMeasurementRecord getDefaultRecord(final String path,
            final String hash) {
        final int sha1HashLength = 20;
        final String sPath;
        if (path == null) {
            sPath = DEFAULT_PATH;
        } else {
            sPath = path;
        }
        byte[] bHash;
        if (hash == null) {
            bHash = getTestDigest(sha1HashLength);
        } else {
            bHash = getDigest(hash);
        }
        final Digest digest = new Digest(DigestAlgorithm.SHA1, bHash);
        return new IMAMeasurementRecord(sPath, digest);
    }

    private byte[] getDigest(final String hash) {
        try {
            return Hex.decodeHex(hash.toCharArray());
        } catch (DecoderException e) {
            LOGGER.error("unable to create digest", e);
            throw new RuntimeException("unable to create digest", e);
        }
    }

    private Digest getDefaultDigest() {
        return new Digest(DigestAlgorithm.SHA1, getDigest(DEFAULT_HASH));
    }

    private byte[] getTestDigest(final int count) {
        final byte[] ret = new byte[count];
        for (int i = 0; i < count; ++i) {
            ret[i] = (byte) i;
        }
        return ret;
    }
}
