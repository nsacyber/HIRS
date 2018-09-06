package hirs.repository;

import hirs.data.persist.SpringPersistenceTest;
import hirs.persist.DBRepositoryManager;
import hirs.persist.RepositoryManager;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import hirs.persist.DBUtility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class tests the {@link RepositoryUpdateService}.
 */
public class RepositoryUpdateServiceTest extends SpringPersistenceTest {
    private static final int FOUR = 4;
    private static final int TEN = 10;
    private static final int ONE_HUNDRED = 100;
    private static final int FIVE_HUNDRED = 500;

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
        DBUtility.removeAllInstances(sessionFactory, TestRepository.class);
        RepositoryUpdateService.getInstance().clearFinishedJobs();
    }

    /**
     * Tests that a basic update workflow functions as expected.
     *
     * @throws IOException if there is a problem generating the update job
     * @throws InterruptedException if the Thread's sleep is interrupted
     * @throws RepositoryException a Repository exception occurs
     */
    @Test
    public final void testBasicUpdateWorkflow()
            throws IOException, InterruptedException, RepositoryException {
        RepositoryUpdateService updateService = RepositoryUpdateService.getInstance();
        RepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        TestRepository repo = new TestRepository("TestRepo", 0);
        repoMan.saveRepository(repo);

        Repository retrievedRepo = repoMan.getRepository(repo.getId());
        Assert.assertEquals(retrievedRepo.getPackages().size(), 0);

        repo.setNumRemotePackages(FOUR);
        updateService.startUpdateJob(repo, 1, repoMan);
        while (updateService.getActiveJobs().size() > 0) {
            Thread.sleep(FIVE_HUNDRED);
        }
        Repository updatedRepo = repoMan.getRepository(repo.getId());
        Assert.assertEquals(updatedRepo.getPackages().size(), FOUR);
        Assert.assertFalse(updateService.hasActiveJobs(createList(repo)));
    }

    /**
     * Tests that the update service functions as expected throughout its entire normal workflow.
     *
     * @throws IOException if there is a problem generating the update job
     * @throws InterruptedException if the Thread's sleep is interrupted
     * @throws RepositoryException a Repository exception occurs
     */
    @Test
    public final void testFullUpdateWorkflow()
            throws IOException, InterruptedException, RepositoryException {
        RepositoryUpdateService updateService = RepositoryUpdateService.getInstance();
        RepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        TestRepository repo = new TestRepository("TestRepo", FIVE_HUNDRED);
        repoMan.saveRepository(repo);
        Assert.assertEquals(updateService.getActiveJobs().size(), 0);
        Assert.assertEquals(updateService.getFinishedJobs().size(), 0);
        repo.setNumRemotePackages(FOUR);
        updateService.startUpdateJob(repo, 1, repoMan);
        Assert.assertEquals(updateService.getActiveJobs().size(), 1);
        Assert.assertTrue(updateService.hasActiveJobs(createList(repo)));
        while (updateService.getActiveJobs().size() > 0) {
            Thread.sleep(FIVE_HUNDRED);
        }
        Assert.assertEquals(updateService.getActiveJobs().size(), 0);
        Assert.assertFalse(updateService.hasActiveJobs(createList(repo)));
        Assert.assertEquals(updateService.getFinishedJobs().size(), 1);
        updateService.clearFinishedJobs();
        Assert.assertEquals(updateService.getFinishedJobs().size(), 0);
        Repository updatedRepo = repoMan.getRepository(repo.getId());
        Assert.assertEquals(updatedRepo.getPackages().size(), FOUR);
    }

    /**
     * Tests that a large update job completes successfully.
     *
     * @throws IOException if there is a problem generating the update job
     * @throws InterruptedException if the Thread's sleep is interrupted
     * @throws RepositoryException a Repository exception occurs
     */
    @Test
    public final void testBigUpdate()
            throws IOException, InterruptedException, RepositoryException {
        RepositoryUpdateService updateService = RepositoryUpdateService.getInstance();
        RepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        TestRepository repo = new TestRepository("TestRepo", ONE_HUNDRED);
        repoMan.saveRepository(repo);
        repo.setNumRemotePackages(ONE_HUNDRED);
        updateService.startUpdateJob(repo, 1, repoMan);
        while (updateService.getActiveJobs().size() > 0) {
            Thread.sleep(FIVE_HUNDRED);
        }
        Repository updatedRepo = repoMan.getRepository(repo.getId());
        Assert.assertEquals(updatedRepo.getPackages().size(), ONE_HUNDRED);
    }

    /**
     * Tests that 10 sequential repository updates, each giving 10 new packages as updates,
     * functions as expected.
     *
     * @throws IOException if there is a problem generating the update job
     * @throws InterruptedException if the Thread's sleep is interrupted
     * @throws RepositoryException a Repository exception occurs
     */
    @Test
    public final void testRepositoryUpdate()
            throws IOException, InterruptedException, RepositoryException {
        RepositoryUpdateService updateService = RepositoryUpdateService.getInstance();
        RepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        TestRepository repo = new TestRepository("TestRepo", TEN);
        repoMan.saveRepository(repo);

        List<Repository> allRepos = new ArrayList<>();

        for (int i = 1; i <= TEN; i++) {
            TestRepository currentRepo = (TestRepository) repoMan.getRepository(repo.getId());
            currentRepo.setNumRemotePackages(i * TEN);
            updateService.startUpdateJob(currentRepo, 1, repoMan);
            allRepos.add(currentRepo);
            Assert.assertTrue(updateService.hasActiveJobs(createList(currentRepo)));
            while (updateService.getActiveJobs().size() > 0) {
                Thread.sleep(FIVE_HUNDRED);
            }
        }

        Repository updatedRepo = repoMan.getRepository(repo.getId());
        Assert.assertEquals(updatedRepo.getPackages().size(), ONE_HUNDRED);
        Assert.assertFalse(updateService.hasActiveJobs(null));
        Assert.assertFalse(updateService.hasActiveJobs(new ArrayList<Repository>()));
        Assert.assertFalse(updateService.hasActiveJobs(allRepos));
    }

    /**
     * Tests that it is possible to cancel a repository update job.
     *
     * @throws IOException if there is a problem generating the update job
     * @throws InterruptedException if the Thread's sleep is interrupted
     * @throws RepositoryException a Repository exception occurs
     */
    @Test
    public final void testCancelRepositoryUpdate()
            throws IOException, InterruptedException, RepositoryException {
        RepositoryUpdateService updateService = RepositoryUpdateService.getInstance();
        RepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        TestRepository repo = new TestRepository("TestRepo", FIVE_HUNDRED);
        repoMan.saveRepository(repo);
        repo.setNumRemotePackages(ONE_HUNDRED);
        updateService.startUpdateJob(repo, 1, repoMan);
        updateService.getActiveJobs().get(repo.getId()).cancel();
        while (updateService.getActiveJobs().size() > 0) {
            Thread.sleep(FIVE_HUNDRED);
        }
        Repository updatedRepo = repoMan.getRepository(repo.getId());
        Assert.assertTrue(updatedRepo.getPackages().size() < ONE_HUNDRED);
        Assert.assertFalse(updateService.hasActiveJobs(createList(repo)));
    }

    /**
     * Tests that haActiveJobs() correctly uses the set of provided repositories.
     *
     * @throws IOException if there is a problem generating the update job
     * @throws InterruptedException if the Thread's sleep is interrupted
     * @throws RepositoryException a Repository exception occurs
     */
    @Test
    public final void tesHasActiveJobs()
            throws IOException, InterruptedException, RepositoryException {
        RepositoryUpdateService updateService = RepositoryUpdateService.getInstance();
        RepositoryManager repoMan = new DBRepositoryManager(sessionFactory);
        TestRepository repo1 = new TestRepository("TestRepo", 0);
        TestRepository repo2 = new TestRepository("TestRepo2", 0);
        repoMan.saveRepository(repo1);
        repoMan.saveRepository(repo2);

        Repository retrievedRepo = repoMan.getRepository(repo1.getId());
        Assert.assertEquals(retrievedRepo.getPackages().size(), 0);

        repo1.setNumRemotePackages(FOUR);
        updateService.startUpdateJob(repo1, 1, repoMan);
        Assert.assertTrue(updateService.hasActiveJobs(createList(repo1)));
        Assert.assertFalse(updateService.hasActiveJobs(createList(repo2)));
        Assert.assertFalse(updateService.hasActiveJobs(null));
        while (updateService.getActiveJobs().size() > 0) {
            Thread.sleep(FIVE_HUNDRED);
        }
        Repository updatedRepo = repoMan.getRepository(repo1.getId());
        Assert.assertEquals(updatedRepo.getPackages().size(), FOUR);
        Assert.assertFalse(updateService.hasActiveJobs(createList(repo1)));
        Assert.assertFalse(updateService.hasActiveJobs(createList(repo2)));
        Assert.assertFalse(updateService.hasActiveJobs(null));
    }

    private List<Repository> createList(final Repository repo) {
        List<Repository> repositories = new ArrayList<>();
        repositories.add(repo);
        return repositories;
    }
}
