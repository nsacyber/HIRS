package hirs.persist;

import hirs.data.persist.certificate.Certificate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is used to persist and retrieve {@link Certificate}s into and from a database.
 */
@Service
public class DBCertificateManager extends DBManager<Certificate>
        implements CertificateManager {

    private static final Logger LOGGER = LogManager.getLogger(DBCertificateManager.class);

    // structure for retrying methods in the database
    private RetryTemplate retryTemplate;

    /**
     * Creates a new {@link DBCertificateManager} that uses the default
     * database.
     *
     * @param sessionFactory session factory used to access database connections
     */
    public DBCertificateManager(final SessionFactory sessionFactory) {
        super(Certificate.class, sessionFactory);
    }

    /**
     * This method does not need to be used directly as it is used by {@link CertificateSelector}'s
     * get* methods.  Regardless, it may be used to retrieve certificates by other code in this
     * package, given a configured CertificateSelector.
     *
     * Example:
     *
     * <pre>
     * {@code
     * CertificateSelector certSelector =
     *      new CertificateSelector(Certificate.Type.CERTIFICATE_AUTHORITY)
     *      .byIssuer("CN=Some certain issuer");
     *
     * Set<Certificate> certificates = certificateManager.get(certSelector);}
     * </pre>
     *
     * @param <T> the type of certificate that will be retrieved
     * @param certificateSelector a configured {@link CertificateSelector} to use for querying
     * @return the resulting set of Certificates, possibly empty
     */
    @SuppressWarnings("unchecked")
    public <T extends Certificate> Set<T> getCertificate(
            final CertificateSelector certificateSelector) {
        return new HashSet<>(0
//                (List<T>) getWithCriteria(
//                    certificateSelector.getCertificateClass(),
//                    Collections.singleton(certificateSelector.getCriterion())
//                )
        );
    }

    /**
     * Remove a certificate from the database.
     *
     * @param certificate the certificate to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteCertificate(final Certificate certificate) {
        return delete(certificate);
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
    @Override
    public Certificate saveCertificate(final Certificate object) throws DBManagerException {
        return retryTemplate.execute(new RetryCallback<Certificate, DBManagerException>() {
            @Override
            public Certificate doWithRetry(final RetryContext context) throws DBManagerException {
                return doSave(object);
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
     * @param object instance of the object to delete
     * @return true if successfully found and deleted the object
     * @throws DBManagerException if unable to find the baseline or delete it
     * from the database
     */
    public final boolean delete(final Certificate object) throws DBManagerException {
        return retryTemplate.execute(new RetryCallback<Boolean, DBManagerException>() {
            @Override
            public Boolean doWithRetry(final RetryContext context) throws DBManagerException {
                return doDelete(object);
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
    public final void updateCertificate(final Certificate object) throws DBManagerException {
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
     * @param id id of the object
     * @return object if found, otherwise null.
     * @throws DBManagerException if unable to search the database or recreate
     * the <code>Object</code>
     */
    public final Certificate getCertificate(final Serializable id) throws DBManagerException {
        return retryTemplate.execute(new RetryCallback<Certificate, DBManagerException>() {
            @Override
            public Certificate doWithRetry(final RetryContext context) throws DBManagerException {
                return doGet(id);
            }
        });
    }

//    /**
//     * Set the parameters used to retry database transactions.  The retry template will
//     * retry transactions that throw a LockAcquisitionException or StaleObjectStateException.
//     * @param  maxTransactionRetryAttempts the maximum number of database transaction attempts
//     * @param retryWaitTimeMilliseconds the transaction retry wait time in milliseconds
//     */
//    public final void setRetryTemplate(final int maxTransactionRetryAttempts,
//                                       final long retryWaitTimeMilliseconds) {
//        Map<Class<? extends Throwable>, Boolean> exceptionsToRetry = new HashMap<>();
//        exceptionsToRetry.put(LockAcquisitionException.class, true);
//        exceptionsToRetry.put(StaleObjectStateException.class, true);
//
//        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
//                maxTransactionRetryAttempts,
//                exceptionsToRetry,
//                true,
//                false
//        );
//
//        FixedBackOffPolicy backoffPolicy = new FixedBackOffPolicy();
//        backoffPolicy.setBackOffPeriod(retryWaitTimeMilliseconds);
//        this.retryTemplate = new RetryTemplate();
//        this.retryTemplate.setRetryPolicy(retryPolicy);
//        this.retryTemplate.setBackOffPolicy(backoffPolicy);
//    }
}
