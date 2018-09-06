package hirs.collector;

/**
 * <code>Exception</code> that is thrown when collection errors occur. This
 * should be thrown when unexpected events happen during collection.
 */
public class CollectorException extends Exception {

    private static final long serialVersionUID = 9167391865535628017L;

    /**
     * Creates a new <code>CollectorException</code> with the specific message.
     *
     * @param message
     *            message
     */
    public CollectorException(final String message) {
        super(message);
    }

    /**
     * Creates a new <code>CollectorException</code> that was caused by
     * <code>cause</code>.
     *
     * @param cause
     *            cause
     */
    public CollectorException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new <code>CollectorException</code> with the specific message
     * that was caused by <code>cause</code>.
     *
     * @param message
     *            message
     * @param cause
     *            cause
     */
    public CollectorException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
