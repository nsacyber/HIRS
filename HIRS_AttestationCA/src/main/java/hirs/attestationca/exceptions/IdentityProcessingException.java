package hirs.attestationca.exceptions;

/**
 * Generic exception thrown while a {@link hirs.attestationca.AttestationCertificateAuthority}
 * is processing a newly submitted Identity.
 */
public class IdentityProcessingException extends RuntimeException {
    /**
     * Constructs a generic instance of this exception using the specified reason.
     *
     * @param reason for the exception
     */
    public IdentityProcessingException(final String reason) {
        super(reason);
    }

    /**
     * Constructs a instance of this exception with the specified reason and backing root
     * exception.
     *
     * @param reason        for this exception
     * @param rootException causing this exception
     */
    public IdentityProcessingException(final String reason, final Throwable rootException) {
        super(reason, rootException);
    }
}
