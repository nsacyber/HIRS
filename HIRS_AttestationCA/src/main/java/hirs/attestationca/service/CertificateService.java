package hirs.attestationca.service;

import hirs.data.persist.certificate.Certificate;

import java.util.UUID;

/**
 * A <code>CertificateService</code> manages <code>Certificate</code>s. A
 * <code>CertificateService</code> is used to store and manage certificates. It has
 * support for the basic create, read, update, and delete methods.
 */
public interface CertificateService {

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
     * @param uuid UUID for the database object
     * @return a Certificate object
     */
    Certificate updateCertificate(Certificate certificate, UUID uuid);
}
