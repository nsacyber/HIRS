package hirs.persist;

import hirs.repository.RepoPackage;
import hirs.repository.Repository;

import java.io.Serializable;
import java.util.List;

/**
 * A <code>RepositoryManager</code> manages the persistence of
 * {@link Repository} and {@link RepoPackage} instances.
 */
public interface RepositoryManager {
    /**
     * Retrieve a Repository by name.  This Repository will have a fully-populated list of
     * RepoPackages (but the packages will not be populated with records.)
     *
     * @param name the name of the Repository to retrieve
     * @return the Repository associated with the given name
     */
    Repository<?> getRepository(String name);

    /**
     * Retrieve a Repository by ID.  This Repository will have a fully-populated list of
     * RepoPackages (but the packages will not be populated with records.)
     *
     * @param id the ID of the Repository to retrieve
     * @return the Repository identified by the given ID
     */
    Repository<?> getRepository(Serializable id);

    /**
     * Retrieve a list of all Repositories of a certain type.  These Repository objects will not
     * have their RepoPackage lists populated.
     *
     * @param clazz the type of Repository to list
     * @return a list of all persisted Repositories of the given type
     */
    List<Repository> getRepositoryList(Class<Repository> clazz);

    /**
     * Persist the given Repository.
     *
     * @param repository the Repository to persist
     * @return the persisted repository
     */
    Repository<?> saveRepository(Repository<?> repository);

    /**
     * Update the given Repository.
     *
     * @param repository the repository to update
     */
    void updateRepository(Repository<?> repository);

    /**
     * Delete the given Repository.
     *
     * @param repository the repository to delete
     * @return true if the repository was deleted, false otherwise
     */
    boolean deleteRepository(Repository<?> repository);

    /**
     * Retrieve a RepoPackage by ID, including the package's records.
     *
     * @param id the ID of the RepoPackage to retrieve
     * @return the RepoPackage identified by the given ID
     */
    RepoPackage getRepoPackage(Serializable id);

    /**
     * Persist the given RepoPackage.  The RepoPackage must
     * have its measurements set before persisting.
     *
     * @param repoPackage the RepoPackage to persist
     * @return the persisted RepoPackage
     */
    RepoPackage saveRepoPackage(RepoPackage repoPackage);
}
