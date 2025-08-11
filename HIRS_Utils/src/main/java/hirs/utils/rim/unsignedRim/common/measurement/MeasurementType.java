package hirs.utils.rim.unsignedRim.common.measurement;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * An enum that stores the list of measurement types.
 */
@Getter @AllArgsConstructor
public enum MeasurementType {
    /**
     * Measurement type is unknown.
     */
    UNKNOWN("Unknown"),
    /**
     * Measurement type is PC Client.
     */
    PCCLIENT("PC Client"),
    /**
     * Measurement type is DICE.
     */
    DICE("DICE");

    /**
     * Type of measurement (Pc Client, DICE, etc).
     */
    private final String type;

}
