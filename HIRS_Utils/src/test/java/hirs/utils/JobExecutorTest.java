package hirs.utils;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class holds tests for {@link Job} and {@link JobExecutor}.
 */
public class JobExecutorTest {
    private static final int TEN_SECONDS_MS = 10 * 1000;
    private static final int TEN = 10;
    private static final int FIFTY = 50;
    private static final int ONE_HUNDRED = 100;
    private static final int FIVE_HUNDRED = 500;
    private static final HashSet<Integer> ONE_THROUGH_TEN =
            new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

    private JobExecutor jobExecutor;
    private AtomicInteger counter;

    private Callable<Integer> incrementTask = new Callable<Integer>() {
        @Override
        public Integer call() throws Exception {
            return counter.incrementAndGet();
        }
    };

    private Callable<Integer> failingTask = new Callable<Integer>() {
        @Override
        public Integer call() throws Exception {
            throw new RuntimeException("Task failure.");
        }
    };

    private Callable<Void> longTask = new Callable<Void>() {
        @Override
        public Void call() throws Exception {
            Thread.sleep(TEN_SECONDS_MS);
            return null;
        }
    };

    private Callable<Void> longFailingTask = new Callable<Void>() {
        @Override
        public Void call() throws Exception {
            Thread.sleep(TEN_SECONDS_MS);
            throw new RuntimeException("Task failure.");
        }
    };

    @BeforeMethod
    private void setup() {
        jobExecutor = new JobExecutor();
        counter = new AtomicInteger();
    }

    @AfterMethod
    private void teardown() throws InterruptedException {
        jobExecutor.shutdown();
    }

    /**
     * Tests that a job with a single task completes successfully with no issues.
     *
     * @throws InterruptedException if the JobExecutor is interrupted while shutting down
     */
    @Test
    public final void testJobWithOneTask() throws InterruptedException {
        List<Callable<Integer>> tasks = new ArrayList<>();
        tasks.add(incrementTask);
        Job<Integer> job = new Job<>(tasks);
        jobExecutor.scheduleJob(job);
        jobExecutor.shutdown();
        Assert.assertTrue(job.isFinished());
        Assert.assertEquals(job.getState(), Job.State.COMPLETED);
        Assert.assertEquals(job.getTotalTaskCount(), 1);
        Assert.assertEquals(job.getFinishedTaskCount(), 1);
        Assert.assertEquals(job.getSuccessfulTaskCount(), 1);
        Assert.assertEquals(job.getFailedTaskCount(), 0);
        Assert.assertEquals(job.getCanceledTaskCount(), 0);
        Assert.assertEquals(job.getResults(incrementTask).size(), 1);
        Assert.assertEquals(counter.get(), 1);
    }

    /**
     * Tests that a job with multiple tasks completes successfully with no issues.
     *
     * @throws InterruptedException if the JobExecutor is interrupted while shutting down
     */
    @Test
    public final void testJobWithManyTasks() throws InterruptedException {
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < TEN; i++) {
            tasks.add(incrementTask);
        }

        Job<Integer> job = new Job<>(tasks);
        jobExecutor.scheduleJob(job);
        jobExecutor.shutdown();
        Assert.assertTrue(job.isFinished());
        Assert.assertEquals(job.getState(), Job.State.COMPLETED);
        Assert.assertEquals(job.getTotalTaskCount(), TEN);
        Assert.assertEquals(job.getFinishedTaskCount(), TEN);
        Assert.assertEquals(job.getSuccessfulTaskCount(), TEN);
        Assert.assertEquals(job.getFailedTaskCount(), 0);
        Assert.assertEquals(job.getCanceledTaskCount(), 0);
        Assert.assertEquals(job.getResults(incrementTask).size(), TEN);
        Assert.assertEquals(counter.get(), TEN);
    }

    /**
     * Tests that access to the return values from a job's tasks works as expected.
     *
     * @throws InterruptedException if the JobExecutor is interrupted while shutting down
     */
    @Test
    public final void testReturnValues() throws InterruptedException {
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < TEN; i++) {
            tasks.add(incrementTask);
        }

        Job<Integer> job = new Job<>(tasks);
        jobExecutor.scheduleJob(job);
        jobExecutor.shutdown();
        Assert.assertTrue(job.isFinished());
        Assert.assertEquals(job.getState(), Job.State.COMPLETED);
        Assert.assertEquals(job.getTotalTaskCount(), TEN);
        Assert.assertEquals(job.getFinishedTaskCount(), TEN);
        Assert.assertEquals(job.getSuccessfulTaskCount(), TEN);
        Assert.assertEquals(job.getFailedTaskCount(), 0);
        Assert.assertEquals(job.getCanceledTaskCount(), 0);
        Assert.assertEquals(job.getResults(incrementTask).size(), TEN);
        Assert.assertEquals(new HashSet<>(job.getResults(incrementTask)), ONE_THROUGH_TEN);
        Assert.assertEquals(counter.get(), TEN);
    }


    /**
     * Tests that many jobs, each having a single task, are scheduled and operate as expected.
     *
     * @throws InterruptedException if the JobExecutor is interrupted while shutting down
     */
    @Test
    public final void testManyJobsWithOneTask() throws InterruptedException {
        List<Callable<Integer>> tasks = new ArrayList<>();
        tasks.add(incrementTask);

        List<Job<Integer>> jobs = new ArrayList<>();

        for (int i = 0; i < TEN; i++) {
            Job<Integer> job = new Job<>(tasks);
            jobExecutor.scheduleJob(job);
            jobs.add(job);
        }

        jobExecutor.shutdown();

        for (Job<Integer> job : jobs) {
            Assert.assertTrue(job.isFinished());
            Assert.assertEquals(job.getState(), Job.State.COMPLETED);
            Assert.assertEquals(job.getTotalTaskCount(), 1);
            Assert.assertEquals(job.getFinishedTaskCount(), 1);
            Assert.assertEquals(job.getSuccessfulTaskCount(), 1);
            Assert.assertEquals(job.getFailedTaskCount(), 0);
            Assert.assertEquals(job.getCanceledTaskCount(), 0);
            Assert.assertEquals(job.getResults(incrementTask).size(), 1);
        }
        Assert.assertEquals(counter.get(), TEN);
    }

    /**
     * Tests that many jobs, each having many tasks, are scheduled and operate as expected.
     *
     * @throws InterruptedException if the JobExecutor is interrupted while shutting down
     */
    @Test
    public final void testManyJobsWithManyTasks() throws InterruptedException {
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < TEN; i++) {
            tasks.add(incrementTask);
        }

        List<Job<Integer>> jobs = new ArrayList<>();

        for (int i = 0; i < TEN; i++) {
            Job<Integer> job = new Job<>(tasks);
            jobExecutor.scheduleJob(job);
            jobs.add(job);
        }

        jobExecutor.shutdown();

        for (Job<Integer> job : jobs) {
            Assert.assertTrue(job.isFinished());
            Assert.assertEquals(job.getState(), Job.State.COMPLETED);
            Assert.assertEquals(job.getTotalTaskCount(), TEN);
            Assert.assertEquals(job.getFinishedTaskCount(), TEN);
            Assert.assertEquals(job.getSuccessfulTaskCount(), TEN);
            Assert.assertEquals(job.getFailedTaskCount(), 0);
            Assert.assertEquals(job.getCanceledTaskCount(), 0);
            Assert.assertEquals(job.getResults(incrementTask).size(), TEN);
        }
        Assert.assertEquals(counter.get(), ONE_HUNDRED);
    }

    /**
     * Tests that a jobs with a task that always fails operates as expected.
     *
     * @throws InterruptedException if the JobExecutor is interrupted while shutting down
     */
    @Test
    public final void testJobWithOneFailingTask() throws InterruptedException {
        List<Callable<Integer>> tasks = new ArrayList<>();
        tasks.add(failingTask);
        Job<Integer> job = new Job<>(tasks);
        jobExecutor.scheduleJob(job);
        jobExecutor.shutdown();
        Assert.assertTrue(job.isFinished());
        Assert.assertEquals(job.getState(), Job.State.FAILED);
        Assert.assertEquals(job.getTotalTaskCount(), 1);
        Assert.assertEquals(job.getFinishedTaskCount(), 1);
        Assert.assertEquals(job.getSuccessfulTaskCount(), 0);
        Assert.assertEquals(job.getFailedTaskCount(), 1);
        Assert.assertEquals(job.getCanceledTaskCount(), 0);
        Assert.assertEquals(job.getAllFailures().size(), 1);
        Assert.assertEquals(job.getResults(failingTask).size(), 0);
        Assert.assertEquals(job.getFailures(failingTask).size(), 1);
    }

    /**
     * Tests that a job with many tasks that always fail operates as expected.
     *
     * @throws InterruptedException if the JobExecutor is interrupted while shutting down
     */
    @Test
    public final void testJobWithManyFailingTasks() throws InterruptedException {
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < TEN; i++) {
            tasks.add(failingTask);
        }
        Job<Integer> job = new Job<>(tasks);
        jobExecutor.scheduleJob(job);
        jobExecutor.shutdown();
        Assert.assertTrue(job.isFinished());
        Assert.assertEquals(job.getState(), Job.State.FAILED);
        Assert.assertEquals(job.getTotalTaskCount(), TEN);
        Assert.assertEquals(job.getFinishedTaskCount(), TEN);
        Assert.assertEquals(job.getSuccessfulTaskCount(), 0);
        Assert.assertEquals(job.getFailedTaskCount(), TEN);
        Assert.assertEquals(job.getCanceledTaskCount(), 0);
        Assert.assertEquals(job.getAllFailures().size(), TEN);
        Assert.assertEquals(job.getResults(failingTask).size(), 0);
        Assert.assertEquals(job.getFailures(failingTask).size(), TEN);
    }

    /**
     * Tests that a job with both succeeding and failing tasks operates as expected.
     *
     * @throws InterruptedException if the JobExecutor is interrupted while shutting down
     */
    @Test
    public final void testJobWithBothSucceedingAndFailingTasks() throws InterruptedException {
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < FIFTY; i++) {
            tasks.add(incrementTask);
            tasks.add(failingTask);
        }
        Job<Integer> job = new Job<>(tasks);
        jobExecutor.scheduleJob(job);
        jobExecutor.shutdown();
        Assert.assertTrue(job.isFinished());
        Assert.assertEquals(job.getState(), Job.State.FAILED);
        Assert.assertEquals(job.getTotalTaskCount(), ONE_HUNDRED);
        Assert.assertEquals(job.getFinishedTaskCount(), ONE_HUNDRED);
        Assert.assertEquals(job.getSuccessfulTaskCount(), FIFTY);
        Assert.assertEquals(job.getFailedTaskCount(), FIFTY);
        Assert.assertEquals(job.getCanceledTaskCount(), 0);
        Assert.assertEquals(job.getAllFailures().size(), FIFTY);
        Assert.assertEquals(job.getResults(incrementTask).size(), FIFTY);
        Assert.assertEquals(job.getResults(failingTask).size(), 0);
        Assert.assertEquals(job.getFailures(incrementTask).size(), 0);
        Assert.assertEquals(job.getFailures(failingTask).size(), FIFTY);
        Assert.assertEquals(counter.get(), FIFTY);
    }

    /**
     * Tests that many jobs with both succeeding and failing tasks operate as expected.
     *
     * @throws InterruptedException if the JobExecutor is interrupted while shutting down
     */
    @Test
    public final void testManyJobsWithBothSucceedingAndFailingTasks() throws InterruptedException {
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < FIFTY; i++) {
            tasks.add(incrementTask);
            tasks.add(failingTask);
        }

        List<Job<Integer>> jobs = new ArrayList<>();

        for (int i = 0; i < TEN; i++) {
            Job<Integer> job = new Job<>(tasks);
            jobExecutor.scheduleJob(job);
            jobs.add(job);
        }

        jobExecutor.shutdown();

        for (Job<Integer> job : jobs) {
            Assert.assertTrue(job.isFinished());
            Assert.assertEquals(job.getState(), Job.State.FAILED);
            Assert.assertEquals(job.getTotalTaskCount(), ONE_HUNDRED);
            Assert.assertEquals(job.getFinishedTaskCount(), ONE_HUNDRED);
            Assert.assertEquals(job.getSuccessfulTaskCount(), FIFTY);
            Assert.assertEquals(job.getFailedTaskCount(), FIFTY);
            Assert.assertEquals(job.getCanceledTaskCount(), 0);
            Assert.assertEquals(job.getAllFailures().size(), FIFTY);
            Assert.assertEquals(job.getResults(incrementTask).size(), FIFTY);
            Assert.assertEquals(job.getResults(failingTask).size(), 0);
            Assert.assertEquals(job.getFailures(incrementTask).size(), 0);
            Assert.assertEquals(job.getFailures(failingTask).size(), FIFTY);
        }

        Assert.assertEquals(counter.get(), FIVE_HUNDRED);
    }

    /**
     * Tests that a job is able to be immediately canceled.
     *
     * @throws InterruptedException if the JobExecutor is interrupted while shutting down
     */
    @Test
    public final void testCancelJob() throws InterruptedException {
        List<Callable<Void>> tasks = new ArrayList<>();
        tasks.add(longTask);
        Job<Void> job = new Job<>(tasks);
        jobExecutor.scheduleJob(job);
        job.cancel();
        jobExecutor.shutdown();

        Assert.assertTrue(job.isFinished());
        Assert.assertEquals(job.getState(), Job.State.CANCELED);
        Assert.assertEquals(job.getTotalTaskCount(), 1);
        Assert.assertEquals(job.getFinishedTaskCount(), 1);
        Assert.assertEquals(job.getSuccessfulTaskCount(), 0);
        Assert.assertEquals(job.getFailedTaskCount(), 0);
        Assert.assertEquals(job.getCanceledTaskCount(), 1);
        Assert.assertEquals(job.getResults(longTask).size(), 0);
    }

    /**
     * Tests that a job is able to be canceled after a certain time.
     *
     * @throws InterruptedException if the JobExecutor is interrupted while shutting down
     */
    @Test
    public final void testCancelJobWithDelay() throws InterruptedException {
        List<Callable<Void>> tasks = new ArrayList<>();
        tasks.add(longTask);
        Job<Void> job = new Job<>(tasks);
        jobExecutor.scheduleJob(job);

        Thread.sleep(FIVE_HUNDRED * TEN);

        job.cancel();
        jobExecutor.shutdown();

        Assert.assertTrue(job.isFinished());
        Assert.assertEquals(job.getState(), Job.State.CANCELED);
        Assert.assertEquals(job.getTotalTaskCount(), 1);
        Assert.assertEquals(job.getFinishedTaskCount(), 1);
        Assert.assertEquals(job.getSuccessfulTaskCount(), 0);
        Assert.assertEquals(job.getFailedTaskCount(), 0);
        Assert.assertEquals(job.getCanceledTaskCount(), 1);
        Assert.assertEquals(job.getResults(longTask).size(), 0);
    }

    /**
     * Tests that a JobExecutor can successfully be shut down with shutdownNow and will cancel
     * any running jobs.
     *
     * @throws InterruptedException if the JobExecutor is interrupted while shutting down
     */
    @Test
    public final void testExecutorShutdownNow() throws InterruptedException {
        List<Callable<Void>> tasks = new ArrayList<>();
        tasks.add(longTask);
        Job<Void> job = new Job<>(tasks);
        jobExecutor.scheduleJob(job);
        jobExecutor.shutdownNow();

        Assert.assertTrue(job.isFinished());
        Assert.assertEquals(job.getState(), Job.State.CANCELED);
        Assert.assertEquals(job.getTotalTaskCount(), 1);
        Assert.assertEquals(job.getFinishedTaskCount(), 1);
        Assert.assertEquals(job.getSuccessfulTaskCount(), 0);
        Assert.assertEquals(job.getFailedTaskCount() + job.getCanceledTaskCount(), 1);
        Assert.assertEquals(job.getResults(longTask).size(), 0);
        Assert.assertEquals(job.getAllFailures().size(), 1);
    }

    /**
     * Tests that a single job with an onFinish task executes properly.
     *
     * @throws Exception if the JobExecutor is interrupted while shutting down or if the onFinish
     * task throws an Exception
     */
    @Test
    public final void testJobWithOnFinishTask() throws Exception {
        List<Callable<Integer>> tasks = new ArrayList<>();
        tasks.add(incrementTask);
        Job<Integer> job = new Job<>(tasks, incrementTask);
        jobExecutor.scheduleJob(job);
        jobExecutor.shutdown();

        Assert.assertTrue(job.isFinished());
        Assert.assertEquals(job.getState(), Job.State.COMPLETED);
        Assert.assertEquals(job.getTotalTaskCount(), 1);
        Assert.assertEquals(job.getFinishedTaskCount(), 1);
        Assert.assertEquals(job.getSuccessfulTaskCount(), 1);
        Assert.assertEquals(job.getFailedTaskCount(), 0);
        Assert.assertEquals(job.getCanceledTaskCount(), 0);
        Assert.assertEquals(job.getResults(incrementTask).size(), 1);
        Assert.assertEquals(job.getAllFailures().size(), 0);
        Assert.assertEquals(counter.get(), 2);
        Assert.assertEquals(job.getOnFinishResult().intValue(), 2);
    }

    /**
     * Tests that a single job with a failing onFinish tasks executes properly.
     *
     * @throws Exception if the JobExecutor is interrupted while shutting down or if the onFinish
     * task throws an Exception (it is expected that it will, in this case)
     */
    @Test(expectedExceptions = ExecutionException.class)
    public final void testJobWithFailingOnFinishTask() throws Exception {
        List<Callable<Integer>> tasks = new ArrayList<>();
        tasks.add(incrementTask);
        Job<Integer> job = new Job<>(tasks, failingTask);
        jobExecutor.scheduleJob(job);
        jobExecutor.shutdown();

        Assert.assertTrue(job.isFinished());
        Assert.assertEquals(job.getState(), Job.State.FAILED);
        Assert.assertEquals(job.getTotalTaskCount(), 1);
        Assert.assertEquals(job.getFinishedTaskCount(), 1);
        Assert.assertEquals(job.getSuccessfulTaskCount(), 1);
        Assert.assertEquals(job.getFailedTaskCount(), 0);
        Assert.assertEquals(job.getCanceledTaskCount(), 0);
        Assert.assertEquals(job.getResults(incrementTask).size(), 1);
        Assert.assertEquals(job.getAllFailures().size(), 0);
        Assert.assertEquals(counter.get(), 1);
        job.getOnFinishResult();
    }

    /**
     * Tests that an IllegalArgumentException is thrown if a null task list is passed into Job's
     * constructor.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void testNewJobNullTasks() {
        new Job<Void>(null);
    }

    /**
     * Tests that constructing a new job with a null onFinish value results in an
     * IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void testNewJobNullOnFinish() {
        List<Callable<Integer>> tasks = new ArrayList<>();
        tasks.add(incrementTask);
        new Job<>(tasks, null);
    }

    /**
     * Tests that an IllegalArgumentException is thrown if an empty task list is passed into Job's
     * constructor.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void testNewJobEmptyTaskCollection() {
        new Job<>(new ArrayList<Callable<Void>>());
    }

    /**
     * Tests that an IllegalArgumentException is thrown if a null task is passed into getResults.
     *
     * @throws InterruptedException if the JobExecutor is interrupted while shutting down
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void testGetResultsNullTask() throws InterruptedException {
        List<Callable<Integer>> tasks = new ArrayList<>();
        tasks.add(incrementTask);
        Job<Integer> job = new Job<>(tasks);
        jobExecutor.scheduleJob(job);
        jobExecutor.shutdown();
        job.getResults(null);
    }

    /**
     * Tests that an IllegalStateException is thrown if an attempt is made to retrieve results while
     * the job is still running.
     */
    @Test(expectedExceptions = IllegalStateException.class)
    public final void testGetResultsJobNotFinished() {
        List<Callable<Void>> tasks = new ArrayList<>();
        tasks.add(longTask);
        Job<Void> job = new Job<>(tasks);
        jobExecutor.scheduleJob(job);
        job.getResults(longTask);
    }

    /**
     * Tests that getting the results for a task not part of the job returns an empty list.
     *
     * @throws InterruptedException if the JobExecutor is interrupted while shutting down
     */
    @Test
    public final void testGetResultsUnknownTask() throws InterruptedException {
        List<Callable<Integer>> tasks = new ArrayList<>();
        tasks.add(incrementTask);
        Job<Integer> job = new Job<>(tasks);
        jobExecutor.scheduleJob(job);
        jobExecutor.shutdown();
        Assert.assertEquals(job.getResults(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return null;
            }
        }).size(), 0);
    }

    /**
     * Tests that getting the failures for a null task throws an IllegalArgumentException.
     *
     * @throws InterruptedException if the JobExecutor is interrupted while shutting down
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void testGetFailuresNullTask() throws InterruptedException {
        List<Callable<Integer>> tasks = new ArrayList<>();
        tasks.add(incrementTask);
        Job<Integer> job = new Job<>(tasks);
        jobExecutor.scheduleJob(job);
        jobExecutor.shutdown();
        job.getFailures(null);
    }

    /**
     * Tests that getting the failures for a task not part of the job returns an empty list.
     *
     * @throws InterruptedException if the JobExecutor is interrupted while shutting down
     */
    @Test
    public final void testGetFailuresUnknownTask() throws InterruptedException {
        List<Callable<Integer>> tasks = new ArrayList<>();
        tasks.add(incrementTask);
        Job<Integer> job = new Job<>(tasks);
        jobExecutor.scheduleJob(job);
        jobExecutor.shutdown();
        Assert.assertEquals(job.getFailures(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return null;
            }
        }).size(), 0);
    }

    /**
     * Tests that getting the failures while a job is still running throws an IllegalStateException.
     */
    @Test(expectedExceptions = IllegalStateException.class)
    public final void testGetFailuresJobNotFinished() {
        List<Callable<Integer>> tasks = new ArrayList<>();
        tasks.add(incrementTask);
        Job<Integer> job = new Job<>(tasks);
        jobExecutor.scheduleJob(job);
        job.getFailures(incrementTask);
    }

    /**
     * Tests that getting all failures on a job that has not yet finished results in an
     * IllegalStateException.
     */
    @Test(expectedExceptions = IllegalStateException.class)
    public final void testGetAllFailuresJobNotFinished() {
        List<Callable<Integer>> tasks = new ArrayList<>();
        tasks.add(incrementTask);
        Job<Integer> job = new Job<>(tasks);
        jobExecutor.scheduleJob(job);
        job.getAllFailures();
    }

    /**
     * Tests that the job's isRunning method returns the expected results throughout the lifecycle
     * of the job.
     *
     * @throws InterruptedException if the JobExecutor is interrupted while shutting down
     */
    @Test
    public final void testIsRunningAndIsFinished() throws InterruptedException {
        List<Callable<Void>> tasks = new ArrayList<>();
        tasks.add(longTask);
        Job<Void> job = new Job<>(tasks);
        Assert.assertEquals(job.getState(), Job.State.UNSTARTED);
        Assert.assertFalse(job.isRunning());
        Assert.assertFalse(job.isFinished());
        jobExecutor.scheduleJob(job);
        Assert.assertEquals(job.getState(), Job.State.IN_PROGRESS);
        Assert.assertTrue(job.isRunning());
        Assert.assertFalse(job.isFinished());
        jobExecutor.shutdown();
        Assert.assertEquals(job.getState(), Job.State.COMPLETED);
        Assert.assertFalse(job.isRunning());
        Assert.assertTrue(job.isFinished());
    }

    /**
     * Tests that the job's isRunning method returns the expected results throughout the lifecycle
     * of a failing job.
     *
     * @throws InterruptedException if the JobExecutor is interrupted while shutting down
     */
    @Test
    public final void testIsRunningAndIsFinishedWithFailingJob() throws InterruptedException {
        List<Callable<Void>> tasks = new ArrayList<>();
        tasks.add(longFailingTask);
        Job<Void> job = new Job<>(tasks);
        Assert.assertEquals(job.getState(), Job.State.UNSTARTED);
        Assert.assertFalse(job.isRunning());
        Assert.assertFalse(job.isFinished());
        jobExecutor.scheduleJob(job);
        Assert.assertEquals(job.getState(), Job.State.IN_PROGRESS);
        Assert.assertTrue(job.isRunning());
        Assert.assertFalse(job.isFinished());
        jobExecutor.shutdown();
        Assert.assertEquals(job.getState(), Job.State.FAILED);
        Assert.assertFalse(job.isRunning());
        Assert.assertTrue(job.isFinished());
    }
}
