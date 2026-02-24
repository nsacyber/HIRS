package hirs.attestationca.persist.exceptions;

/**
 * Exception thrown when duplicate Subject Key Identifiers (SKIs) are detected
 * within the CA credential repository.
 */
public class NonUniqueSKIException extends RuntimeException {
    /**
     * Constructs an instance of this exception with the specified reason and backing root
     * exception.
     *
     * @param reason        for this exception
     * @param rootException causing this exception
     */
    public NonUniqueSKIException(final String reason, final Throwable rootException) {
        super(reason, rootException);
    }
}
