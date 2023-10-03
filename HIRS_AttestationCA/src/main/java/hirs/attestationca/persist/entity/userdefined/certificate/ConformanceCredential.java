package hirs.attestationca.persist.entity.userdefined.certificate;

import hirs.utils.Certificate;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Path;

/**
 * This class persists Conformance credentials by extending the base Certificate
 * class with fields unique to Conformance credentials.
 */
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@Entity
public class ConformanceCredential extends Certificate {
    /**
     * This class enables the retrieval of ConformanceCredentials by their attributes.
     */
//    public static class Selector extends CertificateSelector<ConformanceCredential> {
//        /**
//         * Construct a new CertificateSelector that will use the given {@link CertificateManager} to
//         * retrieve one or many ConformanceCredentials.
//         *
//         * @param certificateManager the certificate manager to be used to retrieve certificates
//         */
//        public Selector(final CertificateManager certificateManager) {
//            super(certificateManager, ConformanceCredential.class);
//        }
//    }

    /**
     * Get a Selector for use in retrieving ConformanceCredentials.
     *
     * @param certMan the CertificateManager to be used to retrieve persisted certificates
     * @return a ConformanceCredential.Selector instance to use for retrieving certificates
     */
//    public static Selector select(final CertificateManager certMan) {
//        return new Selector(certMan);
//    }

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

}
