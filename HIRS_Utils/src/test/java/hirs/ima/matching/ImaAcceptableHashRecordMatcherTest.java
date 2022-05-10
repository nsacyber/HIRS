package hirs.ima.matching;

import hirs.data.persist.Digest;
import hirs.data.persist.baseline.IMABaselineRecord;
import hirs.data.persist.enums.ReportMatchStatus;
import hirs.data.persist.baseline.SimpleImaBaseline;
import hirs.data.persist.SimpleImaBaselineTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests for the ImaAcceptableHashRecordMatcher.  These are basic tests of its functionality;
 * more complete tests for contains() as used operationally by baselines that test various
 * permutations of parameters are located in SimpleImaBaselineTest, BroadRepoImaBaselineTest,
 * TargetedRepoImaBaselineTest, ImaBlacklistBaselineTest, and ImaIgnoreSetBaselineTest.
 */
public class ImaAcceptableHashRecordMatcherTest {
    private static final String FILENAME_1 = "/bin/ls";
    private static final String FILENAME_2 = "/bin/ls_with_another_name";
    private static final Digest HASH_1 =
            SimpleImaBaselineTest.getDigest("33333c2f7f3003d2e4baddc46ed4763a49543333");
    private static final Digest HASH_2 =
            SimpleImaBaselineTest.getDigest("00000c2f7f3003d2e4baddc46ed4763a49543333");

    /**
     * Tests that the 'contains' method functions if no records are given.
     */
    @Test
    public void testContainsEmpty() {
        SimpleImaBaseline baseline = getTestSimpleImaBaseline();
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(FILENAME_1, HASH_1);
        Assert.assertEquals(
                new ImaAcceptableHashRecordMatcher(
                        Collections.emptyList(),
                        SimpleImaBaselineTest.getTestImaPolicy(false),
                        baseline).contains(measurementRecord),
                new IMAMatchStatus(measurementRecord, ReportMatchStatus.UNKNOWN, baseline)
        );
    }

    /**
     * Tests that the 'contains' method functions if a matching record is given in the case of that
     * matching record being both filename and hash.
     */
    @Test
    public void testContainsSameFilename() {
        SimpleImaBaseline baseline = getTestSimpleImaBaseline();
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(FILENAME_1, HASH_1);
        IMABaselineRecord baselineRecord = new IMABaselineRecord(FILENAME_1, HASH_1);
        Assert.assertEquals(
                new ImaAcceptableHashRecordMatcher(
                        Collections.singletonList(baselineRecord),
                        SimpleImaBaselineTest.getTestImaPolicy(false),
                        baseline).contains(measurementRecord),
                new IMAMatchStatus<>(
                        measurementRecord, ReportMatchStatus.MATCH, baselineRecord, baseline
                )
        );
    }

    /**
     * Tests that the 'contains' method matches on hash properly, even if
     * a measurement record has a different filename than the matching baseline record.
     */
    @Test
    public void testContainsDifferentFilename() {
        SimpleImaBaseline baseline = getTestSimpleImaBaseline();
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(FILENAME_2, HASH_1);
        IMABaselineRecord baselineRecord = new IMABaselineRecord(FILENAME_1, HASH_1);
        Assert.assertEquals(
                new ImaAcceptableHashRecordMatcher(
                        Collections.singletonList(baselineRecord),
                        SimpleImaBaselineTest.getTestImaPolicy(false),
                        baseline).contains(measurementRecord),
                new IMAMatchStatus<>(
                        measurementRecord, ReportMatchStatus.MATCH, baselineRecord, baseline
                )
        );
    }

    /**
     * Tests that the 'contains' method returns the expected 'UNKNOWN' match
     * status if no record matches the collected measurement in a nonempty baseline.
     */
    @Test
    public void testContainsNonEmptyButUnknown() {
        SimpleImaBaseline baseline = getTestSimpleImaBaseline();
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(FILENAME_1, HASH_1);
        IMABaselineRecord baselineRecord = new IMABaselineRecord(FILENAME_1, HASH_2);
        Assert.assertEquals(
                new ImaAcceptableHashRecordMatcher(
                        Collections.singletonList(baselineRecord),
                        SimpleImaBaselineTest.getTestImaPolicy(false),
                        baseline).contains(measurementRecord),
                new IMAMatchStatus(measurementRecord, ReportMatchStatus.UNKNOWN, baseline)
        );
    }

    /**
     * Tests that the 'contains' method returns all matching baseline records from a
     * baseline when there are multiple matches to a given measurement record.
     */
    @Test
    public void testContainsMultipleMatchingBaselineRecords() {
        SimpleImaBaseline baseline = getTestSimpleImaBaseline();
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(FILENAME_1, HASH_1);
        Set<IMABaselineRecord> baselineRecords = new HashSet<>(Arrays.asList(
                new IMABaselineRecord(FILENAME_1, HASH_1),
                new IMABaselineRecord(FILENAME_2, HASH_1),
                new IMABaselineRecord(FILENAME_1, HASH_2)
        ));
        Assert.assertEquals(
                new ImaAcceptableHashRecordMatcher(
                        baselineRecords,
                        SimpleImaBaselineTest.getTestImaPolicy(false),
                        baseline
                ).contains(measurementRecord),
                new IMAMatchStatus<>(
                        measurementRecord,
                        ReportMatchStatus.MATCH,
                        new HashSet<>(Arrays.asList(
                            new IMABaselineRecord(FILENAME_1, HASH_1),
                            new IMABaselineRecord(FILENAME_2, HASH_1))
                        ),
                        baseline
                )
        );
    }

    /**
     * Tests that the 'contains' method throws an IllegalArgumentException if given a null
     * measurement record to check.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testContainsOnNullRecord() {
        SimpleImaBaseline baseline = getTestSimpleImaBaseline();
        IMABaselineRecord baselineRecord = new IMABaselineRecord(FILENAME_1, HASH_1);
        new ImaAcceptableHashRecordMatcher(
                Collections.singletonList(baselineRecord),
                SimpleImaBaselineTest.getTestImaPolicy(false),
                baseline).contains(null);
    }

    private static SimpleImaBaseline getTestSimpleImaBaseline() {
        return new SimpleImaBaseline("Test IMA Baseline");
    }
}
