package hirs.repository.spacewalk;

/**
 * Exception class encapsulating all Spacewalk exceptions relating to bad username or password.
 *
 */
public class SpacewalkAuthenticationException extends SpacewalkException {

    private static final long serialVersionUID = 28200381049689242L;

    /**
     * Creates a SpacewalkAuthenticationException.
     * @param message the exception message
     */
    public SpacewalkAuthenticationException(final String message) {
        super(message);
    }

    /**
     * Creates a SpacewalkAuthenticationException.
     * @param innerException the inner exception
     */
    public SpacewalkAuthenticationException(final Exception innerException) {
        super(innerException);
    }

    /**
     * Creates a SpacewalkAuthenticationException.
     * @param message the exception message
     * @param innerException the inner exception
     */
    public SpacewalkAuthenticationException(final String message, final Exception innerException) {
        super(message, innerException);
    }
}
