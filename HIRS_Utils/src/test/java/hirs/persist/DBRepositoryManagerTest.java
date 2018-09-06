package hirs.persist;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hirs.data.persist.IMABaselineRecord;
import hirs.data.persist.SpringPersistenceTest;
import hirs.repository.RPMRepoPackage;
import hirs.repository.RepoPackage;
import hirs.repository.Repository;
import hirs.repository.YumRepository;
import hirs.repository.spacewalk.Credentials;
import hirs.repository.spacewalk.SpacewalkChannelRepository;

import org.hibernate.LazyInitializationException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import hirs.repository.RepoPackageTest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * This class holds unit tests for <code>RepositoryManager</code> and
 * <code>DBRepositoryManager</code>.
 */
public class DBRepositoryManagerTest extends SpringPersistenceTest {
    private static final String TEST_USER_NAME = "ut-user";
    private static final String TEST_PASSWORD = "ut-pass";

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
     * Resets the test state to a known good state. This currently only resets
     * the database by removing all <code>Repository</code> and <code>RepoPackage</code> objects.
     */
    @AfterMethod
    public final void resetTestState() {
        DBUtility.removeAllInstances(sessionFactory, YumRepository.class);
        DBUtility.removeAllInstances(sessionFactory, SpacewalkChannelRepository.class);
    }

    /**
     * Creates a Repository for testing purposes.
     *
     * @return a Repository instance
     * @throws IOException if the Repository cannot be instantiated
     */
    public static Repository getTestRepository() throws IOException {
        return new YumRepository(
                "TestRepo",
                new URL("http://example.com"),
                new URL("http://example.com"),
                false,
                null
        );
    }

    /**
     * Creates a Repository for testing purposes.
     *
     * @return a Repository instance
     * @throws IOException if the Repository cannot be instantiated
     */
    public static SpacewalkChannelRepository getTestSpacewalkRepository() throws IOException {
        return new SpacewalkChannelRepository(
                "SpaceRepChan",
                new URL("https://example.com"),
                "test-chan"
        );
    }

    /**
     * Gets a Credentials for testing.
     * @return a Credentials instance
     */
    public static Credentials getTestCredentials() {
        return new Credentials(TEST_USER_NAME, TEST_PASSWORD);
    }

    /**
     * Creates a RepoPackage for testing purposes.
     *
     * @param repo the Repository that contains this package
     * @return a RepoPackage instance
     */
    public static RepoPackage getTestRepoPackage(final Repository repo) {
        return new RPMRepoPackage(
                "TestRepoPackage",
                "1.0",
                "1",
                "x86",
                repo
        );
    }

    /**
     * Creates an IMABaselineRecord for testing purposes.
     *
     * @return an IMABaselineRecord instance
     * @throws UnsupportedEncodingException if UTF-8 is not supported on this platform
     */
    public static IMABaselineRecord getTestIMABaselineRecord() throws UnsupportedEncodingException {
        return new IMABaselineRecord("/tmp/test", RepoPackageTest.getTestDigest());
    }

    /**
     * Tests that a Repository can be persisted to a database.
     *
     * @throws IOException if an error is encountered while using a Repository
     */
    @Test
    public final void testSaveRepository() throws IOException {
        DBRepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        repoMan.saveRepository(getTestRepository());
    }

    /**
     * Tests that a Repository can be persisted and retrieved from a database.
     *
     * @throws IOException if an error is encountered while using a Repository
     */
    @Test
    public final void testSaveAndGetRepository() throws IOException {
        DBRepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        Repository savedRepo = repoMan.saveRepository(getTestRepository());
        Assert.assertEquals(savedRepo, repoMan.getRepository(savedRepo.getId()));
        Assert.assertNotNull(savedRepo.getScheduledJobInfo());
    }

    /**
     * Tests that a RepoPackage can be persisted to a database.
     *
     * @throws IOException if an error is encountered while using a Repository
     */
    @Test
    public final void testSaveRepoPackage() throws IOException {
        DBRepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        Repository savedRepo = repoMan.saveRepository(getTestRepository());
        RepoPackage repoPackage = getTestRepoPackage(savedRepo);
        Set<IMABaselineRecord> measurements = new HashSet<>();
        measurements.add(getTestIMABaselineRecord());
        repoPackage.setAllMeasurements(measurements, RepoPackageTest.getTestDigest());
        repoMan.saveRepoPackage(repoPackage);
    }

    /**
     * Tests that a RepoPackage can be persisted and retrieved from a database.
     *
     * @throws IOException if an error is encountered while using a Repository
     */
    @Test
    public final void testSaveAndGetRepoPackage() throws IOException {
        DBRepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        Repository savedRepo = repoMan.saveRepository(getTestRepository());
        RepoPackage repoPackage = getTestRepoPackage(savedRepo);
        Set<IMABaselineRecord> measurements = new HashSet<>();
        measurements.add(getTestIMABaselineRecord());
        repoPackage.setAllMeasurements(measurements, RepoPackageTest.getTestDigest());
        RepoPackage savedRepoPackage = repoMan.saveRepoPackage(repoPackage);
        Assert.assertEquals(savedRepoPackage, repoMan.getRepoPackage(savedRepoPackage.getId()));
        Assert.assertEquals(savedRepoPackage.getPackageRecords(), measurements);
        Assert.assertEquals(savedRepoPackage.getPackageMeasurement(),
                RepoPackageTest.getTestDigest());
    }

    /**
     * Tests that attempting to persist an unmeasured RepoPackage results in an exception being
     * thrown.
     *
     * @throws IOException if an error is encountered while using a Repository
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void testSaveUnmeasuredRepoPackage() throws IOException {
        DBRepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        Repository savedRepo = repoMan.saveRepository(getTestRepository());
        repoMan.saveRepoPackage(getTestRepoPackage(savedRepo));
    }

    /**
     * Tests that a Repository will gain an association to a RepoPackage if that RepoPackage is
     * persisted with a link back to its source repository.
     *
     * @throws IOException if an error is encountered while using a Repository
     */
    @Test
    public final void testAssociatePackageWithRepository() throws IOException {
        DBRepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        Repository savedRepo = repoMan.saveRepository(getTestRepository());
        RepoPackage repoPackage = getTestRepoPackage(savedRepo);
        Set<IMABaselineRecord> measurements = new HashSet<>();
        measurements.add(getTestIMABaselineRecord());
        repoPackage.setAllMeasurements(measurements, RepoPackageTest.getTestDigest());
        RepoPackage savedRepoPackage = repoMan.saveRepoPackage(repoPackage);
        Repository<?> retrievedRepo = repoMan.getRepository(savedRepo.getId());
        Assert.assertTrue(retrievedRepo.getPackages().contains(savedRepoPackage));
    }

    /**
     * Tests that if an attempt to save a Repository with a duplicate name, a DBManagerException is
     * thrown.
     *
     * @throws IOException if an error is encountered while using a Repository
     */
    @Test(expectedExceptions = DBManagerException.class)
    public final void testDuplicateRepositoryName() throws IOException {
        DBRepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        repoMan.saveRepository(getTestRepository());
        repoMan.saveRepository(getTestRepository());
    }

    /**
     * Tests that retrieving an unknown Repository returns null.
     */
    @Test
    public final void testGetUnknownRepository() {
        DBRepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        Assert.assertNull(repoMan.getRepository(UUID.randomUUID()));
    }

    /**
     * Tests that retrieving an unknown RepoPackage returns null.
     */
    @Test
    public final void testGetUnknownRepoPackage() {
        DBRepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        Assert.assertNull(repoMan.getRepoPackage(UUID.randomUUID()));
    }

    /**
     * Tests that attempting to retrieve the RepoPackages from a Repository in the list
     * returned from getRepositoryList will result in an exception.
     *
     * @throws IOException if an error is encountered while using a Repository
     */
    @Test(expectedExceptions = LazyInitializationException.class)
    @SuppressFBWarnings(
            value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT",
            justification = "The statement is meant to throw an exception"
    )
    public final void testGetRepoPackagesNotLoaded() throws IOException {
        DBRepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        Repository savedRepo = repoMan.saveRepository(getTestRepository());
        RepoPackage repoPackage = getTestRepoPackage(savedRepo);
        Set<IMABaselineRecord> measurements = new HashSet<>();
        measurements.add(getTestIMABaselineRecord());
        repoPackage.setAllMeasurements(measurements, RepoPackageTest.getTestDigest());
        repoMan.saveRepoPackage(repoPackage);
        List<Repository> repositories = repoMan.getRepositoryList(YumRepository.class);
        Assert.assertEquals(repositories.size(), 1);
        repositories.get(0).getPackages().size();
        Assert.fail("LazyLoadingException was not thrown");
    }

    /**
     * Tests that attempting to retrieve the RepoPackages from a Repository in the list
     * returned from getRepository will return the RepoPackages.
     *
     * @throws IOException if an error is encountered while using a Repository
     */
    @Test
    public final void testGetRepoPackagesLoaded() throws IOException {
        DBRepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        Repository savedRepo = repoMan.saveRepository(getTestRepository());
        RepoPackage repoPackage = getTestRepoPackage(savedRepo);
        Set<IMABaselineRecord> measurements = new HashSet<>();
        measurements.add(getTestIMABaselineRecord());
        repoPackage.setAllMeasurements(measurements, RepoPackageTest.getTestDigest());
        repoMan.saveRepoPackage(repoPackage);
        List<Repository> repositories = repoMan.getRepositoryList(YumRepository.class);
        Repository<?> repository = repoMan.getRepository(repositories.get(0).getId());
        Assert.assertEquals(repository.getPackages().size(), 1);
        Assert.assertEquals(repository.getPackages().iterator().next(), repoPackage);
    }

    /**
     * Tests that attempting to retrieve packageRecords from a RepoPackage returned from
     * getRepository will result in an exception.
     *
     * @throws IOException if an error is encountered while using a Repository
     */
    @Test(expectedExceptions = LazyInitializationException.class)
    public final void testRepoPackageRecordsNotLoaded() throws IOException {
        DBRepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        Repository savedRepo = repoMan.saveRepository(getTestRepository());
        RepoPackage repoPackage = getTestRepoPackage(savedRepo);
        Set<IMABaselineRecord> measurements = new HashSet<>();
        measurements.add(getTestIMABaselineRecord());
        repoPackage.setAllMeasurements(measurements, RepoPackageTest.getTestDigest());
        repoMan.saveRepoPackage(repoPackage);
        List<Repository> repositories = repoMan.getRepositoryList(YumRepository.class);
        Repository<?> repository = repoMan.getRepository(repositories.get(0).getId());
        for (RepoPackage pkg : repository.getPackages()) {
            pkg.getPackageRecords();
        }
        Assert.fail("LazyLoadingException was not thrown");
    }

    /**
     * Tests that attempting to retrieve packageRecords from a RepoPackage returned from
     * getRepoPackage will return the records.
     *
     * @throws IOException if an error is encountered while using a Repository
     */
    @Test
    public final void testRepoPackageRecordsLoaded() throws IOException {
        DBRepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        Repository savedRepo = repoMan.saveRepository(getTestRepository());
        RepoPackage repoPackage = getTestRepoPackage(savedRepo);
        Set<IMABaselineRecord> measurements = new HashSet<>();
        measurements.add(getTestIMABaselineRecord());
        repoPackage.setAllMeasurements(measurements, RepoPackageTest.getTestDigest());
        repoMan.saveRepoPackage(repoPackage);
        List<Repository> repositories = repoMan.getRepositoryList(YumRepository.class);
        Repository<?> repository = repoMan.getRepository(repositories.get(0).getId());
        RepoPackage packageWithRecords = repoMan.getRepoPackage(
                repository.getPackages().iterator().next().getId()
        );
        Assert.assertEquals(
                packageWithRecords.getPackageRecords(), repoPackage.getPackageRecords()
        );
        Assert.assertEquals(
                packageWithRecords.getPackageMeasurement(), repoPackage.getPackageMeasurement()
        );
    }

    /**
     * Tests that a repository can be updated.
     *
     * @throws IOException if an error is encountered while using a Repository
     */
    @Test
    public final void testUpdateRepository() throws IOException {
        DBRepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        Repository savedRepo = repoMan.saveRepository(getTestRepository());
        RepoPackage repoPackage = getTestRepoPackage(savedRepo);
        Set<IMABaselineRecord> measurements = new HashSet<>();
        measurements.add(getTestIMABaselineRecord());
        repoPackage.setAllMeasurements(measurements, RepoPackageTest.getTestDigest());
        repoMan.saveRepoPackage(repoPackage);
        Repository repositoryWithPackage = repoMan.getRepository(savedRepo.getId());

        RepoPackage newRepoPackage = new RPMRepoPackage(repoPackage.getName(),
                "2.0", repoPackage.getRelease(), repoPackage.getArchitecture(),
                repoPackage.getSourceRepository()
        );
        measurements.clear();
        measurements.add(getTestIMABaselineRecord());
        newRepoPackage.setAllMeasurements(measurements, RepoPackageTest.getTestDigest());
        repoMan.saveRepoPackage(newRepoPackage);

        repositoryWithPackage.setName("New name");
        repoMan.updateRepository(repositoryWithPackage);
        repositoryWithPackage = repoMan.getRepository(savedRepo.getId());
        Assert.assertEquals(repositoryWithPackage.getName(), "New name");
        Assert.assertEquals(repositoryWithPackage.getPackages().size(), 2);
    }

    /**
     * Tests that a spacewalk repository can be created and does not store the authentication
     * information as configured.
     * @throws IOException if an error is encountered while using a Repository
     */
    @Test
    public final void testCreateSpacewalkRepositoryWithoutStoredAuth() throws IOException {
        DBRepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        SpacewalkChannelRepository repo = getTestSpacewalkRepository();
        repo.setCredentials(getTestCredentials(), false);

        repoMan.saveRepository(repo);
        SpacewalkChannelRepository retrievedRepo =
                (SpacewalkChannelRepository) repoMan.getRepository(repo.getId());

        Assert.assertNull(retrievedRepo.getCredentials());
    }

    /**
     * Tests that a Spacewalk repository persists the authentication information if configured.
     * @throws IOException if an error is encountered while using a Repository
     */
    @Test
    public final void testCreateSpacewalkRepositoryWithStoredAuth() throws IOException {
        DBRepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        SpacewalkChannelRepository repo = getTestSpacewalkRepository();
        repo.setCredentials(getTestCredentials(), true);

        repoMan.saveRepository(repo);
        SpacewalkChannelRepository retrievedRepo =
                (SpacewalkChannelRepository) repoMan.getRepository(repo.getId());

        Credentials auth = retrievedRepo.getCredentials();
        Assert.assertEquals(auth.getUserName(), TEST_USER_NAME);
        Assert.assertEquals(auth.getPassword(), TEST_PASSWORD);
    }

    /**
     * Verifies the unique constraints defined for a Spacewalk repo. Creating the second
     * repository should fail becuase it is not a unique URL / channel tupple.
     * @throws IOException if an error is encountered while using a Repository
     */
    @Test(expectedExceptions = DBManagerException.class)
    public final void verifySpacewalkRepositoryUniqueConstraints() throws IOException {
        DBRepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        SpacewalkChannelRepository repo1 = getTestSpacewalkRepository();
        SpacewalkChannelRepository repo2 = getTestSpacewalkRepository();

        repoMan.saveRepository(repo1);
        repoMan.saveRepository(repo2);
    }

    /**
     * Tests that a repository can be deleted, and that when it is deleted, its RepoPackages are
     * also deleted.
     *
     * @throws IOException if an error is encountered while using a Repository
     */
    @Test
    public final void testDeleteRepository() throws IOException {
        DBRepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        Repository savedRepo = repoMan.saveRepository(getTestRepository());
        RepoPackage repoPackage = getTestRepoPackage(savedRepo);
        Set<IMABaselineRecord> measurements = new HashSet<>();
        measurements.add(getTestIMABaselineRecord());
        repoPackage.setAllMeasurements(measurements, RepoPackageTest.getTestDigest());
        repoMan.saveRepoPackage(repoPackage);
        Repository repositoryWithPackage = repoMan.getRepository(savedRepo.getId());
        repoMan.deleteRepository(repositoryWithPackage);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, YumRepository.class), 0);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, RPMRepoPackage.class), 0);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, IMABaselineRecord.class), 0);
    }
}
