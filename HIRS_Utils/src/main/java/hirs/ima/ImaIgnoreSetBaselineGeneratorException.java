package hirs.ima;

/**
 * This class represents an <code>Exception</code> generated by a
 * <code>CreateImaIgnoreSetBaseline</code>.
 */
public class ImaIgnoreSetBaselineGeneratorException extends Exception {

    private static final long serialVersionUID = 1704308568386321875L;

    /**
     * Creates a new <code>CreateImaIgnoreSetBaselineException</code> that has the
     * message <code>msg</code>.
     *
     * @param msg
     *            exception message
     */
    ImaIgnoreSetBaselineGeneratorException(final String msg) {
        super(msg);
    }

    /**
     * Creates a new <code>CreateImaIgnoreSetBaselineException</code> that wraps the
     * given <code>Throwable</code>.
     *
     * @param t
     *            root cause
     */
    ImaIgnoreSetBaselineGeneratorException(final Throwable t) {
        super(t);
    }

    /**
     * Creates a new <code>CreateImaIgnoreSetBaselineException</code> that has the
     * message <code>msg</code> and wraps the root cause.
     *
     * @param msg
     *            exception message
     * @param t
     *            root cause
     */
    ImaIgnoreSetBaselineGeneratorException(final String msg, final Throwable t) {
        super(msg, t);
    }

}