package hirs.ima;

import hirs.data.persist.Digest;
import hirs.data.persist.IMABaselineRecord;
import hirs.data.persist.IMAMeasurementRecord;
import hirs.data.persist.ReportMatchStatus;
import hirs.data.persist.SimpleImaBaseline;
import hirs.ima.matching.IMAMatchStatus;
import hirs.ima.matching.ImaAcceptableRecordMatcher;
import org.testng.Assert;
import org.testng.annotations.Test;
import hirs.data.persist.SimpleImaBaselineTest;

import java.util.Arrays;
import java.util.Collections;

/**
 * Tests ImaAcceptableRecordMatcher.  These are very basic tests of its functionality;
 * more complete tests for contains() as used operationally by baselines that test various
 * permutations of parameters are located in SimpleImaBaselineTest, BroadRepoImaBaselineTest,
 * TargetedRepoImaBaselineTest, ImaBlacklistBaselineTest, and ImaIgnoreSetBaselineTest.
 */
public class ImaAcceptableRecordMatcherTest {
    private static final String FILE_1 = "/bin/ls";
    private static final String PARTIAL_FILE_1 = "ls";
    private static final Digest HASH_1 =
            SimpleImaBaselineTest.getDigest("33333c2f7f3003d2e4baddc46ed4763a49543333");

    private static final String USR_LIB64_LD_FILE = "/usr/lib64/ld-2.17.so";
    private static final Digest USR_LIB64_LD_HASH =
            SimpleImaBaselineTest.getDigest("44444c2f7f3003d2e4baddc46ed4763a49543333");
    private static final String LIB64_LD_FILE = "/lib64/ld-2.17.so";
    private static final Digest LIB64_LD_HASH =
            SimpleImaBaselineTest.getDigest("55555c2f7f3003d2e4baddc46ed4763a49543333");


    /**
     * Tests that contains functions if no records are given.
     */
    @Test
    public void testContainsEmpty() {
        SimpleImaBaseline baseline = getTestSimpleImaBaseline();
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(FILE_1, HASH_1);
        Assert.assertEquals(
                new ImaAcceptableRecordMatcher(
                        Collections.emptyList(),
                        SimpleImaBaselineTest.getTestImaPolicy(false),
                        baseline).contains(measurementRecord),
                new IMAMatchStatus(measurementRecord, ReportMatchStatus.UNKNOWN, baseline)
        );
    }

    /**
     * Tests that contains functions if a matching record is given.
     */
    @Test
    public void testContains() {
        SimpleImaBaseline baseline = getTestSimpleImaBaseline();
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(FILE_1, HASH_1);
        IMABaselineRecord baselineRecord = new IMABaselineRecord(FILE_1, HASH_1);
        Assert.assertEquals(
                new ImaAcceptableRecordMatcher(
                        Collections.singletonList(baselineRecord),
                        SimpleImaBaselineTest.getTestImaPolicy(false),
                        baseline).contains(measurementRecord),
                new IMAMatchStatus<>(
                        measurementRecord, ReportMatchStatus.MATCH, baselineRecord, baseline
                )
        );
    }

    /**
     * Tests that contains correctly matches partial paths.
     */
    @Test
    public void testContainsPartialPaths() {
        SimpleImaBaseline baseline = getTestSimpleImaBaseline();
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(PARTIAL_FILE_1, HASH_1);
        IMABaselineRecord baselineRecord = new IMABaselineRecord(FILE_1, HASH_1);
        Assert.assertEquals(
                new ImaAcceptableRecordMatcher(
                        Collections.singletonList(baselineRecord),
                        SimpleImaBaselineTest.getTestImaPolicy(true),
                        baseline).contains(measurementRecord),
                new IMAMatchStatus<>(
                        measurementRecord, ReportMatchStatus.MATCH, baselineRecord, baseline
                )
        );
    }

    /**
     * Tests that contains correctly matches equivalent paths.
     */
    @Test
    public void testContainsEquivalentPaths() {
        SimpleImaBaseline baseline = getTestSimpleImaBaseline();
        IMABaselineRecord baselineRecord =
                new IMABaselineRecord(USR_LIB64_LD_FILE, USR_LIB64_LD_HASH);
        IMAMeasurementRecord measurementRecord =
                new IMAMeasurementRecord(LIB64_LD_FILE, USR_LIB64_LD_HASH);
        Assert.assertEquals(
                new ImaAcceptableRecordMatcher(
                        Collections.singletonList(baselineRecord),
                        SimpleImaBaselineTest.getTestImaPolicy(false),
                        baseline).contains(measurementRecord),
                new IMAMatchStatus<>(
                        measurementRecord, ReportMatchStatus.MATCH, baselineRecord, baseline
                )
        );
    }

    /**
     * This tests a case where a baseline includes a file at both /lib64 and /usr/lib64
     * with distinct hashes, and a report contains an entry for a file at /usr/lib64 whose hash
     * matches a record in the baseline at /lib64.  When repo sync measures packages, including
     * initramfs files, it does not assume a filesystem and therefore does not have knowledge of
     * any existing symbolic links.  IMA Appraisal should be able to know that a file measured
     * at /lib64 and accessed via /usr/lib64 should be considered a match if their hashes are
     * identical.  A use case that exposes this behavior is booting a system and measuring its
     * initramfs.
     */
    @Test
    public void testInitRamFsCase() {
        SimpleImaBaseline baseline = getTestSimpleImaBaseline();
        IMABaselineRecord libBaselineRecord = new IMABaselineRecord(LIB64_LD_FILE, LIB64_LD_HASH);
        IMABaselineRecord usrLibBaselineRecord =
                new IMABaselineRecord(USR_LIB64_LD_FILE, USR_LIB64_LD_HASH);
        IMAMeasurementRecord measurementRecord =
                new IMAMeasurementRecord(USR_LIB64_LD_FILE, LIB64_LD_HASH);
        Assert.assertEquals(
                new ImaAcceptableRecordMatcher(
                        Arrays.asList(libBaselineRecord, usrLibBaselineRecord),
                        SimpleImaBaselineTest.getTestImaPolicy(false),
                        baseline).contains(measurementRecord),
                new IMAMatchStatus<>(
                        measurementRecord, ReportMatchStatus.MATCH, libBaselineRecord, baseline
                )
        );
    }

    private static SimpleImaBaseline getTestSimpleImaBaseline() {
        return new SimpleImaBaseline("Test IMA Baseline");
    }
}
