package hirs.attestationca.persist.validation;

/**
 * This class represents exceptions thrown by the SupplyChainValidator class.
 */
public class SupplyChainValidatorException extends Exception {

    private static final long serialVersionUID = 8563981058518865230L;

    /**
     * Creates a new <code>SupplyChainValidatorException</code> that has the message
     * <code>message</code> and <code>Throwable</code> cause <code>cause</code>.
     *
     * @param message exception message
     * @param cause   root cause
     */
    public SupplyChainValidatorException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new <code>SupplyChainValidatorException</code> that has the <code>String</code>
     * message <code>message</code>.
     *
     * @param message exception message
     */
    public SupplyChainValidatorException(final String message) {
        super(message);
    }

    /**
     * Creates a new <code>SupplyChainValidatorException</code> that has the <code>Throwable</code>
     * cause <code>cause</code>.
     *
     * @param cause root cause
     */
    public SupplyChainValidatorException(final Throwable cause) {
        super(cause);
    }

}
