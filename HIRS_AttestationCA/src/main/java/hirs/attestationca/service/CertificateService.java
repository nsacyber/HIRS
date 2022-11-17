package hirs.attestationca.service;

import hirs.attestationca.entity.certificate.Certificate;
import hirs.attestationca.entity.CertificateSelector;
import hirs.persist.OrderedQuery;

import java.util.Set;
import java.util.UUID;

/**
 * A <code>CertificateService</code> manages <code>Certificate</code>s. A
 * <code>CertificateService</code> is used to store and manage certificates. It has
 * support for the basic create, read, update, and delete methods.
 */
public interface CertificateService extends OrderedQuery<Certificate> {

    /**
     * Saves the <code>Certificate</code> in the database. This creates a new
     * database session and saves the certificate.
     *
     * @param certificate Certificate to save
     * @return reference to saved certificate
     */
    Certificate saveCertificate(Certificate certificate);

    /**
     * Updates a <code>Certificate</code>. This updates the database entries to
     * reflect the new values that should be set.
     *
     * @param certificate Certificate object to save
     * @return a Certificate object
     */
    Certificate updateCertificate(Certificate certificate);

    /**
     * Updates a <code>Certificate</code>. This updates the database entries to
     * reflect the new values that should be set.
     *
     * @param certificate Certificate object to save
     * @param uuid UUID for the database object
     * @return a Certificate object
     */
    Certificate updateCertificate(Certificate certificate, UUID uuid);

    /**
     * Retrieve Certificates according to the given {@link CertificateSelector}.
     *
     * @param <T> the type of certificate that will be retrieved
     * @param certificateSelector a {@link CertificateSelector} to use for querying
     * @return a Set of matching Certificates, which may be empty
     */
    <T extends Certificate> Set<T> getCertificate(CertificateSelector certificateSelector);
}
