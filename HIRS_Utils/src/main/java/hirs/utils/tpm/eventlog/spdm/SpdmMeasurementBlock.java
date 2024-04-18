package hirs.utils.tpm.eventlog.spdm;

import lombok.Getter;

public class SpdmMeasurementBlock {

    /**
     * Measurement Spec.
     */
    @Getter
    private String measurementSpec = "";
    /**
     * Measurement value type (such as mutable firmware, etc).
     */
    @Getter
    private String dmtfSpecMeasurementValueType = "";
    /**
     * Measurement value (digest).
     */
    @Getter
    private String dmtfSpecMeasurementValue = "";

    public SpdmMeasurementBlock(final byte[] spdmMeasBlockBytes) {


    }

    public String toString() {
        return "TEMP TEST spdmMeasBlockBytes";
    }

}
