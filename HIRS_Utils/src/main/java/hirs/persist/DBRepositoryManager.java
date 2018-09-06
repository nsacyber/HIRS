package hirs.persist;

import hirs.repository.RepoPackage;
import hirs.repository.Repository;
import org.hibernate.SessionFactory;

import java.io.Serializable;
import java.util.List;

/**
 * This class defines a {@link RepositoryManager} that stores Repositories and RepoPackages
 * in a database.
 */
public class DBRepositoryManager implements RepositoryManager {
    private final DBManager<Repository> repositoryDBManager;
    private final DBManager<RepoPackage> repoPackageDBManager;

    /**
     * Creates a new <code>DBRepositoryManager</code> that uses the default
     * database. The default database is used to store and retrieve all objects.
     *
     * @param sessionFactory session factory used to access database connections
     */
    public DBRepositoryManager(final SessionFactory sessionFactory) {
        super();
        repositoryDBManager = new DBManager<>(Repository.class, sessionFactory);
        repoPackageDBManager = new DBManager<>(RepoPackage.class, sessionFactory);
    }

    /**
     * Retrieve a Repository by name from the database, populated with its collection of
     * RepoPackages.  The Repository's RepoPackages do not have their sets of records populated.
     *
     * @param name the name of the Repository to retrieve
     * @return the Repository associated with the given name
     */
    @Override
    public final Repository<?> getRepository(final String name) {
        return repositoryDBManager.getAndLoadLazyFields(name, false);
    }

    /**
     * Retrieve a Repository by ID from the database, populated with its collection of RepoPackages.
     * The Repository's RepoPackages do not have their sets of records populated.
     *
     * @param id the ID of the Repository to retrieve
     * @return the Repository identified by the given ID
     */
    public final Repository<?> getRepository(final Serializable id) {
        return repositoryDBManager.getAndLoadLazyFields(id, false);
    }

    /**
     * Retrieve a listing of all Repositories by class from the database, without their collections
     * of RepoPackages.
     *
     * @param clazz the type of Repository to list
     * @return a list of Repositories
     */
    @Override
    public final List<Repository> getRepositoryList(final Class<? extends Repository> clazz) {
        return repositoryDBManager.getList(clazz);
    }

    /**
     * Saves the given Repository to the database.
     *
     * @param repository the repository to save
     * @return the saved Repository
     */
    @Override
    public final Repository<?> saveRepository(final Repository<?> repository) {
        return repositoryDBManager.save(repository);
    }

    /**
     * Updates the given Repository in the database.
     *
     * @param repository the repository to update
     */
    @Override
    public final void updateRepository(final Repository<?> repository) {
        repositoryDBManager.update(repository);
    }

    /**
     * Deletes the given Repository in the database.
     *
     * @param repository the repository to update
     * @return true if the repository was successfully deleted
     */
    @Override
    public final boolean deleteRepository(final Repository<?> repository) {
        return repositoryDBManager.delete(repository);
    }

    /**
     * Retrieves the given RepoPackage from the database.  The returned RepoPackage is populated
     * with its set of records.
     *
     * @param id the ID of the RepoPackage to retrieve
     * @return the requested RepoPackage
     */
    @Override
    public final RepoPackage getRepoPackage(final Serializable id) {
        return repoPackageDBManager.getAndLoadLazyFields(id, false);
    }

    /**
     * Saves the given RepoPackage in the database.  The RepoPackage must
     * have its measurements set before persisting.
     *
     * @param repoPackage the RepoPackage to persist
     * @return the persisted RepoPackage
     */
    @Override
    public final RepoPackage saveRepoPackage(final RepoPackage repoPackage) {
        if (!repoPackage.isMeasured()) {
            throw new IllegalArgumentException("Cannot persist an unmeasured RepoPackage.");
        }

        return repoPackageDBManager.save(repoPackage);
    }

    /**
     * Sets the retry template for the underlying managers.
     * @param maxTransactionRetryAttempts the max retry attempts
     * @param retryWaitTimeMilliseconds the retry wait time in ms
     */
    void setRetryTemplate(final int maxTransactionRetryAttempts,
                                 final long retryWaitTimeMilliseconds) {
        this.repoPackageDBManager.setRetryTemplate(maxTransactionRetryAttempts,
                retryWaitTimeMilliseconds);
        this.repositoryDBManager.setRetryTemplate(maxTransactionRetryAttempts,
                retryWaitTimeMilliseconds);
    }
}
