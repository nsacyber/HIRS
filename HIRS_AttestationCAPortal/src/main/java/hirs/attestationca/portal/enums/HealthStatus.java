package hirs.attestationca.portal.enums;

import java.util.Arrays;
import java.util.stream.Collectors;

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

    private String healthStatus;

    /**
     * Creates a new <code>HealthStatus</code> object given a String.
     *
     * @param healthStatus
     *            "trusted", "untrusted", or "unknown"
     */
    HealthStatus(final String healthStatus) {
        this.healthStatus = healthStatus;
    }

    /**
     * Returns the health status.
     *
     * @return the status
     */
    public String getStatus() {
        return this.healthStatus;
    }

    @Override
    public String toString() {
        return getStatus();
    }

    public static boolean isValidStatus(final String healthStatus) {
        return Arrays.stream(HealthStatus.values())
                .map(HealthStatus::name)
                .collect(Collectors.toSet())
                .contains(healthStatus);
    }
}