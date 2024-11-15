package hirs.attestationca.persist.entity.userdefined.certificate;

import hirs.attestationca.persist.entity.userdefined.Certificate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * This class persists Certificate Authority credentials by extending the base Certificate
 * class with fields unique to CA credentials.
 */
@Getter
@Entity
public class CertificateAuthorityCredential extends Certificate {

    /**
     * Holds the name of the 'subjectKeyIdentifier' field.
     */
    public static final String SUBJECT_KEY_IDENTIFIER_FIELD = "subjectKeyIdentifier";

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private static final String SUBJECT_KEY_IDENTIFIER_EXTENSION = "2.5.29.14";

    private static final int CA_BYTE_SIZE = 20;

    private static final int PREFIX_BYTE_SIZE = 4;

    @Getter(AccessLevel.NONE)
    @Column
    private final byte[] subjectKeyIdentifier;

    /**
     * this field is part of the TCG CA specification, but has not yet been found in
     * manufacturer-provided CAs, and is therefore not currently parsed.
     */
    @Column
    private final String credentialType = "TCPA Trusted Platform Module Endorsement";

    @Column
    private String subjectKeyIdString;

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

    /**
     * Helper method that uses the provided certificate bytes and truncates a portion
     * of the certificate bytes array.
     *
     * @param certificateBytes byte array representation of the certificate bytes
     * @return a truncated certificate byte array
     */
    private byte[] truncatePrefixBytes(final byte[] certificateBytes) {
        byte[] temp = new byte[CA_BYTE_SIZE];
        System.arraycopy(certificateBytes, PREFIX_BYTE_SIZE, temp, 0, CA_BYTE_SIZE);

        return temp;
    }

    /**
     * Compares this Certificate Authority Credential object to another Certificate
     * Authority Credential object.
     *
     * @param o object to compare
     * @return true if both this and the provided Certificate Authority Credential objects are equal,
     * false otherwise
     */
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

    /**
     * Creates an integer hash code.
     *
     * @return an integer hash code
     */
    @Override
    public int hashCode() {
        final int hashCodeConst = 31;
        int result = super.hashCode();
        result = hashCodeConst * result + credentialType.hashCode();
        result = hashCodeConst * result + Arrays.hashCode(subjectKeyIdentifier);
        return result;
    }
}
