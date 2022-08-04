package hirs.attestationca.service;

import hirs.data.persist.ArchivableEntity;
import hirs.persist.DBManagerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.LockAcquisitionException;
import org.jadira.usertype.spi.repository.JpaBaseRepository;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;


/**
 * @param <T> passed in type
 * Generic database manager for managing objects in a database. This provides create, read, update,
 * archive, and delete operations for managing objects in a database.
 *
 */
@Service
public class DbServiceImpl<T> {
    private static final Logger LOGGER = LogManager.getLogger(DbServiceImpl.class);

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
    private EntityManager em;

    /**
     * Creates a new <code>DbServiceImpl</code> that uses the default database. The
     * default database is used to store all of the objects.
     *
     */
    public DbServiceImpl() {
        setRetryTemplate(DEFAULT_MAX_RETRY_ATTEMPTS, DEFAULT_RETRY_WAIT_TIME_MS);
    }

    /**
     * Creates a new <code>DbServiceImpl</code> that uses the default database. The
     * default database is used to store all of the objects.
     *
     */
    public DbServiceImpl(final EntityManager em) {
        setRetryTemplate(DEFAULT_MAX_RETRY_ATTEMPTS, DEFAULT_RETRY_WAIT_TIME_MS);
        this.em = em;
    }

    /**
     * Set the parameters used to retry database transactions.  The retry template will
     * retry transactions that throw a LockAcquisitionException or StaleObjectStateException.
     * @param maxTransactionRetryAttempts the maximum number of database transaction attempts
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
     * Accessor method for the retry function.
     * @return instance of the RetryTemplate
     */
    protected RetryTemplate getRetryTemplate() {
        return this.retryTemplate;
    }

    /**
     * Registers a retry listener to be notified of retry activity.
     * @param retryListener the retry listener
     */
    public void addRetryListener(final RetryListener retryListener) {
        retryTemplate.registerListener(retryListener);
    }

    /**
     * Getter for the EntityManager.
     * @return instance of the manager
     */
    public final EntityManager getEm() {
        return em;
    }

    /**
     * Archives the named object and updates it in the database.
     *
     * @param name name of the object to archive
     * @return true if the object was successfully found and archived, false if the object was not
     * found
     * @throws hirs.persist.DBManagerException if the object is not an instance of <code>ArchivableEntity</code>
     */
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
