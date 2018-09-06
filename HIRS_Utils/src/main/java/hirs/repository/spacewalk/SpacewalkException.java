package hirs.repository.spacewalk;

/**
 * Exception class encapsulating all Spacewalk related exceptions.
 *
 */
public class SpacewalkException extends Exception {

    private static final long serialVersionUID = 2868200381049699242L;

    /**
     * Creates a SpacewalkException.
     * @param message the exception message
     */
    public SpacewalkException(final String message) {
        super(message);
    }

    /**
     * Creates a SpacewalkException.
     * @param innerException the inner exception
     */
    public SpacewalkException(final Exception innerException) {
        super(innerException);
    }

    /**
     * Creates a SpacewalkException.
     * @param message the exception message
     * @param innerException the inner exception
     */
    public SpacewalkException(final String message, final Exception innerException) {
        super(message, innerException);
    }
}
