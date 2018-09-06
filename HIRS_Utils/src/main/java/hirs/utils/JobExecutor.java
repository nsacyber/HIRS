package hirs.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This class facilitates running {@link Job}s in a parallel, asynchronous fashion.
 */
public class JobExecutor {
    /**
     * These values describe the internal state of a JobExecutor instance.
     */
    public enum State {
        /**
         * The JobExecutor is currently running an accepting new Jobs.
         */
        RUNNING,

        /**
         * The JobExecutor is currently in the process of shutting down and is not accepting new
         * Jobs.
         */
        SHUTTING_DOWN,

        /**
         * The JobExecutor has been shut down and is not accepting new Jobs.
         */
        SHUT_DOWN
    }

    private static final int ONE_SECOND_IN_MS = 1000;
    private static final int PROC_COUNT = Runtime.getRuntime().availableProcessors();

    private final List<Job> runningJobs = Collections.synchronizedList(new ArrayList<Job>());

    private volatile ExecutorService executorService;
    private volatile State state;

    /**
     * Constructs a new JobExecutor with a threadCountMultiplier of 1.  See the next constructor's
     * doc for more details.
     */
    public JobExecutor() {
        this(1);
    }

    /**
     * Constructs a new JobExecutor.  The parameter configures the maximum parallelism of the
     * job.  At minimum, the job executor uses an equal number of threads to the system's processor
     * count.  At maximum, it will use as many threads as this number * the threadCountMultiplier
     * parameter.  For example, if the system has four processors and this parameter is 2, the
     * executor will use at most 8 threads (useful for tasks dealing with blocking I/O, etc.)
     *
     * @param threadCountMultiplier the
     */
    public JobExecutor(final int threadCountMultiplier) {
        if (threadCountMultiplier < 1) {
            throw new IllegalArgumentException("threadCountMultiplier argument must be at least 1");
        }

        executorService = new ThreadPoolExecutor(
                PROC_COUNT,
                PROC_COUNT * threadCountMultiplier,
                ONE_SECOND_IN_MS,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>()
        );

        state = State.RUNNING;
    }

    /**
     * Schedule a new Job to execute using this JobExecutor.  The tasks from this Job will be queued
     * and started as soon as there are free threads available.
     *
     * @param job the job whose tasks should be scheduled
     */
    public final void scheduleJob(final Job<?> job) {
        if (state != State.RUNNING) {
            throw new IllegalStateException("Cannot schedule new job on a JobExecutor that has"
                    + " been shut down.");
        }

        runningJobs.add(job);

        job.submitTasks(executorService, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                runningJobs.remove(job);
                return null;
            }
        });
    }

    /**
     * Gets the current state of this JobExecutor.
     *
     * @return the current state of this JobExecutor
     */
    public final State getState() {
        return state;
    }

    /**
     * Waits for jobs to finish, then cleans up the resources associated with this instance.  After
     * this method is called, no more jobs can be submitted to this JobExecutor instance.
     *
     * @throws InterruptedException if this thread is interrupted while waiting for running jobs to
     * finish execution
     */
    @SuppressFBWarnings(
            value = "SWL_SLEEP_WITH_LOCK_HELD",
            justification = "No resources are being consumed or waited on; reentrant threads should"
                    + "remain locked out while .")
    public final synchronized void shutdown() throws InterruptedException {
        if (state == State.SHUT_DOWN) {
            return;
        }

        state = State.SHUTTING_DOWN;

        while (runningJobs.size() > 0) {
            Thread.sleep(ONE_SECOND_IN_MS);
        }

        executorService.shutdownNow();
        executorService = null;
        state = State.SHUT_DOWN;
    }

    /**
     * Cancels currently-executing jobs, then cleans up the resources associated with
     * this instance.  After this method is called, no more jobs can be submitted to this
     * JobExecutor instance.
     *
     * @throws InterruptedException if this thread is interrupted while attempting to terminate
     */
    public final void shutdownNow() throws InterruptedException {
        List<Job> currentlyRunningJobs = new ArrayList<>(runningJobs);
        for (Job job : currentlyRunningJobs) {
            job.cancel();
        }

        shutdown();
    }
}
