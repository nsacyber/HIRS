package hirs.attestationca.persist.exceptions;

/**
 * Generic exception thrown while a {@link hirs.attestationca.persist.AttestationCertificateAuthority}
 * is processing a newly created Attestation Certificate for a validated identity.
 */
public class CertificateProcessingException extends RuntimeException {
    /**
     * Constructs a generic instance of this exception using the specified reason.
     *
     * @param reason for the exception
     */
    public CertificateProcessingException(final String reason) {
        super(reason);
    }

    /**
     * Constructs a instance of this exception with the specified reason and backing root
     * exception.
     *
     * @param reason        for this exception
     * @param rootException causing this exception
     */
    public CertificateProcessingException(final String reason, final Throwable rootException) {
        super(reason, rootException);
    }
}
