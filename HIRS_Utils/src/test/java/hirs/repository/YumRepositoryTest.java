package hirs.repository;

import static org.apache.logging.log4j.LogManager.getLogger;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import hirs.data.persist.baseline.IMABaselineRecord;
import hirs.data.persist.SpringPersistenceTest;
import hirs.persist.DBRepositoryManager;
import hirs.persist.RepositoryManager;

import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import hirs.data.persist.ImaBaselineRecordTest;
import hirs.persist.DBUtility;

/**
 * Test cases for <code>YumRepository</code>.
 * <p>
 * These test cases use a manually created local repository that contains three packages;
 * hello_world-1.0, hello_world-2.0, and hello_world-3.0. These packages simply contain three
 * versions of the source for a simple hello world C program and the Makefiles to compile them with
 * gcc. If you were to install the packages the hello world binary would be installed to
 * "/tmp/hello" and the hashes of those binaries would be the hashes below.
 */

public final class YumRepositoryTest extends SpringPersistenceTest {

    private static final int PACKAGE_COUNT = 3;
    private static final String PACKAGE_NAME = "hello_world";
    private static final String PACKAGE_ONE_VERSION = "1.0";
    private static final String PACKAGE_TWO_VERSION = "2.0";
    private static final String PACKAGE_THREE_VERSION = "3.0";
    private static final String PACKAGE_ONE_RELEASE = "1.el6";
    private static final String PACKAGE_TWO_RELEASE = "2.el6";
    private static final String PACKAGE_THREE_RELEASE = "3.el6";
    private static final String PACKAGE_ARCHITECTURE = "x86_64";
    private static final String BIN_PATH = "/tmp/hello";
    private static final String HASH_ONE = "e68d50b1a81359a5e6a37813d475ece07ce3a850";
    private static final String HASH_TWO = "a3daaef484ab610c06d5150755046c97bfdaf7d3";
    private static final String HASH_THREE = "f0d05b42b06d04c4b051ac6da002a2d8488d4253";
    private static final String BASE_PATH = "/testrepo";
    private static final String REPO_NAME = "testrepo";
    private static final String GPG_KEY = "my_secret_key";
    private static final boolean GPG_CHECK = false;
    private static final Logger LOGGER = getLogger(YumRepositoryTest.class);

    private YumRepository repo;
    private Set<IMABaselineRecord> requiredRecords;
    private IMABaselineRecord recordOne;
    private IMABaselineRecord recordTwo;
    private IMABaselineRecord recordThree;
    private RepoPackage packageOne;
    private RepoPackage packageTwo;
    private RepoPackage packageThree;
    private URL baseUrl;
    private String fullUrlPath;

    /**
     * Initializes a repository for use in tests.
     */
    @BeforeClass
    public void setup() {
        baseUrl = this.getClass().getResource(BASE_PATH);
        fullUrlPath = baseUrl.toString();

        try {
            // make a test repo with the local url and no gpg checks
            repo = new YumRepository(REPO_NAME, null, baseUrl, GPG_CHECK, null);
        } catch (Exception e) {
            LOGGER.error("unexpected exception", e);
            throw new RuntimeException("unexpected exception", e);
        }

        // generate some test baseline records using the actual hash of RPM files
        recordOne = new IMABaselineRecord(BIN_PATH, ImaBaselineRecordTest.getDigest(HASH_ONE));
        recordTwo = new IMABaselineRecord(BIN_PATH, ImaBaselineRecordTest.getDigest(HASH_TWO));
        recordThree = new IMABaselineRecord(BIN_PATH, ImaBaselineRecordTest.getDigest(HASH_THREE));

        // populate a set of records that we know should be in the packages
        requiredRecords = new HashSet<IMABaselineRecord>();
        requiredRecords.add(recordOne);
        requiredRecords.add(recordTwo);
        requiredRecords.add(recordThree);

        // initialize empty copies of the packages in the test repository
        packageOne = new RPMRepoPackage(PACKAGE_NAME, PACKAGE_ONE_VERSION, PACKAGE_ONE_RELEASE,
                PACKAGE_ARCHITECTURE, repo);
        packageTwo = new RPMRepoPackage(PACKAGE_NAME, PACKAGE_TWO_VERSION, PACKAGE_TWO_RELEASE,
                PACKAGE_ARCHITECTURE, repo);
        packageThree = new RPMRepoPackage(PACKAGE_NAME, PACKAGE_THREE_VERSION,
                PACKAGE_THREE_RELEASE, PACKAGE_ARCHITECTURE, repo);
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterClass
    public void tearDown() {

    }

    /**
     * Resets the test state to a known good state. This resets the database by removing all
     * <code>Repository</code> and <code>RepoPackage</code> objects. It also re-initializes the
     * repository instance.
     */
    @AfterMethod
    public void resetTestState() {
        DBUtility.removeAllInstances(sessionFactory, YumRepository.class);
        DBUtility.removeAllInstances(sessionFactory, RepoPackage.class);

        try {
            repo = new YumRepository(REPO_NAME, null, baseUrl, GPG_CHECK, null);
        } catch (Exception e) {
            LOGGER.error("unexpected exception", e);
            throw new RuntimeException("unexpected exception", e);
        }
    }

    /**
     * Test that the ScheduleJobInfo can be returned.
     */
    @Test
    public void testGetScheduledJobInfo() {
        YumRepository repository;
        try {
            repository = new YumRepository(REPO_NAME, new URL(fullUrlPath), null, GPG_CHECK, null);
        } catch (Exception e) {
            LOGGER.error("unexpected exception", e);
            throw new RuntimeException("unexpected exception", e);
        }
        Assert.assertNotNull(repository.getScheduledJobInfo());
    }

    /**
     * Test that the mirror list URL can be returned.
     */
    @Test
    public void testGetMirrorListUrl() {
        YumRepository repository;
        try {
            repository = new YumRepository(REPO_NAME, new URL(fullUrlPath), null, GPG_CHECK, null);
        } catch (Exception e) {
            LOGGER.error("unexpected exception", e);
            throw new RuntimeException("unexpected exception", e);
        }
        Assert.assertEquals(repository.getMirrorListUrl().toString(), fullUrlPath);
    }

    /**
     * Tests that the base URL can be returned.
     */
    @Test
    public void testGetBaseUrl() {
        YumRepository repository;
        try {
            repository = new YumRepository(REPO_NAME, null, new URL(fullUrlPath), GPG_CHECK, null);
        } catch (Exception e) {
            LOGGER.error("unexpected exception", e);
            throw new RuntimeException("unexpected exception", e);
        }

        Assert.assertEquals(repository.getBaseUrl().toString(), fullUrlPath);
    }

    /**
     * Tests that the GPG enable flag can be checked.
     */
    @Test
    public void testIsGpgCheck() {
        Assert.assertEquals(repo.isGpgCheck(), GPG_CHECK);
    }

    /**
     * Tests that the GPG key can be returned.
     */
    @Test
    public void testGetGpgKey() {
        YumRepository repository;
        try {
            repository = new YumRepository(REPO_NAME, null, baseUrl, GPG_CHECK, GPG_KEY);
        } catch (Exception e) {
            LOGGER.error("unexpected exception", e);
            throw new RuntimeException("unexpected exception", e);
        }
        Assert.assertEquals(repository.getGpgKey(), GPG_KEY);
    }

    /**
     * Tests that a repository can list its remote packages. This one should only have two packages
     * both called "hello".
     *
     * @throws RepositoryException if unable to communicate with repo
     */
    @Test(groups = { "rhel-6" })
    public void testListRemotePackages() throws RepositoryException {
        final Set<RPMRepoPackage> packages = repo.listRemotePackages();
        Assert.assertNotNull(packages);
        Assert.assertTrue(packages.size() == PACKAGE_COUNT);
        for (RPMRepoPackage repoPackage : packages) {
            Assert.assertEquals(repoPackage.getName(), PACKAGE_NAME);
        }
    }

    /**
     * Tests that a repository can have all of its packages measured.
     *
     * @throws RepositoryException if unable to measure all of the packages
     */
    @Test(groups = { "rhel-6" })
    public void testMeasurePackages() throws RepositoryException {

        // retrieve the list of remote packages
        final Set<RPMRepoPackage> remotePackages = repo.listRemotePackages();
        Assert.assertEquals(remotePackages.size(), PACKAGE_COUNT);

        // measure each of the packages and collect all of the records from them
        Set<IMABaselineRecord> foundRecords = new HashSet<IMABaselineRecord>();
        for (RPMRepoPackage remotePackage : remotePackages) {
            repo.measurePackage(remotePackage);
            Assert.assertNotNull(remotePackage.getPackageRecords());
            foundRecords.addAll(remotePackage.getPackageRecords());
            LOGGER.debug("measured package {} has records: {}",
                    remotePackage, remotePackage.getPackageRecords());
        }

        // assert that we found exactly the records we required
        Assert.assertEquals(foundRecords, requiredRecords);
    }



    /**
     * Tests that getUpdatedPackages returns all of the newer versions of an old package.
     *
     * @throws RepositoryException if unable to measure all the packages
     */
    @Test(groups = { "rhel-6" })
    public void testGetUpdatedPackages() throws RepositoryException {

        // save the repository to the database
        RepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        repoMan.saveRepository(repo);

        // measure and save each package
        for (RPMRepoPackage remotePackage : repo.listRemotePackages()) {
            repo.measurePackage(remotePackage);
            repoMan.saveRepoPackage(remotePackage);
        }

        // load the repository using its name, which will populate the packages from the database
        YumRepository loadedRepo = (YumRepository) repoMan.getRepository(REPO_NAME);

        // check that the repository returns the correct updated packages
        Set<RepoPackage> correctPackages = new HashSet<RepoPackage>();
        correctPackages.add(packageTwo);
        correctPackages.add(packageThree);
        Assert.assertEquals(loadedRepo.getUpdatedPackages(packageOne), correctPackages);
        correctPackages.remove(packageTwo);
        Assert.assertEquals(loadedRepo.getUpdatedPackages(packageTwo), correctPackages);
        correctPackages.remove(packageThree);
        Assert.assertEquals(loadedRepo.getUpdatedPackages(packageThree), correctPackages);
    }

    /**
     * Tests that packages contain the correct records once the repository has been saved and
     * loaded from the database.
     *
     * @throws RepositoryException if unable to measure all the packages
     */
    @Test(groups = { "rhel-6" })
    public void testSaveGetPackages() throws RepositoryException {

        // save the repository to the database
        RepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        repoMan.saveRepository(repo);

        // measure and save each package
        for (RPMRepoPackage remotePackage : repo.listRemotePackages()) {
            repo.measurePackage(remotePackage);
            repoMan.saveRepoPackage(remotePackage);
        }

        // load the repository using its name
        YumRepository loadedRepo = (YumRepository) repoMan.getRepository(REPO_NAME);

        // get the empty (not measured) packages
        Set<RPMRepoPackage> loadedPackages = loadedRepo.getPackages();

        // load the fully measured packages and collect all the records from them
        Set<IMABaselineRecord> foundRecords = new HashSet<IMABaselineRecord>();
        for (RepoPackage loadedPackage : loadedPackages) {
            RepoPackage fullPackage = repoMan.getRepoPackage(loadedPackage.getId());
            foundRecords.addAll(fullPackage.getPackageRecords());
        }

        // assert that we found exactly the records we required
        Assert.assertEquals(foundRecords, requiredRecords);
    }
}
