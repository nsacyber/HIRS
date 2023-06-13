package hirs.attestationca.persist.entity.userdefined.certificate;

import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.service.CertificateServiceImpl;
import hirs.attestationca.persist.service.selector.CertificateSelector;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * This class persists Certificate Authority credentials by extending the base Certificate
 * class with fields unique to CA credentials.
 */
@Entity
public class CertificateAuthorityCredential extends Certificate {


    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private static final String SUBJECT_KEY_IDENTIFIER_EXTENSION = "2.5.29.14";

    /**
     * Holds the name of the 'subjectKeyIdentifier' field.
     */
    public static final String SUBJECT_KEY_IDENTIFIER_FIELD = "subjectKeyIdentifier";

    private static final int CA_BYTE_SIZE = 20;
    private static final int PREFIX_BYTE_SIZE = 4;

    @Column
    private final byte[] subjectKeyIdentifier;

    @Getter
    @Column
    private String subjectKeyIdString;

    /**
     * this field is part of the TCG CA specification, but has not yet been found in
     * manufacturer-provided CAs, and is therefore not currently parsed.
     */
    @Getter
    @Column
    private final String credentialType = "TCPA Trusted Platform Module Endorsement";

    /**
     * This class enables the retrieval of CertificateAuthorityCredentials by their attributes.
     */
    public static class Selector extends CertificateSelector<CertificateAuthorityCredential> {
        /**
         * Construct a new CertificateSelector that will use the given {@link CertificateServiceImpl} to
         * retrieve one or many CertificateAuthorityCredentials.
         *
         * @param certificateService the certificate manager to be used to retrieve certificates
         */
        public Selector(final CertificateServiceImpl certificateService) {
            super(certificateService, CertificateAuthorityCredential.class);
        }

        /**
         * Specify a subject key identifier that certificates must have to be considered
         * as matching.
         *
         * @param subjectKeyIdentifier a subject key identifier buffer to query, not empty or null
         * @return this instance (for chaining further calls)
         */
        public Selector bySubjectKeyIdentifier(final byte[] subjectKeyIdentifier) {
            setFieldValue(SUBJECT_KEY_IDENTIFIER_FIELD, subjectKeyIdentifier);
            return this;
        }
    }

    /**
     * Get a Selector for use in retrieving CertificateAuthorityCredentials.
     *
     * @param certMan the CertificateService to be used to retrieve persisted certificates
     * @return a CertificateAuthorityCredential.Selector instance to use for retrieving certificates
     */
    public static Selector select(final CertificateServiceImpl certMan) {
        return new Selector(certMan);
    }

    /**
     * Construct a new CertificateAuthorityCredential given its binary contents.  The given
     * certificate should represent either an X509 certificate or X509 attribute certificate.
     *
     * @param certificateBytes the contents of a certificate file
     * @throws IOException if there is a problem extracting information from the certificate
     */
    public CertificateAuthorityCredential(final byte[] certificateBytes)
            throws IOException {
        super(certificateBytes);
        byte[] tempBytes = getX509Certificate()
                .getExtensionValue(SUBJECT_KEY_IDENTIFIER_EXTENSION);

        if (tempBytes != null && tempBytes.length > CA_BYTE_SIZE) {
            this.subjectKeyIdentifier = truncatePrefixBytes(tempBytes);
        } else {
            this.subjectKeyIdentifier =
                    getX509Certificate().getExtensionValue(SUBJECT_KEY_IDENTIFIER_EXTENSION);
        }

        if (this.subjectKeyIdentifier != null) {
            this.subjectKeyIdString = Hex.encodeHexString(this.subjectKeyIdentifier);
        }
    }

    /**
     * Construct a new CertificateAuthorityCredential by parsing the file at the given path.
     * The given certificate should represent either an X509 certificate or X509 attribute
     * certificate.
     *
     * @param certificatePath the path on disk to a certificate
     * @throws IOException if there is a problem reading the file
     */
    public CertificateAuthorityCredential(final Path certificatePath)
            throws IOException {
        super(certificatePath);
        byte[] tempBytes = getX509Certificate()
                .getExtensionValue(SUBJECT_KEY_IDENTIFIER_EXTENSION);

        if (tempBytes.length > CA_BYTE_SIZE) {
            this.subjectKeyIdentifier = truncatePrefixBytes(tempBytes);
        } else {
            this.subjectKeyIdentifier =
                    getX509Certificate().getExtensionValue(SUBJECT_KEY_IDENTIFIER_EXTENSION);
        }
        if (this.subjectKeyIdentifier != null) {
            this.subjectKeyIdString = Hex.encodeHexString(this.subjectKeyIdentifier);
        }
    }

    /**
     * Default constructor for Hibernate.
     */
    protected CertificateAuthorityCredential() {
        subjectKeyIdentifier = null;
    }

    /**
     * @return this certificate's subject key identifier.
     */
    public byte[] getSubjectKeyIdentifier() {
        if (subjectKeyIdentifier != null) {
            return subjectKeyIdentifier.clone();
        }
        return null;
    }

    private byte[] truncatePrefixBytes(final byte[] certificateBytes) {
        byte[] temp = new byte[CA_BYTE_SIZE];
        System.arraycopy(certificateBytes, PREFIX_BYTE_SIZE, temp, 0, CA_BYTE_SIZE);

        return temp;
    }

    @Override
    @SuppressWarnings("checkstyle:avoidinlineconditionals")
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        CertificateAuthorityCredential that = (CertificateAuthorityCredential) o;

//        if (!Objects.equals(credentialType, that.credentialType)) {
//            return false;
//        }

        return Arrays.equals(subjectKeyIdentifier, that.subjectKeyIdentifier);
    }

    @Override
    @SuppressWarnings({"checkstyle:magicnumber", "checkstyle:avoidinlineconditionals"})
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (credentialType != null ? credentialType.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(subjectKeyIdentifier);
        return result;
    }
}
