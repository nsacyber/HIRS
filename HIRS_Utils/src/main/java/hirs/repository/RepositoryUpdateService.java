package hirs.repository;

import hirs.persist.RepositoryManager;
import hirs.utils.Job;
import hirs.utils.JobExecutor;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class provides a singleton instance to conduct threaded, asynchronous Repository updates.
 * Call RepositoryUpdateService.getInstance to retrieve the singleton instance.
 */
public final class RepositoryUpdateService {
    private static final RepositoryUpdateService SINGLETON;

    static {
        SINGLETON = new RepositoryUpdateService();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    SINGLETON.shutdownNow();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Get the singleton instance of this class.
     *
     * @return the RepositoryUpdateService instance
     */
    public static RepositoryUpdateService getInstance() {
        return SINGLETON;
    }

    private final ConcurrentHashMap<UUID, Job<Boolean>> activeJobs =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, Job<Boolean>> finishedJobs =
            new ConcurrentHashMap<>();

    private final JobExecutor jobExecutor = new JobExecutor();

    private RepositoryUpdateService() {
    }

    /**
     * Queue an update job for the given repository.
     * <p>
     * The repository will be queried for its list of packages, and any new (unmeasured) packages
     * in the repository will be downloaded measured, and persisted by the given RepositoryManager.
     * The given repository must have previously been persisted.
     * <p>
     * This method will return true if an update job has been queued.  Two cases in which it would
     * return false is if there is already an active update job for this repository underway, or if
     * the repository reported that there are no new packages to update with.
     *
     * @param repository the repository to update
     * @param maxDownloadAttempts the max number of attempts that will be made to download a package
     * @param repoMan the repository manager that will be used for persistence
     * @return true if a job was scheduled, false otherwise.
     * @throws IOException if there is an issue determining which packages should be measured
     * @throws RepositoryException if an error occurs processing the repository
     */
    public synchronized boolean startUpdateJob(final Repository<?> repository,
                                               final int maxDownloadAttempts,
                                               final RepositoryManager repoMan)
            throws IOException, RepositoryException {
        cleanupJobs();

        if (activeJobs.get(repository.getId()) != null) {
            return false;
        }

        Job<Boolean> updateJob = repository.createUpdateJob(repoMan, maxDownloadAttempts);

        if (updateJob == null) {
            return false;
        }

        activeJobs.put(repository.getId(), updateJob);
        jobExecutor.scheduleJob(updateJob);
        return true;
    }

    /**
     * Get a mapping of the active update jobs.  This mapping is from Repository to Job, which
     * allows control over the specific job (to measure progress, cancel, etc.)
     *
     * @return a mapping from Repository to Job
     */
    public synchronized HashMap<UUID, Job<Boolean>> getActiveJobs() {
        cleanupJobs();
        return new HashMap<>(activeJobs);
    }

    /**
     * Convenience method to checks if there are any active update jobs for the specified
     * repositories.
     *
     * @return true if there are 1 or more active jobs, false otherwise.
     * @param repositories set of repositories to check for active repo update jobs
     */
    public synchronized boolean hasActiveJobs(final List<Repository> repositories) {
        if (CollectionUtils.isEmpty(repositories)) {
            return false;
        }
        HashMap<UUID, Job<Boolean>> currentActiveJobs = getActiveJobs();
        if (currentActiveJobs.size() == 0) {
            return false;
        }

        // check if any of the active job IDs, which are Repository IDs, match the proided
        // set.
        for (UUID jobRepositoryId : currentActiveJobs.keySet()) {
            for (Repository repository : repositories) {
                if (repository.getId().equals(jobRepositoryId)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Get a mapping of finished update jobs.  This mapping is from Repository to Job, which
     * can give data on the success or failure of the Job.  Only the most recently-finished job for
     * each Repository is kept.
     *
     * @return a mapping from Repository to Job
     */
    public synchronized HashMap<UUID, Job<Boolean>> getFinishedJobs() {
        cleanupJobs();
        return new HashMap<>(finishedJobs);
    }

    /**
     * This method clears the history of finished jobs.
     */
    public synchronized void clearFinishedJobs() {
        cleanupJobs();
        finishedJobs.clear();
    }

    private synchronized void shutdownNow() throws InterruptedException {
        jobExecutor.shutdownNow();
    }

    private synchronized void cleanupJobs() {
        List<UUID> keysToRemove = new ArrayList<>();

        for (final Map.Entry<UUID, Job<Boolean>> entry : activeJobs.entrySet()) {
            Job<Boolean> job = entry.getValue();

            if (job.isFinished()) {
                finishedJobs.put(entry.getKey(), entry.getValue());
                keysToRemove.add(entry.getKey());
            }
        }

        for (UUID uuid : keysToRemove) {
            activeJobs.remove(uuid);
        }
    }
}
