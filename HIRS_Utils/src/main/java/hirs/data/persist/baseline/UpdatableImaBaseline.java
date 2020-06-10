package hirs.data.persist.baseline;

import hirs.persist.RepositoryManager;

/**
 * An IMA baseline that supports an update capability through which the associated records can be
 * managed by an external source, such as a Yum repository.
 *
 */
public interface UpdatableImaBaseline {
    /**
     * Update this baseline from an external source.
     *
     * @param repositoryManager a repository manager to use to gather data about repositories, or
     *                          null if object already contains all info (no lazy-loaded data)
     */
    void update(RepositoryManager repositoryManager);
}
