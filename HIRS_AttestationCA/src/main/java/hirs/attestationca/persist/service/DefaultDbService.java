package hirs.attestationca.persist.service;

import hirs.attestationca.persist.DBManagerException;
import hirs.attestationca.persist.entity.AbstractEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Log4j2
@Service
@NoArgsConstructor
public class DefaultDbService<T extends AbstractEntity> {
    /**
     * The default maximum number of retries to attempt a database transaction.
     */
    public static final int DEFAULT_MAX_RETRY_ATTEMPTS = 10;
    /*
     * The default number of milliseconds to wait before retrying a database transaction.
     */
    private static final long DEFAULT_RETRY_WAIT_TIME_MS = 3000;
    private static final int MAX_CLASS_CACHE_ENTRIES = 500;

    private Class<T> clazz;
    @PersistenceContext
    private EntityManager entityManager;
    private JpaRepository repository;
    // structure for retrying methods in the database
    private RetryTemplate retryTemplate;

    /**
     * Creates a new <code>DefaultDbService</code>.
     *
     * @param clazz Class to search for when doing Hibernate queries,
     * unfortunately class type of T cannot be determined using only T
     */
    public DefaultDbService(final Class<T> clazz) {
        setRetryTemplate();
    }

    public void defineRepository(final JpaRepository repository) {
        this.repository = repository;
    }

    public List<T> listAll() {
        return this.repository.findAll();
    }

    public void save(final T entity) {
        this.repository.save(entity);
    }

    public void delete(final T entity) {
        this.repository.delete(entity);
    }

    public void delete(final UUID id) {
        this.repository.deleteById(id);
    }

    /**
     * Set the parameters used to retry database transactions.  The retry template will
     * retry transactions that throw a LockAcquisitionException or StaleObjectStateException.
     */
    public final void setRetryTemplate() {
        Map<Class<? extends Throwable>, Boolean> exceptionsToRetry = new HashMap<>();
        exceptionsToRetry.put(LockAcquisitionException.class, true);
        exceptionsToRetry.put(StaleObjectStateException.class, true);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                DEFAULT_MAX_RETRY_ATTEMPTS,
                exceptionsToRetry,
                true,
                false
        );

        FixedBackOffPolicy backoffPolicy = new FixedBackOffPolicy();
        backoffPolicy.setBackOffPeriod(DEFAULT_RETRY_WAIT_TIME_MS);
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
     * reconstructs the <code>Object</code> from the database entry.
     *
     * @param name name of the object
     * @return object if found, otherwise null.
     * @throws DBManagerException if unable to search the database or recreate
     * the <code>Object</code>
     */
    protected T doGet(final String name) throws DBManagerException {
        log.debug("getting object: {}", name);
        if (name == null) {
            log.debug("null name argument");
            return null;
        }

        Object entity = entityManager.find(clazz, name);
        entityManager.detach(entity);

        return clazz.cast(entity);
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
    protected T doGet(final Serializable id) throws DBManagerException {
        log.debug("getting object: {}", id);
        if (id == null) {
            log.debug("null id argument");
            return null;
        }

        Object entity = entityManager.find(clazz, id);
        entityManager.detach(entity);

        return clazz.cast(entity);
    }
}
