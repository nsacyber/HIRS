package hirs.data.persist;

import hirs.data.persist.enums.DigestAlgorithm;
import hirs.data.persist.baseline.ImaBaseline;
import hirs.data.persist.baseline.IMABaselineRecord;
import hirs.data.persist.baseline.SimpleImaBaseline;
import hirs.data.persist.baseline.Baseline;
import hirs.data.persist.enums.ReportMatchStatus;
import hirs.ima.matching.BatchImaMatchStatus;
import hirs.ima.matching.IMAMatchStatus;
import hirs.persist.BaselineManager;
import hirs.persist.DBBaselineManager;
import hirs.persist.DbImaBaselineRecordManager;
import hirs.persist.ImaBaselineRecordManager;

import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import hirs.ima.IMATestUtil;

import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * SimpleImaBaselineTest is a unit test class for the SimpleImaBaseline class.
 */
public class SimpleImaBaselineTest extends SpringPersistenceTest {
    private static final Logger LOGGER = LogManager.getLogger(SimpleImaBaselineTest.class);
    private ImaBaselineRecordManager recordManager;

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an
     * in-memory database that is used for testing.
     */
    @BeforeClass
    public final void setup() {
        LOGGER.debug("retrieving session factory");
        recordManager = new DbImaBaselineRecordManager(sessionFactory);
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterClass
    public final void tearDown() {
        LOGGER.debug("closing session factory");
    }

    /**
     * Resets the test state to a known good state. This currently only resets
     * the database by removing all <code>Baseline</code> objects.
     */
    @AfterMethod
    public final void resetTestState() {
        LOGGER.debug("reset test state");
        LOGGER.debug("deleting all baselines");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final List<?> baselines
                = session.createCriteria(Baseline.class).list();
        for (Object o : baselines) {
            LOGGER.debug("deleting baseline: {}", o);
            session.delete(o);
        }
        LOGGER.debug("all baselines removed");
        session.getTransaction().commit();
    }

    /**
     * Tests instantiation of ImaBaseline object.
     */
    @Test
    public final void imaBaseline() {

        ImaBaseline imaBaseline = new SimpleImaBaseline("TestBaseline");
        Assert.assertNotNull(imaBaseline);
    }

    /**
     * Tests that ImaBaseline constructor throws a NullPointerException with
     * null name.
     *
     * @throws ParseException
     *             if error generated parsing date
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void imaBaselineNullName() throws ParseException {
        new SimpleImaBaseline(null);
    }

    /**
     * Tests that the ImaBaseline constructor with the URL correctly sets it.
     *
     * @throws MalformedURLException if the given URL is malformed
     */
    @Test
    public final void imaBaselineWithRepoUrl() throws MalformedURLException {
        Assert.assertEquals(
                new SimpleImaBaseline("TestBaseline", IMATestUtil.getYumRepoURL()).getYumRepoURL(),
                IMATestUtil.getYumRepoURL()
        );
    }

    /**
     * Tests that a Yum repo URL can be set on an ImaBaseline.
     *
     * @throws MalformedURLException if the given URL is malformed
     */
    @Test
    public final void setRepoUrl() throws MalformedURLException {
        SimpleImaBaseline baseline = new SimpleImaBaseline("TestBaseline");
        Assert.assertNull(baseline.getYumRepoURL());
        baseline.setYumRepoURL(IMATestUtil.getYumRepoURL());
        Assert.assertEquals(baseline.getYumRepoURL(), IMATestUtil.getYumRepoURL());
    }

    /**
     * Tests that a Yum repo URL can be removed from an ImaBaseline.
     *
     * @throws MalformedURLException if the given URL is malformed
     */
    @Test
    public final void clearRepoUrl() throws MalformedURLException {
        SimpleImaBaseline baseline = new SimpleImaBaseline("TestBaseline",
                IMATestUtil.getYumRepoURL());
        Assert.assertEquals(baseline.getYumRepoURL(), IMATestUtil.getYumRepoURL());
        baseline.setYumRepoURL(null);
        Assert.assertNull(baseline.getYumRepoURL());
    }

    /**
     * Tests that the constructor correctly initializes the date to the current
     * date.
     */
    @Test
    public final void initializeDate() {
        Date beforeDate = new Date();
        Date setDate = new SimpleImaBaseline("Fake Name").getDate();
        Date afterDate = new Date();
        Assert.assertTrue(setDate.after(beforeDate)
                || setDate.equals(beforeDate));
        Assert.assertTrue(setDate.before(afterDate)
                || setDate.equals(afterDate));
    }

    /**
     * Tests adding IMA records to baseline.
     *
     * @throws ParseException
     *             if error generated parsing date
     */
    @Test
    public final void addToBaseline() throws ParseException {
        SimpleImaBaseline imaBaseline = new SimpleImaBaseline("TestBaseline");
        IMABaselineRecord imaRecord;
        imaRecord = createTestIMARecord("imaTestRecord");
        imaBaseline.addToBaseline(imaRecord);
        Set<IMABaselineRecord> imaRecords
                = imaBaseline.getBaselineRecords();
        Assert.assertNotNull(imaRecords);
        Assert.assertTrue(imaRecords.contains(imaRecord));
        Assert.assertEquals(imaRecords.size(), 1);
    }

    /**
     * Tests that addToBaseline() handles duplicate IMA records.
     *
     * @throws ParseException
     *             if error generated parsing date
     */
    @Test
    public final void addToBaselineDuplicateRecords() throws ParseException {
        SimpleImaBaseline imaBaseline = new SimpleImaBaseline("TestBaseline");
        IMABaselineRecord imaRecord;
        imaRecord = createTestIMARecord("imaTestRecord");
        imaBaseline.addToBaseline(imaRecord);
        imaBaseline.addToBaseline(imaRecord);
    }

    /**
     * Tests that contains returns MATCH when an
     * <code>IMAMeasurementRecord</code> matches the full path and hash of an
     * <code>IMABaselineRecord</code> contained in an <code>ImaBaseline</code>.
     */
    @Test
    public final void contains() {
        final SimpleImaBaseline baseline = new SimpleImaBaseline("TestBaseline");
        final String binGradle = "/usr/bin/gradle";
        final Digest binGradleHash =
                getDigest("33333c2f7f3003d2e4baddc46ed4763a49543333");
        final IMABaselineRecord baselineRecord =
                new IMABaselineRecord(binGradle, binGradleHash);
        baseline.addToBaseline(baselineRecord);
        IMAMeasurementRecord measurementRecord =
                new IMAMeasurementRecord(binGradle, binGradleHash);
        Assert.assertEquals(
                baselineContains(baseline, measurementRecord, getTestImaPolicy(true)),
                new IMAMatchStatus<>(
                        measurementRecord, ReportMatchStatus.MATCH, baselineRecord, baseline
                )
        );
        Assert.assertEquals(baselineContains(
                baseline, measurementRecord, getTestImaPolicy(false)),
                new IMAMatchStatus<>(
                        measurementRecord, ReportMatchStatus.MATCH, baselineRecord, baseline
                )
        );
    }

    private IMAMatchStatus<IMABaselineRecord> baselineContains(
            final SimpleImaBaseline baseline,
            final IMAMeasurementRecord record,
            final IMAPolicy imaPolicy) {
        return baseline.contains(
                Collections.singletonList(record), recordManager, imaPolicy
        ).getIMAMatchStatuses(record).iterator().next();
    }

    private BatchImaMatchStatus<IMABaselineRecord> baselineContainsHashes(
            final SimpleImaBaseline baseline,
            final IMAMeasurementRecord record,
            final IMAPolicy imaPolicy) {
        return baseline.containsHashes(
                Collections.singletonList(record), recordManager, imaPolicy
        );
    }

    /**
     * Create a test IMAPolicy object.
     *
     * @param partialPathEnable whether to enable partial path support
     * @return a test IMAPolicy object
     */
    public static IMAPolicy getTestImaPolicy(final boolean partialPathEnable) {
        IMAPolicy imaPolicy = new IMAPolicy("Test IMA Policy");
        imaPolicy.setPartialPathEnable(partialPathEnable);
        return imaPolicy;
    }

    /**
     * Tests that the partial path comparison works when the measurement record
     * is a partial path, the baseline record is a full path, and partial paths
     * are enabled. This creates two entries in the baseline for a gradle
     * binary. One is under /usr/bin and the other is under a user's home
     * directory. Then the contains method is called to verify that a record
     * with path set to "gradle" will match with a good hash for either one of
     * the expected hash values.
     */
    @Test
    public final void containsPartialMeasurementRecord() {
        final SimpleImaBaseline baseline = new SimpleImaBaseline("TestBaseline");
        final String binGradle = "/usr/bin/gradle";
        final String userGradle = "/home/foo/bin/gradle";
        final Digest binGradleHash = getDigest("33333c2f7f3003d2e4baddc46ed4763a49543333");
        final Digest userGradleHash = getDigest("44443c2f7f3003d2e4baddc46ed4763a49544444");
        final IMABaselineRecord binRecord = new IMABaselineRecord(binGradle, binGradleHash);
        final IMABaselineRecord userRecord = new IMABaselineRecord(userGradle, userGradleHash);
        baseline.addToBaseline(binRecord);
        baseline.addToBaseline(userRecord);
        final String gradle = "gradle";

        IMAMeasurementRecord partialBinRecord = new IMAMeasurementRecord(gradle, binGradleHash);
        Assert.assertEquals(
                baselineContains(baseline, partialBinRecord, getTestImaPolicy(true)),
                new IMAMatchStatus<>(
                        partialBinRecord, ReportMatchStatus.MATCH, binRecord, baseline
                )
        );

        IMAMeasurementRecord partialUserRecord = new IMAMeasurementRecord(gradle, userGradleHash);
        Assert.assertEquals(
                baselineContains(baseline, partialUserRecord, getTestImaPolicy(true)),
                new IMAMatchStatus<>(
                        partialUserRecord, ReportMatchStatus.MATCH, userRecord, baseline
                )
        );
    }

    /**
     * Tests that the partial path comparison first fails when partial paths are
     * disabled, then works when they are enabled. Both pieces are tested
     * elsewhere, but this tests shows that toggling the boolean causes the
     * baseline's internal maps to be updated properly.
     */
    @Test
    public final void containsTogglePartial() {
        final SimpleImaBaseline baseline = new SimpleImaBaseline("TestBaseline");
        final String binGradle = "/usr/bin/gradle";
        final Digest binGradleHash = getDigest("33333c2f7f3003d2e4baddc46ed4763a49543333");
        final IMABaselineRecord binRecord = new IMABaselineRecord(binGradle, binGradleHash);
        baseline.addToBaseline(binRecord);
        final String gradle = "gradle";

        IMAMeasurementRecord partialBinRecord = new IMAMeasurementRecord(gradle, binGradleHash);
        Assert.assertEquals(
                baselineContains(baseline, partialBinRecord, getTestImaPolicy(false)),
                new IMAMatchStatus<>(
                        partialBinRecord, ReportMatchStatus.UNKNOWN, baseline
                )
        );
        Assert.assertEquals(
                baselineContains(baseline, partialBinRecord, getTestImaPolicy(true)),
                new IMAMatchStatus<>(
                        partialBinRecord, ReportMatchStatus.MATCH, binRecord, baseline
                )
        );
    }

    /**
     * Tests that the partial path comparison fails when the measurement record
     * is a partial path, the baseline record is a full path, and partial paths
     * are disabled. This creates two entries in the baseline for a gradle
     * binary. One is under /usr/bin and the other is under a user's home
     * directory. Then the contains method is called to verify that a record
     * with path set to "gradle" will return unknown even with a good hash for
     * both of the expected hash values
     */
    @Test
    public final void containsPartialMeasurementRecordDisabled() {
        final SimpleImaBaseline baseline = new SimpleImaBaseline("TestBaseline");
        final String binGradle = "/usr/bin/gradle";
        final String userGradle = "/home/foo/bin/gradle";
        final Digest binGradleHash = getDigest("33333c2f7f3003d2e4baddc46ed4763a49543333");
        final Digest userGradleHash = getDigest("44443c2f7f3003d2e4baddc46ed4763a49544444");
        final IMABaselineRecord binRecord = new IMABaselineRecord(binGradle, binGradleHash);
        final IMABaselineRecord userRecord = new IMABaselineRecord(userGradle, userGradleHash);
        baseline.addToBaseline(binRecord);
        baseline.addToBaseline(userRecord);
        final String gradle = "gradle";
        IMAMeasurementRecord partialBinRecord =
                new IMAMeasurementRecord(gradle, binGradleHash);
        Assert.assertEquals(baselineContains(baseline, partialBinRecord, getTestImaPolicy(false)),
                new IMAMatchStatus<>(partialBinRecord, ReportMatchStatus.UNKNOWN, baseline));
        IMAMeasurementRecord partialUserRecord =
                new IMAMeasurementRecord(gradle, userGradleHash);
        Assert.assertEquals(baselineContains(baseline, partialUserRecord, getTestImaPolicy(false)),
                new IMAMatchStatus<>(partialUserRecord, ReportMatchStatus.UNKNOWN, baseline));
    }

    /**
     * Tests that the partial path comparison works when the measurement record
     * is a full path, the baseline record is a partial path, and partial paths
     * are enabled. This creates two entries in the baseline for a gradle
     * binary, both under the partial path filename "gradle". Then the contains
     * method is called to verify that two different records with full paths
     * will both match with a good hash.
     */
    @Test
    public final void containsPartialBaselineRecord() {
        final SimpleImaBaseline baseline = new SimpleImaBaseline("TestBaseline");
        final String gradle = "gradle";
        final Digest binGradleHash = getDigest("33333c2f7f3003d2e4baddc46ed4763a49543333");
        final Digest userGradleHash = getDigest("44443c2f7f3003d2e4baddc46ed4763a49544444");
        final IMABaselineRecord binRecord = new IMABaselineRecord(gradle, binGradleHash);
        final IMABaselineRecord userRecord = new IMABaselineRecord(gradle, userGradleHash);
        baseline.addToBaseline(binRecord);
        baseline.addToBaseline(userRecord);
        final String binGradle = "/usr/bin/gradle";
        final String userGradle = "/home/foo/bin/gradle";
        IMAMeasurementRecord partialBinRecord =
                new IMAMeasurementRecord(binGradle, binGradleHash);
        Assert.assertEquals(baselineContains(baseline, partialBinRecord, getTestImaPolicy(true)),
                new IMAMatchStatus<>(
                        partialBinRecord, ReportMatchStatus.MATCH, binRecord, baseline
                )
        );
        IMAMeasurementRecord partialUserRecord =
                new IMAMeasurementRecord(userGradle, userGradleHash);
        Assert.assertEquals(baselineContains(baseline, partialUserRecord, getTestImaPolicy(true)),
                new IMAMatchStatus<>(
                        partialUserRecord, ReportMatchStatus.MATCH, userRecord, baseline
                )
        );
    }

    /**
     * Tests that the partial path comparison is not done when the
     * partialPathEnable setting is false. This creates two entries in the
     * baseline for a gradle binary. One is under /usr/bin and the other is
     * under a user's home directory. Then the contains method is called to
     * verify that a record with a partial path set to "gradle" will not match
     * either one of the expected hash values.
     */
    @Test
    public final void containsPartialBaselineRecordDisabled() {
        final SimpleImaBaseline baseline = new SimpleImaBaseline("TestBaseline");
        final String binGradle = "/usr/bin/gradle";
        final String userGradle = "/home/foo/bin/gradle";
        final Digest binGradleHash = getDigest("33333c2f7f3003d2e4baddc46ed4763a49543333");
        final Digest userGradleHash = getDigest("44443c2f7f3003d2e4baddc46ed4763a49544444");
        final IMABaselineRecord binRecord = new IMABaselineRecord(binGradle, binGradleHash);
        final IMABaselineRecord userRecord = new IMABaselineRecord(userGradle, userGradleHash);
        baseline.addToBaseline(binRecord);
        baseline.addToBaseline(userRecord);
        final String gradle = "gradle";
        IMAMeasurementRecord record =
                new IMAMeasurementRecord(gradle, binGradleHash);
        Assert.assertEquals(baselineContains(baseline, record, getTestImaPolicy(false)),
                new IMAMatchStatus<>(record, ReportMatchStatus.UNKNOWN, baseline));
        record = new IMAMeasurementRecord(gradle, userGradleHash);
        Assert.assertEquals(baselineContains(baseline, record, getTestImaPolicy(false)),
                new IMAMatchStatus<>(record, ReportMatchStatus.UNKNOWN, baseline));
    }

    /**
     * Tests that the partial path comparison works for a mismatch. This creates
     * two entries in the baseline for a gradle binary. One is under /usr/bin
     * and the other is under a user's home directory. Then the contains method
     * is called to verify that a record with path set to "gradle" will cause a
     * mismatch for an unknown hash value as compared to the other two hash
     * values.
     */
    @Test
    public final void containsPartialMismatch() {
        final SimpleImaBaseline baseline = new SimpleImaBaseline("TestBaseline");
        final String binGradle = "/usr/bin/gradle";
        final String userGradle = "/home/foo/bin/gradle";
        final Digest binGradleHash = getDigest("33333c2f7f3003d2e4baddc46ed4763a49543333");
        final Digest userGradleHash = getDigest("44443c2f7f3003d2e4baddc46ed4763a49544444");
        final Digest unknownGradleHash = getDigest("66663c2f7f3003d2e4baddc46ed4763a49546666");
        final IMABaselineRecord binRecord = new IMABaselineRecord(binGradle, binGradleHash);
        final IMABaselineRecord userRecord = new IMABaselineRecord(userGradle, userGradleHash);
        baseline.addToBaseline(binRecord);
        baseline.addToBaseline(userRecord);
        final String gradle = "gradle";
        final IMAMeasurementRecord record = new IMAMeasurementRecord(gradle, unknownGradleHash);
        final Set<IMABaselineRecord> records = new HashSet<>();
        records.add(binRecord);
        records.add(userRecord);
        final IMAMatchStatus expected =
                new IMAMatchStatus<>(record, ReportMatchStatus.MISMATCH, records, baseline);
        IMAMatchStatus<IMABaselineRecord> result =
                baselineContains(baseline, record, getTestImaPolicy(true));
        Assert.assertEquals(result.getStatus(), expected.getStatus());
        Assert.assertTrue(result.getBaselineRecords().containsAll(expected.getBaselineRecords()));
    }

    /**
     * Tests that the partial path comparison works for an unknown file. This
     * creates two entries in the baseline for a gradle binary. One is under
     * /usr/bin and the other is under a user's home directory. Then the
     * contains method is then called to verify that a file with an unknown name
     * is not found.
     */
    @Test
    public final void containsPartialUnknown() {
        final SimpleImaBaseline baseline = new SimpleImaBaseline("TestBaseline");
        final String binGradle = "/usr/bin/gradle";
        final String userGradle = "/home/foo/bin/gradle";
        final Digest binGradleHash = getDigest("33333c2f7f3003d2e4baddc46ed4763a49543333");
        final Digest userGradleHash = getDigest("44443c2f7f3003d2e4baddc46ed4763a49544444");
        final IMABaselineRecord binRecord = new IMABaselineRecord(binGradle, binGradleHash);
        final IMABaselineRecord userRecord = new IMABaselineRecord(userGradle, userGradleHash);
        baseline.addToBaseline(binRecord);
        baseline.addToBaseline(userRecord);
        final String unknownFile = "unknown";
        final Digest unknownHash = getDigest("66663c2f7f3003d2e4baddc46ed4763a49546666");
        final IMAMeasurementRecord record = new IMAMeasurementRecord(unknownFile, unknownHash);
        final IMAMatchStatus expected = new IMAMatchStatus<>(
                record, ReportMatchStatus.UNKNOWN, baseline
        );
        Assert.assertEquals(baselineContains(baseline, record, getTestImaPolicy(true)), expected);
    }

    /**
     * Tests that contains returns UNKNOWN when partial paths are enabled, but a
     * full path <code>IMAMeasurementRecord</code> with a matching filename is
     * located in the wrong directory.
     */
    @Test
    public final void containsFullUnknown() {
        final SimpleImaBaseline baseline = new SimpleImaBaseline("TestBaseline");
        final String binPath = "/usr/bin/gradle";
        final String userPath = "/home/foo/bin/gradle";
        final Digest hash = getDigest("33333c2f7f3003d2e4baddc46ed4763a49543333");
        final IMABaselineRecord binRecord = new IMABaselineRecord(binPath, hash);
        final IMAMeasurementRecord userRecord = new IMAMeasurementRecord(userPath, hash);
        baseline.addToBaseline(binRecord);
        Assert.assertEquals(baselineContains(baseline, userRecord, getTestImaPolicy(true)),
                new IMAMatchStatus<>(userRecord, ReportMatchStatus.UNKNOWN, baseline));
        Assert.assertEquals(baselineContains(baseline, userRecord, getTestImaPolicy(false)),
                new IMAMatchStatus<>(userRecord, ReportMatchStatus.UNKNOWN, baseline));
    }

    /**
     * Tests that contains returns MISMATCH when
     * <code>IMAMeasurementRecord</code> matches the path but not the hash.
     */
    @Test
    public final void containsFullMismatch() {
        final SimpleImaBaseline baseline = new SimpleImaBaseline("TestBaseline");
        final IMABaselineRecord baselineRecord;
        baselineRecord = createTestIMARecord("imaTestRecord");
        baseline.addToBaseline(baselineRecord);
        final String path = "imaTestRecord";
        final String sha1 = "cafe3c2f7f3003d2e4baddc46ed4763a4954babe";
        final IMAMeasurementRecord record = new IMAMeasurementRecord(path, getDigest(sha1));
        final IMAMatchStatus expected = new IMAMatchStatus<>(
                record, ReportMatchStatus.MISMATCH, baselineRecord, baseline
        );
        Assert.assertEquals(baselineContains(baseline, record, getTestImaPolicy(true)), expected);
    }

    /**
     * Tests the case in which an <code>IMABaselineRecord</code> and an
     * <code>IMAMeasurementRecord</code> both have partial path names (starting
     * with a character other than '/') and have the same hash. This should
     * return a MATCH even when partialPathEnable is false, because all exact
     * path matches should always succeed.
     */
    @Test
    public final void containsExactPartialPath() {
        final SimpleImaBaseline baseline = new SimpleImaBaseline("TestBaseline");
        final String path = "gradle";
        final Digest hash = getDigest("33333c2f7f3003d2e4baddc46ed4763a49543333");
        final IMABaselineRecord baselineRecord = new IMABaselineRecord(path, hash);
        baseline.addToBaseline(baselineRecord);
        IMAMeasurementRecord record = new IMAMeasurementRecord(path, hash);
        Assert.assertEquals(baselineContains(baseline, record, getTestImaPolicy(true)),
                new IMAMatchStatus<>(record, ReportMatchStatus.MATCH, baselineRecord, baseline));
        Assert.assertEquals(baselineContains(baseline, record, getTestImaPolicy(false)),
                new IMAMatchStatus<>(record, ReportMatchStatus.MATCH, baselineRecord, baseline));
    }

    /**
     * Tests that contains throws an <code>IllegalArgumentException</code> when
     * <code>IMAMeasurementRecord</code> is null.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void containsNull() {
        baselineContains(new SimpleImaBaseline("TestBaseline"), null, getTestImaPolicy(true));
    }

    /**
     * Tests that IMA baselines correctly evaluates records according to the configured
     * equivalent paths.  Tests that the relationships function both directions and only as
     * configured.
     */
    @Test
    public final void containsEquivalentPath() {
        ArrayList<String> paths = new ArrayList<>(Arrays.asList(
                "/bin/ls", "/lib/ld.so", "/lib64/ld.so"
        ));
        ArrayList<String> usrPaths = new ArrayList<>(Arrays.asList(
                "/usr/bin/ls", "/usr/lib/ld.so", "/usr/lib64/ld.so"
        ));
        ArrayList<String> otherPaths = new ArrayList<>(Arrays.asList(
                "/other/bin/ls", "/other/lib/ld.so", "/other/lib64/ld.so"
        ));

        final Digest hash = getDigest("33333c2f7f3003d2e4baddc46ed4763a49543333");

        SimpleImaBaseline baseline;
        for (Boolean swap : Arrays.asList(false, true)) {
            for (int i = 0; i < paths.size(); i++) {
                baseline = new SimpleImaBaseline("TestBaseline");

                final IMABaselineRecord baselineRecord;
                final IMAMeasurementRecord measurementRecord;

                if (!swap) {
                    baselineRecord = new IMABaselineRecord(paths.get(i), hash);
                    measurementRecord = new IMAMeasurementRecord(usrPaths.get(i), hash);
                } else {
                    baselineRecord = new IMABaselineRecord(usrPaths.get(i), hash);
                    measurementRecord = new IMAMeasurementRecord(paths.get(i), hash);
                }

                final IMAMeasurementRecord otherRecord =
                        new IMAMeasurementRecord(otherPaths.get(i), hash);
                baseline.addToBaseline(baselineRecord);

                Assert.assertEquals(baselineContains(
                        baseline, measurementRecord, getTestImaPolicy(true)
                        ),
                        new IMAMatchStatus<>(
                                measurementRecord,
                                ReportMatchStatus.MATCH,
                                baselineRecord,
                                baseline
                        )
                );
                Assert.assertEquals(baselineContains(
                        baseline, measurementRecord, getTestImaPolicy(false)
                        ),
                        new IMAMatchStatus<>(
                                measurementRecord,
                                ReportMatchStatus.MATCH,
                                baselineRecord,
                                baseline
                        )
                );

                Assert.assertEquals(baselineContains(
                        baseline, otherRecord, getTestImaPolicy(true)
                        ),
                        new IMAMatchStatus<>(otherRecord, ReportMatchStatus.UNKNOWN, baseline));
                Assert.assertEquals(baselineContains(
                        baseline, otherRecord, getTestImaPolicy(false)
                        ),
                        new IMAMatchStatus<>(otherRecord, ReportMatchStatus.UNKNOWN, baseline));
            }
        }
    }

    /**
     * Simple test that ensures a SimpleImaBaseline can determine whether it contains
     * baseline records that match measurement records based solely on their hashes.
     */
    @Test
    public final void containsHashes() {
        final SimpleImaBaseline baseline = new SimpleImaBaseline("TestBaseline");
        final String baselineGradleFilename = "/usr/bin/gradle";
        final String measuredGradleFilename = "/usr/bin/gradle_by_another_name";
        final Digest gradleHash = getDigest("33333c2f7f3003d2e4baddc46ed4763a49543333");

        final IMABaselineRecord baseRecSameNameMatchingHash = new IMABaselineRecord(
                measuredGradleFilename, gradleHash
        );

        final IMABaselineRecord baseRecDifferentNameMatchingHash = new IMABaselineRecord(
                baselineGradleFilename, gradleHash
        );

        baseline.addToBaseline(baseRecSameNameMatchingHash);
        baseline.addToBaseline(baseRecDifferentNameMatchingHash);

        baseline.addToBaseline(new IMABaselineRecord(
                baselineGradleFilename,
                getDigest("00000c2f7f3003d2e4baddc46ed4763a49543333")
        ));
        baseline.addToBaseline(new IMABaselineRecord(
                measuredGradleFilename,
                getDigest("00000c2f7f3003d2e4baddc46ed4763a49543333")
        ));


        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(
                measuredGradleFilename, gradleHash
        );

        Set<IMABaselineRecord> matchingRecords = new HashSet<>(Arrays.asList(
                baseRecSameNameMatchingHash,
                baseRecDifferentNameMatchingHash
        ));

        Assert.assertEquals(
                baselineContainsHashes(baseline, measurementRecord, getTestImaPolicy(false)),
                new BatchImaMatchStatus<>(
                        Collections.singleton(new IMAMatchStatus<>(
                                measurementRecord,
                                ReportMatchStatus.MATCH,
                                matchingRecords,
                                baseline
                        ))
                )
        );
    }

    /**
     * Simple test that ensures a SimpleImaBaseline can determine whether it contains
     * baseline records that match measurement records based solely on their hashes.
     */
    @Test
    public final void containsHashesWithNoMatches() {
        final SimpleImaBaseline baseline = new SimpleImaBaseline("TestBaseline");
        final String baselineGradleFilename = "/usr/bin/gradle";
        final String measuredGradleFilename = "/usr/bin/gradle_by_another_name";
        final Digest gradleHash = getDigest("33333c2f7f3003d2e4baddc46ed4763a49543333");

        baseline.addToBaseline(new IMABaselineRecord(
                baselineGradleFilename,
                getDigest("00000c2f7f3003d2e4baddc46ed4763a49543333")
        ));

        IMAMeasurementRecord record = new IMAMeasurementRecord(
                measuredGradleFilename, gradleHash
        );

        Assert.assertEquals(baselineContainsHashes(baseline, record, getTestImaPolicy(false)),
                new BatchImaMatchStatus<>(
                        Collections.singleton(new IMAMatchStatus<>(
                                record,
                                ReportMatchStatus.UNKNOWN,
                                baseline
                        ))
                )
        );
    }

    /**
     * Tests that getBaselineRecords() returns a list of IMA records.
     *
     * @throws ParseException
     *             if error generated parsing date
     */
    @Test
    public final void getMeasurementRecords() throws ParseException {
        SimpleImaBaseline imaBaseline = new SimpleImaBaseline("TestBaseline");
        IMABaselineRecord imaRecord;
        imaRecord = createTestIMARecord("imaTestRecord");
        imaBaseline.addToBaseline(imaRecord);
        Set<IMABaselineRecord> imaRecords
                = imaBaseline.getBaselineRecords();
        Assert.assertNotNull(imaRecords);
        Assert.assertTrue(imaRecords.contains(imaRecord));
        Assert.assertEquals(imaRecords.size(), 1);
    }

    /**
     * Tests that getName() returns the name of IMA baseline.
     */
    @Test
    public final void getName() {
        final String name = "TestBaseline";
        SimpleImaBaseline imaBaseline = new SimpleImaBaseline(name);
        Assert.assertEquals(imaBaseline.getName(), name);
    }

    /**
     * Tests that removeFromBaseline() removes an IMA record from the baseline
     * and that a subsequent call to contains() for that records produces an
     * UNKNOWN result.
     *
     * @throws ParseException
     *             if error generated parsing date
     */
    @Test
    public final void removeFromBaseline() throws ParseException {
        boolean removed = false;
        final SimpleImaBaseline baseline = new SimpleImaBaseline("TestBaseline");
        final String binGradle = "/usr/bin/gradle";
        final String userGradle = "/home/foo/bin/gradle";
        final Digest binGradleHash =
                getDigest("33333c2f7f3003d2e4baddc46ed4763a49543333");
        final Digest userGradleHash =
                getDigest("44443c2f7f3003d2e4baddc46ed4763a49544444");
        final IMABaselineRecord binRecord =
                new IMABaselineRecord(binGradle, binGradleHash);
        final IMABaselineRecord userRecord =
                new IMABaselineRecord(userGradle, userGradleHash);
        baseline.addToBaseline(binRecord);
        baseline.addToBaseline(userRecord);

        final String gradle = "gradle";
        final IMAMeasurementRecord binGradleRecord =
                new IMAMeasurementRecord(binGradle, binGradleHash);
        final IMAMeasurementRecord userGradleRecord =
                new IMAMeasurementRecord(userGradle, userGradleHash);
        final IMAMeasurementRecord partialPathBinRecord =
                new IMAMeasurementRecord(gradle, binGradleHash);

        Set<IMABaselineRecord> records = baseline.getBaselineRecords();
        Assert.assertTrue(records.contains(binRecord));
        Assert.assertTrue(records.contains(userRecord));
        Assert.assertEquals(records.size(), 2);
        Assert.assertEquals(
                baselineContains(baseline, binGradleRecord, getTestImaPolicy(true)),
                new IMAMatchStatus<>(
                        binGradleRecord, ReportMatchStatus.MATCH, binRecord, baseline
                )
        );
        Assert.assertEquals(
                baselineContains(baseline, userGradleRecord, getTestImaPolicy(true)),
                new IMAMatchStatus<>(
                        userGradleRecord, ReportMatchStatus.MATCH, userRecord, baseline
                )
        );
        Assert.assertEquals(baselineContains(
                baseline, partialPathBinRecord, getTestImaPolicy(false)
                ),
                new IMAMatchStatus<>(partialPathBinRecord, ReportMatchStatus.UNKNOWN, baseline));
        Assert.assertEquals(baselineContains(
                baseline, partialPathBinRecord, getTestImaPolicy(true)
                ),
                new IMAMatchStatus<>(
                        partialPathBinRecord, ReportMatchStatus.MATCH, binRecord, baseline
                )
        );

        removed = baseline.removeFromBaseline(binRecord);
        Assert.assertTrue(removed);
        Assert.assertFalse(records.contains(binRecord));
        Assert.assertTrue(records.contains(userRecord));
        Assert.assertEquals(records.size(), 1);
        Assert.assertEquals(baselineContains(baseline, binGradleRecord, getTestImaPolicy(true)),
                new IMAMatchStatus<>(binGradleRecord, ReportMatchStatus.UNKNOWN, baseline));
        Assert.assertEquals(baselineContains(baseline, userGradleRecord, getTestImaPolicy(true)),
                new IMAMatchStatus<>(
                        userGradleRecord, ReportMatchStatus.MATCH, userRecord, baseline
                )
        );
        Assert.assertEquals(baselineContains(
                baseline, partialPathBinRecord, getTestImaPolicy(false)
                ),
                new IMAMatchStatus<>(partialPathBinRecord, ReportMatchStatus.UNKNOWN, baseline));
        Assert.assertEquals(
                baselineContains(baseline, partialPathBinRecord, getTestImaPolicy(true)
                ),
                new IMAMatchStatus<>(
                        partialPathBinRecord, ReportMatchStatus.MISMATCH, userRecord, baseline
                )
        );
    }

    /**
     * Tests that removeFromBaseline() handles attempt to remove record not
     * found in baseline.
     *
     * @throws ParseException
     *             if error generated parsing date
     */
    @Test
    public final void removeFromBaselineRecordNotFound() throws ParseException {
        boolean removed = false;
        SimpleImaBaseline imaBaseline = new SimpleImaBaseline("TestBaseline");
        IMABaselineRecord imaRecord;
        IMABaselineRecord imaRecord2;
        imaRecord = createTestIMARecord("imaTestRecord");
        imaRecord2 = createTestIMARecord("imaTestRecord2");
        imaBaseline.addToBaseline(imaRecord);
        removed = imaBaseline.removeFromBaseline(imaRecord2);
        Assert.assertFalse(removed);
    }

    /**
     * Tests that removeFromBaseline() handles invalid ima record.
     *
     * @throws ParseException
     *             if error generated parsing date
     */
    @Test
    public final void removeFromBaselineNullRecord() throws ParseException {
        SimpleImaBaseline imaBaseline = new SimpleImaBaseline("TestBaseline");
        IMABaselineRecord imaRecord;
        imaRecord = createTestIMARecord("imaTestRecord");
        imaBaseline.addToBaseline(imaRecord);
        imaRecord = null;
        Assert.assertFalse(imaBaseline.removeFromBaseline(imaRecord));
    }

    /**
     * Tests that a <code>ImaBaseline</code> can be saved using Hibernate.
     */
    @Test
    public final void testSaveBaseline() {
        LOGGER.debug("save IMA baseline test started");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final Baseline b = IMATestUtil.getVerifyBaseline();
        session.save(b);
        session.getTransaction().commit();
    }

    /**
     * Tests that an <code>ImaBaseline</code> can be saved and retrieved. This
     * saves a <code>ImaBaseline</code> in the repo. Then a new session is
     * created, and the baseline is retrieved and its properties verified.
     */
    @Test
    public final void testGetBaseline() {
        LOGGER.debug("get IMA baseline test started");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final SimpleImaBaseline  b = IMATestUtil.getVerifyBaseline();
        LOGGER.debug("saving baseline");
        final UUID id = (UUID) session.save(b);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        LOGGER.debug("getting baseline");
        final SimpleImaBaseline testBaseline = (SimpleImaBaseline) session.get(
                SimpleImaBaseline.class, id);

        LOGGER.debug("verifying baseline's properties");
        Assert.assertEquals(testBaseline.getName(), IMATestUtil.VERIFY_BASELINE_NAME);
        final Set<IMABaselineRecord> expectedRecords = IMATestUtil
                .getExpectedRecords();
        final Set<IMABaselineRecord> baselineRecords = testBaseline
                .getBaselineRecords();
        Assert.assertTrue(baselineRecords.equals(expectedRecords));
        session.getTransaction().commit();
    }

    /**
     * Tests that a baseline can be saved and then later updated. This saves
     * the baseline, retrieves it, adds a baseline record to it, and then
     * retrieves it and verifies it.
     *
     * @throws MalformedURLException if the URL used for the Yum Repo is malformed
     */
    @Test
    public final void testUpdateBaseline() throws MalformedURLException {
        LOGGER.debug("update IMA baseline test started");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final ImaBaseline b = IMATestUtil.getVerifyBaseline();
        LOGGER.debug("saving baseline");
        final UUID id = (UUID) session.save(b);
        session.getTransaction().commit();

        final String path = "/some/file/foo.so";
        final IMABaselineRecord addedRecord = createTestIMARecord(path);

        LOGGER.debug("updating baseline");
        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        SimpleImaBaseline testBaseline = (SimpleImaBaseline) session.get(SimpleImaBaseline.class,
                id);
        testBaseline.addToBaseline(addedRecord);
        testBaseline.setYumRepoURL(IMATestUtil.getYumRepoURL());
        session.update(testBaseline);
        session.getTransaction().commit();

        LOGGER.debug("getting baseline");
        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        testBaseline = (SimpleImaBaseline) session.get(SimpleImaBaseline.class, id);

        final Set<IMABaselineRecord> expectedRecords = IMATestUtil
                .getExpectedRecords();
        expectedRecords.add(addedRecord);
        Assert.assertTrue(testBaseline.getBaselineRecords().equals(expectedRecords));
        Assert.assertEquals(testBaseline.getYumRepoURL(), IMATestUtil.getYumRepoURL());
        session.getTransaction().commit();
    }

    /**
     * Tests that a <code>SimpleImaBaseline</code> can be archived.
     */
    @Test
    public final void testArchiveBaseline() {
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        LOGGER.debug("archive IMA baseline test started");

        final SimpleImaBaseline baseline = IMATestUtil.getVerifyBaseline();
        mgr.saveBaseline(baseline);
        mgr.archive(baseline.getName());
        SimpleImaBaseline retrievedBaseline =
                (SimpleImaBaseline) mgr.getBaseline(baseline.getName());
        Assert.assertTrue(retrievedBaseline.isArchived());
    }

    /**
     * Tests that the date can be persisted with the baseline.
     */
    @Test
    public final void setGetDate() {
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final SimpleImaBaseline  b = IMATestUtil.getVerifyBaseline();
        Date setDate = new Date();
        b.setDate(setDate);
        final UUID id = (UUID) session.save(b);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final SimpleImaBaseline  testBaseline = (SimpleImaBaseline) session.get(
                SimpleImaBaseline .class, id);
        session.getTransaction().commit();

        Date getDate = new Date(testBaseline.getDate().getTime());
        Assert.assertEquals(setDate, getDate);

    }

    /**
     * Create and return a test IMA record.
     *
     * @param fileName
     *            of ima record
     * @return the generated record
     */
    public static IMABaselineRecord createTestIMARecord(final String fileName) {
        final String sha1 = "3d5f3c2f7f3003d2e4baddc46ed4763a4954f648";
        try {
            final byte[] hash = Hex.decodeHex(sha1.toCharArray());
            final Digest digest = new Digest(DigestAlgorithm.SHA1, hash);
            return new IMABaselineRecord(fileName, digest);
        } catch (Exception e) {
            LOGGER.error("unexpected exception", e);
            throw new RuntimeException("unexpected exception", e);
        }
    }

    /**
     * Create a SHA1 digest object from the given sha1 string (encoded in hexadecimal).
     *
     * @param sha1 the sha1 string representing a digest
     * @return the Digest object representing the given SHA1 digest
     */
    public static Digest getDigest(final String sha1) {
        try {
            final byte[] hash = Hex.decodeHex(sha1.toCharArray());
            return new Digest(DigestAlgorithm.SHA1, hash);
        } catch (Exception e) {
            LOGGER.error("unexpected exception", e);
            throw new RuntimeException("unexpected exception", e);
        }
    }
}
