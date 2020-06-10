package hirs.data.persist;

import hirs.data.persist.baseline.IMABaselineRecord;
import hirs.data.persist.baseline.TargetedRepoImaBaseline;
import hirs.data.persist.baseline.Baseline;
import hirs.data.persist.enums.ReportMatchStatus;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import hirs.ima.matching.BatchImaMatchStatus;
import hirs.ima.matching.IMAMatchStatus;
import hirs.persist.BaselineManager;
import hirs.persist.DBBaselineManager;
import hirs.persist.DBRepositoryManager;
import hirs.persist.DbImaBaselineRecordManager;
import hirs.persist.RepositoryManager;
import hirs.repository.RPMRepoPackage;
import hirs.repository.RepoPackage;
import hirs.repository.Repository;
import hirs.repository.RepositoryException;
import hirs.repository.RepositoryUpdateService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import hirs.persist.DBBaselineManagerTest;
import hirs.persist.DBUtility;
import hirs.repository.RepoPackageTest;
import hirs.repository.TestRepository;

/**
 * Unit tests for the <code>TargetedRepoImaBaseline</code> class.
 */
public class TargetedRepoImaBaselineTest extends SpringPersistenceTest {

    private static final Logger LOGGER = LogManager.getLogger(TargetedRepoImaBaseline.class);
    private static final int ONE_HUNDRED = 100;
    private static final int NUM_PACKAGES_IN_REPO = 3;
    private static final String NAME = "test-package";
    private static final String VERSION1 = "1.1.1";
    private static final String VERSION2 = "2.2.2";
    private static final String RELEASE1 = "75";
    private static final String RELEASE2 = "77";
    private static final String ARCHITECTURE = "x86";
    private static final String BASELINE_NAME = "test targeted repo baseline";
    private static final Integer PACKAGE_COUNT = 3;
    private static final String FILEPATH1 = "/original/test/path.file";
    private static final String FILEPATH2 = "/update1/test/path.file";
    private static final String FILEPATH3 = "/update2/test/path.file";
    private static final String BIN_FILE = "/bin/ls";
    private static final String USR_BIN_FILE = "/usr/bin/ls";
    private Repository repo;
    private RepoPackage repoPackage;
    private RepoPackage update1;
    private RepoPackage update2;
    private TargetedRepoImaBaseline baseline;

    /**
     * Initializes a <code>TargetedRepoImaBaseline</code>, some <code>RepoPackage</code>s with
     * records, and a mock repository for updates.
     *
     * @throws Exception if creating a RepoPackage fails.
     */
    @BeforeMethod
    public final void setUp() throws Exception {
        baseline = new TargetedRepoImaBaseline(BASELINE_NAME);
        repo = Mockito.mock(Repository.class);
        repoPackage = new RPMRepoPackage(NAME, VERSION1, RELEASE1, ARCHITECTURE, repo);
        Set<IMABaselineRecord> imaRecords = new HashSet<>();
        imaRecords.add(SimpleImaBaselineTest.createTestIMARecord(FILEPATH1));
        repoPackage.setAllMeasurements(imaRecords, RepoPackageTest.getTestDigest());
        Set<RepoPackage> originalPackages = new HashSet<>();
        originalPackages.add(repoPackage);
        baseline.setRepoPackages(originalPackages);
        update1 = new RPMRepoPackage(NAME, VERSION2, RELEASE1, ARCHITECTURE, repo);
        imaRecords.clear();
        imaRecords.add(SimpleImaBaselineTest.createTestIMARecord(FILEPATH2));
        update1.setAllMeasurements(imaRecords, RepoPackageTest.getTestDigest());
        update2 = new RPMRepoPackage(NAME, VERSION1, RELEASE2, ARCHITECTURE, repo);
        imaRecords.clear();
        imaRecords.add(SimpleImaBaselineTest.createTestIMARecord(FILEPATH3));
        update2.setAllMeasurements(imaRecords, RepoPackageTest.getTestDigest());
        Set<RepoPackage> updatedPackages = new HashSet<>();
        updatedPackages.add(update1);
        updatedPackages.add(update2);
        Mockito.when(repo.getUpdatedPackages(repoPackage)).thenReturn(updatedPackages);
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterClass
    public final void tearDown() {
        LOGGER.debug("closing session factory");
    }

    /**
     * Resets the test state between tests.
     */
    @AfterMethod
    public final void resetTestState() {
        LOGGER.debug("reset test state");
        LOGGER.debug("deleting all baselines");
        DBUtility.removeAllInstances(sessionFactory, Baseline.class);
        DBUtility.removeAllInstances(sessionFactory, Repository.class);
        LOGGER.debug("all baselines and repos removed");
    }

    /**
     * Tests the only constructor, which takes in the baseline name as a string.
     */
    @Test
    public final void testNamedConstructor() {
        Assert.assertTrue(baseline.getName().equals(BASELINE_NAME));
        Assert.assertEquals(baseline.getRepositoryPackages().size(), 1);
    }

    /**
     * Tests that each <code>RepoPackage</code> in this baseline can be updated from its
     * associated <code>Repository</code>.
     */
    @Test
    public final void testUpdate() {
        Assert.assertEquals(repo.getUpdatedPackages(repoPackage).size(), 2);
        baseline.update(null);
        Assert.assertEquals((Integer) baseline.getRepositoryPackages().size(), PACKAGE_COUNT);
    }

    /**
     * Tests that each <code>RepoPackage</code> in this baseline can be updated from its
     * associated <code>Repository</code> that has been persisted (and are not currently held
     * in memory by the baseline).
     *
     * @throws IOException if there is a problem generating the update job
     * @throws InterruptedException if the Thread's sleep is interrupted
     * @throws RepositoryException a Repository exception occurs
     */
    @Test
    public final void testUpdatePersistedRepository() throws IOException, RepositoryException,
            InterruptedException {
        BaselineManager baselineManager = new DBBaselineManager(sessionFactory);
        RepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        RepositoryUpdateService updateService = RepositoryUpdateService.getInstance();

        baseline.setRepoPackages(new HashSet<>());
        baselineManager.saveBaseline(baseline);

        TestRepository testRepo = new TestRepository("Test Repo");
        repoMan.saveRepository(testRepo);

        // will persist one package to repository
        testRepo.setNumRemotePackages(1);
        updateService.startUpdateJob(testRepo, 1, repoMan);
        while (updateService.getActiveJobs().size() > 0) {
            Thread.sleep(ONE_HUNDRED);
        }

        // manually add first package to targeted baseline
        TargetedRepoImaBaseline retrievedBaseline = (TargetedRepoImaBaseline)
                baselineManager.getBaseline(baseline.getName());
        retrievedBaseline.addRepoPackage(new ArrayList<RepoPackage>(
                repoMan.getRepository(testRepo.getId()).getPackages()).get(0)
        );
        baselineManager.updateBaseline(retrievedBaseline);

        // put 'updated' packages in repository
        testRepo.setNumRemotePackages(NUM_PACKAGES_IN_REPO);
        RepositoryUpdateService.getInstance().startUpdateJob(testRepo, 1, repoMan);
        while (updateService.getActiveJobs().size() > 0) {
            Thread.sleep(ONE_HUNDRED);
        }

        // retrieve baseline
        retrievedBaseline = (TargetedRepoImaBaseline)
                baselineManager.getBaseline(baseline.getName());

        // update targeted baseline
        retrievedBaseline.update(repoMan);

        Assert.assertEquals(retrievedBaseline.getRepositoryPackages().size(), NUM_PACKAGES_IN_REPO);
    }

    /**
     * Tests that a baseline contains the correct records before and after an update.
     */
    @Test
    public final void testGetMeasurementRecords() {
        Set<IMABaselineRecord> retrievedRecords = baseline.getBaselineRecords();
        Assert.assertEquals(retrievedRecords.size(), 1);
        for (IMABaselineRecord retrievedRecord : retrievedRecords) {
            Assert.assertTrue(retrievedRecord.getPath().equals(FILEPATH1));
        }
        baseline.update(null);
        retrievedRecords = baseline.getBaselineRecords();
        Assert.assertEquals((Integer) retrievedRecords.size(), PACKAGE_COUNT);
        for (IMABaselineRecord retrievedRecord : retrievedRecords) {
            Assert.assertTrue(retrievedRecord.getPath().equals(FILEPATH1)
                           || retrievedRecord.getPath().equals(FILEPATH2)
                           || retrievedRecord.getPath().equals(FILEPATH3));
        }
    }

    /**
     * Tests that the <code>Set</code> of <code>RepoPackage</code>s associated with this baseline
     * can be set, retrieved, and returned.
     */
    @Test
    public final void testGetSetRepoPackages() {
        Set<RepoPackage> retrievedPackages = baseline.getRepositoryPackages();
        Assert.assertTrue(retrievedPackages.contains(repoPackage));
        Set<RepoPackage> addedPackages = new HashSet<>();
        addedPackages.add(update1);
        baseline.setRepoPackages(addedPackages);
        retrievedPackages = baseline.getRepositoryPackages();
        Assert.assertFalse(retrievedPackages.contains(repoPackage));
        Assert.assertTrue(retrievedPackages.contains(update1));
    }

    /**
     * Tests that a TargetedRepoImaBaseline is able to be saved.
     */
    @Test
    public final void testSaveTargetedRepoImaBaseline() {
        TargetedRepoImaBaseline testBaseline = new TargetedRepoImaBaseline(BASELINE_NAME);
        DBBaselineManager baselineManager = new DBBaselineManager(sessionFactory);
        TargetedRepoImaBaseline baseline2 =
                (TargetedRepoImaBaseline) baselineManager.save(testBaseline);
        Assert.assertEquals(baseline2, testBaseline);
        Assert.assertTrue(DBBaselineManagerTest.isInDatabase(BASELINE_NAME, sessionFactory));
    }

    /**
     * Tests that the TargetedRepoImaBaseline with a RepoPackage is able to be saved.
     *
     * @throws UnsupportedEncodingException
     *             if an error is encountered while getting the test digest
     */
    @Test
    public final void testSaveTargetedRepoImaBaselineWithRepoPackage()
            throws UnsupportedEncodingException {
        TargetedRepoImaBaseline testBaseline = new TargetedRepoImaBaseline(BASELINE_NAME);
        Repository testRepo = new TestRepository("Test Repository", 0);
        DBRepositoryManager repoManager = new DBRepositoryManager(sessionFactory);
        testRepo = repoManager.saveRepository(testRepo);
        RepoPackage testRepoPackage =
                new RPMRepoPackage(NAME, VERSION1, RELEASE1, ARCHITECTURE, testRepo);
        Set<IMABaselineRecord> imaRecords = new HashSet<>();
        imaRecords.add(SimpleImaBaselineTest.createTestIMARecord(FILEPATH1));
        testRepoPackage.setAllMeasurements(imaRecords, RepoPackageTest.getTestDigest());
        repoManager.saveRepoPackage(testRepoPackage);
        Set<RepoPackage> originalPackages = new HashSet<>();
        originalPackages.add(testRepoPackage);
        testBaseline.setRepoPackages(originalPackages);

        DBBaselineManager baselineManager = new DBBaselineManager(sessionFactory);

        TargetedRepoImaBaseline baseline2 =
                (TargetedRepoImaBaseline) baselineManager.save(testBaseline);
        Assert.assertEquals(baseline2, testBaseline);
        Assert.assertTrue(DBBaselineManagerTest.isInDatabase(BASELINE_NAME, sessionFactory));
    }

    /**
     * Tests the basic functionality of the contains() method.
     *
     * @throws UnsupportedEncodingException
     *             if an error is encountered while getting the test digest
     */
    @Test
    public final void testContains() throws UnsupportedEncodingException {
        TargetedRepoImaBaseline testBaseline = new TargetedRepoImaBaseline(BASELINE_NAME);
        Repository testRepo = new TestRepository("Test Repository", 0);
        DBRepositoryManager repoManager = new DBRepositoryManager(sessionFactory);
        testRepo = repoManager.saveRepository(testRepo);
        RepoPackage testRepoPackage =
                new RPMRepoPackage(NAME, VERSION1, RELEASE1, ARCHITECTURE, testRepo);
        Set<IMABaselineRecord> imaRecords = new HashSet<>();
        imaRecords.add(SimpleImaBaselineTest.createTestIMARecord(FILEPATH1));
        testRepoPackage.setAllMeasurements(imaRecords, RepoPackageTest.getTestDigest());
        repoManager.saveRepoPackage(testRepoPackage);
        Set<RepoPackage> originalPackages = new HashSet<>();
        originalPackages.add(testRepoPackage);
        testBaseline.setRepoPackages(originalPackages);

        DBBaselineManager baselineManager = new DBBaselineManager(sessionFactory);
        TargetedRepoImaBaseline savedBaseline =
                (TargetedRepoImaBaseline) baselineManager.save(testBaseline);

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
     * Tests the basic functionality of the contains() method.
     *
     * @throws UnsupportedEncodingException
     *             if an error is encountered while getting the test digest
     */
    @Test
    public final void testContainsEquivalentPath() throws UnsupportedEncodingException {
        TargetedRepoImaBaseline testBaseline = new TargetedRepoImaBaseline(BASELINE_NAME);
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
        Set<RepoPackage> originalPackages = new HashSet<>();
        originalPackages.add(testRepoPackage);
        testBaseline.setRepoPackages(originalPackages);

        DBBaselineManager baselineManager = new DBBaselineManager(sessionFactory);
        TargetedRepoImaBaseline savedBaseline =
                (TargetedRepoImaBaseline) baselineManager.save(testBaseline);

        IMABaselineRecord tempBaselineRecord =
                SimpleImaBaselineTest.createTestIMARecord(USR_BIN_FILE);
        IMAMeasurementRecord measurementRecord = new IMAMeasurementRecord(
                tempBaselineRecord.getPath(), tempBaselineRecord.getHash()
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
     * Test that ensures a TargetedRepoImaBaseline can correctly determine if
     * it contains any matching baseline records solely based upon a given measurement
     * record's hash.
     *
     * @throws UnsupportedEncodingException
     *             if an error is encountered while getting the test digest
     */
    @Test
    public final void containsHashes() throws UnsupportedEncodingException {
        TargetedRepoImaBaseline testBaseline = new TargetedRepoImaBaseline(BASELINE_NAME);
        Repository testRepo = new TestRepository("Test Repository", 0);
        DBRepositoryManager repoManager = new DBRepositoryManager(sessionFactory);
        testRepo = repoManager.saveRepository(testRepo);
        RepoPackage testRepoPackage =
                new RPMRepoPackage(NAME, VERSION1, RELEASE1, ARCHITECTURE, testRepo);
        Set<IMABaselineRecord> imaRecords = new HashSet<>();
        imaRecords.add(SimpleImaBaselineTest.createTestIMARecord(FILEPATH1));
        testRepoPackage.setAllMeasurements(imaRecords, RepoPackageTest.getTestDigest());
        repoManager.saveRepoPackage(testRepoPackage);
        Set<RepoPackage> originalPackages = new HashSet<>();
        originalPackages.add(testRepoPackage);
        testBaseline.setRepoPackages(originalPackages);

        DBBaselineManager baselineManager = new DBBaselineManager(sessionFactory);
        TargetedRepoImaBaseline savedBaseline =
                (TargetedRepoImaBaseline) baselineManager.save(testBaseline);

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
     * Test that ensures a TargetedRepoImaBaseline can correctly determine that
     * it does not contain any matching baseline records solely based upon a given measurement
     * record's hash.
     *
     * @throws UnsupportedEncodingException
     *             if an error is encountered while getting the test digest
     */
    @Test
    public final void containsHashesWithNoMatches() throws UnsupportedEncodingException {
        TargetedRepoImaBaseline testBaseline = new TargetedRepoImaBaseline(BASELINE_NAME);
        Repository testRepo = new TestRepository("Test Repository", 0);
        DBRepositoryManager repoManager = new DBRepositoryManager(sessionFactory);
        testRepo = repoManager.saveRepository(testRepo);
        RepoPackage testRepoPackage =
                new RPMRepoPackage(NAME, VERSION1, RELEASE1, ARCHITECTURE, testRepo);
        Set<IMABaselineRecord> imaRecords = new HashSet<>();
        imaRecords.add(SimpleImaBaselineTest.createTestIMARecord(FILEPATH1));
        testRepoPackage.setAllMeasurements(imaRecords, RepoPackageTest.getTestDigest());
        repoManager.saveRepoPackage(testRepoPackage);
        Set<RepoPackage> originalPackages = new HashSet<>();
        originalPackages.add(testRepoPackage);
        testBaseline.setRepoPackages(originalPackages);

        DBBaselineManager baselineManager = new DBBaselineManager(sessionFactory);
        TargetedRepoImaBaseline savedBaseline =
                (TargetedRepoImaBaseline) baselineManager.save(testBaseline);

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
     * Tests that a <code>TargetedRepoImaBaseline</code> can be archived.
     */
    @Test
    public final void testArchiveBaseline() {
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        LOGGER.debug("archive IMA baseline test started");

        baseline = new TargetedRepoImaBaseline(BASELINE_NAME);
        mgr.saveBaseline(baseline);
        mgr.archive(baseline.getName());
        TargetedRepoImaBaseline retrievedBaseline =
                (TargetedRepoImaBaseline) mgr.getBaseline(baseline.getName());
        Assert.assertTrue(retrievedBaseline.isArchived());
    }

    /**
     * Tests that an updated baseline is saved correctly in the database.
     *
     * @throws UnsupportedEncodingException
     *             if error encountered while creating the test digest
     */
    @Test
    public final void testUpdateSavedTargetedRepoImaBaseline() throws UnsupportedEncodingException {
        TargetedRepoImaBaseline testBaseline = new TargetedRepoImaBaseline(BASELINE_NAME);
        DBBaselineManager baselineManager = new DBBaselineManager(sessionFactory);
        TargetedRepoImaBaseline savedBaseline =
                (TargetedRepoImaBaseline) baselineManager.save(testBaseline);
        Assert.assertEquals(savedBaseline, testBaseline);
        Assert.assertTrue(DBBaselineManagerTest.isInDatabase(BASELINE_NAME, sessionFactory));

        Repository testRepo = new TestRepository("Test Repository", 0);
        DBRepositoryManager repoManager = new DBRepositoryManager(sessionFactory);
        testRepo = repoManager.saveRepository(testRepo);
        RepoPackage testRepoPackage =
                new RPMRepoPackage(NAME, VERSION1, RELEASE1, ARCHITECTURE, testRepo);
        Set<IMABaselineRecord> imaRecords = new HashSet<>();
        imaRecords.add(SimpleImaBaselineTest.createTestIMARecord(FILEPATH1));
        testRepoPackage.setAllMeasurements(imaRecords, RepoPackageTest.getTestDigest());
        repoManager.saveRepoPackage(testRepoPackage);
        Set<RepoPackage> originalPackages = new HashSet<>();
        originalPackages.add(testRepoPackage);
        savedBaseline.setRepoPackages(originalPackages);

        baselineManager.updateBaseline(savedBaseline);

        TargetedRepoImaBaseline updatedBaseline =
                (TargetedRepoImaBaseline) baselineManager.getBaseline(BASELINE_NAME);

        Assert.assertEquals(updatedBaseline, savedBaseline);

        Assert.assertEquals(updatedBaseline.getRepositoryPackages().size(), 1);
        Assert.assertEquals(updatedBaseline.getRepositoryPackages(),
                savedBaseline.getRepositoryPackages());
    }

    /**
     * Tests that the TargetedRepoImaBaseline with a RepoPackage is associated
     * with the package's corresponding Repository.
     *
     * @throws UnsupportedEncodingException
     *             if an error is encountered while getting the test digest
     */
    @Test
    public final void testRepoPackageAssociation()
            throws UnsupportedEncodingException {
        TargetedRepoImaBaseline testBaseline = new TargetedRepoImaBaseline(BASELINE_NAME);
        Repository testRepo = new TestRepository("Test Repository", 0);
        DBRepositoryManager repoManager = new DBRepositoryManager(sessionFactory);
        testRepo = repoManager.saveRepository(testRepo);
        RepoPackage testRepoPackage =
                new RPMRepoPackage(NAME, VERSION1, RELEASE1, ARCHITECTURE, testRepo);
        Set<IMABaselineRecord> imaRecords = new HashSet<>();
        imaRecords.add(SimpleImaBaselineTest.createTestIMARecord(FILEPATH1));
        testRepoPackage.setAllMeasurements(imaRecords, RepoPackageTest.getTestDigest());
        repoManager.saveRepoPackage(testRepoPackage);
        Set<RepoPackage> originalPackages = new HashSet<>();
        originalPackages.add(testRepoPackage);
        testBaseline.setRepoPackages(originalPackages);

        DBBaselineManager baselineManager = new DBBaselineManager(sessionFactory);

        TargetedRepoImaBaseline baseline2 =
                (TargetedRepoImaBaseline) baselineManager.save(testBaseline);
        Assert.assertEquals(baseline2, testBaseline);
        Assert.assertTrue(DBBaselineManagerTest.isInDatabase(BASELINE_NAME, sessionFactory));

        List<Repository> repositoryList = new ArrayList<>();
        repositoryList.add(testRepo);
        Assert.assertTrue(baseline2.isAssociatedWithRepositories(repositoryList));
        Assert.assertFalse(baseline2.isAssociatedWithRepositories(null));
    }

    /**
     * Tests that the TargetedRepoImaBaseline with no packages is not associated with a repository
     * with the package's corresponding Repository.
     *
     * @throws UnsupportedEncodingException
     *             if an error is encountered while getting the test digest
     */
    @Test
    public final void testRepoPackageAssociationForBaselineWithNoPackages()
            throws UnsupportedEncodingException {
        TargetedRepoImaBaseline testBaseline = new TargetedRepoImaBaseline(BASELINE_NAME);
        Repository testRepo = new TestRepository("Test Repository", 0);
        DBRepositoryManager repoManager = new DBRepositoryManager(sessionFactory);
        testRepo = repoManager.saveRepository(testRepo);

        DBBaselineManager baselineManager = new DBBaselineManager(sessionFactory);

        TargetedRepoImaBaseline baseline2 =
                (TargetedRepoImaBaseline) baselineManager.save(testBaseline);
        Assert.assertEquals(baseline2, testBaseline);
        Assert.assertTrue(DBBaselineManagerTest.isInDatabase(BASELINE_NAME, sessionFactory));

        List<Repository> repositoryList = new ArrayList<>();
        repositoryList.add(testRepo);
        Assert.assertFalse(baseline2.isAssociatedWithRepositories(repositoryList));
        Assert.assertFalse(baseline2.isAssociatedWithRepositories(null));
    }
}
