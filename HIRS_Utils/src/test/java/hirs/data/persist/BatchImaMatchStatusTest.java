package hirs.data.persist;

import hirs.ima.matching.BatchImaMatchStatus;
import hirs.ima.matching.IMAMatchStatus;

import org.apache.commons.codec.binary.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * Unit tests for {@link BatchImaMatchStatus}.
 */
public class BatchImaMatchStatusTest {
    private static final ImaBaseline BASELINE_1 = new SimpleImaBaseline("Baseline 1");
    private static final ImaBaseline BASELINE_2 = new SimpleImaBaseline("Baseline 2");

    private static final String PATH = "/test/path";
    private static final String DIGEST_STRING = "c068d837b672cb3f80ac";
    private static final Digest DIGEST;
    private static final IMAMeasurementRecord IMA_MEASUREMENT_RECORD;
    private static final IMABaselineRecord IMA_BASELINE_RECORD;
    private static final IMAMatchStatus<IMABaselineRecord> IMA_MATCH_STATUS;

    private static final String PATH_2 = "/another/test/path";
    private static final String DIGEST_STRING_2 = "d1cac837b672cb3f80ac";
    private static final Digest DIGEST_2;
    private static final IMAMeasurementRecord IMA_MEASUREMENT_RECORD_2;
    private static final IMABaselineRecord IMA_BASELINE_RECORD_2;
    private static final IMAMatchStatus<IMABaselineRecord> IMA_MATCH_STATUS_2;

    private static final String PATH_3 = "/another/test/path3";
    private static final String DIGEST_STRING_3 = "d1cac837b672cb3f80ac";
    private static final Digest DIGEST_3;
    private static final IMAMeasurementRecord IMA_MEASUREMENT_RECORD_3;
    private static final IMABaselineRecord IMA_BASELINE_RECORD_3;
    private static final IMAMatchStatus<IMABaselineRecord> IMA_MATCH_STATUS_3;

    private static final String DIGEST_STRING_3_MISMATCH = "abcac837b672cb3f80ac";
    private static final Digest DIGEST_3_MISMATCH;
    private static final IMABaselineRecord IMA_BASELINE_RECORD_3_MISMATCH;
    private static final IMAMatchStatus<IMABaselineRecord> IMA_MATCH_STATUS_4;

    private static final String PATH_5 = "/another/test/path5";
    private static final String DIGEST_STRING_5 = "defac837b672cb3f4242";
    private static final Digest DIGEST_5;
    private static final IMAMeasurementRecord IMA_MEASUREMENT_RECORD_5;
    private static final IMAMatchStatus<IMABaselineRecord> IMA_MATCH_STATUS_5;

    static {
        DIGEST =
                new Digest(DigestAlgorithm.SHA1, StringUtils.getBytesUtf8(DIGEST_STRING));
        DIGEST_2 =
                new Digest(DigestAlgorithm.SHA1, StringUtils.getBytesUtf8(DIGEST_STRING_2));
        DIGEST_3 =
                new Digest(DigestAlgorithm.SHA1, StringUtils.getBytesUtf8(DIGEST_STRING_3));
        DIGEST_3_MISMATCH =
                new Digest(DigestAlgorithm.SHA1,
                        StringUtils.getBytesUtf8(DIGEST_STRING_3_MISMATCH));
        DIGEST_5 =
                new Digest(DigestAlgorithm.SHA1, StringUtils.getBytesUtf8(DIGEST_STRING_5));

        IMA_MEASUREMENT_RECORD = new IMAMeasurementRecord(PATH, DIGEST);
        IMA_BASELINE_RECORD = new IMABaselineRecord(PATH, DIGEST);
        IMA_MATCH_STATUS = new IMAMatchStatus<>(
                IMA_MEASUREMENT_RECORD,
                ReportMatchStatus.MATCH,
                IMA_BASELINE_RECORD,
                BASELINE_1
        );

        IMA_MEASUREMENT_RECORD_2 = new IMAMeasurementRecord(PATH_2, DIGEST_2);
        IMA_BASELINE_RECORD_2 = new IMABaselineRecord(PATH_2, DIGEST_2);
        IMA_MATCH_STATUS_2 = new IMAMatchStatus<>(
                IMA_MEASUREMENT_RECORD_2,
                ReportMatchStatus.MATCH,
                IMA_BASELINE_RECORD_2,
                BASELINE_1
        );

        IMA_MEASUREMENT_RECORD_3 = new IMAMeasurementRecord(PATH_3, DIGEST_3);
        IMA_BASELINE_RECORD_3 = new IMABaselineRecord(PATH_3, DIGEST_3);
        IMA_MATCH_STATUS_3 = new IMAMatchStatus<>(
                IMA_MEASUREMENT_RECORD_3,
                ReportMatchStatus.MATCH,
                IMA_BASELINE_RECORD_3,
                BASELINE_1
        );

        IMA_BASELINE_RECORD_3_MISMATCH = new IMABaselineRecord(PATH_3, DIGEST_3_MISMATCH);
        IMA_MATCH_STATUS_4 = new IMAMatchStatus<>(
                IMA_MEASUREMENT_RECORD_3,
                ReportMatchStatus.MISMATCH,
                IMA_BASELINE_RECORD_3_MISMATCH,
                BASELINE_2
        );

        IMA_MEASUREMENT_RECORD_5 = new IMAMeasurementRecord(PATH_5, DIGEST_5);
        IMA_MATCH_STATUS_5 = new IMAMatchStatus<>(
                IMA_MEASUREMENT_RECORD_5,
                ReportMatchStatus.UNKNOWN,
                BASELINE_1
        );
    }

    /**
     * Tests that an empty {@link BatchImaMatchStatus} behaves as expected.
     */
    @Test
    public final void testContainsAndGetAppraisedMeasurementRecords() {
        BatchImaMatchStatus<IMABaselineRecord> matchStatus = new BatchImaMatchStatus<>();
        Assert.assertEquals(matchStatus.getAppraisedMeasurementRecords().size(), 0);

        matchStatus = new BatchImaMatchStatus<>();
        matchStatus.add(IMA_MATCH_STATUS);
        Assert.assertTrue(matchStatus.contains(IMA_MEASUREMENT_RECORD));
        Assert.assertFalse(matchStatus.contains(IMA_MEASUREMENT_RECORD_2));
        Assert.assertEquals(matchStatus.getAppraisedMeasurementRecords(),
                Collections.singletonList(IMA_MEASUREMENT_RECORD));

        matchStatus = getTestImaMatchStatus();
        Assert.assertTrue(matchStatus.contains(IMA_MEASUREMENT_RECORD));
        Assert.assertTrue(matchStatus.contains(IMA_MEASUREMENT_RECORD_2));
        Assert.assertTrue(matchStatus.contains(IMA_MEASUREMENT_RECORD_3));
        Assert.assertTrue(matchStatus.contains(IMA_MEASUREMENT_RECORD_5));
        Assert.assertEquals(matchStatus.getAppraisedMeasurementRecords(),
                new HashSet<>(Arrays.asList(
                        IMA_MEASUREMENT_RECORD,
                        IMA_MEASUREMENT_RECORD_2,
                        IMA_MEASUREMENT_RECORD_3,
                        IMA_MEASUREMENT_RECORD_5
                ))
        );
    }

    private BatchImaMatchStatus<IMABaselineRecord> getTestImaMatchStatus() {
        return new BatchImaMatchStatus<>(
                Arrays.asList(
                        IMA_MATCH_STATUS,
                        IMA_MATCH_STATUS_2,
                        IMA_MATCH_STATUS_3,
                        IMA_MATCH_STATUS_4,
                        IMA_MATCH_STATUS_5
                )
        );
    }

    /**
     * Tests that getBaselineRecords performs as expected across matches, mismatches, and
     * unknowns.
     */
    @Test
    public final void testGetBaselineRecords() {
        BatchImaMatchStatus matchStatus = getTestImaMatchStatus();

        Assert.assertEquals(matchStatus.getBaselineRecords(IMA_MEASUREMENT_RECORD),
                Collections.singleton(IMA_BASELINE_RECORD));

        Assert.assertEquals(matchStatus.getBaselineRecords(IMA_MEASUREMENT_RECORD_2),
                Collections.singleton(IMA_BASELINE_RECORD_2));

        Assert.assertEquals(matchStatus.getBaselineRecords(IMA_MEASUREMENT_RECORD_3),
                new HashSet<>(Arrays.asList(IMA_BASELINE_RECORD_3, IMA_BASELINE_RECORD_3_MISMATCH))
        );

        Assert.assertEquals(matchStatus.getBaselineRecords(IMA_MEASUREMENT_RECORD_5),
                Collections.<IMABaselineRecord>emptySet());
    }

    /**
     * Tests that getMachingBaselineRecords and getMismatchingBaselineRecords work as expected.
     */
    @Test
    public final void testGetCertainBaselineRecords() {
        BatchImaMatchStatus matchStatus = getTestImaMatchStatus();

        Assert.assertEquals(matchStatus.getMatchingBaselineRecords(IMA_MEASUREMENT_RECORD),
                Collections.singleton(IMA_BASELINE_RECORD));

        Assert.assertEquals(matchStatus.getMismatchingBaselineRecords(IMA_MEASUREMENT_RECORD),
                Collections.<IMABaselineRecord>emptySet());

        Assert.assertEquals(matchStatus.getMatchingBaselineRecords(IMA_MEASUREMENT_RECORD_2),
                Collections.singleton(IMA_BASELINE_RECORD_2));

        Assert.assertEquals(matchStatus.getMismatchingBaselineRecords(IMA_MEASUREMENT_RECORD_2),
                Collections.<IMABaselineRecord>emptySet());

        Assert.assertEquals(matchStatus.getMatchingBaselineRecords(IMA_MEASUREMENT_RECORD_3),
                Collections.singleton(IMA_BASELINE_RECORD_3));

        Assert.assertEquals(matchStatus.getMismatchingBaselineRecords(IMA_MEASUREMENT_RECORD_3),
                Collections.singleton(IMA_BASELINE_RECORD_3_MISMATCH));
    }

    /**
     * Tests that retrieving baseline records from a {@link BatchImaMatchStatus} for a measurement
     * record it doesn't contain throws an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void testGetBaselineRecordMissingMeasurement() {
        new BatchImaMatchStatus().getBaselineRecords(IMA_MEASUREMENT_RECORD);
    }

    /**
     * Tests that retrieving matching baseline records from a {@link BatchImaMatchStatus} for a
     * measurement record it doesn't contain throws an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void testGetMatchingBaselineRecordMissingMeasurement() {
        new BatchImaMatchStatus().getMatchingBaselineRecords(IMA_MEASUREMENT_RECORD);
    }

    /**
     * Tests that retrieving mismatching baseline records from a {@link BatchImaMatchStatus} for a
     * measurement record it doesn't contain throws an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void testGetMismatchingBaselineRecordMissingMeasurement() {
        new BatchImaMatchStatus().getMismatchingBaselineRecords(IMA_MEASUREMENT_RECORD);
    }

    /**
     * Tests that a {@link BatchImaMatchStatus} with a single record returns a match status for the
     * a corresponding measurement record.
     */
    @Test
    public final void testSingleRecord() {
        BatchImaMatchStatus<IMABaselineRecord> matchStatus = new BatchImaMatchStatus<>();
        matchStatus.add(IMA_MATCH_STATUS);
        Assert.assertEquals(matchStatus.getIMAMatchStatuses(IMA_MEASUREMENT_RECORD),
                Collections.singletonList(IMA_MATCH_STATUS));
    }

    /**
     * Tests that a {@link BatchImaMatchStatus} instantiated with a null collection throws a
     * {@link IllegalArgumentException}.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void testNullCollection() {
        new BatchImaMatchStatus<IMABaselineRecord>(null);
    }

    /**
     * Tests that a {@link BatchImaMatchStatus} instantiated with a collection containing a null
     * value throws a {@link IllegalArgumentException}.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void testCollectionWithNullElements() {
        Collection<IMAMatchStatus<IMABaselineRecord>> matches =
                Arrays.asList(IMA_MATCH_STATUS, IMA_MATCH_STATUS_2, null);
        new BatchImaMatchStatus<>(matches);
    }

    /**
     * Tests that a {@link BatchImaMatchStatus} with two records returns corresponding matches
     * for each record.
     */
    @Test
    public final void testManyRecords() {
        BatchImaMatchStatus<IMABaselineRecord> matchStatus = new BatchImaMatchStatus<>(
                Arrays.asList(IMA_MATCH_STATUS, IMA_MATCH_STATUS_2)
        );
        Assert.assertEquals(matchStatus.getIMAMatchStatuses(IMA_MEASUREMENT_RECORD),
                Collections.singletonList(IMA_MATCH_STATUS));
        Assert.assertEquals(
                matchStatus.getIMAMatchStatuses(IMA_MEASUREMENT_RECORD_2),
                Collections.singletonList(IMA_MATCH_STATUS_2)
        );
    }

    /**
     * Tests that foundMatch returns true/false as appropriate where matches have been found
     * or not found for certain measurement records.
     */
    @Test
    public final void testFoundMatch() {
        BatchImaMatchStatus matchStatus = getTestImaMatchStatus();

        Assert.assertTrue(matchStatus.foundMatch(IMA_MEASUREMENT_RECORD));
        Assert.assertTrue(matchStatus.foundMatch(IMA_MEASUREMENT_RECORD_2));
        Assert.assertTrue(matchStatus.foundMatch(IMA_MEASUREMENT_RECORD_3));
        Assert.assertFalse(matchStatus.foundMatch(IMA_MEASUREMENT_RECORD_5));
    }

    /**
     * Tests that calling foundMatch with a measurement not contained in a
     * {@link BatchImaMatchStatus} throws an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void testFoundMatchWithMissingRecord() {
        new BatchImaMatchStatus().foundMatch(IMA_MEASUREMENT_RECORD);
    }

    /**
     * Tests that calling foundMismatch with a measurement not contained in a
     * {@link BatchImaMatchStatus} throws an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void testFoundMismatchWithMissingRecord() {
        new BatchImaMatchStatus().foundMismatch(IMA_MEASUREMENT_RECORD);
    }

    /**
     * Tests that calling foundOnlyUnknown with a measurement not contained in a
     * {@link BatchImaMatchStatus} throws an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void testFoundOnlyUnknownWithMissingRecord() {
        new BatchImaMatchStatus().foundOnlyUnknown(IMA_MEASUREMENT_RECORD);
    }

    /**
     * Tests that foundMismatch returns true/false as appropriate where mismatches have been found
     * or not found for certain measurement records.
     */
    @Test
    public final void testFoundMismatch() {
        BatchImaMatchStatus matchStatus = getTestImaMatchStatus();

        Assert.assertFalse(matchStatus.foundMismatch(IMA_MEASUREMENT_RECORD));
        Assert.assertTrue(matchStatus.foundMismatch(IMA_MEASUREMENT_RECORD_3));
        Assert.assertFalse(matchStatus.foundMismatch(IMA_MEASUREMENT_RECORD_5));
    }

    /**
     * Tests that foundOnlyUnknown returns true/false as appropriate where no matches or mismatches,
     * but only 'unknown' results, have been found.
     */
    @Test
    public final void testFoundOnlyUnknown() {
        BatchImaMatchStatus matchStatus = getTestImaMatchStatus();

        Assert.assertFalse(matchStatus.foundOnlyUnknown(IMA_MEASUREMENT_RECORD));
        Assert.assertFalse(matchStatus.foundOnlyUnknown(IMA_MEASUREMENT_RECORD_3));
        Assert.assertTrue(matchStatus.foundOnlyUnknown(IMA_MEASUREMENT_RECORD_5));
    }

    /**
     * Tests that {@link BatchImaMatchStatus} evaluates equality based on its matches.
     */
    @Test
    public final void testEquals() {
        Assert.assertTrue(new BatchImaMatchStatus().equals(new BatchImaMatchStatus()));

        BatchImaMatchStatus<IMABaselineRecord> matchStatus = new BatchImaMatchStatus<>();
        matchStatus.add(IMA_MATCH_STATUS);
        Assert.assertFalse(matchStatus.equals(new BatchImaMatchStatus()));

        BatchImaMatchStatus<IMABaselineRecord> anotherMatchStatus = new BatchImaMatchStatus<>();
        anotherMatchStatus.add(IMA_MATCH_STATUS_2);
        Assert.assertFalse(matchStatus.equals(anotherMatchStatus));

        matchStatus.add(IMA_MATCH_STATUS_2);
        anotherMatchStatus.add(IMA_MATCH_STATUS);
        Assert.assertTrue(matchStatus.equals(anotherMatchStatus));
    }

    /**
     * Tests that {@link BatchImaMatchStatus} constructs its hashcode based on its matches.
     */
    @Test
    public final void testHashCode() {
        Assert.assertEquals(
                new BatchImaMatchStatus().hashCode(),
                new BatchImaMatchStatus().hashCode()
        );

        BatchImaMatchStatus<IMABaselineRecord> matchStatus = new BatchImaMatchStatus<>();
        matchStatus.add(IMA_MATCH_STATUS);
        Assert.assertNotEquals(matchStatus.hashCode(), new BatchImaMatchStatus().hashCode());

        BatchImaMatchStatus<IMABaselineRecord> anotherMatchStatus = new BatchImaMatchStatus<>();
        anotherMatchStatus.add(IMA_MATCH_STATUS_2);
        Assert.assertNotEquals(matchStatus.hashCode(), anotherMatchStatus.hashCode());

        matchStatus.add(IMA_MATCH_STATUS_2);
        anotherMatchStatus.add(IMA_MATCH_STATUS);
        Assert.assertEquals(matchStatus.hashCode(), anotherMatchStatus.hashCode());
    }
}
