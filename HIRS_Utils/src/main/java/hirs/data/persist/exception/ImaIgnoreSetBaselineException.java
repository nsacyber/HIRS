/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hirs.data.persist.exception;

/**
 *
 */
public class ImaIgnoreSetBaselineException extends RuntimeException {

    private static final long serialVersionUID = 6085128975737370815L;

    /**
     * Creates a new <code>ImaIgnoreSetBaselineException</code>.
     */
    ImaIgnoreSetBaselineException() {
        super();
    }

    /**
     * Creates a new <code>ImaIgnoreSetBaselineException</code> that has the message
     * <code>msg</code>.
     *
     * @param msg exception message
     */
    public ImaIgnoreSetBaselineException(final String msg) {
        super(msg);
    }

    /**
     * Creates a new <code>ImaIgnoreSetBaselineException</code> that wraps the given
     * <code>Throwable</code>.
     *
     * @param t root cause
     */
    ImaIgnoreSetBaselineException(final Throwable t) {
        super(t);
    }

    /**
     * Creates a new <code>ImaIgnoreSetBaselineException</code> that has the message
     * <code>msg</code> and wraps the root cause.
     *
     * @param msg exception message
     * @param t root cause
     */
    ImaIgnoreSetBaselineException(final String msg, final Throwable t) {
        super(msg, t);
    }
}
