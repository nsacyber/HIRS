package hirs.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This class can be used to execute periodic recurring tasks reliably using a
 * {@link ScheduledThreadPoolExecutor}. It can take a single {@link Runnable} and
 * execute it according to the given period.  Exceptions thrown by the given {@link Runnable}
 * do not prohibit the task from being rescheduled or kill the thread
 * running the task; they are logged instead.  This class is threadsafe.
 */
public class PeriodicExecutor {
    private static final Logger LOGGER = LogManager.getLogger(PeriodicExecutor.class);

    private ScheduledThreadPoolExecutor executor;

    /**
     * This method starts the periodic execution of a {@link Runnable} in its own thread.
     * Tasks are first run immediately upon submission.  If the {@link Runnable} throws an
     * Exception during its execution, the exception is logged and the next execution will take
     * place at the regularly scheduled interval.
     * <p>
     * The second and all subsequent calls to this method without a corresponding call to
     * <code>stop</code> will throw an {@link IllegalStateException}.  To schedule a new
     * task on this same object, call <code>stop</code> first.
     *
     * @param runnable the {@link Runnable} to run
     * @param runPeriodUnit the unit of time that the next parameter refers to
     * @param runPeriod the period's magnitude of the above time unit
     */
    public final synchronized void start(final Runnable runnable,
                                         final TimeUnit runPeriodUnit, final long runPeriod) {
        if (hasScheduledTask()) {
            throw new IllegalStateException("Cannot start an already started PeriodicExecutor.");
        }

        executor = new ScheduledThreadPoolExecutor(1);

        // wrap runnable to be able to report exceptions
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Exception e) {
                    LOGGER.warn("PeriodicExecutor task threw an exception", e);
                }
            }
        }, 0, runPeriod, runPeriodUnit);
    }

    /**
     * Prevents further execution of the scheduled task.  This method will wait the specified
     * length of time for any currently executing tasks to finish, and will then make a best effort
     * to interrupt the Thread that is running it using Thread#interrupt.  After this method is
     * called, another task can be scheduled for execution.
     *
     * This method will throw an {@link IllegalStateException} if it is called when no task is
     * currently scheduled.
     *
     * @param taskTerminationTimeoutUnit the unit of time that the next parameter refers to
     * @param taskTerminationTimeout the magnitude of time to wait for tasks to terminate
     *
     * @throws InterruptedException if an error is encountered while stopping the schedule
     */
    public final synchronized void stop(final TimeUnit taskTerminationTimeoutUnit,
                                        final long taskTerminationTimeout)
            throws InterruptedException {
        if (!hasScheduledTask()) {
            throw new IllegalStateException("PeriodicExecutor is already in a stopped state.");
        }

        // allows currently-executing tasks to run but disallows scheduling of new tasks
        executor.shutdown();
        boolean executorTerminated = executor.awaitTermination(
                taskTerminationTimeout, taskTerminationTimeoutUnit
        );

        if (!executorTerminated) {
            LOGGER.warn("Attempting to interrupt running tasks.");

            // calls the Thread#interrupt method to attempt to stop the task, but will not block
            executor.shutdownNow();
        }

        executor = null;
    }

    /**
     * Gives whether the <code>PeriodicExecutor</code> currently has a scheduled task or not.
     *
     * @return true if a task has been scheduled, false otherwise
     */
    public final synchronized boolean hasScheduledTask() {
        return executor != null;
    }
}
