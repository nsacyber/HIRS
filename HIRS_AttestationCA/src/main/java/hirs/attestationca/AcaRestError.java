package hirs.attestationca;

/**
 * A simple POJO that will provide a clean error message to clients making
 * REST requests to the ACA. It is to be serialized to JSON for the return message.
 */
public class AcaRestError {

    private String error;

    /**
     * Basic constructor necessary for Jackson JSON serialization to work properly.
     */
    public AcaRestError() {
        // Don't remove this constructor as it's required for JSON mapping
    }

    /**
     * Parameterized constructor for creating this class normally.
     *
     * @param error the error message to store in this object
     */
    public AcaRestError(final String error) {
        this.error = error;
    }

    /**
     * Simple getter to get the error message stored in this object.
     *
     * @return the error message
     */
    public String getError() {
        return error;
    }

    /**
     * Simple setter to get the error message stored in this object.
     *
     * @param error the new error message to store in this object
     */
    public void setError(final String error) {
        this.error = error;
    }

}
