package hirs.persist;

import hirs.FilteredRecordsList;
import hirs.data.persist.ArchivableEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.criterion.Criterion;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic database manager for managing objects in a database. This provides create, read, update,
 * archive, and delete operations for managing objects in a database.
 *
 */
public class DBManager<T> extends AbstractDbManager<T> {
    private static final Logger LOGGER = LogManager.getLogger(DBManager.class);

    /**
     * The default maximum number of retries to attempt a database transaction.
     */
    public static final int DEFAULT_MAX_RETRY_ATTEMPTS = 10;
    /*
     * The default number of milliseconds to wait before retrying a database transaction.
     */
    private static final long DEFAULT_RETRY_WAIT_TIME_MS = 3000;

    // structure for retrying methods in the database
    private RetryTemplate retryTemplate;

    /**
     * An enum to describe various supported DB implementations.
     */
    public enum DBImpl {
        /**
         * Represents HSQL DB.
         */
        HSQL,

        /**
         * Represents MySQL DB.
         */
        MYSQL
    }

    /**
     * Creates a new <code>DBManager</code> that uses the default database. The
     * default database is used to store all of the objects.
     *
     * @param clazz Class to search for when doing Hibernate queries
     * @param sessionFactory the session factory to use to connect to the database
     * unfortunately class type of T cannot be determined using only T
     */
    public DBManager(final Class<T> clazz, final SessionFactory sessionFactory) {
        super(clazz, sessionFactory);
        setRetryTemplate(DEFAULT_MAX_RETRY_ATTEMPTS, DEFAULT_RETRY_WAIT_TIME_MS);
    }

    /**
     * Set the parameters used to retry database transactions.  The retry template will
     * retry transactions that throw a LockAcquisitionException or StaleObjectStateException.
     * @param  maxTransactionRetryAttempts the maximum number of database transaction attempts
     * @param retryWaitTimeMilliseconds the transaction retry wait time in milliseconds
     */
    public final void setRetryTemplate(final int maxTransactionRetryAttempts,
                                               final long retryWaitTimeMilliseconds) {
        Map<Class<? extends Throwable>, Boolean> exceptionsToRetry = new HashMap<>();
        exceptionsToRetry.put(LockAcquisitionException.class, true);
        exceptionsToRetry.put(StaleObjectStateException.class, true);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                maxTransactionRetryAttempts,
                exceptionsToRetry,
                true,
                false
        );

        FixedBackOffPolicy backoffPolicy = new FixedBackOffPolicy();
        backoffPolicy.setBackOffPeriod(retryWaitTimeMilliseconds);
        this.retryTemplate = new RetryTemplate();
        this.retryTemplate.setRetryPolicy(retryPolicy);
        this.retryTemplate.setBackOffPolicy(backoffPolicy);
    }

    /**
     * Registers a retry listener to be notified of retry activity.
     * @param retryListener the retry listener
     */
    public void addRetryListener(final RetryListener retryListener) {
        retryTemplate.registerListener(retryListener);
    }

    /**
     * Runs a Criteria query using the given collection of Criterion over the
     * associated class.
     *
     * @param criteriaCollection the collection of Criterion to apply
     *
     * @return a List of objects that match the criteria
     * @throws DBManagerException if an error is encountered while performing the query or creating
     * the result objects
     */
    public final List<T> getWithCriteria(final Collection<Criterion> criteriaCollection)
            throws DBManagerException {
        return retryTemplate.execute(
            new RetryCallback<List<T>, DBManagerException>() {
                @Override
                public List<T> doWithRetry(final RetryContext context)
                        throws DBManagerException {
                    return doGetWithCriteria(criteriaCollection);
                }
            });
    }

    /**
     * Runs a Criteria query using the given collection of Criterion over the
     * associated class.
     *
     * @param clazzToGet the class of object to retrieve
     * @param criteriaCollection the collection of Criterion to apply
     *
     * @return a List of objects that match the criteria
     * @throws DBManagerException if an error is encountered while performing the query or creating
     * the result objects
     */
    protected final List<T> getWithCriteria(
            final Class<T> clazzToGet,
            final Collection<Criterion> criteriaCollection) throws DBManagerException {
        return retryTemplate.execute(
                new RetryCallback<List<T>, DBManagerException>() {
                    @Override
                    public List<T> doWithRetry(final RetryContext context)
                            throws DBManagerException {
                        return doGetWithCriteria(clazzToGet, criteriaCollection);
                    }
                });
    }

    /**
     * Deletes all instances of the associated class.
     *
     * @return the number of entities deleted
     */
    @Override
    public final int deleteAll() {
        return retryTemplate.execute(
            new RetryCallback<Integer, DBManagerException>() {
                @Override
                public Integer doWithRetry(final RetryContext context)
                        throws DBManagerException {
                    return doDeleteAll();
                }
            });
    }

    /**
     * Saves the <code>Object</code> in the database. This creates a new
     * database session and saves the object. If the <code>Object</code> had
     * previously been saved then a <code>DBManagerException</code> is thrown.
     *
     * @param object object to save
     * @return reference to saved object
     * @throws DBManagerException if object has previously been saved or an
     * error occurs while trying to save it to the database
     */
    public final T save(final T object) throws DBManagerException {
        return retryTemplate.execute(new RetryCallback<T, DBManagerException>() {
            @Override
            public T doWithRetry(final RetryContext context) throws DBManagerException {
                return doSave(object);
            }
        });
    }

    /**
     * Updates an object stored in the database. This updates the database
     * entries to reflect the new values that should be set.
     *
     * @param object object to update
     * @throws DBManagerException if an error occurs while trying to save it to the database
     */
    public final void update(final T object) throws DBManagerException {
        retryTemplate.execute(new RetryCallback<Void, DBManagerException>() {
            @Override
            public Void doWithRetry(final RetryContext context) throws DBManagerException {
                doUpdate(object);
                return null;
            }
        });
    }

    /**
     * Retrieves the <code>Object</code> from the database. This searches the
     * database for an entry whose name matches <code>name</code>. It then
     * reconstructs the <code>Object</code> from the database entry.
     *
     * @param name name of the object
     * @return object if found, otherwise null.
     * @throws DBManagerException if unable to search the database or recreate
     * the <code>Object</code>
     */
    public final T get(final String name) throws DBManagerException {
        return retryTemplate.execute(new RetryCallback<T, DBManagerException>() {
            @Override
            public T doWithRetry(final RetryContext context) throws DBManagerException {
                return doGet(name);
            }
        });
    }

    /**
     * Retrieves the <code>Object</code> from the database. This searches the
     * database for an entry whose id matches <code>id</code>. It then
     * reconstructs the <code>Object</code> from the database entry.
     *
     * @param id id of the object
     * @return object if found, otherwise null.
     * @throws DBManagerException if unable to search the database or recreate
     * the <code>Object</code>
     */
    public final T get(final Serializable id) throws DBManagerException {
        return retryTemplate.execute(new RetryCallback<T, DBManagerException>() {
            @Override
            public T doWithRetry(final RetryContext context) throws DBManagerException {
                return doGet(id);
            }
        });
    }

    /**
     * Retrieves the <code>Object</code> from the database. This searches the
     * database for an entry whose name matches <code>name</code>. It then
     * reconstructs the <code>Object</code> from the database entry.  It will also
     * load all the lazy fields in the given class.  If the parameter <code>recurse</code>
     * is set to true, this method will recursively descend into each of the object's fields
     * to load all the lazily-loaded entities.  If false, only the fields belonging to the object
     * itself will be loaded.
     *
     * @param name name of the object
     * @param recurse whether to recursively load lazy data throughout the object's structures
     * @return object if found, otherwise null.
     * @throws DBManagerException if unable to search the database or recreate
     * the <code>Object</code>
     */
    public final T getAndLoadLazyFields(final String name, final boolean recurse)
            throws DBManagerException {
        return retryTemplate.execute(new RetryCallback<T, DBManagerException>() {
            @Override
            public T doWithRetry(final RetryContext context) throws DBManagerException {
                return null;
//                return doGetAndLoadLazyFields(name, recurse);
            }
        });
    }

    /**
     * Retrieves the <code>Object</code> from the database. This searches the
     * database for an entry whose id matches <code>id</code>. It then
     * reconstructs the <code>Object</code> from the database entry.  It will also
     * load all the lazy fields in the given class.  If the parameter <code>recurse</code>
     * is set to true, this method will recursively descend into each of the object's fields
     * to load all the lazily-loaded entities.  If false, only the fields belonging to the object
     * itself will be loaded.
     *
     * @param id id of the object
     * @param recurse whether to recursively load lazy data throughout the object's structures
     * @return object if found, otherwise null.
     * @throws DBManagerException if unable to search the database or recreate
     * the <code>Object</code>
     */
    public final T getAndLoadLazyFields(final Serializable id, final boolean recurse)
            throws DBManagerException {
        return null;
//        return doGetAndLoadLazyFields(id, recurse);
    }

    /**
     * Returns a list of all <code>T</code>s of type <code>clazz</code> in the database, with no
     * additional restrictions.
     * <p>
     * This would be useful if <code>T</code> has several subclasses being
     * managed. This class argument allows the caller to limit which types of
     * <code>T</code> should be returned.
     *
     * @param entity class type of <code>T</code>s to search for (may be null to
     * use Class&lt;T&gt;)
     * @return list of <code>T</code> names
     * @throws DBManagerException if unable to search the database
     */
    public List<T> getList(final T entity)
            throws DBManagerException {
        return getList(entity, null);
    }

    /**
     * Returns a list of all <code>T</code>s of type <code>clazz</code> in the database, with
     * additional restrictions.
     * <p>
     * This would be useful if <code>T</code> has several subclasses being
     * managed. This class argument allows the caller to limit which types of
     * <code>T</code> should be returned.
     *
     * @param entity class type of <code>T</code>s to search for (may be null to
     * use Class&lt;T&gt;)
     * @param additionalRestriction additional restrictions to apply to criteria.
     * @return list of <code>T</code> names
     * @throws DBManagerException if unable to search the database
     */
    @Override
    public List<T> getList(final T entity, final Criterion additionalRestriction)
            throws DBManagerException {
        return retryTemplate.execute(new RetryCallback<List<T>, DBManagerException>() {
            @Override
            public List<T> doWithRetry(final RetryContext context) throws DBManagerException {
                return doGetList(entity, additionalRestriction);
            }
        });
    }

    /**
     * Returns a list of all <code>T</code>s that are ordered by a column and
     * direction (ASC, DESC) that is provided by the user. This method helps
     * support the server-side processing in the JQuery DataTables.
     *
     * @param clazz class type of <code>T</code>s to search for (may be null to
     * use Class&lt;T&gt;)
     * @param columnToOrder Column to be ordered
     * @param ascending direction of sort
     * @param firstResult starting point of first result in set
     * @param maxResults total number we want returned for display in table
     * @param search string of criteria to be matched to visible columns
     * @param searchableColumns Map of String and boolean values with column
     * headers and whether they should be searched. Boolean is true if field provides a
     * typical String that can be searched by Hibernate without transformation.
     * @return FilteredRecordsList object with query data
     * @throws DBManagerException if unable to create the list
     */
    @Override
    public final FilteredRecordsList getOrderedList(
            final Class<T> clazz, final String columnToOrder,
            final boolean ascending, final int firstResult,
            final int maxResults, final String search,
            final Map<String, Boolean> searchableColumns)
            throws DBManagerException {

        CriteriaModifier defaultModifier = new CriteriaModifier() {
            @Override
            public void modify(final Criteria criteria) {
                // Do nothing
            }
        };

        return getOrderedList(clazz, columnToOrder, ascending, firstResult, maxResults, search,
                searchableColumns, defaultModifier);
    }

    /**
     * Returns a list of all <code>T</code>s that are ordered by a column and
     * direction (ASC, DESC) that is provided by the user. This method helps
     * support the server-side processing in the JQuery DataTables. For entities that support
     * soft-deletes, the returned list does not contain <code>T</code>s that have been soft-deleted.
     *
     * @param clazz class type of <code>T</code>s to search for (may be null to
     * use Class&lt;T&gt;)
     * @param columnToOrder Column to be ordered
     * @param ascending direction of sort
     * @param firstResult starting point of first result in set
     * @param maxResults total number we want returned for display in table
     * @param search string of criteria to be matched to visible columns
     * @param searchableColumns Map of String and boolean values with column
     * headers and whether they should be searched. Boolean is true if field provides a
     * typical String that can be searched by Hibernate without transformation.
     * @param criteriaModifier a way to modify the criteria used in the query
     * @return FilteredRecordsList object with query data
     * @throws DBManagerException if unable to create the list
     */
    @SuppressWarnings("checkstyle:parameternumber")
    public final FilteredRecordsList<T> getOrderedList(
            final Class<T> clazz, final String columnToOrder,
            final boolean ascending, final int firstResult,
            final int maxResults, final String search,
            final Map<String, Boolean> searchableColumns, final CriteriaModifier criteriaModifier)
            throws DBManagerException {

        return retryTemplate.execute(
                new RetryCallback<FilteredRecordsList<T>, DBManagerException>() {
                    @Override
                    public FilteredRecordsList<T> doWithRetry(final RetryContext context)
                            throws DBManagerException {
                        return doGetOrderedList(clazz, columnToOrder, ascending,
                                firstResult, maxResults,
                                search, searchableColumns, criteriaModifier);
                    }
                });
    }

    /**
     * Deletes the object from the database. This removes all of the database
     * entries that stored information with regards to the this object.
     * <p>
     * If the object is referenced by any other tables then this will throw a
     * <code>DBManagerException</code>.
     *
     * @param name name of the object to delete
     * @return true if successfully found and deleted the object
     * @throws DBManagerException if unable to find the baseline or delete it
     * from the database
     */
    public final boolean delete(final String name) throws DBManagerException {
        return retryTemplate.execute(new RetryCallback<Boolean, DBManagerException>() {
            @Override
            public Boolean doWithRetry(final RetryContext context) throws DBManagerException {
                return doDelete(name);
            }
        });
    }


    /**
     * Deletes the object from the database. This removes all of the database
     * entries that stored information with regards to the this object.
     * <p>
     * If the object is referenced by any other tables then this will throw a
     * <code>DBManagerException</code>.
     *
     * @param id id of the object to delete
     * @return true if successfully found and deleted the object
     * @throws DBManagerException if unable to find the baseline or delete it
     * from the database
     */
    public final boolean deleteById(final Serializable id)
            throws DBManagerException {
        return retryTemplate.execute(new RetryCallback<Boolean, DBManagerException>() {
            @Override
            public Boolean doWithRetry(final RetryContext context) throws DBManagerException {
                return doDelete(id);
            }
        });
    }

    /**
     * Deletes the object from the database. This removes all of the database
     * entries that stored information with regards to the this object.
     * <p>
     * If the object is referenced by any other tables then this will throw a
     * <code>DBManagerException</code>.
     *
     * @param object object to delete
     * @return true if successfully found and deleted the object
     * @throws DBManagerException if unable to delete the object from the database
     */
    @Override
    public final boolean delete(final Class<T> object) throws DBManagerException {
        return retryTemplate.execute(new RetryCallback<Boolean, DBManagerException>() {
            @Override
            public Boolean doWithRetry(final RetryContext context) throws DBManagerException {
                return doDelete(object);
            }
        });
    }

    /**
     * Archives the named object and updates it in the database.
     *
     * @param name name of the object to archive
     * @return true if the object was successfully found and archived, false if the object was not
     * found
     * @throws DBManagerException if the object is not an instance of <code>ArchivableEntity</code>
     */
    @Override
    public final boolean archive(final String name) throws DBManagerException {
        LOGGER.debug("archiving object: {}", name);
        if (name == null) {
            LOGGER.debug("null name argument");
            return false;
        }

        T target = get(name);
        if (target == null) {
            return false;
        }
        if (!(target instanceof ArchivableEntity)) {
            throw new DBManagerException("unable to archive non-archivable object");
        }

        ((ArchivableEntity) target).archive();
        update(target);
        return true;
    }
}
