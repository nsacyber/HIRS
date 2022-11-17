package hirs.attestationca.entity.certificate;

import hirs.attestationca.entity.CertificateSelector;
import hirs.persist.service.CertificateService;

import javax.persistence.Entity;
import java.io.IOException;
import java.nio.file.Path;

/**
 * This class persists Conformance credentials by extending the base Certificate
 * class with fields unique to Conformance credentials.
 */
@Entity
public class ConformanceCredential extends Certificate {
    /**
     * This class enables the retrieval of ConformanceCredentials by their attributes.
     */
    public static class Selector extends CertificateSelector<ConformanceCredential> {
        /**
         * Construct a new CertificateSelector that will use the
         * given {@link CertificateService} to
         * retrieve one or many ConformanceCredentials.
         *
         * @param certificateService the certificate service to be used to retrieve certificates
         */
        public Selector(final CertificateService certificateService) {
            super(certificateService, ConformanceCredential.class);
        }
    }

    /**
     * Get a Selector for use in retrieving ConformanceCredentials.
     *
     * @param certificateService the CertificateService to be used to retrieve
     *                          persisted certificates
     * @return a ConformanceCredential.Selector instance to use for retrieving certificates
     */
    public static Selector select(final CertificateService certificateService) {
        return new Selector(certificateService);
    }

    /**
     * Construct a new ConformanceCredential given its binary contents.  The given certificate
     * should represent either an X509 certificate or X509 attribute certificate.
     *
     * @param certificateBytes the contents of a certificate file
     * @throws IOException if there is a problem extracting information from the certificate
     */
    public ConformanceCredential(final byte[] certificateBytes) throws IOException {
        super(certificateBytes);
    }

    /**
     * Construct a new ConformanceCredential by parsing the file at the given path.  The given
     * certificate should represent either an X509 certificate or X509 attribute certificate.
     *
     * @param certificatePath the path on disk to a certificate
     * @throws IOException if there is a problem reading the file
     */
    public ConformanceCredential(final Path certificatePath) throws IOException {
        super(certificatePath);
    }

    /**
     * Default constructor for Hibernate.
     */
    protected ConformanceCredential() {

    }
}
