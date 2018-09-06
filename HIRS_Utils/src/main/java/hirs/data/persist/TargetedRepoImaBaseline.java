package hirs.data.persist;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import hirs.persist.RepositoryManager;
import hirs.repository.RepoPackage;
import hirs.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

/**
 * An IMA baseline that references a specified set of <code>RepoPackage</code>s. These packages are
 * used during appraisal to generate a set of measurement records. An update() will use the source
 * repository referenced within each package to refresh the measurements of that package.
 *
 */
@Entity
@Access(AccessType.FIELD)
public class TargetedRepoImaBaseline extends QueryableRecordImaBaseline
        implements UpdatableImaBaseline {
    private static final Logger LOGGER = LogManager.getLogger(TargetedRepoImaBaseline.class);
    private static final int MAX_REPO_CACHE_SIZE = 10;

    /**
     * Name of the repoPackages field.
     */
    public static final String REPO_PACKAGES_FIELD = "repoPackages";

    /**
     * The packages used for an appraisal against this baseline.
     */
    @OneToMany(fetch = FetchType.EAGER)
    private Set<RepoPackage> repoPackages;

    @Transient
    private final Set<IMABaselineRecord> cachedBaselineRecords = new HashSet<>();

    @Transient
    private boolean shouldUpdateCache = true;

    /**
     * Constructor used to initialize a <code>TargetedRepoImaBaseline</code> with the given name and
     * an empty <code>Set</code> of <code>RepoPackage</code>s.
     * @param name the name of the new baseline
     */
    public TargetedRepoImaBaseline(final String name) {
        super(name);
        repoPackages = new HashSet<>();
    }

    /** Default constructor necessary for Hibernate. Makes an empty <code>Set</code> of
     * <code>RepoPackage</code>s.
     */
    protected TargetedRepoImaBaseline() {
        super();
        repoPackages = new HashSet<>();
    }

    /**
     * Iterate through the <code>RepoPackage</code>s referenced in this baseline and check the
     * associated repository for any updated versions or releases.
     *
     * @param repositoryManager a repository manager to use to gather data about repositories, or
    *                     null if object already contains all info (no lazy-loaded data)
     */
    @Override
    public final void update(final RepositoryManager repositoryManager) {
        LoadingCache<UUID, Repository<? extends RepoPackage>> repositoryCache =
                CacheBuilder.newBuilder()
                .maximumSize(MAX_REPO_CACHE_SIZE)
                .build(
                        new CacheLoader<UUID, Repository<? extends RepoPackage>>() {
                            @Override
                            public Repository<? extends RepoPackage> load(final UUID id)
                                    throws Exception {
                                LOGGER.debug("Retrieving repository {}", id);
                                return repositoryManager.getRepository(id);
                            }
                        }
                );

        Set<RepoPackage> newPackages = new HashSet<>();
        Repository<? extends RepoPackage> repoWithPackages;

        for (RepoPackage baselineRepoPackage : repoPackages) {
            try {
                if (repositoryManager != null) {
                    repoWithPackages = repositoryCache.get(
                            baselineRepoPackage.getSourceRepository().getId()
                    );
                } else {
                    repoWithPackages = baselineRepoPackage.getSourceRepository();
                }
            } catch (ExecutionException e) {
                throw new RuntimeException("Couldn't retrieve persisted repository via cache.");
            }
            newPackages.addAll(repoWithPackages.getUpdatedPackages(baselineRepoPackage));
        }

        repoPackages.addAll(newPackages);

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
     * @return the RepoPackages tracked by this baseline
     */
    public final Set<RepoPackage> getRepositoryPackages() {
        return Collections.unmodifiableSet(repoPackages);
    }

    /**
     * Returns the actual <code>Set</code> that contains the IMA records. This is needed for
     * Hibernate.
     *
     * @return RepoPackages
     */
    private Set<RepoPackage> getRepoPackages() {
        return this.repoPackages;
    }

    /**
     * Set the <code>Set</code> of <code>RepoPackage</code>s associated with this baseline. Also
     * flips the map update needed flags so the next call to contains will require map updates.
     *
     * @param newRepoPackages the new RepoPackages to be tracked by this baseline
     */
    public final void setRepoPackages(final Set<RepoPackage> newRepoPackages) {
        this.repoPackages = newRepoPackages;
    }

    /**
     * Adds to the <code>Set</code> of <code>RepoPackage</code>s. Also
     * flips the map update needed flags so the next call to contains will require map updates.
     *
     * @param repoPackage the new RepoPackage to be tracked by this baseline
     */
    public final void addRepoPackage(final RepoPackage repoPackage) {
        this.repoPackages.add(repoPackage);
    }

    /**
     * Deletes from the <code>Set</code> of <code>RepoPackage</code>s. Also
     * flips the map update needed flags so the next call to contains will require map updates.
     *
     * @param repoPackage this package should no longer be tracked by this baseline
     */
    public final void removeRepoPackage(final RepoPackage repoPackage) {
        this.repoPackages.remove(repoPackage);
    }


    /**
     * Checks if this baseline's set of packages is associated with the specified set of
     * repositories. This is determined by checking each repo package's source repository.
     * @param repositories the set of repositories to check for association
     * @return true if any repository packages are linked to the set of specified repositories
     */
    public boolean isAssociatedWithRepositories(final Collection<Repository> repositories) {

        Set<RepoPackage> repositoryPackages = getRepositoryPackages();

        if (CollectionUtils.isEmpty(repositoryPackages) || CollectionUtils.isEmpty(repositories)) {
            return false;
        }

        for (RepoPackage repoPackage : repositoryPackages) {
            if (repositories.contains(repoPackage.getSourceRepository())) {
                return true;
            }
        }

        return false;
    }
}
