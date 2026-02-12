package hirs.attestationca.persist.exceptions;

/**
 * Generic exception thrown when processing the REST requests made by the provisioner
 * encounters an unexpected condition that can't be handled.
 */
public class UnexpectedServerException extends RuntimeException {
    /**
     * Constructs a generic instance of this exception using the specified reason.
     *
     * @param reason for the exception
     */
    public UnexpectedServerException(final String reason) {
        super(reason);
    }

    /**
     * Constructs a instance of this exception with the specified reason and backing root
     * exception.
     *
     * @param reason        for this exception
     * @param rootException causing this exception
     */
    public UnexpectedServerException(final String reason, final Throwable rootException) {
        super(reason, rootException);
    }
}
