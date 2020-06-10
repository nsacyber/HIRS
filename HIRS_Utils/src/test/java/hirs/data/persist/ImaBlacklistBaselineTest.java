package hirs.data.persist;

import hirs.data.persist.baseline.ImaBaseline;
import hirs.data.persist.baseline.ImaBlacklistBaseline;
import hirs.data.persist.enums.ReportMatchStatus;
import hirs.ima.matching.BatchImaMatchStatus;
import hirs.ima.matching.IMAMatchStatus;
import hirs.persist.BaselineManager;
import hirs.persist.DBBaselineManager;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link ImaBlacklistBaseline}.
 */
public class ImaBlacklistBaselineTest extends SpringPersistenceTest {
    private static final int THREE = 3;
    private static final int FOUR = 4;
    private static final int FIVE = 5;
    private static final int SEVEN = 7;
    private static final String TEST_NAME = "Test Blacklist Baseline";

    private IMAPolicy policyEnabledPartialPath;
    private IMAPolicy policyDisabledPartialPath;

    /**
     * Set up IMA policies for use in the tests.
     */
    @BeforeClass
    public void setup() {
        policyEnabledPartialPath = new IMAPolicy("Test Policy - Partial Path Enabled");
        policyEnabledPartialPath.setPartialPathEnable(true);
        policyDisabledPartialPath = new IMAPolicy("Test Policy - Partial Path Disabled");
        policyDisabledPartialPath.setPartialPathEnable(false);
    }

    /**
     * Tests that a new IMA blacklist baseline can be constructed.
     */
    @Test
    public void testConstruct() {
        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline(TEST_NAME);
        Assert.assertEquals(baseline.getName(), TEST_NAME);
    }

    /**
     * Tests that an ImaBlacklistBaseline can't be constructed with a null name.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testConstructNullName() {
        new ImaBlacklistBaseline(null);
    }

    /**
     * Tests that a baseline record can be added to a baseline.
     */
    @Test
    public void testAddBaselineRecord() {
        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline(TEST_NAME);
        ImaBlacklistRecord record = ImaBlacklistRecordTest.getTestBlacklistRecord();
        Assert.assertTrue(baseline.addToBaseline(record));
        Assert.assertEquals(baseline.getRecords().size(), 1);
        Assert.assertTrue(baseline.getRecords().contains(record));
    }

    /**
     * Tests that a baseline record can be removed from a baseline.
     */
    @Test
    public void testRemoveBaselineRecord() {
        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline(TEST_NAME);
        ImaBlacklistRecord record = ImaBlacklistRecordTest.getTestBlacklistRecord();

        Assert.assertTrue(baseline.addToBaseline(record));
        Assert.assertEquals(baseline.getRecords().size(), 1);
        Assert.assertTrue(baseline.getRecords().contains(record));

        Assert.assertTrue(baseline.removeFromBaseline(record));
        Assert.assertEquals(baseline.getRecords().size(), 0);
        Assert.assertFalse(baseline.getRecords().contains(record));
    }

    /**
     * Tests that multiple baseline records can be added to a baseline.
     */
    @Test
    public void testAddManyBaselineRecords() {
        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline(TEST_NAME);
        ImaBlacklistRecord record1 = new ImaBlacklistRecord(
                "/test",
                DigestTest.getTestSHA1Digest()
        );
        ImaBlacklistRecord record2 = new ImaBlacklistRecord(
                "/test2",
                DigestTest.getTestSHA1Digest()
        );
        ImaBlacklistRecord record3 = new ImaBlacklistRecord(
                "/test2",
                DigestTest.getTestSHA1Digest((byte) 2)
        );

        Assert.assertEquals(baseline.getRecords().size(), 0);

        Assert.assertTrue(baseline.addToBaseline(record1));
        Assert.assertEquals(baseline.getRecords().size(), 1);
        Assert.assertTrue(baseline.getRecords().contains(record1));

        Assert.assertFalse(baseline.addToBaseline(record1));
        Assert.assertEquals(baseline.getRecords().size(), 1);

        Assert.assertTrue(baseline.addToBaseline(record2));
        Assert.assertEquals(baseline.getRecords().size(), 2);
        Assert.assertTrue(baseline.getRecords().contains(record2));

        Assert.assertTrue(baseline.addToBaseline(record3));
        Assert.assertEquals(baseline.getRecords().size(), THREE);
        Assert.assertTrue(baseline.getRecords().contains(record3));
    }

    /**
     * Tests that multiple baseline records can be added to a baseline, and that one can
     * then be removed.
     */
    @Test
    public void testAddManyBaselineRecordsRemoveOne() {
        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline(TEST_NAME);
        ImaBlacklistRecord record1 = new ImaBlacklistRecord(
                "/test",
                DigestTest.getTestSHA1Digest()
        );
        ImaBlacklistRecord record2 = new ImaBlacklistRecord(
                "/test2",
                DigestTest.getTestSHA1Digest()
        );
        ImaBlacklistRecord record3 = new ImaBlacklistRecord(
                "/test2",
                DigestTest.getTestSHA1Digest((byte) 2)
        );

        Assert.assertEquals(baseline.getRecords().size(), 0);

        Assert.assertTrue(baseline.addToBaseline(record1));
        Assert.assertEquals(baseline.getRecords().size(), 1);
        Assert.assertTrue(baseline.getRecords().contains(record1));

        Assert.assertFalse(baseline.addToBaseline(record1));
        Assert.assertEquals(baseline.getRecords().size(), 1);

        Assert.assertTrue(baseline.addToBaseline(record2));
        Assert.assertEquals(baseline.getRecords().size(), 2);
        Assert.assertTrue(baseline.getRecords().contains(record2));

        Assert.assertTrue(baseline.addToBaseline(record3));
        Assert.assertEquals(baseline.getRecords().size(), THREE);
        Assert.assertTrue(baseline.getRecords().contains(record3));

        Assert.assertTrue(baseline.removeFromBaseline(record2));
        Assert.assertEquals(baseline.getRecords().size(), 2);
        Assert.assertTrue(baseline.getRecords().contains(record1));
        Assert.assertFalse(baseline.getRecords().contains(record2));
        Assert.assertTrue(baseline.getRecords().contains(record3));
    }

    /**
     * Tests that a baseline is unaffected by attempting to remove a record that it doesn't
     * hold.
     */
    @Test
    public void testRemoveNonExistentRecord() {
        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline(TEST_NAME);
        ImaBlacklistRecord record1 = new ImaBlacklistRecord(
                "/test",
                DigestTest.getTestSHA1Digest()
        );
        ImaBlacklistRecord record2 = new ImaBlacklistRecord(
                "/test2",
                DigestTest.getTestSHA1Digest()
        );

        Assert.assertEquals(baseline.getRecords().size(), 0);

        Assert.assertTrue(baseline.addToBaseline(record1));
        Assert.assertEquals(baseline.getRecords().size(), 1);
        Assert.assertTrue(baseline.getRecords().contains(record1));

        Assert.assertFalse(baseline.removeFromBaseline(record2));
        Assert.assertEquals(baseline.getRecords().size(), 1);
    }

    /**
     * Tests that a baseline can have its records set in bulk via its setRecords method.
     */
    @Test
    public void testSetRecords() {
        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline(TEST_NAME);

        Set<ImaBlacklistRecord> records = new HashSet<>(Arrays.asList(
                new ImaBlacklistRecord(
                        "/test",
                        DigestTest.getTestSHA1Digest()
                ),
                new ImaBlacklistRecord(
                        "/test2",
                        DigestTest.getTestSHA1Digest()
                )
        ));

        baseline.setBaselineRecords(records);
        Assert.assertEquals(baseline.getRecords().size(), 2);

        for (ImaBlacklistRecord record : records) {
            Assert.assertTrue(baseline.getRecords().contains(record));
        }
    }

    /**
     * Tests that setRecords cannot accept null as its argument.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testSetRecordsNull() {
        new ImaBlacklistBaseline(TEST_NAME).setBaselineRecords(null);
    }

    /**
     * Tests that passing an empty Set into setRecords effectively clears all records
     * from a baseline.
     */
    @Test
    public void testSetRecordsEmpty() {
        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline(TEST_NAME);
        baseline.addToBaseline(ImaBlacklistRecordTest.getTestBlacklistRecord());
        Assert.assertEquals(baseline.getRecords().size(), 1);
        baseline.setBaselineRecords(Collections.emptySet());
        Assert.assertEquals(baseline.getRecords().size(), 0);
    }

    /**
     * Tests that the baseline's contains method correctly detects a match on a measurement record
     * that has the same path as a path-only record in the baseline.
     */
    @Test
    public void testContainsSingleRecordOnlyPath() {
        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline(TEST_NAME);
        ImaBlacklistRecord baselineRecord = new ImaBlacklistRecord(
                "/tmp/test"
        );
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(
                "/tmp/test", DigestTest.getTestSHA1Digest((byte) SEVEN)
        );
        baseline.addToBaseline(baselineRecord);
        BatchImaMatchStatus<ImaBlacklistRecord> matches = baseline.contains(
                Collections.singleton(measurementRecord), null, policyDisabledPartialPath
        );
        assertFoundMatch(matches, measurementRecord, baselineRecord, baseline);
    }

    /**
     * Tests that the baseline's contains method ignores a match on a measurement record
     * that has the same path, but a different hash, as a path and hash record in the baseline.
     */
    @Test
    public void testContainsSingleRecordOnlyPathMismatchedHash() {
        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline(TEST_NAME);
        ImaBlacklistRecord baselineRecord = new ImaBlacklistRecord(
                "/tmp/test", DigestTest.getTestSHA1Digest((byte) FIVE)
        );
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(
                "/tmp/test", DigestTest.getTestSHA1Digest((byte) SEVEN)
        );
        baseline.addToBaseline(baselineRecord);
        BatchImaMatchStatus<ImaBlacklistRecord> matches = baseline.contains(
                Collections.singleton(measurementRecord), null, policyDisabledPartialPath
        );
        assertFoundNoMatch(matches, measurementRecord, baselineRecord, baseline);
    }

    /**
     * Tests that the baseline's contains method correctly detects a match on a measurement record
     * that has (only) the same hash as the sole entry in the baseline.
     */
    @Test
    public void testContainsSingleRecordOnlyHash() {
        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline(TEST_NAME);
        ImaBlacklistRecord baselineRecord = new ImaBlacklistRecord(
                DigestTest.getTestSHA1Digest((byte) FIVE)
        );
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(
                "/tmp/test", DigestTest.getTestSHA1Digest((byte) FIVE)
        );
        baseline.addToBaseline(baselineRecord);
        BatchImaMatchStatus<ImaBlacklistRecord> matches = baseline.contains(
                Collections.singleton(measurementRecord), null, policyDisabledPartialPath
        );
        assertFoundMatch(matches, measurementRecord, baselineRecord, baseline);
    }

    /**
     * Tests that the baseline's contains method correctly detects a match on a measurement record
     * that has (only) the same hash as the sole entry in the baseline.
     */
    @Test
    public void testContainsSingleRecordOnlyHashMismatchedPath() {
        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline(TEST_NAME);
        ImaBlacklistRecord baselineRecord = new ImaBlacklistRecord(
                "/another/file", DigestTest.getTestSHA1Digest((byte) FIVE)
        );
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(
                "/tmp/test", DigestTest.getTestSHA1Digest((byte) FIVE)
        );
        baseline.addToBaseline(baselineRecord);
        BatchImaMatchStatus<ImaBlacklistRecord> matches = baseline.contains(
                Collections.singleton(measurementRecord), null, policyDisabledPartialPath
        );
        assertFoundNoMatch(matches, measurementRecord, baselineRecord, baseline);
    }

    /**
     * Tests that the baseline's contains method correctly detects a match on a measurement record
     * that has both the same path and hash as the sole entry in the baseline.
     */
    @Test
    public void testContainsSingleRecordBothPathAndHash() {
        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline(TEST_NAME);
        ImaBlacklistRecord baselineRecord = ImaBlacklistRecordTest.getTestBlacklistRecord();
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(
                baselineRecord.getPath(), baselineRecord.getHash()
        );
        baseline.addToBaseline(baselineRecord);
        BatchImaMatchStatus<ImaBlacklistRecord> matches = baseline.contains(
                Collections.singleton(measurementRecord), null, policyDisabledPartialPath
        );
        assertFoundMatch(matches, measurementRecord, baselineRecord, baseline);
    }

    /**
     * Tests that the baseline's contains method does not detect a match on a measurement
     * record that contains no information in common with the sole entry in the baseline.
     */
    @Test
    public void testContainsSingleRecordNoMatches() {
        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline(TEST_NAME);
        ImaBlacklistRecord baselineRecord = ImaBlacklistRecordTest.getTestBlacklistRecord();
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(
                "/some/other/path", DigestTest.getTestSHA1Digest((byte) THREE)
        );
        baseline.addToBaseline(baselineRecord);
        BatchImaMatchStatus<ImaBlacklistRecord> matches = baseline.contains(
                Collections.singleton(measurementRecord), null, policyDisabledPartialPath
        );
        Assert.assertTrue(matches.contains(measurementRecord));
        Assert.assertFalse(matches.foundMatch(measurementRecord));
        Assert.assertFalse(matches.foundMismatch(measurementRecord));
        Assert.assertTrue(matches.foundOnlyUnknown(measurementRecord));
        Assert.assertTrue(matches.getAppraisedMeasurementRecords().contains(measurementRecord));
        Assert.assertEquals(matches.getBaselineRecords(measurementRecord), Collections.EMPTY_SET);
        Assert.assertEquals(
                matches.getMatchingBaselineRecords(measurementRecord),
                Collections.EMPTY_SET
        );
        Assert.assertEquals(
                matches.getMismatchingBaselineRecords(measurementRecord),
                Collections.EMPTY_SET
        );
        Assert.assertEquals(
                matches.getIMAMatchStatuses(measurementRecord),
                Collections.singleton(
                        new IMAMatchStatus<>(measurementRecord, ReportMatchStatus.UNKNOWN, baseline)
                )
        );
    }

    /**
     * Tests that the baseline's contains method correctly detects a match on a measurement record
     * that (only) has the same 'partial path' as the sole entry in the baseline.
     */
    @Test
    public void testContainsSingleRecordMatchPartialPath() {
        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline(TEST_NAME);
        ImaBlacklistRecord baselineRecord = new ImaBlacklistRecord(
                "/tmp/test", DigestTest.getTestSHA1Digest((byte) THREE)
        );
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(
                "test", DigestTest.getTestSHA1Digest((byte) THREE)
        );
        baseline.addToBaseline(baselineRecord);
        BatchImaMatchStatus<ImaBlacklistRecord> matches = baseline.contains(
                Collections.singleton(measurementRecord), null, policyEnabledPartialPath
        );
        assertFoundMatch(matches, measurementRecord, baselineRecord, baseline);
    }

    /**
     * Tests that the baseline's contains method correctly detects a match on a measurement record
     * that (only) has the same 'partial path' as the sole entry in the baseline.
     */
    @Test
    public void testContainsSingleRecordMatchPartialPathMismatchedHash() {
        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline(TEST_NAME);
        ImaBlacklistRecord baselineRecord = new ImaBlacklistRecord(
                "/tmp/test", DigestTest.getTestSHA1Digest((byte) FIVE)
        );
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(
                "test", DigestTest.getTestSHA1Digest((byte) THREE)
        );
        baseline.addToBaseline(baselineRecord);
        BatchImaMatchStatus<ImaBlacklistRecord> matches = baseline.contains(
                Collections.singleton(measurementRecord), null, policyEnabledPartialPath
        );
        assertFoundNoMatch(matches, measurementRecord, baselineRecord, baseline);
    }

    /**
     * Tests that the baseline's contains method does not detect a match on a measurement record
     * that has both the same partial path as the sole entry in the baseline if the IMA policy
     * has disabled partial path checking.
     */
    @Test
    public void testContainsSingleRecordUnknownPartialPathDisabled() {
        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline(TEST_NAME);
        ImaBlacklistRecord baselineRecord = ImaBlacklistRecordTest.getTestBlacklistRecord();
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(
                "test", DigestTest.getTestSHA1Digest((byte) THREE)
        );
        baseline.addToBaseline(baselineRecord);
        BatchImaMatchStatus<ImaBlacklistRecord> matches = baseline.contains(
                Collections.singleton(measurementRecord), null, policyDisabledPartialPath
        );
        Assert.assertTrue(matches.contains(measurementRecord));
        Assert.assertFalse(matches.foundMatch(measurementRecord));
        Assert.assertFalse(matches.foundMismatch(measurementRecord));
        Assert.assertTrue(matches.foundOnlyUnknown(measurementRecord));
        Assert.assertTrue(matches.getAppraisedMeasurementRecords().contains(measurementRecord));
        Assert.assertEquals(matches.getBaselineRecords(measurementRecord), Collections.EMPTY_SET);
        Assert.assertEquals(
                matches.getMatchingBaselineRecords(measurementRecord),
                Collections.EMPTY_SET
        );
        Assert.assertEquals(
                matches.getMismatchingBaselineRecords(measurementRecord),
                Collections.EMPTY_SET
        );
        Assert.assertEquals(
                matches.getIMAMatchStatuses(measurementRecord),
                Collections.singleton(
                        new IMAMatchStatus<>(measurementRecord, ReportMatchStatus.UNKNOWN, baseline)
                )
        );
    }

    /**
     * This test matches five baseline records against five measurement records, with a variety
     * of matching elements as described throughout the test.  This test exercises that all
     * match information is populated correctly given a variety of circumstances (matches on
     * paths, partial paths, and hashes, as well as a measurement record that has no matching
     * baseline record.)
     */
    @SuppressWarnings("checkstyle:methodlength")
    @Test
    public void testContainsVariety() {
        // set up baseline and records

        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline(TEST_NAME);

        ImaBlacklistRecord baselineRecord1 = new ImaBlacklistRecord(
                "/tmp/test1", DigestTest.getTestSHA1Digest((byte) 1)
        );

        ImaBlacklistRecord baselineRecord2 = new ImaBlacklistRecord(
                "/tmp/test2"
        );

        ImaBlacklistRecord baselineRecord3 = new ImaBlacklistRecord(
                "/tmp/test3"
        );

        ImaBlacklistRecord baselineRecord4 = new ImaBlacklistRecord(
                DigestTest.getTestSHA1Digest((byte) FOUR)
        );

        Set<ImaBlacklistRecord> blacklistRecords = new HashSet<>(Arrays.asList(
                baselineRecord1, baselineRecord2, baselineRecord3, baselineRecord4
        ));

        baseline.setBaselineRecords(blacklistRecords);

        // set up measurement records

        // this should match baselineRecord2 via its path
        IMAMeasurementRecord measurementRecord2 = new IMAMeasurementRecord(
                "/tmp/test2", DigestTest.getTestSHA1Digest((byte) 0)
        );

        // this should match baselineRecord3 via its partial path
        IMAMeasurementRecord measurementRecord3 = new IMAMeasurementRecord(
                "test3", DigestTest.getTestSHA1Digest((byte) 0)
        );

        // this should match baselineRecord4 via its hash
        IMAMeasurementRecord measurementRecord4 = new IMAMeasurementRecord(
                "/different", DigestTest.getTestSHA1Digest((byte) FOUR)
        );

        // this won't match any blacklist record
        IMAMeasurementRecord measurementRecord5 = new IMAMeasurementRecord(
                "/another/one", DigestTest.getTestSHA1Digest((byte) FIVE)
        );

        List<IMAMeasurementRecord> measurementRecords = Arrays.asList(
                measurementRecord2, measurementRecord3, measurementRecord4, measurementRecord5
        );

        BatchImaMatchStatus<ImaBlacklistRecord> matches = baseline.contains(
                measurementRecords, null, policyEnabledPartialPath
        );

        // all measurement records should have match results
        for (IMAMeasurementRecord measurementRecord : measurementRecords) {
            Assert.assertTrue(matches.contains(measurementRecord));
        }

        // measurement records 2, 3, and 4 should have matches
        Assert.assertTrue(matches.foundMatch(measurementRecord2));
        Assert.assertTrue(matches.foundMatch(measurementRecord3));
        Assert.assertTrue(matches.foundMatch(measurementRecord4));
        Assert.assertFalse(matches.foundMatch(measurementRecord5));

        // none of the measurement records should have mismatches
        for (IMAMeasurementRecord measurementRecord : measurementRecords) {
            Assert.assertFalse(matches.foundMismatch(measurementRecord));
        }

        // only measurement record 5 should have only an 'unknown' status
        Assert.assertFalse(matches.foundOnlyUnknown(measurementRecord2));
        Assert.assertFalse(matches.foundOnlyUnknown(measurementRecord3));
        Assert.assertFalse(matches.foundOnlyUnknown(measurementRecord4));
        Assert.assertTrue(matches.foundOnlyUnknown(measurementRecord5));

        // all measurement records should be contained in the 'appraised' measurement records list
        // note: this check is equivalent to checking matches.contains(record) for each record
        Set<IMAMeasurementRecord> appraisedMeasRecords = matches.getAppraisedMeasurementRecords();
        for (IMAMeasurementRecord measurementRecord : measurementRecords) {
            Assert.assertTrue(appraisedMeasRecords.contains(measurementRecord));
        }

        // check that the proper baseline record, if applicable, is associated to each measurement
        Assert.assertEquals(
                matches.getBaselineRecords(measurementRecord2),
                Collections.singleton(baselineRecord2)
        );
        Assert.assertEquals(
                matches.getBaselineRecords(measurementRecord3),
                Collections.singleton(baselineRecord3)
        );
        Assert.assertEquals(
                matches.getBaselineRecords(measurementRecord4),
                Collections.singleton(baselineRecord4)
        );
        Assert.assertEquals(
                matches.getBaselineRecords(measurementRecord5),
                Collections.EMPTY_SET
        );

        // make sure that the proper baseline record, if applicable, is associated as a match
        // to each measurement
        Assert.assertEquals(
                matches.getMatchingBaselineRecords(measurementRecord2),
                Collections.singleton(baselineRecord2)
        );
        Assert.assertEquals(
                matches.getMatchingBaselineRecords(measurementRecord3),
                Collections.singleton(baselineRecord3)
        );
        Assert.assertEquals(
                matches.getMatchingBaselineRecords(measurementRecord4),
                Collections.singleton(baselineRecord4)
        );
        Assert.assertEquals(
                matches.getMatchingBaselineRecords(measurementRecord5),
                Collections.EMPTY_SET
        );

        // make sure that no baseline records are associated as mismatches to each measurement
        Assert.assertEquals(
                matches.getMismatchingBaselineRecords(measurementRecord2),
                Collections.EMPTY_SET
        );
        Assert.assertEquals(
                matches.getMismatchingBaselineRecords(measurementRecord3),
                Collections.EMPTY_SET
        );
        Assert.assertEquals(
                matches.getMismatchingBaselineRecords(measurementRecord4),
                Collections.EMPTY_SET
        );
        Assert.assertEquals(
                matches.getMismatchingBaselineRecords(measurementRecord5),
                Collections.EMPTY_SET
        );

        // make sure that each measurement record's match status has the expected information
        Assert.assertEquals(
                matches.getIMAMatchStatuses(measurementRecord2),
                Collections.singleton(new IMAMatchStatus<>(
                        measurementRecord2, ReportMatchStatus.MATCH, baselineRecord2, baseline
                ))
        );

        Assert.assertEquals(
                matches.getIMAMatchStatuses(measurementRecord3),
                Collections.singleton(new IMAMatchStatus<>(
                        measurementRecord3, ReportMatchStatus.MATCH, baselineRecord3, baseline
                ))
        );

        Assert.assertEquals(
                matches.getIMAMatchStatuses(measurementRecord4),
                Collections.singleton(new IMAMatchStatus<>(
                        measurementRecord4, ReportMatchStatus.MATCH, baselineRecord4, baseline
                ))
        );

        Assert.assertEquals(
                matches.getIMAMatchStatuses(measurementRecord5),
                Collections.singleton(new IMAMatchStatus<ImaBlacklistRecord>(
                        measurementRecord5, ReportMatchStatus.UNKNOWN, Collections.emptySet(),
                        baseline
                ))
        );
    }

    /**
     * Simple test to ensure ImaBlacklistBaselines can be persisted and retrieved.
     */
    @Test
    public void testPersistBlacklistRecords() {
        BaselineManager baselineManager = new DBBaselineManager(sessionFactory);

        ImaBlacklistRecord baselineRecord1 = new ImaBlacklistRecord(
                "/tmp/test1", DigestTest.getTestSHA1Digest((byte) 1), "1"
        );

        ImaBlacklistRecord baselineRecord2 = new ImaBlacklistRecord(
                "/tmp/test2"
        );

        ImaBlacklistRecord baselineRecord3 = new ImaBlacklistRecord(
                DigestTest.getTestSHA1Digest((byte) THREE)
        );

        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline(TEST_NAME);
        baseline.addToBaseline(baselineRecord1);
        baseline.addToBaseline(baselineRecord2);
        baseline.addToBaseline(baselineRecord3);

        ImaBlacklistBaseline savedBaseline =
                (ImaBlacklistBaseline) baselineManager.saveBaseline(baseline);
        Assert.assertEquals(savedBaseline, baseline);

        ImaBlacklistBaseline retrievedBaseline =
                (ImaBlacklistBaseline) baselineManager.getBaseline(baseline.getName());

        Assert.assertEquals(retrievedBaseline, baseline);
        Assert.assertEquals(retrievedBaseline.getRecords().size(), THREE);
        Assert.assertTrue(retrievedBaseline.getRecords().contains(baselineRecord1));
        Assert.assertTrue(retrievedBaseline.getRecords().contains(baselineRecord2));
        Assert.assertTrue(retrievedBaseline.getRecords().contains(baselineRecord3));
    }

    private void assertFoundMatch(
            final BatchImaMatchStatus<ImaBlacklistRecord> matches,
            final IMAMeasurementRecord measurementRecord,
            final ImaBlacklistRecord baselineRecord,
            final ImaBaseline baseline) {
        Assert.assertTrue(matches.contains(measurementRecord));
        Assert.assertTrue(matches.foundMatch(measurementRecord));
        Assert.assertFalse(matches.foundMismatch(measurementRecord));
        Assert.assertFalse(matches.foundOnlyUnknown(measurementRecord));
        Assert.assertTrue(matches.getAppraisedMeasurementRecords().contains(measurementRecord));
        Assert.assertEquals(
                matches.getBaselineRecords(measurementRecord),
                Collections.singleton(baselineRecord)
        );
        Assert.assertEquals(
                matches.getMatchingBaselineRecords(measurementRecord),
                Collections.singleton(baselineRecord)
        );
        Assert.assertEquals(
                matches.getMismatchingBaselineRecords(measurementRecord),
                Collections.EMPTY_SET
        );
        Assert.assertEquals(
                matches.getIMAMatchStatuses(measurementRecord),
                Collections.singleton(new IMAMatchStatus<>(
                        measurementRecord, ReportMatchStatus.MATCH, baselineRecord, baseline
                        ))
        );
    }

    private void assertFoundNoMatch(
            final BatchImaMatchStatus<ImaBlacklistRecord> matches,
            final IMAMeasurementRecord measurementRecord,
            final ImaBlacklistRecord baselineRecord,
            final ImaBaseline baseline) {
        Assert.assertTrue(matches.contains(measurementRecord));
        Assert.assertFalse(matches.foundMatch(measurementRecord));
        Assert.assertFalse(matches.foundMismatch(measurementRecord));
        Assert.assertTrue(matches.foundOnlyUnknown(measurementRecord));
        Assert.assertTrue(matches.getAppraisedMeasurementRecords().contains(measurementRecord));
        Assert.assertEquals(
                matches.getBaselineRecords(measurementRecord),
                Collections.EMPTY_SET
        );
        Assert.assertEquals(
                matches.getMatchingBaselineRecords(measurementRecord),
                Collections.EMPTY_SET
        );
        Assert.assertEquals(
                matches.getMismatchingBaselineRecords(measurementRecord),
                Collections.EMPTY_SET
        );
        Assert.assertEquals(
                matches.getIMAMatchStatuses(measurementRecord),
                Collections.singleton(new IMAMatchStatus<>(
                        measurementRecord, ReportMatchStatus.UNKNOWN, baseline
                ))
        );
    }
}
