package hirs.data.persist;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import hirs.ima.matching.BatchImaMatchStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import hirs.ima.matching.IMAMatchStatus;
import hirs.persist.BaselineManager;
import hirs.persist.DBBaselineManager;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import hirs.persist.DBBaselineManagerTest;
import hirs.persist.DBRepositoryManager;
import hirs.persist.DBUtility;
import hirs.persist.DbImaBaselineRecordManager;
import hirs.repository.RPMRepoPackage;
import hirs.repository.RepoPackage;
import hirs.repository.RepoPackageTest;
import hirs.repository.Repository;
import hirs.repository.TestRepository;

/**
 * Unit tests for the <code>BroadRepoImaBaseline</code> class.
 */
public class BroadRepoImaBaselineTest extends SpringPersistenceTest {
    private static final Logger LOGGER = LogManager.getLogger(BroadRepoImaBaseline.class);
    private static final String NAME = "test-package";
    private static final String VERSION1 = "1.1.1";
    private static final String VERSION2 = "2.2.2";
    private static final String RELEASE1 = "75";
    private static final String RELEASE2 = "77";
    private static final String ARCHITECTURE = "x86";
    private static final String BASELINE_NAME = "test broad repo baseline";
    private static final Integer RECORD_COUNT = 3;
    private static final String FILEPATH1 = "/original/test/path.file";
    private static final String FILEPATH2 = "/update1/test/path.file";
    private static final String FILEPATH3 = "/update2/test/path.file";
    private static final String BIN_FILE = "/bin/ls";
    private static final String USR_BIN_FILE = "/usr/bin/ls";
    private static final String REPO_NAME_ONE = "first test repo";
    private static final String REPO_NAME_TWO = "second test repo";
    private TestRepository repo1;
    private TestRepository repo2;
    private RPMRepoPackage repoPackage;
    private RPMRepoPackage update1;
    private RPMRepoPackage update2;
    private BroadRepoImaBaseline baseline;

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an
     * in-memory database that is used for testing.
     */
    @BeforeClass
    public final void setup() {
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterClass
    public final void tearDown() {
    }

    /**
     * Initializes a <code>BroadRepoImaBaseline</code>, some <code>RepoPackage</code>s with
     * records, and a mock repository for updates.
     *
     * @throws Exception if creating a RepoPackage fails.
     */
    @BeforeMethod
    public final void setUp() throws Exception {
        baseline = new BroadRepoImaBaseline(BASELINE_NAME);
        repo1 = new TestRepository(REPO_NAME_ONE);

        Set<Repository<?>> repos = new HashSet<>();
        repoPackage = new RPMRepoPackage(NAME, VERSION1, RELEASE1, ARCHITECTURE, repo1);

        Set<IMABaselineRecord> imaRecords = new HashSet<>();
        imaRecords.add(SimpleImaBaselineTest.createTestIMARecord(FILEPATH1));

        repoPackage.setAllMeasurements(imaRecords, RepoPackageTest.getTestDigest());
        Set<RPMRepoPackage> originalPackages = new HashSet<>();
        originalPackages.add(repoPackage);

        repo1.setPackages(originalPackages);
        repos.add(repo1);
        baseline.setRepositories(repos);
        baseline.update(null);

        DBUtility.removeAllInstances(sessionFactory, Baseline.class);
        DBUtility.removeAllInstances(sessionFactory, RepoPackage.class);
        DBUtility.removeAllInstances(sessionFactory, Repository.class);
    }

    /**
     * Tests the only constructor, which takes in the baseline name as a string.
     */
    @Test
    public final void testNamedConstructor() {
        Assert.assertTrue(baseline.getName().equals(BASELINE_NAME));
        Assert.assertEquals(baseline.getRepositories().size(), 1);
        Assert.assertTrue(baseline.getRepositories().contains(repo1));
    }

    /**
     * Tests that an update will populate the baseline with additional repositories. Also tests
     * getBaselineRecords before and after the update.
     *
     * @throws Exception for unsupported digest encoding
     */
    @Test
    public final void testUpdate() throws Exception {
        repo2 = new TestRepository(REPO_NAME_TWO);

        update1 = new RPMRepoPackage(NAME, VERSION2, RELEASE1, ARCHITECTURE, repo2);
        Set<IMABaselineRecord> imaRecords = new HashSet<>();
        imaRecords.add(SimpleImaBaselineTest.createTestIMARecord(FILEPATH2));
        update1.setAllMeasurements(imaRecords, RepoPackageTest.getTestDigest());

        update2 = new RPMRepoPackage(NAME, VERSION1, RELEASE2, ARCHITECTURE, repo2);
        imaRecords.clear();
        imaRecords.add(SimpleImaBaselineTest.createTestIMARecord(FILEPATH3));
        update2.setAllMeasurements(imaRecords, RepoPackageTest.getTestDigest());

        Set<RPMRepoPackage> updatedPackages = new HashSet<>();
        updatedPackages.add(update1);
        updatedPackages.add(update2);
        repo2.setPackages(updatedPackages);

        Set<Repository<?>> oldRepositories = baseline.getRepositories();
        Set<Repository<?>> newRepositories = new HashSet<>(oldRepositories);
        newRepositories.add(repo2);

        Set<IMABaselineRecord> oldRetrievedRecords = baseline.getBaselineRecords();
        Assert.assertEquals(oldRetrievedRecords.size(), 1);

        baseline.setRepositories(newRepositories);
        baseline.update(null);

        Set<IMABaselineRecord> newRetrievedRecords = baseline.getBaselineRecords();
        Assert.assertEquals((Integer) newRetrievedRecords.size(), RECORD_COUNT);
        for (IMABaselineRecord retrievedRecord : newRetrievedRecords) {
            Assert.assertTrue(retrievedRecord.getPath().equals(FILEPATH1)
                    || retrievedRecord.getPath().equals(FILEPATH2)
                    || retrievedRecord.getPath().equals(FILEPATH3));
        }
    }

    /**
     * Tests the basic functionality of the contains() method. More extensive edge case testing is
     * done in <code>SimpleImaBaselineTest</code> and is not reproduced here.
     *
     * @throws UnsupportedEncodingException
     *             if an error is encountered while getting the test digest
     */
    @Test
    public final void testContains() throws UnsupportedEncodingException {
        BroadRepoImaBaseline testBaseline = new BroadRepoImaBaseline(BASELINE_NAME);
        Repository testRepo = new TestRepository("Test Repository", 0);
        DBRepositoryManager repoManager = new DBRepositoryManager(sessionFactory);
        testRepo = repoManager.saveRepository(testRepo);
        RepoPackage testRepoPackage =
                new RPMRepoPackage(NAME, VERSION1, RELEASE1, ARCHITECTURE, testRepo);
        Set<IMABaselineRecord> imaRecords = new HashSet<>();
        imaRecords.add(SimpleImaBaselineTest.createTestIMARecord(FILEPATH1));
        testRepoPackage.setAllMeasurements(imaRecords, RepoPackageTest.getTestDigest());
        repoManager.saveRepoPackage(testRepoPackage);
        Set<Repository<?>> originalRepositories = new HashSet<>();
        originalRepositories.add(testRepo);
        testBaseline.setRepositories(originalRepositories);
        testBaseline.update(repoManager);

        DBBaselineManager baselineManager = new DBBaselineManager(sessionFactory);
        BroadRepoImaBaseline savedBaseline =
                (BroadRepoImaBaseline) baselineManager.save(testBaseline);

        IMABaselineRecord baselineRecord = SimpleImaBaselineTest.createTestIMARecord(FILEPATH1);
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(baselineRecord.getPath(),
                baselineRecord.getHash());
        Assert.assertEquals(
                savedBaseline.contains(
                        Collections.singletonList(measurementRecord),
                        new DbImaBaselineRecordManager(sessionFactory),
                        SimpleImaBaselineTest.getTestImaPolicy(false)

                ).getIMAMatchStatuses(measurementRecord),
                Collections.singleton(
                        new IMAMatchStatus<>(
                                measurementRecord, ReportMatchStatus.MATCH, baselineRecord, baseline
                        )
                )
        );
    }

    /**
     * Tests the basic functionality of the contains() method. More extensive edge case testing is
     * done in <code>SimpleImaBaselineTest</code> and is not reproduced here.
     *
     * @throws UnsupportedEncodingException
     *             if an error is encountered while getting the test digest
     */
    @Test
    public final void testContainsEquivalentPath() throws UnsupportedEncodingException {
        BroadRepoImaBaseline testBaseline = new BroadRepoImaBaseline(BASELINE_NAME);
        Repository testRepo = new TestRepository("Test Repository", 0);
        DBRepositoryManager repoManager = new DBRepositoryManager(sessionFactory);
        testRepo = repoManager.saveRepository(testRepo);
        RepoPackage testRepoPackage =
                new RPMRepoPackage(NAME, VERSION1, RELEASE1, ARCHITECTURE, testRepo);
        Set<IMABaselineRecord> imaRecords = new HashSet<>();
        IMABaselineRecord baselineRecord = SimpleImaBaselineTest.createTestIMARecord(BIN_FILE);
        imaRecords.add(baselineRecord);
        testRepoPackage.setAllMeasurements(imaRecords, RepoPackageTest.getTestDigest());
        repoManager.saveRepoPackage(testRepoPackage);
        Set<Repository<?>> originalRepositories = new HashSet<>();
        originalRepositories.add(testRepo);
        testBaseline.setRepositories(originalRepositories);
        testBaseline.update(repoManager);

        DBBaselineManager baselineManager = new DBBaselineManager(sessionFactory);
        BroadRepoImaBaseline savedBaseline =
                (BroadRepoImaBaseline) baselineManager.save(testBaseline);

        IMABaselineRecord tempBaselineRecord =
                SimpleImaBaselineTest.createTestIMARecord(USR_BIN_FILE);
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(
                tempBaselineRecord.getPath(),
                tempBaselineRecord.getHash()
        );
        Assert.assertEquals(
                savedBaseline.contains(
                        Collections.singletonList(measurementRecord),
                        new DbImaBaselineRecordManager(sessionFactory),
                        SimpleImaBaselineTest.getTestImaPolicy(false)

                ).getIMAMatchStatuses(measurementRecord),
                Collections.singleton(
                        new IMAMatchStatus<>(
                                measurementRecord, ReportMatchStatus.MATCH, baselineRecord, baseline
                        )
                )
        );
    }

    /**
     * Test that ensures a BroadRepoImaBaseline can correctly determine if
     * it contains any matching baseline records solely based upon a given measurement
     * record's hash.
     *
     * @throws UnsupportedEncodingException
     *             if an error is encountered while getting the test digest
     */
    @Test
    public final void containsHashes() throws UnsupportedEncodingException {
        BroadRepoImaBaseline testBaseline = new BroadRepoImaBaseline(BASELINE_NAME);
        Repository testRepo = new TestRepository("Test Repository", 0);
        DBRepositoryManager repoManager = new DBRepositoryManager(sessionFactory);
        testRepo = repoManager.saveRepository(testRepo);
        RepoPackage testRepoPackage =
                new RPMRepoPackage(NAME, VERSION1, RELEASE1, ARCHITECTURE, testRepo);
        Set<IMABaselineRecord> imaRecords = new HashSet<>();
        imaRecords.add(SimpleImaBaselineTest.createTestIMARecord(FILEPATH1));
        testRepoPackage.setAllMeasurements(imaRecords, RepoPackageTest.getTestDigest());
        repoManager.saveRepoPackage(testRepoPackage);
        Set<Repository<?>> originalRepositories = new HashSet<>();
        originalRepositories.add(testRepo);
        testBaseline.setRepositories(originalRepositories);
        testBaseline.update(repoManager);

        DBBaselineManager baselineManager = new DBBaselineManager(sessionFactory);
        BroadRepoImaBaseline savedBaseline =
                (BroadRepoImaBaseline) baselineManager.save(testBaseline);

        IMABaselineRecord baselineRecord = SimpleImaBaselineTest.createTestIMARecord(FILEPATH1);
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(
                baselineRecord.getPath(),
                baselineRecord.getHash()
        );
        Assert.assertEquals(
                savedBaseline.containsHashes(
                        Collections.singletonList(measurementRecord),
                        new DbImaBaselineRecordManager(sessionFactory),
                        SimpleImaBaselineTest.getTestImaPolicy(false)

                ).getIMAMatchStatuses(measurementRecord),
                Collections.singleton(
                        new IMAMatchStatus<>(
                                measurementRecord, ReportMatchStatus.MATCH, baselineRecord, baseline
                        )
                )
        );

        measurementRecord = new IMAMeasurementRecord(
                "/some/other/file",
                baselineRecord.getHash()
        );
        Assert.assertEquals(
                savedBaseline.containsHashes(
                        Collections.singletonList(measurementRecord),
                        new DbImaBaselineRecordManager(sessionFactory),
                        SimpleImaBaselineTest.getTestImaPolicy(false)

                ).getIMAMatchStatuses(measurementRecord),
                Collections.singleton(
                        new IMAMatchStatus<>(
                                measurementRecord, ReportMatchStatus.MATCH, baselineRecord, baseline
                        )
                )
        );
    }

    /**
     * Test that ensures a BroadRepoImaBaseline can correctly determine that
     * it does not contain any matching baseline records solely based upon a given measurement
     * record's hash.
     *
     * @throws UnsupportedEncodingException
     *             if an error is encountered while getting the test digest
     */
    @Test
    public final void containsHashesWithNoMatches() throws UnsupportedEncodingException {
        BroadRepoImaBaseline testBaseline = new BroadRepoImaBaseline(BASELINE_NAME);
        Repository testRepo = new TestRepository("Test Repository", 0);
        DBRepositoryManager repoManager = new DBRepositoryManager(sessionFactory);
        testRepo = repoManager.saveRepository(testRepo);
        RepoPackage testRepoPackage =
                new RPMRepoPackage(NAME, VERSION1, RELEASE1, ARCHITECTURE, testRepo);
        Set<IMABaselineRecord> imaRecords = new HashSet<>();
        imaRecords.add(SimpleImaBaselineTest.createTestIMARecord(FILEPATH1));
        testRepoPackage.setAllMeasurements(imaRecords, RepoPackageTest.getTestDigest());
        repoManager.saveRepoPackage(testRepoPackage);
        Set<Repository<?>> originalRepositories = new HashSet<>();
        originalRepositories.add(testRepo);
        testBaseline.setRepositories(originalRepositories);
        testBaseline.update(repoManager);

        DBBaselineManager baselineManager = new DBBaselineManager(sessionFactory);
        BroadRepoImaBaseline savedBaseline =
                (BroadRepoImaBaseline) baselineManager.save(testBaseline);

        IMABaselineRecord baselineRecord = SimpleImaBaselineTest.createTestIMARecord(FILEPATH1);
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(
                baselineRecord.getPath(),
                SimpleImaBaselineTest.getDigest("0d5f3c2f7f3003d2e4baddc46ed4763a4954f648")
        );
        Assert.assertEquals(
                savedBaseline.containsHashes(
                        Collections.singletonList(measurementRecord),
                        new DbImaBaselineRecordManager(sessionFactory),
                        SimpleImaBaselineTest.getTestImaPolicy(false)

                ),
                new BatchImaMatchStatus<>(Collections.singleton(new IMAMatchStatus<>(
                        measurementRecord,
                        ReportMatchStatus.UNKNOWN,
                        baseline
                )))
        );
    }

    /**
     * Tests that the <code>Set</code> of <code>Repositories</code>s associated with this baseline
     * can be set, retrieved, and returned.
     */
    @Test
    public final void testGetSetRepositories() {
        Set<Repository<?>> retrievedRepositories = baseline.getRepositories();
        Assert.assertTrue(retrievedRepositories.contains(repo1));

        repo2 = new TestRepository(REPO_NAME_TWO);
        Set<Repository<?>> addedRepository = new HashSet<>();
        Assert.assertFalse(addedRepository.contains(repo1));
        addedRepository.add(repo2);
        Assert.assertFalse(addedRepository.contains(repo1));
        baseline.setRepositories(addedRepository);

        retrievedRepositories = baseline.getRepositories();
        Assert.assertFalse(retrievedRepositories.contains(repo1));
        Assert.assertTrue(retrievedRepositories.contains(repo2));
    }

    /**
     * Tests that a BroadRepoImaBaseline is able to be saved.
     */
    @Test
    public final void testSaveBroadRepoImaBaseline() {
        BroadRepoImaBaseline testBaseline = new BroadRepoImaBaseline(BASELINE_NAME);
        DBBaselineManager baselineManager = new DBBaselineManager(sessionFactory);
        BroadRepoImaBaseline savedBaseline =
                (BroadRepoImaBaseline) baselineManager.save(testBaseline);
        Assert.assertEquals(savedBaseline, testBaseline);
        Assert.assertTrue(DBBaselineManagerTest.isInDatabase(BASELINE_NAME, sessionFactory));
    }

    /**
     * Tests that the BroadRepoImaBaseline with a Repo is able to be saved.
     *
     * @throws UnsupportedEncodingException
     *             if an error is encountered while getting the test digest
     */
    @Test
    public final void testSaveBroadRepoImaBaselineWithRepository()
            throws UnsupportedEncodingException {
        BroadRepoImaBaseline testBaseline = new BroadRepoImaBaseline(BASELINE_NAME);

        Repository testRepo = new TestRepository("Test Repository", 0);
        DBRepositoryManager repoManager = new DBRepositoryManager(sessionFactory);
        testRepo = repoManager.saveRepository(testRepo);
        RepoPackage testRepoPackage =
                new RPMRepoPackage(NAME, VERSION1, RELEASE1, ARCHITECTURE, testRepo);
        Set<IMABaselineRecord> imaRecords = new HashSet<>();
        imaRecords.add(SimpleImaBaselineTest.createTestIMARecord(FILEPATH1));
        testRepoPackage.setAllMeasurements(imaRecords, RepoPackageTest.getTestDigest());
        repoManager.saveRepoPackage(testRepoPackage);
        Set<Repository<?>> originalRepositories = new HashSet<>();
        originalRepositories.add(testRepo);
        testBaseline.setRepositories(originalRepositories);

        DBBaselineManager baselineManager = new DBBaselineManager(sessionFactory);

        BroadRepoImaBaseline baseline2 =
                (BroadRepoImaBaseline) baselineManager.save(testBaseline);
        Assert.assertEquals(baseline2, testBaseline);
        Assert.assertTrue(DBBaselineManagerTest.isInDatabase(BASELINE_NAME, sessionFactory));
    }

    /**
     * Tests that a <code>BroadRepoImaBaseline</code> can be archived.
     */
    @Test
    public final void testArchiveBaseline() {
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        LOGGER.debug("archive IMA baseline test started");

        baseline = new BroadRepoImaBaseline(BASELINE_NAME);
        mgr.saveBaseline(baseline);
        mgr.archive(baseline.getName());
        BroadRepoImaBaseline retrievedBaseline =
                (BroadRepoImaBaseline) mgr.getBaseline(baseline.getName());
        Assert.assertTrue(retrievedBaseline.isArchived());
    }

    /**
     * Tests that an updated baseline is saved correctly in the database.
     *
     * @throws UnsupportedEncodingException
     *             if error encountered while creating the test digest
     */
    @Test
    public final void testUpdateSavedBroadRepoImaBaseline() throws UnsupportedEncodingException {
        BroadRepoImaBaseline testBaseline = new BroadRepoImaBaseline(BASELINE_NAME);
        DBBaselineManager baselineManager = new DBBaselineManager(sessionFactory);
        BroadRepoImaBaseline savedBaseline =
                (BroadRepoImaBaseline) baselineManager.save(testBaseline);
        Assert.assertEquals(savedBaseline, testBaseline);
        Assert.assertTrue(DBBaselineManagerTest.isInDatabase(BASELINE_NAME, sessionFactory));

        Repository testRepo = new TestRepository("Test Repository", 0);
        DBRepositoryManager repoManager = new DBRepositoryManager(sessionFactory);
        testRepo = repoManager.saveRepository(testRepo);
        Set<Repository<?>> originalRepositories = new HashSet<>();
        originalRepositories.add(testRepo);
        savedBaseline.setRepositories(originalRepositories);

        baselineManager.updateBaseline(savedBaseline);

        BroadRepoImaBaseline updatedBaseline =
                (BroadRepoImaBaseline) baselineManager.getBaseline(BASELINE_NAME);

        Assert.assertEquals(updatedBaseline, savedBaseline);

        Assert.assertEquals(updatedBaseline.getRepositories().size(), 1);
        Assert.assertEquals(updatedBaseline.getRepositories(),
                savedBaseline.getRepositories());
    }
}
