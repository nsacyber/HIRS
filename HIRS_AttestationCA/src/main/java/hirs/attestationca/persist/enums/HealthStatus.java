package hirs.attestationca.persist.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * <code>HealthStatus</code> is used to represent the health of a device.
 */
@Getter
@AllArgsConstructor
@ToString
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

    private final String healthStatus;

    /**
     * Determines if the provided health status is a valid health status.
     *
     * @param healthStatus string representation of the healh status
     * @return true if the health status is valid, otherwise false
     */
    public static boolean isValidStatus(final String healthStatus) {
        return Arrays.stream(HealthStatus.values())
                .map(HealthStatus::name)
                .collect(Collectors.toSet())
                .contains(healthStatus);
    }
}
