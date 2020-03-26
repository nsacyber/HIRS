package hirs.data.persist.baseline;

import hirs.persist.RepositoryManager;
import hirs.repository.Repository;
import hirs.repository.RepoPackage;
import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

/**
 * An IMA baseline that references a set of <code>Repository</code> instances. Appraisals are done
 * by using every package in each of the repositories as a baseline. An update() will synchronize
 * the local packages to the measured contents of the repositories.
 *
 */
@Entity
@Access(AccessType.FIELD)
public class BroadRepoImaBaseline extends QueryableRecordImaBaseline
        implements UpdatableImaBaseline {
    /**
     * The repositories used for an appraisal against this baseline.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Repository<?>> repositories;

    /**
     * Name of the repoPackages field.
     */
    public static final String REPO_PACKAGES_FIELD = "repoPackages";

    /**
     * The packages that have been extracted from this baseline's repositories.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<RepoPackage> repoPackages;

    @Transient
    private final Set<IMABaselineRecord> cachedBaselineRecords = new HashSet<>();

    @Transient
    private boolean shouldUpdateCache = true;

    /**
     * Constructor used to initialize a <code>BroadRepoImaBaseline</code> with the given name and
     * an empty <code>Set</code> of <code>Repository</code> instances and <code>RepoPackage</code>s.
     *
     * @param name the name of the new baseline
     */
    public BroadRepoImaBaseline(final String name) {
        super(name);
        repositories = new HashSet<>();
        repoPackages = new HashSet<>();
    }

    /** Default constructor necessary for Hibernate. Makes an empty <code>Set</code> of
     * <code>Repository</code> instances and <code>RepoPackage</code>s.
     */
    protected BroadRepoImaBaseline() {
        super();
        repositories = new HashSet<>();
        repoPackages = new HashSet<>();
    }

    /**
     * Update this baseline to match repositories that may have been updated. First clear the
     * current <code>Set</code> of <code>RepoPackage</code>s, then iterate through the
     * <code>Repository</code> objects referenced in this baseline and use all of the measured
     * packages found within each of them to populate this baseline's <code>Set</code> of
     * <code>RepoPackage</code>s.
     *
     * @param repositoryManager a repository manager to use to gather data about repositories, or
     *                          null if object already contains all info (no lazy-loaded data)
     */
    @Override
    public final void update(final RepositoryManager repositoryManager) {
        repoPackages.clear();
        for (Repository<?> repository : repositories) {
            Repository<?> repoWithPackages;
            if (repositoryManager != null) {
                repoWithPackages = repositoryManager.getRepository(repository.getId());
            } else {
                repoWithPackages = repository;
            }
            repoPackages.addAll(repoWithPackages.getPackages());
        }

        synchronized (cachedBaselineRecords) {
            this.shouldUpdateCache = true;
        }
    }

    @Override
    public final Set<IMABaselineRecord> getBaselineRecords() {
        synchronized (cachedBaselineRecords) {
            if (!shouldUpdateCache) {
                return Collections.unmodifiableSet(cachedBaselineRecords);
            }

            cachedBaselineRecords.clear();
            for (RepoPackage repoPackage : repoPackages) {
                cachedBaselineRecords.addAll(repoPackage.getPackageRecords());
            }
            shouldUpdateCache = false;

            return Collections.unmodifiableSet(cachedBaselineRecords);
        }
    }

    @Override
    public void configureCriteriaForBaselineRecords(final Criteria criteria, final int bucket) {
        criteria.add(Restrictions.eq("id", getId()))
                .setProjection(Projections.projectionList()
                        .add(Projections.property(
                                String.format("%s.%s",
                                        RepoPackage.PACKAGE_RECORDS_FIELD,
                                        IMABaselineRecord.PATH_FIELD)
                                ), IMABaselineRecord.PATH_FIELD)
                        .add(Projections.property(
                                String.format("%s.%s",
                                        RepoPackage.PACKAGE_RECORDS_FIELD,
                                        IMABaselineRecord.HASH_FIELD)
                                ), IMABaselineRecord.HASH_FIELD)
                );

        criteria.add(Restrictions.eq(
                String.format("%s.%s",
                        RepoPackage.PACKAGE_RECORDS_FIELD,
                        IMABaselineRecord.BUCKET_FIELD),
                bucket)
        );
        criteria.createAlias(REPO_PACKAGES_FIELD, REPO_PACKAGES_FIELD);
        criteria.createAlias(
                String.format("%s.%s", REPO_PACKAGES_FIELD, RepoPackage.PACKAGE_RECORDS_FIELD),
                RepoPackage.PACKAGE_RECORDS_FIELD
        );
    }

    /**
     * Get the <code>Set</code> of <code>RepoPackage</code>s associated with this baseline.
     *
     * @return the RepoPackages associated with this baseline
     */
    public final Set<RepoPackage> getRepositoryPackages() {
        return Collections.unmodifiableSet(repoPackages);
    }

    /**
     * Get the <code>Set</code> of <code>Repository</code> instances associated with this baseline.
     *
     * @return the Repositories tracked by this baseline
     */
    public final Set<Repository<?>> getRepositories() {
        return Collections.unmodifiableSet(repositories);
    }

    /**
     * Set the <code>Set</code> of <code>Repository</code> instances associated with this baseline.
     * Note that the map update needed flags do not change here. They will only change during an
     * update() because that is where the <code>RepoPackage</code>s change.
     *
     * @param newRepositories the new repositories to be tracked by this baseline
     */
    public final void setRepositories(final Set<Repository<?>> newRepositories) {
        repositories.clear();
        repositories.addAll(newRepositories);
    }
}
