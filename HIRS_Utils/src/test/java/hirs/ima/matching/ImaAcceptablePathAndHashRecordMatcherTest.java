package hirs.ima.matching;

import hirs.data.persist.Digest;
import hirs.data.persist.DigestTest;
import hirs.data.persist.baseline.IMABaselineRecord;
import hirs.data.persist.IMAMeasurementRecord;
import hirs.data.persist.baseline.ImaAcceptableRecordBaseline;
import hirs.data.persist.enums.ReportMatchStatus;
import hirs.data.persist.baseline.SimpleImaBaseline;
import hirs.data.persist.SimpleImaBaselineTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests ImaAcceptablePathAndHashRecordMatcher.  These are very basic tests of its functionality;
 * more complete tests for contains() as used operationally by baselines that test various
 * permutations of parameters are located in SimpleImaBaselineTest, BroadRepoImaBaselineTest,
 * TargetedRepoImaBaselineTest, ImaBlacklistBaselineTest, and ImaIgnoreSetBaselineTest.
 */
public class ImaAcceptablePathAndHashRecordMatcherTest {
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

    private static final Digest ONES = DigestTest.getTestSHA1Digest();

    /**
     * Tests that contains functions if no records are given.
     */
    @Test
    public void testContainsEmpty() {
        SimpleImaBaseline baseline = getTestSimpleImaBaseline();
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(FILE_1, HASH_1);
        Assert.assertEquals(
                new ImaAcceptablePathAndHashRecordMatcher(
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
                new ImaAcceptablePathAndHashRecordMatcher(
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
                new ImaAcceptablePathAndHashRecordMatcher(
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
                new ImaAcceptablePathAndHashRecordMatcher(
                        Collections.singletonList(baselineRecord),
                        SimpleImaBaselineTest.getTestImaPolicy(false),
                        baseline
                ).contains(measurementRecord),
                new IMAMatchStatus<>(
                        measurementRecord, ReportMatchStatus.MATCH, baselineRecord, baseline
                )
        );
    }

    /**
     * Tests that contains correctly matches equivalent paths for real-world
     * examples that have been seen.
     */
    @Test
    public void testContainsExperiencedEquivalentPaths() {
        List<List<String>> pairs = Arrays.asList(
                Arrays.asList("/usr/sbin/dhclient", "/sbin/dhclient"),
                Arrays.asList("/usr/sbin/sysctl", "/sbin/sysctl"),
                Arrays.asList("/usr/sbin/swapon", "/sbin/swapon"),
                Arrays.asList("/sbin/audispd", "/usr/sbin/audispd"),
                Arrays.asList("/usr/sbin/sysctl", "/sbin/sysctl"),
                Arrays.asList("/sbin/ldconfig", "/usr/sbin/ldconfig"),
                Arrays.asList("/sbin/kexec", "/usr/sbin/kexec"),
                Arrays.asList("/usr/sbin/ip", "/sbin/ip"),
                Arrays.asList("/usr/bin/bash", "/bin/bash")
        );

        for (List<String> pair : pairs) {
            testEquivalentNames(pair.get(0), pair.get(1));
        }
    }

    /**
     * Tests that contains correctly matches equivalent paths.
     */
    @Test
    public void testContainsExhaustiveEquivalentPaths() {
        List<List<String>> pairs = Arrays.asList(
                Arrays.asList("/bin/foofile", "/usr/bin/foofile"),
                Arrays.asList("/lib/foofile", "/usr/lib/foofile"),
                Arrays.asList("/lib64/foofile", "/usr/lib64/foofile"),
                Arrays.asList("/usr/bin/foofile", "/usr/sbin/foofile"),
                Arrays.asList("/sbin/foofile", "/usr/sbin/foofile")
        );

        for (int i = 0; i < pairs.size(); i++) {
            List<String> equivPair = pairs.get(i);

            testEquivalentNames(equivPair.get(0), equivPair.get(1));
            testEquivalentNames(equivPair.get(1), equivPair.get(0));
        }
    }

    private void testEquivalentNames(final String measuredFilename, final String baselineFilename) {
        final IMAMeasurementRecord measurementRecord =
                new IMAMeasurementRecord(measuredFilename, ONES);
        final IMABaselineRecord baselineRecord = new IMABaselineRecord(baselineFilename, ONES);
        final ImaAcceptableRecordBaseline baseline = getTestSimpleImaBaseline();

        Assert.assertEquals(
                new ImaAcceptablePathAndHashRecordMatcher(
                        Collections.singleton(baselineRecord),
                        SimpleImaBaselineTest.getTestImaPolicy(false),
                        baseline
                ).contains(measurementRecord),
                new IMAMatchStatus<>(
                        measurementRecord,
                        ReportMatchStatus.MATCH,
                        baselineRecord,
                        baseline
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
                new ImaAcceptablePathAndHashRecordMatcher(
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
