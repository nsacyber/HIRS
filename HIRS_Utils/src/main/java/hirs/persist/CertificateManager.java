package hirs.persist;

import hirs.data.persist.certificate.Certificate;

import java.util.Set;

/**
 * This class facilitates the persistence of {@link Certificate}s, including storage, retrieval,
 * and deletion.
 */
public interface CertificateManager {
    /**
     * Persists a new Certificate.
     *
     * @param certificate the Certificate
     * @return the persisted Certificate
     */
    Certificate saveCertificate(Certificate certificate);

    /**
     * Updates an existing certificate.
     * @param certificate the cert to update
     */
    void updateCertificate(Certificate certificate);

    /**
     * Retrieve Certificates according to the given {@link CertificateSelector}.
     *
     * @param <T> the type of certificate that will be retrieved
     * @param certificateSelector a {@link CertificateSelector} to use for querying
     * @return a Set of matching Certificates, which may be empty
     */
    <T extends Certificate> Set<T> getCertificate(CertificateSelector certificateSelector);

    /**
     * Delete the given Certificate.
     *
     * @param certificate the Certificate to delete.
     * @return true if the deletion succeeded, false otherwise
     */
    boolean delete(Certificate certificate);
}
