package hirs.data.persist;

/**
 * Represents an exception thrown when a <code>Policy</code> is misconfigured in
 * some way.
 *
 */
public class PolicyException extends RuntimeException {

    private static final long serialVersionUID = 4742433920746884476L;

    /**
     * Creates a new <code>PolicyException</code> with the given message.
     *
     * @param message the message to log
     */
    public PolicyException(final String message) {
        super(message);
    }

    /**
     * Creates a new <code>PolicyException</code> with the given cause.
     *
     * @param cause the root cause
     */
    public PolicyException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new <code>PolicyException</code> with the given message and
     * cause.
     *
     * @param message the message to log
     * @param cause the root cause
     */
    public PolicyException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
