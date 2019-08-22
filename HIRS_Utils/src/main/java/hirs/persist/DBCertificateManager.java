package hirs.persist;

import hirs.data.persist.SupplyChainValidationSummary;
import hirs.data.persist.certificate.Certificate;
import org.apache.logging.log4j.util.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class is used to persist and retrieve {@link Certificate}s into and from a database.
 */
public class DBCertificateManager extends DBManager<Certificate>
        implements CertificateManager {

    private static final Logger LOGGER = LogManager.getLogger(DBCertificateManager.class);

    private SupplyChainValidationSummary summaryManager;

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
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Certificate> Set<T> get(final CertificateSelector certificateSelector) {
        return new HashSet<>(
                (List<T>) getWithCriteria(
                    certificateSelector.getCertificateClass(),
                    Collections.singleton(certificateSelector.getCriterion())
                )
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
     * Setter for a Supply Chain Validation Summary.
     * @param scvSummary the value to be stored.
     */
    public void setSummary(final SupplyChainValidationSummary scvSummary) {
        LOGGER.error("TDM - Woot, we made the shot.");
        this.summaryManager = scvSummary;
    }

    /**
     * The Getter for the message associated with the summary.
     * @return a string for the message.
     */
    public String getSummaryMessage() {
        if (this.summaryManager != null) {
            return this.summaryManager.getMessage();
        } else {
            return Strings.EMPTY;
        }
    }
}
