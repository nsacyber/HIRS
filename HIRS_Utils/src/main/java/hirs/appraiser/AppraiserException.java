package hirs.appraiser;

/**
 * <code>Exception</code> that is thrown when appraisal errors occur. This
 * should not be thrown for failed appraisals but rather when unexpected events
 * happen during appraisal. An example would be that a report is missing a
 * section that is required for appraisal.
 */
public class AppraiserException extends RuntimeException {

    private static final long serialVersionUID = 6433101465643135458L;

    /**
     * Creates a new <code>AppraiserException</code> with the specific message.
     *
     * @param message message
     */
    public AppraiserException(final String message) {
        super(message);
    }

    /**
     * Creates a new <code>AppraiserException</code> that was caused by
     * <code>cause</code>.
     *
     * @param cause
     *            cause
     */
    public AppraiserException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new <code>AppraiserException</code> with the specific message
     * that was caused by <code>cause</code>.
     *
     * @param message
     *            message
     * @param cause
     *            cause
     */
    public AppraiserException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
