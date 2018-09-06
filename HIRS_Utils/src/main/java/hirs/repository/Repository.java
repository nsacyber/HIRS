package hirs.repository;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hirs.data.persist.UserDefinedEntity;
import hirs.persist.RepositoryManager;
import hirs.persist.ScheduledJobInfo;
import hirs.utils.Job;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.LocalTime;
import org.springframework.util.Assert;

import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * This interface defines the methods by which the rest of HIRS can interact with a software
 * repository.
 *
 * @param <T> the type of packages an implementing Repository contains
 */
@Entity
public abstract class Repository<T extends RepoPackage> extends UserDefinedEntity {
    private static final Logger LOGGER = LogManager.getLogger(Repository.class);

    /**
     * The default job frequency of 1 day in milliseconds.
     */
    public static final long DEFAULT_JOB_FREQUENCY_MS = 86400000;

    /**
     * The default start time of 21:00.
     */
    public static final LocalTime DEFAULT_START_TIME = new LocalTime(21, 0, 0);

    /**
     * Minimum periodic update interval is once every hour.
     */
    public static final long MINIMUM_PERIODIC_UPDATE_INTERVAL = 3600000;

    /**
     * Creates a new <code>ScheduledJobInfo</code> with default values.
     *
     * @return the default ScheduledJobInfo
     */
    public static ScheduledJobInfo createDefaultScheduledJobInfo() {
        return new ScheduledJobInfo(DEFAULT_JOB_FREQUENCY_MS, DEFAULT_START_TIME);
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "sourceRepository",
            targetEntity = RepoPackage.class, cascade = CascadeType.REMOVE)
    @JsonIgnore
    private Set<T> packages = new HashSet<>();

    @Embedded
    private ScheduledJobInfo scheduledJobInfo;

    /**
     * Construct a new Repository with a given name.
     *
     * @param name the name by which this Repository will be referenced
     */
    public Repository(final String name) {
        super(name);
        scheduledJobInfo = createDefaultScheduledJobInfo();
    }

    /**
     * Protected default constructor for Hibernate.
     */
    protected Repository() {
        super();
    }

    /**
     * Gets the current set of packages in this repository that have been measured.
     *
     * @return the repository's set of packages it has measured
     */
    public Set<T> getPackages() {
        return Collections.unmodifiableSet(packages);
    }

    /**
     * Set the packages contained in this <code>Repository</code>.
     * @param newPackages the packages to set
     */
    public final void setPackages(final Set<T> newPackages) {
        packages.clear();
        packages.addAll(newPackages);
    }

    /**
     * Search this repository for every measured package that is an update of the given package.
     *
     * @param oldPackage the package to find updates for
     * @return a <code>Set</code> of <code>RepoPackage</code>s that are updates of the given package
     */
    public abstract Set<RepoPackage> getUpdatedPackages(RepoPackage oldPackage);

    /**
     * This method accesses the repository and assembles a <code>Set</code> representing
     * the collection of packages available in that repository.
     *
     * @return a <code>Set</code> of unmeasured <code>RepoPackage</code> objects representing the
     * software in the repository
     *
     * @throws RepositoryException if an error occurs processing the repository
     */
    protected abstract Set<T> listRemotePackages() throws RepositoryException;

    /**
     * This method retrieves the given package and measures its contents.  The resulting
     * measurements are stored in the given <code>RepoPackage</code> objects.  The measurements
     * are a set of {@link hirs.data.persist.IMABaselineRecord}s
     * that describe the full file paths and their hashes
     * that a software package contains.  The software package itself will also be measured, and
     * the measurement will be recorded in the RepoPackage.
     *
     * @param repoPackage the package to measure
     * @param maxDownloadAttempts the package download attempt limit
     *
     * @throws RepositoryException if there is an error communicatin with the repository
     */
    protected abstract void measurePackage(T repoPackage, int maxDownloadAttempts)
            throws RepositoryException;

    /**
     * Creates an update job for this repository, given the current packages it contains and the
     * packages in the actual remote repository.  Packages found in the remote repository that are
     * not contained in this instance will be downloaded, measured, and persisted during this job.
     *
     * @param repoMan the repository manager, used for persistence
     * @param maxDownloadAttempts the max number of attempts that will be made to download a package
     * @return the Job to be executed, or null if no Job is required (no new packages to measure)
     * @throws RepositoryException if an error occurs processing the repository
     */
    protected final Job<Boolean> createUpdateJob(
            final RepositoryManager repoMan,
            final int maxDownloadAttempts) throws RepositoryException {
        Set<T> packagesToMeasure = listRemotePackages();
        packagesToMeasure.removeAll(this.packages);

        if (packagesToMeasure.size() == 0) {
            return null;
        }

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (final T p : packagesToMeasure) {
            tasks.add(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    try {
                        measurePackage(p, maxDownloadAttempts);
                        repoMan.saveRepoPackage(p);
                    } catch (Exception e) {
                        LOGGER.error(String.format("Measurement of package %s failed", p), e);
                        throw e;
                    }
                    return true;
                }

                @Override
                public String toString() {
                    return String.format("\"Task measuring %s\"", p);

                }
            });
        }

        return new Job<>(tasks);
    }

    /**
     * Gets the ScheduleJobInfo for this Repository.
     * @return the SecheduleJobInfo
     */
    public ScheduledJobInfo getScheduledJobInfo() {
        return scheduledJobInfo;
    }

    /**
     * Sets the ScheduleJobInfo for this Repository.
     * @param scheduledJobInfo the ScheduleJobInfo
     */
    public void setScheduledJobInfo(final ScheduledJobInfo scheduledJobInfo) {
        Assert.notNull(scheduledJobInfo, "scheduledJobInfo");
        this.scheduledJobInfo = scheduledJobInfo;
    }

    @Override
    public String toString() {
        return String.format("Repository{name=%s}", getName());
    }
}
