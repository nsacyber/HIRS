package hirs.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Job represents a collection of tasks in the form of {@link Callable}s to execute in parallel.
 * This class facilitates controlling and reading their collective results.  The class also allows
 * the constructor's caller to specify a task to run upon completion of the rest of the tasks.  This
 * class should be executed with a {@link JobExecutor} instance.  The tasks will be executed
 * in parallel in no specific order.
 *
 * @param <T> the return type of the Callables that an instance of this class will contain.
 */
public class Job<T> {
    private static final Logger LOGGER = LogManager.getLogger(Job.class);

    /**
     * This enum contains values to represent the current internal state of the Job.
     */
    public enum State {
        /**
         * The job has yet to be started (has not yet been passed into JobExecutor#scheduleJob).
         */
        UNSTARTED,

        /**
         * The job's tasks are currently being executed.
         */
        IN_PROGRESS,

        /**
         * The job's tasks are currently being executed and at least one has thrown an exception.
         */
        IN_PROGRESS_WITH_FAILURES,

        /**
         * The job's tasks have all completed execution successfully.
         */
        COMPLETED,

        /**
         * The job is in the process of canceling further execution of tasks.
         */
        CANCELING,

        /**
         * The job has been canceled before all tasks had completed execution.
         */
        CANCELED,

        /**
         * The job's tasks have all completed execution and at least one has thrown an exception.
         */
        FAILED;

        private static final EnumMap<State, Set<State>> VALID_STATE_TRANSITIONS
                = new EnumMap<>(State.class);


        static {
            for (State s : State.values()) {
                VALID_STATE_TRANSITIONS.put(s, new HashSet<State>());
            }

            VALID_STATE_TRANSITIONS.get(State.UNSTARTED).add(State.IN_PROGRESS);
            VALID_STATE_TRANSITIONS.get(State.IN_PROGRESS).add(State.COMPLETED);
            VALID_STATE_TRANSITIONS.get(State.IN_PROGRESS).add(State.IN_PROGRESS_WITH_FAILURES);
            VALID_STATE_TRANSITIONS.get(State.IN_PROGRESS_WITH_FAILURES).add(State.FAILED);
            VALID_STATE_TRANSITIONS.get(State.IN_PROGRESS_WITH_FAILURES).add(State.CANCELING);
            VALID_STATE_TRANSITIONS.get(State.IN_PROGRESS).add(State.CANCELING);
            VALID_STATE_TRANSITIONS.get(State.CANCELING).add(State.CANCELED);
        }

        /**
         * Checks that a state is allowed to transition to a new State.
         * @param newState new desired state
         * @return true only if state transition is allowed
         */
        boolean canTransitionTo(final State newState) {
            return VALID_STATE_TRANSITIONS.get(this).contains(newState);
        }

    }

    private final List<Callable<T>> tasks;
    private final Map<Callable<T>, List<Future<T>>> results;
    private volatile State state;
    private volatile AtomicInteger successfulTasks = new AtomicInteger();
    private volatile AtomicInteger failedTasks = new AtomicInteger();
    private volatile AtomicInteger canceledTasks = new AtomicInteger();
    private Callable<Void> jobExecutorFinishHandler;

    private Callable<T> onFinish;
    private T onFinishResult;
    private Exception onFinishThrew;

    /**
     * Construct a new Job from the given {@link Collection} of tasks ({@link Callable}s).
     *
     * @param tasks the collection of tasks this job will run, cannot be null
     */
    public Job(final Collection<Callable<T>> tasks) {
        if (tasks == null) {
            throw new IllegalArgumentException("Tasks argument cannot be null.");
        }

        if (tasks.size() == 0) {
            throw new IllegalArgumentException("Cannot create a job with no tasks.");
        }
        this.tasks = new ArrayList<>(tasks);
        this.results = new HashMap<>();
        this.state = State.UNSTARTED;
    }

    /**
     * Construct a new Job from the given {@link Collection} of tasks ({@link Callable}s) and a
     * specific task to run upon completion of the job.  The onFinish task will run no matter the
     * result of the job (successful, failed, or canceled.)
     *
     * @param tasks the collection of tasks this job will run, cannot be null
     * @param onFinish the task to run upon finishing the tasks given by the first parameter,
     *                 cannot be null
     */
    public Job(final List<Callable<T>> tasks, final Callable<T> onFinish) {
        this(tasks);

        if (onFinish == null) {
            throw new IllegalArgumentException("onFinish parameter cannot be null.");
        }

        this.onFinish = onFinish;
    }

    /**
     * This is method is used by {@link JobExecutor} to submit tasks for execution.
     *
     * @param executor the executorService on which these tasks should be executed
     * @param finishHandler a callback to notify the JobExecutor that the job is complete
     */
    protected final synchronized void submitTasks(final ExecutorService executor,
                                                  final Callable<Void> finishHandler) {
        if (state != State.UNSTARTED) {
            throw new IllegalStateException("Cannot start an already started job.");
        }

        changeState(State.IN_PROGRESS);

        jobExecutorFinishHandler = finishHandler;

        for (final Callable<T> t : tasks) {
            final Future<T> future = executor.submit(new Callable<T>() {
                @Override
                public T call() throws Exception {
                    try {
                        T result = t.call();
                        successfulTasks.incrementAndGet();
                        return result;
                    } catch (Exception e) {
                        if (!(e instanceof InterruptedException)) {
                            failedTasks.incrementAndGet();
                            changeState(State.IN_PROGRESS_WITH_FAILURES);
                        }
                        throw e;
                    } finally {
                        checkFinish();
                    }
                }
            });

            synchronized (results) {
                if (!results.containsKey(t)) {
                    results.put(t, new ArrayList<Future<T>>());
                }
                results.get(t).add(future);
            }
        }
    }

    private synchronized void changeState(final State newState) {
        if (state == newState) {
            return;
        }

        if (state.canTransitionTo(newState)) {
            state = newState;
            return;
        }

        throw new IllegalStateException(
                String.format("Cannot change state from %s to %s", state, newState)
        );
    }

    private synchronized void checkFinish() {
        if (getFinishedTaskCount() == getTotalTaskCount()) {
            if (onFinish != null) {
                try {
                    onFinishResult = onFinish.call();
                } catch (Exception e) {
                    onFinishThrew = e;
                    changeState(State.IN_PROGRESS_WITH_FAILURES);
                }
            }

            if (state == State.IN_PROGRESS_WITH_FAILURES) {
                changeState(State.FAILED);
            } else if (state == State.CANCELING) {
                changeState(State.CANCELED);
            } else if (state == State.IN_PROGRESS) {
                changeState(State.COMPLETED);
            }

            try {
                jobExecutorFinishHandler.call();
            } catch (Exception e) {
                LOGGER.warn("Finish handler threw an exception", e);
            }
        }
    }

    /**
     * Returns the number of tasks that are 'finished': a total of the number of tasks that have
     * completed successfully, have failed, or have been canceled.
     *
     * @return the total number of completed tasks
     */
    public final int getFinishedTaskCount() {
        return getSuccessfulTaskCount() + getFailedTaskCount() + getCanceledTaskCount();
    }

    /**
     * Cancels the currently-running job.  This will prevent the execution of tasks that have yet
     * to start and will not attempt to interrupt currently-running tasks.  This has no effect on
     * already-finished tasks.
     *
     * If the job is not running, invoking this method has no effect.
     */
    public final synchronized void cancel() {
        if (!isRunning()) {
            return;
        }

        changeState(State.CANCELING);

        for (List<Future<T>> futures : results.values()) {
            for (Future future : futures) {
                if (future.cancel(false)) {
                    canceledTasks.incrementAndGet();
                    checkFinish();
                }
            }
        }
    }

    /**
     * Returns true if the job's tasks are currently executing, false otherwise.
     *
     * @return true if the job's tasks are currently executing, false otherwise
     */
    public final synchronized boolean isRunning() {
        return state == State.IN_PROGRESS || state == State.IN_PROGRESS_WITH_FAILURES;
    }

    /**
     * Returns true if the job's has finished successfully, finished with errors, or has been
     * successfully canceled. This method is for use outside of the Job class as opposed to
     * {@link #isFinishedState(State)}  isFinishedState} which should be used internally for
     * performance reasons.
     *
     * @return true if the job has finished according to the above criteria, false otherwise
     */
    public final synchronized boolean isFinished() {
        return isFinishedState(state);
    }

    /**
     * Returns true if the job's has finished successfully, finished with errors, or has been
     * successfully canceled. This method is private and not synchronized so it can be called from
     * within the class without the performance penalty of trying call a sychronized method from
     * another synchronized method which is holding the same lock.
     *
     * @param state state to check for completion
     * @return true if the job has finished according to the above criteria, false otherwise
     */
    private static boolean isFinishedState(final State state) {
        return state == State.COMPLETED || state == State.FAILED || state == State.CANCELED;
    }

    /**
     * Gets the current internal representation of the {@link Job}.
     *
     * @return the current internal representation of the {@link Job}
     */
    public final synchronized State getState() {
        return state;
    }

    /**
     * Gets the total count of tasks that comprise this job (not including any specified onFinish
     * task).
     *
     * @return the total count of tasks in this Job
     */
    public final synchronized int getTotalTaskCount() {
        return tasks.size();
    }

    /**
     * Gets the current number of tasks that have completed successfully.
     *
     * @return the number of tasks that have completed successfully
     */
    public final int getSuccessfulTaskCount() {
        return successfulTasks.get();
    }

    /**
     * Gets the current number of tasks that have failed.
     *
     * @return the number of tasks that have failed
     */
    public final int getFailedTaskCount() {
        return failedTasks.get();
    }

    /**
     * Gets the current number of tasks that were canceled before they started.
     *
     * @return the number of tasks that were canceled before they started
     */
    public final int getCanceledTaskCount() {
        return canceledTasks.get();
    }

    /**
     * Gets a list of Strings describing each failed task, including the exception that caused
     * the failure.  Each String represents a single invocation of a task.  This method can only
     * be invoked when the job has finished.
     *
     * @return a List of Strings detailing each failure
     */
    public final synchronized List<String> getAllFailures() {
        if (state == State.COMPLETED) {
            return new ArrayList<>();
        }

        if (!isFinishedState(state)) {
            throw new IllegalStateException("Please wait to get failure information until job is"
                    + " finished.");
        }

        List<String> failures = new ArrayList<>();
        for (Map.Entry<Callable<T>, List<Future<T>>> entry : results.entrySet()) {
            for (Future future : entry.getValue()) {
                try {
                    future.get();
                } catch (Exception e) {
                    failures.add(String.format(
                            "An invocation of task %s failed: %s", entry.getKey(), e
                    ));
                }
            }
        }

        return failures;
    }

    /**
     * Gets a list of Strings describing a certain failed task, including the exception that caused
     * the failure.  As the same task can be scheduled multiple times for a single Job, each String
     * represents a single invocation of the specified task.  This method can only
     * be invoked when the job has finished.
     *
     * @param task the task for which to retrieve failure information, cannot be null
     * @return a List of Strings detailing each failure
     */
    public final synchronized List<String> getFailures(final Callable<T> task) {
        if (task == null) {
            throw new IllegalArgumentException("Task parameter cannot be null.");
        }

        if (!isFinishedState(state)) {
            throw new IllegalStateException("Please wait to get failure information until job is"
                    + " finished.");
        }

        List<String> failures = new ArrayList<>();

        if (!results.containsKey(task)) {
            return failures;
        }

        for (Future<T> future : results.get(task)) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException | CancellationException e) {
                failures.add(String.format(
                        "An invocation of task %s failed: %s", task, e
                ));
            }
        }

        return failures;
    }

    /**
     * Use this method to retrieve the values of the successful completions of a certain task.  As
     * the same task can be scheduled multiple times for a single Job, each value represents a
     * single invocation of the specified task.  This method can only
     * be invoked when the job has finished.
     *
     * @param task the task for which to retrieve the return values, cannot be null
     * @return the List of values returned by successful completions of the given task
     */
    public final synchronized List<T> getResults(final Callable<T> task) {
        if (task == null) {
            throw new IllegalArgumentException("Task parameter cannot be null.");
        }

        if (!isFinishedState(state)) {
            throw new IllegalStateException("Please wait to get task results until all job"
                    + " tasks finish.");
        }

        List<T> resultValues = new ArrayList<>();

        if (!results.containsKey(task)) {
            return resultValues;
        }

        for (Future<T> future : results.get(task)) {
            try {
                resultValues.add(future.get());
            } catch (ExecutionException e) {
                LOGGER.error(String.format("Exception thrown by %s.", task), e);
            } catch (InterruptedException | CancellationException ignored) {
                LOGGER.debug(String.format("Interruption or cancellation on %s.", task));
            }
        }

        return resultValues;
    }

    /**
     * Gets the result of the onFinish task, if one was specified at construction.  If none was
     * specified, this method will return null.  If one was specified and executed without throwing
     * an exception, its return value is returned from this method.  It one was specified and threw
     * an exception, this method will throw an ExecutionException that includes the details of the
     * original exception.
     *
     * @return the value returned by the onFinish handler, or null if none was specified
     * @throws ExecutionException if the onFinish task did not complete successfully
     */
    public final synchronized T getOnFinishResult() throws ExecutionException {
        if (onFinish == null) {
            return null;
        }

        if (onFinishThrew == null) {
            return onFinishResult;
        }

        throw new ExecutionException("The onFinish task threw an exception", onFinishThrew);
    }
}
