package hirs.data.persist.enums;

/**
 * <code>HealthStatus</code> is used to represent the health of a device.
 */
public enum HealthStatus {
    /**
     * The trusted state, no issues with the device.
     */
    TRUSTED("trusted"),

    /**
     * The untrusted state, there is a problem with the device.
     */
    UNTRUSTED("untrusted"),

    /**
     * A state for when the health has not been calculated yet.
     */
    UNKNOWN("unknown");

    private String status;

    /**
     * Creates a new <code>HealthStatus</code> object given a String.
     *
     * @param status
     *            "trusted", "untrusted", or "unknown"
     */
    HealthStatus(final String status) {
        this.status = status;
    }

    /**
     * Returns the health status.
     *
     * @return the status
     */
    public String getStatus() {
        return this.status;
    }

    @Override
    public String toString() {
        return getStatus();
    }
}
